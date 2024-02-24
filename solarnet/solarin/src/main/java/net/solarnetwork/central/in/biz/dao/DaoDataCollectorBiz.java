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

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.biz.dao.AsyncDaoDatumCollector;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.FilterResults;
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
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Implementation of {@link DataCollectorBiz} using {@link DatumEntityDao} and
 * {@link GeneralLocationDatumDao} APIs to persist the data.
 * 
 * <p>
 * This service expects all calls into {@link #postGeneralNodeDatum(Iterable)}
 * and {@link #postGeneralLocationDatum(Iterable)} and
 * {@link #postStreamDatum(Iterable)} to provide an {@link AuthenticatedNode}
 * via the normal Spring Security {@link SecurityContextHolder} API. Any attempt
 * to post data for a node different from the currently authenticated node will
 * result in a {@link SecurityException}. If a {@link GeneralNodeDatum} is
 * posted with a <em>null</em> {@link GeneralNodeDatum#getNodeId()} value, this
 * service will set the node ID to the authenticated node ID automatically.
 * </p>
 * 
 * @author matt
 * @version 4.2
 */
public class DaoDataCollectorBiz implements DataCollectorBiz {

	private SolarNodeDao solarNodeDao = null;
	private SolarLocationDao solarLocationDao = null;
	private SolarNodeMetadataBiz solarNodeMetadataBiz;
	private DatumEntityDao datumDao = null;
	private DatumStreamMetadataDao metaDao = null;
	private DatumMetadataBiz datumMetadataBiz = null;
	private int filteredResultsLimit = 250;
	private TransactionTemplate transactionTemplate;
	private Cache<Serializable, Serializable> datumCache;

	/** A class-level logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	private Integer limitFilterMaximum(Integer requestedMaximum) {
		if ( requestedMaximum == null || requestedMaximum.intValue() > filteredResultsLimit
				|| requestedMaximum.intValue() < 1 ) {
			return filteredResultsLimit;
		}
		return requestedMaximum;
	}

	private Integer limitFilterOffset(Integer requestedOffset) {
		if ( requestedOffset == null || requestedOffset.intValue() < 0 ) {
			return 0;
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
		final Cache<Serializable, Serializable> buffer = getDatumCache();
		TransactionCallbackWithoutResult action = new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
					if ( buffer != null ) {
						buffer.put(d.getId(), d);
					} else {
						try {
							datumDao.store(d);
						} catch ( TransientDataAccessException e ) {
							throw new RepeatableTaskException(
									"Transient error storing datum " + d.getId(), e);
						}
					}
				}
			}
		};
		if ( buffer == null && transactionTemplate != null ) {
			transactionTemplate.execute(action);
		} else {
			action.doInTransaction(null);
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
		final Cache<Serializable, Serializable> buffer = getDatumCache();
		TransactionCallbackWithoutResult action = new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for ( GeneralLocationDatum d : datums ) {
					if ( d.getLocationId() == null ) {
						throw new IllegalArgumentException(
								"A locationId value is required for GeneralLocationDatum");
					}
					if ( buffer != null ) {
						buffer.put(d.getId(), d);
					} else {
						try {
							datumDao.store(d);
						} catch ( TransientDataAccessException e ) {
							throw new RepeatableTaskException(
									"Transient error storing location datum " + d.getId(), e);
						}
					}
				}
			}
		};
		if ( buffer == null && transactionTemplate != null ) {
			transactionTemplate.execute(action);
		} else {
			action.doInTransaction(null);
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
		final Cache<Serializable, Serializable> buffer = getDatumCache();
		final Instant now = Instant.now();
		TransactionCallbackWithoutResult action = new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for ( StreamDatum d : datums ) {
					if ( d.getStreamId() == null ) {
						throw new IllegalArgumentException(
								"A streamId value is required for StreamDatum");
					}
					BasicDatumCriteria criteria = new BasicDatumCriteria();
					criteria.setStreamId(d.getStreamId());
					ObjectDatumStreamMetadata meta = metaDao.findStreamMetadata(criteria);
					if ( meta == null ) {
						if ( log.isWarnEnabled() ) {
							log.warn("Unknown stream datum post by node {} as stream {}",
									authNode.getNodeId(), d.getStreamId());
						}
						throw new AuthorizationException(Reason.ACCESS_DENIED, d.getStreamId());
					}
					if ( meta.getKind() == ObjectDatumKind.Node
							&& !authNode.getNodeId().equals(meta.getObjectId()) ) {
						if ( log.isWarnEnabled() ) {
							log.warn("Illegal stream datum post by node {} as node {}",
									authNode.getNodeId(), meta.getObjectId());
						}
						throw new AuthorizationException(Reason.ACCESS_DENIED, meta.getObjectId());
					}

					DatumProperties dp = DatumProperties.propertiesOf(
							d.getProperties().getInstantaneous(), d.getProperties().getAccumulating(),
							d.getProperties().getStatus(), d.getProperties().getTags());
					DatumEntity datum = new DatumEntity(d.getStreamId(), d.getTimestamp(), now, dp);

					if ( buffer != null ) {
						buffer.put(datum.getId(), datum);
					} else {
						try {
							datumDao.store(datum);
						} catch ( TransientDataAccessException e ) {
							throw new RepeatableTaskException(
									"Transient error storing stream datum " + datum.getId(), e);
						}
					}
				}
			}
		};
		if ( buffer == null && transactionTemplate != null ) {
			transactionTemplate.execute(action);
		} else {
			action.doInTransaction(null);
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
		} else if ( nodeId.equals(authNode.getNodeId()) == false ) {
			log.warn("Illegal location fetch by node {} for node {}", authNode.getNodeId(), nodeId);
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		return solarLocationDao.getSolarLocationForNode(nodeId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateLocation(Long nodeId, final net.solarnetwork.domain.Location location) {
		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( nodeId == null ) {
			nodeId = authNode.getNodeId();
		} else if ( nodeId.equals(authNode.getNodeId()) == false ) {
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
			Long locId = solarLocationDao.store(SolarLocation.normalizedLocation(loc));
			SolarNode node = solarNodeDao.get(nodeId);
			log.debug("Updating node {} location from {} to {}", node.getId(), node.getLocationId(),
					locId);
			node.setLocationId(locId);
			solarNodeDao.store(node);
		} else {
			// update current location
			solarLocationDao.store(loc);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
		} else if ( nodeId.equals(authNode.getNodeId()) == false ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Illegal datum metadata post by node " + authNode.getNodeId() + " as node "
						+ nodeId);
			}
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		datumMetadataBiz.addGeneralNodeDatumMetadata(nodeId, sourceId, meta);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
		} else if ( nodeId.equals(authNode.getNodeId()) == false ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Illegal node metadata post by node " + authNode.getNodeId() + " as node "
						+ nodeId);
			}
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
		if ( !(criteria instanceof DatumFilterCommand) ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		DatumFilterCommand dfc = (DatumFilterCommand) criteria;
		dfc.setNodeId(authNode.getNodeId());
		return dfc;
	}

	@Override
	public FilterResults<SolarNodeMetadataFilterMatch> findSolarNodeMetadata(
			SolarNodeMetadataFilter criteria, final List<SortDescriptor> sortDescriptors,
			final Integer offset, final Integer max) {
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
		if ( !(criteria instanceof DatumFilterCommand) ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		DatumFilterCommand dfc = (DatumFilterCommand) criteria;
		dfc.setNodeId(authNode.getNodeId());
		return dfc;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch> findGeneralNodeDatumMetadata(
			final GeneralNodeDatumMetadataFilter criteria, final List<SortDescriptor> sortDescriptors,
			final Integer offset, final Integer max) {
		return datumMetadataBiz.findGeneralNodeDatumMetadata(
				metadataCriteriaForcedToAuthenticatedNode(criteria), sortDescriptors, offset, max);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralLocationDatumMetadataFilterMatch> findGeneralLocationDatumMetadata(
			final GeneralLocationDatumMetadataFilter criteria,
			final List<SortDescriptor> sortDescriptors, final Integer offset, final Integer max) {
		return datumMetadataBiz.findGeneralLocationDatumMetadata(criteria, sortDescriptors, offset, max);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<LocationMatch> findLocations(Location criteria) {
		FilterResults<LocationMatch> matches = solarLocationDao.findFiltered(criteria, null,
				limitFilterOffset(null), limitFilterMaximum(null));
		List<LocationMatch> resultList = new ArrayList<LocationMatch>(matches.getReturnedResultCount());
		for ( LocationMatch m : matches.getResults() ) {
			resultList.add(m);
		}
		return resultList;
	}

	private AuthenticatedNode getAuthenticatedNode() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if ( auth != null ) {
			Object principal = auth.getPrincipal();
			if ( principal instanceof AuthenticatedNode ) {
				return (AuthenticatedNode) principal;
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
	 * Get the datum DAO.
	 * 
	 * @return the DAO to use
	 * @since 3.3
	 */
	public DatumEntityDao getDatumDao() {
		return datumDao;
	}

	/**
	 * Set the datum DAO.
	 * 
	 * @param datumDao
	 *        the DAO to set
	 * @since 3.3
	 */
	public void setDatumDao(DatumEntityDao datumDao) {
		this.datumDao = datumDao;
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
	 * Get the datum cache.
	 * 
	 * @return the cache
	 * @since 3.2
	 */
	public Cache<Serializable, Serializable> getDatumCache() {
		return datumCache;
	}

	/**
	 * Set the datum cache.
	 * 
	 * <p>
	 * If this cache is configured, then datum are stored here <b>instead</b> of
	 * directly storing via one of the configured DAO instances. Some other
	 * process must persist the entities from the cache, e.g.
	 * {@link AsyncDaoDatumCollector}.
	 * </p>
	 * 
	 * @param datumCache
	 *        the cache
	 * @since 3.2
	 */
	public void setDatumCache(Cache<Serializable, Serializable> datumCache) {
		this.datumCache = datumCache;
	}

	/**
	 * Set the transaction template.
	 * 
	 * @param transactionTemplate
	 *        the template
	 * @since 3.2
	 */
	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Get the transaction template.
	 * 
	 * @return the template
	 * @since 3.2
	 */
	public TransactionTemplate getTransactionTemplate() {
		return transactionTemplate;
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
