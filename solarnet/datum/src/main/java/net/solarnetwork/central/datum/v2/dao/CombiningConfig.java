/* ==================================================================
 * CombiningConfig.java - 4/12/2020 3:06:51 pm
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

package net.solarnetwork.central.datum.v2.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.domain.CombiningType;

/**
 * Data structure to help with combining query execution.
 *
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class CombiningConfig {

	/** A name to use for node IDs configuration. */
	public static final String OBJECT_IDS_CONFIG = "obj";

	/** A name to use for source IDs configuration. */
	public static final String SOURCE_IDS_CONFIG = "source";

	private final CombiningType type;
	private final Map<String, CombiningIdsConfig<?>> configMap;

	/**
	 * Constructor.
	 *
	 * @param type
	 *        the type
	 * @param configs
	 *        the list of configurations
	 * @throws IllegalArgumentException
	 *         if {@code type} is {@code null}
	 */
	public CombiningConfig(CombiningType type, @Nullable List<CombiningIdsConfig<Object>> configs) {
		super();
		this.type = requireNonNullArgument(type, "type");
		if ( configs == null || configs.isEmpty() ) {
			this.configMap = Map.of();
		} else {
			this.configMap = new LinkedHashMap<>(configs.size());
			for ( CombiningIdsConfig<?> config : configs ) {
				this.configMap.put(config.getName(), config);
			}
		}
	}

	/**
	 * Create a new {@link CombiningConfig} instance from an
	 * {@link ObjectStreamCriteria}.
	 *
	 * @param filter
	 *        the criteria
	 * @return the config, or {@code null} if {@code filter} is {@code null} or
	 *         has no combining configuration
	 */
	public static @Nullable CombiningConfig configFromCriteria(@Nullable ObjectStreamCriteria filter) {
		if ( filter == null || !filter.hasIdMappings() ) {
			return null;
		}
		Map<Long, Set<Long>> objMappings = filter.getObjectIdMappings();
		Map<String, Set<String>> sourceMappings = filter.getSourceIdMappings();
		List<CombiningIdsConfig<Object>> configs = new ArrayList<>(2);
		if ( objMappings != null && !objMappings.isEmpty() ) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			CombiningIdsConfig<Object> config = (CombiningIdsConfig) new CombiningIdsConfig<>(
					OBJECT_IDS_CONFIG, objMappings);
			configs.add(config);
		}
		if ( sourceMappings != null && !sourceMappings.isEmpty() ) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			CombiningIdsConfig<Object> config = (CombiningIdsConfig) new CombiningIdsConfig<>(
					SOURCE_IDS_CONFIG, sourceMappings);
			configs.add(config);
		}

		CombiningType type = filter.getCombiningType();
		if ( type == null ) {
			type = CombiningType.Sum;
		}

		return new CombiningConfig(type, configs);
	}

	/**
	 * Get the combining action type.
	 *
	 * @return the type
	 */
	public final CombiningType getType() {
		return type;
	}

	/**
	 * Get all available ID configuration keys.
	 *
	 * @return the available ID configuration keys
	 */
	public final Set<String> getIdsConfigKeys() {
		return configMap.keySet();
	}

	/**
	 * Get the IDs configuration for a specific key, casting the result.
	 *
	 * @param <T>
	 *        the expected IDs configuration type
	 * @param key
	 *        the configuration key to get
	 * @return the configuration, or {@code null} if not available
	 * @throws ClassCastException
	 *         if the value cannot be cast to {@code T}
	 */
	@SuppressWarnings("unchecked")
	public final <T> @Nullable CombiningIdsConfig<T> getIdsConfig(String key) {
		return (CombiningIdsConfig<T>) configMap.get(key);
	}

	/**
	 * Get the combining IDs configurations.
	 *
	 * @return the IDs configurations
	 */
	public final Collection<CombiningIdsConfig<?>> getIdsConfigs() {
		return configMap.values();
	}

	/**
	 * Test if an object IDs configuration is available.
	 *
	 * @return {@literal true} if an IDs configuration for
	 *         {@link #OBJECT_IDS_CONFIG} exists
	 */
	public final boolean isWithObjectIds() {
		return configMap.containsKey(OBJECT_IDS_CONFIG);
	}

	/**
	 * Test if a source IDs configuration is available.
	 *
	 * @return {@literal true} if an IDs configuration for
	 *         {@link #SOURCE_IDS_CONFIG} exists
	 */
	public final boolean isWithSourceIds() {
		return configMap.containsKey(SOURCE_IDS_CONFIG);
	}

}
