/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Web controller for "are you there" type requests.
 * 
 * @author matt
 */
@RestController
@RequestMapping(path = "/api/v1", method = RequestMethod.GET)
public class PingController {

  /**
   * Get a simple {@literal allGood} assessment.
   * 
   * @return map of properties
   */
  @RequestMapping("/ping")
  public Response<Map<String, ?>> ping() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("allGood", true);
    return Response.response(data);
  }

}
