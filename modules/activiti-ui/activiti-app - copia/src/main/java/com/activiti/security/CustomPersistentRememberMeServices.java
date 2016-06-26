/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.activiti.security;

import com.activiti.domain.idm.PersistentToken;
import com.activiti.domain.idm.User;
import com.activiti.repository.idm.UserRepository;
import com.activiti.service.idm.PersistentTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;

/**
 * Custom implementation of Spring Security's RememberMeServices.
 * <p/>
 * Persistent tokens are used by Spring Security to automatically log in users.
 * <p/>
 * This is a specific implementation of Spring Security's remember-me authentication, but it is much
 * more powerful than the standard implementations:
 * <ul>
 * <li>It allows a user to see the list of his currently opened sessions, and invalidate them</li>
 * <li>It stores more information, such as the IP address and the user agent, for audit purposes<li>
 * <li>When a user logs out, only his current session is invalidated, and not all of his sessions</li>
 * </ul>
 * <p/>
 * This is inspired by:
 * <ul>
 * <li><a href="http://jaspan.com/improved_persistent_login_cookie_best_practice">Improved Persistent Login Cookie
 * Best Practice</a></li>
 * <li><a href="https://github.com/blog/1661-modeling-your-app-s-user-session">Github's "Modeling your App's User Session"</a></li></li>
 * </ul>
 * <p/>
 * The main algorithm comes from Spring Security's PersistentTokenBasedRememberMeServices, but this class
 * couldn't be cleanly extended.
 * <p/>
 */
@Service
public class CustomPersistentRememberMeServices extends AbstractRememberMeServices implements CustomRememberMeService {

    private final Logger log = LoggerFactory.getLogger(CustomPersistentRememberMeServices.class);
    
    public static final String COOKIE_NAME = "ACTIVITI_REMEMBER_ME";

    @Autowired
    private PersistentTokenService persistentTokenService;

    @Autowired
    private CustomUserDetailService customUserDetailService;
    
    @Autowired
    private UserRepository userRepository;
    
    private final int tokenMaxAgeInSeconds;
    private final long tokenMaxAgeInMilliseconds;
    private final long tokenRefreshDurationInMilliseconds;
    
    @Autowired
    public CustomPersistentRememberMeServices(Environment env, org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        super(env.getProperty("security.rememberme.key"), userDetailsService);
        
        setAlwaysRemember(true);
        
        Integer tokenMaxAgeSeconds = env.getProperty("security.cookie.max-age", Integer.class);
        if (tokenMaxAgeSeconds != null) {
        	log.info("Cookie max-age set to " + tokenMaxAgeSeconds + " seconds");
        } else {
        	tokenMaxAgeSeconds = 2678400; // Default: 31 days
        }
        tokenMaxAgeInSeconds = tokenMaxAgeSeconds;
        tokenMaxAgeInMilliseconds = tokenMaxAgeSeconds.longValue() * 1000L;
        
        Integer tokenRefreshSeconds = env.getProperty("security.cookie.refresh-age", Integer.class);
        if (tokenRefreshSeconds != null) {
        	log.info("Cookie refresh age set to " + tokenRefreshSeconds + " seconds");
        } else {
        	tokenRefreshSeconds = 86400; // Default : 1 day
        }
        tokenRefreshDurationInMilliseconds = tokenRefreshSeconds.longValue() * 1000L;
        
        setCookieName(COOKIE_NAME);
    }
    
    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        String userEmail = successfulAuthentication.getName();

        log.debug("Creating new persistent login for user {}", userEmail);
        ActivitiAppUser activitiAppUser = (ActivitiAppUser) successfulAuthentication.getPrincipal();

        PersistentToken token = createAndInsertPersistentToken(activitiAppUser.getUserObject(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        addCookie(token, request, response);
    }

    @Override
    @Transactional
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {

        PersistentToken token = getPersistentToken(cookieTokens);

        // Refresh token if refresh period has been passed
        if (new Date().getTime() - token.getTokenDate().getTime() > tokenRefreshDurationInMilliseconds) {
        
//	        log.info("Refreshing persistent login token for user '{}', series '{}'", token.getUserId(), token.getSeries());
	        try {
	        	
	        	// Refreshing: creating a new token to be used for subsequent calls
	        	
	        	token = persistentTokenService.createToken(userRepository.findOne(token.getUserId()), request.getRemoteAddr(), request.getHeader("User-Agent"));
	            addCookie(token, request, response);
	            
	        } catch (DataAccessException e) {
	            log.error("Failed to update token: ", e);
	            throw new RememberMeAuthenticationException("Autologin failed due to data access problem: " + e.getMessage());
	        }
	        
        }
        
        return customUserDetailService.loadByUserId(token.getUserId());
    }
    
    /**
     * When logout occurs, only invalidate the current token, and not all user sessions.
     * <p/>
     * The standard Spring Security implementations are too basic: they invalidate all tokens for the
     * current user, so when he logs out from one browser, all his other sessions are destroyed.
     */
    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String rememberMeCookie = extractRememberMeCookie(request);
        if (rememberMeCookie != null && rememberMeCookie.length() != 0) {
            try {
                String[] cookieTokens = decodeCookie(rememberMeCookie);
                PersistentToken token = getPersistentToken(cookieTokens);
                persistentTokenService.delete(token);
            } catch (InvalidCookieException ice) {
                log.info("Invalid cookie, no persistent token could be deleted");
            } catch (RememberMeAuthenticationException rmae) {
                log.debug("No persistent token found, so no token could be deleted");
            }
        }
        super.logout(request, response, authentication);
    }

    /**
     * Validate the token and return it.
     */
    private PersistentToken getPersistentToken(String[] cookieTokens) {
        if (cookieTokens.length != 2) {
            throw new InvalidCookieException("Cookie token did not contain " + 2 +
                    " tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
        }

        final String presentedSeries = cookieTokens[0];
        final String presentedToken = cookieTokens[1];

        PersistentToken token = persistentTokenService.getPersistentToken(presentedSeries);

        if (token == null) {
            // No series match, so we can't authenticate using this cookie
            throw new RememberMeAuthenticationException("No persistent token found for series id: " + presentedSeries);
        }

        // We have a match for this user/series combination
        if (!presentedToken.equals(token.getTokenValue())) {
        	
        	// This could be caused by the opportunity window where the token just has been refreshed, but 
        	// has not been put into the token cache yet. Invalidate the token and refetch and it the new token value from the db is now returned.
        	
        	token = persistentTokenService.getPersistentToken(presentedSeries, true); // Note the 'true' here, which invalidates the cache before fetching
        	if (!presentedToken.equals(token.getTokenValue())) {
        	
	            // Token doesn't match series value. Delete this session and throw an exception.
	        	persistentTokenService.delete(token);
	            throw new CookieTheftException("Invalid remember-me token (Series/token) mismatch. Implies previous cookie theft attack.");
	            
        	}
        }

        if (new Date().getTime() - token.getTokenDate().getTime() > tokenMaxAgeInMilliseconds) {
            throw new RememberMeAuthenticationException("Remember-me login has expired");
        }
        return token;
    }

    private void addCookie(PersistentToken token, HttpServletRequest request, HttpServletResponse response) {
        setCookie(
                new String[]{token.getSeries(), token.getTokenValue()},
                tokenMaxAgeInSeconds, request, response);
    }
    
    @Override
    protected void setCookie(String[] tokens, int maxAge, HttpServletRequest request, HttpServletResponse response) {
        String cookieValue = encodeCookie(tokens);
        Cookie cookie = new Cookie(getCookieName(), cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");

        cookie.setSecure(request.isSecure());

        Method setHttpOnlyMethod = ReflectionUtils.findMethod(Cookie.class,"setHttpOnly", boolean.class);
        if(setHttpOnlyMethod != null) {
            ReflectionUtils.invokeMethod(setHttpOnlyMethod, cookie, Boolean.TRUE);
        } else if (logger.isDebugEnabled()) {
            logger.debug("Note: Cookie will not be marked as HttpOnly because you are not using Servlet 3.0 (Cookie#setHttpOnly(boolean) was not found).");
        }

        response.addCookie(cookie);
    }

    @Override
    public PersistentToken createAndInsertPersistentToken(User user, String remoteAddress, String userAgent) {
        return persistentTokenService.createToken(user, remoteAddress, userAgent);
    }
}
