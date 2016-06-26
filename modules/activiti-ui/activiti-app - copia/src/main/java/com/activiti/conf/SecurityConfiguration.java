/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.activiti.conf;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.activiti.security.AjaxAuthenticationFailureHandler;
import com.activiti.security.AjaxAuthenticationSuccessHandler;
import com.activiti.security.AjaxLogoutSuccessHandler;
import com.activiti.security.CustomClientOAuthDetails;
import com.activiti.security.CustomClientResourceDetails;
import com.activiti.security.CustomDaoAuthenticationProvider;
import com.activiti.security.CustomPersistentRememberMeServices;
import com.activiti.security.GoogleTokenServices;
import com.activiti.security.Http401UnauthorizedEntryPoint;
import com.activiti.security.jwt.FacebookSuccessHandler;
import com.activiti.security.jwt.StatelessJwtAuthenticationFilter;
import com.activiti.web.CustomFormLoginConfig;


/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 * 
 * @author Joram Barrez
 */
@Configuration
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);
	
	public static final String KEY_LDAP_ENABLED = "ldap.authentication.enabled";

    //
	// GLOBAL CONFIG
	//

	@Autowired
	private Environment env;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) {

		// Default auth (database backed)
		try {
			auth.authenticationProvider(dbAuthenticationProvider());
		} catch (Exception e) {
			logger.error("Could not configure authentication mechanism:", e);
		}
	}

	@Bean
	public UserDetailsService userDetailsService() {
		com.activiti.security.UserDetailsService userDetailsService = new com.activiti.security.UserDetailsService();

		// Undocumented setting to configure the amount of time user data is cached before a new check for validity is made
		// Use <= 0 for always do a check
		userDetailsService.setUserValidityPeriod(env.getProperty("cache.users.recheck.period", Long.class, 30000L));

		return userDetailsService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new StandardPasswordEncoder();
	}
	
	@Bean(name = "dbAuthenticationProvider")
	public AuthenticationProvider dbAuthenticationProvider() {
		CustomDaoAuthenticationProvider daoAuthenticationProvider = new CustomDaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(userDetailsService());
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
		return daoAuthenticationProvider;
	}
	
	//
	// REGULAR WEBAP CONFIG
	//
	
//	@Configuration
//	@Order(10) // API config first (has Order(1))
//    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
//
//		private static final Logger logger = LoggerFactory.getLogger(FormLoginWebSecurityConfigurerAdapter.class);
//		
//	    @Inject
//	    private Environment env;
//
//	    @Inject
//	    private AjaxAuthenticationSuccessHandler ajaxAuthenticationSuccessHandler;
//
//	    @Inject
//	    private AjaxAuthenticationFailureHandler ajaxAuthenticationFailureHandler;
//
//	    @Inject
//	    private AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;
//	    
//	    @Inject
//	    private Http401UnauthorizedEntryPoint authenticationEntryPoint;
//	    
//	    @Override
//	    protected void configure(HttpSecurity http) throws Exception {
//	        http
//	            .exceptionHandling()
//	                .authenticationEntryPoint(authenticationEntryPoint) 
//	                .and()
//	            .sessionManagement()
//	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//	                .and()
//	            .rememberMe()
//	                .rememberMeServices(rememberMeServices())
//	                .key(env.getProperty("security.rememberme.key"))
//	                .and()
//	            .logout()
//	                .logoutUrl("/app/logout")
//	                .logoutSuccessHandler(ajaxLogoutSuccessHandler)
//	                .deleteCookies("JSESSIONID")
//	                .permitAll()
//	                .and()
//	            .csrf()
//	                .disable() // Disabled, cause enabling it will cause sessions
//	            .headers()
//	                .frameOptions()
//	                	.sameOrigin()
//	                	.addHeaderWriter(new XXssProtectionHeaderWriter())
//	                .and()
//	            .authorizeRequests()
//	                .antMatchers("/*").permitAll()
//	                .antMatchers("/app/rest/authenticate").permitAll()
//                    .antMatchers("/app/rest/integration/login").permitAll()
//                    .antMatchers("/app/rest/temporary/example-options").permitAll()
//	                .antMatchers("/app/rest/idm/email-actions/*").permitAll()
//	                .antMatchers("/app/rest/idm/signups").permitAll()
//	                .antMatchers("/app/rest/idm/passwords").permitAll()
//	                .antMatchers("/app/**").authenticated();
//
//	        // Custom login form configurer to allow for non-standard HTTP-methods (eg. LOCK)
//	        CustomFormLoginConfig<HttpSecurity> loginConfig = new CustomFormLoginConfig<HttpSecurity>();
//	        loginConfig.loginProcessingUrl("/app/authentication")
//	            .successHandler(ajaxAuthenticationSuccessHandler)
//	            .failureHandler(ajaxAuthenticationFailureHandler)
//	            .usernameParameter("j_username")
//	            .passwordParameter("j_password")
//	            .permitAll();
//	        
//	        http.apply(loginConfig);
//	    }
//	    
//
//	    @Bean
//	    public RememberMeServices rememberMeServices() {
//            return new CustomPersistentRememberMeServices(env, userDetailsService());
//	    }
//	    
//	    @Bean
//	    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider() {
//	        return new RememberMeAuthenticationProvider(env.getProperty("security.rememberme.key"));
//	    }
//	    
//	    
//    }
	
//	@Configuration
//	@EnableOAuth2Client
//	@Order(10) // API config first (has Order(1))
//	@EnableAuthorizationServer
//	public static class OAuth2ClientConfiguration extends WebSecurityConfigurerAdapter {		
//
//		@Autowired
//		private OAuth2ClientContext oAuth2ClientContext;
//		
//		@Override
//	    protected void configure(HttpSecurity httpSecurity) throws Exception {
//
//	        httpSecurity
//	                //.csrf().disable()
//	                .sessionManagement()
//	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//	                .enableSessionUrlRewriting(false)
//	                .and()
//	                .antMatcher("/**").authorizeRequests()
//	                .antMatchers("/login/**").permitAll()
//	                .anyRequest().authenticated().and()
//	                .exceptionHandling().authenticationEntryPoint(new Http403ForbiddenEntryPoint()).and()
//	                //.addFilterBefore(new CORSFilter(), ChannelProcessingFilter.class)
//	                //.addFilterBefore(statelessJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
//	                .addFilterBefore(createSsoFilter(customClientOAuthDetails, customClientResourceDetails,facebookSuccessHandler(),
//	                		"/login/facebook"),
//	                		BasicAuthenticationFilter.class);
//	    }
//		
//		private OAuth2ClientAuthenticationProcessingFilter createSsoFilter(CustomClientOAuthDetails customClientOAuthDetails,
//				CustomClientResourceDetails customClientResourceDetails, 
//				AuthenticationSuccessHandler successHandler,
//				String path) {
//	        OAuth2ClientAuthenticationProcessingFilter ssoFilter = new OAuth2ClientAuthenticationProcessingFilter(path);
//	        //ssoFilter.setAllowSessionCreation(false);
//	        
//	        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(customClientOAuthDetails, oAuth2ClientContext);
//	        ssoFilter.setRestTemplate(restTemplate);
//	        //ssoFilter.setTokenServices(new UserInfoTokenServices(customClientResourceDetails.getUserInfoUri(),customClientOAuthDetails.getClientId()));
//	        ssoFilter.setTokenServices(new GoogleTokenServices(customClientResourceDetails.getUserInfoUri(),customClientOAuthDetails.getClientId()));
//	        ssoFilter.setAuthenticationSuccessHandler(successHandler);
//	        return ssoFilter;
//	    }
//		
//		@Bean
//		 protected StatelessJwtAuthenticationFilter statelessJwtAuthenticationFilter() {
//			return new StatelessJwtAuthenticationFilter();
//		 }
//		@Bean // handles the redirect to facebook
//	    public FilterRegistrationBean oAuth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
//	        FilterRegistrationBean registration = new FilterRegistrationBean();
//	        registration.setFilter(filter);
//	        registration.setOrder(-100);
//	        return registration;
//	    }
//		@Bean
//	    protected AuthenticationSuccessHandler facebookSuccessHandler() {
//	        return new FacebookSuccessHandler();
//	    }
////		
//		@Autowired
//	    private CustomClientOAuthDetails customClientOAuthDetails;
//		
//		@Autowired
//	    private CustomClientResourceDetails customClientResourceDetails;
//	
//	}		
	public static class LdapAuthenticationEnabledCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getEnvironment().getProperty(KEY_LDAP_ENABLED, Boolean.class, false);
		}

	}

}
class ClientResourceDetails {
    private OAuth2ProtectedResourceDetails client=new AuthorizationCodeResourceDetails();	        
    private ResourceServerProperties resource = new ResourceServerProperties();
    
//    public ClientResourceDetails(){
//    	client = new AuthorizationCodeResourceDetails();
//    	((BaseOAuth2ProtectedResourceDetails) client).setClientId(env.getProperty("facebook.client.clientId"));
//    	((BaseOAuth2ProtectedResourceDetails) client).setClientSecret(env.getProperty("facebook.client.clientSecret"));
//    	((BaseOAuth2ProtectedResourceDetails) client).setAccessTokenUri(env.getProperty("facebook.client.accessTokenUri"));
//    	(setTokenName(env.getPropert(BaseOAuth2ProtectedResourceDetails) clienty("facebook.client.oauth_token"));
//    }

    public OAuth2ProtectedResourceDetails getClient() {
        return client;
    }
    public ResourceServerProperties getResource() {
        return resource;
    }	    
}


