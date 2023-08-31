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

import java.util.Set;

import net.solarnetwork.central.security.SecurityPolicy;

/**
 * API for details associated with an authenticated user.
 * 
 * @author matt
 * @version 1.0
 */
public interface Actor {

  /**
   * The SolarNetwork security token associated with the actor.
   * 
   * @return the token ID
   */
  String getTokenId();

  /**
   * The actor type.
   * 
   * @return the actor type, or {@literal null} if not known
   */
  ActorType getActorType();

  /**
   * Flag if publishing is allowed for this actor.
   * 
   * @return {@literal true} if publishing is allowed
   */
  boolean isPublishAllowed();

  /**
   * Get the SolarNetwork user ID associated with the actor.
   * 
   * <p>
   * Typically this is the owner of the security token returned by {@link #getTokenId()}.
   * </p>
   * 
   * @return the user ID, never {@literal null}
   */
  Long getUserId();

  /**
   * Get the security policy associated with the actor.
   * 
   * @return the policy, or {@literal null} if none available
   */
  SecurityPolicy getPolicy();

  /**
   * Get the complete set of SolarNode IDs the user owns.
   * 
   * @return the user's node IDs, never {@literal null}
   */
  Set<Long> getUserNodeIds();

  /**
   * Get the allowed node IDs.
   * 
   * <p>
   * This will return the complete set of node IDs the actor is allowed access to, based on both
   * {@link SecurityPolicy#getNodeIds()} if a policy is available and {@link #getUserNodeIds()}.
   * </p>
   * 
   * @return the allowed node IDs, never {@literal null}
   */
  Set<Long> getAllowedNodeIds();

}
