package com.activiti.model.runtime;

import org.activiti.bpmn.model.FixedValueProperty;
import com.activiti.model.common.AbstractRepresentation;

public class FixedValueRepresentation extends AbstractRepresentation{
	protected String id;
    protected String name;
    protected String type;
    protected String value;
    protected String valueId;
    
    
    public FixedValueRepresentation(FixedValueProperty  fixedValueProperty) {
        this.id=fixedValueProperty.getId();
        this.name=fixedValueProperty.getName();
        this.type=fixedValueProperty.getType();
        this.value=fixedValueProperty.getValue();
        this.valueId=fixedValueProperty.getValueId();
    }
    
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public String getValueId() {
        return this.valueId;
    }

    public void setValueId(String valueId) {
        this.valueId = valueId;
    }

}

