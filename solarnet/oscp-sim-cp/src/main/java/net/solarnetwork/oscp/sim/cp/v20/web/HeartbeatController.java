/* ==================================================================
 * HeartbeatController.java - 24/08/2022 10:00:07 am
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

package net.solarnetwork.oscp.sim.cp.v20.web;

import static net.solarnetwork.central.oscp.web.OscpWebUtils.authorizationTokenFromRequest;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HEARTBEAT_URL_PATH;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;
import oscp.v20.Heartbeat;

/**
 * Heartbeat web API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class HeartbeatController {

	/** The base URL path to this controller. */
	public static final String HB_20_URL_PATH = CAPACITY_PROVIDER_V20_URL_PATH + HEARTBEAT_URL_PATH;

	private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

	private final CapacityProviderDao capacityProviderDao;

	public HeartbeatController(CapacityProviderDao capacityProviderDao) {
		super();
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
	}

	@PostMapping(path = HB_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> heartbeat20(@RequestBody Heartbeat input, WebRequest request) {
		log.info("Processing {} request: {}", HB_20_URL_PATH, input);

		requireNonNullArgument(input.getOfflineModeAt(), "input.offline_mode_at");

		SystemConfiguration system = capacityProviderDao
				.verifyAuthToken(authorizationTokenFromRequest(request));
		synchronized ( system ) {
			system.setOfflineDate(input.getOfflineModeAt());
			capacityProviderDao.saveSystemConfiguration(system);
		}

		return ResponseEntity.noContent().build();
	}

}
