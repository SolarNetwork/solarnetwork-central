/* ==================================================================
 * LocationLookupController.java - Feb 20, 2011 7:54:21 PM
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
 */

package net.solarnetwork.central.in.web;

import java.util.List;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.web.domain.Response;

/**
 * Web access to PriceLocation data.
 * 
 * @author matt
 * @version 2.0
 */
@Controller
public class LocationLookupController {

	/** The default value for the {@code viewName} property. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	/** The model key for the {@code PriceLocation} result. */
	public static final String MODEL_KEY_RESULT = "result";

	private DataCollectorBiz dataCollectorBiz;
	private String viewName = DEFAULT_VIEW_NAME;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public LocationLookupController() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz
	 *        the {@link DataCollectorBiz} to use
	 */
	@Autowired
	public LocationLookupController(DataCollectorBiz dataCollectorBiz) {
		setDataCollectorBiz(dataCollectorBiz);
	}

	private class CriteriaValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(Object target, Errors errors) {
			if ( target instanceof SourceLocationFilter ) {
				boolean sourceRequired = true;
				SourceLocationFilter filter = (SourceLocationFilter) target;
				if ( filter.getId() != null || (target instanceof GenericSourceLocationFilter
						&& ((GenericSourceLocationFilter) target)
								.getType() == GenericSourceLocationFilter.LocationType.Basic) ) {
					sourceRequired = false;
				}
				if ( sourceRequired ) {
					if ( !StringUtils.hasText(filter.getSourceName()) ) {
						errors.rejectValue("sourceName", "error.field.required",
								new Object[] { "sourceName" }, "Field is required.");
					}
					if ( !StringUtils.hasText(filter.getLocationName()) ) {
						errors.rejectValue("locationName", "error.field.required",
								new Object[] { "locationName" }, "Field is required.");
					}
				}
			}
		}
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new CriteriaValidator());
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Handle an {@link RuntimeException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 */
	@ExceptionHandler({ BindException.class, RuntimeException.class })
	public ModelAndView handleRuntimeException(Exception e) {
		log.error("BindException in {} controller", getClass().getSimpleName(), e);
		ModelAndView mv = new ModelAndView(getViewName(), MODEL_KEY_RESULT,
				new Response<Object>(false, null, e.getMessage(), null));
		return mv;
	}

	/**
	 * Query for any supported location type.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @param model
	 *        the model
	 * @return the result view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = { "/locationSearch.*", "/u/locationSearch.*" })
	public String searchForLocations(@Valid GenericSourceLocationFilter criteria, Model model) {
		List<? extends EntityMatch> matches = getDataCollectorBiz()
				.findLocations(criteria.getLocation());
		if ( matches != null && matches.size() > 0 ) {
			model.asMap().clear();
			model.addAttribute(MODEL_KEY_RESULT, matches);
		}
		return getViewName();
	}

	public DataCollectorBiz getDataCollectorBiz() {
		return dataCollectorBiz;
	}

	public void setDataCollectorBiz(DataCollectorBiz dataCollectorBiz) {
		this.dataCollectorBiz = dataCollectorBiz;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

}
