/* ==================================================================
 * DatumExportController.java - 29/03/2018 6:22:43 AM
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.domain.export.DataConfiguration;
import net.solarnetwork.central.datum.domain.export.DestinationConfiguration;
import net.solarnetwork.central.datum.domain.export.OutputConfiguration;
import net.solarnetwork.central.reg.web.domain.DatumExportFullConfigurations;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.domain.UserIdentifiableConfiguration;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for datum export management.
 * 
 * @author matt
 * @version 1.0
 * @since 1.26
 */
@RestController("v1DatumExportController")
@RequestMapping(value = { "/sec/export", "/v1/sec/user/export" })
public class DatumExportController extends WebServiceControllerSupport {

	private final OptionalService<UserExportBiz> exportBiz;
	private final ConcurrentMap<String, List<SettingSpecifier>> serviceSettings;

	/**
	 * Constructor.
	 * 
	 * @param billingBiz
	 *        the billing biz to use
	 */
	@Autowired
	public DatumExportController(@Qualifier("exportBiz") OptionalService<UserExportBiz> exportBiz) {
		super();
		this.exportBiz = exportBiz;
		serviceSettings = new ConcurrentHashMap<>(8);
	}

	@ResponseBody
	@RequestMapping(value = "/services/output", method = RequestMethod.GET)
	public Response<List<LocalizedServiceInfo>> availableOutputFormatServices(Locale locale) {
		final UserExportBiz biz = exportBiz.service();
		List<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			Iterable<DatumExportOutputFormatService> services = biz.availableOutputFormatServices();
			result = new ArrayList<>();
			for ( DatumExportOutputFormatService s : services ) {
				result.add(s.getLocalizedServiceInfo(locale));
			}
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/services/destination", method = RequestMethod.GET)
	public Response<List<LocalizedServiceInfo>> availableDestinationServices(Locale locale) {
		final UserExportBiz biz = exportBiz.service();
		List<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			Iterable<DatumExportDestinationService> services = biz.availableDestinationServices();
			result = new ArrayList<>();
			for ( DatumExportDestinationService s : services ) {
				result.add(s.getLocalizedServiceInfo(locale));
			}
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/services/compression", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableCompressionServices(Locale locale) {
		final UserExportBiz biz = exportBiz.service();
		Iterable<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			result = biz.availableOutputCompressionTypes(locale);
		}
		return response(result);
	}

	private List<SettingSpecifier> settingsForService(String id,
			Iterable<? extends SettingSpecifierProvider> providers) {
		if ( providers == null ) {
			return null;
		}
		for ( SettingSpecifierProvider provider : providers ) {
			if ( id.equals(provider.getSettingUID()) ) {
				return provider.getSettingSpecifiers();
			}
		}
		return null;
	}

	private <T extends UserIdentifiableConfiguration> List<T> maskConfigurations(List<T> configurations,
			Function<Void, Iterable<? extends SettingSpecifierProvider>> settingProviderFunction) {
		if ( configurations == null || configurations.isEmpty() ) {
			return Collections.emptyList();
		}
		List<T> result = new ArrayList<>(configurations.size());
		for ( T config : configurations ) {
			T maskedConfig = maskConfiguration(config, settingProviderFunction);
			if ( maskedConfig != null ) {
				result.add(maskedConfig);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T extends UserIdentifiableConfiguration> T maskConfiguration(T config,
			Function<Void, Iterable<? extends SettingSpecifierProvider>> settingProviderFunction) {
		String id = config.getServiceIdentifier();
		if ( id == null ) {
			return null;
		}
		List<SettingSpecifier> settings = serviceSettings.get(id);
		if ( settings == null ) {
			settings = settingsForService(id, settingProviderFunction.apply(null));
			if ( settings != null ) {
				serviceSettings.put(id, settings);
			}
		}
		if ( settings != null ) {
			Map<String, ?> serviceProps = config.getServiceProperties();
			Map<String, Object> maskedServiceProps = StringUtils.sha256MaskedMap(
					(Map<String, Object>) serviceProps, SettingUtils.secureKeys(settings));
			if ( maskedServiceProps != null ) {
				ClassUtils.setBeanProperties(config,
						Collections.singletonMap("serviceProps", maskedServiceProps), true);
			}
		}
		return config;
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.GET)
	public Response<DatumExportFullConfigurations> fullConfiguration() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final UserExportBiz biz = exportBiz.service();
		List<UserDatumExportConfiguration> configs = null;
		List<UserDataConfiguration> dataConfigs = Collections.emptyList();
		List<UserDestinationConfiguration> destConfigs = Collections.emptyList();
		List<UserOutputConfiguration> outputConfigs = Collections.emptyList();
		if ( biz != null ) {
			configs = biz.datumExportsForUser(userId);
			// TODO: remove cast here after define data service API
			dataConfigs = biz.configurationsForUser(userId, UserDataConfiguration.class);
			destConfigs = maskConfigurations(
					biz.configurationsForUser(userId, UserDestinationConfiguration.class), (Void) -> {
						return biz.availableDestinationServices();
					});
			outputConfigs = maskConfigurations(
					biz.configurationsForUser(userId, UserOutputConfiguration.class), (Void) -> {
						return biz.availableOutputFormatServices();
					});
		}
		return response(
				new DatumExportFullConfigurations(configs, dataConfigs, destConfigs, outputConfigs));
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data", method = RequestMethod.POST)
	public Response<DataConfiguration> saveDataConfiguration(@RequestBody UserDataConfiguration config) {
		final UserExportBiz biz = exportBiz.service();
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
		final UserExportBiz biz = exportBiz.service();
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
	@RequestMapping(value = "/configs/output", method = RequestMethod.POST)
	public Response<OutputConfiguration> saveOutputConfiguration(
			@RequestBody UserOutputConfiguration config) {
		final UserExportBiz biz = exportBiz.service();
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
				return response(maskConfiguration(config, (Void) -> {
					return biz.availableOutputFormatServices();
				}));
			}
		}
		return new Response<OutputConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/output/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteOutputConfiguration(@PathVariable("id") Long id) {
		final UserExportBiz biz = exportBiz.service();
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserOutputConfiguration config = biz.configurationForUser(userId,
					UserOutputConfiguration.class, id);
			if ( config != null ) {
				biz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/destination", method = RequestMethod.POST)
	public Response<DestinationConfiguration> saveDestinationConfiguration(
			@RequestBody UserDestinationConfiguration config) {
		final UserExportBiz biz = exportBiz.service();
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
				return response(maskConfiguration(config, (Void) -> {
					return biz.availableDestinationServices();
				}));
			}
		}
		return new Response<DestinationConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/destination/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteDestinationConfiguration(@PathVariable("id") Long id) {
		final UserExportBiz biz = exportBiz.service();
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDestinationConfiguration config = biz.configurationForUser(userId,
					UserDestinationConfiguration.class, id);
			if ( config != null ) {
				biz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

}
