/* ==================================================================
 * HandshakeController.java - 19/08/2022 2:13:36 pm
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

import static net.solarnetwork.central.oscp.web.OscpWebUtils.REQUEST_ID_HEADER;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_ACK_URL_PATH;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import oscp.v20.HandshakeAcknowledge;

/**
 * OSCP 2.0 Handshake Acknowledge web API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("HandshakeAcknowledgeControllerV20")
@RequestMapping(HandshakeAcknowledgeController.URL_PATH)
public class HandshakeAcknowledgeController {

	/** The base URL path to this controller. */
	public static final String URL_PATH = FLEXIBILITY_PROVIDER_V20_URL_PATH + HANDSHAKE_ACK_URL_PATH;

	private final FlexibilityProviderBiz flexibilityProviderBiz;

	/**
	 * Constructor.
	 * 
	 * @param flexibilityProviderBiz
	 *        the flexibility provider service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HandshakeAcknowledgeController(FlexibilityProviderBiz flexibilityProviderBiz) {
		super();
		this.flexibilityProviderBiz = requireNonNullArgument(flexibilityProviderBiz,
				"flexibilityProviderBiz");
	}

	/**
	 * Acknowledge handshake.
	 * 
	 * @param input
	 *        the handshake acknowledgement request
	 * @param principal
	 *        the authenticated actor
	 * @param requestId
	 *        the OSCP request ID
	 * @return the response
	 */
	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> handshakeAcknowledge(@Valid @RequestBody HandshakeAcknowledge input,
			Principal principal, @RequestHeader(REQUEST_ID_HEADER) String requestId) {
		if ( input.getRequiredBehaviour() != null ) {
			SystemSettings settings = SystemSettings.forOscp20Value(input.getRequiredBehaviour());
			AuthRoleInfo actor = OscpSecurityUtils.authRoleInfoForPrincipal(principal);
			flexibilityProviderBiz.handshakeAcknowledge(actor, settings, requestId);
		}

		return ResponseEntity.noContent().build();
	}

}
