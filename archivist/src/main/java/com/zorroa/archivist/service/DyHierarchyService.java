package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.DyHierarchySpec;
import com.zorroa.archivist.domain.Folder;

/**
 * Created by chambers on 7/14/16.
 */
public interface DyHierarchyService {

    boolean update(int id, DyHierarchy spec);

    boolean delete(DyHierarchy dyhi);

    /**
     * Create a dynamic hierarchy generator.
     *
     * @param spec
     */
    DyHierarchy create(DyHierarchySpec spec);

    /**
     * Generate a dynamic hierarchy.
     *
     * @param agg
     */
    int generate(DyHierarchy agg);

    DyHierarchy get(int id);

    DyHierarchy get(Folder folder);

    /**
     * Generate all hierarchies.
     */
    void generateAll();

    /**
     * Submit command to generate folders on all hierarchies.  This command
     * is throttled to a reasonable rate.  To bypass the throttle, set the
     * 'force' argument to true.
     *
     * @param refresh
     */
    void submitGenerateAll(boolean refresh);

    /**
     * Generate a dynamic hierarchy.
     *
     * @param agg
     */
    void submitGenerate(DyHierarchy agg);
}
