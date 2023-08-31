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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorDetails;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorType;

/**
 * Test cases for the {@link ActorDetails} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ActorDetailsTests {

  @Test
  public void allowedNodeIdsNoPolicyNoNodes() {
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 1L, null, null);
    assertThat("No allowed nodes", actor.getAllowedNodeIds(), allOf(notNullValue(), hasSize(0)));
  }

  @Test
  public void allowedNodeIdsWithPolicyNoNodes() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(singleton(1L)).build();
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 2L, policy, null);
    assertThat("Policy node rejected", actor.getAllowedNodeIds(),
        allOf(notNullValue(), hasSize(0)));
  }

  @Test
  public void allowedNodeIdsNoPolicyWithNodes() {
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 1L, null, singleton(2L));
    assertThat("Manual nodes", actor.getAllowedNodeIds(), allOf(notNullValue(), contains(2L)));
  }

  @Test
  public void allowedNodeIdsWithPolicyEmptyWithNodes() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(emptySet()).build();
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 1L, policy, singleton(2L));
    assertThat("Manual nodes", actor.getAllowedNodeIds(), allOf(notNullValue(), contains(2L)));
  }

  @Test
  public void allowedNodeIdsWithPolicyWithNodesMismatch() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(singleton(1L)).build();
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 2L, policy, singleton(3L));
    assertThat("Policy node restricted by user node", actor.getAllowedNodeIds(),
        allOf(notNullValue(), hasSize(0)));
  }

  @Test
  public void allowedNodeIdsWithPolicyWithNodesMismatchSome() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder()
        .withNodeIds(new HashSet<>(asList(1L, 2L, 3L))).build();
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 4L, policy,
        new HashSet<>(asList(1L, 3L)));
    assertThat("Policy nodes restricted by user nodes", actor.getAllowedNodeIds(),
        allOf(notNullValue(), containsInAnyOrder(1L, 3L)));
  }

  @Test
  public void allowedNodeIdsWithPolicyWithNodesMatch() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(singleton(1L)).build();
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 2L, policy, singleton(1L));
    assertThat("Policy node matches user node", actor.getAllowedNodeIds(),
        allOf(notNullValue(), contains(1L)));
  }

  @Test
  public void allowedNodeIdsWithPolicyWithNodesMatchMulti() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder()
        .withNodeIds(new HashSet<>(asList(1L, 2L, 3L))).build();
    ActorDetails actor = new ActorDetails("foo", ActorType.Node, true, 4L, policy,
        new HashSet<>(asList(1L, 2L, 3L)));
    assertThat("Policy nodes match user nodes", actor.getAllowedNodeIds(),
        allOf(notNullValue(), containsInAnyOrder(1L, 2L, 3L)));
  }
}
