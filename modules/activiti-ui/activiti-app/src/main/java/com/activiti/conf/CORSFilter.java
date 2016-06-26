package com.activiti.conf;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//@Component
public class CORSFilter implements Filter{
    static Logger logger = LoggerFactory.getLogger(CORSFilter.class);
    
 // This is to be replaced with a list of domains allowed to access the server
 	private final List<String> allowedOrigins = Arrays.asList("http://localhost:63911", "http://127.0.0.1:63911");

 	public void destroy() {
 	}

 	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

 		// Lets make sure that we are working with HTTP (that is, against HttpServletRequest and HttpServletResponse objects)
 		if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
 			HttpServletRequest request = (HttpServletRequest) req;
 			HttpServletResponse response = (HttpServletResponse) res;

 			// Access-Control-Allow-Origin
 			String origin = request.getHeader("Origin");
 			response.setHeader("Access-Control-Allow-Origin", allowedOrigins.contains(origin) ? origin : "");
 			response.setHeader("Vary", "Origin");

 			// Access-Control-Max-Age
 			response.setHeader("Access-Control-Max-Age", "3600");

 			// Access-Control-Allow-Credentials
 			response.setHeader("Access-Control-Allow-Credentials", "true");

 			// Access-Control-Allow-Methods
 			response.setHeader("Access-Control-Allow-Methods", "PUT, HEAD, POST, GET, OPTIONS, DELETE");

 			// Access-Control-Allow-Headers
 			response.setHeader("Access-Control-Allow-Headers",
 				"Origin, X-Requested-With, Content-Type, Accept,X-XSRF-TOKEN,X-CSRF-TOKEN,Authorization");
 			
 			if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
 		        response.setStatus(HttpServletResponse.SC_OK);
 		    } else {
 		        chain.doFilter(req, res);
 		    }
 		} 		
 	} 

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
//
//    @Override
//    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
//    	HttpServletRequest request = (HttpServletRequest) req;
//        HttpServletResponse response = (HttpServletResponse) res;
//        response.setHeader("Access-Control-Allow-Origin", "*");
//        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
//        response.setHeader("Access-Control-Max-Age", "3600");
//        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Range, Content-Disposition, Content-Type, Authorization, X-CSRF-TOKEN");        
//        
//        
//        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
//            response.setStatus(HttpServletResponse.SC_OK);
//        } else {
//            chain.doFilter(req, res);
//        }    
//        //chain.doFilter(request, response);
//    }
//
//    public void destroy() {}
}
