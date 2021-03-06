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
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for {@link UserAuthToken} management.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("v1UserAuthTokenController")
@RequestMapping(value = "/v1/sec/user/auth-tokens")
public class UserAuthTokenController extends WebServiceControllerSupport {

	private final UserBiz userBiz;

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
	public Response<List<UserAuthToken>> listAll(Principal principal) {
		final Long actorUserId = getUserId(principal);
		List<UserAuthToken> tokens = userBiz.getAllUserAuthTokens(actorUserId);
		return Response.response(tokens);
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
	public Response<UserAuthToken> generateToken(Principal principal,
			@PathVariable("type") UserAuthTokenType type,
			@RequestBody(required = false) BasicSecurityPolicy policy) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken token = userBiz.generateUserAuthToken(actorUserId, type, policy);
		return new Response<UserAuthToken>(token);
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
	@RequestMapping(value = "/{tokenId}", method = RequestMethod.DELETE)
	public Response<Object> deleteToken(Principal principal, @PathVariable("tokenId") String tokenId) {
		final Long actorUserId = getUserId(principal);
		userBiz.deleteUserAuthToken(actorUserId, tokenId);
		return new Response<Object>();
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
	@RequestMapping(value = "/{tokenId}", method = RequestMethod.PATCH, consumes = "application/json")
	public Response<UserAuthToken> mergePolicy(Principal principal,
			@PathVariable("tokenId") String tokenId, @RequestBody BasicSecurityPolicy policy) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken token = userBiz.updateUserAuthTokenPolicy(actorUserId, tokenId, policy, false);
		return Response.response(token);
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
	@RequestMapping(value = "/{tokenId}", method = RequestMethod.PUT, consumes = "application/json")
	public Response<UserAuthToken> replacePolicy(Principal principal,
			@PathVariable("tokenId") String tokenId, @RequestBody BasicSecurityPolicy policy) {
		final Long actorUserId = getUserId(principal);
		UserAuthToken token = userBiz.updateUserAuthTokenPolicy(actorUserId, tokenId, policy, true);
		return Response.response(token);
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
	@RequestMapping(value = "/{tokenId}/status", method = RequestMethod.POST)
	public Response<Object> changeStatus(Principal principal, @PathVariable("tokenId") String tokenId,
			@RequestParam("status") UserAuthTokenStatus status) {
		final Long actorUserId = getUserId(principal);
		userBiz.updateUserAuthTokenStatus(actorUserId, tokenId, status);
		return new Response<Object>();
	}

}
