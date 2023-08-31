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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthService;

/**
 * VerneMQ web hooks for MQTT v5 authorization.
 * 
 * @author matt
 * @version 1.1
 */
@RestController
@RequestMapping(path = "/hook", method = RequestMethod.POST)
public class AuthHooksControllerV5 {

  private final AuthService authService;

  private static final Logger log = LoggerFactory.getLogger(AuthHooksControllerV5.class);

  @Autowired
  public AuthHooksControllerV5(AuthService authService) {
    super();
    this.authService = authService;
  }

  /**
   * Authenticate on register hook.
   * 
   * @return map of properties
   */
  @RequestMapping(value = "", headers = "vernemq-hook=auth_on_register_m5")
  public Response authOnRegister(@RequestBody RegisterRequest request) {
    if (log.isTraceEnabled()) {
      log.trace("Register request: {}", JsonUtils.getJSONString(request, null));
    }
    return authService.authenticateRequest(request);
  }

  /**
   * Authorize on publish hook.
   * 
   * @return map of properties
   */
  @RequestMapping(value = "", headers = "vernemq-hook=auth_on_publish_m5")
  public Response authOnPublish(@RequestBody PublishRequest request) {
    return authService.authorizeRequest(request);
  }

  /**
   * Authorize on subscribe hook.
   * 
   * @return map of properties
   */
  @RequestMapping(value = "", headers = "vernemq-hook=auth_on_subscribe_m5")
  public Response authOnSubscribe(@RequestBody SubscribeRequest request) {
    return authService.authorizeRequest(request);
  }

}
