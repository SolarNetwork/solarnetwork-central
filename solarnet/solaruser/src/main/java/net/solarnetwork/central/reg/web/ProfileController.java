/* ==================================================================
 * ProfileController.java - Nov 23, 2012 1:04:19 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.User;

/**
 * Controller for user profile management.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/u/sec/profile")
public class ProfileController {

	@Autowired
	private UserBiz userBiz;

	@Autowired
	private RegistrationBiz registrationBiz;

	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView viewProfile() {
		SecurityUser user = SecurityUtils.getCurrentUser();
		User u = userBiz.getUser(user.getUserId());
		return new ModelAndView("profile/view", "user", u);
	}

	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public ModelAndView editProfile() {
		SecurityUser user = SecurityUtils.getCurrentUser();
		User u = userBiz.getUser(user.getUserId());
		return new ModelAndView("profile/form", "user", u);
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public ModelAndView saveProfile(User user) {
		User u = registrationBiz.updateUser(user);
		u.setPassword(RegistrationBiz.DO_NOT_CHANGE_VALUE);
		ModelAndView mv = new ModelAndView("profile/view", "user", u);
		mv.addObject(WebConstants.MODEL_KEY_STATUS_MSG, "user.profile.saved");
		return mv;
	}

	public void setUserBiz(UserBiz userBiz) {
		this.userBiz = userBiz;
	}

	public void setRegistrationBiz(RegistrationBiz registrationBiz) {
		this.registrationBiz = registrationBiz;
	}

}
