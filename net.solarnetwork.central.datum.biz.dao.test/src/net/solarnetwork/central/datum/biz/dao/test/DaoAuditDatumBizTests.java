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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.daum.biz.dao.DaoAuditDatumBiz;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Test cases for the {@link DaoAuditDatumBiz} class.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class DaoAuditDatumBizTests {

	private GeneralNodeDatumDao datumDao;

	private DaoAuditDatumBiz biz;

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(GeneralNodeDatumDao.class);

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
		// given
		List<AuditDatumRecordCounts> counts = new ArrayList<AuditDatumRecordCounts>();
		BasicFilterResults<AuditDatumRecordCounts> filterResults = new BasicFilterResults<AuditDatumRecordCounts>(
				counts);
		Capture<AggregateGeneralNodeDatumFilter> filterCaptor = new Capture<AggregateGeneralNodeDatumFilter>();
		expect(datumDao.findAuditRecordCountsFiltered(capture(filterCaptor),
				EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
				EasyMock.<Integer> isNull())).andReturn(filterResults);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		FilterResults<AuditDatumRecordCounts> results = biz.findFilteredAuditRecordCounts(filter, null,
				null, null);

		// then
		assertThat("Filter passed to DAO", filterCaptor.getValue(),
				sameInstance((GeneralNodeDatumFilter) filter));
		assertThat("Results come from DAO", results,
				sameInstance((FilterResults<AuditDatumRecordCounts>) filterResults));
	}

}
