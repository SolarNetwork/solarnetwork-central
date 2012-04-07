/* ==================================================================
 * UserController.java - Jun 10, 2011 5:05:21 PM
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

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.central.dras.biz.UserBiz;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.UserRole;
import net.solarnetwork.central.dras.support.UserCriteria;
import net.solarnetwork.central.dras.support.UserGroupCriteria;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for UserBiz API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/user")
public class UserController extends ControllerSupport {

	private UserBiz userBiz;
	
	/**
	 * Constructor.
	 * 
	 * @param userBiz the UserBiz to use
	 */
	@Autowired
	public UserController(UserBiz userBiz) {
		this.userBiz = userBiz;
	}
	
	/**
	 * Setup support for Joda date/times.
	 * @param binder
	 */
	@InitBinder 
	public void initBinder(WebDataBinder binder) {
		// we turn OFF autoGrowNestedPaths, so we can make use of interfaces as collection types
		// e.g. List<SortDescriptor> which does not work with autoGrowNestedPaths
		binder.setAutoGrowNestedPaths(false);
	}
	
	/**
	 * Get a list of available user roles.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/roles.*")
	public String getAllUserRoles(HttpServletRequest request, Model model) {
		Set<UserRole> roles = userBiz.getAllUserRoles();
		model.addAttribute(MODEL_KEY_RESULT, roles);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get a single user.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/user.*")
	public String getUser(HttpServletRequest request, Model model,
			@RequestParam(value = "userId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, userBiz.getUserInfo(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for users.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findUsers.*")
	public String findUsers(HttpServletRequest request, Model model,
			UserCriteria input) {
		List<Match> results = userBiz.findUsers(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get a single user group.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/userGroup.*")
	public String getUserGroup(HttpServletRequest request, Model model,
			@RequestParam(value = "userGroupId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, userBiz.getUserGroup(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for user groups.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findUserGroups.*")
	public String findUsers(HttpServletRequest request, Model model,
			UserGroupCriteria input) {
		List<Match> results = userBiz.findUserGroups(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get the set of User constraints.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param userId the user ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/userConstraints.*")
	public String getUserConstraints(HttpServletRequest request, Model model,
			@RequestParam(value = "userId") Long userId) {
		Set<Constraint> results = userBiz.getUserConstraints(userId);
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get the set of User Program constraints.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param userId the user ID
	 * @param programId the user ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/userProgramConstraints.*")
	public String getUserConstraints(HttpServletRequest request, Model model,
			@RequestParam(value = "userId") Long userId,
			@RequestParam(value = "programId") Long programId) {
		Set<Constraint> results = userBiz.getUserProgramConstraints(userId, programId);
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
