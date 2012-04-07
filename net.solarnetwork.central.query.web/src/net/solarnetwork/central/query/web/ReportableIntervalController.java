/* ===================================================================
 * ReportableIntervalController.java
 * 
 * Created Aug 5, 2009 3:12:15 PM
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

package net.solarnetwork.central.query.web;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.AbstractNodeController;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadableInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for querying for reportable interval values.
 * 
 * <p>See the {@link ReportableInterval} class for information about what is returned to
 * the view.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
@Controller
@RequestMapping("/reportableInterval.*")
public class ReportableIntervalController extends AbstractNodeController {
	
	private QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param solarNodeDao the SolarNodeDao to use
	 * @param queryBiz the QueryBiz to use
	 */
	@Autowired
	public ReportableIntervalController(SolarNodeDao solarNodeDao, QueryBiz queryBiz) {
		super();
		setSolarNodeDao(solarNodeDao);
		this.queryBiz = queryBiz;
	}
	
	/**
	 * Get a reportable interval for a node and list of NodeDatum types.
	 * 
	 * @param cmd the command
	 * @param request the request
	 * @return the model and view
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView getReportableInterval(Command cmd, HttpServletRequest request) {
		ModelAndView mv = resolveViewFromUrlExtension(request);
		
		// get the SolarNode for the specified node, for the appropriate time zone
		SolarNode node = cmd.getNodeId() == null 
			? null : getSolarNodeDao().get(cmd.getNodeId());
		
		List<Class<? extends NodeDatum>> typeList = new ArrayList<Class<? extends NodeDatum>>();
		for ( IntervalType type : cmd.getTypes() ) {
			typeList.add(type.getDatumTypeClass());
		}
		ReadableInterval interval = queryBiz.getReportableInterval(cmd.getNodeId(), 
				typeList.toArray(new Class[0]));
		ReportableInterval data = new ReportableInterval(interval, 
				node == null ? null : node.getTimeZone());
		mv.addObject("data", data);
		if ( node != null ) {
			mv.addObject("tz", node.getTimeZone());
		}

		if ( node != null ) {
			// set up a PropertyEditorRegistrar that can be used for serializing data into view-friendly values
			setupViewPropertyEditorRegistrar(request, DEFAULT_DATE_FORMAT, node);
		}
		return mv;
	}
	
	/**
	 * Enum type for use in the command.
	 */
	public static enum IntervalType {
		
		/** ConsumptionDatum */
		Consumption,
		
		/** PowerDatum. */
		Power;
		
		/**
		 * Get a NodeDatum class type for this enum value.
		 * 
		 * @return the class type
		 */
		public Class<? extends NodeDatum> getDatumTypeClass() {
			switch ( this ) {
				case Consumption:
					return ConsumptionDatum.class;
					
				case Power:
					return PowerDatum.class;
					
				default:
					return null;
			}
		}
	}
	
	/**
	 * Command object.
	 * 
	 * <p>The {@code types} array should be a list of {@link NodeDatum} "simple" class names, 
	 * e.g. {@code PowerDatum} or {@code ConsumptionDatum}.</p>
	 *
	 * @author matt
	 * @version $Revision$ $Date$
	 */
	public static final class Command {
		
		private Long nodeId;
		private IntervalType[] types;

		/**
		 * @return the types
		 */
		public IntervalType[] getTypes() {
			return types;
		}
		
		/**
		 * @param types the types to set
		 */
		public void setTypes(IntervalType[] types) {
			this.types = types;
		}
		
		/**
		 * @return the nodeId
		 */
		public Long getNodeId() {
			return nodeId;
		}

		/**
		 * @param nodeId the nodeId to set
		 */
		public void setNodeId(Long nodeId) {
			this.nodeId = nodeId;
		}
		
	}
	
	/**
	 * Result view object.
	 */
	public static final class ReportableInterval {
		
		private ReadableInterval interval;
		private TimeZone tz;
		
		/**
		 * Constructor.
		 * 
		 * @param interval the interval
		 * @param tz the node's time zone (may be <em>null</em>)
		 */
		public ReportableInterval(ReadableInterval interval, TimeZone tz) {
			this.interval = interval;
			this.tz = tz;
		}
		
		/**
		 * Get a count of days this interval spans (inclusive).
		 * 
		 * <p>This is the complete number of calendar days the data is
		 * present in, so partial days are counted as full days. For
		 * example, the interval {@code 2008-08-11/2009-08-05} returns 360.</p>
		 * 
		 * @return count of days within the interval
		 */
		public Long getDayCount() {
			if ( interval == null ) {
				return Long.valueOf(0);
			}
			return Long.valueOf(interval.toPeriod(PeriodType.days()).getDays() + 1);
		}
		
		/**
		 * Get a count of months this interval spans (inclusive).
		 * 
		 * <p>This is the complete number of calendar months the data is
		 * present in, so partial months are counted as full months. For
		 * example, the interval {@code 2008-08-11/2009-08-05} returns 13.</p>
		 * 
		 * @return count of months within the interval
		 */
		public Long getMonthCount() {
			if ( interval == null ) {
				return Long.valueOf(0);
			}
			ReadableInstant s = interval.getStart().withDayOfMonth(
					interval.getStart().dayOfMonth().getMinimumValue());
			ReadableInstant e = interval.getEnd().withDayOfMonth(
					interval.getEnd().dayOfMonth().getMaximumValue());
			Period p = new Period(s, e, PeriodType.months());
			return Long.valueOf(p.getMonths() + 1);
		}
		
		/**
		 * Get a count of years this interval spans (inclusive).
		 * 
		 * <p>This is the complete number of calendar years the data is
		 * present in, so partial years are counted as full years. For
		 * example, the interval {@code 2008-08-11/2009-08-05} returns 2.</p>
		 * 
		 * @return count of months within the interval
		 */
		public Long getYearCount() {
			if ( interval == null ) {
				return Long.valueOf(0);
			}
			int s = interval.getStart().getYear();
			int e = interval.getEnd().getYear();
			return Long.valueOf((e - s) + 1);
		}
		
		/**
		 * @return the startDate
		 */
		public LocalDateTime getStartDate() {
			if ( interval == null ) {
				return null;
			}
			return tz == null ? interval.getStart().toLocalDateTime()
					: interval.getStart().toDateTime(DateTimeZone.forTimeZone(tz)).toLocalDateTime();
		}
		
		/**
		 * @return the endDate
		 */
		public LocalDateTime getEndDate() {
			if ( interval == null ) {
				return null;
			}
			return tz == null ? interval.getEnd().toLocalDateTime()
					: interval.getEnd().toDateTime(DateTimeZone.forTimeZone(tz)).toLocalDateTime();
		}
		
	}
	
}
