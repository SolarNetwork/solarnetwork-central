/* ==================================================================
 * MigrationResult.java - Nov 23, 2013 2:09:15 PM
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

package net.solarnetwork.central.cassandra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import net.solarnetwork.util.StringUtils;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Migration task result.
 * 
 * @author matt
 * @version 1.0
 */
public class MigrationResult {

	private static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder().appendDays()
			.appendSuffix(" day ", "days").minimumPrintedDigits(2).appendHours().appendSeparator(":")
			.printZeroAlways().appendMinutes().appendSeparator(":").appendSecondsWithOptionalMillis()
			.toFormatter();

	private final String taskName;
	private final Map<String, Object> taskProperties;
	private boolean success;
	private Long processedCount;
	private final long start;
	private long duration = 0;
	private final List<Future<MigrationResult>> subtasks = new ArrayList<Future<MigrationResult>>(5);

	public MigrationResult(String taskName) {
		super();
		this.taskName = taskName;
		start = System.currentTimeMillis();
		taskProperties = new LinkedHashMap<String, Object>(5);
	}

	public void finished() {
		duration = System.currentTimeMillis() - start;
	}

	public void addSubtask(Future<MigrationResult> subtask) {
		subtasks.add(subtask);
	}

	public String getStatusMessage() {
		Period p = new Period(duration);
		StringBuilder buf = new StringBuilder();
		final double recordsPerSecond = (processedCount == null || duration == 0 ? 0 : processedCount
				.doubleValue() / (duration / 1000.0));
		buf.append("Task ").append(getTaskName()).append(" ");
		if ( taskProperties != null && taskProperties.size() > 0 ) {
			buf.append("(").append(StringUtils.delimitedStringFromMap(taskProperties)).append(") ");
		}
		buf.append(isSuccess() ? "succeeded" : "failed").append(", processing ")
				.append(getProcessedCount()).append(" records in ").append(PERIOD_FORMATTER.print(p))
				.append(" (").append(String.format("%.1f", recordsPerSecond)).append("/s)");
		return buf.toString();

	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Long getProcessedCount() {
		return processedCount;
	}

	public void setProcessedCount(Long processedCount) {
		this.processedCount = processedCount;
	}

	public String getTaskName() {
		return taskName;
	}

	public long getStart() {
		return start;
	}

	public Map<String, Object> getTaskProperties() {
		return taskProperties;
	}

	public List<Future<MigrationResult>> getSubtasks() {
		return subtasks;
	}

}
