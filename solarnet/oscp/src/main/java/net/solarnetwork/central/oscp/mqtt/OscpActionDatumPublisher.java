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
import static net.solarnetwork.domain.datum.Aggregation.None;
import java.util.Collection;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.OwnedGeneralNodeDatum;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.common.mqtt.MqttQos;

/**
 * Publish OSCP action events as datum.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpActionDatumPublisher extends MqttJsonPublisher<OwnedGeneralNodeDatum>
		implements Consumer<DatumPublishEvent> {

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This sets {@code retained} to {@literal false} and {@code publishQos} to
	 * {@code AtMostOnce}.
	 * </p>
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param name
	 *        the display name of this service
	 * @param objectMapper
	 *        the object mapper
	 * @param topicFn
	 *        the function to generate the MQTT topic from a datum
	 */
	public OscpActionDatumPublisher(ObjectMapper objectMapper) {
		this(objectMapper, false, MqttQos.AtMostOnce);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param name
	 *        the display name of this service
	 * @param objectMapper
	 *        the object mapper
	 * @param retained
	 *        the MQTT retained flag to use
	 * @param publishQos
	 *        the MQTT QoS level to use
	 * @param
	 */
	public OscpActionDatumPublisher(ObjectMapper objectMapper, boolean retained, MqttQos publishQos) {
		super("OSCP Action Datum Publisher", objectMapper, (d) -> {
			return topicForDatum(d);
		}, retained, publishQos);
	}

	private static String topicForDatum(OwnedGeneralNodeDatum d) {
		Long userId = d.getUserId();
		Long nodeId = d.getNodeId();
		String sourceId = d.getSourceId();
		if ( nodeId == null || sourceId == null || sourceId.isBlank() ) {
			return null;
		}
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return format(NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE, userId, nodeId, None.getKey(), sourceId);
	}

	@Override
	public void accept(DatumPublishEvent event) {
		Collection<OwnedGeneralNodeDatum> datum = event.datum();
		if ( datum == null || !event.publishToSolarFlux() ) {
			return;
		}
		Long nodeId = event.nodeId();
		Long userId = event.userId();
		if ( datum != null ) {
			for ( OwnedGeneralNodeDatum d : datum ) {
				if ( (d.getNodeId() != null && !nodeId.equals(d.getNodeId()))
						|| !userId.equals(d.getUserId()) ) {
					// not allowed
					continue;
				}
				apply(d);
			}
		}
	}

}
