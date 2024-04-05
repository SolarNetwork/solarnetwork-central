/* ==================================================================
 * UserMetadataController.java - 11/11/2016 8:09:50 PM
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
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Controller for user metadata.
 *
 * @author matt
 * @version 2.1
 * @since 1.18
 */
@GlobalExceptionRestController
@Controller("v1UserMetadataController")
@RequestMapping(value = { "/api/v1/sec/user/meta", "/api/v1/sec/users/meta" })
public class UserMetadataController {

	private final UserMetadataBiz userMetadataBiz;

	/**
	 * Constructor.
	 *
	 * @param userMetadataBiz
	 *        the UserMetadataBiz to use
	 */
	@Autowired
	public UserMetadataController(UserMetadataBiz userMetadataBiz) {
		super();
		this.userMetadataBiz = userMetadataBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Find all metadata for a list of user IDs.
	 *
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET, params = "userIds")
	public Result<FilterResults<UserMetadataEntity, Long>> findMetadata(UserFilterCommand criteria) {
		if ( criteria.getUserId() == null ) {
			// default to current actor
			criteria.setUserId(SecurityUtils.getCurrentActorUserId());
		}
		UserMetadataFilter f = criteria.toUserMetadataFilter();
		FilterResults<UserMetadataEntity, Long> results = userMetadataBiz.findUserMetadata(f,
				f.getSorts(), f.getOffset(), f.getMax());
		return success(results);
	}

	/**
	 * Get metadata for the active user.
	 *
	 * @return the result
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET, params = { "!userIds", "!userId" })
	public Result<UserMetadataEntity> getMetadata() {
		return getMetadata(SecurityUtils.getCurrentActorUserId());
	}

	/**
	 * Get metadata for a specific user ID.
	 *
	 * @param userId
	 *        any user ID
	 * @return the result
	 */
	@ResponseBody
	@RequestMapping(value = { "/{userId}" }, method = RequestMethod.GET)
	public Result<UserMetadataEntity> getMetadata(@PathVariable("userId") Long userId) {
		BasicUserMetadataFilter criteria = new BasicUserMetadataFilter();
		criteria.setUserId(userId);
		FilterResults<UserMetadataEntity, Long> results = userMetadataBiz.findUserMetadata(criteria,
				null, null, null);
		UserMetadataEntity result = null;
		if ( results != null ) {
			try {
				result = results.iterator().next();
			} catch ( NoSuchElementException e ) {
				// ignore
			}
		}
		return success(result);
	}

	/**
	 * Add metadata to the current user. The metadata is merged only, and will
	 * not replace existing values.
	 *
	 * @param meta
	 *        the metadata to merge
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.POST)
	public Result<Object> addMetadata(@RequestBody GeneralDatumMetadata meta) {
		return addMetadata(SecurityUtils.getCurrentActorUserId(), meta);
	}

	/**
	 * Add metadata to a user. The metadata is merged only, and will not replace
	 * existing values.
	 *
	 * @param userId
	 *        the user ID
	 * @param meta
	 *        the metadata to merge
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{userId}" }, method = RequestMethod.POST)
	public Result<Object> addMetadata(@PathVariable("userId") Long userId,
			@RequestBody GeneralDatumMetadata meta) {
		userMetadataBiz.addUserMetadata(userId, meta);
		return success();
	}

	/**
	 * Completely replace the metadata for the current user ID, or create it if
	 * it doesn't already exist.
	 *
	 * @param meta
	 *        the metadata to store
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.PUT)
	public Result<Object> replaceMetadata(@RequestBody GeneralDatumMetadata meta) {
		return replaceMetadata(SecurityUtils.getCurrentActorUserId(), meta);
	}

	/**
	 * Completely replace the metadata for a given user ID, or create it if it
	 * doesn't already exist.
	 *
	 * @param userId
	 *        the user ID
	 * @param meta
	 *        the metadata to store
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{userId}" }, method = RequestMethod.PUT)
	public Result<Object> replaceMetadata(@PathVariable("userId") Long userId,
			@RequestBody GeneralDatumMetadata meta) {
		userMetadataBiz.storeUserMetadata(userId, meta);
		return success();
	}

	/**
	 * Completely remove the metadata for the current user ID.
	 *
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.DELETE)
	public Result<Object> deleteMetadata() {
		return deleteMetadata(SecurityUtils.getCurrentActorUserId());
	}

	/**
	 * Completely remove the metadata for a given user ID.
	 *
	 * @param userId
	 *        the user ID
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{userId}" }, method = RequestMethod.DELETE)
	public Result<Object> deleteMetadata(@PathVariable("userId") Long userId) {
		userMetadataBiz.removeUserMetadata(userId);
		return success();
	}

}
