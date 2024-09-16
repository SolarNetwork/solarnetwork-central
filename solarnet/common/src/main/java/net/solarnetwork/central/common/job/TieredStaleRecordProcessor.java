/* ==================================================================
 * TieredStaleRecordProcessor.java - 3/07/2018 7:46:22 AM
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

package net.solarnetwork.central.common.job;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Abstract job to process "stale" record tiers.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to accept one or two
 * arguments. The first argument is the configured {@code tierProcessType}. If
 * {@code tierProcessMax} is not {@literal null} then that will be passed as the
 * second argument. The JDBC procedure must return an {@code Integer} result
 * representing the number of rows processed by the call. If the procedure
 * returns zero, the job stops immediately.
 * </p>
 * 
 * @author matt
 * @version 2.1
 * @since 1.6
 */
public abstract class TieredStaleRecordProcessor extends StaleRecordProcessor {

	private final String taskDescription;
	private String tierProcessType = "h";
	private Integer tierProcessMax;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @param taskDescription
	 *        a description of the task to use in log statements
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TieredStaleRecordProcessor(JdbcOperations jdbcOps, String taskDescription) {
		super(jdbcOps);
		this.taskDescription = requireNonNullArgument(taskDescription, "taskDescription");
		setMaximumIterations(1);
		setParallelism(1);
	}

	/**
	 * Execute the stale processing task.
	 * 
	 * @param remainingCount
	 *        the maximum remaining number of rows to process
	 * @return number of rows processed
	 */
	protected abstract int execute(final AtomicInteger remainingCount);

	@Override
	protected int executeJobTask(AtomicInteger remainingIterataions) throws Exception {
		try {
			return execute(remainingIterataions);
		} catch ( CannotAcquireLockException e ) {
			log.warn("Failure acquiring DB lock while processing {} for tier '{}' with call {}",
					taskDescription, tierProcessType, getJdbcCall(), e);
		}
		return 0;
	}

	@Override
	public void run() {
		executeParallelJob(String.format("%s tier '%s'", taskDescription, tierProcessType));
	}

	/**
	 * Get the tier process type.
	 * 
	 * @return the type; defaults to {@literal "h"}
	 */
	public String getTierProcessType() {
		return tierProcessType;
	}

	/**
	 * Set the type of tier data to process.
	 * 
	 * <p>
	 * This is the first parameter passed to the JDBC procedure.
	 * </p>
	 * 
	 * @param tierProcessType
	 *        the type to set
	 */
	public void setTierProcessType(String tierProcessType) {
		this.tierProcessType = tierProcessType;
	}

	/**
	 * Get the maximum tier rows to process per procedure call.
	 * 
	 * @return the maximum row count, or {@literal null} for no explicit limit
	 */
	public Integer getTierProcessMax() {
		return tierProcessMax;
	}

	/**
	 * Set the maximum number of tier rows to process per procedure call.
	 * 
	 * <p>
	 * If this value is not {@literal null}, then it will be passed as the
	 * second parameter passed to the JDBC procedure. When {@literal null} then
	 * the JDBC procedure is expected to take only one argument.
	 * </p>
	 * 
	 * @param tierProcessMax
	 *        the maximum number of rows, or {@literal null} for no explicit
	 *        limit; default is {@literal null}
	 */
	public void setTierProcessMax(Integer tierProcessMax) {
		this.tierProcessMax = tierProcessMax;
	}

}
