/* ==================================================================
 * HeartbeatController.java - 21/08/2022 12:57:52 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.v20.web;

import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HEARTBEAT_URL_PATH;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.security.Principal;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import oscp.v20.Heartbeat;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@RestController("HeartbeatControllerV20")
@RequestMapping(HeartbeatController.URL_PATH)
public class HeartbeatController {

	/** The base URL path to this controller. */
	public static final String URL_PATH = FLEXIBILITY_PROVIDER_V20_URL_PATH + HEARTBEAT_URL_PATH;

	private final FlexibilityProviderBiz flexibilityProviderBiz;

	/**
	 * Constructor.
	 * 
	 * @param flexibilityProviderBiz
	 *        the flexibility provider service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HeartbeatController(FlexibilityProviderBiz flexibilityProviderBiz) {
		super();
		this.flexibilityProviderBiz = requireNonNullArgument(flexibilityProviderBiz,
				"flexibilityProviderBiz");
	}

	/**
	 * Initiate handshake.
	 * 
	 * @param input
	 *        the handshake request
	 * @return the response
	 */
	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> heartbeat(@Valid @RequestBody Heartbeat input, Principal principal) {
		AuthRoleInfo actor = OscpSecurityUtils.authRoleInfoForPrincipal(principal);

		flexibilityProviderBiz.heartbeat(actor, input.getOfflineModeAt());

		return ResponseEntity.noContent().build();
	}

}
