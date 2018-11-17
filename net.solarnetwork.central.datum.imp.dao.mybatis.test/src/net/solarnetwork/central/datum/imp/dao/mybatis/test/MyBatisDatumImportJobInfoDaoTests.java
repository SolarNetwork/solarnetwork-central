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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.imp.dao.mybatis.MyBatisDatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * Test cases for the {@link MyBatisDatumImportJobInfoDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumImportJobInfoDaoTests extends AbstractMyBatisDatumImportDaoTestSupport {

	private MyBatisDatumImportJobInfoDao dao;

	private User user;
	private DatumImportJobInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisDatumImportJobInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertThat("Test user", this.user, notNullValue());

		info = null;
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

	@Test
	public void storeNew() {
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setImportDate(new DateTime());
		info.setConfig(createNewConfig());

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
		assertThat("User ID", info.getUserId(), equalTo(this.user.getId()));
		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Import date", info.getImportDate(), notNullValue());

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void update() {
		storeNew();

		DatumImportJobInfo info = dao.get(this.info.getId());
		DateTime originalCreated = info.getCreated();
		info.setCreated(new DateTime()); // should not actually save
		info.setImportState(DatumImportState.Completed);
		info.setJobSuccess(Boolean.TRUE);
		info.setLoadedCount(1234);

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
	}

	@Test
	public void purgeCompletedNoneCompleted() {
		getByPrimaryKey();
		long result = dao.purgeOldJobs(new DateTime());
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompletedNoneExpired() {
		getByPrimaryKey();

		long result = dao.purgeOldJobs(new DateTime().hourOfDay().roundCeilingCopy());
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompleted() {
		getByPrimaryKey();
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(new DateTime());
		info.setImportState(DatumImportState.Completed);
		info.setCompleted(new DateTime().hourOfDay().roundFloorCopy());
		info = dao.get(dao.store(info));

		long result = dao.purgeOldJobs(new DateTime().hourOfDay().roundCeilingCopy());
		assertThat("Delete count", result, equalTo(1L));

		DatumImportJobInfo notCompleted = dao.get(this.info.getId());
		assertThat("Unfinished job still available", notCompleted, notNullValue());
	}

	@Test
	public void purgeStaged() {
		getByPrimaryKey();

		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(new DateTime());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(new DateTime().hourOfDay().roundFloorCopy());
		info = dao.get(dao.store(info));

		long result = dao.purgeOldJobs(new DateTime().hourOfDay().roundCeilingCopy());
		assertThat("Delete count", result, equalTo(1L));

		DatumImportJobInfo notCompleted = dao.get(this.info.getId());
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
		int count = dao.deleteForUser(this.user.getId(), null, EnumSet.of(DatumImportState.Completed));
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
				EnumSet.of(DatumImportState.Unknown));
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserMatchingAll() {
		storeNew();
		int count = dao.deleteForUser(this.user.getId(), singleton(this.info.getId().getId()),
				EnumSet.of(DatumImportState.Unknown));
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void deleteForUserIgnoresNonMatchingRecords() {
		storeNew();

		// add another job
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(new DateTime());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(new DateTime().minusHours(1));
		info = dao.get(dao.store(info));

		int count = dao.deleteForUser(this.user.getId(), singleton(this.info.getId().getId()),
				EnumSet.of(DatumImportState.Unknown));
		assertThat("Delete count", count, equalTo(1));

		List<DatumImportJobInfo> infos = dao.findForUser(this.user.getId(), null);
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
		List<DatumImportJobInfo> results = dao.findForUser(user.getId(), null);
		assertThat("Empty results returned", results, hasSize(0));
	}

	@Test
	public void findForUserNotFoundWithState() {
		storeNew();
		List<DatumImportJobInfo> results = dao.findForUser(user.getId(),
				singleton(DatumImportState.Completed));
		assertThat("Empty results returned", results, hasSize(0));
	}

	@Test
	public void findForUserFound() {
		storeNew();

		// add another job that should _not_ be found
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId() - 1L, UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(new DateTime());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(new DateTime().hourOfDay().roundFloorCopy());
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(user.getId(), null);
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(this.info));
	}

	@Test
	public void findForUserFoundWithState() {
		// add job that should _not_ be found
		storeNew();

		// add another job that _should_ be found
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(new DateTime());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(new DateTime().hourOfDay().roundFloorCopy());
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(user.getId(),
				singleton(DatumImportState.Staged));
		assertThat("Results returned", results, hasSize(1));
		assertThat("Result matches", results.get(0), equalTo(info));
	}

	@Test
	public void findForUserFoundMultiple() {
		storeNew();

		// add another job
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setConfig(new BasicConfiguration());
		info.setImportDate(new DateTime());
		info.setImportState(DatumImportState.Staged);
		info.setCreated(new DateTime().minusHours(1));
		info = dao.get(dao.store(info));

		List<DatumImportJobInfo> results = dao.findForUser(user.getId(),
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
}
