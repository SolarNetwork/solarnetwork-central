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

package net.solarnetwork.flux.vernemq.webhook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.flux.vernemq.webhook.service.impl.SimpleAuthorizationEvaluator;

/**
 * Configuration for authorization services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class AuthConfig {

  @Value("${auth.userTopicPrefixEnabled:true}")
  private boolean userTopicPrefix = true;

  @Value("${mqtt.maxQos:1}")
  private int maxQos = Qos.AtLeastOnce.getKey();

  /**
   * The {@link AuthorizationEvaluator}.
   * 
   * @return the evaluator service
   */
  @Bean
  public SimpleAuthorizationEvaluator authorizationEvaluator() {
    SimpleAuthorizationEvaluator ae = new SimpleAuthorizationEvaluator();
    ae.setUserTopicPrefix(userTopicPrefix);
    ae.setMaxQos(Qos.forKey(maxQos));
    return ae;
  }

}
