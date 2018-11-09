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
import java.util.Map;
import org.joda.time.DateTime;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
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
	 * The {@code DELETE} query name used for
	 * {@link #purgeCompletedJobs(DateTime)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-DatumImportJobInfo-completed";

	private String queryForClaimQueuedJob;
	private String updateDeleteCompletedJobs;

	/**
	 * Constructor.
	 */
	public MyBatisDatumImportJobInfoDao() {
		super(DatumImportJobInfo.class, UserUuidPK.class);
		setQueryForClaimQueuedJob(QUERY_FOR_CLAIMING_JOB);
		setUpdateDeleteCompletedJobs(UPDATE_PURGE_COMPLETED);
	}

	@Override
	public DatumImportJobInfo claimQueuedJob() {
		return selectFirst(queryForClaimQueuedJob, null);
	}

	@Override
	public long purgeCompletedJobs(DateTime olderThanDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(updateDeleteCompletedJobs, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
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
	 * Set the statement name for the {@link #purgeCompletedJobs(DateTime)}
	 * method to use.
	 * 
	 * @param updateDeleteCompletedJobs
	 *        the statement name; defaults to {@link #UPDATE_PURGE_COMPLETED}
	 */
	public void setUpdateDeleteCompletedJobs(String updateDeleteCompletedJobs) {
		this.updateDeleteCompletedJobs = updateDeleteCompletedJobs;
	}

}
