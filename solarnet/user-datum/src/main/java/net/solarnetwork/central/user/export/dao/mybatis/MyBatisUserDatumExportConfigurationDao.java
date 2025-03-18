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
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link UserDatumExportConfigurationDao}.
 * 
 * @author matt
 * @version 2.2
 */
public class MyBatisUserDatumExportConfigurationDao
		extends BaseMyBatisUserRelatedGenericDao<UserDatumExportConfiguration, UserLongCompositePK>
		implements UserDatumExportConfigurationDao {

	/** The query name used for {@link #findConfigurationsForUser(Long)}. */
	public static final String QUERY_CONFIGURATIONS_FOR_USER = "find-UserDatumExportConfiguration-for-user";

	/**
	 * The query name used for {@link #findForExecution(Instant, ScheduleType)}.
	 */
	public static final String QUERY_CONFIGURATIONS_FOR_EXECUTION = "find-UserDatumExportConfiguration-for-execution";

	/**
	 * The query name used for
	 * {@link #updateMinimumExportDate(Long, Long, Instant)}.
	 */
	public static final String UPDATE_MINIMUM_EXPORT_DATE = "update-UserDatumExportConfiguration-minimum-export-date";

	/**
	 * Default constructor.
	 */
	public MyBatisUserDatumExportConfigurationDao() {
		super(UserDatumExportConfiguration.class, UserLongCompositePK.class);
	}

	@Override
	public UserLongCompositePK create(Long userId, UserDatumExportConfiguration entity) {
		if ( !userId.equals(entity.getUserId()) ) {
			entity = entity.copyWithId(new UserLongCompositePK(userId, entity.getConfigId()));
		}
		return save(entity);
	}

	@Override
	protected UserLongCompositePK handleInsert(UserDatumExportConfiguration datum) {
		UserLongCompositePK id = super.handleInsert(datum);
		if ( !id.entityIdIsAssigned() && datum.getConfigId() != null ) {
			id = new UserLongCompositePK(id.getUserId(), datum.getConfigId());
		}
		return id;
	}

	@Override
	public Collection<UserDatumExportConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		return findConfigurationsForUser(userId);
	}

	@Override
	public List<UserDatumExportConfiguration> findConfigurationsForUser(Long userId) {
		return selectList(QUERY_CONFIGURATIONS_FOR_USER, userId, null, null);
	}

	@Override
	public List<UserDatumExportConfiguration> findForExecution(Instant exportDate,
			ScheduleType scheduleType) {
		if ( scheduleType == null ) {
			scheduleType = ScheduleType.Daily;
		}
		Timestamp ts = Timestamp.from(exportDate);
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", ts);
		params.put("schedule", scheduleType.getKey());
		return selectList(QUERY_CONFIGURATIONS_FOR_EXECUTION, params, null, null);
	}

	@Override
	public int updateMinimumExportDate(Long id, Long userId, Instant minimumDate) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", id);
		params.put("userId", userId);
		params.put("date", Timestamp.from(minimumDate));
		return getSqlSession().update(UPDATE_MINIMUM_EXPORT_DATE, params);
	}

}
