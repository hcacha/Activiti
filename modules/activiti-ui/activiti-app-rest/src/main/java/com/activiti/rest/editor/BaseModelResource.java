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
package com.activiti.rest.editor;

import java.util.List;

import javax.inject.Inject;

import org.activiti.editor.language.json.converter.util.CollectionUtils;

import com.activiti.domain.editor.Model;
import com.activiti.domain.editor.ModelHistory;
import com.activiti.domain.editor.ModelShareInfo;
import com.activiti.domain.editor.SharePermission;
import com.activiti.domain.idm.User;
import com.activiti.model.editor.ModelRepresentation;
import com.activiti.repository.editor.ModelHistoryRepository;
import com.activiti.repository.editor.ModelRepository;
import com.activiti.repository.editor.ModelShareInfoRepository;
import com.activiti.security.SecurityUtils;
import com.activiti.service.exception.NotFoundException;
import com.activiti.service.exception.NotPermittedException;
import com.activiti.util.UserUtil;

/**
 * @author Frederik Heremans
 */
public class BaseModelResource {

    protected static final String PROCESS_NOT_FOUND_MESSAGE_KEY = "PROCESS.ERROR.NOT-FOUND";

    @Inject
    protected ModelRepository modelRepository;
    
    @Inject
    protected ModelHistoryRepository historyRepository;
    
    @Inject
    protected ModelShareInfoRepository shareInfoRepository;
    
    protected Model getModel(Long modelId, boolean checkRead, boolean checkEdit) {
        Model model = modelRepository.findOne(modelId);
        
        if (model == null) {
            NotFoundException modelNotFound = new NotFoundException("No model found with the given id: " + modelId);
            modelNotFound.setMessageKey(PROCESS_NOT_FOUND_MESSAGE_KEY);
            throw modelNotFound;
        }
        
        User currentUser = SecurityUtils.getCurrentUserObject();

        // Owner can always read/write a model
        boolean isOwner = model.getCreatedBy().equals(currentUser);
        
        if (!isOwner) {
            
            Model validateModel = null;
            // check parent model if there is a model reference
            if (model.getReferenceId() != null) {
                validateModel = getParentModel(model.getReferenceId());
            } else {
                validateModel = model;
            }
            
            // Check if model is shared with the current user
        	
        	List<ModelShareInfo> shareInfoList = null; 
        	List<Long> groupIds = UserUtil.getGroupIds(currentUser);
        	if (CollectionUtils.isNotEmpty(groupIds)) {
        		shareInfoList = shareInfoRepository.findByModelIdWithUserIdOrGroups(validateModel.getId(), currentUser.getId(), groupIds);
        	} else {
        		shareInfoList = shareInfoRepository.findByModelIdWithUserId(validateModel.getId(), currentUser.getId());
        	}
            
            if (checkRead) {
                if (CollectionUtils.isEmpty(shareInfoList)) {
                    throw new NotPermittedException("You are not permitted to access this process model");
                }
            }
            
            if (checkEdit) {
                if (CollectionUtils.isEmpty(shareInfoList)) {
                    throw new NotPermittedException("You are not permitted to access this process model");
                } else {
                    boolean hasWritePermission = false;
                    for (ModelShareInfo modelShareInfo : shareInfoList) {
                        if (modelShareInfo.getPermission() == SharePermission.WRITE) {
                            hasWritePermission = true;
                            break;
                        }
                    }
                    
                    if (hasWritePermission == false) {
                        throw new NotPermittedException("You are not permitted to modify this process model");
                    }
                }
            }
        }
        
        return model;
    }
    
    protected Model getProcessModelForOwner(Long processModelId) {
        Model model = modelRepository.findOne(processModelId);
        
        if(model == null) {
            throw new NotFoundException("No process model found with the given id: " + processModelId);
        }

        User currentUser = SecurityUtils.getCurrentUserObject();
        
        if (!model.getCreatedBy().equals(currentUser)) {
            throw new NotPermittedException("You are not permitted to access this process model");
        }
        
        return model;
    }
    
    protected ModelHistory getModelHistory(Long modelId, Long modelHistoryId, boolean checkRead, boolean checkEdit) {
        // Check if the user has read-rights on the process-model in order to fetch history
        Model model = getModel(modelId, checkRead, checkEdit);
        ModelHistory modelHistory = historyRepository.findOne(modelHistoryId);
        
        // Check if history corresponds to the current model and is not deleted
        if (modelHistory== null || modelHistory.getRemovalDate() != null || !modelHistory.getModelId().equals(model.getId())) {
            throw new NotFoundException("Process model history not found: " + modelHistoryId);
        }
        return modelHistory;
    }
    
    protected void populatePermissions(Model model, ModelRepresentation response) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        if(model.getCreatedBy().equals(currentUser)) {
            response.setSharePermission(SharePermission.WRITE);
        } else {
        	
        	List<ModelShareInfo> shareInfoList = null;
        	List<Long> groupIds = UserUtil.getGroupIds(currentUser);
        	if (CollectionUtils.isNotEmpty(groupIds)) {
        		shareInfoList = shareInfoRepository.findByModelIdWithUserIdOrGroups(model.getId(), currentUser.getId(), groupIds);
        	} else {
        		shareInfoList = shareInfoRepository.findByModelIdWithUserId(model.getId(), currentUser.getId());
        	}
        	
            if (CollectionUtils.isNotEmpty(shareInfoList)) {
                for (ModelShareInfo modelShareInfo : shareInfoList) {
                    response.setSharePermission(modelShareInfo.getPermission());
                    if (modelShareInfo.getPermission() == SharePermission.WRITE) {
                        break;
                    }
                }
            }
            
        }
    }
    
    protected Model getParentModel(Long parentModelId) {
        Model model = modelRepository.findOne(parentModelId);
        if (model.getReferenceId() != null) {
            return getParentModel(model.getReferenceId());
        }
        return model;
    }
    
}
