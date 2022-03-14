/* ==================================================================
 * MyBatisUserDatumDeleteJobInfoDaoTests.java - 26/11/2018 11:44:24 AM
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

package net.solarnetwork.central.user.expire.dao.mybatis.test;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.expire.dao.mybatis.MyBatisUserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;

/**
 * Test cases for the {@link MyBatisUserDatumDeleteJobInfoDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserDatumDeleteJobInfoDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisUserDatumDeleteJobInfoDao dao;

	private User user;
	private DatumDeleteJobInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisUserDatumDeleteJobInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertThat("Test user", this.user, notNullValue());

		info = null;
	}

	private DatumFilterCommand createNewConfig() {
		DatumFilterCommand conf = new DatumFilterCommand();
		conf.setNodeIds(new Long[] { TEST_NODE_ID, -2L });
		conf.setUserId(this.user.getId());
		conf.setSourceIds(new String[] { "a", "b" });
		return conf;
	}

	@Test
	public void storeNew() {
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfiguration(createNewConfig());

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
		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("User ID", info.getUserId(), equalTo(this.user.getId()));
		assertThat("Config", info.getConfiguration(), notNullValue());
		assertThat("Percent complete", info.getPercentComplete(), equalTo(0.0));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void update() {
		storeNew();

		DatumDeleteJobInfo info = dao.get(this.info.getId());
		Instant originalCreated = info.getCreated();
		info.setCreated(Instant.now()); // should not actually save
		info.setJobState(DatumDeleteJobState.Completed);
		info.setStarted(Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.SECONDS));
		info.setCompleted(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1));
		info.setJobSuccess(Boolean.TRUE);
		info.setResult(1234L);
		info.setPercentComplete(50.0);

		UserUuidPK id = dao.store(info);
		assertThat("PK unchanged", id, equalTo(this.info.getId()));

		DatumDeleteJobInfo updated = dao.get(id);
		assertThat("Found by PK", updated, notNullValue());
		assertThat("New entity returned", updated, not(sameInstance(info)));
		assertThat("PK", updated.getId(), equalTo(info.getId()));
		assertThat("Created unchanged", updated.getCreated(), equalTo(originalCreated));
		assertThat("State changed", updated.getJobState(), equalTo(info.getJobState()));
		assertThat("Success changed", updated.isSuccess(), equalTo(info.isSuccess()));
		assertThat("Loaded count changed", updated.getResult(), equalTo(info.getResult()));
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
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfiguration(new DatumFilterCommand());
		info.setJobState(DatumDeleteJobState.Completed);
		info.setCompleted(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		long result = dao
				.purgeOldJobs(Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(1L));

		DatumDeleteJobInfo notCompleted = dao.get(this.info.getId());
		assertThat("Unfinished job still available", notCompleted, notNullValue());
	}

	@Test
	public void deleteForUserNoMatchingUser() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId() - 1, null, null);
		assertThat("Delete count", count, equalTo(0));
	}

	@Test
	public void deleteForUserNoMatchingId() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), singleton(UUID.randomUUID()), null);
		assertThat("Delete count", count, equalTo(0));
	}

	@Test
	public void deleteForUserNoMatchingState() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), null,
				EnumSet.of(DatumDeleteJobState.Completed));
		assertThat("Delete count", count, equalTo(0));
	}

	@Test
	public void deleteForUserMatchingUser() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), null, null);
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingId() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), singleton(this.info.getId().getId()), null);
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingState() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), singleton(this.info.getId().getId()),
				EnumSet.of(DatumDeleteJobState.Unknown));
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingAll() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), singleton(this.info.getId().getId()),
				EnumSet.of(DatumDeleteJobState.Unknown));
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserIgnoresNonMatchingRecords() {
		storeNew();

		// add another job
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfiguration(new DatumFilterCommand());
		info.setJobState(DatumDeleteJobState.Queued);
		info.setCreated(Instant.now().minus(1, ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		int count = dao.deleteForUser(this.user.getId(), singleton(this.info.getId().getId()),
				EnumSet.of(DatumDeleteJobState.Unknown));
		assertThat("Delete count", count, equalTo(1));

		List<DatumDeleteJobInfo> infos = dao.findForUser(this.user.getId(), null);
		assertThat("Remaining jobs", infos, hasSize(1));
		assertThat("Remaining job ID", infos.get(0).getId(), equalTo(info.getId()));
	}

	@Test
	public void updateStateNotFound() {
		boolean updated = dao.updateJobState(new UserUuidPK(-123L, UUID.randomUUID()),
				DatumDeleteJobState.Completed, null);
		assertThat("Update result", updated, equalTo(false));
	}

	@Test
	public void updateState() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumDeleteJobState.Completed, null);
		assertThat("Update result", updated, equalTo(true));

		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("New instance", info, not(sameInstance(this.info)));
		assertThat("State updated", info.getJobState(), equalTo(DatumDeleteJobState.Completed));
	}

	@Test
	public void updateStateWithExpectedStateNotFound() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumDeleteJobState.Completed,
				Collections.singleton(DatumDeleteJobState.Queued));
		assertThat("Update result", updated, equalTo(false));

		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("New instance", info, not(sameInstance(this.info)));
		assertThat("State unchanged", info.getJobState(), equalTo(DatumDeleteJobState.Unknown));
	}

	@Test
	public void updateStateWithExpectedState() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumDeleteJobState.Completed,
				Collections.singleton(DatumDeleteJobState.Unknown));
		assertThat("Update result", updated, equalTo(true));

		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("New instance", info, not(sameInstance(this.info)));
		assertThat("State updated", info.getJobState(), equalTo(DatumDeleteJobState.Completed));
	}

	@Test
	public void updateStateWithExpectedStates() {
		storeNew();
		boolean updated = dao.updateJobState(this.info.getId(), DatumDeleteJobState.Completed,
				EnumSet.of(DatumDeleteJobState.Unknown, DatumDeleteJobState.Queued));
		assertThat("Update result", updated, equalTo(true));

		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("New instance", info, not(sameInstance(this.info)));
		assertThat("State updated", info.getJobState(), equalTo(DatumDeleteJobState.Completed));
	}

	@Test
	public void findForUserNotFound() {
		List<DatumDeleteJobInfo> results = dao.findForUser(user.getId(), null);
		assertThat("Empty results returned", results, hasSize(0));
	}

	@Test
	public void findForUserNotFoundWithState() {
		storeNew();
		List<DatumDeleteJobInfo> results = dao.findForUser(user.getId(),
				singleton(DatumDeleteJobState.Completed));
		assertThat("Empty results returned", results, hasSize(0));
	}

	@Test
	public void findForUserFound() {
		storeNew();

		User user2 = createNewUser("user2@localhost");

		// add another job that should _not_ be found
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(user2.getId(), UUID.randomUUID()));
		info.setConfiguration(new DatumFilterCommand());
		info.setJobState(DatumDeleteJobState.Queued);
		info.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		List<DatumDeleteJobInfo> results = dao.findForUser(user.getId(), null);
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(this.info));
	}

	@Test
	public void findForUserFoundWithState() {
		// add job that should _not_ be found
		storeNew();

		// add another job that _should_ be found
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfiguration(new DatumFilterCommand());
		info.setJobState(DatumDeleteJobState.Queued);
		info.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		List<DatumDeleteJobInfo> results = dao.findForUser(user.getId(),
				singleton(DatumDeleteJobState.Queued));
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(info));
	}

	@Test
	public void findForUserFoundMultiple() {
		storeNew();

		// add another job
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfiguration(new DatumFilterCommand());
		info.setJobState(DatumDeleteJobState.Queued);
		info.setCreated(Instant.now().minus(1, ChronoUnit.HOURS));
		info = dao.get(dao.store(info));

		List<DatumDeleteJobInfo> results = dao.findForUser(user.getId(),
				EnumSet.of(DatumDeleteJobState.Queued, DatumDeleteJobState.Unknown));
		assertThat("Results returned", results, hasSize(2));

		// should be ordered by creation date (descending)
		assertThat("Result matches", results.get(0), equalTo(this.info));
		assertThat("Result matches", results.get(1), equalTo(info));
	}

	@Test
	public void updateConfigNotFound() {
		DatumFilterCommand config = new DatumFilterCommand();
		boolean updated = dao.updateJobConfiguration(new UserUuidPK(-123L, UUID.randomUUID()), config);
		assertThat("Update result", updated, equalTo(false));
	}

	@Test
	public void updateConfig() {
		storeNew();
		dao.updateJobState(info.getId(), DatumDeleteJobState.Queued, null);

		DatumFilterCommand config = new DatumFilterCommand(info.getConfiguration());
		config.setNodeId(-4L);
		boolean updated = dao.updateJobConfiguration(info.getId(), config);
		assertThat("Update result", updated, equalTo(true));

		DatumDeleteJobInfo modified = dao.get(info.getId());
		assertThat("New instance", modified, not(sameInstance(info)));
		assertThat("Modified config nodeId", modified.getConfiguration().getNodeId(),
				equalTo(config.getNodeId()));
	}

	@Test
	public void updateConfigWithExpectedStateNotFound() {
		storeNew();

		DatumFilterCommand config = new DatumFilterCommand(info.getConfiguration());
		config.setNodeId(-4L);
		boolean updated = dao.updateJobConfiguration(info.getId(), config);
		assertThat("Update result", updated, equalTo(false));

		DatumDeleteJobInfo unchanged = dao.get(info.getId());
		assertThat("New instance", unchanged, not(sameInstance(info)));
		assertThat("Unchanged config nodeId", unchanged.getConfiguration().getNodeId(),
				equalTo(info.getConfiguration().getNodeId()));
	}

	@Test
	public void updateProgress() {
		storeNew();
		boolean updated = dao.updateJobProgress(this.info.getId(), 0.1, 2L);
		assertThat("Update result", updated, equalTo(true));
		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("Progress updated", info.getPercentComplete(), equalTo(0.1));
		assertThat("Loaded updated", info.getResult(), equalTo(2L));
	}

	@Test
	public void updateProgressNotFound() {
		storeNew();
		UserUuidPK id = new UserUuidPK(this.user.getId(), UUID.randomUUID());
		boolean updated = dao.updateJobProgress(id, 0.1, 2L);
		assertThat("Update result", updated, equalTo(false));
		DatumDeleteJobInfo info = dao.get(this.info.getId());
		assertThat("Progress not updated", info.getPercentComplete(), equalTo(0.0));
		assertThat("Loaded not updated", info.getResult(), nullValue());
	}

}
