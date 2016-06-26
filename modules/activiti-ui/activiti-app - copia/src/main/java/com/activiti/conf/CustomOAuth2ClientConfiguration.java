package com.activiti.conf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;

@Configuration
@Import(OAuth2ClientConfiguration.class)
public class CustomOAuth2ClientConfiguration {
		
	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	protected AccessTokenRequest accessTokenRequest(@Value("#{request.parameterMap}")
	Map<String, String[]> parameters, @Value("#{request.getAttribute('currentUri')}") String currentUri,
	@Value("#{request.getHeader('Authorization')}") String authHeader) {	
			DefaultAccessTokenRequest request = new DefaultAccessTokenRequest(parameters);			
			request.setCurrentUri(currentUri);
			if(authHeader!=null && authHeader.startsWith("Bearer ")){
				final String token = authHeader.substring(7);
				HashMap<String,List<String>> headers= new HashMap<String,List<String>>();
				headers.put("Authorization", Arrays.asList(token));
				request.setHeaders(headers);
			}
			return request;
	}
	
}
