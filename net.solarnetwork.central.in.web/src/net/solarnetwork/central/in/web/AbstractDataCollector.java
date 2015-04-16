/* ===================================================================
 * AbstractDataCollector.java
 * 
 * Created Aug 31, 2008 2:49:36 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 */

package net.solarnetwork.central.in.web;

import java.beans.PropertyEditor;
import java.util.List;
import javax.annotation.Resource;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;
import net.solarnetwork.util.OptionalServiceTracker;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Base class for data collector implementations.
 *
 * @author matt.magoffin
 * @version 1.1
 */
public abstract class AbstractDataCollector {

	/** The default value for the {@code viewName} property. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	/** The model key for the {@code Datum} result. */
	public static final String MODEL_KEY_RESULT = "result";

	/** The model key for the collection of {@code Instruction} results. */
	public static final String MODEL_KEY_INSTRUCTIONS = "instructions";

	/** The model key for the node's {@code TimeZone}. */
	public static final String MODEL_KEY_NODE_TZ = "node-tz";

	/** The date format to use for parsing dates. */
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

	/** The date format to use for parsing dates. */
	public static final String DATE_FORMAT = "yyyy-MM-dd";

	/** The date format to use for parsing times. */
	public static final String TIME_FORMAT = "HH:mm";

	/** A cloneable PropertyEditor for the {@link #DATE_FORMAT}. */
	protected static final JodaDateFormatEditor LOCAL_DATE_EDITOR = new JodaDateFormatEditor(
			DATE_FORMAT, ParseMode.LocalDate);

	/** A cloneable PropertyEditor for the {@link #TIME_FORMAT}. */
	protected static final JodaDateFormatEditor LOCAL_TIME_EDITOR = new JodaDateFormatEditor(
			TIME_FORMAT, ParseMode.LocalTime);

	/** A cloneable PropertyEditor for the {@link #DATE_TIME_FORMAT}. */
	protected static final JodaDateFormatEditor DATE_TIME_EDITOR = new JodaDateFormatEditor(
			DATE_TIME_FORMAT, ParseMode.DateTime);

	private DataCollectorBiz dataCollectorBiz;
	private SolarNodeDao solarNodeDao;
	private String viewName = DEFAULT_VIEW_NAME;

	@Resource
	private OptionalServiceTracker<InstructorBiz> instructorBiz;

	/** A class-level logger. */
	protected final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Warn that POST is required.
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String getData() {
		return "post-required";
	}

	/**
	 * Default handling of node instructions.
	 * 
	 * <p>
	 * This method will use the configured {@link #getInstructorBiz()}, if
	 * available, and put the returned instructions on the {@code Model} on the
	 * {@link #MODEL_KEY_INSTRUCTIONS} key.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID to get instructions for
	 * @param model
	 *        the model
	 */
	protected void defaultHandleNodeInstructions(Long nodeId, Model model) {
		// look for instructions to return for the given node
		if ( getInstructorBiz() != null && getInstructorBiz().isAvailable() ) {
			List<Instruction> instructions = getInstructorBiz().getService()
					.getActiveInstructionsForNode(nodeId);
			if ( instructions.size() > 0 ) {
				model.addAttribute(MODEL_KEY_INSTRUCTIONS, instructions);
			}
		}
	}

	/**
	 * Add a SolarNode's TimeZone to the result model.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param model
	 *        the model
	 * @return the SolarNode entity
	 */
	protected SolarNode setupNodeTimeZone(Long nodeId, Model model) {
		SolarNode node = solarNodeDao.get(nodeId);
		model.asMap().remove("weatherDatum");
		if ( node != null ) {
			model.addAttribute(MODEL_KEY_NODE_TZ, node.getTimeZone());
		}
		return node;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(DateTime.class, (PropertyEditor) DATE_TIME_EDITOR.clone());
	}

	/**
	 * Get the currently authenticated node.
	 * 
	 * @param required
	 *        <em>true</em> if AuthenticatedNode is required, or <em>false</em>
	 *        if not
	 * @return AuthenticatedNode
	 * @throws SecurityException
	 *         if an AuthenticatedNode is not available and {@code required} is
	 *         <em>true</em>
	 */
	protected AuthenticatedNode getAuthenticatedNode(boolean required) {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if ( principal instanceof AuthenticatedNode ) {
			return (AuthenticatedNode) principal;
		}
		if ( required ) {
			throw new SecurityException("Authenticated node required but not avaialble");
		}
		return null;
	}

	/**
	 * @return the solarNodeDao
	 */
	public SolarNodeDao getSolarNodeDao() {
		return solarNodeDao;
	}

	/**
	 * @param solarNodeDao
	 *        the solarNodeDao to set
	 */
	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

	/**
	 * @return the viewName
	 */
	public String getViewName() {
		return viewName;
	}

	/**
	 * @param viewName
	 *        the viewName to set
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * @return the dataCollectorBiz
	 */
	public DataCollectorBiz getDataCollectorBiz() {
		return dataCollectorBiz;
	}

	/**
	 * @param dataCollectorBiz
	 *        the dataCollectorBiz to set
	 */
	public void setDataCollectorBiz(DataCollectorBiz dataCollectorBiz) {
		this.dataCollectorBiz = dataCollectorBiz;
	}

	/**
	 * @return the instructorBiz
	 */
	public OptionalServiceTracker<InstructorBiz> getInstructorBiz() {
		return instructorBiz;
	}

	/**
	 * @param instructorBiz
	 *        the instructorBiz to set
	 */
	public void setInstructorBiz(OptionalServiceTracker<InstructorBiz> instructorBiz) {
		this.instructorBiz = instructorBiz;
	}

}
