package com.activiti.security;

import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.redirect.AbstractRedirectResourceDetails;

public class IntrospectionResourceDetails extends AbstractRedirectResourceDetails{
	private String introspectionEndpoint;	
	private OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails;
	
	public IntrospectionResourceDetails() {
		setGrantType("introspection");
	}
	
	public String getIntrospectionEndpoint() {
		
		return introspectionEndpoint;
	}
	public void setIntrospectionEndpoint(String introspectionEndpoint) {
		this.introspectionEndpoint = introspectionEndpoint;
	}
	public void setOAuth2ProtectedResourceDetails(OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails) {
		this.oAuth2ProtectedResourceDetails = oAuth2ProtectedResourceDetails;
	}

	public OAuth2ProtectedResourceDetails getOAuth2ProtectedResourceDetails() {
		return this.oAuth2ProtectedResourceDetails;
	}	

}
