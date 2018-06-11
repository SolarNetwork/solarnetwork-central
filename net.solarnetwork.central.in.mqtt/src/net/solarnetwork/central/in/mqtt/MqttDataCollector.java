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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttStats.Counts;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.Identifiable;
import net.solarnetwork.support.SSLService;
import net.solarnetwork.util.OptionalService;

/**
 * MQTT implementation of upload service.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttDataCollector implements MqttCallbackExtended, Identifiable {

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

	/** The {@link GeneralNodeDatum} or {@link GeneralLocationDatum} type. */
	public static final String GENERAL_NODE_DATUM_TYPE = "datum";

	/**
	 * The JSON field name for a location ID on a {@link GeneralLocationDatum}
	 * value.
	 */
	public static final String LOCATION_ID_FIELD = "locationId";

	private static final long RETRY_CONNECT_DELAY = 2000L;
	private static final long MAX_CONNECT_DELAY_MS = 120000L;

	private final ObjectMapper objectMapper;
	private final DataCollectorBiz dataCollectorBiz;
	private final OptionalService<InstructorBiz> instructorBizRef;
	private final OptionalService<SSLService> sslServiceRef;
	private final AtomicReference<IMqttClient> clientRef;
	private final MqttStats stats;

	private String uid = UUID.randomUUID().toString();
	private String groupUid;
	private String displayName;

	private boolean retryConnect;
	private String serverUri;
	private String clientId;
	private String username;
	private String password;
	private String persistencePath = "var/mqtt-solarin";

	private Thread connectThread = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public MqttDataCollector(ObjectMapper objectMapper, DataCollectorBiz dataCollectorBiz,
			OptionalService<InstructorBiz> instructorBiz, OptionalService<SSLService> sslService,
			boolean retryConnect) {
		this(objectMapper, dataCollectorBiz, instructorBiz, sslService, null, null, retryConnect);
	}

	public MqttDataCollector(ObjectMapper objectMapper, DataCollectorBiz dataCollectorBiz,
			OptionalService<InstructorBiz> instructorBiz, OptionalService<SSLService> sslService,
			String serverUri, String clientId, boolean retryConnect) {
		super();
		this.objectMapper = objectMapper;
		this.dataCollectorBiz = dataCollectorBiz;
		this.instructorBizRef = instructorBiz;
		this.sslServiceRef = sslService;
		this.serverUri = serverUri;
		this.clientId = clientId;
		this.retryConnect = retryConnect;
		this.clientRef = new AtomicReference<IMqttClient>();
		this.stats = new MqttStats(serverUri, 500);
	}

	/**
	 * Immediately connect.
	 */
	public void init() {
		if ( retryConnect ) {
			synchronized ( clientRef ) {
				if ( connectThread != null ) {
					return;
				}
				Runnable connector = new Runnable() {

					final AtomicLong sleep = new AtomicLong(0);

					@Override
					public void run() {
						final long sleepMs = sleep.get();
						if ( sleepMs > 0 ) {
							try {
								Thread.sleep(sleepMs);
							} catch ( InterruptedException e ) {
								// ignore
							}
						}
						try {
							IMqttClient client = setupClient();
							if ( client != null ) {
								synchronized ( connectThread ) {
									connectThread = null;
								}
								return;
							}
						} catch ( RuntimeException e ) {
							// ignore
						}
						long delay = sleep.accumulateAndGet(sleep.get() / RETRY_CONNECT_DELAY,
								(c, s) -> {
									long d = (s * 2) * RETRY_CONNECT_DELAY;
									if ( d == 0 ) {
										d = RETRY_CONNECT_DELAY;
									}
									if ( d > MAX_CONNECT_DELAY_MS ) {
										d = MAX_CONNECT_DELAY_MS;
									}
									return d;
								});
						log.info("Failed to connect to MQTT server {}, will try again in {}s", serverUri,
								TimeUnit.MILLISECONDS.toSeconds(delay));
						synchronized ( connectThread ) {
							connectThread = new Thread(this, "MQTT-connect " + serverUri);
							connectThread.setDaemon(true);
							connectThread.start();
						}
					}
				};
				connectThread = new Thread(connector, "MQTT-connect " + serverUri);
				connectThread.setDaemon(true);
				connectThread.start();
			}
		} else {
			setupClient();
		}
	}

	private IMqttClient setupClient() {
		IMqttClient client = null;
		IMqttClient oldClient = null;
		try {
			client = createClient();
			if ( client != null ) {
				MqttConnectOptions connOptions = new MqttConnectOptions();
				connOptions.setCleanSession(false);
				connOptions.setAutomaticReconnect(true);
				if ( username != null && !username.isEmpty() ) {
					connOptions.setUserName(username);
				}
				if ( password != null && !password.isEmpty() ) {
					connOptions.setPassword(password.toCharArray());
				}

				final SSLService sslService = (sslServiceRef != null ? sslServiceRef.service() : null);
				if ( sslService != null ) {
					connOptions.setSocketFactory(sslService.getSSLSocketFactory());
				}

				client.connect(connOptions);

				subscribeToTopics(client);

				oldClient = clientRef.getAndSet(client);
			}
		} catch ( MqttException e ) {
			log.error("Error creating MQTT client", e);
			try {
				if ( client != null ) {
					client.close();
				}
			} catch ( MqttException e2 ) {
				// ignore
			}
			client = null;
		}
		if ( oldClient != null ) {
			try {
				oldClient.close();
			} catch ( MqttException e ) {
				log.warn("Error closing MQTT client: {}", e.getMessage());
			}
		}
		return client;
	}

	private IMqttClient createClient() throws MqttException {
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
		String serverUri = (useSsl ? "ssl" : "tcp") + "://" + uri.getHost()
				+ (port > 0 ? ":" + uri.getPort() : "");

		Path p = Paths.get(persistencePath, DigestUtils.md5DigestAsHex(uid.getBytes()));
		if ( !Files.isDirectory(p) ) {
			try {
				Files.createDirectories(p);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Unable to create MQTT persistance directory [" + p + "]: " + e.getMessage(), e);
			}
		}
		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(p.toString());
		MqttClient c = null;
		c = new MqttClient(serverUri, clientId, persistence);
		c.setCallback(this);
		return c;
	}

	private IMqttClient client() {
		IMqttClient client = clientRef.get();
		if ( client == null ) {
			client = setupClient();
		}
		return client;
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
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								Thread.sleep(20);
							} catch ( InterruptedException e ) {
								// just continue
							}
							try {
								client.disconnect();
							} catch ( MqttException e ) {
								log.warn("Error disconnecting from MQTT server @ {}: {}", serverURI,
										e.getMessage());
							}
						}
					}, "MQTT-disconnect " + serverUri);
					t.setDaemon(true);
					t.start();
				}
			}
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		stats.incrementAndGet(Counts.MessagesReceived);
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
		} catch ( IOException e ) {
			log.debug("Communication error handling message on MQTT topic {}", topic, e);
		} catch ( RepeatableTaskException e ) {
			if ( log.isDebugEnabled() ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.debug("RepeatableTaskException caused by: " + root.getMessage());
			}
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
		}
	}

	private void handleInstructionStatus(final JsonNode node) {
		stats.incrementAndGet(Counts.InstructionStatusReceived);
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
		stats.incrementAndGet(Counts.NodeDatumReceived);
		try {
			GeneralNodeDatum d = objectMapper.treeToValue(node, GeneralNodeDatum.class);
			d.setNodeId(nodeId);
			dataCollectorBiz.postGeneralNodeDatum(singleton(d));
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralNodeDatum: {}", e.getMessage());
		}
	}

	private void handleGeneralLocationDatum(final JsonNode node) {
		stats.incrementAndGet(Counts.LocationDatumReceived);
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

	@Override
	public String getUid() {
		return uid;
	}

	/**
	 * Set the service unique ID.
	 * 
	 * @param uid
	 *        the unique ID
	 */
	public void setUid(String uid) {
		if ( uid == null ) {
			throw new IllegalArgumentException("uid value must not be null");
		}
		this.uid = uid;
		this.stats.setUid(uid);
	}

	@Override
	public String getGroupUid() {
		return groupUid;
	}

	/**
	 * Set the service group unique ID.
	 * 
	 * @param groupUid
	 *        the group ID
	 */
	public void setGroupUid(String groupUid) {
		this.groupUid = groupUid;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the service display name.
	 * 
	 * @param displayName
	 *        the display name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Set the MQTT server URI to connect to.
	 * 
	 * <p>
	 * This should be in the form <code>mqtt[s]://host:port</code>.
	 * </p>
	 * 
	 * @param serverUri
	 *        the URI to connect to
	 */
	public void setServerUri(String serverUri) {
		this.serverUri = serverUri;
	}

	/**
	 * Set the MQTT client ID to use.
	 * 
	 * @param clientId
	 *        the client ID
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Set the MQTT username to authenticate as.
	 * 
	 * @param username
	 *        the username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the MQTT password to authenticate with.
	 * 
	 * @param password
	 *        the password
	 */
	public void setPassword(String password) {
		this.password = password;
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

	/**
	 * Set flag to retry connecting during startup.
	 * 
	 * <p>
	 * This affects how the {@link #init()} method handles connecting to the
	 * MQTT broker. When {@literal true} then the connection will happen in a
	 * background thread, and this service will keep re-trying to connect if the
	 * connection fails. It will delay re-try attempts in an increasing fashion,
	 * up to a limit of 2 minutes. When this property is {@literal false} then
	 * the connection will happen in the calling thread and this service will
	 * <b>not</b> attempt to re-try the connection if the connection fails.
	 * </p>
	 * 
	 * <p>
	 * Note that once connected to a broker, this service <b>will</b>
	 * automatically re-connect to that broker if the connection closes for any
	 * reason.
	 * </p>
	 * 
	 * @param retryConnect
	 *        {@literal true} to re-try connecting to the MQTT broker during
	 *        startup if the connection attempt fails, {@literal false} to only
	 *        try once
	 */
	public void setRetryConnect(boolean retryConnect) {
		this.retryConnect = retryConnect;
	}

	/**
	 * Set the statistic log frequency.
	 * 
	 * @param frequency
	 *        the statistic log frequency
	 */
	public void setStatLogFrequency(int frequency) {
		if ( frequency < 1 ) {
			frequency = 1;
		}
		stats.setLogFrequency(frequency);
	}

}
