/* ========================================================================
 * Copyright 2021 SolarNetwork Foundation
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

package net.solarnetwork.flux.vernemq.webhook.service;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;

/**
 * API for a service that can audit specific events.
 * 
 * @author matt
 * @version 1.0
 */
public interface AuditService {

  /**
   * Audit the publication of a message by a node.
   * 
   * @param actor
   *        the node actor
   * @param nodeId
   *        the node ID
   * @param message
   *        the message that is to be published
   */
  void auditPublishMessage(Actor actor, Long nodeId, String sourceId, Message message);

}
