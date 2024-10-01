/* ==================================================================
 * LocusEnergyCloudDatumStreamService.java - 30/09/2024 8:13:21 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.domain.CloudDataValueGroup;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.MultiValueSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;

/**
 * Locus Energy implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class LocusEnergyCloudDatumStreamService extends
		BaseSettingsSpecifierLocalizedServiceInfoProvider<String> implements CloudDatumStreamService {

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	private static final List<SettingSpecifier> SETTINGS;
	static {
		var settings = new ArrayList<SettingSpecifier>(1);

		// @formatter:off
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(
				GRANULARITY_SETTING, "latest");
		var granularityTitles = new LinkedHashMap<String, String>(2);
		for (String g : new String[] {
				"latest",
		        "1min",
		        "5min",
		        "15min",
		        "hourly",
		        "daily",
		        "monthly",
		        "yearly"}) {
			granularityTitles.put(g, g);
		}
		granularitySpec.setValueTitles(granularityTitles);
		settings.add(granularitySpec);
		// @formatter:on

		SETTINGS = Collections.unmodifiableList(settings);
	}

	/**
	 * Constructor.
	 */
	public LocusEnergyCloudDatumStreamService() {
		super("net.solarnetwork.c2c.LocusEnergyDatumStreamService");
	}

	@Override
	public String getDisplayName() {
		return "Locus Energy Datum Stream Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return SETTINGS;
	}

	@Override
	protected void populateInfoMessages(Locale locale, SettingSpecifier spec, Map<String, String> msgs,
			MessageSource ms) {
		super.populateInfoMessages(locale, spec, msgs, ms);
		if ( spec instanceof KeyedSettingSpecifier<?> k ) {
			if ( GRANULARITY_SETTING.equals(k.getKey()) ) {
				MultiValueSettingSpecifier mv = (MultiValueSettingSpecifier) spec;
				for ( String valueKey : mv.getValueTitles().keySet() ) {
					String titleKey = "granularity." + valueKey;
					msgs.put(titleKey, ms.getMessage(titleKey, null, valueKey, locale));
				}
			}
		}
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { "componentId", "siteId" } ) {
			result.add(new BasicLocalizedServiceInfo("collectionId", locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<CloudDataValueGroup> dataValueGroups(Long userId, Long configId,
			Map<String, ?> filters) {
		// TODO Auto-generated method stub
		return null;
	}

}
