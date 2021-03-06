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

import static net.solarnetwork.domain.IdentifiableConfiguration.maskConfiguration;
import static net.solarnetwork.domain.IdentifiableConfiguration.maskConfigurations;
import static net.solarnetwork.support.LocalizedServiceInfoProvider.localizedServiceSettings;
import static net.solarnetwork.web.domain.Response.response;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for node event hook management.
 * 
 * @author matt
 * @version 1.0
 * @since 2.3
 */
@RestController("v1NodeEventController")
@RequestMapping(value = { "/sec/event", "/v1/sec/user/event" })
public class NodeEventController extends WebServiceControllerSupport {

	private final OptionalService<UserEventHookBiz> eventHookBiz;
	private final ConcurrentMap<String, List<SettingSpecifier>> serviceSettings;

	/**
	 * Constructor.
	 * 
	 * @param eventHookBiz
	 *        the event hook biz to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	@Autowired
	public NodeEventController(
			@Qualifier("eventHookBiz") OptionalService<UserEventHookBiz> eventHookBiz) {
		super();
		if ( eventHookBiz == null ) {
			throw new IllegalArgumentException("The eventHookBiz argument must not be null.");
		}
		this.eventHookBiz = eventHookBiz;
		serviceSettings = new ConcurrentHashMap<>(8);
	}

	@ResponseBody
	@RequestMapping(value = "/node/topics", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableDatumEventHookTopics(Locale locale) {
		final UserEventHookBiz biz = eventHookBiz.service();
		Iterable<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			result = biz.availableDatumEventTopics(locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hook/services", method = RequestMethod.GET)
	public Response<List<LocalizedServiceInfo>> availableNodeEventHookServices(Locale locale) {
		final UserEventHookBiz biz = eventHookBiz.service();
		List<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			result = localizedServiceSettings(biz.availableNodeEventHookServices(), locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks", method = RequestMethod.GET)
	public Response<List<UserNodeEventHookConfiguration>> nodeHookConfigurations() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final UserEventHookBiz biz = eventHookBiz.service();
		List<UserNodeEventHookConfiguration> configs = null;
		if ( biz != null ) {
			configs = maskConfigurations(
					biz.configurationsForUser(userId, UserNodeEventHookConfiguration.class),
					serviceSettings, (Void) -> {
						return biz.availableNodeEventHookServices();
					});
		}
		return response(configs);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks", method = RequestMethod.POST)
	public Response<UserNodeEventHookConfiguration> saveNodeHookConfiguration(
			@RequestBody UserNodeEventHookConfiguration config) {
		final UserEventHookBiz biz = eventHookBiz.service();
		if ( biz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			UserLongPK id = biz.saveConfiguration(config);
			if ( id != null ) {
				config = biz.configurationForUser(id.getUserId(), UserNodeEventHookConfiguration.class,
						id.getId());
				return response(maskConfiguration(config, serviceSettings, (Void) -> {
					return biz.availableNodeEventHookServices();
				}));
			}
		}
		return new Response<>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks/{id}", method = RequestMethod.GET)
	public Response<UserNodeEventHookConfiguration> viewNodeHookConfiguration(
			@PathVariable("id") Long id) {
		final UserEventHookBiz biz = eventHookBiz.service();
		UserNodeEventHookConfiguration result = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = biz.configurationForUser(userId, UserNodeEventHookConfiguration.class, id);
			if ( result != null ) {
				result = maskConfiguration(result, serviceSettings, (Void) -> {
					return biz.availableNodeEventHookServices();
				});
			}
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/node/hooks/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteNodeHookConfiguration(@PathVariable("id") Long id) {
		final UserEventHookBiz biz = eventHookBiz.service();
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserNodeEventHookConfiguration config = biz.configurationForUser(userId,
					UserNodeEventHookConfiguration.class, id);
			if ( config != null ) {
				biz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

}
