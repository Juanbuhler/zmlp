package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class UserController  {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @Autowired
    Validator validator;

    @RequestMapping(value="/api/v1/generate_api_key", method=RequestMethod.POST)
    public String generate_api_key() {
        return userService.generateHmacKey(SecurityUtils.getUsername());
    }

    /**
     * An HTTP auth based login endpoint.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/login", method=RequestMethod.POST)
    public User login() {
        return userService.get(SecurityUtils.getUser().getId());
    }

    /**
     * An HTTP auth based logout endpoint.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/logout", method=RequestMethod.POST)
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }


    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users")
    public List<User> getAll() {
        return userService.getAll();
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users", method=RequestMethod.POST)
    public User create(@Valid @RequestBody UserSpec builder, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new RuntimeException("Failed to add user");
        }
        return userService.create(builder);
    }

    @RequestMapping(value="/api/v1/who")
    public User getCurrent() {
        return userService.get(SecurityUtils.getUser().getId());
    }

    @RequestMapping(value="/api/v1/users/{id}")
    public User get(@PathVariable int id) {
        validatePermissions(id);
        return userService.get(id);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users/{username}/_exists")
    public Map get(@PathVariable String username) {
        return ImmutableMap.of("result", userService.exists(username));
    }

    @RequestMapping(value="/api/v1/users/{id}/_profile", method=RequestMethod.PUT)
    public Object updateProfile(@RequestBody UserProfileUpdate form, @PathVariable int id) {
        validatePermissions(id);
        User user = userService.get(id);
        return HttpUtils.updated("users", id, userService.update(user, form), userService.get(id));
    }

    @RequestMapping(value="/api/v1/users/{id}/_settings", method=RequestMethod.PUT)
    public Object updateSettings(@RequestBody UserSettings settings, @PathVariable int id) {
        validatePermissions(id);
        User user = userService.get(id);
        return HttpUtils.updated("users", id, userService.updateSettings(user, settings), userService.get(id));
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.DELETE)
    public Object disable(@PathVariable int id) {
        User user = userService.get(id);
        if (id == SecurityUtils.getUser().getId()) {
            throw new IllegalArgumentException("You cannot disable yourself");
        }
        return HttpUtils.status("users", id, "disable", userService.setEnabled(user, false));
    }

    /**
     * Return the list of permissions for the given user id.
     *
     * @param id
     * @return
     */
    @RequestMapping(value="/api/v1/users/{id}/permissions", method=RequestMethod.GET)
    public List<Permission> getPermissions(@PathVariable int id) {
        validatePermissions(id);
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
    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users/{id}/permissions", method=RequestMethod.PUT)
    public List<Permission> setPermissions(@RequestBody List<Integer> pids, @PathVariable int id) {
        User user = userService.get(id);
        List<Permission> perms = pids.stream().map(
                i->userService.getPermission(i)).collect(Collectors.toList());
        userService.setPermissions(user, perms);
        return userService.getPermissions(user);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users/{id}/permissions/_add", method=RequestMethod.PUT)
    public List<Permission> addPermissions(@RequestBody List<String> pids, @PathVariable int id) {
        User user = userService.get(id);
        Set<Permission> resolved = Sets.newHashSetWithExpectedSize(pids.size());
        for (String pid: pids) {
            resolved.add(userService.getPermission(pid));
        }
        userService.addPermissions(user, resolved);
        return userService.getPermissions(user);
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/users/{id}/permissions/_remove", method=RequestMethod.PUT)
    public List<Permission> removePermissions(@RequestBody List<String> pids, @PathVariable int id) {
        User user = userService.get(id);
        Set<Permission> resolved = Sets.newHashSetWithExpectedSize(pids.size());
        for (String pid: pids) {
            resolved.add(userService.getPermission(pid));
        }
        userService.removePermissions(user, resolved);
        return userService.getPermissions(user);
    }

    private void validatePermissions(int id) {
        if (SecurityUtils.getUser().getId() != id && !SecurityUtils.hasPermission("group::manager")) {
            throw new SecurityException("Access denied.");
        }
    }
}
