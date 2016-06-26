package com.activiti;


import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.activiti.conf.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


@PropertySources({

		@PropertySource("classpath:/META-INF/activiti-app/activiti-app.properties"),
		@PropertySource(value = "classpath:activiti-app.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "file:activiti-app.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "classpath:application.yml", ignoreResourceNotFound = true) })
@SpringBootApplication(scanBasePackages = { "com.activiti.conf", "com.activiti.repository", "com.activiti.service",
		"com.activiti.security", "com.activiti.model.component", "com.activiti.rest" }, exclude = {
				// DataSourceAutoConfiguration.class
				JpaRepositoriesAutoConfiguration.class })
//@EnableWebMvc
@EnableAsync
// @EnableMetrics(proxyTargetClass = true)
public class SpringBootRestApplication {
	// extends SpringBootServletInitializer
	// extends SpringBootServletInitializer
	private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

	@Inject
	private Environment environment;


	public static void main(String[] args) {
		SpringApplication.run(SpringBootRestApplication.class, args);
	}

	/**
	 * This is needed to make property resolving work on annotations ... (see
	 * http://stackoverflow.com/questions/11925952/custom-spring-property-source
	 * -does-not-resolve-placeholders-in-value)
	 * 
	 * @Scheduled(cron="${someProperty}")
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	// @Bean
	// public WebMvcConfigurer corsConfigurer() {
	// return new WebMvcConfigurerAdapter() {
	// @Override
	// public void addCorsMappings(CorsRegistry registry) {
	// registry.addMapping("/greeting-javaconfig").allowedOrigins("http://localhost:9000");
	// }
	// };
	// }

	// @Bean
	// protected ServletContextListener listener() {
	// return new WebConfigurer();
	// }
	// @Bean
	// public ServletRegistrationBean dispatcherServlet() {
	// log.debug("Configuring Spring Web application context");
	// DispatcherServlet dispatcherServlet = new DispatcherServlet();
	// AnnotationConfigWebApplicationContext applicationContext = new
	// AnnotationConfigWebApplicationContext();
	// applicationContext.register(AppDispatcherServletConfiguration.class);
	//
	// dispatcherServlet.setApplicationContext(applicationContext);
	//
	// log.debug("Registering Spring MVC Servlet");
	//
	// ServletRegistrationBean servletRegistrationBean = new
	// ServletRegistrationBean(dispatcherServlet, "/app/*");
	// servletRegistrationBean.setName("app");
	// servletRegistrationBean.setLoadOnStartup(1);
	// servletRegistrationBean.setAsyncSupported(true);
	// return servletRegistrationBean;
	// }

	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

//	@Bean
//	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
//		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
//		//ObjectMapper objectMapper = new ObjectMapper();
//		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//		jsonConverter.setObjectMapper(objectMapper);
//		return jsonConverter;
//	}

	@Bean
	public SessionLocaleResolver localeResolver() {
		return new SessionLocaleResolver();
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		log.debug("Configuring localeChangeInterceptor");
		LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
		localeChangeInterceptor.setParamName("language");
		return localeChangeInterceptor;
	}

	@Bean
	public MultipartResolver multipartResolver() {
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(environment.getProperty("file.upload.max.size", Long.class));
		return multipartResolver;
	}

	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		log.debug("Creating requestMappingHandlerMapping");
		RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
		requestMappingHandlerMapping.setUseSuffixPatternMatch(false);
		requestMappingHandlerMapping.setRemoveSemicolonContent(false);
		Object[] interceptors = { localeChangeInterceptor() };
		requestMappingHandlerMapping.setInterceptors(interceptors);
		return requestMappingHandlerMapping;
	}
//	@Bean
//	public WebMvcConfigurer corsConfigurer() {
//	    return new WebMvcConfigurerAdapter() {
//	      @Override
//	      public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//	          extendMessageConverters(converters);
//	          for (HttpMessageConverter<?> converter: converters) {
//	              if (converter instanceof MappingJackson2HttpMessageConverter) {
//	                  MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = (MappingJackson2HttpMessageConverter) converter;
//	                  jackson2HttpMessageConverter.setObjectMapper(objectMapper);
//	                  break;
//	              }
//	          }
//	      }
//	    };
//	}
	// @Bean
	// public Boolean disableSSLValidation() throws Exception {
	// final SSLContext sslContext = SSLContext.getInstance("TLS");
	//
	// sslContext.init(null, new TrustManager[]{new X509TrustManager() {
	// @Override
	// public void checkClientTrusted(X509Certificate[] x509Certificates, String
	// s) throws CertificateException {
	// }
	//
	// @Override
	// public void checkServerTrusted(X509Certificate[] x509Certificates, String
	// s) throws CertificateException {
	// }
	//
	// @Override
	// public X509Certificate[] getAcceptedIssuers() {
	// return new X509Certificate[0];
	// }
	// }}, null);
	//
	// HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
	// HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
	// public boolean verify(String hostname, SSLSession session) {
	// return true;
	// }
	// });
	// return true;
	// }
}
