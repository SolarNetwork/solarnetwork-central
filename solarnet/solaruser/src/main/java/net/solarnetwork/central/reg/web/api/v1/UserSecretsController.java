/* ==================================================================
 * UserSecretsController.java - 23/03/2025 11:26:47 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.domain.UserKeyPair;
import net.solarnetwork.central.user.domain.UserKeyPairInput;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Web service API for user secret management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetUserConfiguration.USER_SECRETS)
@GlobalExceptionRestController
@RestController("v1UserSecretsController")
@RequestMapping(value = { "/api/v1/sec/user/secrets", "/u/sec/secrets" })
public class UserSecretsController {

	private final UserSecretBiz biz;

	/**
	 * Constructor.
	 *
	 * @param userSecretBiz
	 *        the user secret service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserSecretsController(UserSecretBiz userSecretBiz) {
		super();
		this.biz = requireNonNullArgument(userSecretBiz, "userSecretBiz");
	}

	/**
	 * List the available key pairs.
	 *
	 * @param filter
	 *        the search criteria
	 * @return the matching key pairs
	 */
	@RequestMapping(value = "/key-pair", method = RequestMethod.GET)
	public Result<FilterResults<? extends UserKeyPair, UserStringCompositePK>> listUserKeyPairs(
			BasicUserSecretFilter filter) {
		var result = biz.listKeyPairsForUser(getCurrentActorUserId(), filter);
		return success(result);
	}

	/**
	 * Save a key pair.
	 *
	 * @param input
	 *        the input information
	 * @return the matching key pairs
	 */
	@RequestMapping(value = "/key-pair", method = RequestMethod.POST)
	public Result<UserKeyPair> saveUserKeyPair(@Valid @RequestBody UserKeyPairInput input) {
		var result = biz.saveUserKeyPair(getCurrentActorUserId(), input);
		return success(result);
	}

	/**
	 * Delete a key pair.
	 *
	 * @param key
	 *        the key of the key pair to delete
	 * @return success result
	 */
	@RequestMapping(value = "/key-pair", method = RequestMethod.DELETE)
	public Result<Void> deleteUserKeyPair(@RequestParam("key") String key) {
		biz.deleteUserKeyPair(getCurrentActorUserId(), key);
		return success();
	}

	/**
	 * List the available secrets.
	 *
	 * @param filter
	 *        the search criteria
	 * @return the matching key pairs
	 */
	@RequestMapping(value = "/secret", method = RequestMethod.GET)
	public Result<FilterResults<? extends UserSecret, UserStringStringCompositePK>> listUserSecrets(
			BasicUserSecretFilter filter) {
		var result = biz.listSecretsForUser(getCurrentActorUserId(), filter);
		return success(result);
	}

	/**
	 * Save a secret.
	 *
	 * @param input
	 *        the input information
	 * @return the matching key pairs
	 */
	@RequestMapping(value = "/secret", method = RequestMethod.POST)
	public Result<UserSecret> saveUserSecret(@Valid @RequestBody UserSecretInput input) {
		var result = biz.saveUserSecret(getCurrentActorUserId(), input);
		return success(result);
	}

	/**
	 * Delete a key pair.
	 *
	 * @param topicId
	 *        the topic ID of the key pair to delete
	 * @param key
	 *        the key of the key pair to delete
	 * @return success result
	 */
	@RequestMapping(value = "/secret", method = RequestMethod.DELETE)
	public Result<Void> deleteUserSecret(@RequestParam("topicId") String topicId,
			@RequestParam("key") String key) {
		biz.deleteUserSecret(getCurrentActorUserId(), topicId, key);
		return success();
	}

}
