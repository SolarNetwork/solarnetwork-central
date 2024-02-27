/* ==================================================================
 * MessageController.java - Jun 18, 2011 11:57:42 AM
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

package net.solarnetwork.central.web;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import net.solarnetwork.web.jakarta.support.MessagesSource;
import net.solarnetwork.web.jakarta.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for returning i18n message resources.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/msg")
public class MessageController {
	
	@Autowired private MessageSource messageSource;

	private String viewName;

	/**
	 * Get a single message.
	 * 
	 * @param request the request
	 * @param model the view model
	 * @param locale the locale
	 * @param msgKey the message key
	 * @param params optional parameters for the message
	 * @return the view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/msg.*")
	public String getMessage(HttpServletRequest request, Model model, Locale locale,
			@RequestParam("key") String msgKey, 
			@RequestParam(required = false, value = "param") String[] params) {
		String value = messageSource.getMessage(msgKey, params, locale);
		model.addAttribute("message", value);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get all messages.
	 * 
	 * @param request the request
	 * @param model the view model
	 * @param locale the locale
	 * @return the view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/msgs.*")
	public String getAllMessages(HttpServletRequest request, Model model, Locale locale) {
		if ( !(messageSource instanceof MessagesSource) ) {
			throw new RuntimeException("MessageSource does not implement MessagesSource.");
		}
		MessagesSource ms = (MessagesSource)messageSource;
		Enumeration<String> enumeration = ms.getKeys(locale);
		Map<String, Object> messages = new LinkedHashMap<String, Object>();
		while (enumeration.hasMoreElements()) {
			String msgKey = enumeration.nextElement();
			Object val = ms.getMessage(msgKey, null, locale);
			if (val != null) {
				messages.put(msgKey, val);
			}
		}
		model.addAttribute("messages", messages);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());

	}

	public String getViewName() {
		return viewName;
	}
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}
	
}
