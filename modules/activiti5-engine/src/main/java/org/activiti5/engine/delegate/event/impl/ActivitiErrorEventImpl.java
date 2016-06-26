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
package org.activiti5.engine.delegate.event.impl;

import org.activiti5.engine.delegate.event.ActivitiErrorEvent;
import org.activiti5.engine.delegate.event.ActivitiEventType;

/**
 * Implementation of an {@link ActivitiErrorEvent}.
 * @author Frederik Heremans
 */
public class ActivitiErrorEventImpl extends ActivitiActivityEventImpl implements ActivitiErrorEvent {

	protected String errorCode;
	
	public ActivitiErrorEventImpl(ActivitiEventType type) {
	  super(type);
  }
	
	public void setErrorCode(String errorCode) {
	  this.errorCode = errorCode;
  }
	
	@Override
	public String getErrorCode() {
	  return errorCode;
	}

}
