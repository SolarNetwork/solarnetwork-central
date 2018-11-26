/* ==================================================================
 * MyBatisUserDatumDeleteJobInfoDao.java - 26/11/2018 9:25:21 AM
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

package net.solarnetwork.central.user.expire.dao.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;

/**
 * MyBatis implementation of {@link UserDatumDeleteJobInfoDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserDatumDeleteJobInfoDao extends
		BaseMyBatisGenericDao<DatumDeleteJobInfo, UserUuidPK> implements UserDatumDeleteJobInfoDao {

	/** The default query name used for {@link #claimQueuedJob()}. */
	public static final String QUERY_FOR_CLAIMING_JOB = "get-DatumDeleteJobInfo-for-claim";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(DateTime)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-DatumDeleteJobInfo-completed";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(DateTime)}.
	 */
	public static final String UPDATE_DELETE_FOR_USER = "delete-DatumDeleteJobInfo-for-user";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_STATE = "update-DatumDeleteJobInfo-state";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_CONFIG = "update-DatumDeleteJobInfo-config";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_PROGRESS = "update-DatumDeleteJobInfo-progress";

	/**
	 * The query name used for {@link #findForUser(Long, Set)}.
	 */
	public static final String QUERY_FOR_USER = "find-DatumDeleteJobInfo-for-user";

	private String queryForClaimQueuedJob;
	private String updateDeleteCompletedJobs;
	private String updateDeleteForUser;
	private String updateJobState;
	private String updateJobConfiguration;
	private String updateJobProgress;
	private String queryForUser;

	/**
	 * Constructor.
	 */
	public MyBatisUserDatumDeleteJobInfoDao() {
		super(DatumDeleteJobInfo.class, UserUuidPK.class);
		setQueryForUser(QUERY_FOR_USER);
		setQueryForClaimQueuedJob(QUERY_FOR_CLAIMING_JOB);
		setUpdateDeleteCompletedJobs(UPDATE_PURGE_COMPLETED);
		setUpdateJobState(UPDATE_JOB_STATE);
		setUpdateJobConfiguration(UPDATE_JOB_CONFIG);
		setUpdateJobProgress(UPDATE_JOB_PROGRESS);
		setUpdateDeleteForUser(UPDATE_DELETE_FOR_USER);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public DatumDeleteJobInfo claimQueuedJob() {
		return selectFirst(queryForClaimQueuedJob, null);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long purgeOldJobs(DateTime olderThanDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(updateDeleteCompletedJobs, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean updateJobState(UserUuidPK id, DatumDeleteJobState desiredState,
			Set<DatumDeleteJobState> expectedStates) {
		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("desiredState", desiredState.getKey());
		if ( expectedStates != null && !expectedStates.isEmpty() ) {
			String[] array = expectedStates.stream().map(s -> String.valueOf(s.getKey()))
					.collect(Collectors.toList()).toArray(new String[expectedStates.size()]);
			params.put("expectedStates", array);
		}
		int count = getSqlSession().update(updateJobState, params);
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean updateJobConfiguration(UserUuidPK id, GeneralNodeDatumFilter configuration) {
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setConfiguration(new DatumFilterCommand(configuration));

		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("configJson", info.getConfigJson());
		params.put("expectedStates",
				new String[] { String.valueOf(DatumDeleteJobState.Queued.getKey()) });
		int count = getSqlSession().update(updateJobConfiguration, params);
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean updateJobProgress(UserUuidPK id, double percentComplete, Long loadedCount) {
		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("progress", percentComplete);
		params.put("loadedCount", loadedCount);
		int count = getSqlSession().update(updateJobProgress, params);
		return (count > 0);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<DatumDeleteJobInfo> findForUser(Long userId, Set<DatumDeleteJobState> states) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		if ( states != null && !states.isEmpty() ) {
			String[] array = states.stream().map(s -> String.valueOf(s.getKey()))
					.collect(Collectors.toList()).toArray(new String[states.size()]);
			params.put("states", array);
		}
		return selectList(queryForUser, params, null, null);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public int deleteForUser(Long userId, Set<UUID> jobIds, Set<DatumDeleteJobState> states) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		if ( jobIds != null && !jobIds.isEmpty() ) {
			String[] array = jobIds.stream().map(id -> id.toString()).collect(Collectors.toList())
					.toArray(new String[jobIds.size()]);
			params.put("ids", array);
		}
		if ( states != null && !states.isEmpty() ) {
			String[] array = states.stream().map(s -> String.valueOf(s.getKey()))
					.collect(Collectors.toList()).toArray(new String[states.size()]);
			params.put("states", array);
		}
		return getSqlSession().update(updateDeleteForUser, params);
	}

	/**
	 * Set the query name for the {@link #claimQueuedJob()} method to use.
	 * 
	 * @param queryForClaimQueuedJob
	 *        the query name; defaults to {@link #QUERY_FOR_CLAIMING_JOB}
	 */
	public void setQueryForClaimQueuedJob(String queryForClaimQueuedJob) {
		this.queryForClaimQueuedJob = queryForClaimQueuedJob;
	}

	/**
	 * Set the statement name for the {@link #purgeOldJobs(DateTime)} method to
	 * use.
	 * 
	 * @param updateDeleteCompletedJobs
	 *        the statement name; defaults to {@link #UPDATE_PURGE_COMPLETED}
	 */
	public void setUpdateDeleteCompletedJobs(String updateDeleteCompletedJobs) {
		this.updateDeleteCompletedJobs = updateDeleteCompletedJobs;
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)} method to use.
	 * 
	 * @param updateJobState
	 *        the statement name; defaults to {@link #UPDATE_JOB_STATE}
	 */
	public void setUpdateJobState(String updateJobState) {
		this.updateJobState = updateJobState;
	}

	/**
	 * Set the statement name for the {@link #findForUser(Long, Set)} method to
	 * use.
	 * 
	 * @param queryForUser
	 *        the statement name; defaults to {@link #QUERY_FOR_USER}
	 */
	public void setQueryForUser(String queryForUser) {
		this.queryForUser = queryForUser;
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobConfigurtation(UserUuidPK, net.solarnetwork.central.datum.imp.domain.Configuration)}
	 * method to use.
	 * 
	 * @param updateJobConfiguration
	 *        the statement name; defaults to {@link #UPDATE_JOB_CONFIG}
	 */
	public void setUpdateJobConfiguration(String updateJobConfiguration) {
		this.updateJobConfiguration = updateJobConfiguration;
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobProgress(UserUuidPK, double)} method to use.
	 * 
	 * @param updateJobProgress
	 *        the statement name; defaults to {@link #UPDATE_JOB_PROGRESS}
	 */
	public void setUpdateJobProgress(String updateJobProgress) {
		this.updateJobProgress = updateJobProgress;
	}

	/**
	 * Set the statement name for the {@link #deleteForUser(Long, Set, Set)}
	 * method to use.
	 * 
	 * @param updateDeleteForUser
	 *        the statement name; defaults to {@link #UPDATE_DELETE_FOR_USER}
	 */
	public void setUpdateDeleteForUser(String updateDeleteForUser) {
		this.updateDeleteForUser = updateDeleteForUser;
	}
}
