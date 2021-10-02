/* ==================================================================
 * DaoAuditDatumBiz.java - 12/07/2018 5:25:10 PM
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

package net.solarnetwork.central.daum.biz.dao;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO based implementation of {@link AuditDatumBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoAuditDatumBiz implements AuditDatumBiz {

	private final AuditDatumDao auditDatumDao;

	/**
	 * Constructor.
	 * 
	 * @param auditDatumDao
	 *        the DAO to use
	 */
	public DaoAuditDatumBiz(AuditDatumDao auditDatumDao) {
		super();
		this.auditDatumDao = auditDatumDao;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAuditDatumFiltered(AuditDatumCriteria filter) {
		return auditDatumDao.findAuditDatumFiltered(filter);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAccumulativeAuditDatumFiltered(
			AuditDatumCriteria filter) {
		return auditDatumDao.findAccumulativeAuditDatumFiltered(filter);
	}

	private static AuditDatumCriteria convertFilter(AggregateGeneralNodeDatumFilter legacy,
			List<net.solarnetwork.central.domain.SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		BasicDatumCriteria result = new BasicDatumCriteria();
		result.setAggregation(legacy.getAggregation());
		result.setDatumRollupTypes(legacy.getDatumRollupTypes());
		result.setMostRecent(legacy.isMostRecent());
		result.setWithoutTotalResultsCount(legacy.isWithoutTotalResultsCount());
		result.setNodeIds(legacy.getNodeIds());
		result.setSourceIds(legacy.getSourceIds());
		result.setUserIds(legacy.getUserIds());
		result.setStartDate(legacy.getStartDate());
		result.setEndDate(legacy.getEndDate());
		result.setLocalStartDate(legacy.getLocalStartDate());
		result.setLocalEndDate(legacy.getLocalEndDate());

		if ( sortDescriptors != null && !sortDescriptors.isEmpty() ) {
			List<SortDescriptor> sorts = sortDescriptors.stream().map(e -> {
				return new SimpleSortDescriptor(e.getSortKey(), e.isDescending());
			}).collect(Collectors.toList());
			result.setSorts(sorts);
		}

		result.setOffset(offset);
		result.setMax(max);

		return result;
	}

	private static net.solarnetwork.central.domain.FilterResults<AuditDatumRecordCounts> convertResults(
			FilterResults<AuditDatumRollup, DatumPK> results) {
		List<AuditDatumRecordCounts> counts = StreamSupport.stream(results.spliterator(), false)
				.map(e -> {
					AuditDatumRecordCounts c = new AuditDatumRecordCounts(e.getNodeId(), e.getSourceId(),
							e.getDatumCount(), e.getDatumHourlyCount(), e.getDatumDailyCount(),
							e.getDatumMonthlyCount());
					if ( e.getTimestamp() != null ) {
						c.setCreated(e.getTimestamp());
					}
					return c;
				}).collect(Collectors.toList());
		return new BasicFilterResults<>(counts, results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public net.solarnetwork.central.domain.FilterResults<AuditDatumRecordCounts> findFilteredAuditRecordCounts(
			AggregateGeneralNodeDatumFilter filter,
			List<net.solarnetwork.central.domain.SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return convertResults(
				findAuditDatumFiltered(convertFilter(filter, sortDescriptors, offset, max)));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public net.solarnetwork.central.domain.FilterResults<AuditDatumRecordCounts> findFilteredAccumulativeAuditRecordCounts(
			AggregateGeneralNodeDatumFilter filter,
			List<net.solarnetwork.central.domain.SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return convertResults(
				findAccumulativeAuditDatumFiltered(convertFilter(filter, sortDescriptors, offset, max)));
	}

}
