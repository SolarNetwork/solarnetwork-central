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

package net.solarnetwork.flux.vernemq.webhook.service;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;

/**
 * Service to evaluate requests against security policies.
 * 
 * @author matt
 * @version 1.1
 */
public interface AuthorizationEvaluator {

  /**
   * Evaluate a request to subscribe to a set of topics.
   * 
   * @param actor
   *        the authenticated actor
   * @param topics
   *        the topics to subscribe to
   * @return the resulting topics to subscribe to, or {@literal null} if subscribing is not
   *         authorized for any reason
   */
  TopicSettings evaluateSubscribe(Actor actor, TopicSettings topics);

  /**
   * Evaluate a request to publish to a topic.
   * 
   * @param actor
   *        the authenticated actor
   * @param message
   *        the message to publish
   * @return the resulting message to publish; or {@literal null} if publishing is not authorized
   *         for any reason
   */
  Message evaluatePublish(Actor actor, Message message);

  /**
   * Extract the source ID from a publish message.
   * 
   * @param actor
   *        the actor
   * @param message
   *        the message
   * @return the source ID, or {@literal null} if one cannot be determined
   * @since 1.1
   */
  String sourceIdForPublish(Actor actor, Message message);

}
