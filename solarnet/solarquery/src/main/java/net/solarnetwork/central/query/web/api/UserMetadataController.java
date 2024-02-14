/* ==================================================================
 * UserMetadataController.java - 14/11/2016 11:57:46 AM
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

package net.solarnetwork.central.query.web.api;

import static net.solarnetwork.web.jakarta.domain.Response.response;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.domain.UserMetadataFilterMatch;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller for read-only user metadata access.
 * 
 * @author matt
 * @version 2.0
 */
@Controller("v1UserMetadataController")
@RequestMapping({ "/api/v1/pub/users/meta", "/api/v1/sec/users/meta" })
@GlobalExceptionRestController
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
	 * Get metadata for a specific user ID.
	 * 
	 * @param userId
	 *        the user ID; if not provided the user ID of the current actor will
	 *        be used
	 * @return the result
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/", "/{userId}" }, method = RequestMethod.GET)
	public Response<UserMetadataFilterMatch> getMetadata(
			@PathVariable(name = "userId", required = false) Long userId) {
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setUserId(userId);
		FilterResults<UserMetadataFilterMatch> results = userMetadataBiz.findUserMetadata(criteria, null,
				null, null);
		UserMetadataFilterMatch result = null;
		if ( results != null ) {
			try {
				result = results.iterator().next();
			} catch ( NoSuchElementException e ) {
				// ignore
			}
		}
		return response(result);
	}

}
