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

import static net.solarnetwork.central.datum.v2.support.DatumUtils.criteriaFromFilter;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.captureDouble;
import static org.easymock.EasyMock.captureLong;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.support.TaskExecutorAdapter;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.expire.biz.dao.DaoUserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobStatus;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link DaoUserDatumDeleteBiz} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoUserDatumDeleteBizTests {

	private DatumMaintenanceDao datumDao;
	private UserNodeDao userNodeDao;
	private UserDatumDeleteJobInfoDao jobInfoDao;

	private DaoUserDatumDeleteBiz biz;

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumMaintenanceDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		jobInfoDao = EasyMock.createMock(UserDatumDeleteJobInfoDao.class);

		biz = new DaoUserDatumDeleteBiz(new TaskExecutorAdapter(new CallingThreadExecutorService()),
				userNodeDao, datumDao, jobInfoDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao, userNodeDao, jobInfoDao);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, userNodeDao, jobInfoDao);
	}

	/**
	 * Assert one legacy datum record counts has values that match a
	 * {@link DatumRecordCounts} instance.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	private static void assertDatumRecordCounts(String prefix,
			net.solarnetwork.central.datum.domain.DatumRecordCounts result, DatumRecordCounts expected) {
		assertThat(prefix + " ts", result.getDate(), equalTo(expected.getTimestamp()));
		assertThat(prefix + " datum count", result.getDatumCount(), equalTo(expected.getDatumCount()));
		assertThat(prefix + " datum hourly count", result.getDatumHourlyCount(),
				equalTo(expected.getDatumHourlyCount()));
		assertThat(prefix + " datum daily count", result.getDatumDailyCount(),
				equalTo(expected.getDatumDailyCount()));
		assertThat(prefix + " datum monthly count", result.getDatumMonthlyCount(),
				equalTo(expected.getDatumMonthlyCount()));
	}

	@Test
	public void countDatum() {
		// given
		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		DatumRecordCounts counts = AuditDatumEntity.datumRecordCounts(Instant.now(), 1L, 2L, 3, 4);
		expect(datumDao.countDatumRecords(capture(filterCaptor))).andReturn(counts);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setNodeId(2L);
		net.solarnetwork.central.datum.domain.DatumRecordCounts result = biz.countDatumRecords(filter);

		// then
		assertDatumRecordCounts("Result", result, counts);
		assertThat("Filter passed through", filterCaptor.getValue(),
				equalTo(criteriaFromFilter(filter)));
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

		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		DatumRecordCounts counts = AuditDatumEntity.datumRecordCounts(Instant.now(), 1L, 2L, 3, 4);
		expect(datumDao.countDatumRecords(capture(filterCaptor))).andReturn(counts);

		// when
		replayAll();
		final String[] sourceIds = new String[] { "a", "b" };
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setSourceIds(sourceIds);
		filter.setLocalStartDate(LocalDateTime.of(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2018, 12, 1, 0, 0));
		net.solarnetwork.central.datum.domain.DatumRecordCounts result = biz.countDatumRecords(filter);

		// then
		assertDatumRecordCounts("Result", result, counts);
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
	public void performDelete() throws Exception {
		// given
		final UserUuidPK id = new UserUuidPK(-1L, UUID.randomUUID());

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setNodeId(2L);

		DatumDeleteJobInfo jobInfo = new DatumDeleteJobInfo();
		jobInfo.setId(id);
		jobInfo.setConfiguration(filter);

		expect(jobInfoDao.get(id)).andReturn(jobInfo).anyTimes();

		// allow updating the status as job progresses
		expect(jobInfoDao.store(jobInfo)).andReturn(id).anyTimes();

		// delete in week-based batches
		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		final long count = 123L;
		expect(datumDao.deleteFiltered(capture(filterCaptor))).andReturn(count);

		// when
		replayAll();
		DatumDeleteJobStatus result = biz.performDatumDelete(id);

		// then
		assertThat("Result", result, notNullValue());
		assertThat("Result delete count", result.get().getResultCount(), equalTo(count));
		assertThat("Executed filter same", filterCaptor.getValue(), equalTo(criteriaFromFilter(filter)));
	}

	private static final class FilterCapture extends Capture<ObjectStreamCriteria> {

		private static final long serialVersionUID = 1052142891458580229L;

		private FilterCapture(CaptureType type) {
			super(type);
		}

		@Override
		public void setValue(ObjectStreamCriteria value) {
			// make copy of argument, as code mutates same instance values
			super.setValue(BasicDatumCriteria.copy(value));
		}

	}

	@Test
	public void performDelete_timeBatch() throws Exception {
		// given
		final UserUuidPK id = new UserUuidPK(-1L, UUID.randomUUID());

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setNodeId(2L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 2, 1, 0, 0));

		DatumDeleteJobInfo jobInfo = new DatumDeleteJobInfo();
		jobInfo.setId(id);
		jobInfo.setConfiguration(filter);

		expect(jobInfoDao.get(id)).andReturn(jobInfo).anyTimes();

		// update progress for each batch segment
		Capture<Double> progressCapture = new Capture<>(CaptureType.ALL);
		Capture<Long> resultCountsCapture = new Capture<>(CaptureType.ALL);
		expect(jobInfoDao.updateJobProgress(eq(id), captureDouble(progressCapture),
				captureLong(resultCountsCapture))).andReturn(true).times(5);

		// allow updating the status as job progresses
		expect(jobInfoDao.store(jobInfo)).andReturn(id).anyTimes();

		// delete in week-based batches
		Capture<ObjectStreamCriteria> filterCaptor = new FilterCapture(CaptureType.ALL);
		long count = 0;
		for ( int i = 1; i <= 5; i++ ) {
			expect(datumDao.deleteFiltered(capture(filterCaptor))).andReturn((long) i);
			count += i;
		}

		// when
		replayAll();
		DatumDeleteJobStatus result = biz.performDatumDelete(id);

		// then
		assertThat("Result", result, notNullValue());
		assertThat("Result delete count is sum of batch results", result.get().getResultCount(),
				equalTo(count));

		List<ObjectStreamCriteria> batchFilters = filterCaptor.getValues();

		// first 4 batch periods are exactly 1w, 5th remaining
		LocalDateTime currStartDate = filter.getLocalStartDate();
		Double lastProgressValue = 0.0;
		long accumulatedResultCount = 0L;
		for ( int i = 0; i < 5; i++ ) {
			ObjectStreamCriteria batchFilter = batchFilters.get(i);
			assertThat("User ID preserved " + i, batchFilter.getUserId(), equalTo(filter.getUserId()));
			assertThat("Node ID preserved " + i, batchFilter.getNodeId(), equalTo(filter.getNodeId()));
			assertThat("Batch start date " + i, batchFilter.getLocalStartDate(), equalTo(currStartDate));
			assertThat("Progress incremented " + i, progressCapture.getValues().get(i),
					greaterThan(lastProgressValue));
			accumulatedResultCount += (i + 1);
			assertThat("Result count " + i, resultCountsCapture.getValues().get(i),
					equalTo(accumulatedResultCount));

			LocalDateTime currEndDate = currStartDate.plusDays(7);
			if ( currEndDate.isAfter(filter.getLocalEndDate()) ) {
				currEndDate = filter.getLocalEndDate();
			}
			assertThat("Batch end date " + i, batchFilter.getLocalEndDate(), equalTo(currEndDate));
			currStartDate = currEndDate;
		}
	}

	@Test(expected = AuthorizationException.class)
	public void submitDeleteRequestWithoutUser() {
		// given

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(2L);
		biz.submitDatumDeleteRequest(filter);
	}

	@Test
	public void submitDeleteRequestFillNodeIds() throws Exception {
		// given
		final Long userId = 1L;
		final Long[] userNodeIds = new Long[] { 2L, 3L, 4L };
		expect(userNodeDao.findNodeIdsForUser(userId))
				.andReturn(new LinkedHashSet<>(Arrays.asList(userNodeIds)));

		Capture<DatumDeleteJobInfo> jobInfoCaptor = new Capture<>();
		expect(jobInfoDao.store(capture(jobInfoCaptor))).andAnswer(new IAnswer<UserUuidPK>() {

			@Override
			public UserUuidPK answer() throws Throwable {
				return jobInfoCaptor.getValue().getId();
			}
		});
		expect(jobInfoDao.get(assertWith(new Assertion<UserUuidPK>() {

			@Override
			public void check(UserUuidPK argument) throws Throwable {
				assertThat(argument, equalTo(jobInfoCaptor.getValue().getId()));
			}
		}))).andAnswer(new IAnswer<DatumDeleteJobInfo>() {

			@Override
			public DatumDeleteJobInfo answer() throws Throwable {
				return jobInfoCaptor.getValue();
			}

		});

		// when
		replayAll();
		final String[] sourceIds = new String[] { "a", "b" };
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(1L);
		filter.setSourceIds(sourceIds);
		filter.setLocalStartDate(LocalDateTime.of(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2018, 12, 1, 0, 0));
		DatumDeleteJobInfo result = biz.submitDatumDeleteRequest(filter);

		// then
		assertThat("Result returned", result, equalTo(jobInfoCaptor.getValue()));
		assertThat("Job state", result.getJobState(), equalTo(DatumDeleteJobState.Queued));
		assertThat("Node IDs filled in", result.getConfiguration().getNodeIds(),
				arrayContaining(userNodeIds));
	}

}
