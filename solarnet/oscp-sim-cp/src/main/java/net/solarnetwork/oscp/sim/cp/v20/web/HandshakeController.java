/* ==================================================================
 * HandshakeController.java - 24/08/2022 10:42:41 am
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
import static net.solarnetwork.central.oscp.web.OscpWebUtils.newResponseSentCondition;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizer;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.CAPACITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_ACK_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;
import org.springframework.web.context.request.WebRequest;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;
import net.solarnetwork.oscp.sim.cp.web.SystemHttpTask;
import oscp.v20.Handshake;
import oscp.v20.HandshakeAcknowledge;

/**
 * Handshake web API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class HandshakeController {

	/** The URL path for 2.0 Handshake Acknowledge. */
	public static final String HS_ACK_20_URL_PATH = CAPACITY_PROVIDER_V20_URL_PATH
			+ HANDSHAKE_ACK_URL_PATH;

	private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

	private final Executor executor;
	private final CapacityProviderDao capacityProviderDao;
	private final RestOperations restOps;

	public HandshakeController(Executor executor, CapacityProviderDao capacityProviderDao,
			RestOperations restOps) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.restOps = requireNonNullArgument(restOps, "restOps");
	}

	@PostMapping(path = HS_ACK_20_URL_PATH, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> register20(@RequestBody HandshakeAcknowledge input, WebRequest request) {
		log.info("Processing {} request: {}", HS_ACK_20_URL_PATH, input);

		SystemSettings settings = SystemSettings.forOscp20Value(
				requireNonNullArgument(input.getRequiredBehaviour(), "required_behaviour"));

		log.info("Got handshake settings: {}", settings);

		SystemConfiguration system = capacityProviderDao
				.verifyAuthToken(authorizationTokenFromRequest(request));
		synchronized ( system ) {
			/*- could verify response settings here, but ignore for now
			system.setSettings(settings);
			capacityProviderDao.saveSystemConfiguration(system);
			*/
		}

		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/sim/system/{systemId}/handshake", consumes = APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void initiateHandshake(@PathVariable("systemId") UUID systemId,
			@RequestBody SystemSettings input) {
		requireNonNullArgument(input.measurementStyles(), "measurementStyles");
		SystemConfiguration conf = capacityProviderDao.systemConfiguration(systemId);
		synchronized ( conf ) {
			if ( !V20.equals(conf.getOscpVersion()) ) {
				throw new IllegalArgumentException("OSCP version [%s] is not supported (must be %s)."
						.formatted(conf.getOscpVersion(), V20));
			}

			conf.setSettings(input);
			capacityProviderDao.saveSystemConfiguration(conf);

			URI uri = URI.create(conf.getBaseUrl() + HANDSHAKE_URL_PATH);
			log.info("Initiating handshake for {} to [{}]", systemId, uri);

			Handshake req = new Handshake();
			req.setRequiredBehaviour(input.toOscp20Value());
			executor.execute(new SystemHttpTask<>("Handshake", restOps, newResponseSentCondition(),
					HttpMethod.POST, uri, req, tokenAuthorizer(conf.getOutToken())));
		}
	}

}
