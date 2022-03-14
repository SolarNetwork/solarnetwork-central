/* ==================================================================
 * UserDatumConfiguration.java - 9/07/2018 10:04:36 AM
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

package net.solarnetwork.central.user.expire.domain;

import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.service.IdentifiableConfiguration;

/**
 * User specific datum expire policy configuration entity.
 * 
 * @author matt
 * @version 2.0
 */
public interface DataConfiguration extends IdentifiableConfiguration {

	/**
	 * Get a criteria for which datum to include in the expire policy.
	 * 
	 * <p>
	 * It is expected that a minimum of the following filter properties are
	 * supported:
	 * </p>
	 * 
	 * <ul>
	 * <li><code>nodeIds</code> - the list of node IDs to include</li>
	 * <li><code>sourceIds</code> - a list of source IDs, or source ID patterns,
	 * to include</li>
	 * <li><code>aggregation</code> the aggregation level to include, or
	 * {@literal null} for only raw data</li>
	 * </ul>
	 * 
	 * @return the datum filter
	 */
	AggregateGeneralNodeDatumFilter getDatumFilter();

	/**
	 * Get the minimum age after which datum matching the filter should expire.
	 * 
	 * @return the expire age, in days
	 */
	int getExpireDays();

	/**
	 * Flag indicating if the configuration is "active" and applicable.
	 * 
	 * @return {@literal true} if the configuration is active, {@literal false}
	 *         if the configuration should not be applied
	 */
	boolean isActive();

}
