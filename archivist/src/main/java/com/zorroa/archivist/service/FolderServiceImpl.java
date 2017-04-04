package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.repository.TrashFolderDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class FolderServiceImpl implements FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

    @Autowired
    FolderDao folderDao;

    @Autowired
    TrashFolderDao trashFolderDao;

    @Autowired
    AssetDao assetDao;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    LogService logService;

    @Override
    public boolean removeDyHierarchyRoot(Folder folder, String attribute) {
        boolean result = folderDao.removeDyHierarchyRoot(folder);
        transactionEventManager.afterCommit(() -> {
            invalidate(folder);
        });
        return result;
    }

    @Override
    public boolean setDyHierarchyRoot(Folder folder, String attribute) {
        if (folder.getId() == 0) {
            throw new ArchivistWriteException("You cannot make changes to the root folder");
        }
        boolean result = folderDao.setDyHierarchyRoot(folder, attribute);
        transactionEventManager.afterCommit(() -> {
            invalidate(folder);
        });
        return result;
    }

    @Override
    public void setAcl(Folder folder, Acl acl, boolean created) {
        SecurityUtils.canSetAclOnFolder(acl, folder.getAcl(), created);
        folderDao.setAcl(folder.getId(), acl);
        transactionEventManager.afterCommit(() -> {
            invalidate(folder);
            logService.logAsync(LogSpec.build(LogAction.Update, folder)
                    .setMessage("permissions changed"));
        });
    }

    @Override
    public Folder get(int id) {
        return folderDao.get(id);
    }

    @Override
    public Folder get(int parent, String name) {
        return folderDao.get(parent, name, false);
    }

    @Override
    public Folder get(String path) {
        int parentId = Folder.ROOT_ID;
        Folder current = null;

        if ("/".equals(path)) {
            return folderDao.get(0);
        }

        // Just throw the exception to the caller,don't return null
        // as none of the other 'get' functions do.
        for (String name : Splitter.on("/").omitEmptyStrings().trimResults().split(path)) {
            current = folderDao.get(parentId, name, false);
            parentId = current.getId();
        }
        return current;
    }

    @Override
    public boolean exists(String path) {
        try {
            get(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int count() {
        return folderDao.count();
    }

    @Override
    public int count(DyHierarchy dh) {
        return folderDao.count(dh);
    }

    @Override
    public List<Folder> getAll() {
        return folderDao.getChildren(Folder.ROOT_ID);
    }

    @Override
    public List<Folder> getAll(Collection<Integer> ids) {
        return folderDao.getAll(ids);
    }

    @Override
    public List<Integer> getAllIds(DyHierarchy dyhi) {
        return folderDao.getAllIds(dyhi);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return folderDao.getChildren(folder);
    }

    @Override
    public boolean update(int id, Folder folder) {
        if (!SecurityUtils.hasPermission(folderDao.getAcl(id), Access.Write)) {
            throw new ArchivistWriteException("You cannot make changes to this folder");
        }

        if (id == 0 || folder.getId() == 0) {
            throw new ArchivistWriteException("You cannot make changes to the root folder");
        }

        Folder current = folderDao.get(id);
        // If we have a new parent, then double check parent
        if (current.getParentId() != folder.getParentId()) {
            if (isDescendantOf(folderDao.get(folder.getParentId()), current)) {
                throw new ArchivistWriteException("You cannot move a folder into one of its descendants.");
            }
        }

        boolean result = folderDao.update(id, folder);
        if (result) {
            setAcl(folder, folder.getAcl(), false);

            transactionEventManager.afterCommitSync(() -> {
                invalidate(current, current.getParentId());
                logService.logAsync(LogSpec.build(LogAction.Update, folder));
            });
        }
        return result;
    }

    @Override
    public int deleteAll(DyHierarchy dyhi) {
        return folderDao.deleteAll(dyhi);
    }

    @Override
    public int deleteAll(Collection<Integer> ids) {
        return folderDao.deleteAll(ids);
    }

    @Override
    public TrashedFolderOp trash(Folder folder) {

        if (!SecurityUtils.hasPermission(folder.getAcl(), Access.Write)) {
            throw new ArchivistWriteException("You don't have the permissions to delete this folder");
        }

        if (folder.getId() == 0) {
            throw new ArchivistWriteException("You cannot make changes to the root folder");
        }

        /**
         * Don't allow trashing of dyhi folders
         */
        if (folder.isDyhiRoot() || folder.getDyhiId() != null) {
            throw new ArchivistWriteException("Cannot deleted dynamic hierarchy folder.");
        }

        /**
         * The Operation ID keeps track of all folders deleted by this specific
         * transaction.
         */
        String op = UUID.randomUUID().toString();

        List<Folder> children = getAllDescendants(folder, false);
        int order = 0;

        for (int i = children.size(); --i >= 0; ) {
            Folder child = children.get(i);

            if (!SecurityUtils.hasPermission(child.getAcl(), Access.Write)) {
                throw new ArchivistWriteException("You don't have the permissions to delete the subfolder " + child.getName());
            }

            if (folderDao.delete(child)) {
                order++;
                trashFolderDao.create(child, op, false, order);
                transactionEventManager.afterCommit(() -> {
                    invalidate(folder);
                    logService.logAsync(LogSpec.build(LogAction.Delete, folder));
                }, false);
            }
        }

        boolean result = folderDao.delete(folder);
        if (result) {
            order++;
            int id = trashFolderDao.create(folder, op, true, order);
            return new TrashedFolderOp(id, op, order);
        }
        else {
            throw new ArchivistWriteException("Failed to trash the given folder, already deleted.");
        }
    }

    @Override
    public TrashedFolderOp restore(TrashedFolder tf) {
        List<TrashedFolder> folders = trashFolderDao.getAll(tf.getOpId());

        int count = 0;
        for (TrashedFolder folder: folders) {
            folderDao.create(folder);
            count++;
        }

        trashFolderDao.removeAll(tf.getOpId());
        return new TrashedFolderOp(tf.getId(), tf.getOpId(), count);
    }

    @Deprecated
    @Override
    public boolean delete(Folder folder) {

        if (!SecurityUtils.hasPermission(folder.getAcl(), Access.Write)) {
            throw new ArchivistWriteException("You cannot make changes to this folder");
        }

        if (folder.getId() == 0) {
            throw new ArchivistWriteException("You cannot make changes to the root folder");
        }

        /**
         * Delete all children in reverse order.
         */
        List<Folder> children = getAllDescendants(folder, false);
        for (int i = children.size(); --i >= 0; ) {
            if (folderDao.delete(children.get(i))) {
                transactionEventManager.afterCommitSync(() -> {
                    invalidate(folder);
                    logService.logAsync(LogSpec.build(LogAction.Delete, folder));
                });
            }
        }

        boolean result = folderDao.delete(folder);
        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                invalidate(folder);
                logService.logAsync(LogSpec.build(LogAction.Delete, folder));
            });
        }
        return result;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, List<Object>> addAssets(Folder folder, List<String> assetIds) {
        if (assetIds.size() >= 1024) {
            throw new ArchivistWriteException("Cannot have more than 1024 assets in a folder");
        }

        if (!SecurityUtils.hasPermission(folder.getAcl(), Access.Write)) {
            throw new ArchivistWriteException("You cannot make changes to this folder");
        }

        if (folder.getSearch() != null) {
            throw new ArchivistWriteException("Cannot add assets to a smart folder.  Remove the search first.");
        }

        Map<String, List<Object>> result = assetDao.appendLink("folder", folder.getId(), assetIds);
        invalidate(folder);
        logService.logAsync(LogSpec.build("add_assets", folder).putToAttrs("assetIds", assetIds));
        return result;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, List<Object>> removeAssets(Folder folder, List<String> assetIds) {

        if (!SecurityUtils.hasPermission(folder.getAcl(), Access.Write)) {
            throw new ArchivistWriteException("You cannot make changes to this folder");
        }

        Map<String, List<Object>> result = assetDao.removeLink("folder", folder.getId(), assetIds);
        invalidate(folder);
        logService.logAsync(LogSpec.build("remove_assets", folder).putToAttrs("assetIds", assetIds));
        return result;
    }

    private void invalidate(Folder folder, Integer... additional) {
        if (folder != null) {
            if (folder.getParentId() != null) {
                childCache.invalidate(folder.getParentId());
            }
            childCache.invalidate(folder.getId());
        }

        for (Integer id : additional) {
            if (id == null) {
                continue;
            }
            childCache.invalidate(id);
        }
    }

    @Override
    public List<Folder> getAllDescendants(Folder folder, boolean forSearch) {
        return getAllDescendants(Lists.newArrayList(folder), false, forSearch);
    }

    @Override
    public List<Folder> getAllDescendants(Collection<Folder> startFolders, boolean includeStartFolders, boolean forSearch) {
        List<Folder> result = Lists.newArrayListWithCapacity(32);
        Queue<Folder> queue = Queues.newLinkedBlockingQueue();

        if (includeStartFolders) {
            result.addAll(startFolders);
        }

        queue.addAll(startFolders);
        getChildFoldersRecursive(result, queue, forSearch);
        return result;
    }

    private final LoadingCache<Integer, List<Folder>> childCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<Integer, List<Folder>>() {
                public List<Folder> load(Integer key) throws Exception {
                    return folderDao.getChildrenInsecure(key);
                }
            });

    /**
     * A non-recursion based search for finding all child folders
     * of a folder.
     *
     * @param result
     * @param toQuery
     */
    private void getChildFoldersRecursive(List<Folder> result, Queue<Folder> toQuery, boolean forSearch) {

        while (true) {
            Folder current = toQuery.poll();
            if (current == null) {
                return;
            }
            if (Folder.isRoot(current)) {
                continue;
            }

            /*
             * This is a potential optimization to try out that limits the need to traverse into all
             * child folders from a root.  For example, if /exports is set with a query that searches
             * for all assets that have an export ID, then there is no need to traverse all the sub
             * folders.
             */
            if (!current.isRecursive() && forSearch) {
                continue;
            }
            try {

                List<Folder> children = childCache.get(current.getId())
                        .stream()
                        .filter(f -> SecurityUtils.hasPermission(f.getAcl(), Access.Read))
                        .collect(Collectors.toList());

                if (children == null || children.isEmpty()) {
                    continue;
                }

                toQuery.addAll(children);
                result.addAll(children);

            } catch (Exception e) {
                logger.warn("Failed to obtain child folders for {}", current, e);
            }
        }
    }

    private ExecutorService folderExecutor = Executors.newSingleThreadExecutor();

    @Override
    public Future<Folder> submitCreate(FolderSpec spec, boolean mightExist) {
        return folderExecutor.submit(() -> create(spec, mightExist));
    }

    @Override
    public Future<Folder> submitCreate(Folder parent, FolderSpec spec, boolean mightExist) {
        return folderExecutor.submit(() -> create(parent, spec, mightExist));
    }

    @Override
    public Folder create(Folder parent, FolderSpec spec, boolean mightExist) {
        if (!SecurityUtils.hasPermission(parent.getAcl(), Access.Write)) {
            throw new ArchivistException("You cannot make changes to this folder");
        }

        Folder result;
        if (mightExist) {
            try {
                result = get(spec.getParentId(), spec.getName());
            } catch (EmptyResultDataAccessException e) {
                result = folderDao.create(spec);
                setAcl(result, spec.getAcl(), true);
                emitFolderCreated(result);
            }
        } else {
            try {
                result = folderDao.create(spec);
                setAcl(result, spec.getAcl(), true);
                emitFolderCreated(result);
            } catch (DuplicateKeyException e) {
                result = get(spec.getParentId(), spec.getName());
            }
        }

        return result;

    }

    private void emitFolderCreated(Folder folder) {
        transactionEventManager.afterCommitSync(() -> {
            invalidate(null, folder.getParentId());
            logService.logAsync(LogSpec.build(LogAction.Create, folder));
        });
    }

    @Override
    public Folder create(FolderSpec spec, boolean mightExist) {
        Preconditions.checkNotNull(spec.getParentId(), "Parent cannot be null");
        return create(folderDao.get(spec.getParentId()), spec, mightExist);
    }

    @Override
    public Folder create(FolderSpec spec) {
        return create(folderDao.get(spec.getParentId()), spec, false);
    }

    @Override
    public Folder createUserFolder(String username, Permission perm) {
        Folder rootFolder = folderDao.get(Folder.ROOT_ID, "Users", true);
        Folder folder = folderDao.create(new FolderSpec()
                .setName(username)
                .setParentId(rootFolder.getId()));
        folderDao.setAcl(folder.getId(), new Acl().addEntry(perm, Access.Read, Access.Write));
        return folder;
    }

    @Override
    public List<TrashedFolder> getTrashedFolders() {
        return trashFolderDao.getAll(SecurityUtils.getUser().getId());
    }

    @Override
    public List<TrashedFolder> getTrashedFolders(Folder folder) {
        return trashFolderDao.getAll(folder, SecurityUtils.getUser().getId());
    }

    @Override
    public TrashedFolder getTrashedFolder(int id) {
        return trashFolderDao.get(id, SecurityUtils.getUser().getId());
    }

    @Override
    public List<Integer> emptyTrash() {
        return trashFolderDao.removeAll(SecurityUtils.getUser().getId());
    }

    @Override
    public List<Integer> emptyTrash(List<Integer> ids) {
        return trashFolderDao.removeAll(ids, SecurityUtils.getUser().getId());
    }

    @Override
    public int trashCount() {
        return trashFolderDao.count(SecurityUtils.getUser().getId());
    }

    @Override
    public boolean isDescendantOf(Folder target, Folder moving) {
        if (target.getId() == moving.getId()) {
            return true;
        }
        while(target.getParentId() != null) {
            target = folderDao.get(target.getParentId());
            if (target.getId() == moving.getId()) {
                return true;
            }
        }
        return false;
    }

}
