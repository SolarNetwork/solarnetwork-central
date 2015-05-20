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
import java.util.List;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

}
