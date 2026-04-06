/* ==================================================================
 * MyBatisDatumImportJobInfoDao.java - 9/11/2018 5:34:08 PM
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

package net.solarnetwork.central.datum.imp.dao.mybatis;

import static java.time.Instant.now;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.Configuration;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;

/**
 * MyBatis implementation of {@link DatumImportJobInfoDao}.
 *
 * @author matt
 * @version 2.1
 */
public class MyBatisDatumImportJobInfoDao extends BaseMyBatisGenericDao<DatumImportJobInfo, UserUuidPK>
		implements DatumImportJobInfoDao {

	/** The default query name used for {@link #claimQueuedJob()}. */
	public static final String QUERY_FOR_CLAIMING_JOB = "get-DatumImportJobInfo-for-claim";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(Instant)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-DatumImportJobInfo-completed";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(Instant)}.
	 */
	public static final String UPDATE_DELETE_FOR_USER = "delete-DatumImportJobInfo-for-user";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_STATE = "update-DatumImportJobInfo-state";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_CONFIG = "update-DatumImportJobInfo-config";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_PROGRESS = "update-DatumImportJobInfo-progress";

	/**
	 * The query name used for {@link #findForUser(Long, Set)}.
	 */
	public static final String QUERY_FOR_USER = "find-DatumImportJobInfo-for-user";

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
	public MyBatisDatumImportJobInfoDao() {
		super(DatumImportJobInfo.class, UserUuidPK.class);
		this.queryForUser = QUERY_FOR_USER;
		this.queryForClaimQueuedJob = QUERY_FOR_CLAIMING_JOB;
		this.updateDeleteCompletedJobs = UPDATE_PURGE_COMPLETED;
		this.updateJobState = UPDATE_JOB_STATE;
		this.updateJobConfiguration = UPDATE_JOB_CONFIG;
		this.updateJobProgress = UPDATE_JOB_PROGRESS;
		this.updateDeleteForUser = UPDATE_DELETE_FOR_USER;
	}

	@Override
	public @Nullable DatumImportJobInfo claimQueuedJob() {
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
	public boolean updateJobState(UserUuidPK id, DatumImportState desiredState,
			@Nullable Set<DatumImportState> expectedStates) {
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
	public boolean updateJobConfiguration(UserUuidPK id, Configuration configuration) {
		DatumImportJobInfo info = new DatumImportJobInfo(id, now());
		info.setConfig(new BasicConfiguration(configuration));

		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("configJson", info.getConfigJson());
		params.put("expectedStates", new String[] { String.valueOf(DatumImportState.Staged.getKey()) });
		int count = getSqlSession().update(updateJobConfiguration, params);
		return (count > 0);
	}

	@Override
	public boolean updateJobProgress(UserUuidPK id, double percentComplete, @Nullable Long loadedCount) {
		Map<String, Object> params = new HashMap<>(3);
		params.put("id", id);
		params.put("progress", percentComplete);
		params.put("loadedCount", loadedCount);
		int count = getSqlSession().update(updateJobProgress, params);
		return (count > 0);
	}

	@Override
	public List<DatumImportJobInfo> findForUser(Long userId, @Nullable Set<DatumImportState> states) {
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
			@Nullable Set<DatumImportState> states) {
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
	 *        the query name; if {@code null} then
	 *        {@link #QUERY_FOR_CLAIMING_JOB} will be used
	 */
	public final void setQueryForClaimQueuedJob(String queryForClaimQueuedJob) {
		this.queryForClaimQueuedJob = (queryForClaimQueuedJob != null ? queryForClaimQueuedJob
				: QUERY_FOR_CLAIMING_JOB);
	}

	/**
	 * Set the statement name for the {@link #purgeOldJobs(Instant)} method to
	 * use.
	 *
	 * @param updateDeleteCompletedJobs
	 *        the statement name; if {@code null} then
	 *        {@link #UPDATE_PURGE_COMPLETED} will be used
	 */
	public final void setUpdateDeleteCompletedJobs(String updateDeleteCompletedJobs) {
		this.updateDeleteCompletedJobs = (updateDeleteCompletedJobs != null ? updateDeleteCompletedJobs
				: UPDATE_PURGE_COMPLETED);
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)} method to use.
	 *
	 * @param updateJobState
	 *        the statement name; if {@code null} then {@link #UPDATE_JOB_STATE}
	 *        will be used
	 */
	public final void setUpdateJobState(String updateJobState) {
		this.updateJobState = (updateJobState != null ? updateJobState : UPDATE_JOB_STATE);
	}

	/**
	 * Set the statement name for the {@link #findForUser(Long, Set)} method to
	 * use.
	 *
	 * @param queryForUser
	 *        the statement name; if {@code null} then {@link #QUERY_FOR_USER}
	 *        will be used
	 */
	public final void setQueryForUser(String queryForUser) {
		this.queryForUser = (queryForUser != null ? queryForUser : QUERY_FOR_USER);
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobConfiguration(UserUuidPK, Configuration)} method to use.
	 *
	 * @param updateJobConfiguration
	 *        the statement name; if {@code null} then
	 *        {@link #UPDATE_JOB_CONFIG} will be used
	 */
	public final void setUpdateJobConfiguration(String updateJobConfiguration) {
		this.updateJobConfiguration = (updateJobConfiguration != null ? updateJobConfiguration
				: UPDATE_JOB_CONFIG);
	}

	/**
	 * Set the statement name for the
	 * {@link #updateJobProgress(UserUuidPK, double, Long)} method to use.
	 *
	 * @param updateJobProgress
	 *        the statement name; if {@code null} then
	 *        {@link #UPDATE_JOB_PROGRESS} will be used
	 */
	public final void setUpdateJobProgress(String updateJobProgress) {
		this.updateJobProgress = (updateJobProgress != null ? updateJobProgress : UPDATE_JOB_PROGRESS);
	}

	/**
	 * Set the statement name for the {@link #deleteForUser(Long, Set, Set)}
	 * method to use.
	 *
	 * @param updateDeleteForUser
	 *        the statement name; if {@code null} then
	 *        {@link #UPDATE_DELETE_FOR_USER} will be used
	 */
	public final void setUpdateDeleteForUser(String updateDeleteForUser) {
		this.updateDeleteForUser = (updateDeleteForUser != null ? updateDeleteForUser
				: UPDATE_DELETE_FOR_USER);
	}

}
