/* ==================================================================
 * RegistrationController.java - 11/08/2022 1:43:15 pm
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

import static java.lang.String.format;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.security.Principal;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import net.solarnetwork.domain.KeyValuePair;
import oscp.v20.Register;
import oscp.v20.VersionUrl;

/**
 * Registration web API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("RegistrationControllerV20")
@RequestMapping(RegistrationController.URL_PATH)
public class RegistrationController {

	/** The base URL path to this controller. */
	public static final String URL_PATH = FLEXIBILITY_PROVIDER_V20_URL_PATH + REGISTER_URL_PATH;

	private final FlexibilityProviderBiz flexibilityProviderBiz;

	/**
	 * Constructor.
	 * 
	 * @param flexibilityProviderBiz
	 *        the flexibility provider service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public RegistrationController(FlexibilityProviderBiz flexibilityProviderBiz) {
		super();
		this.flexibilityProviderBiz = requireNonNullArgument(flexibilityProviderBiz,
				"flexibilityProviderBiz");
	}

	/**
	 * Initiate registration.
	 * 
	 * @param input
	 *        the user data to save
	 * @return the updated user
	 */
	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> initiateRegistration(@Valid @RequestBody Register input,
			Principal principal) {
		VersionUrl url = input.getVersionUrl().stream().filter(e -> V20.equals(e.getVersion()))
				.findFirst().orElse(null);
		if ( url == null ) {
			return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
					.header(OscpWebUtils.ERROR_MESSAGE_HEADER,
							format("URL version '%s' required but not provided.", V20))
					.build();
		}

		KeyValuePair versionUrl = new KeyValuePair(url.getVersion(), url.getBaseUrl());

		AuthRoleInfo actor = OscpSecurityUtils.authRoleInfoForPrincipal(principal);

		flexibilityProviderBiz.register(actor, input.getToken(), versionUrl);

		return ResponseEntity.noContent().build();
	}

}
