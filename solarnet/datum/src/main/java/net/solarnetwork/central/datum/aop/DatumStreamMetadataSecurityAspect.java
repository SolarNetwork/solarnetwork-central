/* ==================================================================
 * DatumStreamMetadataSecurityAspect.java - 21/11/2021 5:52:40 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.aop;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Security AOP support for {@link DatumStreamMetadataBiz}.
 *
 * @author matt
 * @version 1.1
 * @since 2.0
 */
@Aspect
@Component
public class DatumStreamMetadataSecurityAspect extends AuthorizationSupport {

	private final DatumStreamMetadataDao metaDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 * @param metaDao
	 *        the metadata DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumStreamMetadataSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			DatumStreamMetadataDao metaDao) {
		super(nodeOwnershipDao);
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
	}

	@Pointcut(
			value = "execution(* net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz.update*Attributes(..)) && args(kind, streamId, objectId, sourceId, ..)",
			argNames = "kind,streamId,objectId,sourceId")
	public void updateAttributesMetadata(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId) {
	}

	/**
	 * Check access to modifying datum metadata.
	 *
	 * @param kind
	 *        the kind
	 * @param streamId
	 *        the ID of the stream to verify
	 * @param objectId
	 *        the object ID to verify
	 * @param sourceId
	 *        the source ID to verify
	 */
	@Before(value = "updateAttributesMetadata(kind, streamId, objectId, sourceId)",
			argNames = "kind,streamId,objectId,sourceId")
	public void updateAttributesCheck(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId) {
		if ( streamId == null || kind == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		if ( kind != ObjectDatumKind.Node ) {
			final Authentication authentication = SecurityUtils.getCurrentAuthentication();
			final String opsRole = Role.ROLE_OPS.toString();
			if ( authentication != null ) {
				for ( GrantedAuthority auth : authentication.getAuthorities() ) {
					if ( opsRole.equals(auth.getAuthority()) ) {
						return;
					}
				}
			}
			log.warn("Access DENIED to {} stream {} for user {}; not authorized with required role {}",
					kind, streamId, authentication, opsRole);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, streamId);
		}

		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(kind);
		filter.setStreamId(streamId);
		ObjectDatumStreamMetadata meta = metaDao.findStreamMetadata(filter);
		if ( meta == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, streamId);
		}

		// must have write access to stream's current node ID
		requireNodeWriteAccess(meta.getObjectId());
		if ( objectId != null ) {
			// must have write access to requested new node ID
			requireNodeWriteAccess(objectId);
		}
	}

}
