package com.activiti.security.jwt;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.activiti.security.UserAuthentication;

//@Service
public class JwtAuthenticationService {
	private static final String AUTH_HEADER_NAME = "Authorization";
    private final JwtTokenHandler tokenHandler;
    
    @Autowired
    public JwtAuthenticationService(JwtTokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    public String getAuthenticationToken(final UserAuthentication user) {
        return tokenHandler.createTokenForUser(user);
    }

    public Authentication getAuthentication(HttpServletRequest request) {
        final String authHeader = request.getHeader(AUTH_HEADER_NAME);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            final UserAuthentication user = tokenHandler.parseUserFromToken(token);
            if (user != null) {
                return user;
            }
        }
        return null;
    }
}
