/* ==================================================================
 * SigenergyCloudIntegrationService.java - 5/12/2025 3:07:51 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.sigen;

import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.Map.entry;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.BASE_URI_TEMPLATE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.BaseRestOperationsCloudIntegrationService;
import net.solarnetwork.central.c2c.domain.BasicCloudConfigurationTopicLocalizedServiceInfo;
import net.solarnetwork.central.c2c.domain.CloudConfigurationTopicLocalizedServiceInfo;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationTopicConfiguration;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * Sigenergy implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
public class SigenergyCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.sigen";

	/** An app secret setting name. */
	public static final String REGION_SETTING = "region";

	/** An app key setting name. */
	public static final String APP_KEY_SETTING = "appKey";

	/** An app secret setting name. */
	public static final String APP_SECRET_SETTING = "appSecret";

	/**
	 * The well-known URLs.
	 */
	public static final Map<String, URI> WELL_KNOWN_URLS;
	static {
		Map<String, URI> map = new LinkedHashMap<>(SigenergyRegion.values().length + 1);
		map.put(API_BASE_WELL_KNOWN_URL, UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
				.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri());
		for ( SigenergyRegion reg : SigenergyRegion.values() ) {
			map.put(API_BASE_WELL_KNOWN_URL + ':' + reg.name(), UriComponentsBuilder
					.fromUriString(BASE_URI_TEMPLATE).buildAndExpand(reg.getKey()).toUri());
		}
		WELL_KNOWN_URLS = Collections.unmodifiableMap(map);
	}

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// menu for region
		var regionSpec = new BasicMultiValueSettingSpecifier(REGION_SETTING,
				SigenergyRegion.AustraliaNewZealand.name());
		var regionTitles = unmodifiableMap(Arrays.stream(SigenergyRegion.values())
				.collect(Collectors.toMap(SigenergyRegion::name, SigenergyRegion::name, (l, r) -> r,
						() -> new LinkedHashMap<>(SigenergyRegion.values().length))));
		regionSpec.setValueTitles(regionTitles);

		// @formatter:off
		SETTINGS = List.of(
				regionSpec,
				new BasicTextFieldSettingSpecifier(APP_KEY_SETTING, null),
				new BasicTextFieldSettingSpecifier(APP_SECRET_SETTING, null, true),
				BASE_URL_SETTING_SPECIFIER);
		// @formatter:on
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections
			.unmodifiableSet(SettingUtils.secureKeys(SETTINGS));

	/**
	 * Configuration topic to "adopt" a system into the application associated
	 * with the integration credentials.
	 */
	public static final String ONBOARD_CONFIGURATION_TOPIC = "onboard";

	/**
	 * Configuration topic to "release" a system from the application associated
	 * with the integration credentials.
	 */
	public static final String UNONBOARD_CONFIGURATION_TOPIC = "unonboard";

	/** The setting name for a system ID. */
	public static final String SYSTEM_ID_SETTING = "systemId";

	/**
	 * The available configuration topics and their associated settings.
	 */
	public static final Map<String, List<SettingSpecifier>> CONFIGURATION_TOPICS;
	static {
		SettingSpecifier systemIdSpec = new BasicTextFieldSettingSpecifier(SYSTEM_ID_SETTING, null);
		// @formatter:off
		CONFIGURATION_TOPICS = Map.ofEntries(
				entry(ONBOARD_CONFIGURATION_TOPIC, List.of(systemIdSpec)),
				entry(UNONBOARD_CONFIGURATION_TOPIC, List.of(systemIdSpec))
				);
		// @formatter:on
	}

	/**
	 * Constructor.
	 *
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param controlServices
	 *        the control services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param restOpsHelper
	 *        the REST operations helper; see
	 *        {@link SigenergyRestOperationsHelper}
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SigenergyCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			Collection<CloudControlService> controlServices, UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, RestOperationsHelper restOpsHelper) {
		super(SERVICE_IDENTIFIER, "Sigenergy", datumStreamServices, controlServices,
				userEventAppenderBiz, encryptor, SETTINGS, WELL_KNOWN_URLS, restOpsHelper);
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// check that authentication settings provided
		final List<ErrorDetail> errorDetails = new ArrayList<>(2);
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final String appKey = nonEmptyString(integration.serviceProperty(APP_KEY_SETTING, String.class));
		if ( appKey == null ) {
			String errMsg = ms.getMessage("error.appKey.missing", null, locale);
			errorDetails.add(new ErrorDetail(APP_KEY_SETTING, null, errMsg));
		}

		final String appSecret = nonEmptyString(
				integration.serviceProperty(APP_SECRET_SETTING, String.class));
		if ( appSecret == null ) {
			String errMsg = ms.getMessage("error.appSecret.missing", null, locale);
			errorDetails.add(new ErrorDetail(APP_SECRET_SETTING, null, errMsg));
		}

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("SGCI.0001", errMsg, errorDetails);
		}

		// validate by requesting the available systems
		final SigenergyRegion region = SigenergyRestOperationsHelper.resolveRegion(integration);
		try {
			final String response = restOpsHelper.httpGet("List systems", integration, String.class,
					(req) -> UriComponentsBuilder
							.fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
							.path(SigenergyRestOperationsHelper.SYSTEM_LIST_PATH)
							.buildAndExpand(region.getKey()).toUri(),
					HttpEntity::getBody);
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( RemoteServiceException e ) {
			return validationResult(e, null);
		} catch ( Exception e ) {
			return Result.error("SGCI.0002", "Validation failed: " + e.getMessage());
		}
	}

	@Override
	public Iterable<CloudConfigurationTopicLocalizedServiceInfo> supportedConfigurationTopics(
			Locale locale) {
		return CONFIGURATION_TOPICS.entrySet().stream().sorted(comparing(Entry::getKey)).map(e -> {
			var messageSource = new ResourceBundleMessageSource();
			messageSource.setBundleClassLoader(SigenergyCloudIntegrationService.class.getClassLoader());
			messageSource
					.setBasename(SigenergyCloudIntegrationService.class.getName() + "_" + e.getKey());

			var info = new BaseSettingsSpecifierLocalizedServiceInfoProvider(e.getKey()) {

				@Override
				public String getDisplayName() {
					return e.getKey();
				}

				@Override
				public List<SettingSpecifier> getSettingSpecifiers() {
					return e.getValue();
				}

			};
			info.setMessageSource(messageSource);
			return (CloudConfigurationTopicLocalizedServiceInfo) new BasicCloudConfigurationTopicLocalizedServiceInfo(
					info.getLocalizedServiceInfo(locale != null ? locale : Locale.getDefault()),
					e.getValue(), e.getKey());
		}).toList();
	}

	@Override
	public Result<?> configure(CloudIntegrationConfiguration integration,
			CloudIntegrationTopicConfiguration settings) {
		if ( !CONFIGURATION_TOPICS.containsKey(requireNonNullArgument(settings.topic(), "topic")) ) {
			throw new IllegalArgumentException("Unsupported topic [" + settings.topic() + "]");
		}
		// TODO
		return Result.success();
	}

}
