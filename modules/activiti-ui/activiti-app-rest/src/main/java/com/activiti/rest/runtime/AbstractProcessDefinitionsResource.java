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
package com.activiti.rest.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.activiti.domain.editor.AppDefinition;
import com.activiti.domain.editor.AppModelDefinition;
import com.activiti.domain.idm.User;
import com.activiti.domain.runtime.RuntimeAppDefinition;
import com.activiti.domain.runtime.RuntimeAppDeployment;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.model.editor.ModelRepresentation;
import com.activiti.model.runtime.ProcessDefinitionModelRepresentation;
import com.activiti.model.runtime.ProcessDefinitionRepresentation;
import com.activiti.security.SecurityUtils;
import com.activiti.service.api.RuntimeAppDefinitionService;
import com.activiti.service.exception.InternalServerErrorException;
import com.activiti.service.exception.NotPermittedException;
import com.activiti.service.runtime.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractProcessDefinitionsResource {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessDefinitionsResource.class);
	
	@Inject
	protected RepositoryService repositoryService;

	@Inject
	protected RuntimeAppDefinitionService runtimeAppDefinitionService;

	@Inject
	protected PermissionService permissionService;
	
	@Autowired
    protected ObjectMapper objectMapper;

	public ResultListDataRepresentation getProcessDefinitions(Boolean latest, Long appDefinitionId) {

		ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery();

		User currentUser = SecurityUtils.getCurrentUserObject();

		if (appDefinitionId != null) {
			List<RuntimeAppDeployment> appDeployments = runtimeAppDefinitionService
					.getRuntimeAppDeploymentsForAppId(appDefinitionId);
			if (CollectionUtils.isNotEmpty(appDeployments)) {
				RuntimeAppDeployment latestAppDeployment = null;
				for (RuntimeAppDeployment runtimeAppDeployment : appDeployments) {
					if (latestAppDeployment == null
							|| runtimeAppDeployment.getCreated().after(latestAppDeployment.getCreated())) {
						latestAppDeployment = runtimeAppDeployment;
					}
				}

				if (permissionService.hasReadPermissionOnRuntimeApp(currentUser, appDefinitionId)) {
					if (StringUtils.isNotEmpty(latestAppDeployment.getDeploymentId())) {
						definitionQuery.deploymentId(latestAppDeployment.getDeploymentId());
					} else {
						return new ResultListDataRepresentation();
					}
				} else {
					throw new NotPermittedException();
				}
			} else {
				return new ResultListDataRepresentation(new ArrayList<ProcessDefinitionRepresentation>());
			}

		} else {
			List<RuntimeAppDefinition> appDefinitions = runtimeAppDefinitionService.getDefinitionsForUser(currentUser);
			if (CollectionUtils.isNotEmpty(appDefinitions)) {
				Set<String> deploymentIds = new HashSet<String>();
				for (RuntimeAppDefinition runtimeAppDefinition : appDefinitions) {
					deploymentIds.add(runtimeAppDefinition.getDeploymentId());
				}
				// TODO: UI6 REFACTOR
				// definitionQuery.deploymentIds(deploymentIds);
			} else {
				// When the user doesn't have any apps, don't execute the query
				// and simply return an empty list
				return new ResultListDataRepresentation(new ArrayList<ProcessDefinitionRepresentation>());
			}

			if (latest != null && latest) {
				definitionQuery.latestVersion();
			}
		}

		List<ProcessDefinition> definitions = definitionQuery.list();
		ResultListDataRepresentation result = new ResultListDataRepresentation(convertDefinitionList(definitions));
		return result;
	}

	protected List<ProcessDefinitionRepresentation> convertDefinitionList(List<ProcessDefinition> definitions) {
		Map<String, Boolean> startFormMap = new HashMap<String, Boolean>();
		List<ProcessDefinitionRepresentation> result = new ArrayList<ProcessDefinitionRepresentation>();
		if (CollectionUtils.isNotEmpty(definitions)) {
			for (ProcessDefinition processDefinition : definitions) {
				if (startFormMap.containsKey(processDefinition.getId()) == false) {
					BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
					List<StartEvent> startEvents = bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class,
							false);
					boolean hasStartForm = false;
					for (StartEvent startEvent : startEvents) {
						if (StringUtils.isNotEmpty(startEvent.getFormKey())
								&& NumberUtils.isNumber(startEvent.getFormKey())) {
							hasStartForm = true;
							break;
						}
					}

					startFormMap.put(processDefinition.getId(), hasStartForm);
				}
				ProcessDefinitionRepresentation rep = new ProcessDefinitionRepresentation(processDefinition);
				rep.setHasStartForm(startFormMap.get(processDefinition.getId()));
				result.add(rep);
			}
		}
		return result;
	}

	public ResultListDataRepresentation getProcessDefinitionsModels(Long appDefinitionId) {

		ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery();
		User currentUser = SecurityUtils.getCurrentUserObject();
		List<RuntimeAppDeployment> appDeployments = runtimeAppDefinitionService
				.getRuntimeAppDeploymentsForAppId(appDefinitionId);
		RuntimeAppDeployment latestAppDeployment = null;
		if (CollectionUtils.isNotEmpty(appDeployments)) {			
			for (RuntimeAppDeployment runtimeAppDeployment : appDeployments) {
				if (latestAppDeployment == null
						|| runtimeAppDeployment.getCreated().after(latestAppDeployment.getCreated())) {
					latestAppDeployment = runtimeAppDeployment;
				}
			}
			if (permissionService.hasReadPermissionOnRuntimeApp(currentUser, appDefinitionId)) {
				if (StringUtils.isNotEmpty(latestAppDeployment.getDeploymentId())) {
					definitionQuery.deploymentId(latestAppDeployment.getDeploymentId());
				} else {
					return new ResultListDataRepresentation();
				}
			} else {
				throw new NotPermittedException();
			}
		} else {
			return new ResultListDataRepresentation(new ArrayList<ProcessDefinitionRepresentation>());
		}
		AppDefinition models= null;
        try {
        	models=this.objectMapper.readValue(latestAppDeployment.getDefinition(),AppDefinition.class);
        } catch (Exception e) {
            logger.error("Error while processing app definition json to Models: " + latestAppDeployment.getDefinition(), e);
            throw new InternalServerErrorException("Process definition could not be has models " + appDefinitionId);
        }
		List<ProcessDefinition> definitions = definitionQuery.list();
		ResultListDataRepresentation result = new ResultListDataRepresentation(convertModelDefinitionList(definitions,models.getModels()));
		return result;
	}
	protected List<ProcessDefinitionModelRepresentation> convertModelDefinitionList(List<ProcessDefinition> definitions,List<AppModelDefinition> models) {
		Map<String, Boolean> startFormMap = new HashMap<String, Boolean>();
		List<ProcessDefinitionModelRepresentation> result = new ArrayList<ProcessDefinitionModelRepresentation>();
		if (CollectionUtils.isNotEmpty(definitions)) {
			for (ProcessDefinition processDefinition : definitions) {
				if (startFormMap.containsKey(processDefinition.getId()) == false) {
					BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
					List<StartEvent> startEvents = bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class,
							false);
					boolean hasStartForm = false;
					for (StartEvent startEvent : startEvents) {
						if (StringUtils.isNotEmpty(startEvent.getFormKey())
								&& NumberUtils.isNumber(startEvent.getFormKey())) {
							hasStartForm = true;
							break;
						}
					}

					startFormMap.put(processDefinition.getId(), hasStartForm);
				}
				
				ProcessDefinitionModelRepresentation rep = new ProcessDefinitionModelRepresentation(processDefinition,null);
				
				for(AppModelDefinition model : models){					
					String key= model.getName().replace(" ","");
					if(rep.getKey().equalsIgnoreCase(key)){
						rep.setModelId(model.getId());
						break;
					}
				}				
				rep.setHasStartForm(startFormMap.get(processDefinition.getId()));
				result.add(rep);
			}
		}
		return result;
	}

}
