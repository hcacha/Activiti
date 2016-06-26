package com.activiti.model.runtime;

import org.activiti.engine.repository.ProcessDefinition;

public class ProcessDefinitionModelRepresentation extends ProcessDefinitionRepresentation {
	
	protected Long modelId;
	
	public ProcessDefinitionModelRepresentation(ProcessDefinition processDefinition,Long modelId ) {
        super(processDefinition);       
        this.modelId=modelId;
    }
	public Long getModelId() {
		return this.modelId;
	}
	public void setModelId(Long modelId) {
		this.modelId = modelId;
	}

}
