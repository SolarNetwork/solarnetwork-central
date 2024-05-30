/* ========================================================================
 * Copyright 2024 SolarNetwork Foundation
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.DeliverRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuditService;

/**
 * Non-auth hook methods.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
@RequestMapping(path = "/hook", method = RequestMethod.POST)
public class NonAuthHooksController {

  private final AuditService auditService;

  @Autowired
  public NonAuthHooksController(AuditService auditService) {
    super();
    this.auditService = auditService;
  }

  /**
   * On deliver hook.
   * 
   * @return response
   */
  @RequestMapping(value = "", headers = "vernemq-hook=on_deliver")
  public Response onDeliver(@RequestBody DeliverRequest request) {
    auditService.auditDeliverMessage(request);
    return Response.OK;
  }

  /**
   * On deliver hook MQTT 5.
   * 
   * @return response
   */
  @RequestMapping(value = "", headers = "vernemq-hook=on_deliver_m5")
  public Response onDeliverV5(@RequestBody DeliverRequest request) {
    auditService.auditDeliverMessage(request);
    return Response.OK;
  }

}
