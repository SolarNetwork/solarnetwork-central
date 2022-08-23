/* ==================================================================
 * RegistrationController.java - 23/08/2022 10:21:49 am
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

import static java.lang.String.format;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;
import org.springframework.web.context.request.WebRequest;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;
import net.solarnetwork.oscp.sim.cp.web.OscpCapacityProviderWebUtils;
import oscp.v20.Register;
import oscp.v20.VersionUrl;

/**
 * Registration API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class RegistrationController {

	/** The base URL path to this controller. */
	public static final String REG_20_URL_PATH = CAPACITY_PROVIDER_V20_URL_PATH + REGISTER_URL_PATH;

	private final TaskScheduler taskScheduler;
	private final CapacityProviderDao capacityProviderDao;
	private final RestOperations restOps;

	public RegistrationController(TaskScheduler taskScheduler, CapacityProviderDao capacityProviderDao,
			RestOperations restOps) {
		super();
		this.taskScheduler = requireNonNullArgument(taskScheduler, "taskScheduler");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.restOps = requireNonNullArgument(restOps, "restOps");
	}

	@PostMapping(path = "/sim/system", consumes = APPLICATION_JSON_VALUE)
	public SystemConfiguration createSystem(@RequestBody SystemConfiguration conf) {
		SystemConfiguration copy = conf.copyWithId(UUID.randomUUID());
		capacityProviderDao.saveSystemConfiguration(copy);
		return copy;
	}

	@PostMapping(path = REG_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> register20(@RequestBody Register input, WebRequest request) {
		requireNonNullArgument(input.getVersionUrl(), "input.version_url");
		VersionUrl url = input.getVersionUrl().stream().filter(e -> V20.equals(e.getVersion()))
				.findFirst().orElse(null);
		if ( url == null ) {
			return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
					.header(OscpWebUtils.ERROR_MESSAGE_HEADER,
							format("URL version '%s' required but not provided.", V20))
					.build();
		}

		String token = OscpWebUtils.authorizationTokenFromRequest(request);

		// build a URL to ourselves to give to the FP
		URI loc = fromController(getClass()).path(CAPACITY_PROVIDER_V20_URL_PATH).build().toUri();

		SystemConfiguration system = capacityProviderDao.verifyAuthToken(token);
		SystemConfiguration copy;
		synchronized ( system ) {
			copy = system.copyWithId(system.getId());
			copy.setBaseUrl(url.getBaseUrl());
			copy.setOscpVersion(url.getVersion());
			copy.setInToken(OscpCapacityProviderWebUtils.generateToken());
			capacityProviderDao.saveSystemConfiguration(copy.clone());

			// Give the new token to the FP to complete the registration
			taskScheduler.schedule(() -> {
				String uri = copy.getBaseUrl() + OscpWebUtils.REGISTER_URL_PATH;

				Register resp = new Register(copy.getInToken(),
						Arrays.asList(new VersionUrl(OscpWebUtils.UrlPaths_20.V20, loc.toString())));
				restOps.postForEntity(uri, resp, Void.class);

			}, Instant.now().plusSeconds(5));
		}

		return ResponseEntity.noContent().build();
	}

}
