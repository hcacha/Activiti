package org.activiti.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class UserTaskConverterExtensionElementTest extends AbstractConverterTest {

	@Test
	public void connvertJsonToModel() throws Exception {
		BpmnModel bpmnModel = readJsonFile();
		validateModel(bpmnModel);
	}

	@Test
	public void doubleConversionValidation() throws Exception {
		BpmnModel bpmnModel = readJsonFile();
		bpmnModel = convertToJsonAndBack(bpmnModel);
		validateModel(bpmnModel);
	}

	protected String getResource() {
		return "test.usertaskmodelcustomextensionelements.json";
	}

	protected void validateModel(BpmnModel model) {
		//super.validateModel(model);
		FlowElement flowElement = model.getMainProcess().getFlowElement("usertask", true);
		UserTask userTask = (UserTask) flowElement;		
		
		List<ExtensionElement> extensionElements = userTask.getExtensionElements().get("activiti-custom-extension-elements");		
		assertNotNull(extensionElements);	
		
		ExtensionElement extensionElement= extensionElements.get(0);
			
		List<ExtensionElement> extensionElementsChild1 = extensionElement.getChildElements().get("activiti-sgli-estado");
		assertNotNull(extensionElementsChild1);
		ExtensionElement extensionElement1 = extensionElementsChild1.get(0);
		assertEquals("1", extensionElement1.getElementText());
		assertTrue(!extensionElement1.getChildElements().isEmpty());

		List<ExtensionElement> extensionElements2 = extensionElement.getChildElements().get("activiti-sgli-weigth");
		assertNotNull(extensionElements2);
		ExtensionElement extensionElement2 = extensionElements2.get(0);
		assertEquals("0.10", extensionElement2.getElementText());
	}
}
