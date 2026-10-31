/* ==================================================================
 * MyBatisAccountTaskDaoTests.java - 21/07/2020 6:57:07 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountTaskDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * Test cases for the {@link MyBatisAccountTaskDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class MyBatisAccountTaskDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisAccountTaskDao dao;

	private Address address;
	private Account account;
	private AccountTask last;

	@BeforeEach
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisAccountTaskDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		address = createTestAddress();
		account = createTestAccount(address);
		last = null;
	}

	@Override
	protected Address createTestAddress() {
		Address s = super.createTestAddress();
		return addressDao.get(addressDao.save(s));
	}

	@Override
	protected Account createTestAccount(Address address) {
		Account account = super.createTestAccount(address);
		return accountDao.get(accountDao.save(account));
	}

	private AccountTask createTestAccountTask(Account account) {
		AccountTask t = new AccountTask(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()), AccountTaskType.GenerateInvoice,
				account.getAccountId(), Map.of("foo", "bar"));
		return t;
	}

	@Test
	public void insert() {
		AccountTask entity = createTestAccountTask(account);
		UUID pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		last = entity;
	}

	@Test
	public void insert_duplicate() {
		insert();
		AccountTask entity = createTestAccountTask(account);
		dao.save(entity);
		getSqlSessionTemplate().flushStatements();
	}

	@Test
	public void getByPK() {
		insert();
		AccountTask entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("AccountTask", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void delete() {
		insert();
		dao.delete(last);
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	@Test
	public void delete_noMatch() {
		insert();
		AccountTask someAddr = createTestAccountTask(account);
		dao.delete(someAddr);

		AccountTask entity = dao.get(last.getId());
		assertThat("Entity unchanged", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void claim_handle() {
		insert();
		AccountTask entity = dao.claimAccountTask();
		assertThat("Entity returned", entity.isSameAs(last), equalTo(true));
		dao.delete(entity);
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	private Instant execAt(AccountTask task) {
		getSqlSessionTemplate().flushStatements();
		return jdbcTemplate
				.queryForObject("SELECT exec_at FROM solarbill.bill_account_task WHERE id = ?",
						Timestamp.class, task.getId())
				.toInstant();
	}

	@Test
	public void defer() {
		// GIVEN
		insert();
		final Instant execAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

		// WHEN
		dao.deferAccountTask(last.getId(), execAt);

		// THEN
		// @formatter:off
		then(execAt(last))
			.as("Task exec time updated")
			.isEqualTo(execAt)
			;
		then(dao.get(last.getId()))
			.as("Task otherwise unchanged")
			.matches(t -> t.isSameAs(last))
			.as("Created date unchanged")
			.returns(last.getCreated(), from(AccountTask::getCreated))
			;
		// @formatter:on
	}

	@Test
	public void claim_deferred() {
		// GIVEN
		insert();
		dao.deferAccountTask(last.getId(), Instant.now().plus(1, ChronoUnit.HOURS));

		// WHEN
		AccountTask result = dao.claimAccountTask();

		// THEN
		// @formatter:off
		then(result)
			.as("Deferred task not claimed before its exec time")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void claim_deferredExecTimeArrived() {
		// GIVEN
		insert();
		dao.deferAccountTask(last.getId(), Instant.now().minus(1, ChronoUnit.MINUTES));

		// WHEN
		AccountTask result = dao.claimAccountTask();

		// THEN
		// @formatter:off
		then(result)
			.as("Deferred task claimed once its exec time has arrived")
			.isNotNull()
			.matches(t -> t.isSameAs(last))
			;
		// @formatter:on
	}

	@Test
	public void claim_deferredSkipped() {
		// GIVEN
		insert();
		final AccountTask deferred = last;
		dao.deferAccountTask(deferred.getId(), Instant.now().plus(1, ChronoUnit.HOURS));

		AccountTask later = new AccountTask(UUID.randomUUID(),
				deferred.getCreated().plus(1, ChronoUnit.MINUTES), AccountTaskType.DeliverInvoice,
				account.getAccountId(), Map.of("id", 1L));
		dao.save(later);

		// WHEN
		AccountTask result = dao.claimAccountTask();

		// THEN
		// @formatter:off
		then(result)
			.as("Newer task claimed while older one deferred")
			.isNotNull()
			.returns(later.getId(), from(AccountTask::getId))
			;
		// @formatter:on
	}

}
