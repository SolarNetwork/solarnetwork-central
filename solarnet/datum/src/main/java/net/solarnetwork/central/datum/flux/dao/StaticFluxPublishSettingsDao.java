/* ==================================================================
 * StaticFluxPublishSettingsDao.java - 26/06/2024 3:49:47 pm
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

package net.solarnetwork.central.datum.flux.dao;

import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.util.ObjectUtils;

/**
 * Static implementation of {@link FluxPublishSettingsDao}.
 *
 * <p>
 * This is useful in applications that manage SolarFlux publish settings
 * themselves.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class StaticFluxPublishSettingsDao implements FluxPublishSettingsDao {

	private final FluxPublishSettings settings;

	/**
	 * Constructor.
	 *
	 * @param settings
	 *        the static settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StaticFluxPublishSettingsDao(FluxPublishSettings settings) {
		super();
		this.settings = ObjectUtils.requireNonNullArgument(settings, "settings");
	}

	@Override
	public FluxPublishSettings nodeSourcePublishConfiguration(Long userId, Long nodeId,
			String sourceId) {
		return settings;
	}

}
