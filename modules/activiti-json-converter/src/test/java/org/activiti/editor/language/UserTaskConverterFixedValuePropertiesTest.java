package org.activiti.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FixedValueProperty;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.junit.Test;

public class UserTaskConverterFixedValuePropertiesTest extends AbstractConverterTest {

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
		return "test.usertaskfixedvalueproperties.json";
	}

	protected void validateModel(BpmnModel model) {
		// super.validateModel(model);
		FlowElement flowElement = model.getMainProcess().getFlowElement("usertask", true);
		UserTask userTask = (UserTask) flowElement;

		List<FixedValueProperty> fixedValueProperties = userTask.getFixedValueProperties();
		assertNotNull(fixedValueProperties);

		for (FixedValueProperty fixedValueProperty : fixedValueProperties) {
			if (fixedValueProperty.getId().equals("fixedValue1")) {
				checkFixedValueProperty(fixedValueProperty, "fixedValueName1");
			} else if (fixedValueProperty.getId().equals("fixedValue2")) {
				checkFixedValueProperty(fixedValueProperty, "fixedValueName2");
			} else {
				fail("unexpected form property id " + fixedValueProperty.getId());
			}
		}
	}

	private void checkFixedValueProperty(FixedValueProperty fixedValueProperty, String name) {
		assertEquals(name, fixedValueProperty.getName());		
	}
}
