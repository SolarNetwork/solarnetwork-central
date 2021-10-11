/* ==================================================================
 * DatumInsightOverallStatistics.java - 13/07/2018 11:36:43 AM
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

package net.solarnetwork.central.reg.web.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;

/**
 * DTO for datum insight overall statistics.
 * 
 * @author matt
 * @version 2.0
 * @since 1.30
 */
public class DatumInsightOverallStatistics {

	private List<AuditDatumRecordCounts> counts = Collections.emptyList();
	private Integer nodeCount;
	private Integer sourceCount;
	private Integer activeSourceCount;
	private Integer activeNodeCount;
	private List<AuditDatumRecordCounts> accumulative = Collections.emptyList();

	/**
	 * Default constructor.
	 */
	public DatumInsightOverallStatistics() {
		super();
	}

	/**
	 * Construct from a list of counts.
	 * 
	 * <p>
	 * The {@link #populateStatsFromCounts(Iterable)} method will be invoked
	 * with the provided {@code counts} value, and {@link #setCounts(List)} will
	 * be invoked with a new list created from {@code counts}. Similarly
	 * {@link #setAccumulative(List)} will be invoked with a new list created
	 * from {@code accumulative} and
	 * {@link #populateStatsFromAccumulativeCounts(Iterable)} will be invoked.
	 * </p>
	 * 
	 * @param counts
	 *        the counts
	 * @param accumulative
	 *        the accumulative counts
	 * @since 1.1
	 */
	public DatumInsightOverallStatistics(Iterable<AuditDatumRollup> counts,
			Iterable<AuditDatumRollup> accumulative) {
		super();
		setCounts(convert(counts));
		populateStatsFromCounts(this.counts);
		setAccumulative(convert(accumulative));
		populateStatsFromAccumulativeCounts(this.accumulative);
	}

	private static List<AuditDatumRecordCounts> convert(Iterable<AuditDatumRollup> rollups) {
		return StreamSupport.stream(rollups.spliterator(), false).map(e -> {
			AuditDatumRecordCounts c = new AuditDatumRecordCounts(e.getNodeId(), e.getSourceId(),
					e.getDatumCount(), e.getDatumHourlyCount(), e.getDatumDailyCount(),
					e.getDatumMonthlyCount());
			if ( e.getTimestamp() != null ) {
				c.setCreated(e.getTimestamp());
			}
			return c;
		}).collect(Collectors.toList());
	}

	/**
	 * Extract statistics from a list of counts and set them on this object.
	 * 
	 * <p>
	 * This will set the {@code activeNodeCount} and {@code activeSourceCount}
	 * properties.
	 * </p>
	 * 
	 * @param counts
	 *        the counts to extract statistics from
	 */
	public void populateStatsFromCounts(Iterable<AuditDatumRecordCounts> counts) {
		Set<Long> nodeIds = new HashSet<>(32);
		Set<String> sourceIds = new HashSet<>(32);
		for ( AuditDatumRecordCounts record : counts ) {
			nodeIds.add(record.getNodeId());
			sourceIds.add(record.getNodeId() + ":" + record.getSourceId());
		}
		setActiveNodeCount(nodeIds.size());
		setActiveSourceCount(sourceIds.size());
	}

	/**
	 * Extract statistics from a list of accumulative counts and set them on
	 * this object.
	 * 
	 * <p>
	 * This will set the {@code nodeCount} and {@code sourceCount} properties.
	 * </p>
	 * 
	 * @param counts
	 *        the counts to extract statistics from
	 */
	public void populateStatsFromAccumulativeCounts(Iterable<AuditDatumRecordCounts> counts) {
		Set<Long> nodeIds = new HashSet<>(32);
		Set<String> sourceIds = new HashSet<>(32);
		for ( AuditDatumRecordCounts record : counts ) {
			nodeIds.add(record.getNodeId());
			sourceIds.add(record.getNodeId() + ":" + record.getSourceId());
		}
		setNodeCount(nodeIds.size());
		setSourceCount(sourceIds.size());
	}

	public Long getAccumulativeTotalDatumCount() {
		return accumulative.stream().filter(c -> c.getDatumCount() != null)
				.mapToLong(c -> c.getDatumCount()).sum();
	}

	public Long getAccumulativeTotalDatumHourlyCount() {
		return accumulative.stream().filter(c -> c.getDatumHourlyCount() != null)
				.mapToLong(c -> c.getDatumHourlyCount()).sum();
	}

	public Long getAccumulativeTotalDatumDailyCount() {
		return accumulative.stream().filter(c -> c.getDatumDailyCount() != null)
				.mapToLong(c -> c.getDatumDailyCount().longValue()).sum();
	}

	public Integer getAccumulativeTotalDatumMonthlyCount() {
		return accumulative.stream().filter(c -> c.getDatumMonthlyCount() != null)
				.mapToInt(c -> c.getDatumMonthlyCount()).sum();
	}

	public Long getAccumulativeTotalDatumTotalCount() {
		return getAccumulativeTotalDatumCount() + getAccumulativeTotalDatumHourlyCount()
				+ getAccumulativeTotalDatumDailyCount() + getAccumulativeTotalDatumMonthlyCount();
	}

	public Integer getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(Integer nodeCount) {
		this.nodeCount = nodeCount;
	}

	public Integer getActiveNodeCount() {
		return activeNodeCount;
	}

	public Integer getSourceCount() {
		return sourceCount;
	}

	public void setSourceCount(Integer sourceCount) {
		this.sourceCount = sourceCount;
	}

	public void setActiveNodeCount(Integer activeNodeCount) {
		this.activeNodeCount = activeNodeCount;
	}

	public Integer getActiveSourceCount() {
		return activeSourceCount;
	}

	public void setActiveSourceCount(Integer activeSourceCount) {
		this.activeSourceCount = activeSourceCount;
	}

	public List<AuditDatumRecordCounts> getCounts() {
		return counts;
	}

	public void setCounts(List<AuditDatumRecordCounts> counts) {
		if ( counts == null ) {
			counts = Collections.emptyList();
		}
		this.counts = counts;
	}

	public List<AuditDatumRecordCounts> getAccumulative() {
		return accumulative;
	}

	public void setAccumulative(List<AuditDatumRecordCounts> accumulative) {
		if ( accumulative == null ) {
			accumulative = Collections.emptyList();
		}
		this.accumulative = accumulative;
	}

}
