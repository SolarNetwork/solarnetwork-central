/* ==================================================================
 * CombiningConfig.java - 26/05/2018 6:50:38 AM
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

package net.solarnetwork.central.datum.dao.mybatis;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.datum.domain.CombiningType;

/**
 * Data structure to help with combining query execution.
 * 
 * @author matt
 * @version 1.0
 * @since 2.7
 */
public class CombiningConfig {

	/** A name to use for node IDs configuration. */
	public static final String NODE_IDS_CONFIG = "node";

	/** A name to use for source IDs configuration. */
	public static final String SOURCE_IDS_CONFIG = "source";

	private final CombiningType type;
	private final Map<String, CombineIdsConfig<?>> configMap;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type
	 * @param configs
	 *        the list of configurations
	 */
	public CombiningConfig(CombiningType type, List<CombineIdsConfig<Object>> configs) {
		super();
		this.type = type;
		if ( configs == null || configs.isEmpty() ) {
			this.configMap = Collections.emptyMap();
		} else {
			this.configMap = new LinkedHashMap<String, CombineIdsConfig<?>>(configs.size());
			for ( CombineIdsConfig<?> config : configs ) {
				this.configMap.put(config.getName(), config);
			}
		}
	}

	/**
	 * Get the combining action type.
	 * 
	 * @return the type
	 */
	public CombiningType getType() {
		return type;
	}

	/**
	 * Get the combining IDs configurations.
	 * 
	 * @return the IDs configurations
	 */
	public Collection<CombineIdsConfig<?>> getConfigs() {
		return configMap.values();
	}

	/**
	 * Test if a node IDs configuration is available.
	 * 
	 * @return {@literal true} if an IDs configuration for
	 *         {@link #NODE_IDS_CONFIG} exists
	 */
	public boolean isWithNodeIds() {
		return configMap.containsKey(NODE_IDS_CONFIG);
	}

	/**
	 * Test if a source IDs configuration is available.
	 * 
	 * @return {@literal true} if an IDs configuration for
	 *         {@link #SOURCE_IDS_CONFIG} exists
	 */
	public boolean isWithSourceIds() {
		return configMap.containsKey(SOURCE_IDS_CONFIG);
	}

}
