/* ===================================================================
 * DaoQueryBiz.java
 * 
 * Created Aug 5, 2009 12:31:45 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.query.biz.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.dao.DatumDao;
import net.solarnetwork.central.datum.dao.DayDatumDao;
import net.solarnetwork.central.datum.dao.HardwareControlDatumDao;
import net.solarnetwork.central.datum.dao.PowerDatumDao;
import net.solarnetwork.central.datum.dao.PriceDatumDao;
import net.solarnetwork.central.datum.dao.WeatherDatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import org.joda.time.LocalDate;
import org.joda.time.MutableInterval;
import org.joda.time.ReadableInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link QueryBiz}.
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class DaoQueryBiz implements QueryBiz {

	private ConsumptionDatumDao consumptionDatumDao;
	private PowerDatumDao powerDatumDao;
	private SolarNodeDao solarNodeDao;

	private final Map<Class<? extends NodeDatum>, DatumDao<? extends NodeDatum>> daoMapping;

	/**
	 * Default constructor.
	 */
	public DaoQueryBiz() {
		super();
		daoMapping = new HashMap<Class<? extends NodeDatum>, DatumDao<? extends NodeDatum>>(4);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getReportableInterval(Long nodeId, Class<? extends NodeDatum>[] types) {
		MutableInterval interval = new MutableInterval(0, 0);
		for ( Class<? extends NodeDatum> clazz : types ) {
			ReadableInterval oneInterval = null;
			if ( consumptionDatumDao.getDatumType().isAssignableFrom(clazz) ) {
				oneInterval = consumptionDatumDao.getReportableInterval(nodeId);
			} else if ( powerDatumDao.getDatumType().isAssignableFrom(clazz) ) {
				oneInterval = powerDatumDao.getReportableInterval(nodeId);
			}
			if ( oneInterval != null ) {
				if ( interval.getEndMillis() == 0
						|| oneInterval.getEndMillis() > interval.getEndMillis() ) {
					interval.setEndMillis(oneInterval.getEndMillis());
				}
				if ( interval.getStartMillis() == 0
						|| oneInterval.getStartMillis() < interval.getStartMillis() ) {
					interval.setStartMillis(oneInterval.getStartMillis());
				}
			}
		}
		if ( interval.getStartMillis() == 0 ) {
			return null;
		}
		TimeZone tz = null;
		if ( nodeId != null ) {
			SolarNode node = solarNodeDao.get(nodeId);
			if ( node != null ) {
				tz = node.getTimeZone();
			}
		}
		return new ReportableInterval(interval, tz);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long nodeId, Class<? extends NodeDatum> type,
			LocalDate start, LocalDate end) {
		final Set<String> result;
		if ( consumptionDatumDao.getDatumType().isAssignableFrom(type) ) {
			result = consumptionDatumDao.getAvailableSources(nodeId, start, end);
		} else if ( powerDatumDao.getDatumType().isAssignableFrom(type) ) {
			result = powerDatumDao.getAvailableSources(nodeId, start, end);
		} else {
			result = Collections.emptySet();
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getNetworkReportableInterval(Class<? extends NodeDatum>[] types) {
		return getReportableInterval(null, types);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<? extends NodeDatum> getAggregatedDatum(Class<? extends NodeDatum> datumClass,
			DatumQueryCommand criteria) {
		DatumDao<? extends NodeDatum> dao = daoMapping.get(datumClass);
		if ( dao == null ) {
			throw new IllegalArgumentException("Datum type "
					+ (datumClass == null ? "(null)" : datumClass.getSimpleName()) + " not supported");
		}
		if ( criteria.isMostRecent() ) {
			return dao.getMostRecentDatum(criteria);
		}
		return dao.getAggregatedDatum(criteria);
	}

	@Autowired
	public void setDayDatumDao(DayDatumDao dayDatumDao) {
		daoMapping.put(dayDatumDao.getDatumType().asSubclass(NodeDatum.class), dayDatumDao);
	}

	@Autowired
	public void setPowerDatumDao(PowerDatumDao powerDatumDao) {
		this.powerDatumDao = powerDatumDao;
		daoMapping.put(powerDatumDao.getDatumType().asSubclass(NodeDatum.class), powerDatumDao);
	}

	@Autowired
	public void setWeatherDatumDao(WeatherDatumDao weatherDatumDao) {
		daoMapping.put(weatherDatumDao.getDatumType().asSubclass(NodeDatum.class), weatherDatumDao);
	}

	@Autowired
	public void setConsumptionDatumDao(ConsumptionDatumDao consumptionDatumDao) {
		this.consumptionDatumDao = consumptionDatumDao;
		daoMapping.put(consumptionDatumDao.getDatumType().asSubclass(NodeDatum.class),
				consumptionDatumDao);
	}

	@Autowired
	public void setPriceDatumDao(PriceDatumDao priceDatumDao) {
		daoMapping.put(priceDatumDao.getDatumType().asSubclass(NodeDatum.class), priceDatumDao);
	}

	@Autowired
	public void setHardwareControlDatumDao(HardwareControlDatumDao hardwareControlDatumDao) {
		daoMapping.put(hardwareControlDatumDao.getDatumType().asSubclass(NodeDatum.class),
				hardwareControlDatumDao);
	}

	@Autowired
	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

}
