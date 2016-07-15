package org.activiti.bpmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FixedValueProperty;
import org.activiti.bpmn.model.UserTask;

public class FixedValuePropertyParser extends BaseChildElementParser {

	public String getElementName() {
		return ELEMENT_FIXED_VALUE_PROPERTY;
	}

	public boolean accepts(BaseElement element) {
		return (element instanceof UserTask);
	}

	@Override
	public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
		// TODO Auto-generated method stub
		if (!accepts(parentElement))
			return;

		FixedValueProperty property = new FixedValueProperty();
		BpmnXMLUtil.addXMLLocation(property, xtr);

		property.setId(xtr.getAttributeValue(null, ATTRIBUTE_FIXED_VALUE_ID));
		property.setName(xtr.getAttributeValue(null, ATTRIBUTE_FIXED_VALUE_NAME));
		property.setType(xtr.getAttributeValue(null, ATTRIBUTE_FIXED_VALUE_TYPE));
		property.setValue(xtr.getAttributeValue(null, ATTRIBUTE_FIXED_VALUE_VALUE));
		property.setValueId(xtr.getAttributeValue(null, ATTRIBUTE_FIXED_VALUE_VALUE_ID));	

		if (parentElement instanceof UserTask) {
			((UserTask) parentElement).getFixedValueProperties().add(property);
		} 

	}

}
