package com.activiti.security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;


import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserApprovalRequiredException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;

import org.springframework.security.oauth2.client.token.OAuth2AccessTokenSupport;

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseExtractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IntrospectionEndpointTokenProvider extends OAuth2AccessTokenSupport implements AccessTokenProvider {	
	private static final String AUTH_HEADER_NAME = "Authorization";
	private String tokenAuth;
	
	public boolean supportsResource(OAuth2ProtectedResourceDetails resource) {
		return resource instanceof IntrospectionResourceDetails
				&& "introspection".equals(resource.getGrantType());
	}
	@Override
	public OAuth2AccessToken refreshAccessToken(OAuth2ProtectedResourceDetails resource, OAuth2RefreshToken refreshToken,
			AccessTokenRequest request) throws UserRedirectRequiredException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean supportsRefresh(OAuth2ProtectedResourceDetails resource) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	protected ResponseExtractor<OAuth2AccessToken> getResponseExtractor() {
		return new IntrospectionResponseExtractor(tokenAuth);
	}
	@Override
	protected String getAccessTokenUri(OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form) {
		IntrospectionResourceDetails details = (IntrospectionResourceDetails) resource;		
		String accessTokenUri = details.getIntrospectionEndpoint();
		return accessTokenUri;
	}

	public OAuth2AccessToken obtainAccessToken(OAuth2ProtectedResourceDetails details, AccessTokenRequest request)
			throws UserRedirectRequiredException, UserApprovalRequiredException, AccessDeniedException,
			OAuth2AccessDeniedException {

		IntrospectionResourceDetails resource = (IntrospectionResourceDetails) details;
		List<String> headerAutorization= request.getHeaders().get(AUTH_HEADER_NAME);
		
		if(headerAutorization!=null && !headerAutorization.isEmpty()){
			tokenAuth=headerAutorization.get(0);
		}
		if(tokenAuth==null){
			throw new UserRedirectRequiredException(resource.getUserAuthorizationUri(), request.toSingleValueMap());
		}		
		try {
			// We can assume here that the request contains all the parameters needed for authentication etc.
			OAuth2AccessToken token = retrieveToken(request,
					resource, getParametersForTokenRequest(resource, request), getHeadersForTokenRequest(resource));
			if (token==null) {
				// Probably an authenticated request, but approval is required.  TODO: prompt somehow?
				throw new UserRedirectRequiredException(resource.getRedirectUri(request), request.toSingleValueMap());				
			}
			return token;
		}
		catch (UserRedirectRequiredException e) {
			// ... but if it doesn't then capture the request parameters for the redirect
			throw new UserRedirectRequiredException(e.getRedirectUri(), request.toSingleValueMap());
		}		

	}	
	
	private MultiValueMap<String, String> getParametersForTokenRequest(IntrospectionResourceDetails resource,
			AccessTokenRequest request) {
		
		List<String> headerAutorization= request.getHeaders().get(AUTH_HEADER_NAME);
		String token=null;
		if(headerAutorization!=null && !headerAutorization.isEmpty()){
			token=headerAutorization.get(0);
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("token", token);
		form.set("client_id", resource.getClientId());
		form.set("client_secret", resource.getClientSecret());
		for (String key : request.keySet()) {
			form.put(key, request.get(key));
		}		
		return form;
	}
	private HttpHeaders getHeadersForTokenRequest(IntrospectionResourceDetails resource) {
		String auth = resource.getClientId() + ":" + resource.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64( 
           auth.getBytes(Charset.forName("US-ASCII")) );
        String authHeader = "Basic " + new String( encodedAuth );
        
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTH_HEADER_NAME, authHeader );;
		// No cookie for token request
		return headers;
	}
	private final class IntrospectionResponseExtractor implements ResponseExtractor<OAuth2AccessToken> {
		
		private final ObjectMapper objectMapper=new ObjectMapper();
		private final String ACCESS_TOKEN = "access_token";
		private String tokenAuth;
		
		
		public IntrospectionResponseExtractor(String tokenAuth){
			this.tokenAuth=tokenAuth;
		}
				
		
		public OAuth2AccessToken extractData(ClientHttpResponse response) throws IOException {
			// TODO: this should actually be a 401 if the request asked for JSON			
			TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>(){};			
            
			HashMap<String,String> responseParams =objectMapper.readValue(response.getBody(),typeRef);				
			
			if (responseParams == null || responseParams.get("active")==null || responseParams.get("active").isEmpty() 
					|| responseParams.get("active")!="true") {
				return null;
			}			
			responseParams.put(ACCESS_TOKEN, tokenAuth);
			OAuth2AccessToken accessToken = DefaultOAuth2AccessToken.valueOf(responseParams);			
			return accessToken;
		}
	}

}


