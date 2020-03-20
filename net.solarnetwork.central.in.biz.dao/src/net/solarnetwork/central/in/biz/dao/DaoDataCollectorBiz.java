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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Implementation of {@link DataCollectorBiz} using {@link GeneralNodeDatumDao}
 * and {@link GeneralLocationDatumDao} APIs to persist the data.
 * 
 * <p>
 * This service expects all calls into {@link #postGeneralNodeDatum(Iterable)}
 * and {@link #postGeneralLocationDatum(Iterable)} to provide a
 * {@link AuthenticatedNode} via the normal Spring Security
 * {@link SecurityContextHolder} API. Any attempt to post data for a node
 * different from the currently authenticated node will result in a
 * {@link SecurityException}. If a {@link GeneralNodeDatum} is posted with a
 * <em>null</em> {@link GeneralNodeDatum#getNodeId()} value, this service will
 * set the node ID to the authenticated node ID automatically.
 * </p>
 * 
 * @author matt
 * @version 3.1
 */
public class DaoDataCollectorBiz implements DataCollectorBiz {

	private SolarLocationDao solarLocationDao = null;
	private SolarNodeMetadataBiz solarNodeMetadataBiz;
	private GeneralNodeDatumDao generalNodeDatumDao = null;
	private GeneralLocationDatumDao generalLocationDatumDao = null;
	private DatumMetadataBiz datumMetadataBiz = null;
	private int filteredResultsLimit = 250;

	/** A class-level logger. */
	private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
					log.warn("Illegal datum post by node " + authNode.getNodeId() + " as node "
							+ d.getNodeId());
				}
				throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
			}
			try {
				generalNodeDatumDao.store(d);
			} catch ( TransientDataAccessException e ) {
				throw new RepeatableTaskException("Transient error storing datum " + d.getId(), e);
			}
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
				generalLocationDatumDao.store(d);
			} catch ( TransientDataAccessException e ) {
				throw new RepeatableTaskException("Transient error storing location datum " + d.getId(),
						e);
			}
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

	public GeneralNodeDatumDao getGeneralNodeDatumDao() {
		return generalNodeDatumDao;
	}

	public void setGeneralNodeDatumDao(GeneralNodeDatumDao generalNodeDatumDao) {
		this.generalNodeDatumDao = generalNodeDatumDao;
	}

	public DatumMetadataBiz getDatumMetadataBiz() {
		return datumMetadataBiz;
	}

	public void setDatumMetadataBiz(DatumMetadataBiz datumMetadataBiz) {
		this.datumMetadataBiz = datumMetadataBiz;
	}

	public GeneralLocationDatumDao getGeneralLocationDatumDao() {
		return generalLocationDatumDao;
	}

	public void setGeneralLocationDatumDao(GeneralLocationDatumDao generalLocationDatumDao) {
		this.generalLocationDatumDao = generalLocationDatumDao;
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

}
