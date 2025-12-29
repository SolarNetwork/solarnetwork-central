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

package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Test cases for the {@link Qos} enum.
 * 
 * @author matt
 */
public class QosTests {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void toJson() throws JSONException {
    for (Qos qos : Qos.values()) {
      String json = objectMapper.writeValueAsString(qos);

      JSONAssert.assertEquals("Qos " + qos, String.valueOf(qos.getKey()), json, true);
    }
  }

  @Test
  public void fromJson() throws IOException {
    for (Qos qos : Qos.values()) {
      String json = String.valueOf(qos.getKey());

      Qos q = objectMapper.readValue(json, Qos.class);
      assertThat("Qos " + qos, q, equalTo(qos));
    }
  }

  @Test
  public void forKeyBadValue() {
    assertThrows(IllegalArgumentException.class, () -> {
      Qos.forKey(-1);
    });
  }

}
