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

package net.solarnetwork.flux.vernemq.webhook.service.impl.test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorDetails;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorType;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.SimpleAuthorizationEvaluator;

/**
 * Test cases for the {@link SimpleAuthorizationEvaluator} class.
 * 
 * @author matt
 * @version 1.3
 */
public class SimpleAuthorizationEvaluatorTests {

  private SimpleAuthorizationEvaluator service;

  @BeforeEach
  public void setup() {
    service = new SimpleAuthorizationEvaluator();
  }

  private SecurityPolicy policyForNodes(Long... nodes) {
    return new BasicSecurityPolicy.Builder().withNodeIds(Arrays.stream(nodes).collect(toSet()))
        .build();
  }

  private SecurityPolicy policyForMinAggregation(Aggregation agg) {
    return new BasicSecurityPolicy.Builder().withMinAggregation(agg).build();
  }

  private SecurityPolicy policyForSources(String... sources) {
    return new BasicSecurityPolicy.Builder().withSourceIds(Arrays.stream(sources).collect(toSet()))
        .build();
  }

  private ActorDetails actor(SecurityPolicy policy, Long... nodes) {
    return actor(policy, false, nodes);
  }

  private ActorDetails actor(SecurityPolicy policy, ActorType actorType, Long... nodes) {
    return actor(policy, actorType, false, nodes);
  }

  private ActorDetails actor(SecurityPolicy policy, boolean publishAllowed, Long... nodes) {
    return actor(policy, (publishAllowed ? ActorType.Node : ActorType.ReadNodeDataToken),
        publishAllowed, nodes);
  }

  private ActorDetails actor(SecurityPolicy policy, ActorType actorType, boolean publishAllowed,
      Long... nodes) {
    Set<Long> nodeIds = null;
    if (nodes != null) {
      nodeIds = Arrays.stream(nodes).collect(toSet());
    }
    return new ActorDetails(UUID.randomUUID().toString(), actorType, publishAllowed, 1L, policy,
        nodeIds);
  }

  private ActorDetails actor(Long node) {
    return new ActorDetails(1L, node);
  }

  private TopicSettings requestForTopics(String... topics) {
    List<TopicSubscriptionSetting> settings = Arrays.stream(topics)
        .map(s -> TopicSubscriptionSetting.builder().withTopic(s).withQos(Qos.AtLeastOnce).build())
        .collect(toList());
    return new TopicSettings(settings);
  }

  private Message requestMessage(String topic) {
    return PublishRequest.builder().withTopic(topic).withQos(Qos.AtLeastOnce).build();
  }

  private void thenSubscribeTopic(TopicSettings request, TopicSettings response, Qos expected,
      String message) {
    // @formatter:off
    then(response)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .singleElement()
      .returns(request.getSettings().get(0).getTopic(), from(TopicSubscriptionSetting::getTopic))
      .as(message)
      .returns(expected, from(TopicSubscriptionSetting::getQos))
      ;
    // @formatter:on
  }

  private void thenSubscribeUserTopic(TopicSettings request, TopicSettings response, Qos expected,
      String message) {
    // @formatter:off
    then(response)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .singleElement()
      .returns("user/1/"+request.getSettings().get(0).getTopic(), from(TopicSubscriptionSetting::getTopic))
      .as(message)
      .returns(expected, from(TopicSubscriptionSetting::getQos))
      ;
    // @formatter:on
  }

  @Test
  public void subscribeNoPolicyAllowedNode() {
    ActorDetails actor = actor(null, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Allowed without policy");
  }

  @Test
  public void subscribeNoPolicyAllowedNodeQosDowngraded() {
    ActorDetails actor = actor(null, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    service.setMaxQos(Qos.AtMostOnce);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtMostOnce, "Allowed with downgraded QoS");
  }

  @Test
  public void subscribeNoPolicyDeniedNode() {
    ActorDetails actor = actor(null, 2L);
    TopicSettings request = requestForTopics("node/3/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic denied via ownership");
  }

  @Test
  public void subscribeWithPolicyAllowedNodeNoRestriction() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed via ownership and empty policy restriction");
  }

  @Test
  public void subscribeWithPolicyAllowedNodeWithRestriction() {
    SecurityPolicy policy = policyForNodes(2L);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed via policy restriction");
  }

  @Test
  public void subscribeWithPolicyDeniedNodeWithRestriction() {
    SecurityPolicy policy = policyForNodes(3L);
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic denied via policy restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceAllowed() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed with source restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceDenied() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/bar");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic deinied via source restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceWildStepDenied() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/+");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Wildcard topic deinied via source restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/#");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Wildcard topic deinied via source restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceWildCharAllowed() {
    SecurityPolicy policy = policyForSources("/fo?");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed with source restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceWildCharDenied() {
    SecurityPolicy policy = policyForSources("/fo?");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/boo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic deinied via source restriction");
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceAllowed() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bar/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with multi-level source restriction");
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bim/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via multi-level source restriction");
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceWildStepDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bim/+");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via multi-level source restriction");
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/#");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via multi-level source restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceSimpleStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with simple source wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceSimpleStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/z");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via simple source wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceStartStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/*/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/z/bar/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source start step wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceStartStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/*/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/z/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via source start wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/z/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source middle wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternWildStepAllowed() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/+/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source middle wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/#/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic deined with source middle wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/z/bim");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via source middle wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bar/z");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source end step wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternWildStepAllowed() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bar/+");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source end step wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bar/#");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied with source end step wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bim/z");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed,
        "Topic denied via source end step wildcard restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternSingleStepAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source path wildcard single step restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternMultiStepAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bar");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source path wildcard multi step restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternWildStepAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/+");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source path wildcard single step restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternWildPathAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/#");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source path wildcard single step restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternComplexWildPathAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/*/foo/#");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed with source path wildcard single step restriction");
  }

  @Test
  public void subscribeWithPolicyWithSourcePrefixedPathPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/bar");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed with source path wildcard");
  }

  @Test
  public void subscribeWithPolicyWithSourcePrefixedPathWildPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/#");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed with source path wildcard");
  }

  @Test
  public void subscribeWithPolicyWithSourcePrefixedPathPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/z/bar");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic denied via source path wildcard");
  }

  @Test
  public void subscribeWithPolicyWithSourceComplexPathPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/*/bar/**/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/z/bar/a/b/c/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed with source path wildcard");
  }

  @Test
  public void subscribeWithPolicyWithSourceComplexPathPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/*/bar/**/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/z/bar/a/b/c");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic denied via source path wildcard");
  }

  @Test
  public void subscribeWithPolicyWithSourceComplexSubPathPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/**/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo/a/*/b/bam");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed with source path wildcard");
  }

  @Test
  public void subscribeWithPolicyMinAggregationNone() {
    SecurityPolicy policy = policyForMinAggregation(Aggregation.None);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo", "node/2/datum/h/foo",
        "node/2/datum/d/foo", "node/2/datum/M/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .hasSize(4)
      .as("All topics allowed via policy restriction")
      .allSatisfy(s -> {
        then(s).returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos));
      })
      .satisfies(l -> {
        then(l).element(0).returns("node/2/datum/0/foo", from(TopicSubscriptionSetting::getTopic));
        then(l).element(1).returns("node/2/datum/h/foo", from(TopicSubscriptionSetting::getTopic));
        then(l).element(2).returns("node/2/datum/d/foo", from(TopicSubscriptionSetting::getTopic));
        then(l).element(3).returns("node/2/datum/M/foo", from(TopicSubscriptionSetting::getTopic));
      })
      ;
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyMinAggregationHour() {
    SecurityPolicy policy = policyForMinAggregation(Aggregation.Hour);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo", "node/2/datum/h/foo",
        "node/2/datum/d/foo", "node/2/datum/M/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .hasSize(4)
      .as("Some topics allowed via policy restriction")
      .satisfies(l -> {
        then(l).element(0)
          .returns("node/2/datum/0/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(1)
          .returns("node/2/datum/h/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(2)
          .returns("node/2/datum/d/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(3)
          .returns("node/2/datum/M/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
      })
      ;
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyMinAggregationDay() {
    SecurityPolicy policy = policyForMinAggregation(Aggregation.Day);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo", "node/2/datum/h/foo",
        "node/2/datum/d/foo", "node/2/datum/M/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .hasSize(4)
      .as("Some topics allowed via policy restriction")
      .satisfies(l -> {
        then(l).element(0)
          .returns("node/2/datum/0/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(1)
          .returns("node/2/datum/h/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(2)
          .returns("node/2/datum/d/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(3)
          .returns("node/2/datum/M/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
      })
      ;
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyMinAggregationMonth() {
    SecurityPolicy policy = policyForMinAggregation(Aggregation.Month);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo", "node/2/datum/h/foo",
        "node/2/datum/d/foo", "node/2/datum/M/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .hasSize(4)
      .as("Some topics allowed via policy restriction")
      .satisfies(l -> {
        then(l).element(0)
          .returns("node/2/datum/0/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(1)
          .returns("node/2/datum/h/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(2)
          .returns("node/2/datum/d/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(3)
          .returns("node/2/datum/M/foo", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
      })
      ;
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyMinAggregationYear() {
    SecurityPolicy policy = policyForMinAggregation(Aggregation.Year);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo", "node/2/datum/h/foo",
        "node/2/datum/d/foo", "node/2/datum/M/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .isNotNull()
      .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
      .hasSize(4)
      .as("No topics allowed via policy restriction")
      .allSatisfy(s -> {
        then(s).returns(Qos.NotAllowed, from(TopicSubscriptionSetting::getQos));
      })
      .satisfies(l -> {
        then(l).element(0).returns("node/2/datum/0/foo", from(TopicSubscriptionSetting::getTopic));
        then(l).element(1).returns("node/2/datum/h/foo", from(TopicSubscriptionSetting::getTopic));
        then(l).element(2).returns("node/2/datum/d/foo", from(TopicSubscriptionSetting::getTopic));
        then(l).element(3).returns("node/2/datum/M/foo", from(TopicSubscriptionSetting::getTopic));
      })
      ;
    // @formatter:on
  }

  @Test
  public void subscribeWithWildcardNodeUserTopicPrefixNotEnabled() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/+/datum/0/foo");
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic denied from node wildcard");
  }

  @Test
  public void subscribeWithWildcardNodeAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/+/datum/0/foo");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeUserTopic(request, result, Qos.AtLeastOnce, "Topic allowed with node wildcard");
  }

  @Test
  public void subscribeWithUserTopicPrefixAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeUserTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed and rewritten with user prefix");
  }

  @Test
  public void subscribeWithUserTopicPrefixAllNodesAndSourcesAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/+/datum/0/#");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeUserTopic(request, result, Qos.AtLeastOnce,
        "Topic allowed and rewritten with user prefix");
  }

  @Test
  public void subscribeUserTopicAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, ActorType.UserToken, 2L, 3L);
    TopicSettings request = requestForTopics("user/1/events");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.AtLeastOnce, "Topic allowed and keeps user prefix");
  }

  @Test
  public void subscribeUserTopicNodeNotAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, ActorType.Node, 2L, 3L);
    TopicSettings request = requestForTopics("user/1/events");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic not allowed and keeps user prefix");
  }

  @Test
  public void subscribeUserTopicReadNodeDataNotAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, ActorType.ReadNodeDataToken, 2L, 3L);
    TopicSettings request = requestForTopics("user/1/events");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic not allowed and keeps user prefix");
  }

  @Test
  public void subscribeUserTopicNoPrefixAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, ActorType.UserToken, 2L, 3L);
    TopicSettings request = requestForTopics("events");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeUserTopic(request, result, Qos.AtLeastOnce, "Topic allowed and keeps user prefix");
  }

  @Test
  public void subscribeUserTopicNoPrefixNodeNotAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, ActorType.Node, 2L, 3L);
    TopicSettings request = requestForTopics("events");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic not allowed and omits user prefix");
  }

  @Test
  public void subscribeUserTopicNoPrefixReadNodeDataNotAllowed() {
    SecurityPolicy policy = null;
    ActorDetails actor = actor(policy, ActorType.ReadNodeDataToken, 2L, 3L);
    TopicSettings request = requestForTopics("events");
    service.setUserTopicPrefix(true);
    TopicSettings result = service.evaluateSubscribe(actor, request);

    // THEN
    thenSubscribeTopic(request, result, Qos.NotAllowed, "Topic not allowed and omits user prefix");
  }

  @Test
  public void publishActorNotAllowed() {
    ActorDetails actor = actor(null, false, 2L);
    Message request = requestMessage("node/2/datum/0/foo");
    Message result = service.evaluatePublish(actor, request);
    then(result).as("Result not available").isNull();
  }

  @Test
  public void publishAllowedNoChange() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("node/2/datum/0/foo");
    Message result = service.evaluatePublish(actor, request);
    then(result).as("Result OK").isSameAs(request);
  }

  @Test
  public void publishAllowedQosDowngrade() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("node/2/datum/0/foo");
    service.setMaxQos(Qos.AtMostOnce);
    Message result = service.evaluatePublish(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .as("Result provided")
      .isNotNull()
      .isNotSameAs(request)
      .as("Topic allowed and downgraded Qos")
      .returns("node/2/datum/0/foo", from(Message::getTopic))
      .returns(Qos.AtMostOnce, from(Message::getQos))
      ;
    // @formatter:on
  }

  @Test
  public void publishDeniedWrongNode() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("node/3/datum/0/foo");
    Message result = service.evaluatePublish(actor, request);

    then(result).as("Result not available").isNull();
  }

  @Test
  public void publishAllowedWithUserPrefix() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    Message result = service.evaluatePublish(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .as("Result provided")
      .isNotNull()
      .isNotSameAs(request)
      .as("Topic allowed and rewritten with user prefix")
      .returns("user/1/node/2/datum/0/foo", from(Message::getTopic))
      .returns(Qos.AtLeastOnce, from(Message::getQos))
      ;
    // @formatter:on
  }

  @Test
  public void publishAllowedWithUserPrefix_included() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/1/node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    Message result = service.evaluatePublish(actor, request);

    // THEN
    // @formatter:off
    then(result)
      .as("Result provided")
      .isNotNull()
      .isSameAs(request)
      .as("Topic allowed and keeps user prefix")
      .returns("user/1/node/2/datum/0/foo", from(Message::getTopic))
      .returns(Qos.AtLeastOnce, from(Message::getQos))
      ;
    // @formatter:on
  }

  @Test
  public void publishDeniedWithUserPrefix_included_userIdDiffersFromActor() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/99/node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    Message result = service.evaluatePublish(actor, request);
    assertThat("Result not available", result, nullValue());
  }

  @Test
  public void publishDeniedWithUserPrefix_included_userIdNaN() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/ABC/node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    Message result = service.evaluatePublish(actor, request);
    assertThat("Result not available", result, nullValue());
  }

  @Test
  public void sourceIdForPublish_simple() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/99/node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    String sourceId = service.sourceIdForPublish(actor, request);
    assertThat("Source ID extracted", sourceId, is(equalTo("/foo")));
  }

  @Test
  public void sourceIdForPublish_withoutUserId() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("node/2/datum/0/foo");
    service.setUserTopicPrefix(true);
    String sourceId = service.sourceIdForPublish(actor, request);
    assertThat("Source ID extracted", sourceId, is(equalTo("/foo")));
  }

  @Test
  public void sourceIdForPublish_spaces() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/99/node/2/datum/0/Mock Power Meter");
    service.setUserTopicPrefix(true);
    String sourceId = service.sourceIdForPublish(actor, request);
    assertThat("Source ID extracted", sourceId, is(equalTo("/Mock Power Meter")));
  }

  @Test
  public void sourceIdForPublish_path() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/99/node/2/datum/0/a/path/here");
    service.setUserTopicPrefix(true);
    String sourceId = service.sourceIdForPublish(actor, request);
    assertThat("Source ID extracted", sourceId, is(equalTo("/a/path/here")));
  }

  @Test
  public void sourceIdForPublish_nullInput() {
    String sourceId = service.sourceIdForPublish(null, null);
    assertThat("Source ID not available", sourceId, is(nullValue()));
  }

  @Test
  public void sourceIdForPublish_noMatch() {
    ActorDetails actor = actor(2L);
    Message request = requestMessage("user/99/node/2/datum/0/");
    service.setUserTopicPrefix(true);
    String sourceId = service.sourceIdForPublish(actor, request);
    assertThat("Source ID not available", sourceId, is(nullValue()));
  }

}
