package com.activiti.rest;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.activiti.model.runtime.TaskRepresentation;
import com.codahale.metrics.annotation.Timed;

@RestController
public class DefaultResource {
	
	@RequestMapping(value = "/rest", method = {RequestMethod.HEAD,RequestMethod.GET}, produces = "application/json")
    @Timed
    public TaskRepresentation getDate(HttpServletRequest request) {
		TaskRepresentation demo= new TaskRepresentation();
		 demo.setCreated(Calendar.getInstance().getTime());
		 return demo;
    }
}
