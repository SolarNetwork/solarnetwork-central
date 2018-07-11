/* ==================================================================
 * DatumExpireController.java - 10/07/2018 12:00:38 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.web.domain.Response.response;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.reg.web.domain.DatumExpireFullConfigurations;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.expire.domain.DataConfiguration;
import net.solarnetwork.central.user.expire.domain.DatumRecordCounts;
import net.solarnetwork.central.user.expire.domain.UserDataConfiguration;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for datum expire management.
 * 
 * @author matt
 * @version 1.0
 * @since 1.29
 */
@RestController("v1DatumExpireController")
@RequestMapping(value = { "/sec/expire", "/v1/sec/user/expire" })
public class DatumExpireController {

	private final OptionalService<UserExpireBiz> expireBiz;

	/**
	 * Constructor.
	 * 
	 * @param expireBiz
	 *        the expire biz to use
	 */
	@Autowired
	public DatumExpireController(@Qualifier("expireBiz") OptionalService<UserExpireBiz> expireBiz) {
		super();
		this.expireBiz = expireBiz;
	}

	@ResponseBody
	@RequestMapping(value = "/services/aggregation", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableAggregationServices(Locale locale) {
		final UserExpireBiz biz = expireBiz.service();
		Iterable<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			result = biz.availableAggregationTypes(locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.GET)
	public Response<DatumExpireFullConfigurations> viewDataConfigurations() {
		final UserExpireBiz biz = expireBiz.service();
		final Long userId = SecurityUtils.getCurrentActorUserId();

		List<UserDataConfiguration> dataConfigs = Collections.emptyList();
		if ( biz != null ) {
			dataConfigs = biz.configurationsForUser(userId, UserDataConfiguration.class);
		}

		return response(new DatumExpireFullConfigurations(dataConfigs));
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data", method = RequestMethod.POST)
	public Response<DataConfiguration> saveDataConfiguration(@RequestBody UserDataConfiguration config) {
		final UserExpireBiz biz = expireBiz.service();
		if ( biz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(new DateTime());
			}
			Long id = biz.saveConfiguration(config);
			if ( id != null ) {
				config.setId(id);
				return response(config);
			}
		}
		return new Response<DataConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteDataConfiguration(@PathVariable("id") Long id) {
		final UserExpireBiz biz = expireBiz.service();
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDataConfiguration config = biz.configurationForUser(userId, UserDataConfiguration.class,
					id);
			if ( config != null ) {
				biz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data/{id}/preview", method = RequestMethod.GET)
	public Response<DatumRecordCounts> previewDataConfiguration(@PathVariable("id") Long id) {
		final UserExpireBiz biz = expireBiz.service();
		DatumRecordCounts counts = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDataConfiguration config = biz.configurationForUser(userId, UserDataConfiguration.class,
					id);
			if ( config != null ) {
				counts = biz.countExpiredDataForConfiguration(config);
			}
		}
		return response(counts);
	}
}
