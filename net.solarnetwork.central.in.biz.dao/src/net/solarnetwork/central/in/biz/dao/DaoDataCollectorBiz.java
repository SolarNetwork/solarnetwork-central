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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.dao.PriceLocationDao;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.WeatherLocationDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.dao.DatumDao;
import net.solarnetwork.central.datum.dao.DayDatumDao;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.dao.WeatherDatumDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationDatum;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.datum.domain.PriceDatum;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.util.ClassUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link DataCollectorBiz} using {@link DatumDao} objects to
 * persist the data.
 * 
 * <p>
 * This service expects all calls into {@link #postDatum(Datum)} and
 * {@link #postDatum(Iterable)} to provide a {@link AuthenticatedNode} via the
 * normal Spring Security {@link SecurityContextHolder} API. Any attempt to post
 * data for a node different from the currently authenticated node will result
 * in a {@link SecurityException}. If a {@link NodeDatum} is posted with a
 * <em>null</em> {@link NodeDatum#getNodeId()} value, this service will set the
 * node ID to the authenticated node ID automatically.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>daoMapping</dt>
 * <dd>A mapping of {@link Datum} class objects to associated {@link DatumDao}
 * objects that know how to persist them.</dd>
 * 
 * <dt>solarNodeDao</dt>
 * <dd>The {@link SolarNodeDao} so location information can be added to
 * {@link DayDatum} and {@link WeatherDatum} objects if they are missing that
 * information when passed to {@link #postDatum(Datum)}.</dd>
 * 
 * </dl>
 * 
 * @author matt
 * @version 1.3
 */
public class DaoDataCollectorBiz implements DataCollectorBiz {

	private static final Set<String> CONSUMPTION_DATUM_IGNORE_ENTITY_COPY = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "class", "id", "created",
					"nodeId", "watts" })));

	private static final Set<String> POWER_DATUM_IGNORE_ENTITY_COPY = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "class", "id", "created",
					"nodeId" })));

	private static final Set<String> PRICE_DATUM_IGNORE_ENTITY_COPY = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "class", "id", "created",
					"locationId" })));

	private Map<String, DatumDao<Datum>> daoMapping;
	private SolarNodeDao solarNodeDao = null;
	private PriceLocationDao priceLocationDao = null;
	private WeatherLocationDao weatherLocationDao = null;
	private SolarLocationDao solarLocationDao = null;
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
	public <D extends Datum> D postDatum(D datum) {
		if ( datum == null ) {
			throw new IllegalArgumentException("Datum must not be null");
		}

		// verify node ID with security
		AuthenticatedNode authNode = getAuthenticatedNode();
		if ( authNode == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( datum instanceof NodeDatum ) {
			NodeDatum nd = (NodeDatum) datum;
			if ( nd.getNodeId() == null ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Setting nodeId property to authenticated node ID " + authNode.getNodeId()
							+ " on datum " + datum);
				}
				BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(nd);
				wrapper.setPropertyValue("nodeId", authNode.getNodeId());
			} else if ( !nd.getNodeId().equals(authNode.getNodeId()) ) {
				if ( log.isWarnEnabled() ) {
					log.warn("Illegal datum post by node " + authNode.getNodeId() + " as node "
							+ nd.getNodeId());
				}
				throw new AuthorizationException(Reason.ACCESS_DENIED, nd.getNodeId());
			}
		}

		DatumDao<Datum> dao = daoMapping.get(datum.getClass().getName());
		if ( dao == null ) {
			throw new IllegalArgumentException("The [" + datum.getClass().getName()
					+ "] Datum is not supported. Supported types are: " + daoMapping.keySet());
		}
		D datumToPersist = preprocessDatum(datum, dao);
		if ( datumToPersist == null ) {
			return datum;
		}
		if ( datumToPersist.getId() != null ) {
			// ignore updates... perhaps in future we need way to process updates?
			return datumToPersist;
		}
		try {
			return persistDatum(datumToPersist, dao);
		} catch ( DataIntegrityViolationException e ) {
			if ( log.isDebugEnabled() ) {
				log.debug(
						"DataIntegretyViolation on store of "
								+ datumToPersist.getClass().getSimpleName() + ": "
								+ ClassUtils.getBeanProperties(datumToPersist, null), e);
			} else if ( log.isWarnEnabled() ) {
				log.warn("DataIntegretyViolation on store of "
						+ datumToPersist.getClass().getSimpleName() + ": "
						+ ClassUtils.getBeanProperties(datumToPersist, null));
			}
			throw new net.solarnetwork.central.RepeatableTaskException(e);
		} catch ( RuntimeException e ) {
			// log this
			log.error("Unable to store " + datumToPersist.getClass().getSimpleName() + ": "
					+ ClassUtils.getBeanProperties(datumToPersist, null));
			throw e;
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Datum> postDatum(Iterable<Datum> datums) {
		List<Datum> results = new ArrayList<Datum>();
		for ( Datum d : datums ) {
			Datum entity = postDatum(d);
			results.add(entity);
		}
		return results;
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
			generalNodeDatumDao.store(d);
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
			generalLocationDatumDao.store(d);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addGeneralNodeDatumMetadata(Long nodeId, final String sourceId,
			final GeneralDatumMetadata meta) {
		if ( sourceId == null
				|| meta == null
				|| ((meta.getTags() == null || meta.getTags().isEmpty()) && (meta.getInfo() == null || meta
						.getInfo().isEmpty())) ) {
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
	public List<SourceLocationMatch> findPriceLocations(final SourceLocation criteria) {
		FilterResults<SourceLocationMatch> matches = findPriceLocations(criteria, null, null, null);
		List<SourceLocationMatch> resultList = new ArrayList<SourceLocationMatch>(
				matches.getReturnedResultCount());
		for ( SourceLocationMatch m : matches.getResults() ) {
			resultList.add(m);
		}
		return resultList;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<SourceLocationMatch> findPriceLocations(SourceLocation criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return priceLocationDao.findFiltered(criteria, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<SourceLocationMatch> findWeatherLocations(SourceLocation criteria) {
		FilterResults<SourceLocationMatch> matches = findWeatherLocations(criteria, null, null, null);
		List<SourceLocationMatch> resultList = new ArrayList<SourceLocationMatch>(
				matches.getReturnedResultCount());
		for ( SourceLocationMatch m : matches.getResults() ) {
			resultList.add(m);
		}
		return resultList;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<SourceLocationMatch> findWeatherLocations(SourceLocation criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return weatherLocationDao.findFiltered(criteria, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
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

	@SuppressWarnings("unchecked")
	private <D extends Datum> D preprocessDatum(D datum, DatumDao<? extends Datum> dao) {
		Datum result = datum;
		if ( datum instanceof ConsumptionDatum ) {
			result = preprocessConsumptionDatum((ConsumptionDatum) datum, dao);
		} else if ( datum instanceof DayDatum ) {
			result = preprocessDayDatum((DayDatum) datum, (DayDatumDao) dao);
		} else if ( datum instanceof PowerDatum ) {
			result = preprocessPowerDatum((PowerDatum) datum, dao);
		} else if ( datum instanceof PriceDatum ) {
			result = preprocessPriceDatum((PriceDatum) datum, dao);
		} else if ( datum instanceof WeatherDatum ) {
			result = preprocessWeatherDatum((WeatherDatum) datum, (WeatherDatumDao) dao);
		}
		return (D) result;
	}

	private Datum preprocessConsumptionDatum(ConsumptionDatum datum, DatumDao<? extends Datum> dao) {
		return checkForNodeDatumByDate(datum, dao, CONSUMPTION_DATUM_IGNORE_ENTITY_COPY);
	}

	private Datum preprocessDayDatum(DayDatum datum, DayDatumDao dao) {
		// fill in location ID if not provided
		if ( datum.getLocationId() == null ) {
			SolarNode node = solarNodeDao.get(datum.getNodeId());
			if ( node != null ) {
				datum.setLocationId(node.getWeatherLocationId());
			}
		}
		// see if exists already for given day
		DayDatum entity = null;
		if ( datum.getLocationId() != null ) {
			entity = dao.getDatumForDate(datum.getLocationId(), datum.getDay());
			if ( entity == null || !entity.isSameDay(datum) ) {
				return datum;
			}
		}
		return entity;
	}

	private Datum preprocessPriceDatum(PriceDatum datum, DatumDao<? extends Datum> dao) {
		return checkForLocationDatumByDate(datum, dao, PRICE_DATUM_IGNORE_ENTITY_COPY);
	}

	private Datum preprocessPowerDatum(PowerDatum datum, DatumDao<? extends Datum> dao) {
		return checkForNodeDatumByDate(datum, dao, POWER_DATUM_IGNORE_ENTITY_COPY);
	}

	private Datum preprocessWeatherDatum(WeatherDatum datum, WeatherDatumDao dao) {
		// fill in location ID if not provided
		if ( datum.getLocationId() == null ) {
			SolarNode node = solarNodeDao.get(datum.getNodeId());
			if ( node != null ) {
				datum.setLocationId(node.getWeatherLocationId());
			}
		}
		// see if exists already for given date
		WeatherDatum entity = dao.getDatumForDate(datum.getLocationId(), datum.getInfoDate());
		if ( entity == null || !entity.isSameInfoDate(datum) ) {
			return datum;
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	private <D extends Datum> D persistDatum(D datum, DatumDao<Datum> dao) {
		Long id = dao.storeDatum(datum);
		return (D) dao.getDatum(id);
	}

	private Datum checkForNodeDatumByDate(NodeDatum datum, DatumDao<? extends Datum> dao,
			Set<String> copyPropertiesIgnore) {
		Datum entity = dao.getDatumForDate(datum.getNodeId(), datum.getCreated());
		if ( entity != null ) {
			// copy non-null properties from posted data onto entity
			Map<String, Object> propsToCopy = ClassUtils.getBeanProperties(datum, copyPropertiesIgnore);
			BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(entity);
			bean.setPropertyValues(propsToCopy);
			return entity;
		}
		return datum;
	}

	private Datum checkForLocationDatumByDate(LocationDatum datum, DatumDao<? extends Datum> dao,
			Set<String> copyPropertiesIgnore) {
		Datum entity = dao.getDatumForDate(datum.getLocationId(), datum.getCreated());
		if ( entity != null ) {
			// copy non-null properties from posted data onto entity
			Map<String, Object> propsToCopy = ClassUtils.getBeanProperties(datum, copyPropertiesIgnore);
			BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(entity);
			bean.setPropertyValues(propsToCopy);
			return entity;
		}
		return datum;
	}

	public Map<String, DatumDao<Datum>> getDaoMapping() {
		return daoMapping;
	}

	public void setDaoMapping(Map<String, DatumDao<Datum>> daoMapping) {
		this.daoMapping = daoMapping;
	}

	public SolarNodeDao getSolarNodeDao() {
		return solarNodeDao;
	}

	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

	public PriceLocationDao getPriceLocationDao() {
		return priceLocationDao;
	}

	public void setPriceLocationDao(PriceLocationDao priceLocationDao) {
		this.priceLocationDao = priceLocationDao;
	}

	public WeatherLocationDao getWeatherLocationDao() {
		return weatherLocationDao;
	}

	public void setWeatherLocationDao(WeatherLocationDao weatherLocationDao) {
		this.weatherLocationDao = weatherLocationDao;
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

}
