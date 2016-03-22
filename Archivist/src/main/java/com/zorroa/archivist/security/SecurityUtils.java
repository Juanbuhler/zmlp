package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.index.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.Set;

public class SecurityUtils {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    public static String createPasswordHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCookieId() {
        return RequestContextHolder.currentRequestAttributes().getSessionId();
    }

    public static String getUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException("No login credentials specified");
        }
        else {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return user.getUsername();
        }
    }

    public static User getUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException("No login credentials specified");
        }
        else {
            return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        }
    }

    /**
     * Return true if the user has permission to a particular type of permission.
     *
     * @param field
     * @param asset
     * @return
     */
    public static boolean hasPermission(String field, Asset asset) {
        Set<Integer> perms = asset.getAttr("permissions:"+ field, Json.SET_OF_INTS);
        return hasPermission(perms);
    }

    public static boolean hasPermission(String ... perms) {
        ImmutableSet<String> _perms = ImmutableSet.copyOf(perms);
        Collection<? extends GrantedAuthority> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        for (GrantedAuthority g: authorities) {
            if (_perms.contains(g.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPermission(Set<Integer> permIds) {
        if (permIds.isEmpty()) {
            return true;
        }
        return !Sets.intersection(permIds, SecurityUtils.getPermissionIds()).isEmpty();
    }

    public static Set<Integer> getPermissionIds() {
        Set<Integer> result = Sets.newHashSet();
        for (GrantedAuthority g: SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
            Permission p = (Permission) g;
            result.add(p.getId());
        }
        return result;
    }

    public static FilterBuilder getPermissionsFilter() {
        OrFilterBuilder result = FilterBuilders.orFilter();
        MissingFilterBuilder part1 = FilterBuilders.missingFilter("permissions.search");
        TermsFilterBuilder part2 = FilterBuilders.termsFilter("permissions.search",
                SecurityUtils.getPermissionIds());

        result.add(part1);
        result.add(part2);
        return result;
    }
}
