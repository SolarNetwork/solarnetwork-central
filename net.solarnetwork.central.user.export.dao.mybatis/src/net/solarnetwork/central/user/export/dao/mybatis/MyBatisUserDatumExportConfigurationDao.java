/* ==================================================================
 * MyBatisUserDatumExportConfigurationDao.java - 21/03/2018 4:55:32 PM
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

package net.solarnetwork.central.user.export.dao.mybatis;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.domain.export.ScheduleType;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * MyBatis implementation of {@link UserDatumExportConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserDatumExportConfigurationDao
		extends BaseMyBatisUserRelatedGenericDao<UserDatumExportConfiguration, Long>
		implements UserDatumExportConfigurationDao {

	/** The query name used for {@link #findConfigurationsForUser(Long)}. */
	public static final String QUERY_CONFIGURATIONS_FOR_USER = "find-UserDatumExportConfiguration-for-user";

	/**
	 * The query name used for
	 * {@link #findForExecution(DateTime, ScheduleType)}.
	 */
	public static final String QUERY_CONFIGURATIONS_FOR_EXECUTION = "find-UserDatumExportConfiguration-for-execution";

	/**
	 * Default constructor.
	 */
	public MyBatisUserDatumExportConfigurationDao() {
		super(UserDatumExportConfiguration.class, Long.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserDatumExportConfiguration> findConfigurationsForUser(Long userId) {
		return selectList(QUERY_CONFIGURATIONS_FOR_USER, userId, null, null);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserDatumExportConfiguration> findForExecution(Long userId, DateTime exportDate,
			ScheduleType scheduleType) {
		DateTime date = (exportDate != null ? exportDate : new DateTime());
		if ( scheduleType == null ) {
			scheduleType = ScheduleType.Daily;
		}
		DateTime.Property dateProperty;
		switch (scheduleType) {
			case Hourly:
				dateProperty = date.minuteOfHour();
				break;

			case Weekly:
				dateProperty = date.dayOfWeek();
				break;

			case Monthly:
				dateProperty = date.dayOfMonth();
				break;

			default:
				dateProperty = date.hourOfDay();
		}
		date = dateProperty.roundFloorCopy();
		Timestamp ts = new Timestamp(date.getMillis());
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		params.put("date", ts);
		params.put("schedule", scheduleType.getKey());
		return selectList(QUERY_CONFIGURATIONS_FOR_EXECUTION, params, null, null);
	}

}
