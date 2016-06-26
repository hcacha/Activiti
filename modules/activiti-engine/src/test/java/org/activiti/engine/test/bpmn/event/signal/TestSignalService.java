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
package org.activiti.engine.test.bpmn.event.signal;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class TestSignalService implements JavaDelegate {
  
  /**
   * Dummy service that uses a mock to simulate a file system.
   * 
   * Normally, a database (or file system) is checked, if a file is present or
   * not.
   */
  public void execute(DelegateExecution execution) {
    // save current state into the process variable
    boolean exists = FileExistsMock.getInstance().fileExists();   
    execution.setVariable("fileexists", exists);    
  }

}
