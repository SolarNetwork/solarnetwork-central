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
 * Basic API for authentication and authorization requests.
 * 
 * @author matt
 * @version 1.0
 */
public interface AuthRequest {

  /**
   * The MQTT client ID.
   * 
   * @return the client ID
   */
  String getClientId();

  /**
   * The MQTT mount point.
   * 
   * @return the mount point
   */
  String getMountpoint();

  /**
   * The MQTT username.
   * 
   * @return the username
   */
  String getUsername();

}
