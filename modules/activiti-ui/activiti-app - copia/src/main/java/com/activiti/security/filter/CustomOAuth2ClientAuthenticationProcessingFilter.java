package com.activiti.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import com.activiti.security.ContinueRequestAwareAuthenticationSuccessHandler;
import com.activiti.security.IntrospectionResourceDetails;

public class CustomOAuth2ClientAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
	
	public OAuth2RestOperations restTemplate;

	private ResourceServerTokenServices tokenServices;

	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();
	
	private ContinueRequestAwareAuthenticationSuccessHandler successHandler = new ContinueRequestAwareAuthenticationSuccessHandler();

	public CustomOAuth2ClientAuthenticationProcessingFilter(String defaultFilterProcessesUrl) {
		super(defaultFilterProcessesUrl);
		super.setAuthenticationSuccessHandler(successHandler);
		setAuthenticationManager(new NoopAuthenticationManager());
		setAuthenticationDetailsSource(authenticationDetailsSource);
	}
	public CustomOAuth2ClientAuthenticationProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
		super(requiresAuthenticationRequestMatcher);
		super.setAuthenticationSuccessHandler(successHandler);
		// TODO Auto-generated constructor stub
		setAuthenticationManager(new NoopAuthenticationManager());
		setAuthenticationDetailsSource(authenticationDetailsSource);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		OAuth2AccessToken accessToken;
		try {
			accessToken = restTemplate.getAccessToken();
		} catch (OAuth2Exception e) {
			throw new BadCredentialsException("Could not obtain access token", e);			
		}		
		try {
			OAuth2Authentication result = tokenServices.loadAuthentication(accessToken.getValue());
			if (authenticationDetailsSource!=null) {
				request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, accessToken.getValue());
				request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_TYPE, accessToken.getTokenType());
				result.setDetails(authenticationDetailsSource.buildDetails(request));
			}
			successHandler.setIsRedirectUrl(!(restTemplate.getResource() instanceof IntrospectionResourceDetails));
			return result;
		}
		catch (InvalidTokenException e) {
			throw new BadCredentialsException("Could not obtain user details from token", e);
		}
	}
	@Override
	public void afterPropertiesSet() {
		Assert.state(restTemplate != null, "Supply a rest-template");
		super.afterPropertiesSet();
	}
	/**
	 * Reference to a CheckTokenServices that can validate an OAuth2AccessToken
	 * 
	 * @param tokenServices
	 */
	public void setTokenServices(ResourceServerTokenServices tokenServices) {
		this.tokenServices = tokenServices;
	}
	/**
	 * A rest template to be used to obtain an access token.
	 * 
	 * @param restTemplate a rest template
	 */
	public void setRestTemplate(OAuth2RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain, Authentication authResult) throws IOException, ServletException {
		super.successfulAuthentication(request, response, chain, authResult);
		// Nearly a no-op, but if there is a ClientTokenServices then the token will now be stored
		restTemplate.getAccessToken();
		if(restTemplate.getResource() instanceof IntrospectionResourceDetails){
			chain.doFilter(request, response);
		}
	}	

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		if (failed instanceof AccessTokenRequiredException) {
			// Need to force a redirect via the OAuth client filter, so rethrow here
			throw failed;
		}
		else {
			// If the exception is not a Spring Security exception this will result in a default error page
			super.unsuccessfulAuthentication(request, response, failed);
		}
	}
	
	private static class NoopAuthenticationManager implements AuthenticationManager {
		@Override
		public Authentication authenticate(Authentication authentication)
				throws AuthenticationException {
			throw new UnsupportedOperationException("No authentication should be done with this AuthenticationManager");
		}
		
	}


}
