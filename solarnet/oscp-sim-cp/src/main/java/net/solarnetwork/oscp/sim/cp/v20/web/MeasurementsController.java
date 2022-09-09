/* ==================================================================
 * MeasurementsController.java - 9/09/2022 12:04:12 pm
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
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_OPTIMIZER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_ASSET_MEASUREMENTS_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_MEASUREMENTS_URL_PATH;
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
import oscp.v20.UpdateAssetMeasurement;
import oscp.v20.UpdateGroupMeasurements;

/**
 * Measurements API support.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class MeasurementsController {

	/** The URL path for 2.0 UpdateGroupMeasurements messages. */
	public static final String UGM_20_URL_PATH = CAPACITY_PROVIDER_V20_URL_PATH
			+ UPDATE_GROUP_MEASUREMENTS_URL_PATH;

	/** The URL path for 2.0 UpdateGroupMeasurements messages. */
	public static final String UAM_20_URL_PATH = CAPACITY_OPTIMIZER_V20_URL_PATH
			+ UPDATE_ASSET_MEASUREMENTS_URL_PATH;

	private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

	private final CapacityProviderDao capacityProviderDao;

	public MeasurementsController(CapacityProviderDao capacityProviderDao) {
		super();
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
	}

	@PostMapping(path = UGM_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> updateGroupMeasurements20(@RequestBody UpdateGroupMeasurements input,
			WebRequest request) {
		log.info("Processing {} request: {}", UGM_20_URL_PATH, input);

		requireNonNullArgument(input.getGroupId(), "input.group_id");

		capacityProviderDao.verifyAuthToken(authorizationTokenFromRequest(request));

		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = UAM_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> updateAssetMeasurements20(@RequestBody UpdateAssetMeasurement input,
			WebRequest request) {
		log.info("Processing {} request: {}", UAM_20_URL_PATH, input);

		requireNonNullArgument(input.getGroupId(), "input.group_id");

		capacityProviderDao.verifyAuthToken(authorizationTokenFromRequest(request));

		return ResponseEntity.noContent().build();
	}

}
