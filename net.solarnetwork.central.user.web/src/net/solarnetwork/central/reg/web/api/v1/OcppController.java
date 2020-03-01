/* ==================================================================
 * OcppController.java - 1/03/2020 7:36:49 am
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

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
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
public class OcppController extends WebServiceControllerSupport {

	private final OptionalService<UserOcppBiz> userOcppBiz;

	/**
	 * Constructor.
	 * 
	 * @param userOcppBiz
	 *        the user OCPP service
	 */
	@Autowired
	public OcppController(@Qualifier("userOcppBiz") OptionalService<UserOcppBiz> userOcppBiz) {
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
	 * Get all available charge points for the current user.
	 * 
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers")
	public Response<Collection<CentralChargePoint>> availableChargePoints() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralChargePoint> list = userOcppBiz().chargePointsForUser(userId);
		return Response.response(list);
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
		return Response.response(list);
	}

	/**
	 * Get all available OCPP system users for the current user.
	 * 
	 * @return the system users
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials")
	public Response<Collection<CentralSystemUser>> availableSystemUsers() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralSystemUser> list = userOcppBiz().systemUsersForUser(userId);
		return Response.response(list);
	}

}
