/* ==================================================================
 * DatumValidationType.java - 20/05/2026 10:22:54 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.domain.KeyedValue;

/**
 * Enumeration of common datum validation types.
 *
 * @author matt
 * @version 1.0
 */
public enum DatumValidationType implements KeyedValue {

	/**
	 * Validate energy data values.
	 *
	 * <p>
	 * Some integrations produce unexpectedly large energy readings, commonly
	 * referred to as "spikes". This validation attempts to identify and correct
	 * for these "spikes", for example by limiting their value in some way.
	 * </p>
	 *
	 */
	EnergySpike(DatumValidationType.ENERGY_SPIKE_VALIDATION_TYPE),

	/**
	 * Validate datum stream time gaps.
	 *
	 * <p>
	 * This validation looks for unexpected gaps of time in the datum stream.
	 * </p>
	 */
	TimeGap(DatumValidationType.TIME_GAP_VALIDATION_TYPE),

	;

	/** The energy spike validation type key value. */
	public static final String ENERGY_SPIKE_VALIDATION_TYPE = "energy-spike";

	/** The time gap validation type key value. */
	public static final String TIME_GAP_VALIDATION_TYPE = "time-gap";

	/**
	 * A mapping of all common validation type keys to associated enum
	 * instances.
	 */
	public static final Map<String, DatumValidationType> KEY_MAPPING;
	static {
		var map = new HashMap<String, DatumValidationType>(DatumValidationType.values().length);
		for ( DatumValidationType type : DatumValidationType.values() ) {
			map.put(type.key, type);
		}
		KEY_MAPPING = Map.copyOf(map);
	}

	private final String key;

	private DatumValidationType(String key) {
		this.key = key;
	}

	@Override
	public String getKey() {
		return key;
	}

}
