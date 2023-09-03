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

package net.solarnetwork.flux.vernemq.webhook.service.impl;

import static net.solarnetwork.flux.vernemq.webhook.Globals.AUDIT_LOG;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorType;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.util.StringUtils;

/**
 * Basic implementation of {@link AuthorizationEvaluator}.
 * 
 * <p>
 * This service works with topics adhering to the following syntaxes:
 * </p>
 * 
 * <ul>
 * <li><code>node/{nodeId}/datum/{aggregation}/{sourceId}</code></li>
 * <li><code>user/{userId}/node/{nodeId}/datum/{aggregation}/{sourceId}</code></li>
 * <li><code>{topic}</code> - with user token and {@code userTopicPrefix} enabled</li>
 * <li><code>user/{userId}/{topic}</code> - with user token</li>
 * </ul>
 * 
 * <p>
 * To support wildcard node IDs, {@link #setUserTopicPrefix(boolean)} must be set to
 * {@literal true}. When enabled, all successfully authorized topics will have a
 * <code>user/{userId}/</code> prefix added to them, so the full re-written topics look like this:
 * </p>
 * 
 * <pre>
 * <code>user/{userId}/node/{nodeId}/datum/{aggregation}/{sourceId}</code>
 * </pre>
 * 
 * <p>
 * Then a {@literal +} wildcard may be used for the topic node ID, as long as the actor's policy
 * does not restrict it.
 * </p>
 * 
 * @author matt
 * @version 1.3
 */
public class SimpleAuthorizationEvaluator implements AuthorizationEvaluator {

  /**
   * The default value for the {@code nodeDatumTopicRegex} property.
   */
  // CHECKSTYLE OFF: LineLength
  public static final String DEFAULT_NODE_DATUM_TOPIC_REGEX = "(?:user/(\\d+)/)?node/(\\d+|\\+)/datum/([^/]+)(/.+)";
  // CHECKSTYLE ON: LineLength

  /**
   * The default value for the {@code userTopicRegex} property.
   */
  // CHECKSTYLE OFF: LineLength
  public static final String DEFAULT_USER_TOPIC_REGEX = "(?:user/(\\d+)/)?(.+)";
  // CHECKSTYLE ON: LineLength

  private Pattern nodeDatumTopicRegex = Pattern.compile(DEFAULT_NODE_DATUM_TOPIC_REGEX);
  private Pattern userTopicRegex = Pattern.compile(DEFAULT_USER_TOPIC_REGEX);
  private boolean userTopicPrefix = false;
  private Qos maxQos = null;

  @Override
  public Message evaluatePublish(Actor actor, Message message) {
    if (actor == null || message == null || message.getTopic() == null
        || message.getTopic().isEmpty()) {
      return message;
    }
    String topic = message.getTopic();
    if (!actor.isPublishAllowed()) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: publish not allowed", topic, actor);
      return null;
    }

    Qos qos = message.getQos();
    if (maxQos != null && qos.getKey() > maxQos.getKey()) {
      qos = maxQos;
    }
    Matcher m = nodeDatumTopicRegex.matcher(topic);
    if (!m.matches()) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: invalid topic pattern", topic, actor);
      return null;
    } else {
      String topicUserId = m.group(1);
      String topicNode = m.group(2);
      String topicAgg = m.group(3);
      String topicSource = m.group(4);
      if (!(topicUserAllowed(actor, topic, topicUserId) && topicNodeAllowed(actor, topic, topicNode)
          && topicSourceAllowed(actor, topic, topicSource)
          && topicAggregationAllowed(actor, topic, topicAgg))) {
        return null;
      }
      if (userTopicPrefix && (topicUserId == null || topicUserId.isEmpty())) {
        topic = "user/" + actor.getUserId() + "/" + topic;
      }
    }

    Message result;
    if (qos.equals(message.getQos()) && topic.equals(message.getTopic())) {
      // no change
      result = message;
    } else {
      // @formatter:off
      result = PublishRequest.builder()
          .withTopic(topic)
          .withQos(qos)
          .withRetain(message.getRetain())
          .withPayload(message.getPayload())
          .build();
      // @formatter:on
    }

    AUDIT_LOG.info("User {} granted publish {}", actor, result);
    return result;
  }

  @Override
  public TopicSettings evaluateSubscribe(Actor actor, TopicSettings topics) {
    if (actor == null || topics == null || topics.getSettings() == null
        || topics.getSettings().isEmpty()) {
      return topics;
    }
    List<TopicSubscriptionSetting> req = topics.getSettings();
    List<TopicSubscriptionSetting> res = new ArrayList<>(req.size());
    boolean haveChange = false;
    for (TopicSubscriptionSetting s : req) {
      String topic = s.getTopic();
      Qos qos = s.getQos();
      if (maxQos != null && qos.getKey() > maxQos.getKey()) {
        qos = maxQos;
      }
      Matcher m = nodeDatumTopicRegex.matcher(topic);
      if (!m.matches()) {
        boolean userTopicMatch = false;
        if (actor.getActorType() == ActorType.UserToken) {
          Matcher um = userTopicRegex.matcher(topic);
          if (um.matches()) {
            userTopicMatch = true;
            String topicUserId = um.group(1);
            if (!topicUserAllowed(actor, topic, topicUserId)) {
              qos = Qos.NotAllowed;
            }
            if (userTopicPrefix && (topicUserId == null || topicUserId.isEmpty())) {
              topic = "user/" + actor.getUserId() + "/" + topic;
            }
          }
        }
        if (!userTopicMatch) {
          AUDIT_LOG.info("Topic [{}] access denied to {}: invalid topic pattern", topic, actor);
          qos = Qos.NotAllowed;
        }
      } else {
        String topicUserId = m.group(1);
        String topicNode = m.group(2);
        String topicAgg = m.group(3);
        String topicSource = m.group(4);
        if (!(topicUserAllowed(actor, topic, topicUserId)
            && topicNodeAllowed(actor, topic, topicNode)
            && topicSourceAllowed(actor, topic, topicSource)
            && topicAggregationAllowed(actor, topic, topicAgg))) {
          qos = Qos.NotAllowed;
        }
        if (userTopicPrefix && (topicUserId == null || topicUserId.isEmpty())) {
          topic = "user/" + actor.getUserId() + "/" + topic;
        }
      }
      if (qos.equals(s.getQos()) && topic.equals(s.getTopic())) {
        // no change
        res.add(s);
      } else {
        // changed
        if (!haveChange) {
          haveChange = true;
        }
        res.add(TopicSubscriptionSetting.builder().withTopic(topic).withQos(qos).build());
      }
    }

    TopicSettings result = (haveChange ? new TopicSettings(res) : topics);
    AUDIT_LOG.info("User {} granted subscribe access to topics [{}]", actor, result);
    return result;
  }

  @Override
  public String sourceIdForPublish(Actor actor, Message message) {
    if (actor == null || message == null || message.getTopic() == null
        || message.getTopic().isEmpty()) {
      return null;
    }
    final String topic = message.getTopic();
    final Matcher m = nodeDatumTopicRegex.matcher(topic);
    if (!m.matches()) {
      return null;
    }
    return m.group(4);
  }

  private PathMatcher createPathMatcher() {
    AntPathMatcher matcher = new AntPathMatcher();
    matcher.setCachePatterns(true);
    matcher.setCaseSensitive(true);
    return matcher;
  }

  private boolean topicUserAllowed(Actor actor, String topic, String topicUserId) {
    if (topicUserId != null && !topicUserId.isEmpty()) {
      Long actorUserId = actor.getUserId();
      if (actorUserId == null) {
        // userId required for matching topic user
        AUDIT_LOG.info(
            "Topic [{}] access denied to {}: topic user ID not allowed without actor user ID",
            topic, actor);
        return false;
      }
      try {
        Long userId = Long.valueOf(topicUserId);
        if (!actorUserId.equals(userId)) {
          // requested user ID not allowed
          AUDIT_LOG.info("Topic [{}] access denied to {}: user ID not allowed", topic, actor);
          return false;
        }
      } catch (NumberFormatException e) {
        // should not be here; deny access
        AUDIT_LOG.info("Topic [{}] access denied to {}: user ID not a number", topic, actor);
        return false;
      }
    }
    return true;
  }

  private boolean topicNodeAllowed(Actor actor, String topic, String topicNode) {
    Set<Long> restrictedNodeIds = (actor.getPolicy() != null ? actor.getPolicy().getNodeIds()
        : null);
    if ("+".equals(topicNode)) {
      // trying to use a wild card node ID
      if (!userTopicPrefix) {
        // userTopicPrefix must be enabled for wild card node ID support
        AUDIT_LOG.info("Topic [{}] access denied to {}: wildcard node ID support not enabled",
            topic, actor);
        return false;
      } else if (actor.getUserId() == null) {
        // userId required for wild card node ID support
        AUDIT_LOG.info(
            "Topic [{}] access denied to {}: wildcard node ID not allowed without user ID", topic,
            actor);
        return false;
      } else if (!(restrictedNodeIds == null || restrictedNodeIds.isEmpty())) {
        // policy restricts access so deny
        AUDIT_LOG.info("Topic [{}] access denied to {}: wildcard node ID not allowed by policy",
            topic, actor);
        return false;
      }
    } else {
      Long nodeId;
      try {
        nodeId = Long.valueOf(topicNode);
        if (!actor.getAllowedNodeIds().contains(nodeId)) {
          // requested node ID not allowed, by policy or via ownership
          AUDIT_LOG.info("Topic [{}] access denied to {}: node ID not allowed", topic, actor);
          return false;
        }
      } catch (NumberFormatException e) {
        // should not be here; deny access
        AUDIT_LOG.info("Topic [{}] access denied to {}: node ID not a number", topic, actor);
        return false;
      }
    }
    return true;
  }

  private boolean topicSourceAllowed(Actor actor, String topic, String topicSource) {
    Set<String> policySources = (actor.getPolicy() != null ? actor.getPolicy().getSourceIds()
        : null);
    if (policySources == null || policySources.isEmpty()) {
      return true;
    }
    // to make source wildcard step * NOT match MQTT wildcard path, insert path for all #
    String topicSourceToMatch = topicSource.replaceAll("#", "#/#");
    PathMatcher pathMatcher = createPathMatcher();
    for (String policySource : policySources) {
      if (pathMatcher.isPattern(policySource)) {
        if (pathMatcher.match(policySource, topicSourceToMatch)) {
          return true;
        }
      } else if (policySource.equals(topicSource)) {
        return true;
      }
    }
    AUDIT_LOG.info("Topic [{}] access denied to {}: source policy restrictions: {}", topic, actor,
        StringUtils.commaDelimitedStringFromCollection(policySources));
    return false;
  }

  private boolean topicAggregationAllowed(Actor actor, String topic, String topicAgg) {
    Set<Aggregation> policyAggregations = (actor.getPolicy() != null
        ? actor.getPolicy().getAggregations()
        : null);
    if (policyAggregations == null || policyAggregations.isEmpty()) {
      return true;
    }
    Aggregation agg;
    try {
      agg = Aggregation.forKey(topicAgg);
    } catch (IllegalArgumentException e) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: invalid aggregation [{}]", topic, actor,
          topicAgg);
      return false;
    }
    if (!policyAggregations.contains(agg)) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: aggregation policy restrictions: {}", topic,
          actor, StringUtils.commaDelimitedStringFromCollection(policyAggregations));
      return false;
    }
    return true;
  }

  /**
   * Get the node datum topic regular expression.
   * 
   * @return the regular expression; defaults to {@link #DEFAULT_NODE_DATUM_TOPIC_REGEX}
   */
  public Pattern getNodeDatumTopicRegex() {
    return nodeDatumTopicRegex;
  }

  /**
   * Set the node datum topic regular expression.
   * 
   * <p>
   * This expression is matched against the topic requests, and must provide the following matching
   * groups:
   * </p>
   * 
   * <ol>
   * <li>node ID</li>
   * <li>aggregation</li>
   * <li>source ID</li>
   * </ol>
   * 
   * <p>
   * Each group should be treated as a string, to accommodate topic wild cards.
   * </p>
   * 
   * @param nodeDatumTopicRegex
   *        the regular expression to use
   * @throws IllegalArgumentException
   *         if {@code nodeDatumTopicRegex} is {@literal null}
   */
  public void setNodeDatumTopicRegex(Pattern nodeDatumTopicRegex) {
    if (nodeDatumTopicRegex == null) {
      throw new IllegalArgumentException("nodeDatumTopicRegex must not be null");
    }
    this.nodeDatumTopicRegex = nodeDatumTopicRegex;
  }

  /**
   * Get the user topic regular expression.
   * 
   * @return the regular expression; defaults to {@link #DEFAULT_USER_TOPIC_REGEX}
   */
  public Pattern getUserTopicRegex() {
    return userTopicRegex;
  }

  /**
   * Set the user topic regular expression.
   * 
   * @param userTopicRegex
   *        the regular expression to set
   * @throws IllegalArgumentException
   *         if {@code nodeDatumTopicRegex} is {@literal null}
   */
  public void setUserTopicRegex(Pattern userTopicRegex) {
    if (userTopicRegex == null) {
      throw new IllegalArgumentException("userTopicRegex must not be null");
    }
    this.userTopicRegex = userTopicRegex;
  }

  /**
   * Get the user topic prefix setting.
   * 
   * @return {@literal true} to add a user prefix to all topics
   */
  public boolean isUserTopicPrefix() {
    return userTopicPrefix;
  }

  /**
   * Toggle the user topic prefix setting.
   * 
   * <p>
   * When enabled, all topics will have {@literal user/X} added to the start of each topic, where
   * {@literal X} represents the user ID of the actor. If this is not enabled, then wild card node
   * IDs will not be allowed in any topic.
   * </p>
   * 
   * @param userTopicPrefix
   *        {@literal true} to add a user topic prefix
   */
  public void setUserTopicPrefix(boolean userTopicPrefix) {
    this.userTopicPrefix = userTopicPrefix;
  }

  /**
   * Get the maximum MQTT Qos setting.
   * 
   * @return a maximum Qos to enforce, or {@literal null} for no limit; defaults to {@literal null}
   */
  public Qos getMaxQos() {
    return maxQos;
  }

  /**
   * Set a maximum MQTT Qos setting.
   * 
   * <p>
   * If configured, then publish/subscribe actions with a Qos higher than this will be downgraded to
   * this value.
   * </p>
   * 
   * @param maxQos
   *        the maximum Qos, or {@literal null} for no limit
   */
  public void setMaxQos(Qos maxQos) {
    this.maxQos = maxQos;
  }

}
