package com.activiti.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

public class CustomAccessTokenProviderChain extends AccessTokenProviderChain {
	private final List<AccessTokenProvider> chain;
	
	public CustomAccessTokenProviderChain(List<? extends AccessTokenProvider> chain) {
		super(chain);
		this.chain=chain == null ? Collections.<AccessTokenProvider> emptyList() : Collections
				.unmodifiableList(chain);
	}
	@Override
	protected OAuth2AccessToken obtainNewAccessTokenInternal(OAuth2ProtectedResourceDetails details,
			AccessTokenRequest request) throws UserRedirectRequiredException, AccessDeniedException {	
		
		if (request.isError()) {
			// there was an oauth error...
			throw OAuth2Exception.valueOf(request.toSingleValueMap());
		}
	  	List<String> tokenAuthorization=  request.getHeaders().get("Authorization");
	  	OAuth2ProtectedResourceDetails defaultDetails=null;
	  	
		IntrospectionResourceDetails introspectionResourceDetails=(IntrospectionResourceDetails)details;
		if(introspectionResourceDetails!=null){
			if(tokenAuthorization!=null){
				for (AccessTokenProvider tokenProvider : chain) {
					if (tokenProvider instanceof IntrospectionEndpointTokenProvider && tokenProvider.supportsResource(details)) {
						return tokenProvider.obtainAccessToken(details, request);
					}
				}
			}		
			defaultDetails=introspectionResourceDetails.getOAuth2ProtectedResourceDetails();
		}	

		for (AccessTokenProvider tokenProvider : chain) {
			if (tokenProvider.supportsResource(defaultDetails!=null?defaultDetails: details)) {
				return tokenProvider.obtainAccessToken(defaultDetails!=null?defaultDetails: details, request);
			}
		}

		throw new OAuth2AccessDeniedException("Unable to obtain a new access token for resource '" + details.getId()
				+ "'. The provider manager is not configured to support it.", details);
		
	}
}
