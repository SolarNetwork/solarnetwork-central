/* ========================================================================
 * Copyright 2022 SolarNetwork Foundation
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
 * A security actor type enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum ActorType {

  /** A node token. */
  Node,

  /** A read-node-data token. */
  ReadNodeDataToken,

  /** A user token. */
  UserToken;

  /**
   * Get an enumeration value for a string value.
   * 
   * @param value
   *        the string value
   * @return the enumeration value, or {@literal null} if not known
   */
  public static ActorType forValue(String value) {
    switch (value) {
      case "Node":
        return Node;
      case "ReadNodeData":
        return ReadNodeDataToken;
      case "User":
        return UserToken;
      default:
        return null;
    }
  }

}
