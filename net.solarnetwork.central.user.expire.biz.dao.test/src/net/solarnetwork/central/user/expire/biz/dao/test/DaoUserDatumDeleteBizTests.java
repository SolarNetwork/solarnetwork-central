/* ==================================================================
 * DaoUserDatumDeleteBizTests.java - 24/11/2018 8:11:30 PM
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

package net.solarnetwork.central.user.expire.biz.dao.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.expire.biz.dao.DaoUserDatumDeleteBiz;

/**
 * Test cases for the {@link DaoUserDatumDeleteBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserDatumDeleteBizTests {

	private GeneralNodeDatumDao datumDao;
	private UserNodeDao userNodeDao;

	private DaoUserDatumDeleteBiz biz;

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(GeneralNodeDatumDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		biz = new DaoUserDatumDeleteBiz(new CallingThreadExecutorService(), userNodeDao, datumDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao, userNodeDao);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, userNodeDao);
	}

	@Test
	public void countDatum() {
		// given
		Capture<GeneralNodeDatumFilter> filterCaptor = new Capture<>();
		DatumRecordCounts counts = new DatumRecordCounts();
		expect(datumDao.countDatumRecords(capture(filterCaptor))).andReturn(counts);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setNodeId(2L);
		DatumRecordCounts result = biz.countDatumRecords(filter);

		// then
		assertThat("Result returned", result, sameInstance(counts));
		assertThat("Filter passed through", filterCaptor.getValue(), sameInstance(filter));
	}

	@Test(expected = AuthorizationException.class)
	public void countDatumWithoutUser() {
		// given

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(2L);
		biz.countDatumRecords(filter);
	}

	@Test
	public void countDatumFillNodeIds() {
		// given
		final Long userId = 1L;
		final Long[] userNodeIds = new Long[] { 2L, 3L, 4L };
		expect(userNodeDao.findNodeIdsForUser(userId))
				.andReturn(new LinkedHashSet<>(Arrays.asList(userNodeIds)));

		Capture<GeneralNodeDatumFilter> filterCaptor = new Capture<>();
		DatumRecordCounts counts = new DatumRecordCounts();
		expect(datumDao.countDatumRecords(capture(filterCaptor))).andReturn(counts);

		// when
		replayAll();
		final String[] sourceIds = new String[] { "a", "b" };
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setSourceIds(sourceIds);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2018, 12, 1, 0, 0));
		DatumRecordCounts result = biz.countDatumRecords(filter);

		// then
		assertThat("Result returned", result, sameInstance(counts));
		assertThat("Filter not passed through", filterCaptor.getValue(), not(sameInstance(filter)));
		assertThat("Filter user ID unchanged", filterCaptor.getValue().getUserId(), equalTo(userId));
		assertThat("Filter node IDs populated", filterCaptor.getValue().getNodeIds(),
				arrayContaining(userNodeIds));
		assertThat("Filter source IDs unchanged", filterCaptor.getValue().getSourceIds(),
				arrayContaining(sourceIds));
		assertThat("Filter local start date unchanged", filterCaptor.getValue().getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("Filter local end date unchanged", filterCaptor.getValue().getLocalEndDate(),
				equalTo(filter.getLocalEndDate()));
	}

	@Test
	public void deleteFiltered() throws Exception {
		// given
		Capture<GeneralNodeDatumFilter> filterCaptor = new Capture<>();
		final long count = 123;
		expect(datumDao.deleteFiltered(capture(filterCaptor))).andReturn(count);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setNodeId(2L);
		long result = biz.deleteFiltered(filter).get();

		// then
		assertThat("Result", result, equalTo(count));
		assertThat("Filter passed through", filterCaptor.getValue(), sameInstance(filter));
	}

	@Test(expected = AuthorizationException.class)
	public void deleteFilteredWithoutUser() {
		// given

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(2L);
		biz.deleteFiltered(filter);
	}

	@Test
	public void deleteFilteredFillNodeIds() throws Exception {
		// given
		final Long userId = 1L;
		final Long[] userNodeIds = new Long[] { 2L, 3L, 4L };
		expect(userNodeDao.findNodeIdsForUser(userId))
				.andReturn(new LinkedHashSet<>(Arrays.asList(userNodeIds)));

		Capture<GeneralNodeDatumFilter> filterCaptor = new Capture<>();
		final long count = 234L;
		expect(datumDao.deleteFiltered(capture(filterCaptor))).andReturn(count);

		// when
		replayAll();
		final String[] sourceIds = new String[] { "a", "b" };
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setSourceIds(sourceIds);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2018, 12, 1, 0, 0));
		long result = biz.deleteFiltered(filter).get();

		// then
		assertThat("Result returned", result, equalTo(count));
		assertThat("Filter not passed through", filterCaptor.getValue(), not(sameInstance(filter)));
		assertThat("Filter user ID unchanged", filterCaptor.getValue().getUserId(), equalTo(userId));
		assertThat("Filter node IDs populated", filterCaptor.getValue().getNodeIds(),
				arrayContaining(userNodeIds));
		assertThat("Filter source IDs unchanged", filterCaptor.getValue().getSourceIds(),
				arrayContaining(sourceIds));
		assertThat("Filter local start date unchanged", filterCaptor.getValue().getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("Filter local end date unchanged", filterCaptor.getValue().getLocalEndDate(),
				equalTo(filter.getLocalEndDate()));
	}

}
