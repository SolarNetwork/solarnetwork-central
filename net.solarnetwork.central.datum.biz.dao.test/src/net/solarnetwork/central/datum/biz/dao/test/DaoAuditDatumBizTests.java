/* ==================================================================
 * DaoAuditDatumBizTests.java - 12/07/2018 5:30:27 PM
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

package net.solarnetwork.central.datum.biz.dao.test;

import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup.accumulativeAuditDatumRollup;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.daum.biz.dao.DaoAuditDatumBiz;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link DaoAuditDatumBiz} class.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class DaoAuditDatumBizTests {

	private AuditDatumDao datumDao;

	private DaoAuditDatumBiz biz;

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(AuditDatumDao.class);

		biz = new DaoAuditDatumBiz(datumDao);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private void replayAll() {
		EasyMock.replay(datumDao);
	}

	private void verifyAll() {
		EasyMock.verify(datumDao);
	}

	@Test
	public void findAuditDatumRecordCounts() {
		// GIVEN
		List<AuditDatumRollup> counts = new ArrayList<AuditDatumRollup>();
		counts.add(accumulativeAuditDatumRollup(1L, "a", Instant.now(), 1L, 2L, 3, 4));
		FilterResults<AuditDatumRollup, DatumPK> filterResults = new net.solarnetwork.dao.BasicFilterResults<>(
				counts, 1L, 0, 1);
		Capture<AuditDatumCriteria> filterCaptor = new Capture<AuditDatumCriteria>();
		expect(datumDao.findAuditDatumFiltered(capture(filterCaptor))).andReturn(filterResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.All });
		filter.setStartDate(new org.joda.time.DateTime().dayOfYear().roundFloorCopy());
		filter.setEndDate(filter.getStartDate().plusDays(1));
		filter.setLocalStartDate(new org.joda.time.LocalDateTime().dayOfYear().roundFloorCopy());
		filter.setLocalEndDate(filter.getLocalStartDate().plusDays(1));

		List<net.solarnetwork.central.domain.SortDescriptor> sortDescriptors = Arrays
				.asList(new net.solarnetwork.central.support.SimpleSortDescriptor("foo"));
		net.solarnetwork.central.domain.FilterResults<AuditDatumRecordCounts> results = biz
				.findFilteredAuditRecordCounts(filter, sortDescriptors, 0, 1);

		// THEN
		AuditDatumCriteria criteria = filterCaptor.getValue();
		assertThat("Filter converted to criteria", criteria, notNullValue());
		assertThat("Filter node IDs retained", criteria.getNodeIds(),
				arrayContaining(filter.getNodeIds()));
		assertThat("Filter source IDs retained", criteria.getSourceIds(),
				arrayContaining(filter.getSourceIds()));
		assertThat("Filter user IDs retained", criteria.getUserIds(),
				arrayContaining(filter.getUserIds()));
		assertThat("Filter rollups retained", criteria.getDatumRollupTypes(),
				arrayContaining(filter.getDatumRollupTypes()));
		assertThat("Filter start date converted", criteria.getStartDate(),
				equalTo(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()));
		assertThat("Filter end date converted", criteria.getEndDate(),
				equalTo(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant()));
		assertThat("Filter local start date converted", criteria.getLocalStartDate(),
				equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)));
		assertThat("Filter local end date converted", criteria.getLocalEndDate(),
				equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1)));

		assertThat("Result total count", results.getTotalResults(),
				equalTo(filterResults.getTotalResults()));
		assertThat("Result returned count", results.getReturnedResultCount(),
				equalTo(filterResults.getReturnedResultCount()));
		assertThat("Result offset", results.getStartingOffset(),
				equalTo(filterResults.getStartingOffset()));

		int i = 0;
		for ( AuditDatumRecordCounts c : results ) {
			AuditDatumRollup r = counts.get(i);
			assertThat("Rollup node retrained", c.getNodeId(), equalTo(r.getNodeId()));
			assertThat("Rollup source retrained", c.getSourceId(), equalTo(r.getSourceId()));
			assertThat("Rollup timestamp converted", c.getCreated(),
					equalTo(new DateTime(r.getTimestamp().toEpochMilli())));
			assertThat("Rollup datum count", c.getDatumCount(), equalTo(r.getDatumCount()));
			assertThat("Rollup datum hour count", c.getDatumHourlyCount(),
					equalTo(r.getDatumHourlyCount()));
			assertThat("Rollup datum day count", c.getDatumDailyCount(),
					equalTo(r.getDatumDailyCount()));
			assertThat("Rollup datum month count", c.getDatumMonthlyCount(),
					equalTo(r.getDatumMonthlyCount()));
		}
	}

}
