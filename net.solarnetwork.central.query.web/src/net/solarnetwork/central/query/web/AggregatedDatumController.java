/* ===================================================================
 * AggregatedDatumController.java
 * 
 * Created Aug 4, 2009 10:15:02 AM
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

import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.central.datum.dao.DatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.AbstractNodeController;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for querying and viewing aggregated {@link NodeDatum} data.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>datumClass</dt>
 *   <dd>The type of NodeDatum to obtain aggregated data of. This can also
 *   be configured by calling the {@link #setDatumDao(DatumDao)} method.</dd>
 *   
 *   <dt>defaultTimeZone</dt>
 *   <dd>The default time zone to use when no {@link SolarNode} ID is provided
 *   in the command. For aggregated data across nodes, this is the time zone
 *   that will be to create the start/end dates (if not provided in the command)
 *   and also returned to the view on the {@link #MODEL_KEY_TZ} key. When
 *   the {@link DatumQueryCommand#getNodeId()} is provided, the time zone for
 *   that node will be used instead of this default value. Defaults to
 *   {@code GMT+12}.</dd> 
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class AggregatedDatumController extends AbstractNodeController {
	
	/** The model key for the {@code List&lt;NodeDatum&gt;} results. */
	public static final String MODEL_KEY_DATA_LIST = "data";
	
	/** The model key for the client {@link TimeZone} results. */
	public static final String MODEL_KEY_TZ = "tz";
	
	private TimeZone defaultTimeZone = TimeZone.getTimeZone("GMT+12");
	@Autowired private QueryBiz queryBiz;
	private Class<? extends NodeDatum> datumClass;
	
	/**
	 * Query for consumption data.
	 * 
	 * @param cmd the query criteria
	 * @param request the servlet request
	 * @return model and view
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView getData(DatumQueryCommand cmd, HttpServletRequest request) {
		ModelAndView mv = resolveViewFromUrlExtension(request);

		TimeZone tz = this.defaultTimeZone;
		if ( cmd.getNodeId() != null ) {
			// get the SolarNode for the specified node, for the appropriate time zone
			SolarNode node = getSolarNodeDao().get(cmd.getNodeId());
			if ( node != null ) {
				tz = node.getTimeZone();
			}
		}
		
		// make sure required criteria values are set
		if ( cmd.getStartDate() == null ) {
			// default to 1 day ago
			cmd.setStartDate(new DateTime(DateTimeZone.forTimeZone(tz)).minusDays(1));
		}
		if ( cmd.getEndDate() == null ) {
			cmd.setEndDate(new DateTime(DateTimeZone.forTimeZone(tz)));
		}
		if ( cmd.getPrecision() == null ) {
			cmd.setPrecision(60);
		}
		
		// execute query, and return results
		List<? extends NodeDatum> results = queryBiz.getAggregatedDatum(datumClass, cmd);
		mv.addObject(MODEL_KEY_DATA_LIST, results);
        mv.addObject(MODEL_KEY_TZ, tz);
        
		return mv;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		initBinderDateFormatEditor(binder);
	}
	
	/**
	 * Set the {@code datumClass} via the {@link DatumDao#getDatumType()} method.
	 * 
	 * @param dao the DAO from which to infer the datum class from
	 */
	public void setDatumDao(DatumDao<NodeDatum> dao) {
		setDatumClass(dao.getDatumType());
	}
	
	/**
	 * @return the defaultTimeZone
	 */
	public TimeZone getDefaultTimeZone() {
		return defaultTimeZone;
	}
	
	/**
	 * @param defaultTimeZone the defaultTimeZone to set
	 */
	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * @return the queryBiz
	 */
	public QueryBiz getQueryBiz() {
		return queryBiz;
	}

	/**
	 * @param queryBiz the queryBiz to set
	 */
	public void setQueryBiz(QueryBiz queryBiz) {
		this.queryBiz = queryBiz;
	}

	/**
	 * @return the datumClass
	 */
	public Class<? extends NodeDatum> getDatumClass() {
		return datumClass;
	}

	/**
	 * @param datumClass the datumClass to set
	 */
	public void setDatumClass(Class<? extends NodeDatum> datumClass) {
		this.datumClass = datumClass;
	}
	
}
