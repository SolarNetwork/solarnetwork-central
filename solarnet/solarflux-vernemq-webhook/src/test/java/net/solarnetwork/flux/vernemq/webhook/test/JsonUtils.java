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

package net.solarnetwork.flux.vernemq.webhook.test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;

/**
 * JSON utilities to help with tests.
 * 
 * @author matt
 * @version 2.0
 */
public final class JsonUtils {

  /**
   * Get a default {@link ObjectMapper} instance.
   * 
   * @return the new instance
   */
  public static ObjectMapper defaultObjectMapper() {
    return tools.jackson.databind.json.JsonMapper.builder()
        .changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
        .changeDefaultPropertyInclusion(
            incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
        .build();
  }

}
