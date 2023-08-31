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

package net.solarnetwork.flux.vernemq.webhook.domain;

/**
 * API for a message.
 * 
 * @author matt
 * @version 1.0
 */
public interface Message {

  /**
   * Get the topic.
   * 
   * @return the topic
   */
  String getTopic();

  /**
   * Get the quality of service level.
   * 
   * @return the quality of service
   */
  Qos getQos();

  /**
   * Get the message content.
   * 
   * @return the message content
   */
  byte[] getPayload();

  /**
   * Get the "retain" flag.
   * 
   * @return {@literal true} to have the message retained by the broker
   */
  Boolean getRetain();

}
