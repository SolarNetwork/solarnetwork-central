/* ==================================================================
 * DaoDatumInputEndpointBiz.java - 24/02/2024 8:29:12 am
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

package net.solarnetwork.central.din.biz.impl;

import static net.solarnetwork.central.biz.UserEventAppenderBiz.addUserEvent;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MimeType;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.din.biz.DatumInputEndpointBiz;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.InputDataEntityDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.domain.CentralDinUserEvents;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;

/**
 * DAO implementation of {@link DatumInputEndpointBiz}.
 *
 * @author matt
 * @version 1.7
 */
public class DaoDatumInputEndpointBiz implements DatumInputEndpointBiz, CentralDinUserEvents {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final EndpointConfigurationDao endpointDao;
	private final TransformConfigurationDao transformDao;
	private final DatumWriteOnlyDao datumDao;
	private final InputDataEntityDao previousInputDataDao;
	private final Map<String, TransformService> transformServices;
	private DatumProcessor fluxPublisher;
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param endpointDao
	 *        the endpoint DAO
	 * @param transformDao
	 *        the transform DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param transformServices
	 *        the transform services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoDatumInputEndpointBiz(SolarNodeOwnershipDao nodeOwnershipDao,
			EndpointConfigurationDao endpointDao, TransformConfigurationDao transformDao,
			DatumWriteOnlyDao datumDao, InputDataEntityDao previousInputDataDao,
			Collection<TransformService> transformServices) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.transformDao = requireNonNullArgument(transformDao, "transformDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.previousInputDataDao = requireNonNullArgument(previousInputDataDao, "previousInputDataDao");
		this.transformServices = requireNonNullArgument(transformServices, "transformServices").stream()
				.collect(Collectors.toMap(Identity::getId, Function.identity()));
	}

	private static final MimeType JSON_TYPE = MimeType.valueOf("application/json");
	private static final MimeType TEXT_TYPE = MimeType.valueOf("text/*");

	private static LogEventInfo importErrorEvent(String msg, EndpointConfiguration endpoint,
			TransformConfiguration xform, MimeType contentType, byte[] content, byte[] previousContent,
			Map<String, String> parameters) {
		var eventData = new LinkedHashMap<>(8);
		eventData.put(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId());
		eventData.put(TRANSFORM_ID_DATA_KEY, endpoint.getTransformId());
		eventData.put(TRANSFORM_SERVICE_ID_DATA_KEY, xform.getServiceIdentifier());
		eventData.put(CONTENT_TYPE_DATA_KEY, contentType.toString());

		if ( content != null && content.length > 0 ) {
			String value = eventContentValue(contentType, content);
			eventData.put(CONTENT_DATA_KEY, value);
		}

		if ( previousContent != null && previousContent.length > 0 ) {
			String value = eventContentValue(contentType, previousContent);
			eventData.put(PREVIOUS_CONTENT_DATA_KEY, value);
		}

		if ( parameters != null ) {
			eventData.put(PARAMETERS_DATA_KEY, parameters);
		}
		return event(DATUM_TAGS, msg, getJSONString(eventData, null), ERROR_TAG);
	}

	private static String eventContentValue(MimeType contentType, byte[] content) {
		if ( content == null ) {
			return null;
		}
		String value;
		if ( JSON_TYPE.isCompatibleWith(contentType) || TEXT_TYPE.isCompatibleWith(contentType) ) {
			value = new String(content, contentType.getCharset() != null ? contentType.getCharset()
					: StandardCharsets.UTF_8);
		} else {
			value = Base64.getEncoder().encodeToString(content);
		}
		return value;
	}

	@Override
	public Collection<DatumId> importDatum(Long userId, UUID endpointId, MimeType contentType,
			InputStream in, Map<String, String> parameters) throws IOException {
		final UserUuidPK endpointPk = new UserUuidPK(requireNonNullArgument(userId, "userId"),
				requireNonNullArgument(endpointId, "endpointId"));
		final EndpointConfiguration endpoint = requireNonNullObject(endpointDao.get(endpointPk),
				endpointPk);

		final UserLongCompositePK xformPk = new UserLongCompositePK(userId,
				requireNonNullArgument(endpoint.getTransformId(), "transformId"));
		final TransformConfiguration xform = requireNonNullObject(transformDao.get(xformPk), xformPk);

		final String xformServiceId = requireNonNullArgument(xform.getServiceIdentifier(),
				"transform.serviceIdentifier");
		final TransformService xformService = requireNonNullObject(transformServices.get(xformServiceId),
				xformServiceId);
		final DatumProcessor fluxPublisher = (endpoint.isPublishToSolarFlux() ? getFluxPublisher()
				: null);

		if ( endpoint.getRequestContentType() != null ) {
			// force content type to endpoint configuration
			contentType = MimeType.valueOf(endpoint.getRequestContentType());
		}

		// load data, so can include in error event if needed
		final byte[] inputData = FileCopyUtils.copyToByteArray(in);
		in = new ByteArrayInputStream(inputData);

		if ( !xformService.supportsInput(requireNonNullArgument(in, "in"),
				requireNonNullArgument(contentType, "contentType")) ) {
			String msg = "Transform service %s does not support input type %s with %s."
					.formatted(xformServiceId, contentType, in.getClass().getSimpleName());
			addUserEvent(userEventAppenderBiz, userId,
					importErrorEvent(msg, endpoint, xform, contentType, inputData, null, parameters));
			throw new IllegalArgumentException(msg);
		}

		var params = new HashMap<String, Object>(8);
		if ( parameters != null ) {
			params.putAll(parameters);
		}
		params.put(TransformService.PARAM_USER_ID, userId);
		params.put(TransformService.PARAM_ENDPOINT_ID, endpointId.toString());
		params.put(TransformService.PARAM_TRANSFORM_ID, endpoint.getTransformId());
		params.put(TransformService.PARAM_CONFIGURATION_CACHE_KEY, xform.ident());

		byte[] prevInputData = null;
		Iterable<Datum> datum;
		try {
			// check for previous input support
			final InputDataEntityDao previousInputDao = (endpoint.isPreviousInputTracking()
					? this.previousInputDataDao
					: null);
			if ( previousInputDao != null && parameters != null && parameters.containsKey(PARAM_NODE_ID)
					&& parameters.containsKey(PARAM_SOURCE_ID) ) {
				// track previous input using user/node/source key; node and source MUST
				// be provided as input parameters; if no previous input has been cached
				// then immediately return an empty result, to wait for next input
				try {
					Long nodeId = Long.valueOf(parameters.get(PARAM_NODE_ID));
					String sourceId = parameters.get(PARAM_SOURCE_ID);
					if ( sourceId != null ) {
						UserLongStringCompositePK key = new UserLongStringCompositePK(userId, nodeId,
								sourceId);
						prevInputData = previousInputDao.getAndPut(key, inputData);
						if ( prevInputData == null ) {
							return Collections.emptyList();
						}
						params.put(TransformService.PARAM_PREVIOUS_INPUT,
								new ByteArrayInputStream(prevInputData));
					}
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
				}
			}

			datum = xformService.transform(in, contentType, xform, params);

			if ( log.isTraceEnabled() ) {
				log.trace(
						"Transformed user {} endpoint {} to datum with service {}; parameters {}; previous content [{}]; content [{}]; datum: {}",
						userId, endpointId, xformServiceId, params,
						eventContentValue(contentType, prevInputData),
						eventContentValue(contentType, inputData), datum);
			}
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			String msg = "Error executing transform: " + root.getMessage();

			if ( log.isWarnEnabled() ) {
				log.warn(
						"Error transforming user {} endpoint {} to datum with service {}; parameters {}; previous content [{}]; content: [{}]",
						userId, endpointId, xformServiceId, params,
						eventContentValue(contentType, prevInputData),
						eventContentValue(contentType, inputData), e);
			}

			addUserEvent(userEventAppenderBiz, userId, importErrorEvent(msg, endpoint, xform,
					contentType, inputData, prevInputData, parameters));
			if ( e instanceof IOException ioe ) {
				throw ioe;
			} else if ( e instanceof RuntimeException re ) {
				throw re;
			} else {
				throw new RuntimeException(e);
			}
		}

		var result = (endpoint.isIncludeResponseBody() ? new ArrayList<DatumId>(8) : null);
		for ( Datum d : datum ) {
			Object gd = DatumUtils.convertGeneralDatum(d);
			if ( gd instanceof GeneralNodeDatum gnd ) {
				// use the endpoint's node/source IDs if provided
				if ( endpoint.getNodeId() != null ) {
					gnd.setNodeId(endpoint.getNodeId());
				} else if ( parameters != null && parameters.containsKey(PARAM_NODE_ID) ) {
					try {
						gnd.setNodeId(Long.valueOf(parameters.get(PARAM_NODE_ID)));
					} catch ( IllegalArgumentException e ) {
						// ignore and continue
					}
				}
				if ( endpoint.getSourceId() != null ) {
					gnd.setSourceId(endpoint.getSourceId());
				} else if ( parameters != null && parameters.containsKey(PARAM_SOURCE_ID) ) {
					gnd.setSourceId(parameters.get(PARAM_SOURCE_ID));
				}

				// verify ownership node is owner of endpoint
				Long nodeId = requireNonNullArgument(gnd.getNodeId(), "nodeId");
				String sourceId = requireNonNullArgument(gnd.getSourceId(), "sourceId");
				SolarNodeOwnership owner = requireNonNullObject(
						nodeOwnershipDao.ownershipForNodeId(nodeId), nodeId);
				if ( !userId.equals(owner.getUserId()) ) {
					var ex = new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
					addUserEvent(userEventAppenderBiz, userId, importErrorEvent(ex.getMessage(),
							endpoint, xform, contentType, inputData, prevInputData, parameters));
					throw ex;
				}

				DatumPK pk = datumDao.persist(gnd);
				if ( result != null ) {
					result.add(DatumId.nodeId(nodeId, sourceId, pk.getTimestamp()));
				}

				try {
					if ( fluxPublisher != null && fluxPublisher.isConfigured() ) {
						fluxPublisher.processDatum(gnd);
					}
				} catch ( Exception e ) {
					log.warn("Error publishing endpoint {} datum {}: {}", endpoint.getId(), gnd,
							e.toString(), e);
				}
			}
		}
		return result != null ? Collections.unmodifiableList(result) : null;
	}

	/**
	 * Get the SolarFlux publisher.
	 *
	 * @return the publisher, or {@literal null}
	 */
	public DatumProcessor getFluxPublisher() {
		return fluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 *
	 * @param fluxPublisher
	 *        the publisher to set
	 */
	public void setFluxPublisher(DatumProcessor fluxPublisher) {
		this.fluxPublisher = fluxPublisher;
	}

	/**
	 * Get the user event appender service.
	 *
	 * @return the service
	 * @since 1.2
	 */
	public UserEventAppenderBiz getUserEventAppenderBiz() {
		return userEventAppenderBiz;
	}

	/**
	 * Set the user event appender service.
	 *
	 * @param userEventAppenderBiz
	 *        the service to set
	 * @since 1.2
	 */
	public void setUserEventAppenderBiz(UserEventAppenderBiz userEventAppenderBiz) {
		this.userEventAppenderBiz = userEventAppenderBiz;
	}

}
