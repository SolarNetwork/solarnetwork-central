/* ==================================================================
 * MqttDataCollector.java - 10/06/2018 12:57:43 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.in.mqtt;

import static java.util.Collections.singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.util.OptionalService;

/**
 * MQTT implementation of upload service.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttDataCollector implements MqttCallbackExtended {

	/** The MQTT topic template for node instruction publication. */
	public static final String NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

	/** The MQTT topic template for node data subscription. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%s/datum";

	/**
	 * A regular expression that matches node topics and returns node ID and
	 * sub-topic groups.
	 */
	public static final Pattern NODE_TOPIC_REGEX = Pattern.compile("node/(\\d+)/(.*)");

	/** The JSON field name for an "object type". */
	public static final String OBJECT_TYPE_FIELD = "__type__";

	/** The InstructionStatus type. */
	public static final String INSTRUCTION_STATUS_TYPE = "InstructionStatus";

	/** The NodeControlInfo type. */
	public static final String NODE_CONTROL_INFO_TYPE = "NodeControlInfo";

	/** The {@link GeneralNodeDatum} or {@link GeneralLocationDatum} type. */
	public static final String GENERAL_NODE_DATUM_TYPE = "datum";

	/**
	 * The JSON field name for a location ID on a {@link GeneralLocationDatum}
	 * value.
	 */
	public static final String LOCATION_ID_FIELD = "locationId";

	private static final long MAX_CONNECT_DELAY_MS = 120000L;

	private final ObjectMapper objectMapper;
	private final DataCollectorBiz dataCollectorBiz;
	private final OptionalService<InstructorBiz> instructorBizRef;
	private final TaskScheduler taskScheduler;
	private final AtomicReference<IMqttClient> clientRef;
	private final String serverUri;
	private final String clientId;

	private String persistencePath = "var/mqtt";

	private final Logger log = LoggerFactory.getLogger(getClass());

	public MqttDataCollector(ObjectMapper objectMapper, DataCollectorBiz dataCollectorBiz,
			OptionalService<InstructorBiz> instructorBiz, TaskScheduler taskScheduler, String serverUri,
			String clientId) {
		super();
		this.objectMapper = objectMapper;
		this.dataCollectorBiz = dataCollectorBiz;
		this.instructorBizRef = instructorBiz;
		this.taskScheduler = taskScheduler;
		this.serverUri = serverUri;
		this.clientId = clientId;
		this.clientRef = new AtomicReference<IMqttClient>();
	}

	/**
	 * Immediately connect.
	 */
	public void init() {
		if ( taskScheduler != null ) {
			final AtomicLong sleep = new AtomicLong(2000);
			taskScheduler.schedule(new Runnable() {

				@Override
				public void run() {
					try {
						IMqttClient client = client();
						if ( client != null ) {
							return;
						}
					} catch ( RuntimeException e ) {
						// ignore
					}
					long delay = sleep.accumulateAndGet(sleep.get() / 2000, (c, s) -> {
						long d = (s * 2) * 2000;
						if ( d > MAX_CONNECT_DELAY_MS ) {
							d = MAX_CONNECT_DELAY_MS;
						}
						return d;
					});
					log.info("Failed to connect to MQTT server {}, will try again in {}s", serverUri,
							delay / 1000);
					taskScheduler.schedule(this, new Date(System.currentTimeMillis() + delay));
				}
			}, new Date(System.currentTimeMillis() + sleep.get()));
		} else {
			client();
		}
	}

	private IMqttClient client() {
		IMqttClient client = clientRef.get();
		if ( client != null ) {
			return client;
		}

		URI uri;
		try {
			uri = new URI(serverUri);
		} catch ( URISyntaxException e1 ) {
			log.error("Invalid MQTT URL: " + serverUri);
			return null;
		}

		int port = uri.getPort();
		String scheme = uri.getScheme();
		boolean useSsl = (port == 8883 || "mqtts".equalsIgnoreCase(scheme)
				|| "ssl".equalsIgnoreCase(scheme));

		final String serverUri = (useSsl ? "ssl" : "tcp") + "://" + uri.getHost()
				+ (port > 0 ? ":" + uri.getPort() : "");

		MqttConnectOptions connOptions = new MqttConnectOptions();
		connOptions.setCleanSession(false);
		connOptions.setAutomaticReconnect(true);

		/*-
		final SSLService sslService = (sslServiceOpt != null ? sslServiceOpt.service() : null);
		if ( useSsl && sslService != null ) {
			connOptions.setSocketFactory(sslService.getSolarInSocketFactory());
		}
		*/

		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistencePath);
		MqttClient c = null;
		try {
			c = new MqttClient(serverUri, clientId, persistence);
			c.setCallback(this);
			if ( clientRef.compareAndSet(null, c) ) {
				c.connect(connOptions);
				subscribeToTopics(c);
				return c;
			}
		} catch ( MqttException e ) {
			log.warn("Error configuring MQTT client: {}", e.getMessage());
			if ( c != null ) {
				clientRef.compareAndSet(c, null);
			}
		}
		return null;
	}

	private void subscribeToTopics(IMqttClient client) throws MqttException {
		final String datumTopics = String.format(NODE_DATUM_TOPIC_TEMPLATE, "+");
		client.subscribe(datumTopics);
	}

	@Override
	public void connectionLost(Throwable cause) {
		IMqttClient client = clientRef.get();
		log.info("Connection to MQTT server @ {} lost: {}",
				(client != null ? client.getServerURI() : "N/A"), cause.getMessage());
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// nothing to do
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		log.info("{} to MQTT server @ {}", (reconnect ? "Reconnected" : "Connected"), serverURI);
		if ( reconnect ) {
			// re-subscribe
			final IMqttClient client = clientRef.get();
			if ( client != null ) {
				try {
					subscribeToTopics(client);
				} catch ( MqttException e ) {
					log.error("Error subscribing to node topics: {}", e.getMessage(), e);
					if ( taskScheduler != null ) {
						taskScheduler.schedule(new Runnable() {

							@Override
							public void run() {
								try {
									client.disconnect();
								} catch ( MqttException e ) {
									log.warn("Error disconnecting from MQTT server @ {}: {}", serverURI,
											e.getMessage());
								}
							}
						}, new Date(System.currentTimeMillis() + 20));
					}
				}
			}
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		Matcher m = NODE_TOPIC_REGEX.matcher(topic);
		if ( !m.matches() ) {
			log.info("Unknown topic: {}" + topic);
			return;
		}
		final Long nodeId = Long.valueOf(m.group(1));
		final String subTopic = m.group(2);
		assert "datum".equals(subTopic);

		// assume node security role
		SecurityUtils.becomeNode(nodeId);

		try {
			JsonNode root = objectMapper.readTree(message.getPayload());
			if ( root.isObject() ) {
				handleNode(nodeId, root);
			}
		} catch ( RepeatableTaskException e ) {
			if ( log.isDebugEnabled() ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.debug("RepeatableTaskException caused by: " + root.getMessage());
			}
		} finally {
		}
	}

	private void handleNode(final Long nodeId, final JsonNode node) {
		String nodeType = getStringFieldValue(node, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE);
		if ( GENERAL_NODE_DATUM_TYPE.equalsIgnoreCase(nodeType) ) {
			// if we have a location ID, this is actually a GeneralLocationDatum
			final JsonNode locId = node.get(LOCATION_ID_FIELD);
			if ( locId != null && locId.isNumber() ) {
				handleGeneralLocationDatum(node);
			} else {
				handleGeneralNodeDatum(nodeId, node);
			}
		} else if ( INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(nodeType) ) {
			handleInstructionStatus(node);
		} else if ( NODE_CONTROL_INFO_TYPE.equalsIgnoreCase(nodeType) ) {
			handleNodeControlInfo(nodeId, node);
		}
	}

	private void handleNodeControlInfo(final Long nodeId, final JsonNode node) {
		String controlId = getStringFieldValue(node, "controlId", null);
		String propertyName = getStringFieldValue(node, "propertyName", null);
		String value = getStringFieldValue(node, "value", null);
		String type = getStringFieldValue(node, "type", null);
		if ( type != null && value != null ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
			datum.setSamples(samples);
			final JsonNode createdNode = node.get("created");
			if ( createdNode != null && createdNode.isNumber() ) {
				datum.setCreated(new DateTime(createdNode.asLong()));
			} else {
				datum.setCreated(new DateTime());
			}
			datum.setNodeId(nodeId);
			datum.setSourceId(controlId);
			if ( propertyName == null ) {
				propertyName = "val";
			}

			NodeControlPropertyType t = NodeControlPropertyType.valueOf(type);
			switch (t) {
				case Boolean:
					if ( value.length() > 0 && (value.equals("1") || value.equalsIgnoreCase("yes")
							|| value.equalsIgnoreCase("true")) ) {
						samples.putStatusSampleValue(propertyName, 1);
					} else {
						samples.putStatusSampleValue(propertyName, 0);
					}
					break;

				case Integer:
					samples.putStatusSampleValue(propertyName, Integer.valueOf(value));
					break;

				case Float:
				case Percent:
					samples.putStatusSampleValue(propertyName, Float.valueOf(value));
					break;

				case String:
					samples.putStatusSampleValue(propertyName, value);

				default:
					break;

			}
			dataCollectorBiz.postGeneralNodeDatum(singleton(datum));
		}
	}

	private void handleInstructionStatus(final JsonNode node) {
		String instructionId = getStringFieldValue(node, "instructionId", null);
		String status = getStringFieldValue(node, "status", null);
		Map<String, Object> resultParams = JsonUtils.getStringMapFromTree(node.get("resultParameters"));
		InstructorBiz biz = (instructorBizRef != null ? instructorBizRef.service() : null);
		if ( instructionId != null && status != null && biz != null ) {
			Long id = Long.valueOf(instructionId);
			InstructionState state = InstructionState.valueOf(status);
			biz.updateInstructionState(id, state, resultParams);
		}
	}

	private void handleGeneralNodeDatum(final Long nodeId, final JsonNode node) {
		try {
			GeneralNodeDatum d = objectMapper.treeToValue(node, GeneralNodeDatum.class);
			d.setNodeId(nodeId);
			dataCollectorBiz.postGeneralNodeDatum(singleton(d));
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralNodeDatum: {}", e.getMessage());
		}
	}

	private void handleGeneralLocationDatum(final JsonNode node) {
		try {
			GeneralLocationDatum d = objectMapper.treeToValue(node, GeneralLocationDatum.class);
			dataCollectorBiz.postGeneralLocationDatum(singleton(d));
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralLocationDatum: {}", e.getMessage());
		}
	}

	private String getStringFieldValue(JsonNode node, String fieldName, String placeholder) {
		JsonNode child = node.get(fieldName);
		return (child == null ? placeholder : child.asText());
	}

	/**
	 * Set the path to store persisted MQTT data.
	 * 
	 * <p>
	 * This directory will be created if it does not already exist.
	 * </p>
	 * 
	 * @param persistencePath
	 *        the path to set; defaults to {@literal var/mqtt}
	 */
	public void setPersistencePath(String persistencePath) {
		this.persistencePath = persistencePath;
	}
}
