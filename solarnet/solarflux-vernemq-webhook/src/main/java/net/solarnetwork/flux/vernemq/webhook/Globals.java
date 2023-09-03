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

package net.solarnetwork.flux.vernemq.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some global references for the webhook project.
 * 
 * @author matt
 * @version 1.0
 */
public final class Globals {

  /** A global "audit" logger for audit events to be logged to. */
  public static final Logger AUDIT_LOG = LoggerFactory
      .getLogger(Globals.class.getPackage().getName() + ".AUDIT");

  private Globals() {
    // can't construct me
  }

}
