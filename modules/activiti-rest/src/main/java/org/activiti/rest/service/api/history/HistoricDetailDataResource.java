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

package org.activiti.rest.service.api.history;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
public class HistoricDetailDataResource {

  @Autowired
  protected RestResponseFactory restResponseFactory;

  @Autowired
  protected HistoryService historyService;

  @RequestMapping(value = "/history/historic-detail/{detailId}/data", method = RequestMethod.GET)
  public @ResponseBody
  byte[] getVariableData(@PathVariable("detailId") String detailId, HttpServletRequest request, HttpServletResponse response) {
    try {
      byte[] result = null;
      RestVariable variable = getVariableFromRequest(true, detailId, request);
      if (RestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE.equals(variable.getType())) {
        result = (byte[]) variable.getValue();
        response.setContentType("application/octet-stream");

      } else if (RestResponseFactory.SERIALIZABLE_VARIABLE_TYPE.equals(variable.getType())) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(buffer);
        outputStream.writeObject(variable.getValue());
        outputStream.close();
        result = buffer.toByteArray();
        response.setContentType("application/x-java-serialized-object");

      } else {
        throw new ActivitiObjectNotFoundException("The variable does not have a binary data stream.", null);
      }
      return result;

    } catch (IOException ioe) {
      // Re-throw IOException
      throw new ActivitiException("Unexpected exception getting variable data", ioe);
    }
  }

  public RestVariable getVariableFromRequest(boolean includeBinary, String detailId, HttpServletRequest request) {
    Object value = null;
    HistoricVariableUpdate variableUpdate = null;
    HistoricDetail detailObject = historyService.createHistoricDetailQuery().id(detailId).singleResult();
    if (detailObject instanceof HistoricVariableUpdate) {
      variableUpdate = (HistoricVariableUpdate) detailObject;
      value = variableUpdate.getValue();
    }

    if (value == null) {
      throw new ActivitiObjectNotFoundException("Historic detail '" + detailId + "' doesn't have a variable value.", VariableInstanceEntity.class);
    } else {
      return restResponseFactory.createRestVariable(variableUpdate.getVariableName(), value, null, detailId, RestResponseFactory.VARIABLE_HISTORY_DETAIL, includeBinary);
    }
  }
}
