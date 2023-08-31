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

import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

/**
 * Base class for general testing support.
 * 
 * @author matt
 */
public abstract class TestSupport {

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Load a class-path relative resource.
   * 
   * @param resourceName
   *        the resource
   * @return the content
   * @throws RuntimeException
   *         if any {@link IOException} occurs
   */
  protected byte[] classResourceAsBytes(String resourceName) {
    try {
      return FileCopyUtils.copyToByteArray(getClass().getResourceAsStream(resourceName));
    } catch (IOException e) {
      throw new RuntimeException("Error loading class " + getClass().getSimpleName() + " resource ["
          + resourceName + "]: " + e.getMessage(), e);
    }
  }

  /**
   * Load a class-path relative resource as a string.
   * 
   * @param resourceName
   *        the resource
   * @param charsetName
   *        the charset to use
   * @return the content
   * @throws RuntimeException
   *         if any {@link IOException} occurs
   */
  protected String classResourceAsString(String resourceName, String charsetName) {
    try {
      return FileCopyUtils.copyToString(
          new InputStreamReader(getClass().getResourceAsStream(resourceName), charsetName));
    } catch (IOException e) {
      throw new RuntimeException("Error loading class " + getClass().getSimpleName() + " resource ["
          + resourceName + "]: " + e.getMessage(), e);
    }
  }
}
