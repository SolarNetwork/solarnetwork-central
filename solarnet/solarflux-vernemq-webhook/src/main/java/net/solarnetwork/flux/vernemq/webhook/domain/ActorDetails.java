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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.solarnetwork.central.security.SecurityPolicy;

/**
 * Details about an authenticated actor.
 * 
 * @author matt
 * @version 1.1
 */
public class ActorDetails implements Actor {

  private final String tokenId;
  private final ActorType actorType;
  private final boolean publishAllowed;
  private final Long userId;
  private final SecurityPolicy policy;
  private final Set<Long> userNodeIds;
  private final Set<Long> allowedNodeIds;

  /**
   * Constructor.
   * 
   * @param tokenId
   *        the authenticated token ID
   * @param actorType
   *        the actor type
   * @param publishAllowed
   *        {@literal true} if publishing is allowed
   * @param userId
   *        the associated user ID
   * @param policy
   *        the associated policy (or {@literal null} if none defined)
   * @param userNodeIds
   *        the complete set of node IDs owned by {@code userId}
   * @throws IllegalArgumentException
   *         if {@code userId} is {@literal null}
   */
  public ActorDetails(String tokenId, ActorType actorType, boolean publishAllowed, Long userId,
      SecurityPolicy policy, Set<Long> userNodeIds) {
    super();
    this.tokenId = tokenId;
    this.actorType = actorType;
    this.publishAllowed = publishAllowed;
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
    this.userId = userId;
    this.policy = policy;
    this.userNodeIds = (userNodeIds != null ? userNodeIds : emptySet());
    this.allowedNodeIds = resolveAllowedNodeIds(this.userNodeIds, policy);
  }

  /**
   * Construct for node publishing.
   * 
   * <p>
   * This constructor sets {@code publishAllowed} to {@literal true} and configures the allowed node
   * IDs to just {@code nodeId}. This is designed to be used for publishing by a node to its own
   * topics.
   * </p>
   * 
   * @param userId
   *        the node owner ID
   * @param nodeId
   *        the node ID
   */
  public ActorDetails(Long userId, Long nodeId) {
    this(null, ActorType.Node, true, userId, null, singleton(nodeId));
  }

  private static Set<Long> resolveAllowedNodeIds(Set<Long> userNodeIds, SecurityPolicy policy) {
    Set<Long> nodeIds = userNodeIds;
    if (policy != null && policy.getNodeIds() != null) {
      nodeIds = policy.getNodeIds();
      Set<Long> rejected = null;
      for (Long policyNodeId : nodeIds) {
        if (!userNodeIds.contains(policyNodeId)) {
          if (rejected == null) {
            rejected = new HashSet<>(nodeIds.size());
          }
          rejected.add(policyNodeId);
        }
      }
      if (rejected != null && !rejected.isEmpty()) {
        final Set<Long> finalRejected = rejected;
        nodeIds = nodeIds.stream().filter(n -> !finalRejected.contains(n))
            .collect(toCollection(LinkedHashSet::new));
      }
    }
    if (nodeIds == null) {
      nodeIds = userNodeIds;
    }
    return (nodeIds != null ? unmodifiableSet(nodeIds) : emptySet());
  }

  @Override
  public String toString() {
    return (tokenId != null ? tokenId
        : !userNodeIds.isEmpty() ? "Node-" + userNodeIds.iterator().next() : "-") + " (" + userId
        + ")";
  }

  @Override
  public String getTokenId() {
    return tokenId;
  }

  @Override
  public ActorType getActorType() {
    return actorType;
  }

  @Override
  public boolean isPublishAllowed() {
    return publishAllowed;
  }

  @Override
  public Long getUserId() {
    return userId;
  }

  @Override
  public SecurityPolicy getPolicy() {
    return policy;
  }

  @Override
  public Set<Long> getUserNodeIds() {
    return userNodeIds;
  }

  @Override
  public Set<Long> getAllowedNodeIds() {
    return allowedNodeIds;
  }

}
