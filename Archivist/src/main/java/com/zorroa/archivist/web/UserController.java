package com.zorroa.archivist.web;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController  {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @RequestMapping(value="/api/v1/login", method=RequestMethod.POST)
    public User login() {
        return userService.login();
    }

    @RequestMapping(value="/api/v1/logout", method=RequestMethod.POST)
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }

    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users")
    public List<User> getAll() {
        return userService.getAll();
    }

    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users", method=RequestMethod.POST)
    public User create(@RequestBody UserBuilder builder) {
        return userService.create(builder);
    }

    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users/{id}")
    public User get(@PathVariable int id) {
        return userService.get(id);
    }

    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.PUT)
    public User update(@RequestBody UserUpdateBuilder builder, @PathVariable int id, HttpSession httpSession) {
        Session session = userService.getActiveSession();

        if (session.getUserId() == id || SecurityUtils.hasPermission("manager", "systems")) {
            User user = userService.get(id);
            userService.update(user, builder);
            return userService.get(id);
        }
        else {
            throw new SecurityException("You do not have the access to modify this user.");
        }
    }

    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.DELETE)
    public void delete(@PathVariable int id) {
        User user = userService.get(id);
        userService.delete(user);
    }

    /**
     * Return the list of permissions for the given user id.
     *
     * @param id
     * @return
     */
    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users/{id}/permissions", method=RequestMethod.GET)
    public List<Permission> getPermissions(@PathVariable int id) {
        User user = userService.get(id);
        return userService.getPermissions(user);
    }

    /**
     * Set an array of integers that correspond to permission IDs.  These
     * will be assigned to the user as permissions.  The Permission object
     * assigned are returned back.
     *
     * @param pids
     * @param id
     * @return
     */
    @PreAuthorize("hasRole('manager')")
    @RequestMapping(value="/api/v1/users/{id}/permissions", method=RequestMethod.PUT)
    public List<Permission> setPermissions(@RequestBody List<Integer> pids, @PathVariable int id) {
        User user = userService.get(id);
        List<Permission> perms = pids.stream().map(
                i->userService.getPermission(i)).collect(Collectors.<Permission>toList());

        userService.setPermissions(user, perms);
        return userService.getPermissions(user);
    }
}
