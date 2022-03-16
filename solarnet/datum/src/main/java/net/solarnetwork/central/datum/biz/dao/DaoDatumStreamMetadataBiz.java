/* ==================================================================
 * DaoDatumStreamMetadataBiz.java - 21/11/2021 5:50:04 PM
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

package net.solarnetwork.central.datum.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * DAO-based implementation of {@link DatumStreamMetadataBiz}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class DaoDatumStreamMetadataBiz implements DatumStreamMetadataBiz {

	private final DatumStreamMetadataDao metaDao;

	/**
	 * Constructor.
	 * 
	 * @param metaDao
	 *        the metadata DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoDatumStreamMetadataBiz(DatumStreamMetadataDao metaDao) {
		super();
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ObjectDatumStreamMetadataId updateIdAttributes(ObjectDatumKind kind, UUID streamId,
			Long objectId, String sourceId) {
		return metaDao.updateIdAttributes(kind, streamId, objectId, sourceId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<ObjectDatumStreamMetadata> findDatumStreamMetadata(SecurityActor actor,
			ObjectStreamCriteria criteria) {
		BasicDatumCriteria c = DatumUtils.toBasicDatumCriteria(criteria);
		if ( c == null ) {
			c = new BasicDatumCriteria();
		}
		c.setObjectKind(ObjectDatumKind.Node);
		restrictCriteriaToActor(actor, c);
		Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(c);
		return toList(results);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<ObjectDatumStreamMetadataId> findDatumStreamMetadataIds(SecurityActor actor,
			ObjectStreamCriteria criteria) {
		BasicDatumCriteria c = DatumUtils.toBasicDatumCriteria(criteria);
		if ( c == null ) {
			c = new BasicDatumCriteria();
		}
		c.setObjectKind(ObjectDatumKind.Node);
		restrictCriteriaToActor(actor, c);
		Iterable<ObjectDatumStreamMetadataId> results = metaDao.findDatumStreamMetadataIds(c);
		return toList(results);
	}

	private static <T> List<T> toList(Iterable<T> iterable) {
		if ( iterable instanceof List ) {
			return (List<T>) iterable;
		}
		return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
	}

	private void restrictCriteriaToActor(SecurityActor actor, BasicDatumCriteria c) {
		if ( actor instanceof SecurityNode ) {
			SecurityNode node = (SecurityNode) actor;
			c.setNodeId(node.getNodeId());
		} else if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			c.setUserId(user.getUserId());
		} else if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			c.setTokenId(token.getToken());
		}
	}

}
