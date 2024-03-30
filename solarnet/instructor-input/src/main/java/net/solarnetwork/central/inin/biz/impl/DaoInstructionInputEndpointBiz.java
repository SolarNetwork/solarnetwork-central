/* ==================================================================
 * DaoInstructionInputEndpointBiz.java - 29/03/2024 10:48:10 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.inin.biz.impl;

import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.inin.biz.InstructionInputEndpointBiz;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.CentralInstructionInputUserEvents;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * DAO implementation of {@link InstructionInputEndpointBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoInstructionInputEndpointBiz
		implements InstructionInputEndpointBiz, CentralInstructionInputUserEvents {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final EndpointConfigurationDao endpointDao;
	private final TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;
	private final TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;
	private final Map<String, RequestTransformService> requestTransformServices;
	private final Map<String, ResponseTransformService> responseTransformServices;
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param endpointDao
	 *        the endpoint DAO
	 * @param requestTransformDao
	 *        the request transform DAO
	 * @param responseTransformDao
	 *        the response transform DAO
	 * @param requestTransformServices
	 *        the request transform services
	 * @param responseTransformServices
	 *        the response transform services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoInstructionInputEndpointBiz(SolarNodeOwnershipDao nodeOwnershipDao,
			EndpointConfigurationDao endpointDao,
			TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao,
			TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao,
			Collection<RequestTransformService> requestTransformServices,
			Collection<ResponseTransformService> responseTransformServices) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.requestTransformDao = requireNonNullArgument(requestTransformDao, "requestTransformDao");
		this.responseTransformDao = requireNonNullArgument(responseTransformDao, "responseTransformDao");
		this.requestTransformServices = requireNonNullArgument(requestTransformServices,
				"requestTransformServices").stream()
						.collect(Collectors.toMap(s -> s.getId(), Function.identity()));
		this.responseTransformServices = requireNonNullArgument(responseTransformServices,
				"responseTransformServices").stream()
						.collect(Collectors.toMap(s -> s.getId(), Function.identity()));
	}

	private static LogEventInfo importErrorEvent(String msg, EndpointConfiguration endpoint,
			TransformConfiguration requestXform, TransformConfiguration responseXform,
			MimeType contentType, MimeType outputType, Map<String, String> parameters) {
		var eventData = new LinkedHashMap<>(8);
		eventData.put(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId());
		eventData.put(REQ_TRANSFORM_ID_DATA_KEY, endpoint.getRequestTransformId());
		if ( requestXform != null ) {
			eventData.put(REQ_TRANSFORM_SERVICE_ID_DATA_KEY, requestXform.getServiceIdentifier());
		}
		eventData.put(CONTENT_TYPE_DATA_KEY, contentType.toString());
		eventData.put(RES_TRANSFORM_ID_DATA_KEY, endpoint.getResponseTransformId());
		if ( responseXform != null ) {
			eventData.put(RES_TRANSFORM_SERVICE_ID_DATA_KEY, responseXform.getServiceIdentifier());
		}
		eventData.put(OUTPUT_TYPE_DATA_KEY, outputType.toString());

		if ( parameters != null ) {
			eventData.put(PARAMETERS_DATA_KEY, parameters);
		}
		return event(INSTRUCTION_TAGS, msg, getJSONString(eventData, null), ERROR_TAG);
	}

	@Override
	public Collection<NodeInstruction> importInstructions(Long userId, UUID endpointId,
			MimeType contentType, InputStream in, Map<String, String> parameters) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Set the user event appender service.
	 *
	 * @param userEventAppenderBiz
	 *        the service to set
	 */
	public void setUserEventAppenderBiz(UserEventAppenderBiz userEventAppenderBiz) {
		this.userEventAppenderBiz = userEventAppenderBiz;
	}

}
