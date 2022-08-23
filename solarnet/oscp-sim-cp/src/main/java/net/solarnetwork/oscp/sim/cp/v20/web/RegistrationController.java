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
import static java.util.Arrays.asList;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.authorizationTokenFromRequest;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.newResponseSentCondition;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizer;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;
import org.springframework.web.context.request.WebRequest;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;
import net.solarnetwork.oscp.sim.cp.web.SystemHttpTask;
import oscp.v20.Register;
import oscp.v20.VersionUrl;

/**
 * Registration web API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class RegistrationController {

	/** The base URL path to this controller. */
	public static final String REG_20_URL_PATH = CAPACITY_PROVIDER_V20_URL_PATH + REGISTER_URL_PATH;

	private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

	private final Executor executor;
	private final CapacityProviderDao capacityProviderDao;
	private final RestOperations restOps;

	public RegistrationController(Executor executor, CapacityProviderDao capacityProviderDao,
			RestOperations restOps) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.restOps = requireNonNullArgument(restOps, "restOps");
	}

	@PostMapping(path = "/sim/system", consumes = APPLICATION_JSON_VALUE)
	public SystemConfiguration createSystem(@RequestBody SystemConfiguration input) {
		SystemConfiguration copy = input.copyWithId(UUID.randomUUID());
		capacityProviderDao.saveSystemConfiguration(copy);
		return copy;
	}

	@PostMapping(path = "/sim/system/{systemId}/register", consumes = APPLICATION_JSON_VALUE)
	public SystemConfiguration createSystem(@PathVariable("systemId") UUID systemId,
			@RequestBody SystemConfiguration input) {
		if ( !V20.equals(input.getOscpVersion()) ) {
			throw new IllegalArgumentException("OSCP version [%s] is not supported (must be %s)."
					.formatted(input.getOscpVersion(), V20));
		}
		requireNonNullArgument(input.getBaseUrl(), "baseUrl");
		requireNonNullArgument(input.getOutToken(), "outToken");

		SystemConfiguration conf = capacityProviderDao.systemConfiguration(systemId);
		synchronized ( conf ) {
			conf.setOscpVersion(input.getOscpVersion());
			conf.setBaseUrl(input.getBaseUrl());
			conf.setOutToken(input.getOutToken());

			URI uri = URI.create(input.getBaseUrl() + OscpWebUtils.REGISTER_URL_PATH);
			log.info("Initiating system registration for {} to [{}]", systemId, uri);

			// generate new in token for system to use
			String newInToken = OscpWebUtils.generateToken();
			conf.setInToken(newInToken);
			capacityProviderDao.saveSystemConfiguration(conf);

			// build a URL to ourselves to give to the system
			URI loc = fromController(getClass()).path(CAPACITY_PROVIDER_V20_URL_PATH).build().toUri();

			Register req = new Register(newInToken, asList(new VersionUrl(V20, loc.toString())));
			executor.execute(new SystemHttpTask<>("Register", restOps, newResponseSentCondition(),
					HttpMethod.POST, uri, req, tokenAuthorizer(input.getOutToken())));
		}
		SystemConfiguration copy = conf.copyWithId(UUID.randomUUID());
		capacityProviderDao.saveSystemConfiguration(copy);
		return copy;
	}

	@PostMapping(path = REG_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> register20(@RequestBody Register input, WebRequest request) {
		log.info("Processing {} request: {}", REG_20_URL_PATH, input);

		requireNonNullArgument(input.getVersionUrl(), "version_url");
		VersionUrl url = input.getVersionUrl().stream().filter(e -> V20.equals(e.getVersion()))
				.findFirst().orElse(null);
		if ( url == null ) {
			return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
					.header(OscpWebUtils.ERROR_MESSAGE_HEADER,
							format("URL version '%s' required but not provided.", V20))
					.build();
		}

		SystemConfiguration system = capacityProviderDao
				.verifyAuthToken(authorizationTokenFromRequest(request));
		synchronized ( system ) {
			system.setOutToken(input.getToken());
			/*- Ignoring the acknowledged URL/version for now and keeping what we started with
			system.setBaseUrl(url.getBaseUrl());
			system.setOscpVersion(url.getVersion());
			*/
			capacityProviderDao.saveSystemConfiguration(system);
		}

		return ResponseEntity.noContent().build();
	}

}
