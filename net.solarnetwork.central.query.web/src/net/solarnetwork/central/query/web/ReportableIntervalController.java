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
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadableInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
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
	@RequestMapping(value = "/reportableInterval.*", method = {RequestMethod.POST, RequestMethod.GET})
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
	 * Get the set of available source IDs for a given node and data type, 
	 * optionally filtered by a date range.
	 * 
	 * @param cmd the command
	 * @return the set of source IDs
	 */
	@RequestMapping(value = "/availableSources.*", method = RequestMethod.GET)
	@ResponseBody
	public Set<String> getAvailableSources(Command cmd) {
		if ( cmd.types == null || cmd.types.length < 1 ) {
			return Collections.emptySet();
		}
		Class<? extends NodeDatum> type = cmd.types[0].getDatumTypeClass();
		return queryBiz.getAvailableSources(cmd.nodeId, type, cmd.getStart(), cmd.getEnd());
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
	 */
	public static final class Command {
		
		private Long nodeId;
		private IntervalType[] types;
		private LocalDate start;
		private LocalDate end;

		public IntervalType[] getTypes() {
			return types;
		}
		
		public void setTypes(IntervalType[] types) {
			this.types = types;
		}
		
		public void setType(IntervalType type) {
			this.types = new IntervalType[] {type};
		}
		
		public Long getNodeId() {
			return nodeId;
		}

		public void setNodeId(Long nodeId) {
			this.nodeId = nodeId;
		}

		public LocalDate getStart() {
			return start;
		}

		public void setStart(LocalDate start) {
			this.start = start;
		}

		public LocalDate getEnd() {
			return end;
		}

		public void setEnd(LocalDate end) {
			this.end = end;
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
