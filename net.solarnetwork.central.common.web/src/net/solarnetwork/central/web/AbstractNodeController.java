/* ===================================================================
 * AbstractNodeController.java
 * 
 * Created Aug 6, 2009 10:17:13 AM
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
 * $Id: AbstractNodeController.java 369 2009-09-24 06:24:47Z msqr $
 * ===================================================================
 */

package net.solarnetwork.central.web;

import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.util.CloningPropertyEditorRegistrar;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.web.support.WebUtils;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.ModelAndView;

/**
 * Abstract base class to support node-related controllers.
 * 
 * @author matt
 * @version $Revision: 369 $ $Date: 2009-09-24 18:24:47 +1200 (Thu, 24 Sep 2009)
 *          $
 */
public abstract class AbstractNodeController {

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

	private SolarNodeDao solarNodeDao;
	private String viewName;
	private String[] requestDateFormats = new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT };

	/**
	 * Resolve a ModelAndView with an empty model and a view name determined by
	 * the URL "suffix".
	 * 
	 * <p>
	 * If the {@link #getViewName()} method returns a value, that view name is
	 * used for every request. Otherwise, this sets the view name to the value
	 * of the URL "suffix", that is, everything after the last period in the
	 * URL. This uses {@link StringUtils#getFilenameExtension(String)} on the
	 * request URI to accomplish this. For example a URL like
	 * {@code /myController.json} would resolve to a view named {@code json}.
	 * This can be handy when you want to return different data formats for the
	 * same business logic, such as XML or JSON.
	 * </p>
	 * 
	 * @param request
	 *        the HTTP request
	 * @return a ModelAndView (never <em>null</em>)
	 */
	protected ModelAndView resolveViewFromUrlExtension(HttpServletRequest request) {
		String viewName = WebUtils.resolveViewFromUrlExtension(request, getViewName());
		return new ModelAndView(viewName);
	}

	/**
	 * Set up a {@link CloningPropertyEditorRegistrar} as a request attribute.
	 * 
	 * <p>
	 * This sets up a new {@link CloningPropertyEditorRegistrar} as a request
	 * attribute, which could be used by the view for serializing model
	 * properties in some way. A common use for this is to serialize
	 * {@link DateTime} objects into Strings, so this method accepts a
	 * {@code dateFormat} and {@code node} property which, if provided, will add
	 * a {@link JodaDateFormatEditor} to the registrar for all {@link DateTime}
	 * objects, configured with the node's time zone.
	 * </p>
	 * 
	 * @param request
	 *        the HTTP request
	 * @param dateFormat
	 *        an optional date format
	 * @param node
	 *        an optional node (required if {@code dateFormat} provided)
	 * @return the registrar
	 */
	protected CloningPropertyEditorRegistrar setupViewPropertyEditorRegistrar(
			HttpServletRequest request, String dateFormat, SolarNode node) {
		return setupViewPropertyEditorRegistrar(request, dateFormat,
				(node == null ? null : node.getTimeZone()));
	}

	/**
	 * Set up a {@link CloningPropertyEditorRegistrar} as a request attribute.
	 * 
	 * <p>
	 * This sets up a new {@link CloningPropertyEditorRegistrar} as a request
	 * attribute, which could be used by the view for serializing model
	 * properties in some way. A common use for this is to serialize
	 * {@link DateTime} objects into Strings, so this method accepts a
	 * {@code dateFormat} and {@code node} property which, if provided, will add
	 * a {@link JodaDateFormatEditor} to the registrar for all {@link DateTime}
	 * objects, configured with the node's time zone.
	 * </p>
	 * 
	 * @param request
	 *        the HTTP request
	 * @param dateFormat
	 *        an optional date format
	 * @param timeZone
	 *        an optional node (required if {@code dateFormat} provided)
	 * @return the registrar
	 */
	protected CloningPropertyEditorRegistrar setupViewPropertyEditorRegistrar(
			HttpServletRequest request, String dateFormat, TimeZone timeZone) {
		// set up a PropertyEditorRegistrar that can be used for serializing data into view-friendly values
		CloningPropertyEditorRegistrar registrar = new CloningPropertyEditorRegistrar();
		if ( dateFormat != null && timeZone != null ) {
			// TODO implement caching of JodaDateFormatEditors based on dateFormat + time zone
			registrar.setPropertyEditor(DateTime.class, new JodaDateFormatEditor(dateFormat, timeZone));
		}
		request.setAttribute("propertyEditorRegistrar", registrar);
		return registrar;
	}

	/**
	 * Add a {@link DateTime} property editor, using the
	 * {@link #getRequestDateFormat()} pattern.
	 * 
	 * <p>
	 * This is typically called from an "init binder" method.
	 * </p>
	 * 
	 * @param binder
	 *        the binder to add the editor to
	 */
	protected void initBinderDateFormatEditor(WebDataBinder binder) {
		binder.registerCustomEditor(DateTime.class, new JodaDateFormatEditor(this.requestDateFormats,
				null));
	}

	/**
	 * Get the first request date format.
	 * 
	 * @return the requestDateFormat
	 */
	public String getRequestDateFormat() {
		if ( requestDateFormats == null || requestDateFormats.length < 1 ) {
			return null;
		}
		return requestDateFormats[0];
	}

	/**
	 * Set a single request date format.
	 * 
	 * @param requestDateFormat
	 *        the requestDateFormat to set
	 */
	public void setRequestDateFormat(String requestDateFormat) {
		this.requestDateFormats = new String[] { requestDateFormat };
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
	 * @return the requestDateFormats
	 */
	public String[] getRequestDateFormats() {
		return requestDateFormats;
	}

	/**
	 * @param requestDateFormats
	 *        the requestDateFormats to set
	 */
	public void setRequestDateFormats(String[] requestDateFormats) {
		this.requestDateFormats = requestDateFormats;
	}

}
