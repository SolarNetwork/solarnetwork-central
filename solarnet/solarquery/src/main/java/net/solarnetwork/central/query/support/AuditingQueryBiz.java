/* ==================================================================
 * AuditingQueryBiz.java - 14/02/2018 9:58:59 AM
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

package net.solarnetwork.central.query.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Period;
import java.util.List;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.QueryBiz;

/**
 * {@link QueryBiz} implementation that audits query events using a
 * {@link QueryAuditor}.
 * 
 * @author matt
 * @version 2.0
 */
public class AuditingQueryBiz extends DelegatingQueryBiz {

	private final QueryAuditor auditor;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 * @param queryAuditor
	 *        the query auditor service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AuditingQueryBiz(QueryBiz delegate, QueryAuditor queryAuditor) {
		super(delegate);
		this.auditor = requireNonNullArgument(queryAuditor, "queryAuditor");
	}

	@Override
	public FilterResults<GeneralNodeDatumFilterMatch> findFilteredGeneralNodeDatum(
			GeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		auditor.resetCurrentAuditResults();
		FilterResults<GeneralNodeDatumFilterMatch> results = super.findFilteredGeneralNodeDatum(filter,
				sortDescriptors, offset, max);
		auditor.auditNodeDatumFilterResults(filter, results);
		return results;
	}

	@Override
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateGeneralNodeDatum(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		auditor.resetCurrentAuditResults();
		FilterResults<ReportingGeneralNodeDatumMatch> results = super.findFilteredAggregateGeneralNodeDatum(
				filter, sortDescriptors, offset, max);
		auditor.auditNodeDatumFilterResults(filter, results);
		return results;
	}

	@Override
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredReading(
			GeneralNodeDatumFilter filter, DatumReadingType readingType, Period tolerance) {
		auditor.resetCurrentAuditResults();
		FilterResults<ReportingGeneralNodeDatumMatch> results = super.findFilteredReading(filter,
				readingType, tolerance);
		auditor.auditNodeDatumFilterResults(filter, results);
		return results;
	}

	@Override
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateReading(
			AggregateGeneralNodeDatumFilter filter, DatumReadingType readingType, Period tolerance,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		auditor.resetCurrentAuditResults();
		FilterResults<ReportingGeneralNodeDatumMatch> results = super.findFilteredAggregateReading(
				filter, readingType, tolerance, sortDescriptors, offset, max);
		auditor.auditNodeDatumFilterResults(filter, results);
		return results;
	}

	@Override
	public void findFilteredStreamDatum(StreamDatumFilter filter,
			StreamDatumFilteredResultsProcessor processor, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) throws IOException {
		auditor.resetCurrentAuditResults();
		StreamDatumFilteredResultsProcessor proxy = new AuditingStreamDatumFilterdResultsProcessor(
				processor, auditor);
		super.findFilteredStreamDatum(filter, proxy, sortDescriptors, offset, max);
	}

}
