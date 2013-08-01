/* ==================================================================
 * StaleDatumProcessor.java - Aug 1, 2013 4:27:13 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" reporting aggregate data.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>maximumRowCount</dt>
 * <dd>The maximum number of rows to process, as returned by the stored
 * procedure. Defaults to <b>5</b>.</dd>
 * 
 * <dt>maximumWaitMs</dt>
 * <dd>The maximum time, in milliseconds, to allow for the job to execute before
 * it is considered a failed job. Defaults to <b>10 minutes</b>.</dd>
 * 
 * <dt>jdbcCall</dt>
 * <dd>The stored procedure to call. It must return a single integer result.</dd>
 * 
 * <dt>jobId</dt>
 * <dd>The unique ID of the job to schedule.</dd>
 * 
 * <dt>jobTopic</dt>
 * <dd>The {@link Event} topic to use for this job.</dd>
 * 
 * <dt>jobGroup</dt>
 * <dd>The job group to use. Defaults to <b>Datum</b>.</dd>
 * 
 * <dt>jobCron</dt>
 * <dd>The job cron expression to use for scheduling this job. Defaults to
 * <code>0 0/1 * * * ?</code> (once per minute)</dd>.
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class StaleDatumProcessor extends JobSupport {

	private final JdbcOperations jdbcOps;
	private int maximumRowCount = 5;
	private long maximumWaitMs = 10L * 60L * 1000L;
	private String jdbcCall;
	private String jobId;
	private String jobTopic;
	private String jobGroup = "Datum";
	private String jobCron = "0 0/1 * * * ?";

	/**
	 * Construct with EventAdmin.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 */
	public StaleDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin);
		this.jdbcOps = jdbcOps;
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		int i = 0;
		int resultCount = 0;
		do {
			resultCount = jdbcOps.execute(new CallableStatementCreator() {

				@Override
				public CallableStatement createCallableStatement(Connection con) throws SQLException {
					CallableStatement call = con.prepareCall(jdbcCall);
					call.registerOutParameter(1, Types.INTEGER);
					return call;
				}
			}, new CallableStatementCallback<Integer>() {

				@Override
				public Integer doInCallableStatement(CallableStatement cs) throws SQLException,
						DataAccessException {
					cs.execute();
					return cs.getInt(1);
				}
			});
			i += resultCount;
		} while ( i < maximumRowCount && resultCount > 0 );
		return true;
	}

	@Override
	protected void schedulerReady(Event event) throws Exception {
		// schedule our job to run!
		Map<String, Object> props = new HashMap<String, Object>(5);
		props.put(SchedulerConstants.JOB_ID, jobId);
		props.put(SchedulerConstants.JOB_CRON_EXPRESSION, jobCron);
		props.put(SchedulerConstants.JOB_GROUP, jobGroup);
		props.put(SchedulerConstants.JOB_MAX_WAIT, maximumWaitMs);
		props.put(SchedulerConstants.JOB_TOPIC, jobTopic);

		Event e = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, props);
		getEventAdmin().postEvent(e);
	}

	public void setMaximumRowCount(int maximumRowCount) {
		this.maximumRowCount = maximumRowCount;
	}

	public void setJdbcCall(String jdbcCall) {
		this.jdbcCall = jdbcCall;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public void setJobTopic(String jobTopic) {
		this.jobTopic = jobTopic;
	}

	public void setMaximumWaitMs(long maximumWaitMs) {
		this.maximumWaitMs = maximumWaitMs;
	}

	public void setJobCron(String jobCron) {
		this.jobCron = jobCron;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

}
