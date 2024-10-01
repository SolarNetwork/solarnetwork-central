/* ==================================================================
 * CloudDatumStreamService.java - 29/09/2024 2:50:01 pm
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

package net.solarnetwork.central.c2c.biz;

import java.util.Locale;
import java.util.Map;
import net.solarnetwork.central.c2c.domain.CloudDataValueGroup;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for a cloud datum stream service.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/**
	 * Get a localized collection of the available data value filter criteria.
	 *
	 * <p>
	 * The {@link LocalizedServiceInfo#getId()} of each returned object
	 * represents the name of a filter parameter that can be passed to
	 * {@link #dataValueGroups(Map)}.
	 * </p>
	 *
	 * @param locale
	 *        the desired locale
	 * @return the available filter criteria, never {@literal null}
	 */
	Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale);

	/**
	 * List data value groups.
	 *
	 * @param userId
	 *        the owner user ID
	 * @param configId
	 *        the configuration ID of the {@link CloudDatumStreamConfiguration}
	 *        to get the data value groups for
	 * @param filters
	 *        an optional set of search filters to limit the data value groups
	 *        to; the available key values come from the identifiers returned by
	 *        {@link #dataValueFilters(Locale)}
	 * @return the available groups, never {@literal null}
	 *
	 */
	Iterable<CloudDataValueGroup> dataValueGroups(Long userId, Long configId, Map<String, ?> filters);

}
