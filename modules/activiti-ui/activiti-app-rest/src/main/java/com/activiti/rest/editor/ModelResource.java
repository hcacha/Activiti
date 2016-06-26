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

import java.text.ParseException;
import java.util.Date;

import javax.inject.Inject;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.activiti.domain.editor.AbstractModel;
import com.activiti.domain.editor.Model;
import com.activiti.domain.editor.ModelShareInfo;
import com.activiti.domain.idm.User;
import com.activiti.model.editor.ModelRepresentation;
import com.activiti.repository.editor.ModelShareInfoRepository;
import com.activiti.security.SecurityUtils;
import com.activiti.service.editor.ModelInternalService;
import com.activiti.service.exception.BadRequestException;
import com.activiti.service.exception.ConflictingRequestException;
import com.activiti.service.exception.InternalServerErrorException;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class ModelResource extends AbstractModelResource {

    private static final Logger log = LoggerFactory.getLogger(ModelResource.class);
    
    private static final String RESOLVE_ACTION_OVERWRITE = "overwrite";
    private static final String RESOLVE_ACTION_SAVE_AS = "saveAs";
    private static final String RESOLVE_ACTION_NEW_VERSION = "newVersion";
	
	@Inject
	protected ModelInternalService modelService;
	
	@Inject
	protected ModelShareInfoRepository shareInfoRepository;
	
	protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
	
	protected BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
	
	protected ObjectMapper objectMapper = new ObjectMapper();
	
    
    /**
     * GET /rest/models/{modelId} -> Get process model
     */
    @RequestMapping(value = "/rest/models/{modelId}", method = RequestMethod.GET, produces = "application/json")
    @Timed
    public ModelRepresentation getModel(@PathVariable Long modelId, @RequestParam(required=false) Boolean includePermissions) {    
       return super.getModel(modelId, includePermissions);
    }
    
    /**
     * GET /rest/models/{modelId}/thumbnail -> Get process model thumbnail
     */
    @RequestMapping(value = "/rest/models/{modelId}/thumbnail", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @Timed
    public byte[] getModelThumbnail(@PathVariable Long modelId) {    
       return super.getModelThumbnail(modelId);
    }
    
    /**
     * PUT /rest/models/{modelId} -> update process model properties
     */
    @RequestMapping(value = "/rest/models/{modelId}", method = RequestMethod.PUT)
    @Timed
    public ModelRepresentation updateModel(@PathVariable Long modelId, @RequestBody ModelRepresentation updatedModel) {
        // Get model, write-permission required if not a favorite-update
        Model model = getModel(modelId, true, true);
        
        try {
            if (updatedModel.getName() != null) {
                updatedModel.updateModel(model);
                modelRepository.save(model);
            }
            ModelRepresentation result = new ModelRepresentation(model);
            populatePermissions(model, result);
            
            return result;
            
        } catch (Exception e) {
            throw new BadRequestException("Model cannot be updated: " + modelId);
        }
    }
    
    /**
     * DELETE /rest/models/{modelId} -> delete process model or, as a non-owner,
     * remove the share info link for that user specifically
     */
    @RequestMapping(value = "/rest/models/{modelId}", method = RequestMethod.DELETE)
    @Timed
    public void deleteModel(@PathVariable Long modelId, @RequestParam(required=false) Boolean cascade, @RequestParam(required=false) Boolean deleteRuntimeApp) {

        // Get model to check if it exists, read-permission required for delete (in case user is not owner, only share info
        // will be deleted
        Model model = getModel(modelId, true, false);
        
        try {
        	Long currentUserId = SecurityUtils.getCurrentUserId();
        	boolean currentUserIsOwner = currentUserId.equals(model.getCreatedBy().getId());
        	if (currentUserIsOwner) {
        		modelService.deleteModel(model.getId(), Boolean.TRUE.equals(cascade), Boolean.TRUE.equals(deleteRuntimeApp));
        	} else {
        		// If non-owner, simply delete the share info
        		ModelShareInfo shareInfo = shareInfoRepository
        				.findByModelIdAndUserId(modelId, SecurityUtils.getCurrentUserId());
        		shareInfoRepository.delete(shareInfo);
        	}
        	
        } catch (Exception e) {
            log.error("Error while deleting: ", e);
            throw new BadRequestException("Model cannot be deleted: " + modelId);
        }
    }
    
	/**
	 * GET /rest/models/{modelId}/editor/json -> get the JSON model
	 */
	@RequestMapping(value = "/rest/models/{modelId}/editor/json", method = RequestMethod.GET, produces = "application/json")
	@Timed
	public ObjectNode getModelJSON(@PathVariable Long modelId) {
	    Model model = getModel(modelId, true, true);
		ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put("modelId", model.getId());
        modelNode.put("name", model.getName());
        modelNode.put("description", model.getDescription());
        modelNode.putPOJO("lastUpdated", model.getLastUpdated());
        modelNode.put("lastUpdatedBy", model.getLastUpdatedBy().getFullName());
        if (StringUtils.isNotEmpty(model.getModelEditorJson())) {
            try {
                ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
                editorJsonNode.put("modelType", "model");
                modelNode.put("model", editorJsonNode);
            } catch (Exception e) {
                log.error("Error reading editor json " + modelId, e);
                throw new InternalServerErrorException("Error reading editor json " + modelId);
            }
            
        } else {
            ObjectNode editorJsonNode = objectMapper.createObjectNode();
            editorJsonNode.put("id", "canvas");
            editorJsonNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorJsonNode.put("modelType", "model");
            modelNode.put("model", editorJsonNode);
        }
        return modelNode;
	}

	/**
	 * POST /rest/models/{modelId}/editor/json -> save the JSON model
	 */
	@RequestMapping(value = "/rest/models/{modelId}/editor/json", method = RequestMethod.POST)
	@Timed
	public ModelRepresentation saveModel(@PathVariable Long modelId,
	        @RequestBody MultiValueMap<String, String> values) {
	    

		// Validation: see if there was another update in the meantime
		long lastUpdated = -1L;
		String lastUpdatedString = values.getFirst("lastUpdated");
		if (lastUpdatedString == null) {
			throw new BadRequestException("Missing lastUpdated date");
		}
		try {
		    Date readValue = objectMapper.getDeserializationConfig().getDateFormat().parse(lastUpdatedString);
		    lastUpdated = readValue.getTime();
		} catch (ParseException e) {
	        throw new BadRequestException("Invalid lastUpdated date: '" + lastUpdatedString + "'");
        }
		
		Model model = getModel(modelId, true, true);
		User currentUser = SecurityUtils.getCurrentUserObject();
		boolean currentUserIsOwner = model.getLastUpdatedBy().getId().equals(currentUser.getId());
		String resolveAction = values.getFirst("conflictResolveAction");
		
		// If timestamps differ, there is a conflict or a conflict has been resolved by the user
		if (model.getLastUpdated().getTime() != lastUpdated) {
			
			if (RESOLVE_ACTION_SAVE_AS.equals(resolveAction)) {
			    
				String saveAs = values.getFirst("saveAs");
				String json = values.getFirst("json_xml");
				return createNewModel(saveAs, model.getDescription(), model.getStencilSetId(), 
						model.getModelType(), json);
                
			} else if (RESOLVE_ACTION_OVERWRITE.equals(resolveAction)) {
				return updateModel(modelId, values, false);
			} else if (RESOLVE_ACTION_NEW_VERSION.equals(resolveAction)) {
				return updateModel(modelId, values, true);
			} else {
				
				// Exception case: the user is the owner and selected to create a new version
				String isNewVersionString = values.getFirst("newversion");
				if (currentUserIsOwner && "true".equals(isNewVersionString)) {
					return updateModel(modelId, values, true);
				} else {
					// Tried everything, this is really a conflict, return 409
					ConflictingRequestException exception = new ConflictingRequestException("Process model was updated in the meantime");
					exception.addCustomData("userFullName", model.getLastUpdatedBy().getFullName());
					exception.addCustomData("newVersionAllowed", currentUserIsOwner);
					throw exception;
				}
			
			}
			
		} else {
				
			// Actual, regular, update
			return updateModel(modelId, values, false);
			
		}
	}
	
	/**
     * POST /rest/models/{modelId}/editor/newversion -> create a new model version
     */
    @RequestMapping(value = "/rest/models/{modelId}/newversion", method = RequestMethod.POST)
    @Timed
    public ModelRepresentation importNewVersion(@PathVariable Long modelId, @RequestParam("file") MultipartFile file) {
        return super.importNewVersion(modelId, file);
    }

	protected ModelRepresentation updateModel(Long modelId, MultiValueMap<String, String> values, boolean forceNewVersion) {

	    String name = values.getFirst("name");
		String description = values.getFirst("description");
		String isNewVersionString = values.getFirst("newversion");
		String newVersionComment = null;
		
		boolean newVersion = false;
		if (forceNewVersion) {
			newVersion = true;
			newVersionComment = values.getFirst("comment");
		} else {
			if (isNewVersionString != null) {
				newVersion = "true".equals(isNewVersionString);
				newVersionComment = values.getFirst("comment");
			}
		}

		String json = values.getFirst("json_xml");

		try {
			AbstractModel model = modelService.saveModel(modelId, name, description, json,
			        newVersion, newVersionComment, SecurityUtils.getCurrentUserObject());
			return new ModelRepresentation(model);
		} catch (Exception e) {
			log.error("Error saving model " + modelId, e);
			throw new BadRequestException("Process model could not be saved " + modelId);
		}
	}
	
	protected ModelRepresentation createNewModel(String name, String description, Long stencilSetId, 
			Integer modelType, String editorJson) {
		
	    ModelRepresentation model = new ModelRepresentation();
        model.setName(name);
        model.setDescription(description);
        model.setModelType(modelType);
		Model newModel = modelService.createModel(model, editorJson, SecurityUtils.getCurrentUserObject());
		return new ModelRepresentation(newModel);
	}
}
