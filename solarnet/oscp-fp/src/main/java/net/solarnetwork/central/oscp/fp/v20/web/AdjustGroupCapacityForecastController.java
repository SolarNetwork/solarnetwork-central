/* ==================================================================
 * AdjustGroupCapacityForecastController.java - 31/08/2022 10:31:17 am
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

import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
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
import net.solarnetwork.central.oscp.domain.CapacityForecast;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import oscp.v20.AdjustGroupCapacityForecast;

/**
 * Adjust Group Capacity Forecast web API.
 * 
 * <p>
 * This is not part of OSCP on a Flexibility Provider, however we use this API
 * to forward this message from Capacity Optimizer systems to associated
 * Capacity Provider systems.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@RestController("AdjustCapacityForecastControllerV20")
@RequestMapping(AdjustGroupCapacityForecastController.URL_PATH)
public class AdjustGroupCapacityForecastController {

	/** The base URL path to this controller. */
	public static final String URL_PATH = FLEXIBILITY_PROVIDER_V20_URL_PATH
			+ ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH;

	private final FlexibilityProviderBiz flexibilityProviderBiz;

	/**
	 * Constructor.
	 * 
	 * @param flexibilityProviderBiz
	 *        the flexibility provider service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AdjustGroupCapacityForecastController(FlexibilityProviderBiz flexibilityProviderBiz) {
		super();
		this.flexibilityProviderBiz = requireNonNullArgument(flexibilityProviderBiz,
				"flexibilityProviderBiz");
	}

	/**
	 * Adjust group capacity forecast.
	 * 
	 * @param input
	 *        the forecast request
	 * @return the response
	 */
	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> updateGroupCapacityForecast(
			@Valid @RequestBody AdjustGroupCapacityForecast input, Principal principal) {
		CapacityForecast forecast = CapacityForecast.forOscp20Value(input);

		AuthRoleInfo actor = OscpSecurityUtils.authRoleInfoForPrincipal(principal);

		flexibilityProviderBiz.adjustGroupCapacityForecast(actor, input.getGroupId(), forecast);

		return ResponseEntity.noContent().build();
	}

}
