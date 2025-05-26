/* ==================================================================
 * DaoDataCollectorBiz.java - Dec 14, 2009 10:46:26 AM
 *
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.BasicSecurityException;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Implementation of {@link DataCollectorBiz} using {@link DatumEntityDao} to
 * persist the data.
 *
 * <p>
 * This service expects all calls into {@link #postGeneralNodeDatum(Iterable)}
 * and {@link #postGeneralLocationDatum(Iterable)} and
 * {@link #postStreamDatum(Iterable)} to provide an {@link AuthenticatedNode}
 * via the normal Spring Security {@link SecurityContextHolder} API. Any attempt
 * to post data for a node different from the currently authenticated node will
 * result in a {@link BasicSecurityException}. If a {@link GeneralNodeDatum} is
 * posted with a <em>null</em> {@link GeneralNodeDatum#getNodeId()} value, this
 * service will set the node ID to the authenticated node ID automatically.
 * </p>
 *
 * @author matt
 * @version 4.4
 */
public class DaoDataCollectorBiz implements DataCollectorBiz {

	private final DatumWriteOnlyDao datumDao;
	private SolarNodeDao solarNodeDao = null;
	private SolarLocationDao solarLocationDao = null;
	private SolarNodeMetadataBiz solarNodeMetadataBiz;
	private DatumStreamMetadataDao metaDao = null;
	private DatumMetadataBiz datumMetadataBiz = null;
	private int filteredResultsLimit = 250;

	/** A class-level logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param datumDao
	 *        the datum DAO to use
	 */
	public DaoDataCollectorBiz(DatumWriteOnlyDao datumDao) {
		super();
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
	}

	private Integer limitFilterMaximum(Integer requestedMaximum) {
		if ( requestedMaximum == null || requestedMaximum > filteredResultsLimit
				|| requestedMaximum < 1 ) {
			return filteredResultsLimit;
		}
		return requestedMaximum;
	}

	private Long limitFilterOffset(Long requestedOffset) {
		if ( requestedOffset == null || requestedOffset.intValue() < 0 ) {
			return 0L;
		}
		return requestedOffset;
	}

	@Override
	public void postGeneralNodeDatum(Iterable<GeneralNodeDatum> datums) {
		if ( datums == null ) {
			return;
		}
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}

		for ( GeneralNodeDatum d : datums ) {
			if ( d.getNodeId() == null ) {
				d.setNodeId(authNode.getNodeId());
			} else if ( !d.getNodeId().equals(authNode.getNodeId()) ) {
				if ( log.isWarnEnabled() ) {
					log.warn("Illegal datum post by node {} as node {}", authNode.getNodeId(),
							d.getNodeId());
				}
				throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
			}
			try {
				datumDao.persist(d);
			} catch ( TransientDataAccessException e ) {
				throw new RepeatableTaskException("Transient error storing datum " + d.getId(), e);
			}
		}

	}

	@Override
	public void postGeneralLocationDatum(Iterable<GeneralLocationDatum> datums) {
		if ( datums == null ) {
			return;
		}
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}

		for ( GeneralLocationDatum d : datums ) {
			if ( d.getLocationId() == null ) {
				throw new IllegalArgumentException(
						"A locationId value is required for GeneralLocationDatum");
			}
			try {
				datumDao.persist(d);
			} catch ( TransientDataAccessException e ) {
				throw new RepeatableTaskException("Transient error storing location datum " + d.getId(),
						e);
			}
		}
	}

	@Override
	public void postStreamDatum(Iterable<StreamDatum> datums) {
		if ( datums == null ) {
			return;
		}
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		final Instant now = Instant.now();

		for ( StreamDatum d : datums ) {
			if ( d.getStreamId() == null ) {
				throw new IllegalArgumentException("A streamId value is required for StreamDatum");
			}
			BasicDatumCriteria criteria = new BasicDatumCriteria();
			criteria.setStreamId(d.getStreamId());
			ObjectDatumStreamMetadata meta = metaDao.findStreamMetadata(criteria);
			if ( meta == null ) {
				if ( log.isWarnEnabled() ) {
					log.warn("Unknown stream datum post by node {} as stream {}", authNode.getNodeId(),
							d.getStreamId());
				}
				throw new AuthorizationException(Reason.ACCESS_DENIED, d.getStreamId());
			}
			if ( meta.getKind() == ObjectDatumKind.Node
					&& !authNode.getNodeId().equals(meta.getObjectId()) ) {
				if ( log.isWarnEnabled() ) {
					log.warn("Illegal stream datum post by node {} as node {}", authNode.getNodeId(),
							meta.getObjectId());
				}
				throw new AuthorizationException(Reason.ACCESS_DENIED, meta.getObjectId());
			}

			DatumProperties dp = DatumProperties.propertiesOf(d.getProperties().getInstantaneous(),
					d.getProperties().getAccumulating(), d.getProperties().getStatus(),
					d.getProperties().getTags());
			DatumEntity datum = new DatumEntity(d.getStreamId(), d.getTimestamp(), now, dp);

			try {
				datumDao.store(datum);
			} catch ( TransientDataAccessException e ) {
				throw new RepeatableTaskException(
						"Transient error storing stream datum " + datum.getId(), e);
			}
		}
	}

	private boolean isSharedLocation(SolarLocation loc) {
		return (loc.getStreet() == null && loc.getLatitude() == null && loc.getLongitude() == null
				&& loc.getElevation() == null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Location getLocationForNode(Long nodeId) {
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		} else if ( nodeId == null ) {
			nodeId = authNode.getNodeId();
		} else if ( !nodeId.equals(authNode.getNodeId()) ) {
			log.warn("Illegal location fetch by node {} for node {}", authNode.getNodeId(), nodeId);
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		return solarLocationDao.getSolarLocationForNode(nodeId);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void updateLocation(Long nodeId, final net.solarnetwork.domain.Location location) {
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( nodeId == null ) {
			nodeId = authNode.getNodeId();
		} else if ( !nodeId.equals(authNode.getNodeId()) ) {
			log.warn("Illegal location update by node {} for node {}", authNode.getNodeId(), nodeId);
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		final SolarLocation loc = solarLocationDao.getSolarLocationForNode(nodeId);
		if ( loc == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, nodeId);
		}

		final SolarLocation norm = SolarLocation.normalizedLocation(loc);

		// only GPS coordinates of a node's location can be updated by node itself
		boolean changed = false;
		if ( location.getLatitude() != null && (loc.getLatitude() == null
				|| location.getLatitude().compareTo(loc.getLatitude()) != 0) ) {
			changed = true;
			loc.setLatitude(location.getLatitude());
		}
		if ( location.getLongitude() != null && (loc.getLongitude() == null
				|| location.getLongitude().compareTo(loc.getLongitude()) != 0) ) {
			changed = true;
			loc.setLongitude(location.getLongitude());
		}
		if ( location.getElevation() != null && (loc.getElevation() == null
				|| location.getElevation().compareTo(loc.getElevation()) != 0) ) {
			changed = true;
			loc.setElevation(location.getElevation());
		}

		if ( !changed ) {
			return;
		}

		if ( isSharedLocation(norm) ) {
			// switch node to new non-shared location based on loc updates
			Long locId = solarLocationDao.save(SolarLocation.normalizedLocation(loc));
			SolarNode node = solarNodeDao.get(nodeId);
			log.debug("Updating node {} location from {} to {}", node.getId(), node.getLocationId(),
					locId);
			node.setLocationId(locId);
			solarNodeDao.save(node);
		} else {
			// update current location
			solarLocationDao.save(loc);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void addGeneralNodeDatumMetadata(Long nodeId, final String sourceId,
			final GeneralDatumMetadata meta) {
		if ( sourceId == null || meta == null
				|| ((meta.getTags() == null || meta.getTags().isEmpty())
						&& (meta.getInfo() == null || meta.getInfo().isEmpty())
						&& (meta.getPropertyInfo() == null || meta.getPropertyInfo().isEmpty())) ) {
			return;
		}

		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( nodeId == null ) {
			nodeId = authNode.getNodeId();
		} else if ( !nodeId.equals(authNode.getNodeId()) ) {
			log.warn("Illegal datum metadata post by node {} as node {}", authNode.getNodeId(), nodeId);
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		datumMetadataBiz.addGeneralNodeDatumMetadata(nodeId, sourceId, meta);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void addSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta) {
		if ( meta == null || ((meta.getTags() == null || meta.getTags().isEmpty())
				&& (meta.getInfo() == null || meta.getInfo().isEmpty())
				&& (meta.getPropertyInfo() == null || meta.getPropertyInfo().isEmpty())) ) {
			return;
		}

		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( nodeId == null ) {
			nodeId = authNode.getNodeId();
		} else if ( !nodeId.equals(authNode.getNodeId()) ) {
			log.warn("Illegal node metadata post by node {} as node {}", authNode.getNodeId(), nodeId);
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		solarNodeMetadataBiz.addSolarNodeMetadata(nodeId, meta);
	}

	private SolarNodeMetadataFilter solarNodeMetadataCriteriaForcedToAuthenticatedNode(
			final SolarNodeMetadataFilter criteria) {
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( criteria.getNodeId() != null && authNode.getNodeId().equals(criteria.getNodeId()) ) {
			return criteria;
		}
		if ( !(criteria instanceof DatumFilterCommand dfc) ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		dfc.setNodeId(authNode.getNodeId());
		return dfc;
	}

	@Override
	public FilterResults<SolarNodeMetadataFilterMatch, Long> findSolarNodeMetadata(
			SolarNodeMetadataFilter criteria, final List<SortDescriptor> sortDescriptors,
			final Long offset, final Integer max) {
		return solarNodeMetadataBiz.findSolarNodeMetadata(
				solarNodeMetadataCriteriaForcedToAuthenticatedNode(criteria), sortDescriptors, offset,
				max);
	}

	private GeneralNodeDatumMetadataFilter metadataCriteriaForcedToAuthenticatedNode(
			final GeneralNodeDatumMetadataFilter criteria) {
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( criteria.getNodeId() != null && authNode.getNodeId().equals(criteria.getNodeId()) ) {
			return criteria;
		}
		if ( !(criteria instanceof DatumFilterCommand dfc) ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		dfc.setNodeId(authNode.getNodeId());
		return dfc;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch, NodeSourcePK> findGeneralNodeDatumMetadata(
			final GeneralNodeDatumMetadataFilter criteria, final List<SortDescriptor> sortDescriptors,
			final Long offset, final Integer max) {
		return datumMetadataBiz.findGeneralNodeDatumMetadata(
				metadataCriteriaForcedToAuthenticatedNode(criteria), sortDescriptors, offset, max);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> findGeneralLocationDatumMetadata(
			final GeneralLocationDatumMetadataFilter criteria,
			final List<SortDescriptor> sortDescriptors, final Long offset, final Integer max) {
		return datumMetadataBiz.findGeneralLocationDatumMetadata(criteria, sortDescriptors, offset, max);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<LocationMatch> findLocations(Location criteria) {
		FilterResults<LocationMatch, Long> matches = solarLocationDao.findFiltered(criteria, null,
				limitFilterOffset(null), limitFilterMaximum(null));
		List<LocationMatch> resultList = new ArrayList<>(matches.getReturnedResultCount());
		for ( LocationMatch m : matches.getResults() ) {
			resultList.add(m);
		}
		return resultList;
	}

	private AuthenticatedNode getAuthenticatedNode() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if ( auth != null ) {
			Object principal = auth.getPrincipal();
			if ( principal instanceof AuthenticatedNode n ) {
				return n;
			}
		}
		return null;
	}

	public SolarLocationDao getSolarLocationDao() {
		return solarLocationDao;
	}

	public void setSolarLocationDao(SolarLocationDao solarLocationDao) {
		this.solarLocationDao = solarLocationDao;
	}

	public int getFilteredResultsLimit() {
		return filteredResultsLimit;
	}

	public void setFilteredResultsLimit(int filteredResultsLimit) {
		this.filteredResultsLimit = filteredResultsLimit;
	}

	/**
	 * Get the datum stream metadata DAO.
	 *
	 * @return the DAO to use
	 * @since 3.4
	 */
	public DatumStreamMetadataDao getMetaDao() {
		return metaDao;
	}

	/**
	 * Set the datum stream metadata DAO.
	 *
	 * @param metaDao
	 *        the DAO to set
	 * @since 3.4
	 */
	public void setMetaDao(DatumStreamMetadataDao metaDao) {
		this.metaDao = metaDao;
	}

	public DatumMetadataBiz getDatumMetadataBiz() {
		return datumMetadataBiz;
	}

	public void setDatumMetadataBiz(DatumMetadataBiz datumMetadataBiz) {
		this.datumMetadataBiz = datumMetadataBiz;
	}

	/**
	 * Get the configured node metadata biz.
	 *
	 * @return the service, or {@literal null} if not configured
	 * @since 2.1
	 */
	public SolarNodeMetadataBiz getSolarNodeMetadataBiz() {
		return solarNodeMetadataBiz;
	}

	/**
	 * Set the node metadata biz to use.
	 *
	 * @param solarNodeMetadataBiz
	 *        the service to set
	 * @since 2.1
	 */
	public void setSolarNodeMetadataBiz(SolarNodeMetadataBiz solarNodeMetadataBiz) {
		this.solarNodeMetadataBiz = solarNodeMetadataBiz;
	}

	/**
	 * Get the node DAO.
	 *
	 * @return the node DAO
	 * @since 3.5
	 */
	public SolarNodeDao getSolarNodeDao() {
		return solarNodeDao;
	}

	/**
	 * Set the node DAO.
	 *
	 * @param solarNodeDao
	 *        the DAO to set
	 * @since 3.5
	 */
	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

}
