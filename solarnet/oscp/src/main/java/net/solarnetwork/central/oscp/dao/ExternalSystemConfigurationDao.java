/* ==================================================================
 * ExternalSystemConfigurationDao.java - 18/08/2022 8:32:15 am
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

package net.solarnetwork.central.oscp.dao;

import java.time.Instant;
import java.util.function.Function;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.util.CapacityGroupTaskContext;
import net.solarnetwork.central.oscp.util.TaskContext;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for external system configuration DAOs.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExternalSystemConfigurationDao<C extends BaseOscpExternalSystemConfiguration<C>>
		extends GenericCompositeKey2Dao<C, UserLongCompositePK, Long, Long>,
		FilterableDao<C, UserLongCompositePK, ConfigurationFilter>, ExternalSystemAuthTokenDao {

	/** User event tags for Capacity Provider heartbeat events. */
	String[] CAPACITY_PROVIDER_HEARTBEAT_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_PROVIDER_TAG, OscpUserEvents.HEARTBEAT_TAG };

	/** User event tags for Capacity Provider heartbeat error events. */
	String[] CAPACITY_PROVIDER_HEARTBEAT_ERROR_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_PROVIDER_TAG, OscpUserEvents.HEARTBEAT_TAG,
			OscpUserEvents.ERROR_TAG };

	/** User event tags for Capacity Optimizer heartbeat events. */
	String[] CAPACITY_OPTIMIZER_HEARTBEAT_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_OPTIMIZER_TAG, OscpUserEvents.HEARTBEAT_TAG };

	/** User event tags for Capacity Optimizer heartbeat error events. */
	String[] CAPACITY_OPTIMIZER_HEARTBEAT_ERROR_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_OPTIMIZER_TAG, OscpUserEvents.HEARTBEAT_TAG,
			OscpUserEvents.ERROR_TAG };

	/** User event tags for Capacity Provider measurement events. */
	String[] CAPACITY_PROVIDER_MEASUREMENT_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_PROVIDER_TAG, OscpUserEvents.MEASUREMENT_TAG };

	/** User event tags for Capacity Provider measurement error events. */
	String[] CAPACITY_PROVIDER_MEASUREMENT_ERROR_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_PROVIDER_TAG, OscpUserEvents.MEASUREMENT_TAG,
			OscpUserEvents.ERROR_TAG };

	/** User event tags for Capacity Optimizer measurement events. */
	String[] CAPACITY_OPTIMIZER_MEASUREMENT_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_OPTIMIZER_TAG, OscpUserEvents.MEASUREMENT_TAG };

	/** User event tags for Capacity Optimizer measurement error events. */
	String[] CAPACITY_OPTIMIZER_MEASUREMENT_ERROR_TAGS = new String[] { OscpUserEvents.OSCP_EVENT_TAG,
			OscpUserEvents.CAPACITY_OPTIMIZER_TAG, OscpUserEvents.MEASUREMENT_TAG,
			OscpUserEvents.ERROR_TAG };

	/**
	 * Get a persisted entity by its primary key, locking the row for updates
	 * within the current transaction.
	 * 
	 * @param id
	 *        the primary key to retrieve
	 * @return the domain object, or {@literal null} if not available
	 */
	C getForUpdate(UserLongCompositePK id);

	/**
	 * Save system settings for a given configuration.
	 * 
	 * <p>
	 * The configuration must exist prior to saving any settings for it.
	 * </p>
	 * 
	 * @param id
	 *        the primary key to save the settings for
	 * @param settings
	 *        the settings to save
	 */
	void saveSettings(UserLongCompositePK id, SystemSettings settings);

	/**
	 * Compare and update the heartbeat date.
	 * 
	 * @param id
	 *        the primary key to save the settings for
	 * @param expected
	 *        the expected value
	 * @param ts
	 *        the timestamp to set of {@code expected} matches the current value
	 */
	boolean compareAndSetHeartbeat(UserLongCompositePK id, Instant expected, Instant ts);

	/**
	 * Update the offline date.
	 * 
	 * @param id
	 *        the primary key to save the settings for
	 * @param ts
	 *        the
	 */
	void updateOfflineDate(UserLongCompositePK id, Instant ts);

	/**
	 * Lay claim to an external system who needs to have a heartbeat sent.
	 * 
	 * @param handler
	 *        a function that will be passed a task context for an external
	 *        system that needs to have a heartbeat sent, and returns a new
	 *        heartbeat date if a heartbeat was successfully sent, or
	 *        {@literal null} otherwise
	 * @return {@literal true} if the heartbeat date was updated with the value
	 *         returned from {@code handler}
	 */
	boolean processExternalSystemWithExpiredHeartbeat(Function<TaskContext<C>, Instant> handler);

	/**
	 * Lay claim to an external system who needs to have a measurement sent.
	 * 
	 * @param handler
	 *        a function that will be passed a task context for an external
	 *        system that needs to have a measurement sent, and returns a new
	 *        heartbeat date if a heartbeat was successfully sent, or
	 *        {@literal null} otherwise
	 * @return {@literal true} if the heartbeat date was updated with the value
	 *         returned from {@code handler}
	 */
	boolean processExternalSystemWithExpiredMeasurement(
			Function<CapacityGroupTaskContext<C>, Instant> handler);

}
