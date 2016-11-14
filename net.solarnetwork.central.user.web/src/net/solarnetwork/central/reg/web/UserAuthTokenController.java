/* ==================================================================
 * UserAuthTokenController.java - Dec 12, 2012 11:51:19 AM
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for user authorization ticket management.
 * 
 * @author matt
 * @version 1.2
 */
@Controller
@RequestMapping("/sec/auth-tokens")
public class UserAuthTokenController extends ControllerSupport {

	private final UserBiz userBiz;

	@Autowired
	public UserAuthTokenController(UserBiz userBiz) {
		super();
		this.userBiz = userBiz;
	}

	@ModelAttribute("policyAggregations")
	public Set<Aggregation> policyAggregations() {
		return EnumSet.of(Aggregation.FiveMinute, Aggregation.TenMinute, Aggregation.FifteenMinute,
				Aggregation.ThirtyMinute, Aggregation.Hour, Aggregation.Day, Aggregation.Week,
				Aggregation.Month, Aggregation.RunningTotal);
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String view(Model model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		List<UserAuthToken> tokens = userBiz.getAllUserAuthTokens(user.getUserId());
		if ( tokens != null ) {
			List<UserAuthToken> userTokens = new ArrayList<UserAuthToken>(tokens.size());
			List<UserAuthToken> dataTokens = new ArrayList<UserAuthToken>(tokens.size());
			for ( UserAuthToken token : tokens ) {
				switch (token.getType()) {
					case User:
						userTokens.add(token);
						break;

					case ReadNodeData:
						dataTokens.add(token);
						break;
				}
			}
			model.addAttribute("userAuthTokens", userTokens);
			model.addAttribute("dataAuthTokens", dataTokens);
		}
		model.addAttribute("userNodes", userBiz.getUserNodes(user.getUserId()));
		return "auth-tokens/view";
	}

	@RequestMapping(value = "/generateUser", method = RequestMethod.POST)
	@ResponseBody
	public Response<UserAuthToken> generateUserToken() {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAuthToken token = userBiz.generateUserAuthToken(user.getUserId(), UserAuthTokenType.User,
				(SecurityPolicy) null);
		return new Response<UserAuthToken>(token);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> deleteUserToken(@RequestParam("id") String tokenId) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		userBiz.deleteUserAuthToken(user.getUserId(), tokenId);
		return new Response<Object>();
	}

	@RequestMapping(value = "/changeStatus", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> changeStatus(@RequestParam("id") String tokenId,
			@RequestParam("status") UserAuthTokenStatus status) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		userBiz.updateUserAuthTokenStatus(user.getUserId(), tokenId, status);
		return new Response<Object>();
	}

	@RequestMapping(value = "/generateData", method = RequestMethod.POST)
	@ResponseBody
	public Response<UserAuthToken> generateDataToken(
			@RequestParam(value = "nodeId", required = false) Set<Long> nodeIds,
			@RequestParam(value = "sourceId", required = false) Set<String> sourceIds,
			@RequestParam(value = "minAggregation", required = false) Aggregation minAggregation,
			@RequestParam(value = "nodeMetadataPath", required = false) Set<String> nodeMetadataPaths,
			@RequestParam(value = "userMetadataPath", required = false) Set<String> userMetadataPaths) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAuthToken token = userBiz.generateUserAuthToken(user.getUserId(),
				UserAuthTokenType.ReadNodeData,
				new BasicSecurityPolicy.Builder().withNodeIds(nodeIds).withSourceIds(sourceIds)
						.withMinAggregation(minAggregation).withNodeMetadataPaths(nodeMetadataPaths)
						.withUserMetadataPaths(userMetadataPaths).build());
		return new Response<UserAuthToken>(token);
	}

}
