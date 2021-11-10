/* ==================================================================
 * SolarFluxDatumPublisher.java - 28/02/2020 2:44:10 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.flux;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionConfig;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.common.mqtt.MqttStats.MqttStat;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Publish datum to SolarFlux.
 * 
 * @author matt
 * @version 2.0
 */
public class SolarFluxDatumPublisher extends BaseMqttConnectionService
		implements DatumProcessor, ServiceLifecycleObserver {

	/**
	 * The MQTT topic template for node data publication.
	 * 
	 * <p>
	 * Accepts the following parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><b>user ID</b> (long)</li>
	 * <li><b>node ID</b> (long)</li>
	 * <li><b>aggregation code</b> (string)</li>
	 * <li><b>source ID</b> (string)</li>
	 * </ol>
	 */
	public static final String NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE = "user/%d/node/%d/datum/%s/%s";

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnet-ocpp";

	private final SolarNodeOwnershipDao supportDao;
	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the MQTT connection factory
	 * @param nodeOwnershipDao
	 *        the support DAO
	 * @param objectMapper
	 *        the mapper for JSON
	 */
	public SolarFluxDatumPublisher(MqttConnectionFactory connectionFactory,
			SolarNodeOwnershipDao nodeOwnershipDao, ObjectMapper objectMapper) {
		super(connectionFactory,
				new MqttStats("OcppSolarFluxPublisher", 500, SolarFluxDatumPublishCountStat.values()));
		this.supportDao = nodeOwnershipDao;
		this.objectMapper = objectMapper;
		getMqttConfig().setUsername(DEFAULT_MQTT_USERNAME);
		try {
			getMqttConfig().setServerUri(new URI(DEFAULT_MQTT_HOST));
		} catch ( URISyntaxException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void serviceDidStartup() {
		init();
	}

	@Override
	public void serviceDidShutdown() {
		shutdown();
	}

	@Override
	public String getPingTestName() {
		return "SolarFlux Datum Publisher";
	}

	@Override
	public boolean isConfigured() {
		MqttConnection conn = connection();
		return conn != null && conn.isEstablished();
	}

	@Override
	public boolean processDatumCollection(Iterable<? extends Identity<GeneralNodeDatumPK>> datum,
			Aggregation aggregation) {
		if ( datum == null ) {
			return true;
		}
		MqttConnection conn = connection();
		MqttConnectionConfig mqttConfig = getMqttConfig();
		if ( conn != null && conn.isEstablished() ) {
			try {
				for ( Identity<GeneralNodeDatumPK> d : datum ) {
					final SolarNodeOwnership ownership = supportDao
							.ownershipForNodeId(d.getId().getNodeId());
					if ( ownership == null ) {
						log.info("Not publishing datum {} to SolarFlux because user ID not available.",
								d);
						continue;
					}
					String topic = topicForDatum(ownership.getUserId(), aggregation, d);
					if ( topic == null ) {
						return false;
					}
					JsonNode jsonData = objectMapper.valueToTree(d);
					byte[] payload = objectMapper.writeValueAsBytes(jsonData);
					if ( log.isDebugEnabled() ) {
						log.debug("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
					}
					if ( log.isTraceEnabled() ) {
						log.trace("Publishing to MQTT topic {}\n{}", topic,
								Base64.getEncoder().encodeToString(payload));
					}

					Future<?> f = conn
							.publish(new BasicMqttMessage(topic, true, getPublishQos(), payload));
					f.get(mqttConfig.getConnectTimeoutSeconds(), TimeUnit.SECONDS);

					MqttStat stat = publishStat(aggregation);
					if ( stat != null ) {
						getMqttStats().incrementAndGet(stat);
					}
				}
				return true;
			} catch ( TimeoutException e ) {
				// don't generate error for timeout; just assume the problem is transient, e.g.
				// network connection lost, and will be resolved eventually
				log.warn("Timeout publishing {} datum to SolarFlux @ {}", aggDisplayName(aggregation),
						mqttConfig.getServerUri());
			} catch ( IOException | InterruptedException | ExecutionException e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.error("Error publishing {} datum to SolarFlux @ {}: {}", aggDisplayName(aggregation),
						mqttConfig.getServerUri(), root.toString(), e);
			}
		} else {
			log.debug("MQTT client not avaialable for publishing SolarFlux {} datum.",
					aggDisplayName(aggregation));
		}
		return false;
	}

	private String aggDisplayName(Aggregation aggregation) {
		return aggregation == Aggregation.None ? "Raw" : aggregation.toString();
	}

	private MqttStat publishStat(Aggregation aggregation) {
		switch (aggregation) {
			case None:
				return SolarFluxDatumPublishCountStat.RawDatumPublished;

			case Hour:
				return SolarFluxDatumPublishCountStat.HourlyDatumPublished;

			case Day:
				return SolarFluxDatumPublishCountStat.DailyDatumPublished;

			case Month:
				return SolarFluxDatumPublishCountStat.MonthlyDatumPublished;

			default:
				return null;
		}
	}

	private String topicForDatum(Long userId, Aggregation aggregation,
			Identity<GeneralNodeDatumPK> datum) {
		Long nodeId = datum.getId().getNodeId();
		String sourceId = datum.getId().getSourceId();
		if ( userId == null || nodeId == null || sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return String.format(NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE, userId, nodeId, aggregation.getKey(),
				sourceId);
	}

}
