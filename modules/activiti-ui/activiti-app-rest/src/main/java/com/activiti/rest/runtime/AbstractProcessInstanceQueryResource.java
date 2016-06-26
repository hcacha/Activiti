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
import java.util.List;

import javax.inject.Inject;

import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.apache.commons.lang3.StringUtils;

import com.activiti.domain.idm.User;
import com.activiti.domain.runtime.RuntimeAppDeployment;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.model.idm.LightUserRepresentation;
import com.activiti.model.runtime.ProcessInstanceRepresentation;
import com.activiti.security.SecurityUtils;
import com.activiti.service.api.RuntimeAppDefinitionService;
import com.activiti.service.api.UserCache;
import com.activiti.service.api.UserCache.CachedUser;
import com.activiti.service.exception.BadRequestException;
import com.activiti.service.exception.NotPermittedException;
import com.activiti.service.runtime.PermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractProcessInstanceQueryResource {
    
    private static final int DEFAULT_PAGE_SIZE = 25;

    @Inject
    protected RepositoryService repositoryService;
    
    @Inject
    protected HistoryService historyService;
    
    @Inject
    protected PermissionService permissionService;
    
    @Inject
    protected UserCache userCache;
    
    @Inject
    protected RuntimeAppDefinitionService runtimeAppDefinitionService;
    
    public ResultListDataRepresentation getProcessInstances(ObjectNode requestNode) {
		
		HistoricProcessInstanceQuery instanceQuery = historyService.createHistoricProcessInstanceQuery();
		
		User currentUser = SecurityUtils.getCurrentUserObject();
		instanceQuery.involvedUser(String.valueOf(currentUser.getId()));
		
		// Process definition
        JsonNode processDefinitionIdNode = requestNode.get("processDefinitionId");
        if (processDefinitionIdNode != null && processDefinitionIdNode.isNull() == false) {
            instanceQuery.processDefinitionId(processDefinitionIdNode.asText());
        }
        
        JsonNode appDefinitionIdNode = requestNode.get("appDefinitionId");
        if (appDefinitionIdNode != null && appDefinitionIdNode.isNull() == false) {
            // Results need to be filtered in an app-context. We need to fetch the deployment id for this app and use that in the query
            Long id = appDefinitionIdNode.asLong();
            List<RuntimeAppDeployment> appDeployments = runtimeAppDefinitionService.getRuntimeAppDeploymentsForAppId(id);
            if (CollectionUtils.isEmpty(appDeployments)) {
                throw new BadRequestException("No app deployments exists with id: " + id);
            }
            
            if (!permissionService.hasReadPermissionOnRuntimeApp(SecurityUtils.getCurrentUserObject(), id)) {
                throw new NotPermittedException("You are not allowed to use app definition with id: " + id);
            }
            
            List<String> deploymentIds = new ArrayList<String>();
            for (RuntimeAppDeployment appDeployment : appDeployments) {
                if (StringUtils.isNotEmpty(appDeployment.getDeploymentId())) {
                    deploymentIds.add(appDeployment.getDeploymentId());
                }
            }
            
            instanceQuery.deploymentIdIn(deploymentIds);
        }
        
        // State filtering
        JsonNode stateNode = requestNode.get("state");
        if(stateNode != null && !stateNode.isNull()) {
            String state = stateNode.asText();
            if("running".equals(state) ) {
                instanceQuery.unfinished();
            } else if("completed".equals(state)) {
                instanceQuery.finished();
            } else if(!"all".equals(state)) {
                throw new BadRequestException("Illegal state filter value passed, only 'running', 'completed' or 'all' are supported");
            }
        } else {
            // Default filtering, only running
            instanceQuery.unfinished();
        }
        
        // Sort and ordering
        JsonNode sortNode = requestNode.get("sort");
        if(sortNode != null && !sortNode.isNull()) {
        	
        	if ("created-desc".equals(sortNode.asText())) {
        		instanceQuery.orderByProcessInstanceStartTime().desc();
        	} else if ("created-asc".equals(sortNode.asText())) {
        		instanceQuery.orderByProcessInstanceStartTime().asc();
        	} else if ("ended-desc".equals(sortNode.asText())) {
        		instanceQuery.orderByProcessInstanceEndTime().desc();
        	} else if ("ended-asc".equals(sortNode.asText())) {
        		instanceQuery.orderByProcessInstanceEndTime().asc();
        	}
        	
        } else {
            // Revert to default
            instanceQuery.orderByProcessInstanceStartTime().desc();
        }
        
        int page = 0;
        JsonNode pageNode = requestNode.get("page");
        if(pageNode != null && !pageNode.isNull()) {
            page = pageNode.asInt(0);
        }
        
        int size = DEFAULT_PAGE_SIZE;
        JsonNode sizeNode = requestNode.get("size");
        if(sizeNode != null && !sizeNode.isNull()) {
            size = sizeNode.asInt(DEFAULT_PAGE_SIZE);
        }
        
        List<HistoricProcessInstance> instances = instanceQuery.listPage(page * size, size);
        ResultListDataRepresentation result = new ResultListDataRepresentation(convertInstanceList(instances));
        
        // In case we're not on the first page and the size exceeds the page size, we need to do an additional count for the total
        if(page != 0 || instances.size() == size) {
            Long totalCount = instanceQuery.count();
            result.setTotal(totalCount.intValue());
            result.setStart(page * size);
        }
        return result;
    }
	
	protected List<ProcessInstanceRepresentation> convertInstanceList(List<HistoricProcessInstance> instances) {
        List<ProcessInstanceRepresentation> result = new ArrayList<ProcessInstanceRepresentation>();
        if (CollectionUtils.isNotEmpty(instances)) {
            
            for (HistoricProcessInstance processInstance : instances) {
                LightUserRepresentation userRep = null;
                if(processInstance.getStartUserId() != null) {
                    CachedUser user = userCache.getUser(Long.parseLong(processInstance.getStartUserId()));
                    if(user != null && user.getUser() != null) {
                        userRep = new LightUserRepresentation(user.getUser());
                    }
                }
                
            	ProcessDefinitionEntity procDef = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()); 
                ProcessInstanceRepresentation instanceRepresentation = new ProcessInstanceRepresentation(
                        processInstance, procDef, procDef.isGraphicalNotationDefined(), userRep);
                result.add(instanceRepresentation);
            }
            
        }
        return result;
    }
}
