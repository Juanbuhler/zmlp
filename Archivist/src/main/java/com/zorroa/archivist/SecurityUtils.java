package com.zorroa.archivist;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.User;
import org.elasticsearch.index.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.List;

public class SecurityUtils {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    public static String createPasswordHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static String getSessionId() {
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

    public static List<Integer> getPermissionIds() {
        List<Integer> result = Lists.newArrayList();
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
