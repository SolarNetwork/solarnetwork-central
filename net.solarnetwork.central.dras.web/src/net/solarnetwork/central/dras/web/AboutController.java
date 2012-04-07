/* ==================================================================
 * AboutController.java - Jun 17, 2011 6:51:55 PM
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

package net.solarnetwork.central.dras.web;

import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.web.support.WebUtils;

import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for application details.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/about")
public class AboutController extends ControllerSupport implements BundleContextAware {

	private static final String[] DEFAULT_KEYS
	= new String[] {"Bundle-Name", "Bundle-SymbolicName", "Bundle-Version"};

	private BundleContext bundleContext;
	
	@Autowired private MessageSource messageSource;
	
	@Override
	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
	
	@ModelAttribute("app")
	public Map<String, Object> getApplicationDetails(Locale locale) {
		Map<String, Object> result = new LinkedHashMap<String, Object>(5);
		if ( bundleContext != null ) {
			Dictionary<?, ?> dic = bundleContext.getBundle().getHeaders();
			for ( String key : DEFAULT_KEYS ) {
				Object val = dic.get(key);
				Map<String, Object> value = new LinkedHashMap<String, Object>(2);
				value.put("value", val);
				if ( messageSource != null ) {
					String msg = messageSource.getMessage("about.field."+key, null, locale);
					value.put("title", msg);
				}
				result.put(key, value);
			}
		}
		return result;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/app.*")
	public String version(HttpServletRequest request) {
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

}
