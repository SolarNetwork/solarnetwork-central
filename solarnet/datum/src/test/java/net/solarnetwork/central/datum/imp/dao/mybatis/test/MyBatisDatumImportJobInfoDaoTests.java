/* ==================================================================
 * MyBatisDatumImportJobInfoDaoTests.java - 10/11/2018 7:17:14 AM
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

package net.solarnetwork.central.datum.imp.dao.mybatis.test;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.imp.dao.mybatis.MyBatisDatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;

/**
 * Test cases for the {@link MyBatisDatumImportJobInfoDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisDatumImportJobInfoDaoTests extends AbstractMyBatisDatumImportDaoTestSupport {

	private static final String TEST_NAME = "Test Import";

	private MyBatisDatumImportJobInfoDao dao;

	private Long userId;
	private String tokenId;
	private DatumImportJobInfo info;
	private TransactionTemplate txTemplate;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisDatumImportJobInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		this.userId = storeNewUser(TEST_EMAIL);

		info = null;

		txTemplate = new TransactionTemplate(txManager);
	}

	private BasicConfiguration createNewConfig() {
		BasicConfiguration conf = new BasicConfiguration();
		conf.setName(TEST_NAME);
		conf.setStage(true);

		BasicInputConfiguration inputConfig = new BasicInputConfiguration();
		inputConfig.setName(TEST_NAME);
		inputConfig.setServiceIdentifier("foo.bar");
		inputConfig.setTimeZoneId("Pacific/Auckland");
		inputConfig.setServiceProps(Collections.singletonMap("foo", "bar"));
		conf.setInputConfiguration(inputConfig);

		return conf;
	}

	private DatumImportJobInfo createTestInfo() {
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setImportDate(Instant.now());
		info.setConfig(createNewConfig());
		return info;
	}

	@Test
	public void storeNew() {
		DatumImportJobInfo info = createTestInfo();

		UserUuidPK id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		info.setId(id);
		this.info = info;
	}

	@Test
	public void storeNew_withTokenId() {
		DatumImportJobInfo info = createTestInfo();
		String tokenId = UUID.randomUUID().toString();
		info.setTokenId(tokenId);
		this.tokenId = tokenId;

		UserUuidPK id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		info.setId(id);
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		DatumImportJobInfo info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("User ID", info.getUserId(), equalTo(this.userId));
		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Import date", info.getImportDate(), notNullValue());
		assertThat("Percent complete", info.getPercentComplete(), equalTo(0.0));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey_withTokenId() {
		storeNew_withTokenId();
		DatumImportJobInfo info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("User ID", info.getUserId(), equalTo(this.userId));
		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Import date", info.getImportDate(), notNullValue());
		assertThat("Percent complete", info.getPercentComplete(), equalTo(0.0));
		assertThat("Token ID", info.getTokenId(), equalTo(this.tokenId));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void storeNew_generateGroupKey() {
		storeNew();
		DatumImportJobInfo info = dao.get(this.info.getId());
		assertThat("Group key generated on save when not supplied", info.getGroupKey(), notNullValue());
	}

	@Test
	public void update() {
		storeNew();

		DatumImportJobInfo info = dao.get(this.info.getId());
		Instant originalCreated = info.getCreated();
		info.setCreated(Instant.now()); // should not actually save
		info.setImportState(DatumImportState.Completed);
		info.setStarted(Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.SECONDS));
		info.setCompleted(Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.SECONDS));
		info.setJobSuccess(Boolean.TRUE);
		info.setLoadedCount(1234);
		info.setPercentComplete(50.0);

		UserUuidPK id = dao.store(info);
		assertThat("PK unchanged", id, equalTo(this.info.getId()));

		DatumImportJobInfo updated = dao.get(id);
		assertThat("Found by PK", updated, notNullValue());
		assertThat("New entity returned", updated, not(sameInstance(info)));
		assertThat("PK", updated.getId(), equalTo(info.getId()));
		assertThat("Created unchanged", updated.getCreated(), equalTo(originalCreated));
		assertThat("State changed", updated.getImportState(), equalTo(info.getImportState()));
		assertThat("Success changed", updated.isSuccess(), equalTo(info.isSuccess()));
		assertThat("Loaded count changed", updated.getLoadedCount(), equalTo(info.getLoadedCount()));
		assertThat("Percent complete changed", updated.getPercentComplete(),
				equalTo(info.getPercentComplete()));
		assertThat("Started date changed", updated.getStarted(), equalTo(info.getStarted()));
		assertThat("Completed date changed", updated.getCompleted(), equalTo(info.getCompleted()));
	}

	@Test
	public void purgeCompletedNoneCompleted() {
		getByPrimaryKey();
		long result = dao.purgeOldJobs(Instant.now());
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompletedNoneExpired() {
		getByPrimaryKey();

		long result = dao
				.purgeOldJobs(Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompleted() {
		getByPrimaryKey();
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Completed);
		info.setCompleted(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		long result = dao
				.purgeOldJobs(Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(1L));

		DatumImportJobInfo notCompleted = dao.get(this.info.getId());
		assertThat("Unfinished job still available", notCompleted, notNullValue());
	}

	@Test
	public void purgeStaged() {
		getByPrimaryKey();

		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		long result = dao
				.purgeOldJobs(Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(0L));

		DatumImportJobInfo notCompleted = dao.get(this.info.getId());
		assertThat("Unfinished job still available", notCompleted, notNullValue());
		DatumImportJobInfo staged = dao.get(info.getId());
		assertThat("Staged job still available", staged, notNullValue());
	}

	@Test
	public void deleteForUserNoMatchingUser() {
		storeNew();
		int count = dao.deleteForUser(this.userId - 1, null, null);
		assertThat("Delete count", count, equalTo(0));
	}

	@Test
	public void deleteForUserNoMatchingId() {
		storeNew();
		int count = dao.deleteForUser(this.userId, singleton(UUID.randomUUID()), null);
		assertThat("Delete count", count, equalTo(0));
	}

	@Test
	public void deleteForUserNoMatchingState() {
		storeNew();
		int count = dao.deleteForUser(this.userId, null, EnumSet.of(DatumImportState.Completed));
		assertThat("Delete count", count, equalTo(0));
	}

	@Test
	public void deleteForUserMatchingUser() {
		storeNew();
		int count = dao.deleteForUser(this.userId, null, null);
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingId() {
		storeNew();
		int count = dao.deleteForUser(this.userId, singleton(this.info.getId().getId()), null);
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingState() {
		storeNew();
		int count = dao.deleteForUser(this.userId, singleton(this.info.getId().getId()),
				EnumSet.of(DatumImportState.Unknown));
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingAll() {
		storeNew();
		int count = dao.deleteForUser(this.userId, singleton(this.info.getId().getId()),
				EnumSet.of(DatumImportState.Unknown));
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserIgnoresNonMatchingRecords() {
		storeNew();

		// add another job
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(Instant.now().minus(1, ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		int count = dao.deleteForUser(this.userId, singleton(this.info.getId().getId()),
				EnumSet.of(DatumImportState.Unknown));
		assertThat("Delete count", count, equalTo(1));

		List<DatumImportJobInfo> infos = dao.findForUser(this.userId, null);
		assertThat("Remaining jobs", infos, hasSize(1));
		assertThat("Remaining job ID", infos.get(0).getId(), equalTo(info.getId()));
	}

	@Test
	public void updateStateNotFound() {
		boolean updated = dao.updateJobState(new UserUuidPK(-123L, UUID.randomUUID()),
				DatumImportState.Completed, null);
		assertThat("Update result", updated, equalTo(false));
	}

	@Test
	public void updateState() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumImportState.Completed, null);
		assertThat("Update result", updated, equalTo(true));
	}

	@Test
	public void updateStateWithExpectedStateNotFound() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumImportState.Completed,
				Collections.singleton(DatumImportState.Staged));
		assertThat("Update result", updated, equalTo(false));
	}

	@Test
	public void updateStateWithExpectedState() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumImportState.Retracted,
				Collections.singleton(DatumImportState.Unknown));
		assertThat("Update result", updated, equalTo(true));
	}

	@Test
	public void updateStateWithExpectedStates() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumImportState.Retracted,
				EnumSet.of(DatumImportState.Unknown, DatumImportState.Staged, DatumImportState.Queued));
		assertThat("Update result", updated, equalTo(true));
	}

	@Test
	public void findForUserNotFound() {
		List<DatumImportJobInfo> results = dao.findForUser(this.userId, null);
		assertThat("Empty results returned", results, hasSize(0));
	}

	@Test
	public void findForUserNotFoundWithState() {
		storeNew();
		List<DatumImportJobInfo> results = dao.findForUser(this.userId,
				singleton(DatumImportState.Completed));
		assertThat("Empty results returned", results, hasSize(0));
	}

	@Test
	public void findForUserFound() {
		storeNew();

		Long userId2 = storeNewUser("user2@localhost");

		// add another job that should _not_ be found
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(userId2, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(this.userId, null);
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(this.info));
	}

	@Test
	public void findForUserFoundWithState() {
		// add job that should _not_ be found
		storeNew();

		// add another job that _should_ be found
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(this.userId,
				singleton(DatumImportState.Staged));
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(info));
	}

	@Test
	public void findForUserFoundWithGroupKey() {
		// add job that should _not_ be found
		storeNew();

		final String groupKey = UUID.randomUUID().toString();

		// add another job that _should_ be found
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info.setGroupKey(groupKey);
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(this.userId,
				singleton(DatumImportState.Staged));
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(info));
		assertThat("Result group matches", results.get(0).getGroupKey(), equalTo(groupKey));
	}

	@Test
	public void findForUserFoundMultiple() {
		storeNew();

		// add another job
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.userId, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(Instant.now().minus(1, ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(this.userId,
				EnumSet.of(DatumImportState.Staged, DatumImportState.Unknown));
		assertThat("Results returned", results, hasSize(2));

		// should be ordered by creation date (descending)
		assertThat("Result matches", results.get(0), equalTo(this.info));
		assertThat("Result matches", results.get(1), equalTo(info));
	}

	@Test
	public void updateConfigNotFound() {
		BasicConfiguration config = new BasicConfiguration("Foo", true);
		boolean updated = dao.updateJobConfiguration(new UserUuidPK(-123L, UUID.randomUUID()), config);
		assertThat("Update result", updated, equalTo(false));
	}

	@Test
	public void updateConfig() {
		storeNew();
		dao.updateJobState(info.getId(), DatumImportState.Staged, null);

		BasicConfiguration config = new BasicConfiguration(info.getConfiguration());
		config.setName("Updated");
		config.getInputConfig().setTimeZoneId("UTC");
		boolean updated = dao.updateJobConfiguration(info.getId(), config);
		assertThat("Update result", updated, equalTo(true));

		DatumImportJobInfo modified = dao.get(info.getId());
		assertThat("New instance", modified, not(sameInstance(info)));
		assertThat("Modified config name", modified.getConfiguration().getName(),
				equalTo(config.getName()));
		assertThat("Modified time zone",
				modified.getConfiguration().getInputConfiguration().getTimeZoneId(),
				equalTo(config.getInputConfiguration().getTimeZoneId()));
	}

	@Test
	public void updateConfigWithExpectedStateNotFound() {
		storeNew();

		BasicConfiguration config = new BasicConfiguration(info.getConfiguration());
		config.setName("Updated");
		config.getInputConfig().setTimeZoneId("UTC");
		boolean updated = dao.updateJobConfiguration(info.getId(), config);
		assertThat("Update result", updated, equalTo(false));

		DatumImportJobInfo unchanged = dao.get(info.getId());
		assertThat("New instance", unchanged, not(sameInstance(info)));
		assertThat("Unchanged config name", unchanged.getConfiguration().getName(),
				equalTo(info.getConfiguration().getName()));
	}

	@Test
	public void updateProgress() {
		storeNew();
		boolean updated = dao.updateJobProgress(this.info.getId(), 0.1, 2L);
		assertThat("Update result", updated, equalTo(true));
		DatumImportJobInfo info = dao.get(this.info.getId());
		assertThat("Progress updated", info.getPercentComplete(), equalTo(0.1));
		assertThat("Loaded updated", info.getLoadedCount(), equalTo(2L));
	}

	@Test
	public void updateProgressNotFound() {
		storeNew();
		UserUuidPK id = new UserUuidPK(this.userId, UUID.randomUUID());
		boolean updated = dao.updateJobProgress(id, 0.1, 2L);
		assertThat("Update result", updated, equalTo(false));
		DatumImportJobInfo info = dao.get(this.info.getId());
		assertThat("Progress not updated", info.getPercentComplete(), equalTo(0.0));
		assertThat("Loaded not updated", info.getLoadedCount(), equalTo(0L));
	}

	@Test
	public void claimQueuedJob_none() {
		// GIVEN

		// WHEN
		DatumImportJobInfo claimed = dao.claimQueuedJob();

		// THEN
		assertThat("No job claimed when no jobs present", claimed, nullValue());
	}

	@Test
	public void claimQueuedJob_noneInQueuedState() {
		// GIVEN
		DatumImportJobInfo info = createTestInfo();
		info.setImportState(DatumImportState.Completed);
		dao.store(info);

		// WHEN
		DatumImportJobInfo claimed = dao.claimQueuedJob();

		// THEN
		assertThat("No job claimed when only completed job present", claimed, nullValue());
	}

	@Test
	public void claimQueuedJob_oneInQueuedState() {
		// GIVEN
		DatumImportJobInfo info = createTestInfo();
		info.setImportState(DatumImportState.Queued);
		UserUuidPK id = dao.store(info);

		// WHEN
		DatumImportJobInfo claimed = dao.claimQueuedJob();

		// THEN
		assertThat("Job claimed when queued job present", claimed.getId(), equalTo(id));
	}

	@Test
	public void claimQueuedJob_multipleInQueuedState() {
		// GIVEN
		List<UserUuidPK> ids = new ArrayList<>(3);
		Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < 3; i++ ) {
			DatumImportJobInfo info = createTestInfo();
			info.setImportState(DatumImportState.Queued);
			info.setCreated(start.plus(i, ChronoUnit.MINUTES));
			UserUuidPK id = dao.store(info);
			ids.add(id);
		}

		// WHEN
		for ( int i = 0; i < 3; i++ ) {
			DatumImportJobInfo claimed = dao.claimQueuedJob();

			// THEN
			assertThat("Oldest job " + i + " claimed when queued job present", claimed.getId(),
					equalTo(ids.get(i)));
		}

		assertThat("No more queued jobs available", dao.claimQueuedJob(), nullValue());
	}

	@Test
	public void claimQueuedJob_ignoreExecutingGroupKey() throws Exception {
		// GIVEN
		List<UserUuidPK> ids = new ArrayList<>(3);
		Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < 3; i++ ) {
			DatumImportJobInfo info = createTestInfo();
			if ( i == 0 ) {
				info.setImportState(DatumImportState.Executing);
			} else {
				info.setImportState(DatumImportState.Queued);
			}
			info.setGroupKey("foo");
			info.setCreated(start.plus(i, ChronoUnit.MINUTES));
			UserUuidPK id = dao.store(info);
			ids.add(id);
		}

		// WHEN
		DatumImportJobInfo claimed = dao.claimQueuedJob();

		// THEN
		assertThat("No queued job available when executing in group", claimed, nullValue());
	}

	@Test
	public void claimQueuedJob_completedGroupKey() throws Exception {
		// GIVEN
		List<UserUuidPK> ids = new ArrayList<>(3);
		Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < 3; i++ ) {
			DatumImportJobInfo info = createTestInfo();
			if ( i == 0 ) {
				info.setImportState(DatumImportState.Completed);
			} else {
				info.setImportState(DatumImportState.Queued);
			}
			info.setGroupKey("foo");
			info.setCreated(start.plus(i, ChronoUnit.MINUTES));
			UserUuidPK id = dao.store(info);
			ids.add(id);
		}

		// WHEN
		DatumImportJobInfo claimed = dao.claimQueuedJob();

		// THEN
		assertThat("Oldest queued job claimed when group has completed tasks", claimed.getId(),
				equalTo(ids.get(1)));
	}

	private void runExternalTransaction(Runnable task, Runnable main) throws Exception {
		// GIVEN

		// latch for row lock thread to indicate it has locked the row and the main thread can continue
		final CountDownLatch lockedLatch = new CountDownLatch(1);

		// list to capture exception thrown by row lock thread
		final List<Exception> threadExceptions = new ArrayList<Exception>(1);

		// object monitor for main thread to signal to row lock thread to complete
		final Object lockThreadSignal = new Object();

		// lock a stale row
		Thread lockThread = new Thread(new Runnable() {

			@Override
			public void run() {
				txTemplate.execute(new TransactionCallback<Object>() {

					@Override
					public Object doInTransaction(TransactionStatus status) {
						try {
							task.run();

							log.debug("Waiting for signal while keeping transaction open...");

							lockedLatch.countDown();

							// wait
							try {
								synchronized ( lockThreadSignal ) {
									lockThreadSignal.wait();
								}
							} catch ( InterruptedException e ) {
								log.error("StaleRowLockingThread interrupted waiting", e);
							}
						} catch ( RuntimeException e ) {
							threadExceptions.add(e);
							throw e;
						} finally {
							status.setRollbackOnly();
						}
						return null;
					}

				});
			}

		}, "ExternalTransactionThread");
		lockThread.setDaemon(true);
		lockThread.start();

		// wait for our latch
		boolean locked = lockedLatch.await(5, TimeUnit.SECONDS);
		if ( !threadExceptions.isEmpty() ) {
			throw threadExceptions.get(0);
		}
		assertThat("External transaction executed: {}", locked, equalTo(true));

		// WHEN
		txTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					main.run();
				} finally {
					synchronized ( lockThreadSignal ) {
						lockThreadSignal.notifyAll();
					}
				}
				return null;
			}

		});

		// wait for the lock thread to complete
		lockThread.join(5000);
	}

	@Test
	public void claimQueuedJob_ignoreClaimedInOtherTransaction() throws Exception {
		final DatumImportJobInfo info = createTestInfo();
		info.setImportState(DatumImportState.Queued);
		final UserUuidPK id = dao.store(info);
		TestTransaction.flagForCommit();
		TestTransaction.end();

		try {
			// GIVEN
			runExternalTransaction(new Runnable() {

				@Override
				public void run() {
					DatumImportJobInfo info = dao.claimQueuedJob();
					assertThat("Claimed job in external transaction", info.getId(), equalTo(id));
				}
			}, new Runnable() {

				@Override
				public void run() {
					DatumImportJobInfo claimed = dao.claimQueuedJob();
					assertThat("Job claimed in external transaction not re-claimed", claimed,
							nullValue());
				}
			});
		} finally {
			jdbcTemplate.execute("delete from solarnet.sn_datum_import_job");
			jdbcTemplate.execute("delete from solaruser.user_user");
		}
	}

	@Test
	public void claimQueuedJob_ignoreClaimedGroupKeyInOtherTransaction() throws Exception {
		List<UserUuidPK> ids = new ArrayList<>(3);
		Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < 3; i++ ) {
			DatumImportJobInfo info = createTestInfo();
			info.setImportState(DatumImportState.Queued);
			info.setGroupKey("foo");
			info.setCreated(start.plus(i, ChronoUnit.MINUTES));
			UserUuidPK id = dao.store(info);
			ids.add(id);
		}
		TestTransaction.flagForCommit();
		TestTransaction.end();

		try {
			// GIVEN
			runExternalTransaction(new Runnable() {

				@Override
				public void run() {
					DatumImportJobInfo info = dao.claimQueuedJob();
					log.debug("Claimed job {}", info.getId());
					assertThat("Claimed oldest job in external transaction", info.getId(),
							equalTo(ids.get(0)));
				}
			}, new Runnable() {

				@Override
				public void run() {
					DatumImportJobInfo claimed = dao.claimQueuedJob();
					assertThat("No other jobs claimed because of shared group key", claimed,
							nullValue());
				}
			});
		} finally {
			jdbcTemplate.execute("delete from solarnet.sn_datum_import_job");
			jdbcTemplate.execute("delete from solaruser.user_user");
		}
	}

	@Test
	public void claimQueuedJob_ignoreClaimedGroupKeyInOtherTransaction_withCompleted() throws Exception {
		List<UserUuidPK> ids = new ArrayList<>(3);
		Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < 3; i++ ) {
			DatumImportJobInfo info = createTestInfo();
			if ( i == 0 ) {
				info.setImportState(DatumImportState.Completed);
			} else {
				info.setImportState(DatumImportState.Queued);
			}
			info.setGroupKey("foo");
			info.setCreated(start.plus(i, ChronoUnit.MINUTES));
			UserUuidPK id = dao.store(info);
			ids.add(id);
		}
		TestTransaction.flagForCommit();
		TestTransaction.end();

		try {
			// GIVEN
			runExternalTransaction(new Runnable() {

				@Override
				public void run() {
					DatumImportJobInfo info = dao.claimQueuedJob();
					log.debug("Claimed job {}", info.getId());
					assertThat("Claimed oldest non-completed job in external transaction", info.getId(),
							equalTo(ids.get(1)));
				}
			}, new Runnable() {

				@Override
				public void run() {
					DatumImportJobInfo claimed = dao.claimQueuedJob();
					assertThat("No other jobs claimed because of shared group key", claimed,
							nullValue());
				}
			});
		} finally {
			jdbcTemplate.execute("delete from solarnet.sn_datum_import_job");
			jdbcTemplate.execute("delete from solaruser.user_user");
		}
	}

}
