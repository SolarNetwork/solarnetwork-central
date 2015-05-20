/* ==================================================================
 * UserAlertController.java - 19/05/2015 7:35:10 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.domain.Response;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for user alerts.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/sec/alerts")
public class UserAlertController extends ControllerSupport {

	private final UserBiz userBiz;
	private final UserAlertBiz userAlertBiz;

	@Autowired
	public UserAlertController(UserBiz userBiz, UserAlertBiz userAlertBiz) {
		super();
		this.userBiz = userBiz;
		this.userAlertBiz = userAlertBiz;
	}

	@ModelAttribute("nodeDataAlertTypes")
	public List<UserAlertType> nodeDataAlertTypes() {
		// now now, only one alert type!
		return Collections.singletonList(UserAlertType.NodeStaleData);
	}

	@ModelAttribute("alertStatuses")
	public UserAlertStatus[] alertStatuses() {
		return UserAlertStatus.values();
	}

	/**
	 * View the main Alerts screen.
	 * 
	 * @param model
	 *        The model object.
	 * @return The view name.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String view(Model model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		List<UserAlert> alerts = userAlertBiz.userAlertsForUser(user.getUserId());
		if ( alerts != null ) {
			List<UserAlert> nodeDataAlerts = new ArrayList<UserAlert>(alerts.size());
			for ( UserAlert alert : alerts ) {
				switch (alert.getType()) {
					case NodeStaleData:
						nodeDataAlerts.add(alert);
						break;
				}
			}
			model.addAttribute("nodeDataAlerts", nodeDataAlerts);
		}
		model.addAttribute("userNodes", userBiz.getUserNodes(user.getUserId()));
		return "alerts/view-alerts";
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Response<UserAlert> addAlert(UserAlert model, Errors alertErrors) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAlert alert = new UserAlert();
		alert.setId(model.getId());
		alert.setNodeId(model.getNodeId());
		alert.setUserId(user.getUserId());
		alert.setCreated(new DateTime());
		alert.setStatus(model.getStatus() == null ? UserAlertStatus.Active : model.getStatus());
		alert.setType(model.getType() == null ? UserAlertType.NodeStaleData : model.getType());

		// reset validTo date to now, so alert re-processed
		alert.setValidTo(new DateTime());

		Map<String, Object> options = new HashMap<String, Object>();
		if ( model.getOptions() != null ) {
			for ( Map.Entry<String, Object> me : model.getOptions().entrySet() ) {
				if ( "ageMinutes".equalsIgnoreCase(me.getKey()) ) {
					// convert ageMinutes to age (seconds)
					Object v = model.getOptions().get("ageMinutes");
					double minutes = 1;
					try {
						minutes = Double.parseDouble(v.toString());
					} catch ( NumberFormatException e ) {
						// ignore
						log.warn("Alert option ageMinutes is not a number, setting to 1: [{}]", v);
					}
					options.put(UserAlertOptions.AGE_THRESHOLD, Math.round(minutes * 60.0));
				} else if ( "sources".equalsIgnoreCase(me.getKey()) && me.getValue() != null ) {
					// convert sources to List of String
					Set<String> sources = StringUtils
							.commaDelimitedStringToSet(me.getValue().toString());
					if ( sources != null ) {
						List<String> sourceList = new ArrayList<String>(sources);
						options.put(UserAlertOptions.SOURCE_IDS, sourceList);
					}
				}
			}
		}
		if ( options.size() > 0 ) {
			alert.setOptions(options);
		}

		Long id = userAlertBiz.saveAlert(alert);
		alert.setId(id);
		return new Response<UserAlert>(alert);
	}

}
