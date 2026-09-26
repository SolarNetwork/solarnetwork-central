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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.ObjectRecordId;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;

/**
 * DTO for datum insight overall statistics.
 *
 * @author matt
 * @version 2.2
 * @since 1.30
 */
public class DatumInsightOverallStatistics {

	private final List<AuditDatumRecordCounts> counts;
	private final Integer nodeCount;
	private final Integer sourceCount;
	private final Integer activeSourceCount;
	private final Integer activeNodeCount;
	private final List<AuditDatumRecordCounts> accumulative;

	/**
	 * Construct from a list of counts.
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

		final Set<Long> nodeIds = new HashSet<>(32);
		final Set<String> sourceIds = new HashSet<>(32);

		this.counts = convert(counts);
		for ( AuditDatumRecordCounts record : this.counts ) {
			nodeIds.add(record.getNodeId());
			sourceIds.add(record.getNodeId() + ":" + record.getSourceId());
		}
		this.activeNodeCount = nodeIds.size();
		this.activeSourceCount = sourceIds.size();

		nodeIds.clear();
		sourceIds.clear();
		this.accumulative = convert(accumulative);
		for ( AuditDatumRecordCounts record : this.accumulative ) {
			nodeIds.add(record.getNodeId());
			sourceIds.add(record.getNodeId() + ":" + record.getSourceId());
		}
		this.nodeCount = nodeIds.size();
		this.sourceCount = sourceIds.size();
	}

	private static List<AuditDatumRecordCounts> convert(Iterable<AuditDatumRollup> rollups) {
		return StreamSupport.stream(rollups.spliterator(), false).map(e -> {
			AuditDatumRecordCounts c = new AuditDatumRecordCounts(
					new ObjectRecordId(e.getNodeId(), e.getSourceId(), e.getTimestamp()),
					e.getDatumCount(), e.getDatumHourlyCount(), e.getDatumDailyCount(),
					e.getDatumMonthlyCount());
			if ( e.getDatumPropertyCount() != null ) {
				c.setDatumPropertyPostedCount(e.getDatumPropertyCount()
						+ (e.getDatumPropertyUpdateCount() != null ? e.getDatumPropertyUpdateCount()
								: 0));
			}
			c.setDatumQueryCount(e.getDatumQueryCount());
			return c;
		}).collect(Collectors.toList());
	}

	public Long getAccumulativeTotalDatumCount() {
		return accumulative.stream().filter(c -> c.getDatumCount() != null)
				.mapToLong(AuditDatumRecordCounts::getDatumCount).sum();
	}

	public Long getAccumulativeTotalDatumHourlyCount() {
		return accumulative.stream().filter(c -> c.getDatumHourlyCount() != null)
				.mapToLong(AuditDatumRecordCounts::getDatumHourlyCount).sum();
	}

	public Long getAccumulativeTotalDatumDailyCount() {
		return accumulative.stream().filter(c -> c.getDatumDailyCount() != null)
				.mapToLong(c -> c.getDatumDailyCount().longValue()).sum();
	}

	public Integer getAccumulativeTotalDatumMonthlyCount() {
		return accumulative.stream().filter(c -> c.getDatumMonthlyCount() != null)
				.mapToInt(AuditDatumRecordCounts::getDatumMonthlyCount).sum();
	}

	public Long getAccumulativeTotalDatumTotalCount() {
		return getAccumulativeTotalDatumCount() + getAccumulativeTotalDatumHourlyCount()
				+ getAccumulativeTotalDatumDailyCount() + getAccumulativeTotalDatumMonthlyCount();
	}

	public Integer getNodeCount() {
		return nodeCount;
	}

	public Integer getActiveNodeCount() {
		return activeNodeCount;
	}

	public Integer getSourceCount() {
		return sourceCount;
	}

	public Integer getActiveSourceCount() {
		return activeSourceCount;
	}

	public List<AuditDatumRecordCounts> getCounts() {
		return counts;
	}

	public List<AuditDatumRecordCounts> getAccumulative() {
		return accumulative;
	}

}
