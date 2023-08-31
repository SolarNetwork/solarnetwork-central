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

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A webhook response object.
 * 
 * @author matt
 */
@JsonPropertyOrder(value = { "result", "modifiers" })
public class Response {

  private final ResponseStatus status;

  private final Map<Object, Object> errorStatus;

  private final ResponseModifiers modifiers;

  private final ResponseTopics topics;

  /**
   * Simple OK response constructor.
   */
  public Response() {
    this(ResponseStatus.OK);
  }

  /**
   * OK response constructor with modifiers.
   * 
   * @param modifiers
   *        the modifiers
   */
  public Response(ResponseModifiers modifiers) {
    this(ResponseStatus.OK, null, modifiers, null);
  }

  /**
   * OK response constructor with topics.
   * 
   * @param topics
   *        the topics
   */
  public Response(ResponseTopics topics) {
    this(ResponseStatus.OK, null, null, topics);
  }

  /**
   * Construct with specific status.
   * 
   * @param status
   *        the status
   */
  public Response(ResponseStatus status) {
    this(status, null, null, null);
  }

  /**
   * Simple ERROR response constructor.
   * 
   * @param errorMessage
   *        the error message
   */
  public Response(String errorMessage) {
    this(ResponseStatus.ERROR, errorMessage, null, null);
  }

  /**
   * Constructor.
   * 
   * @param status
   *        the status
   * @param message
   *        the message
   * @param modifiers
   *        the modifiers
   * @param topics
   *        the topics
   */
  private Response(ResponseStatus status, String message, ResponseModifiers modifiers,
      ResponseTopics topics) {
    super();
    this.status = status;
    if (message != null) {
      this.errorStatus = Collections.singletonMap(status, message);
    } else {
      this.errorStatus = null;
    }
    this.modifiers = modifiers;
    this.topics = topics;
  }

  @Override
  public String toString() {
    return "Response{" + (status != null ? "status=" + status + ", " : "")
        + (errorStatus != null ? "errorStatus=" + errorStatus + ", " : "")
        + (modifiers != null ? "modifiers=" + modifiers + ", " : "")
        + (topics != null ? "topics=" + topics : "") + "}";
  }

  @JsonProperty(value = "result")
  public Object getResult() {
    return errorStatus != null ? errorStatus : status;
  }

  @JsonIgnore
  public ResponseStatus getStatus() {
    return status;
  }

  @JsonIgnore
  public Map<Object, ?> getErrorStatus() {
    return errorStatus;
  }

  @JsonProperty(value = "modifiers")
  public ResponseModifiers getModifiers() {
    return modifiers;
  }

  @JsonProperty(value = "topics")
  public ResponseTopics getTopics() {
    return topics;
  }

}
