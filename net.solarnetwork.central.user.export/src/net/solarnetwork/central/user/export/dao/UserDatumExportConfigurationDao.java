/* ==================================================================
 * UserDatumExportConfigurationDao.java - 21/03/2018 11:12:06 AM
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

package net.solarnetwork.central.user.export.dao;

import java.util.List;
import org.joda.time.DateTime;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.dao.UserRelatedGenericDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * DAO API for {@link UserDatumExportConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserDatumExportConfigurationDao
		extends UserRelatedGenericDao<UserDatumExportConfiguration, Long>,
		UserConfigurationDao<UserDatumExportConfiguration, Long> {

	/**
	 * Find all configurations requiring export task execution.
	 * 
	 * @param exportDate
	 *        the export date
	 * @param scheduleType
	 *        the schedule type to find
	 * @return the configurations, never {@literal null}
	 */
	List<UserDatumExportConfiguration> findForExecution(DateTime exportDate, ScheduleType scheduleType);

	/**
	 * Update the minimum export date on an existing configuration, as long as
	 * it is <b>older</b> than {@code minimumDate}.
	 * 
	 * @param id
	 *        the configuration ID to update
	 * @param userId
	 *        the user ID
	 * @param minimumDate
	 *        the minimum date to set
	 * @return the number of rows updated
	 */
	int updateMinimumExportDate(Long id, Long userId, DateTime minimumDate);

}
