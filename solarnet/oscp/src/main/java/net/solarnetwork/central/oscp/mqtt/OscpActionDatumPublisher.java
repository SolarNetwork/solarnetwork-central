/* ==================================================================
 * OscpActionDatumPublisher.java - 10/10/2022 1:44:18 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.mqtt;

import static java.lang.String.format;
import static net.solarnetwork.central.datum.flux.SolarFluxDatumPublisher.NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Publish OSCP action events as datum.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpActionDatumPublisher extends MqttJsonPublisher<DatumPublishEvent>
		implements Consumer<DatumPublishEvent> {

	private static final Logger log = LoggerFactory.getLogger(OscpActionDatumPublisher.class);

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final DatumEntityDao datumDao;

	public OscpActionDatumPublisher(SolarNodeOwnershipDao nodeOwnershipDao, DatumEntityDao datumDao,
			String name, ObjectMapper objectMapper, Function<DatumPublishEvent, String> topicFn,
			boolean retained, MqttQos publishQos) {
		super(name, objectMapper, (event) -> {
			Long nodeId = event.nodeId();
			final SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(nodeId);
			if ( ownership == null ) {
				log.info("Not publishing event {} because user ID not available.", event);
				return null;
			}
			Long userId = event.userId();
			if ( !ownership.getUserId().equals(userId) ) {
				log.info("Not publishing event {} because node {} not owned by user {}.", event, nodeId,
						userId);
				return null;
			}
			return topicForDatum(userId, nodeId, event.sourceId());
		}, retained, publishQos);
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
	}

	public static String topicForDatum(Long userId, Long nodeId, String sourceId) {
		if ( userId == null || nodeId == null || sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return format(NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE, userId, nodeId, Aggregation.None, sourceId);
	}

	@Override
	public void accept(DatumPublishEvent event) {
		GeneralNodeDatum datum = event.datum();
		if ( datum == null ) {
			return;
		}
		Long nodeId = event.nodeId();
		final SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(nodeId);
		if ( ownership == null ) {
			log.info("Not publishing event {} because user ID not available.", event);
			return;
		}
		Long userId = event.userId();
		if ( !ownership.getUserId().equals(userId) ) {
			log.info("Not publishing event {} because node {} not owned by user {}.", event, nodeId,
					userId);
			return;
		}
		String sourceId = event.sourceId();
		if ( event.publishToSolarIn() ) {
			GeneralNodeDatum copy = datum.clone();
			copy.setNodeId(nodeId);
			copy.setSourceId(sourceId);
			datumDao.store(copy);
		}
		if ( event.publishToSolarFlux() ) {
			publish(event, sourceId);
		}
	}

}
