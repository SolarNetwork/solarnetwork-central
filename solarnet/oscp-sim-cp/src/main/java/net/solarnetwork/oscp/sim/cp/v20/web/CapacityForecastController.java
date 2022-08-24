/* ==================================================================
 * CapacityForecastController.java - 24/08/2022 11:25:01 am
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

import static net.solarnetwork.central.oscp.web.OscpWebUtils.newResponseSentCondition;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizer;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.oscp.domain.CapacityForecast;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;
import net.solarnetwork.oscp.sim.cp.web.SystemHttpTask;
import oscp.v20.UpdateGroupCapacityForecast;

/**
 * Web API for capacity forecasts.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class CapacityForecastController {

	private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

	private final Executor executor;
	private final CapacityProviderDao capacityProviderDao;
	private final RestOperations restOps;

	public CapacityForecastController(Executor executor, CapacityProviderDao capacityProviderDao,
			RestOperations restOps) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.restOps = requireNonNullArgument(restOps, "restOps");
	}

	@PostMapping(path = "/sim/system/{systemId}/group/{groupId}/capacity-forecast", consumes = APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateGroupCapcityForecast(@PathVariable("systemId") UUID systemId,
			@PathVariable("groupId") String groupId, @RequestBody CapacityForecast input) {
		requireNonNullArgument(input.type(), "type");
		requireNonEmptyArgument(input.blocks(), "blocks");
		SystemConfiguration conf = capacityProviderDao.systemConfiguration(systemId);
		UpdateGroupCapacityForecast req = input.toOscp20GroupCapacityValue(groupId);
		synchronized ( conf ) {
			if ( !V20.equals(conf.getOscpVersion()) ) {
				throw new IllegalArgumentException("OSCP version [%s] is not supported (must be %s)."
						.formatted(conf.getOscpVersion(), V20));
			}
			URI uri = URI.create(conf.getBaseUrl() + UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH);
			log.info("Initiating group {} capacity update for {} to [{}]", groupId, systemId, uri);

			executor.execute(
					new SystemHttpTask<>("GroupCapacityUpdate", restOps, newResponseSentCondition(),
							HttpMethod.POST, uri, req, tokenAuthorizer(conf.getOutToken())));
		}
	}

}
