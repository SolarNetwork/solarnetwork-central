/* ==================================================================
 * UserOcppController.java - 1/03/2020 7:36:49 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static net.solarnetwork.web.domain.Response.response;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for OCPP management.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("v1OcppController")
@RequestMapping(value = { "/sec/ocpp", "/v1/sec/user/ocpp" })
public class UserOcppController extends WebServiceControllerSupport {

	private final OptionalService<UserOcppBiz> userOcppBiz;

	/**
	 * Constructor.
	 * 
	 * @param userOcppBiz
	 *        the user OCPP service
	 */
	@Autowired
	public UserOcppController(@Qualifier("userOcppBiz") OptionalService<UserOcppBiz> userOcppBiz) {
		super();
		if ( userOcppBiz == null ) {
			throw new IllegalArgumentException("The userOcppBiz parameter must not be null.");
		}
		this.userOcppBiz = userOcppBiz;
	}

	/**
	 * Get the {@link UserOcppBiz}.
	 * 
	 * @return the service; never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserOcppBiz userOcppBiz() {
		UserOcppBiz biz = userOcppBiz.service();
		if ( biz == null ) {
			throw new UnsupportedOperationException("OCPP service not available.");
		}
		return biz;
	}

	/**
	 * Create a response entity out of a save operation on an entity.
	 * 
	 * @param <T>
	 *        the entity type
	 * @param id
	 *        the entity ID that was provided on the request to be saved
	 * @param out
	 *        the entity that was saved and should be returned in the response
	 * @return a response entity, with {@link HttpStatus#CREATED} if the
	 *         {@code in} has a {@literal null} primary key or
	 *         {@link HttpStatus#OK} otherwise
	 */
	private <T extends Entity<?>> ResponseEntity<Response<T>> responseForSave(Object id, T out) {
		HttpStatus status = id == null ? HttpStatus.CREATED : HttpStatus.OK;
		return new ResponseEntity<Response<T>>(response(out), status);
	}

	/**
	 * Get all available charge points for the current user.
	 * 
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers")
	public Response<Collection<CentralChargePoint>> availableChargePoints() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralChargePoint> list = userOcppBiz().chargePointsForUser(userId);
		return response(list);
	}

	/**
	 * Save a charge point.
	 * 
	 * @param chargePoint
	 *        the charge point to save
	 * @return the saved charge point
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/chargers")
	public ResponseEntity<Response<CentralChargePoint>> saveChargePoint(
			@RequestBody CentralChargePoint chargePoint) {
		return responseForSave(chargePoint.getId(), userOcppBiz().saveChargePoint(chargePoint));
	}

	/**
	 * Get all available charge points for the current user.
	 * 
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/authorizations")
	public Response<Collection<CentralAuthorization>> availableAuthorizations() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralAuthorization> list = userOcppBiz().authorizationsForUser(userId);
		return response(list);
	}

	/**
	 * Save an authorization.
	 * 
	 * @param authorization
	 *        the authorization to save
	 * @return the saved authorization
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/authorizations")
	public ResponseEntity<Response<CentralAuthorization>> saveAuthorization(
			@RequestBody CentralAuthorization authorization) {
		return responseForSave(authorization.getId(), userOcppBiz().saveAuthorization(authorization));
	}

	/**
	 * Get all available OCPP system users for the current user.
	 * 
	 * @return the system users
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials", params = "!username")
	public Response<Collection<CentralSystemUser>> availableSystemUsers() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralSystemUser> list = userOcppBiz().systemUsersForUser(userId);
		return response(list);
	}

	/**
	 * View a system user by its username.
	 * 
	 * @param username
	 *        the username of the system user to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials", params = "username")
	public Response<CentralSystemUser> viewSystemUser(@RequestParam("username") String username) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().systemUserForUser(userId, username));
	}

	/**
	 * Save a system user.
	 * 
	 * @param systemUser
	 *        the system user to save
	 * @return the saved system user
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/credentials")
	public ResponseEntity<Response<CentralSystemUser>> saveSystemUser(
			@RequestBody CentralSystemUser systemUser) {
		return responseForSave(systemUser.getId(), userOcppBiz().saveSystemUser(systemUser));
	}

	/**
	 * View a system user.
	 * 
	 * @param id
	 *        the ID of the system user to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials/{id}")
	public Response<CentralSystemUser> viewSystemUser(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().systemUserForUser(userId, id));
	}

	/**
	 * Delete a system user.
	 * 
	 * @param id
	 *        the ID of the system user to delete
	 * @return the deleted system user
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/credentials/{id}")
	public Response<Void> deleteSystemUser(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserSystemUser(userId, id);
		return response(null);
	}

}
