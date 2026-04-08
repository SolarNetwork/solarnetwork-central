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

package net.solarnetwork.central.user.datum.expire.dao.mybatis;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.user.datum.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.datum.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.datum.expire.domain.DatumDeleteJobState;

/**
 * MyBatis implementation of {@link UserDatumDeleteJobInfoDao}.
 *
 * @author matt
 * @version 1.1
 */
public class MyBatisUserDatumDeleteJobInfoDao extends
		BaseMyBatisGenericDao<DatumDeleteJobInfo, UserUuidPK> implements UserDatumDeleteJobInfoDao {

	/** The default query name used for {@link #claimQueuedJob()}. */
	public static final String QUERY_FOR_CLAIMING_JOB = "get-DatumDeleteJobInfo-for-claim";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(Instant)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-DatumDeleteJobInfo-completed";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(Instant)}.
	 */
	public static final String UPDATE_DELETE_FOR_USER = "delete-DatumDeleteJobInfo-for-user";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumDeleteJobState, Set)}.
	 */
	public static final String UPDATE_JOB_STATE = "update-DatumDeleteJobInfo-state";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumDeleteJobState, Set)}.
	 */
	public static final String UPDATE_JOB_CONFIG = "update-DatumDeleteJobInfo-config";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumDeleteJobState, Set)}.
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
		this.queryForUser = QUERY_FOR_USER;
		this.queryForClaimQueuedJob = QUERY_FOR_CLAIMING_JOB;
		this.updateDeleteCompletedJobs = UPDATE_PURGE_COMPLETED;
		this.updateJobState = UPDATE_JOB_STATE;
		this.updateJobConfiguration = UPDATE_JOB_CONFIG;
		this.updateJobProgress = UPDATE_JOB_PROGRESS;
		this.updateDeleteForUser = UPDATE_DELETE_FOR_USER;
	}

	@Override
	public @Nullable DatumDeleteJobInfo claimQueuedJob() {
		return selectFirst(queryForClaimQueuedJob, null);
	}

	@Override
	public long purgeOldJobs(Instant olderThanDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(updateDeleteCompletedJobs, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result);
	}

	@Override
	public boolean updateJobState(UserUuidPK id, DatumDeleteJobState desiredState,
			@Nullable Set<DatumDeleteJobState> expectedStates) {
		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("desiredState", desiredState.getKey());
		if ( expectedStates != null && !expectedStates.isEmpty() ) {
			String[] array = expectedStates.stream().map(s -> String.valueOf(s.getKey()))
					.toArray(String[]::new);
			params.put("expectedStates", array);
		}
		int count = getSqlSession().update(updateJobState, params);
		return (count > 0);
	}

	@Override
	public boolean updateJobConfiguration(UserUuidPK id, GeneralNodeDatumFilter configuration) {
		var info = new DatumDeleteJobInfo(id);
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
	public boolean updateJobProgress(UserUuidPK id, double percentComplete, @Nullable Long loadedCount) {
		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("progress", percentComplete);
		if ( loadedCount != null ) {
			params.put("loadedCount", loadedCount);
		}
		int count = getSqlSession().update(updateJobProgress, params);
		return (count > 0);
	}

	@Override
	public List<DatumDeleteJobInfo> findForUser(Long userId, @Nullable Set<DatumDeleteJobState> states) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		if ( states != null && !states.isEmpty() ) {
			String[] array = states.stream().map(s -> String.valueOf(s.getKey())).toArray(String[]::new);
			params.put("states", array);
		}
		return selectList(queryForUser, params, null, null);
	}

	@Override
	public int deleteForUser(Long userId, @Nullable Set<UUID> jobIds,
			@Nullable Set<DatumDeleteJobState> states) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		if ( jobIds != null && !jobIds.isEmpty() ) {
			String[] array = jobIds.stream().map(UUID::toString).toArray(String[]::new);
			params.put("ids", array);
		}
		if ( states != null && !states.isEmpty() ) {
			String[] array = states.stream().map(s -> String.valueOf(s.getKey())).toArray(String[]::new);
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
		this.queryForClaimQueuedJob = (queryForClaimQueuedJob != null ? queryForClaimQueuedJob
				: QUERY_FOR_CLAIMING_JOB);
	}

	/**
	 * Set the statement name for the {@link #purgeOldJobs(Instant)} method to
	 * use.
	 *
	 * @param updateDeleteCompletedJobs
	 *        the statement name; defaults to {@link #UPDATE_PURGE_COMPLETED}
	 */
	public void setUpdateDeleteCompletedJobs(String updateDeleteCompletedJobs) {
		this.updateDeleteCompletedJobs = (updateDeleteCompletedJobs != null ? updateDeleteCompletedJobs
				: UPDATE_PURGE_COMPLETED);
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobState(UserUuidPK, DatumDeleteJobState, Set)} method to
	 * use.
	 *
	 * @param updateJobState
	 *        the statement name; defaults to {@link #UPDATE_JOB_STATE}
	 */
	public void setUpdateJobState(String updateJobState) {
		this.updateJobState = (updateJobState != null ? updateJobState : UPDATE_JOB_STATE);
	}

	/**
	 * Set the statement name for the {@link #findForUser(Long, Set)} method to
	 * use.
	 *
	 * @param queryForUser
	 *        the statement name; defaults to {@link #QUERY_FOR_USER}
	 */
	public void setQueryForUser(String queryForUser) {
		this.queryForUser = (queryForUser != null ? queryForUser : QUERY_FOR_USER);
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobConfiguration(UserUuidPK, GeneralNodeDatumFilter)}
	 * method to use.
	 *
	 * @param updateJobConfiguration
	 *        the statement name; defaults to {@link #UPDATE_JOB_CONFIG}
	 */
	public void setUpdateJobConfiguration(String updateJobConfiguration) {
		this.updateJobConfiguration = (updateJobConfiguration != null ? updateJobConfiguration
				: UPDATE_JOB_CONFIG);
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobProgress(UserUuidPK, double, Long)} method to use.
	 *
	 * @param updateJobProgress
	 *        the statement name; defaults to {@link #UPDATE_JOB_PROGRESS}
	 */
	public void setUpdateJobProgress(String updateJobProgress) {
		this.updateJobProgress = (updateJobProgress != null ? updateJobProgress : UPDATE_JOB_PROGRESS);
	}

	/**
	 * Set the statement name for the {@link #deleteForUser(Long, Set, Set)}
	 * method to use.
	 *
	 * @param updateDeleteForUser
	 *        the statement name; defaults to {@link #UPDATE_DELETE_FOR_USER}
	 */
	public void setUpdateDeleteForUser(String updateDeleteForUser) {
		this.updateDeleteForUser = (updateDeleteForUser != null ? updateDeleteForUser
				: UPDATE_DELETE_FOR_USER);
	}
}
