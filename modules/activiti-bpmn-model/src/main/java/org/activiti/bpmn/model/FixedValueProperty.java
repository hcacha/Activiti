/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.bpmn.model;

/**
 * @author Hugo Cacha
 */
public class FixedValueProperty extends BaseElement {

	protected String name;
	protected String type;
	protected String value;
	protected String valueId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}		
	public void setType(String type) {
		this.type=type;
	}

	public String getValue() {
		return value;
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

	public FixedValueProperty clone() {
		FixedValueProperty clone = new FixedValueProperty();
		clone.setValues(this);
		return clone;
	}

	public void setValues(FixedValueProperty otherProperty) {
		super.setValues(otherProperty);		
		setName(otherProperty.getName());		
		setType(otherProperty.getType());
		setValue(otherProperty.getValue());
		setValueId(otherProperty.getValueId());		
	}

}
