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

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.MimeType;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.din.biz.DatumInputEndpointBiz;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;

/**
 * DAO implementation of {@link DatumInputEndpointBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoDatumInputEndpointBiz implements DatumInputEndpointBiz {

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final EndpointConfigurationDao endpointDao;
	private final TransformConfigurationDao transformDao;
	private final DatumWriteOnlyDao datumDao;
	private final Map<String, TransformService> transformServices;

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
			DatumWriteOnlyDao datumDao, Collection<TransformService> transformServices) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.transformDao = requireNonNullArgument(transformDao, "transformDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.transformServices = requireNonNullArgument(transformServices, "transformServices").stream()
				.collect(Collectors.toMap(s -> s.getId(), Function.identity()));
	}

	@Override
	public Collection<DatumId> importDatum(Long userId, UUID endpointId, MimeType contentType,
			InputStream in) throws IOException {
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

		if ( !xformService.supportsInput(requireNonNullArgument(in, "in"),
				requireNonNullArgument(contentType, "contentType")) ) {
			throw new IllegalArgumentException(
					"Transform service %s does not support input type %s with %s."
							.formatted(xformServiceId, contentType, in.getClass().getSimpleName()));
		}

		var params = Map.of("userId", userId, "endpointId", endpointId,
				TransformService.PARAM_CONFIGURATION_CACHE_KEY, xformPk.ident());
		Iterable<Datum> datum = xformService.transform(in, contentType, xform, params);

		var result = new ArrayList<DatumId>(8);
		for ( Datum d : datum ) {
			Object gd = DatumUtils.convertGeneralDatum(d);
			if ( gd instanceof GeneralNodeDatum gnd ) {
				// use the endpoint's node/source IDs if provided
				if ( endpoint.getNodeId() != null ) {
					gnd.setNodeId(endpoint.getNodeId());
				}
				if ( endpoint.getSourceId() != null ) {
					gnd.setSourceId(endpoint.getSourceId());
				}

				// verify ownership node is owner of endpoint
				Long nodeId = requireNonNullArgument(gnd.getNodeId(), "nodeId");
				String sourceId = requireNonNullArgument(gnd.getSourceId(), "sourceId");
				SolarNodeOwnership owner = requireNonNullObject(
						nodeOwnershipDao.ownershipForNodeId(nodeId), nodeId);
				if ( !userId.equals(owner.getUserId()) ) {
					throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
				}

				DatumPK pk = datumDao.persist(gnd);
				result.add(DatumId.nodeId(nodeId, sourceId, pk.getTimestamp()));
			}
		}
		return result;
	}

}
