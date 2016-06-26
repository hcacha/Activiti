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
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.domain.idm.User;
import com.activiti.model.idm.LightUserRepresentation;
import com.activiti.model.runtime.TaskRepresentation;
import com.activiti.model.runtime.TaskUpdateRepresentation;
import com.activiti.rest.util.TaskUtil;
import com.activiti.security.SecurityUtils;
import com.activiti.service.api.UserCache;
import com.activiti.service.api.UserCache.CachedUser;
import com.activiti.service.exception.NotFoundException;
import com.activiti.service.runtime.PermissionService;

/**
 * @author jbarrez
 */
public abstract class AbstractTaskResource {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractTaskResource.class);

	@Inject
	protected TaskService taskService;
	
	@Inject
	protected HistoryService historyService;

	@Inject
	protected UserCache userCache;
	
	@Inject
	protected PermissionService permissionService;
	
	@Inject
	protected RepositoryService repositoryService;

    public TaskRepresentation getTask(String taskId, HttpServletResponse response) {
		User currentUser = SecurityUtils.getCurrentUserObject();
		HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(currentUser, taskId);
		
		ProcessDefinition processDefinition = null;
		if (StringUtils.isNotEmpty(task.getProcessDefinitionId())) {
			try {
				processDefinition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
			} catch (ActivitiException e) {
				logger.error("Error getting process definition " + task.getProcessDefinitionId(), e);
			}
			
			
		}

		TaskRepresentation rep = new TaskRepresentation(task, processDefinition);
		TaskUtil.fillPermissionInformation(rep, task, currentUser, historyService, repositoryService);

		// Populate the people
		populateAssignee(task, rep);
		rep.setInvolvedPeople(getInvolvedUsers(taskId));

		return rep;
	}

	protected void populateAssignee(TaskInfo task, TaskRepresentation rep) {
	    if (task.getAssignee() != null) {
        	CachedUser cachedUser = userCache.getUser(task.getAssignee());
        	if (cachedUser != null && cachedUser.getUser() != null) {
        		rep.setAssignee(new LightUserRepresentation(cachedUser.getUser()));
        	} 
        }
    }

	protected List<LightUserRepresentation> getInvolvedUsers(String taskId) {
		List<HistoricIdentityLink> idLinks = historyService.getHistoricIdentityLinksForTask(taskId);
		List<LightUserRepresentation> result = new ArrayList<LightUserRepresentation>(idLinks.size());

		for (HistoricIdentityLink link : idLinks) {
			// Only include users and non-assignee links
			if (link.getUserId() != null && !IdentityLinkType.ASSIGNEE.equals(link.getType())) {
				CachedUser cachedUser = userCache.getUser(link.getUserId());
				if (cachedUser != null && cachedUser.getUser() != null) {
					result.add(new LightUserRepresentation(cachedUser.getUser()));
				}
			}
		}
		return result;
	}

    public TaskRepresentation updateTask(String taskId, TaskUpdateRepresentation updated) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        
        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }
        
        permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), task.getId());
        
        if (updated.isNameSet()) {
            task.setName(updated.getName());
        }
        
        if (updated.isDescriptionSet()) {
            task.setDescription(updated.getDescription());
        }
        
        if (updated.isDueDateSet()) {
            task.setDueDate(updated.getDueDate());
        }
        
        taskService.saveTask(task);
        
        return new TaskRepresentation(task);
    }

}
