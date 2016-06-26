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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.activiti.security.CustomDaoAuthenticationProvider;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 * 
 * @author Joram Barrez
 */
@Configuration
@Order(9)
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

	@Bean(name = "userDetailsService")
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



