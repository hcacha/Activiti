package com.activiti.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;

public class ContinueRequestAwareAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	protected final Log logger = LogFactory.getLog(this.getClass());
	private RequestCache requestCache = new HttpSessionRequestCache();
	private boolean isRedirectUrl=true;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication)
			throws ServletException, IOException {
		
		if(isRedirectUrl){
			SavedRequest savedRequest = requestCache.getRequest(request, response);

			if (savedRequest == null) {
				super.onAuthenticationSuccess(request, response, authentication);

				return;
			}
			String targetUrlParameter = getTargetUrlParameter();
			if (isAlwaysUseDefaultTargetUrl()
					|| (targetUrlParameter != null && StringUtils.hasText(request
							.getParameter(targetUrlParameter)))) {
				requestCache.removeRequest(request, response);
				super.onAuthenticationSuccess(request, response, authentication);

				return;
			}
			clearAuthenticationAttributes(request);

			// Use the DefaultSavedRequest URL
			String targetUrl = savedRequest.getRedirectUrl();
			logger.debug("Redirecting to DefaultSavedRequest Url: " + targetUrl);
			getRedirectStrategy().sendRedirect(request, response, targetUrl);
			
		}else{
			clearAuthenticationAttributes(request);
		}
		
	}

	public void setRequestCache(RequestCache requestCache) {
		this.requestCache = requestCache;
	}
	public void setIsRedirectUrl(boolean isRedirectUrl){
		this.isRedirectUrl=isRedirectUrl;
	}
}
