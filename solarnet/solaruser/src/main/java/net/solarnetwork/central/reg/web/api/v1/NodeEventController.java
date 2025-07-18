/* ==================================================================
 * NodeEventController.java - 17/06/2020 6:23:18 am
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.service.IdentifiableConfiguration.maskConfiguration;
import static net.solarnetwork.service.IdentifiableConfiguration.maskConfigurations;
import static net.solarnetwork.service.LocalizedServiceInfoProvider.localizedServiceSettings;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.domain.CompositeKey2;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Web service API for node event hook management.
 *
 * @author matt
 * @version 2.1
 * @since 2.3
 */
@GlobalExceptionRestController
@RestController("v1NodeEventController")
@RequestMapping(value = { "/u/sec/event", "/api/v1/sec/user/event" })
public class NodeEventController {

	private final UserEventHookBiz eventHookBiz;
	private final ConcurrentMap<String, List<SettingSpecifier>> serviceSettings;

	/**
	 * Constructor.
	 *
	 * @param eventHookBiz
	 *        the event hook biz to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public NodeEventController(@Autowired(required = false) UserEventHookBiz eventHookBiz) {
		super();
		this.eventHookBiz = eventHookBiz;
		serviceSettings = new ConcurrentHashMap<>(8);
	}

	@ResponseBody
	@RequestMapping(value = "/node/topics", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableDatumEventHookTopics(Locale locale) {
		Iterable<LocalizedServiceInfo> result = null;
		if ( eventHookBiz != null ) {
			result = eventHookBiz.availableDatumEventTopics(locale);
		}
		return success(result);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hook/services", method = RequestMethod.GET)
	public Result<List<LocalizedServiceInfo>> availableNodeEventHookServices(Locale locale) {
		List<LocalizedServiceInfo> result = null;
		if ( eventHookBiz != null ) {
			result = localizedServiceSettings(eventHookBiz.availableNodeEventHookServices(), locale);
		}
		return success(result);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks", method = RequestMethod.GET)
	public Result<List<UserNodeEventHookConfiguration>> nodeHookConfigurations() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		List<UserNodeEventHookConfiguration> configs = null;
		if ( eventHookBiz != null ) {
			configs = maskConfigurations(
					eventHookBiz.configurationsForUser(userId, UserNodeEventHookConfiguration.class),
					serviceSettings, eventHookBiz::availableNodeEventHookServices);
		}
		return success(configs);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks", method = RequestMethod.POST)
	public Result<UserNodeEventHookConfiguration> saveNodeHookConfiguration(
			@RequestBody UserNodeEventHookConfiguration config) {
		if ( eventHookBiz != null ) {
			if ( config.getUserId() == null ) {
				config = config.withUserId(SecurityUtils.getCurrentActorUserId());
			}
			CompositeKey2<Long, Long> id = eventHookBiz.saveConfiguration(config);
			if ( id != null ) {
				config = eventHookBiz.configurationForUser(id.keyComponent1(),
						UserNodeEventHookConfiguration.class, id.keyComponent2());
				return success(maskConfiguration(config, serviceSettings,
						eventHookBiz::availableNodeEventHookServices));
			}
		}
		return error();
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks/{id}", method = RequestMethod.GET)
	public Result<UserNodeEventHookConfiguration> viewNodeHookConfiguration(
			@PathVariable("id") Long id) {
		UserNodeEventHookConfiguration result = null;
		if ( eventHookBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = eventHookBiz.configurationForUser(userId, UserNodeEventHookConfiguration.class, id);
			if ( result != null ) {
				result = maskConfiguration(result, serviceSettings,
						eventHookBiz::availableNodeEventHookServices);
			}
		}
		return success(result);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks/{id}", method = RequestMethod.DELETE)
	public Result<Void> deleteNodeHookConfiguration(@PathVariable("id") Long id) {
		if ( eventHookBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserNodeEventHookConfiguration config = eventHookBiz.configurationForUser(userId,
					UserNodeEventHookConfiguration.class, id);
			if ( config != null ) {
				eventHookBiz.deleteConfiguration(config);
			}
		}
		return success();
	}

}
