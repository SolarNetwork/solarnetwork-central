/* ==================================================================
 * UserDnp3Controller.java - 17/08/2023 6:56:35 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import net.solarnetwork.central.security.SecurityUtils;

/**
 * Controller for DNP3 UI.
 * 
 * @author matt
 * @version 1.0
 */
@GlobalServiceController
public class UserDnp3Controller {

	/** The model attribute for the actor's user ID. */
	public static final String ACTOR_USER_ID_ATTR = "actorUserId";

	@RequestMapping(value = "/u/sec/dnp3", method = RequestMethod.GET)
	public String home() {
		return "sec/dnp3/dnp3";
	}

	@ModelAttribute(name = ACTOR_USER_ID_ATTR)
	public Long actorUserId() {
		return SecurityUtils.getCurrentActorUserId();
	}

}
