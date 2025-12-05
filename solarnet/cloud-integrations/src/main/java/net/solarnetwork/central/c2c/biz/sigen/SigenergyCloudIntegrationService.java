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
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.BaseRestOperationsCloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.SettingSpecifier;
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

	/** The base URL to the AlsoEnergy API. */
	public static final String BASE_URI_TEMPLATE = "https://api-{region}.sigencloud.com";

	/** An app secret setting name. */
	public static final String REGION_SETTING = "region";

	/** An app key setting name. */
	public static final String APP_KEY_SETTING = "apKeyId";

	/** An app secret setting name. */
	public static final String APP_SECRET_SETTING = "appSecret";

	/**
	 * The well-known URLs.
	 */
	public static final Map<String, URI> WELL_KNOWN_URLS;
	static {
		Map<String, URI> map = new LinkedHashMap<>(SigenergyRegion.values().length + 1);
		map.put(API_BASE_WELL_KNOWN_URL, URI.create(BASE_URI_TEMPLATE.formatted("{region}")));
		for ( SigenergyRegion reg : SigenergyRegion.values() ) {
			map.put(API_BASE_WELL_KNOWN_URL + ':' + reg.name(),
					URI.create(BASE_URI_TEMPLATE.formatted(reg.getKey())));
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
				.collect(Collectors.toMap(SigenergyRegion::getKey, SigenergyRegion::name, (l, r) -> r,
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
	 * @param restOps
	 *        the REST operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SigenergyCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			Collection<CloudControlService> controlServices, UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "Sigenergy", datumStreamServices, controlServices,
				userEventAppenderBiz, encryptor, SETTINGS, WELL_KNOWN_URLS,
				null /* TODO */);
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

}
