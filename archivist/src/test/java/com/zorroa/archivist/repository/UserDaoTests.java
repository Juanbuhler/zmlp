package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserProfileUpdate;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.RoomBuilder;
import com.zorroa.sdk.domain.Session;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

import static org.junit.Assert.*;

public class UserDaoTests extends AbstractTest {

    @Autowired
    UserDao userDao;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    RoomDao roomDao;

    @Autowired
    SessionDao sessionDao;

    User user;

    @Before
    public void init() {
        UserSpec builder = new UserSpec();
        builder.setUsername("test");
        builder.setPassword("test");
        builder.setEmail("test@test.com");
        user = userDao.create(builder);
    }

    @Test
    public void testGet() {
        User user2 = userDao.get(user.getId());
        assertEquals(user.getId(), user2.getId());
    }

    @Test
    public void testGetCount() {
        long count = userDao.getCount();
        assertEquals(count, userDao.getCount());
        UserSpec builder = new UserSpec();
        builder.setUsername("test2");
        builder.setPassword("test2");
        builder.setEmail("test@test.com");
        user = userDao.create(builder);
        assertEquals(++count, userDao.getCount());
    }

    @Test
    public void testAll() {
        assertEquals(3, userDao.getAll().size());

        UserSpec builder = new UserSpec();
        builder.setUsername("foo");
        builder.setPassword("test");
        builder.setEmail("test@test.com");
        userDao.create(builder);

        assertEquals(4, userDao.getAll().size());
    }

    @Test
    public void testAllPageable() {
        assertEquals(3, userDao.getAll(Pager.first()).size());
        assertEquals(0, userDao.getAll(new Pager(2, 4)).size());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetFailed() {
        userDao.get("blah");
    }

    @Test
    public void testGetPassword() {
        // The crypted password
        String hashed = userDao.getPassword(user.getUsername());
        assertTrue(hashed.startsWith("$"));

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("test", hashed));
        assertFalse(BCrypt.checkpw("gtfo", hashed));
    }

    @Test
    public void testResetPassword() {
        assertTrue(userDao.setPassword(user, "fiddlesticks"));
        assertTrue(BCrypt.checkpw("fiddlesticks", userDao.getPassword(user.getUsername())));
        assertFalse(BCrypt.checkpw("smeagol", userDao.getPassword(user.getUsername())));
    }

    @Test
    public void testUpdate() {
        UserProfileUpdate builder = new UserProfileUpdate();
        builder.setFirstName("foo");
        builder.setLastName("bar");
        builder.setEmail("test@test.com");

        assertTrue(userDao.update(user, builder));
        User updated = userDao.get(user.getId());
        assertEquals(builder.getEmail(), updated.getEmail());
        assertEquals(builder.getFirstName(), updated.getFirstName());
        assertEquals(builder.getLastName(), updated.getLastName());
    }

    @Test
    public void testSetEnabled() {
        assertTrue(userDao.setEnabled(user, false));
        assertFalse(userDao.setEnabled(user, false));
    }

    @Test
    public void testExists() {
        assertTrue(userDao.exists(user.getUsername()));
        assertFalse(userDao.exists("sibawitzawis"));
    }

    @Test
    public void testGetUsers() {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("the room");
        bld.setVisible(true);
        Room room = roomDao.create(bld);

        Session session = sessionDao.create(userDao.get(1), "1");
        roomDao.join(room, session);

        List<User> users = userDao.getAll(room);
        assertEquals(1, users.size());
        assertTrue(users.contains(userDao.get(1)));
    }

    @Test
    public void testHasPermissionUningNames() {
        assertFalse(userDao.hasPermission(user, "group", "manager"));
        userDao.addPermission(user, permissionDao.get("group::manager"), false);
        assertTrue(userDao.hasPermission(user, "group", "manager"));
        assertFalse(userDao.hasPermission(user, "a", "b"));
    }

    @Test
    public void testHasPermission() {
        assertFalse(userDao.hasPermission(user, "group", "manager"));
        userDao.addPermission(user, permissionDao.get("group::manager"), false);
        assertTrue(userDao.hasPermission(user, permissionDao.get("group", "manager")));
        assertFalse(userDao.hasPermission(user, permissionDao.get("group", "administrator")));
    }

    @Test
    public void testAddPermission() {
        userDao.addPermission(user, permissionDao.get("group::manager"), false);
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(permissionDao.get("group::manager")));
    }

    @Test
    public void testSetPermissions() {
        Permission p = permissionDao.get("group::manager");
        assertEquals(1, userDao.setPermissions(user, Lists.newArrayList(p)));
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(p));
    }

}
