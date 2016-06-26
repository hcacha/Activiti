package com.activiti.security.filter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class AutentificationSecurityRequestMatcher implements RequestMatcher {
	private static final String AUTH_HEADER_NAME = "Authorization";
		
	AntPathRequestMatcher antPathRequestMatcher;
	
	public AutentificationSecurityRequestMatcher(String defaultFilterProcessesUrl){		
		this.antPathRequestMatcher=new AntPathRequestMatcher(defaultFilterProcessesUrl);
	}
	
	@Override
	public boolean matches(HttpServletRequest request) {
		// TODO Auto-generated method stub
		final String authHeader = request.getHeader(AUTH_HEADER_NAME);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);            
            if (token != null && !token.isEmpty()) {
                return true;
            }
        }		
		if(antPathRequestMatcher!=null){
			return antPathRequestMatcher.matches(request);
		}
		//AntPathRequestMatcher
		return false;
	}

}
