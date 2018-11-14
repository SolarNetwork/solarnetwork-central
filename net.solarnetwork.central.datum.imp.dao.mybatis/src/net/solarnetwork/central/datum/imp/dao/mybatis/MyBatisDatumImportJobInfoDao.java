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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * MyBatis implementation of {@link DatumImportJobInfoDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumImportJobInfoDao extends BaseMyBatisGenericDao<DatumImportJobInfo, UserUuidPK>
		implements DatumImportJobInfoDao {

	/** The default query name used for {@link #claimQueuedJob()}. */
	public static final String QUERY_FOR_CLAIMING_JOB = "get-DatumImportJobInfo-for-claim";

	/**
	 * The {@code DELETE} query name used for {@link #purgeOldJobs(DateTime)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-DatumImportJobInfo-completed";

	/**
	 * The {@code UPDATE} query name used for
	 * {@link #updateJobState(UserUuidPK, DatumImportState, Set)}.
	 */
	public static final String UPDATE_JOB_STATE = "update-DatumImportJobInfo-state";

	/**
	 * The query name used for {@link #findForUser(Long, Set)}.
	 */
	public static final String QUERY_FOR_USER = "find-DatumImportJobInfo-for-user";

	private String queryForClaimQueuedJob;
	private String updateDeleteCompletedJobs;
	private String updateJobState;
	private String queryForUser;

	/**
	 * Constructor.
	 */
	public MyBatisDatumImportJobInfoDao() {
		super(DatumImportJobInfo.class, UserUuidPK.class);
		setQueryForClaimQueuedJob(QUERY_FOR_CLAIMING_JOB);
		setUpdateDeleteCompletedJobs(UPDATE_PURGE_COMPLETED);
		setUpdateJobState(UPDATE_JOB_STATE);
		setQueryForUser(QUERY_FOR_USER);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public DatumImportJobInfo claimQueuedJob() {
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
	public boolean updateJobState(UserUuidPK id, DatumImportState desiredState,
			Set<DatumImportState> expectedStates) {
		Map<String, Object> params = new HashMap<>(2);
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<DatumImportJobInfo> findForUser(Long userId, Set<DatumImportState> states) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		if ( states != null && !states.isEmpty() ) {
			String[] array = states.stream().map(s -> String.valueOf(s.getKey()))
					.collect(Collectors.toList()).toArray(new String[states.size()]);
			params.put("states", array);
		}
		return selectList(queryForUser, params, null, null);
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

}
