/* ===================================================================
 * AbstractDataCollector.java
 *
 * Created Aug 31, 2008 2:49:36 PM
 *
 * Copyright (c) 2008 SolarNetwork.net Dev Team.
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

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.BasicSecurityException;

/**
 * Base class for data collector implementations.
 *
 * @author matt
 * @version 2.2
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

	private DataCollectorBiz dataCollectorBiz;
	private SolarNodeDao solarNodeDao;
	private String viewName = DEFAULT_VIEW_NAME;

	@Autowired(required = false)
	private InstructorBiz instructorBiz;

	/** A class-level logger. */
	protected final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Warn that POST is required.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED, reason = "POST required")
	public void getData() {
		// nothing to do
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
	 * Get the currently authenticated node.
	 *
	 * @param required
	 *        <em>true</em> if AuthenticatedNode is required, or <em>false</em>
	 *        if not
	 * @return AuthenticatedNode
	 * @throws BasicSecurityException
	 *         if an AuthenticatedNode is not available and {@code required} is
	 *         <em>true</em>
	 */
	protected AuthenticatedNode getAuthenticatedNode(boolean required) {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if ( principal instanceof AuthenticatedNode n ) {
			return n;
		}
		if ( required ) {
			throw new BasicSecurityException("Authenticated node required but not avaialble");
		}
		return null;
	}

	/**
	 * Get the node DAO.
	 *
	 * @return the solarNodeDao
	 */
	public SolarNodeDao getSolarNodeDao() {
		return solarNodeDao;
	}

	/**
	 * Set the node DAO.
	 *
	 * @param solarNodeDao
	 *        the solarNodeDao to set
	 */
	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

	/**
	 * Get the view name.
	 *
	 * @return the viewName
	 */
	public String getViewName() {
		return viewName;
	}

	/**
	 * Set the view name.
	 *
	 * @param viewName
	 *        the viewName to set
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * Get the data collector biz.
	 *
	 * @return the dataCollectorBiz
	 */
	public DataCollectorBiz getDataCollectorBiz() {
		return dataCollectorBiz;
	}

	/**
	 * Set the data collector biz.
	 *
	 * @param dataCollectorBiz
	 *        the dataCollectorBiz to set
	 */
	public void setDataCollectorBiz(DataCollectorBiz dataCollectorBiz) {
		this.dataCollectorBiz = dataCollectorBiz;
	}

	/**
	 * Get the instructor biz.
	 *
	 * @return the instructorBiz
	 */
	public InstructorBiz getInstructorBiz() {
		return instructorBiz;
	}

	/**
	 * Set the instructor biz.
	 *
	 * @param instructorBiz
	 *        the instructorBiz to set
	 */
	public void setInstructorBiz(InstructorBiz instructorBiz) {
		this.instructorBiz = instructorBiz;
	}

}
