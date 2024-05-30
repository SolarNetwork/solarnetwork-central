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
 * Constants for hook names.
 * 
 * <p>
 * Defining the names here allows {@link HookType} to use them in both enum definitions and JSON
 * annotations.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public interface HookNames {

  /** Hook name for authorization on register. */
  String AUTH_ON_REGISTER = "auth_on_register";

  /** Hook name for authorization on publish. */
  String AUTH_ON_PUBLISH = "auth_on_publish";

  /** Hook name for authorization on subscribe. */
  String AUTH_ON_SUBSCRIBE = "auth_on_subscribe";

  /**
   * Hook name for on deliver.
   * 
   * @since 1.1
   */
  String ON_DELIVER = "on_deliver";

}
