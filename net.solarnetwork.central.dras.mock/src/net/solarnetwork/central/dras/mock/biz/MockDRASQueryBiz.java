/* ==================================================================
 * MockDRASQueryBiz.java - May 1, 2011 4:49:24 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.mock.biz;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.datum.domain.ReportingConsumptionDatum;
import net.solarnetwork.central.datum.domain.ReportingPowerDatum;
import net.solarnetwork.central.query.biz.QueryBiz;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.MutableDateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInterval;

/**
 * Mock implementation of QueryBiz that supports the {@link MockDRASObserverBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class MockDRASQueryBiz implements QueryBiz {
	
	private final ReadableInterval reportableInterval;
	
	private final float consumptionWattHours = 1000.0f;
	private final float generationWattHours = 100.0f;
	
	/**
	 * Constructor.
	 * 
	 * @param observerBiz the observer biz to setup mock data with
	 */
	public MockDRASQueryBiz(MockDRASObserverBiz observerBiz) {
		DateTimeZone tz = DateTimeZone.forID("Pacific/Auckland");
		reportableInterval = new Interval(
				new DateTime(2011, 1, 1, 8, 0, 0, 0, tz),
				new DateTime(2011, 2, 1, 8, 0, 0, 0, tz));
		/*
		for ( Program program : observerBiz.getAllPrograms(null) ) {
			for ( NodeIdentity node : observerBiz.getProgramParticipants(program, null, null)) {
				
				
			}
		}
		*/
	}

	@Override
	public ReadableInterval getReportableInterval(Long nodeId,
			Class<? extends NodeDatum>[] types) {
		return reportableInterval;
	}

	@Override
	public ReadableInterval getNetworkReportableInterval(
			Class<? extends NodeDatum>[] types) {
		return reportableInterval;
	}

	@Override
	public List<? extends NodeDatum> getAggregatedDatum(
			Class<? extends NodeDatum> datumClass, DatumQueryCommand criteria) {
		MutableDateTime mdt = new MutableDateTime(criteria.getStartDate());
		Period period;
		switch ( criteria.getAggregate() ) {
		case Hour:
			period = Period.hours(1);
			break;
			
		case Day:
			period = Period.days(1);
			break;
			
		case Week:
			period = Period.weeks(1);
			break;
			
		case Month:
			period = Period.months(1);
			break;
			
		default:
			period = Period.minutes(1);
		}
		List<NodeDatum> results = new ArrayList<NodeDatum>();
		do {
			NodeDatum datum = null;
			if ( ConsumptionDatum.class.isAssignableFrom(datumClass) ) {
				ReportingConsumptionDatum d = new ReportingConsumptionDatum();
				d.setNodeId(criteria.getNodeId());
				d.setCreated(mdt.toDateTime());
				Duration dur = period.toDurationFrom(mdt);
				float hours = (float)((double)dur.getMillis() / (double)(1000 * 60 * 60));
				d.setWattHours(Double.valueOf(hours * consumptionWattHours));
				datum = d;
			} else if ( PowerDatum.class.isAssignableFrom(datumClass) ) {
				ReportingPowerDatum d = new ReportingPowerDatum();
				d.setNodeId(criteria.getNodeId());
				d.setCreated(mdt.toDateTime());
				Duration dur = period.toDurationFrom(mdt);
				float hours = (float)((double)dur.getMillis() / (double)(1000 * 60 * 60));
				d.setWattHours(Double.valueOf(hours * generationWattHours));
				datum = d;
			}
			if ( datum != null ) {
				results.add(datum);
			}
			mdt.add(period);
		} while (mdt.isBefore(criteria.getEndDate()));
		return results;
	}

}
