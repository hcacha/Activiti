package com.activiti.security;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.stereotype.Component;

//@Component
public final class CustomClientResourceDetails extends ResourceServerProperties {
	
//	@Autowired
//	public CustomClientResourceDetails(@Value("${facebook.client.resource.userInfoUri}") String userInfoUri){		
//		this.setUserInfoUri(userInfoUri);		
//	}
}
