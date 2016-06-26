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

package org.activiti.engine.impl.persistence.entity;

import java.util.Date;

import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.impl.db.Entity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface HistoricDetailEntity extends HistoricDetail, Entity {

  void setProcessInstanceId(String processInstanceId);

  void setActivityInstanceId(String activityInstanceId);

  void setTaskId(String taskId);

  void setExecutionId(String executionId);

  void setTime(Date time);

  String getDetailType();
  
  void setDetailType(String detailType);
  
}
