package com.zorroa.archivist;

import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class ZorroaAuthenticationProvider implements AuthenticationProvider {

    protected static final Logger logger = LoggerFactory.getLogger(ZorroaAuthenticationProvider.class);

    @Autowired
    UserService userService;

    public ZorroaAuthenticationProvider() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        String storedPassword;
        User user;
        try {
            storedPassword = userService.getPassword(username);
            user = userService.get(username);
        } catch (Exception e) {
            logger.warn("failed to find user: {}", username, e);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!BCrypt.checkpw(authentication.getCredentials().toString(), storedPassword)) {
            logger.warn("password authentication failed for user: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        return new UsernamePasswordAuthenticationToken(user, storedPassword,
                userService.getPermissions(user));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }

}
