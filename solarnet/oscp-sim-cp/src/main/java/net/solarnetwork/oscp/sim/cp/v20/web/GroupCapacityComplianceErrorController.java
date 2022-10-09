/* ==================================================================
 * GroupCapacityComplianceErrorController.java - 9/10/2022 4:54:19 pm
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

import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import oscp.v20.GroupCapacityComplianceError;

/**
 * Web API for group capacity compliance errors.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class GroupCapacityComplianceErrorController {

	/**
	 * The URL path for 2.0 GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH messages.
	 */
	public static final String GCCE_20_URL_PATH = CAPACITY_PROVIDER_V20_URL_PATH
			+ GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH;

	private static final Logger log = LoggerFactory
			.getLogger(GroupCapacityComplianceErrorController.class);

	@PostMapping(path = GCCE_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> register20(@RequestBody GroupCapacityComplianceError input,
			WebRequest request) {
		log.info("Processing {} request: {}", GCCE_20_URL_PATH, input);
		return ResponseEntity.noContent().build();
	}

}
