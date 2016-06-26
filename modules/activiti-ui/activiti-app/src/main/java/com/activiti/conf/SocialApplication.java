package com.activiti.conf;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.activiti.security.CustomAccessTokenProviderChain;
import com.activiti.security.CustomUserInfoTokenServices;
import com.activiti.security.IntrospectionEndpointTokenProvider;
import com.activiti.security.IntrospectionResourceDetails;
import com.activiti.security.filter.AutentificationSecurityRequestMatcher;
import com.activiti.security.filter.CustomOAuth2ClientAuthenticationProcessingFilter;


//@Configuration
//@EnableConfigurationProperties
//@RestController
//@Order(10)
//@EnableOAuth2Client
public class SocialApplication 
//extends WebSecurityConfigurerAdapter 
{


//	@Autowired
//	OAuth2ClientContext oauth2ClientContext;
//
//
//	@RequestMapping({ "/user", "/me" })
//	public Map<String, String> user(Principal principal) {
//		Map<String, String> map = new LinkedHashMap<>();
//		map.put("name", principal.getName());
//		return map;
//	}
//
//	@Override
//	protected void configure(HttpSecurity http) throws Exception {
//		// @formatter:off	
//		http.antMatcher("/**")
//			.authorizeRequests()
//				.antMatchers("/", "/login**", "/webjars/**").permitAll()
//				.anyRequest().authenticated()
//			.and().exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/"))
//			.and().logout().logoutSuccessUrl("/").permitAll()
//			.and().csrf().csrfTokenRepository(csrfTokenRepository())
//			.and().addFilterAfter(csrfHeaderFilter(), CsrfFilter.class)
//			.addFilterBefore(new CORSFilter(), ChannelProcessingFilter.class)
//			.addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
//		// @formatter:on
//	}	
//	@Bean
//	public FilterRegistrationBean oauth2ClientFilterRegistration(
//			OAuth2ClientContextFilter filter) {
//		FilterRegistrationBean registration = new FilterRegistrationBean();
//		registration.setFilter(filter);
//		registration.setOrder(-100);
//		return registration;
//	}
//	private Filter ssoFilter() {		
//		CustomOAuth2ClientAuthenticationProcessingFilter facebookFilter = 
//				new CustomOAuth2ClientAuthenticationProcessingFilter(new AutentificationSecurityRequestMatcher("/login/indentity"));
//		
//		IntrospectionResourceDetails introspectionResourceDetails=identity();
//		introspectionResourceDetails.setOAuth2ProtectedResourceDetails(identityAuthorizationCode());
//		
//		OAuth2RestTemplate identityTemplate = new OAuth2RestTemplate(introspectionResourceDetails, oauth2ClientContext);
//		identityTemplate.setAccessTokenProvider(new CustomAccessTokenProviderChain(Arrays.<AccessTokenProvider> asList(
//				new AuthorizationCodeAccessTokenProvider(), new ImplicitAccessTokenProvider(),
//				new ResourceOwnerPasswordAccessTokenProvider(), 
//				new ClientCredentialsAccessTokenProvider(),
//				new IntrospectionEndpointTokenProvider())));
//		
//		facebookFilter.setRestTemplate(identityTemplate);
//		facebookFilter.setTokenServices(new CustomUserInfoTokenServices(identityResource().getUserInfoUri(), introspectionResourceDetails.getClientId()));
//		return facebookFilter;
//	}
//	@Bean
//	@ConfigurationProperties(prefix="identity.client",locations="classpath:application.yml")
//	IntrospectionResourceDetails identity() {		
//		return new IntrospectionResourceDetails();
//	}
//	@Bean
//	@ConfigurationProperties(prefix="identity.client",locations="classpath:application.yml")
//	OAuth2ProtectedResourceDetails identityAuthorizationCode() {		
//		return new AuthorizationCodeResourceDetails();
//	}
//	@Bean
//	@ConfigurationProperties(prefix="identity.resource",locations="classpath:application.yml")
//	ResourceServerProperties identityResource() {
//		return new ResourceServerProperties();		
//	}
//	private Filter csrfHeaderFilter() {
//		return new OncePerRequestFilter() {
//			@Override
//			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//					FilterChain filterChain) throws ServletException, IOException {
//				CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
//				if (csrf != null) {
//					Cookie cookie = WebUtils.getCookie(request, "XSRF-TOKEN");
//					String token = csrf.getToken();
//					if (cookie == null || token != null && !token.equals(cookie.getValue())) {
//						cookie = new Cookie("XSRF-TOKEN", token);
//						cookie.setPath("/");
//						response.addCookie(cookie);
//					}
//				}
//				filterChain.doFilter(request, response);
//			}
//		};
//	}
//	private CsrfTokenRepository csrfTokenRepository() {
//		HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
//		repository.setHeaderName("X-XSRF-TOKEN");
//		return repository;
//	}


}
