package com.activiti.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;


//
//@Component
public final class CustomClientOAuthDetails extends AuthorizationCodeResourceDetails{	
	
//	@Autowired
//	public CustomClientOAuthDetails(@Value("${facebook.client.clientId}") String clientId,
//			@Value("${facebook.client.clientSecret}") String clientSecret,
//			@Value("${facebook.client.accessTokenUri}") String accessTokenUri,
//			@Value("${facebook.client.userAuthorizationUri}") String userAuthorizationUri,
//			@Value("${facebook.client.tokenName}") String tokenName,
//			@Value("${facebook.client.authenticationScheme}")  AuthenticationScheme authenticationScheme,
//			@Value("${facebook.client.scope}") List<String> scope			
//			){		
//		this.setClientId(clientId);
//		this.setClientSecret(clientSecret);
//		this.setAccessTokenUri(accessTokenUri);
//		this.setAccessTokenUri(accessTokenUri);	
//		this.setUserAuthorizationUri(userAuthorizationUri);
//		this.setTokenName(tokenName);
//		this.setAuthenticationScheme(authenticationScheme);
//		this.setScope(scope);		
//	}
	
}
