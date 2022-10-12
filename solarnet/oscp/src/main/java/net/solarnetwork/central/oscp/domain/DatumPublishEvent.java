/* ==================================================================
 * ActionEvent.java - 10/10/2022 1:48:44 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import static net.solarnetwork.central.oscp.domain.UserSettings.removeEmptySourceIdSegments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.datum.domain.OwnedGeneralNodeDatum;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.util.StringUtils;

/**
 * An event for publishing to a datum stream.
 * 
 * @param role
 *        the system user
 * @param action
 *        the action
 * @param src
 *        the source configuration
 * @param dest
 *        the destination configuration
 * @param group
 *        the capacity group
 * @param settings
 *        the publish settings
 * @param datum
 *        the datum to publish
 * @param params
 *        optional template parameters
 * @author matt
 * @version 1.0
 */
public record DatumPublishEvent(OscpRole role, String action, BaseOscpExternalSystemConfiguration<?> src,
		BaseOscpExternalSystemConfiguration<?> dest, CapacityGroupConfiguration group,
		DatumPublishSettings settings, Collection<OwnedGeneralNodeDatum> datum, KeyValuePair... params) {

	/** The parameter name for the role alias. */
	public static final String ROLE_ALIAS_PARAM = "role";

	/** The parameter name for the action. */
	public static final String ACTION_PARAM = "action";

	/** The parameter name for the source system ID. */
	public static final String SOURCE_PARAM = "src";

	/** The parameter name for the destination system ID. */
	public static final String DESTINATION_PARAM = "dest";

	/** The parameter name for a Capacity Group ID. */
	public static final String CAPACITY_GROUP_PARAM = "cg";

	/** The parameter name for a Capacity Group identifier. */
	public static final String CAPACITY_GROUP_IDENTIFIER_PARAM = "cgIdentifier";

	/** The parameter name for a Capacity Group name. */
	public static final String CAPACITY_GROUP_NAME_PARAM = "cgName";

	/** The parameter name for a Capacity Provider ID. */
	public static final String CAPACITY_PROVIDER_ID_PARAM = "cp";

	/** The parameter name for a Capacity Provider name. */
	public static final String CAPACITY_PROVIDER_NAME_PARAM = "cpName";

	/** The parameter name for a Capacity Optimizer ID. */
	public static final String CAPACITY_OPTIMIZER_ID_PARAM = "co";

	/** The parameter name for a Capacity Optimizer name. */
	public static final String CAPACITY_OPTIMIZER_NAME_PARAM = "coName";

	/** The parameter name for a {@code ForecastType} alias. */
	public static final String FORECAST_TYPE_PARAM = "forecastType";

	/**
	 * Get the event user ID.
	 * 
	 * @return the user ID
	 */
	public Long userId() {
		return (group != null ? group.getUserId()
				: src != null ? src.getUserId() : dest != null ? dest.getUserId() : null);
	}

	/**
	 * Get the node ID.
	 * 
	 * @return the settings node ID, or {@literal null} if not available
	 */
	public Long nodeId() {
		return (settings != null ? settings.getNodeId() : null);
	}

	/**
	 * Resolve the source ID according the the configured settings and
	 * parameters.
	 * 
	 * @return the source ID, or {@literal null} if no source ID template is
	 *         available
	 */
	public String sourceId() {
		String template = (settings != null ? settings.sourceIdTemplate() : null);
		if ( template == null ) {
			return null;
		}
		return removeEmptySourceIdSegments(StringUtils.expandTemplateString(template, parameters()));
	}

	/**
	 * Get the "publish to SolarIn" flag.
	 * 
	 * @return {@literal true} if datum should be published to SolarIn
	 */
	public boolean publishToSolarIn() {
		return (settings != null ? settings.isPublishToSolarIn() : false);
	}

	/**
	 * Get the "publish to SolarFlux" flag.
	 * 
	 * @return {@literal true} if datum should be published to SolarFlux
	 */
	public boolean publishToSolarFlux() {
		return (settings != null ? settings.isPublishToSolarFlux() : false);
	}

	private void populateSystemId(BaseOscpExternalSystemConfiguration<?> sys, Map<String, Object> m) {
		if ( sys == null || sys.getAuthRole() == null || sys.getAuthRole().role() == null ) {
			return;
		}

		switch (sys.getAuthRole().role()) {
			case CapacityProvider -> {
				m.put(CAPACITY_PROVIDER_ID_PARAM, sys.getEntityId());
				m.put(CAPACITY_PROVIDER_NAME_PARAM, sys.getName());
			}
			case CapacityOptimizer -> {
				m.put(CAPACITY_OPTIMIZER_ID_PARAM, sys.getEntityId());
				m.put(CAPACITY_OPTIMIZER_NAME_PARAM, sys.getName());
			}
			case FlexibilityProvider -> {
				/* ignore */ }
		}
	}

	/**
	 * Get a map of all event parameters.
	 * 
	 * @return the event parameters
	 */
	public Map<String, Object> parameters() {
		Map<String, Object> m = new LinkedHashMap<>(8);
		if ( role != null ) {
			m.put(ROLE_ALIAS_PARAM, role.getAlias());
		}
		if ( action != null ) {
			m.put(ACTION_PARAM, action);
		}
		if ( src != null && src.getEntityId() != null ) {
			m.put(SOURCE_PARAM, src.getEntityId());
			populateSystemId(src, m);
		}
		if ( dest != null && dest.getEntityId() != null ) {
			m.put(DESTINATION_PARAM, dest.getEntityId());
			populateSystemId(dest, m);
		}
		if ( group != null ) {
			m.put(CAPACITY_GROUP_PARAM, group.getEntityId());
			m.put(CAPACITY_GROUP_IDENTIFIER_PARAM, group.getIdentifier());
			m.put(CAPACITY_GROUP_NAME_PARAM, group.getName());
		}
		if ( params != null ) {
			for ( KeyValuePair p : params ) {
				if ( p.getKey() != null && p.getValue() != null ) {
					m.put(p.getKey(), p.getValue());
				}
			}
		}
		return m;
	}

}
