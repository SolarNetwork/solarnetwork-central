/* ==================================================================
 * UserAdminController.java - Jun 11, 2011 12:25:49 PM
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import net.solarnetwork.central.dras.biz.UserAdminBiz;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.central.dras.web.support.ConstraintCommand;
import net.solarnetwork.central.dras.web.support.UserCommand;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for UserAdmin API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/user/admin")
public class UserAdminController extends ControllerSupport {
	
	private UserAdminBiz userAdminBiz;

	/**
	 * Constructor.
	 * 
	 * @param userAdminBiz the UserAdminBiz to use
	 */
	@Autowired
	public UserAdminController(UserAdminBiz userAdminBiz) {
		this.userAdminBiz = userAdminBiz;
	}

	/**
	 * Store a new User.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = {"/addUser.*", "/saveUser.*"})
	public String createUser(HttpServletRequest request, 
			Model model, @Valid UserCommand input, Errors errors) {
		try {
			User newUser = userAdminBiz.storeUser(input.getUser(), 
					input.getRoles(), input.getPrograms());
			model.addAttribute(MODEL_KEY_RESULT, newUser);
		} catch ( DuplicateKeyException e ) {
			log.debug("Duplicate key violation adding new user [{}]", 
					input.getUser().getUsername());
			errors.rejectValue("user.username", "username.taken");
		}
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	
	/**
	 * Assign users to a UserGroup.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignUserGroupMembers.*")
	public String assignUserGroupMembers(HttpServletRequest request, 
			Model model, MembershipCommand input) {
		EffectiveCollection<UserGroup, ? extends Member> result
			= userAdminBiz.assignUserGroupMembers(input);
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Assign constraints to a User.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignUserConstraints.*")
	public String saveUserConstraints(HttpServletRequest request, Model model,
			ConstraintCommand input) {
		userAdminBiz.storeUserConstraints(input.getUserId(), input.getC());
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Assign constraints to a User for a specific Program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignUserProgramConstraints.*")
	public String saveUserProgramConstraints(HttpServletRequest request, Model model,
			ConstraintCommand input) {
		userAdminBiz.storeUserProgramConstraints(
				input.getUserId(), input.getProgramId(), input.getC());
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
