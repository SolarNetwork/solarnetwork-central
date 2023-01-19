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

import static net.solarnetwork.domain.Result.success;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Controller for user authorization ticket management.
 * 
 * @author matt
 * @version 2.3
 */
@GlobalServiceController
@RequestMapping("/u/sec/auth-tokens")
public class UserAuthTokenController extends ControllerSupport {

	private final UserBiz userBiz;

	/**
	 * Constructor.
	 * 
	 * @param userBiz
	 *        the user service to use
	 */
	@Autowired
	public UserAuthTokenController(UserBiz userBiz) {
		super();
		this.userBiz = userBiz;
	}

	/**
	 * Get the available policy aggregations.
	 * 
	 * @return the available policy aggregation types
	 */
	@ModelAttribute("policyAggregations")
	public Set<Aggregation> policyAggregations() {
		return EnumSet.of(Aggregation.FiveMinute, Aggregation.TenMinute, Aggregation.FifteenMinute,
				Aggregation.ThirtyMinute, Aggregation.Hour, Aggregation.Day, Aggregation.Week,
				Aggregation.Month, Aggregation.RunningTotal);
	}

	/**
	 * Generate the model for the main view.
	 * 
	 * @param model
	 *        the model to populate
	 * @return the view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String view(Model model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		List<UserAuthToken> tokens = userBiz.getAllUserAuthTokens(user.getUserId());
		if ( tokens != null ) {
			List<UserAuthToken> userTokens = new ArrayList<>(tokens.size());
			List<UserAuthToken> dataTokens = new ArrayList<>(tokens.size());
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
		return "sec/authtokens/view";
	}

	/**
	 * Generate a user token.
	 * 
	 * @param name
	 *        an optional name
	 * @param description
	 *        an optional description
	 * @param apiPaths
	 *        optional API paths
	 * @return the generated token
	 */
	@RequestMapping(value = "/generateUser", method = RequestMethod.POST)
	@ResponseBody
	public Result<UserAuthToken> generateUserToken(
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "apiPath", required = false) Set<String> apiPaths) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAuthToken token = userBiz.generateUserAuthToken(user.getUserId(), SecurityTokenType.User,
				new BasicSecurityPolicy.Builder().withApiPaths(apiPaths).build());
		token = updateTokenInfo(user, token, name, description);
		return success(token);
	}

	private UserAuthToken updateTokenInfo(final SecurityUser user, final UserAuthToken token,
			final String name, final String description) {
		if ( (name != null && !name.isBlank()) || (description != null && !description.isBlank()) ) {
			token.setName(name != null && !name.isBlank() ? name : null);
			token.setDescription(description != null && !description.isBlank() ? description : null);
			userBiz.updateUserAuthTokenInfo(user.getUserId(), token.getId(), token);
		}
		return token;
	}

	/**
	 * Delete a token.
	 * 
	 * @param tokenId
	 *        the ID of the token to delete
	 * @return the result status
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public Result<Object> deleteUserToken(@RequestParam("id") String tokenId) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		userBiz.deleteUserAuthToken(user.getUserId(), tokenId);
		return success();
	}

	/**
	 * Change the status of a token.
	 * 
	 * @param tokenId
	 *        the ID of the token to change
	 * @param status
	 *        the status to change to
	 * @return the result status
	 */
	@RequestMapping(value = "/changeStatus", method = RequestMethod.POST)
	@ResponseBody
	public Result<Object> changeStatus(@RequestParam("id") String tokenId,
			@RequestParam("status") SecurityTokenStatus status) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		userBiz.updateUserAuthTokenStatus(user.getUserId(), tokenId, status);
		return success();
	}

	/**
	 * Generate a data token.
	 * 
	 * @param name
	 *        an optional name
	 * @param description
	 *        an optional description
	 * @param nodeIds
	 *        optional node IDs
	 * @param sourceIds
	 *        optional source IDs
	 * @param minAggregation
	 *        optional minimum aggregation
	 * @param nodeMetadataPaths
	 *        optional node metadata paths
	 * @param userMetadataPaths
	 *        optional user metadata paths
	 * @param apiPaths
	 *        optional API paths
	 * @param notAfter
	 *        optional expiration date
	 * @param refreshAllowed
	 *        {@literal true} if refresh is allowed
	 * @return
	 */
	@RequestMapping(value = "/generateData", method = RequestMethod.POST)
	@ResponseBody
	public Result<UserAuthToken> generateDataToken(
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "nodeId", required = false) Set<Long> nodeIds,
			@RequestParam(value = "sourceId", required = false) Set<String> sourceIds,
			@RequestParam(value = "minAggregation", required = false) Aggregation minAggregation,
			@RequestParam(value = "nodeMetadataPath", required = false) Set<String> nodeMetadataPaths,
			@RequestParam(value = "userMetadataPath", required = false) Set<String> userMetadataPaths,
			@RequestParam(value = "apiPath", required = false) Set<String> apiPaths,
			@RequestParam(value = "notAfter", required = false) LocalDate notAfter,
			@RequestParam(value = "refreshAllowed", required = false) Boolean refreshAllowed) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		final Instant notAfterDate = (notAfter != null
				? notAfter.atStartOfDay(ZoneOffset.UTC).toInstant()
				: null);
		UserAuthToken token = userBiz.generateUserAuthToken(user.getUserId(),
				SecurityTokenType.ReadNodeData,
				new BasicSecurityPolicy.Builder().withNodeIds(nodeIds).withSourceIds(sourceIds)
						.withMinAggregation(minAggregation).withNodeMetadataPaths(nodeMetadataPaths)
						.withUserMetadataPaths(userMetadataPaths).withApiPaths(apiPaths)
						.withNotAfter(notAfterDate).withRefreshAllowed(refreshAllowed).build());
		token = updateTokenInfo(user, token, name, description);
		return success(token);
	}

	/**
	 * Update token info.
	 * 
	 * @param tokenId
	 *        the ID of the token to update
	 * @param name
	 *        the name to set
	 * @param description
	 *        the description to set
	 * @return the result
	 * @since 2.3
	 */
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public Result<Object> update(@RequestParam("id") String tokenId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "description", required = false) String description) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAuthToken info = new UserAuthToken();
		info.setName(name != null && !name.isBlank() ? name : null);
		info.setDescription(description != null && !description.isBlank() ? description : null);
		userBiz.updateUserAuthTokenInfo(user.getUserId(), tokenId, info);
		return success();
	}

}
