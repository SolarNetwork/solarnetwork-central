/* ==================================================================
 * UserAuthTokenController.java - 10/10/2016 6:16:41 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.domain.Result.success;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;

/**
 * Web service API for {@link UserAuthToken} management.
 * 
 * @author matt
 * @version 2.1
 */
@GlobalExceptionRestController
@RestController("v1UserAuthTokenController")
@RequestMapping(value = "/api/v1/sec/user/auth-tokens")
public class UserAuthTokenController {

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

	private Long getUserId(Principal principal) {
		Object actor = null;
		if ( principal instanceof Authentication ) {
			actor = ((Authentication) principal).getPrincipal();
		}
		if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			return token.getUserId();
		} else if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			return user.getUserId();
		}
		throw new SecurityException("User ID not available.");
	}

	/**
	 * Get a list of all available auth tokens for the active user.
	 * 
	 * @param principal
	 *        The active user.
	 * @return the tokens
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Result<List<UserAuthToken>> listAll(Principal principal) {
		final Long actorUserId = getUserId(principal);
		List<UserAuthToken> tokens = userBiz.getAllUserAuthTokens(actorUserId);
		return success(tokens);
	}

	/**
	 * Create a new auth token.
	 * 
	 * @param principal
	 *        The active user.
	 * @param type
	 *        The type of token to generate.
	 * @param policy
	 *        An optional policy to attach to the token.
	 * @return The generated token.
	 */
	@RequestMapping(value = "/generate/{type}", method = RequestMethod.POST)
	public Result<UserAuthToken> generateToken(Principal principal,
			@PathVariable("type") SecurityTokenType type,
			@RequestBody(required = false) BasicSecurityPolicy policy) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken token = userBiz.generateUserAuthToken(actorUserId, type, policy);
		return success(token);
	}

	/**
	 * Update token info.
	 * 
	 * @param principal
	 *        the actor
	 * @param tokenId
	 *        the ID of the token to update
	 * @param name
	 *        the name to set
	 * @param description
	 *        the description to set
	 * @return the result
	 * @since 2.1
	 */
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	public Result<Object> update(Principal principal, @RequestParam("tokenId") String tokenId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "description", required = false) String description) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken info = new UserAuthToken();
		info.setName(name != null && !name.isBlank() ? name : null);
		info.setDescription(description != null && !description.isBlank() ? description : null);
		userBiz.updateUserAuthTokenInfo(actorUserId, tokenId, info);
		return success();
	}

	/**
	 * Delete an existing auth token.
	 * 
	 * @param principal
	 *        The active user.
	 * @param tokenId
	 *        The ID of the token to delete.
	 * @return Success response indicator.
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.DELETE)
	public Result<Object> deleteToken(Principal principal, @RequestParam("tokenId") String tokenId) {
		final Long actorUserId = getUserId(principal);
		userBiz.deleteUserAuthToken(actorUserId, tokenId);
		return success();
	}

	/**
	 * Merge policy updates into an auth token.
	 * 
	 * @param principal
	 *        The active user.
	 * @param tokenId
	 *        The ID of the token to update.
	 * @param policy
	 *        The policy to merge into the token's existing policy.
	 * @return The updated policy.
	 */
	@RequestMapping(value = "/policy", method = RequestMethod.PATCH, consumes = "application/json")
	public Result<UserAuthToken> mergePolicy(Principal principal,
			@RequestParam("tokenId") String tokenId, @RequestBody BasicSecurityPolicy policy) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken token = userBiz.updateUserAuthTokenPolicy(actorUserId, tokenId, policy, false);
		return success(token);
	}

	/**
	 * Replace policy updates in an auth token.
	 * 
	 * @param principal
	 *        The active user.
	 * @param tokenId
	 *        The ID of the token to update.
	 * @param policy
	 *        The policy to set in the token.
	 * @return The updated policy.
	 */
	@RequestMapping(value = "/policy", method = RequestMethod.PUT, consumes = "application/json")
	public Result<UserAuthToken> replacePolicy(Principal principal,
			@RequestParam("tokenId") String tokenId, @RequestBody BasicSecurityPolicy policy) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken token = userBiz.updateUserAuthTokenPolicy(actorUserId, tokenId, policy, true);
		return success(token);
	}

	/**
	 * Modify an auth token's status.
	 * 
	 * @param principal
	 *        The active user.
	 * @param tokenId
	 *        The ID of the token to delete.
	 * @param status
	 *        The status to set.
	 * @return Success response indicator.
	 */
	@RequestMapping(value = "/status", method = RequestMethod.POST)
	public Result<Object> changeStatus(Principal principal, @RequestParam("tokenId") String tokenId,
			@RequestParam("status") SecurityTokenStatus status) {
		final Long actorUserId = getUserId(principal);
		userBiz.updateUserAuthTokenStatus(actorUserId, tokenId, status);
		return success();
	}

}
