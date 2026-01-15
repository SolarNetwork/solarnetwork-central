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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.flux.dao.FluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.util.StatTracker;
import tools.jackson.databind.ObjectMapper;

/**
 * Publish datum to SolarFlux.
 *
 * @author matt
 * @version 3.0
 */
public class SolarFluxDatumPublisher extends MqttJsonPublisher<Identity<GeneralNodeDatumPK>>
		implements DatumProcessor {

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

	/** The {@code errorLogLimitMs} property default value. */
	public static final long ERROR_LOG_LIMIT_MS_DEFAULT = 60_000L;

	private final SolarNodeOwnershipDao supportDao;
	private final FluxPublishSettingsDao fluxPublishSettingsDao;
	private long errorLogLimitMs = ERROR_LOG_LIMIT_MS_DEFAULT;

	private long lastErrorTime = 0; // ignoring thread safety for performance

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the support DAO
	 * @param fluxPublishSettingsDao
	 *        the SolarFlux publish settings DAO
	 * @param objectMapper
	 *        the mapper for JSON
	 */
	public SolarFluxDatumPublisher(SolarNodeOwnershipDao nodeOwnershipDao,
			FluxPublishSettingsDao fluxPublishSettingsDao, ObjectMapper objectMapper) {
		this(nodeOwnershipDao, fluxPublishSettingsDao, objectMapper, false, MqttQos.AtMostOnce);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the support DAO
	 * @param fluxPublishSettingsDao
	 *        the SolarFlux publish settings DAO
	 * @param objectMapper
	 *        the mapper for JSON
	 * @param retained
	 *        {@literal true} to publish each message as retained
	 * @param publishQos
	 *        the publish QoS
	 */
	public SolarFluxDatumPublisher(SolarNodeOwnershipDao nodeOwnershipDao,
			FluxPublishSettingsDao fluxPublishSettingsDao, ObjectMapper objectMapper, boolean retained,
			MqttQos publishQos) {
		super("SolarFlux Datum Publisher", objectMapper, _ -> {
			throw new UnsupportedOperationException();
		}, retained, publishQos);
		this.supportDao = requireNonNullArgument(nodeOwnershipDao, "supportDao");
		this.fluxPublishSettingsDao = requireNonNullArgument(fluxPublishSettingsDao,
				"fluxPublishSettingsDao");
	}

	@Override
	public boolean isConfigured() {
		return isConnected();
	}

	@Override
	public boolean processDatumCollection(Iterable<? extends Identity<GeneralNodeDatumPK>> datum,
			Aggregation aggregation) {
		if ( datum == null ) {
			return true;
		}
		try {
			final int timeout = getPublishTimeoutSeconds();
			for ( Identity<GeneralNodeDatumPK> d : datum ) {
				final SolarNodeOwnership ownership = supportDao
						.ownershipForNodeId(d.getId().getNodeId());
				if ( ownership == null ) {
					log.info("Not publishing datum {} to SolarFlux because user ID not available.", d);
					continue;
				}

				Long nodeId = d.getId().getNodeId();
				String sourceId = d.getId().getSourceId();
				if ( nodeId == null || sourceId == null || sourceId.isEmpty() ) {
					continue;
				}

				// if datum is for aggregate, check agg publish settings
				final FluxPublishSettings pubSettings;
				if ( aggregation != Aggregation.None ) {
					pubSettings = fluxPublishSettingsDao
							.nodeSourcePublishConfiguration(ownership.getUserId(), nodeId, sourceId);
					if ( pubSettings == null || !pubSettings.isPublish() ) {
						continue;
					}
				} else {
					pubSettings = null;
				}
				if ( sourceId.startsWith("/") ) {
					sourceId = sourceId.substring(1);
				}
				final String topic = String.format(NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE,
						ownership.getUserId(), nodeId, aggregation.getKey(), sourceId);

				Future<?> f = publish(d, topic, (pubSettings != null ? pubSettings.isRetain() : false),
						getPublishQos());
				if ( timeout > 0 ) {
					try {
						f.get(timeout, TimeUnit.SECONDS);
					} catch ( ExecutionException e ) {
						if ( e.getCause() instanceof IllegalArgumentException iae ) {
							// assume too large, etc
							log.warn("Problem publishing {} datum to SolarFlux ({}); datum: {}",
									aggDisplayName(aggregation), iae.getMessage(), d);
							continue;
						} else {
							throw e;
						}
					}
				}

				SolarFluxDatumPublishCountStat stat = publishStat(aggregation);
				if ( stat != null ) {
					StatTracker stats = getMqttStats();
					if ( stats != null ) {
						stats.increment(stat);
					}
				}
			}
			return true;
		} catch ( TimeoutException e ) {
			// don't generate error for timeout; just assume the problem is transient, e.g.
			// network connection lost, and will be resolved eventually
			log.warn("Timeout publishing {} datum to SolarFlux", aggDisplayName(aggregation));
		} catch ( InterruptedException | ExecutionException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( errorLogLimitMs > 0 ) {
				final long now = System.currentTimeMillis();
				final long tdiff = errorLogLimitMs > 0 ? now - lastErrorTime : lastErrorTime;
				if ( tdiff >= lastErrorTime ) {
					logPublishError(e, root, aggregation);
					lastErrorTime = now;
				} else {
					log.debug("Problem publishing {} datum to SolarFlux: {}",
							aggDisplayName(aggregation), root.getMessage());
				}
			} else {
				logPublishError(e, root, aggregation);
			}
		}
		return false;
	}

	private void logPublishError(Throwable e, Throwable root, Aggregation aggregation) {
		if ( root instanceof RemoteServiceException ) {
			log.warn("Problem publishing {} datum to SolarFlux: {}", aggDisplayName(aggregation),
					root.getMessage());
		} else {
			log.error("Problem publishing {} datum to SolarFlux: {}", aggDisplayName(aggregation),
					root.toString(), e);
		}
	}

	private static String aggDisplayName(Aggregation aggregation) {
		return aggregation == Aggregation.None ? "Raw" : aggregation.toString();
	}

	private static SolarFluxDatumPublishCountStat publishStat(Aggregation aggregation) {
		return switch (aggregation) {
			case None -> SolarFluxDatumPublishCountStat.RawDatumPublished;
			case Hour -> SolarFluxDatumPublishCountStat.HourlyDatumPublished;
			case Day -> SolarFluxDatumPublishCountStat.DailyDatumPublished;
			case Month -> SolarFluxDatumPublishCountStat.MonthlyDatumPublished;
			default -> null;
		};
	}

	/**
	 * Get the error log limit.
	 *
	 * @return the milliseconds to limit error log message to, or 0 for no
	 *         limit; defaults to {@link #ERROR_LOG_LIMIT_MS_DEFAULT}
	 */
	public long getErrorLogLimitMs() {
		return errorLogLimitMs;
	}

	/**
	 * Set the error log limit.
	 *
	 * @param errorLogLimitMs
	 *        the milliseconds to limit error log message to, or 0 for no limit
	 */
	public void setErrorLogLimitMs(long errorLogLimitMs) {
		this.errorLogLimitMs = errorLogLimitMs;
	}

}
