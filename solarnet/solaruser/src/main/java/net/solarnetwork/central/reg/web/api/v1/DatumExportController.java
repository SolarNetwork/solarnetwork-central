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

import static net.solarnetwork.service.IdentifiableConfiguration.maskConfiguration;
import static net.solarnetwork.service.IdentifiableConfiguration.maskConfigurations;
import static net.solarnetwork.service.LocalizedServiceInfoProvider.localizedServiceSettings;
import static net.solarnetwork.web.domain.Response.response;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DataConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.reg.web.domain.DatumExportFullConfigurations;
import net.solarnetwork.central.reg.web.domain.DatumExportProperties;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for datum export management.
 * 
 * @author matt
 * @version 2.0
 * @since 1.26
 */
@GlobalExceptionRestController
@RestController("v1DatumExportController")
@RequestMapping(value = { "/u/sec/export", "/api/v1/sec/user/export" })
public class DatumExportController {

	private final UserExportBiz exportBiz;
	private final ConcurrentMap<String, List<SettingSpecifier>> serviceSettings;

	/**
	 * Constructor.
	 * 
	 * @param exportBiz
	 *        the export exportBiz to use
	 */

	public DatumExportController(@Autowired(required = false) UserExportBiz exportBiz) {
		super();
		this.exportBiz = exportBiz;
		serviceSettings = new ConcurrentHashMap<>(8);
	}

	@ResponseBody
	@RequestMapping(value = "/services/output", method = RequestMethod.GET)
	public Response<List<LocalizedServiceInfo>> availableOutputFormatServices(Locale locale) {
		List<LocalizedServiceInfo> result = null;
		if ( exportBiz != null ) {
			result = localizedServiceSettings(exportBiz.availableOutputFormatServices(), locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/services/destination", method = RequestMethod.GET)
	public Response<List<LocalizedServiceInfo>> availableDestinationServices(Locale locale) {
		List<LocalizedServiceInfo> result = null;
		if ( exportBiz != null ) {
			result = localizedServiceSettings(exportBiz.availableDestinationServices(), locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/services/compression", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableCompressionServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = null;
		if ( exportBiz != null ) {
			result = exportBiz.availableOutputCompressionTypes(locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/services/schedule", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableScheduleServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = null;
		if ( exportBiz != null ) {
			result = exportBiz.availableScheduleTypes(locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/services/aggregation", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableAggregationServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = null;
		if ( exportBiz != null ) {
			result = exportBiz.availableAggregationTypes(locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.GET)
	public Response<DatumExportFullConfigurations> fullConfiguration() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		List<UserDatumExportConfiguration> configs = null;
		List<UserDataConfiguration> dataConfigs = Collections.emptyList();
		List<UserDestinationConfiguration> destConfigs = Collections.emptyList();
		List<UserOutputConfiguration> outputConfigs = Collections.emptyList();
		if ( exportBiz != null ) {
			configs = exportBiz.datumExportsForUser(userId).stream()
					.map(c -> new DatumExportProperties(c)).collect(Collectors.toList());
			dataConfigs = exportBiz.configurationsForUser(userId, UserDataConfiguration.class);
			destConfigs = maskConfigurations(
					exportBiz.configurationsForUser(userId, UserDestinationConfiguration.class),
					serviceSettings, (Void) -> {
						return exportBiz.availableDestinationServices();
					});
			outputConfigs = maskConfigurations(
					exportBiz.configurationsForUser(userId, UserOutputConfiguration.class),
					serviceSettings, (Void) -> {
						return exportBiz.availableOutputFormatServices();
					});
		}
		return response(
				new DatumExportFullConfigurations(configs, dataConfigs, destConfigs, outputConfigs));
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.POST)
	public Response<UserDatumExportConfiguration> saveExportConfiguration(
			@RequestBody DatumExportProperties config) {
		if ( exportBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			if ( config.getDataConfigurationId() != null ) {
				config.setUserDataConfiguration(exportBiz.configurationForUser(config.getUserId(),
						UserDataConfiguration.class, config.getDataConfigurationId()));
			}
			if ( config.getDestinationConfigurationId() != null ) {
				config.setUserDestinationConfiguration(exportBiz.configurationForUser(config.getUserId(),
						UserDestinationConfiguration.class, config.getDestinationConfigurationId()));
			}
			if ( config.getOutputConfigurationId() != null ) {
				config.setUserOutputConfiguration(exportBiz.configurationForUser(config.getUserId(),
						UserOutputConfiguration.class, config.getOutputConfigurationId()));
			}
			Long id = exportBiz.saveDatumExportConfiguration(config);
			if ( id != null ) {
				config.setId(id);
				return response(config);
			}
		}
		return new Response<UserDatumExportConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteExportConfiguration(@PathVariable("id") Long id) {
		if ( exportBiz != null ) {
			UserDatumExportConfiguration config = exportBiz
					.datumExportConfigurationForUser(SecurityUtils.getCurrentActorUserId(), id);
			if ( config != null ) {
				exportBiz.deleteDatumExportConfiguration(config);
			}
		}
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/{id}/date", method = RequestMethod.POST)
	public Response<LocalDateTime> updateExportConfigurationDate(@PathVariable("id") Long id,
			@RequestBody Map<String, Object> body) {
		LocalDateTime result = null;
		if ( exportBiz != null ) {
			UserDatumExportConfiguration config = exportBiz
					.datumExportConfigurationForUser(SecurityUtils.getCurrentActorUserId(), id);
			if ( config != null ) {
				ScheduleType schedule = config.getSchedule();
				if ( schedule == null ) {
					schedule = ScheduleType.Daily;
				}
				DateTimeFormatter fmt;
				int parseLength;
				switch (schedule) {
					case Hourly:
						// include hours
						fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
						parseLength = 16;
						break;

					default:
						// just date
						fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
						parseLength = 10;
				}
				Object date = body.get("startingExportDate");
				if ( date != null ) {
					String s = date.toString();
					// ignore all after parse length
					if ( s.length() > parseLength ) {
						s = s.substring(0, parseLength);
					}
					LocalDateTime parsedDate = fmt.parse(s, LocalDateTime::from);
					config.setStartingExportDate(parsedDate);
					exportBiz.saveDatumExportConfiguration(config);
					result = parsedDate;
				}
			}
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data", method = RequestMethod.POST)
	public Response<DataConfiguration> saveDataConfiguration(@RequestBody UserDataConfiguration config) {
		if ( exportBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			Long id = exportBiz.saveConfiguration(config);
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
		if ( exportBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDataConfiguration config = exportBiz.configurationForUser(userId,
					UserDataConfiguration.class, id);
			if ( config != null ) {
				exportBiz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/output", method = RequestMethod.POST)
	public Response<OutputConfiguration> saveOutputConfiguration(
			@RequestBody UserOutputConfiguration config) {
		if ( exportBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			Long id = exportBiz.saveConfiguration(config);
			if ( id != null ) {
				config.setId(id);
				return response(maskConfiguration(config, serviceSettings, (Void) -> {
					return exportBiz.availableOutputFormatServices();
				}));
			}
		}
		return new Response<OutputConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/output/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteOutputConfiguration(@PathVariable("id") Long id) {
		if ( exportBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserOutputConfiguration config = exportBiz.configurationForUser(userId,
					UserOutputConfiguration.class, id);
			if ( config != null ) {
				exportBiz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/destination", method = RequestMethod.POST)
	public Response<DestinationConfiguration> saveDestinationConfiguration(
			@RequestBody UserDestinationConfiguration config) {
		if ( exportBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			Long id = exportBiz.saveConfiguration(config);
			if ( id != null ) {
				config.setId(id);
				return response(maskConfiguration(config, serviceSettings, (Void) -> {
					return exportBiz.availableDestinationServices();
				}));
			}
		}
		return new Response<DestinationConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/destination/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteDestinationConfiguration(@PathVariable("id") Long id) {
		if ( exportBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDestinationConfiguration config = exportBiz.configurationForUser(userId,
					UserDestinationConfiguration.class, id);
			if ( config != null ) {
				exportBiz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

	/**
	 * Submit an ad hoc export job request.
	 * 
	 * @param config
	 *        the export job configuration
	 * @return the task info
	 * @since 1.1
	 */
	@ResponseBody
	@RequestMapping(value = "/adhoc", method = RequestMethod.POST)
	public Response<UserAdhocDatumExportTaskInfo> submitAdhocExportJobRequest(
			@RequestBody UserDatumExportConfiguration config) {
		if ( exportBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			UserAdhocDatumExportTaskInfo info = exportBiz
					.saveAdhocDatumExportTaskForConfiguration(config);
			if ( info != null ) {
				info.setConfig(maskExportConfiguration(info.getConfig(), exportBiz));
				return response(info);
			}
		}
		return new Response<UserAdhocDatumExportTaskInfo>(false, null, null, null);
	}

	/**
	 * Submit an ad hoc export job request that refers to existing format and
	 * destination configurations, instead of submitting ad-hoc versions of
	 * those.
	 * 
	 * @param config
	 *        the export job configuration
	 * @return the task info
	 * @since 1.3
	 */
	@ResponseBody
	@RequestMapping(value = "/adhocRef", method = RequestMethod.POST)
	public Response<UserAdhocDatumExportTaskInfo> submitAdhocExportReferenceJobRequest(
			@RequestBody UserDatumExportConfiguration config) {
		if ( exportBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			if ( config.getUserDestinationConfigurationId() != null ) {
				config.setUserDestinationConfiguration(exportBiz.configurationForUser(config.getUserId(),
						UserDestinationConfiguration.class, config.getUserDestinationConfigurationId()));
			}
			if ( config.getUserOutputConfigurationId() != null ) {
				config.setUserOutputConfiguration(exportBiz.configurationForUser(config.getUserId(),
						UserOutputConfiguration.class, config.getUserOutputConfigurationId()));
			}
			UserAdhocDatumExportTaskInfo info = exportBiz
					.saveAdhocDatumExportTaskForConfiguration(config);
			if ( info != null ) {
				info.setConfig(maskExportConfiguration(info.getConfig(), exportBiz));
				return response(info);
			}
		}
		return new Response<UserAdhocDatumExportTaskInfo>(false, null, null, null);
	}

	/**
	 * Get the available ad hoc export tasks for the active actor.
	 * 
	 * @param stateKeys
	 *        an optional list of {@link DatumExportState} keys (or names) to
	 *        filter the results by, or {@literal null} for any state
	 * @param success
	 *        an optional "success" flag to filter the results by, or
	 *        {@literal null} for any success value (including {@literal null})
	 * @return the results
	 * @since 1.1
	 */
	@ResponseBody
	@RequestMapping(value = "/adhoc", method = RequestMethod.GET)
	public Response<List<UserAdhocDatumExportTaskInfo>> allAdhocTasks(
			@RequestParam(value = "states", required = false) String[] stateKeys,
			@RequestParam(value = "success", required = false) Boolean success) {
		if ( exportBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			Set<DatumExportState> states = null;
			if ( stateKeys != null && stateKeys.length > 0 ) {
				states = new HashSet<>(stateKeys.length);
				for ( String key : stateKeys ) {
					if ( key.isEmpty() ) {
						continue;
					}
					DatumExportState state = DatumExportState.forKey(key.charAt(0));
					if ( state == DatumExportState.Unknown ) {
						// try via full name
						try {
							state = DatumExportState.valueOf(key);
						} catch ( IllegalArgumentException e ) {
							throw new IllegalArgumentException("Unsupported state value [" + key + "]");
						}
					}
					if ( state != null && state != DatumExportState.Unknown ) {
						states.add(state);
					}
				}
				states = EnumSet.copyOf(states);
			}
			List<UserAdhocDatumExportTaskInfo> tasks = exportBiz.adhocExportTasksForUser(userId, states,
					success);
			for ( UserAdhocDatumExportTaskInfo task : tasks ) {
				task.setConfig(maskExportConfiguration(task.getConfig(), exportBiz));
			}
			return response(tasks);
		}
		return new Response<List<UserAdhocDatumExportTaskInfo>>(false, null, null, null);
	}

	private Configuration maskExportConfiguration(Configuration config, UserExportBiz exportBiz) {
		if ( config == null || exportBiz == null ) {
			return config;
		}
		BasicConfiguration respConfig = (config instanceof BasicConfiguration
				? (BasicConfiguration) config
				: new BasicConfiguration(config));

		// mask destination config settings, such as S3 password
		BasicDestinationConfiguration respDestConfig = (respConfig
				.getDestinationConfiguration() instanceof BasicDestinationConfiguration
						? (BasicDestinationConfiguration) respConfig.getDestinationConfiguration()
						: new BasicDestinationConfiguration(respConfig.getDestinationConfiguration()));
		respDestConfig = maskConfiguration(respDestConfig, serviceSettings, (Void) -> {
			return exportBiz.availableDestinationServices();
		});
		respConfig.setDestinationConfiguration(respDestConfig);
		return respConfig;
	}

}
