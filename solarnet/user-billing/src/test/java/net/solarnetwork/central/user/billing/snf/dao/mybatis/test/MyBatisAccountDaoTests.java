/* ==================================================================
 * MyBatisAccountDaoTests.java - 21/07/2020 7:55:24 AM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Test cases for the {@link MyBatisAccountDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisAccountDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao dao;

	private Address address;
	private Account last;

	@Before
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());
		dao = new MyBatisAccountDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		address = addressDao.get(addressDao.save(createTestAddress()));
		last = null;
	}

	@Test
	public void insert() {
		Account entity = createTestAccount(address);
		UserLongPK pk = dao.save(entity);
		getSqlSessionTemplate().flushStatements();
		assertThat("PK created", pk, notNullValue());
		assertThat("PK userId preserved", pk.getUserId(), equalTo(entity.getUserId()));
		last = entity;
		last.getId().setId(pk.getId());
	}

	@Test
	public void getByPK() {
		insert();
		Account entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Account", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void getByUser() {
		insert();
		Account entity = dao.getForUser(last.getUserId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Account", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void getByUser_atDate() {
		insert();

		Address oldAddress = createTestAddress(Instant.now().minus(2, ChronoUnit.DAYS));
		oldAddress.setName("OLD");
		oldAddress = addressDao.get(addressDao.save(oldAddress));

		Account now = dao.getForUser(last.getUserId(),
				ZonedDateTime.now(oldAddress.getTimeZone()).toLocalDate());
		Account yesterday = dao.getForUser(last.getUserId(),
				ZonedDateTime.now(oldAddress.getTimeZone()).minus(1, ChronoUnit.DAYS).toLocalDate());
		Account tomorrow = dao.getForUser(last.getUserId(),
				ZonedDateTime.now(oldAddress.getTimeZone()).plus(1, ChronoUnit.DAYS).toLocalDate());

		assertThat("ID", now.getId(), equalTo(last.getId()));
		assertThat("Created", now.getCreated(), equalTo(last.getCreated()));
		assertThat("Account", now.isSameAs(last), equalTo(true));
		assertThat("Current account has current address", now.getAddress(), equalTo(last.getAddress()));

		assertThat("ID", yesterday.getId(), equalTo(last.getId()));
		assertThat("Yesterday account has yesterday address", yesterday.getAddress(),
				equalTo(oldAddress));

		assertThat("ID", tomorrow.getId(), equalTo(last.getId()));
		assertThat("Tomorrow account has current address", tomorrow.getAddress(),
				equalTo(last.getAddress()));
	}

	@Test
	public void update() {
		insert();
		Account obj = dao.get(last.getId());
		obj.setCurrencyCode("USD");
		obj.setLocale("en_US");
		UserLongPK pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		Account entity = dao.get(pk);
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
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
		Account someAddr = createTestAccount(address);
		dao.delete(someAddr);

		Account entity = dao.get(last.getId());
		assertThat("Entity unchanged", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void balance_none() {
		insert();
		AccountBalance balance = dao.getBalanceForUser(last.getUserId());
		assertThat("No balance available", balance, nullValue());
	}

	private void insertAccountBalance(Long accountId, BigDecimal chargeTotal, BigDecimal paymentTotal,
			BigDecimal availableCredit) {
		jdbcTemplate.update(
				"insert into solarbill.bill_account_balance (acct_id,charge_total,payment_total,avail_credit) VALUES (?,?,?,?)",
				accountId, chargeTotal, paymentTotal, availableCredit);
	}

	@Test
	public void balance_get() {
		insert();
		final BigDecimal charge = new BigDecimal("12345.67");
		final BigDecimal payment = new BigDecimal("234789.01");
		final BigDecimal credit = new BigDecimal("65432.10");
		insertAccountBalance(last.getId().getId(), charge, payment, credit);
		AccountBalance balance = dao.getBalanceForUser(last.getUserId());
		assertThat("Balance available", balance, notNullValue());
		assertThat("Balance charge total", balance.getChargeTotal().compareTo(charge), equalTo(0));
		assertThat("Balance payment total", balance.getPaymentTotal().compareTo(payment), equalTo(0));
		assertThat("Balance credit", balance.getAvailableCredit().compareTo(credit), equalTo(0));
	}

	@Test
	public void claimCredit_zeroWhenNoBalanceRecord() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));

		// WHEN
		BigDecimal claimed = dao.claimAccountBalanceCredit(account.getId().getId(), BigDecimal.ZERO);

		// THEN
		assertThat("Able to claim 0 when no claim available", claimed, equalTo(BigDecimal.ZERO));
	}

	@Test
	public void claimCredit_noBalanceRecord() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));

		// WHEN
		BigDecimal claimed = dao.claimAccountBalanceCredit(account.getId().getId(), BigDecimal.TEN);

		// THEN
		assertThat("Able to claim 0 when no claim available", claimed, equalTo(BigDecimal.ZERO));
	}

	@Test
	public void claimCredit_fullExplicit() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));
		insertAccountBalance(account.getId().getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);

		// WHEN
		BigDecimal claimed = dao.claimAccountBalanceCredit(account.getId().getId(), BigDecimal.TEN);

		// THEN
		assertThat("Able to claim requested amount when equal to available credit",
				claimed.compareTo(BigDecimal.TEN), equalTo(0));
	}

	@Test
	public void claimCredit_fullImplicit() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));
		insertAccountBalance(account.getId().getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);

		// WHEN
		BigDecimal claimed = dao.claimAccountBalanceCredit(account.getId().getId(), null);

		// THEN
		assertThat("Able to claim entire amount when max implied", claimed.compareTo(BigDecimal.TEN),
				equalTo(0));
	}

	@Test
	public void claimCredit_partial() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));
		insertAccountBalance(account.getId().getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);

		// WHEN
		BigDecimal claimed = dao.claimAccountBalanceCredit(account.getId().getId(), BigDecimal.ONE);

		// THEN
		assertThat("Able to claim requested amount when less than available credit",
				claimed.compareTo(BigDecimal.ONE), equalTo(0));
	}

	@Test
	public void claimCredit_fullImplicitTwice() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));
		insertAccountBalance(account.getId().getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);

		// WHEN
		BigDecimal claimed1 = dao.claimAccountBalanceCredit(account.getId().getId(), null);
		BigDecimal claimed2 = dao.claimAccountBalanceCredit(account.getId().getId(), null);

		// THEN
		assertThat("Able to claim full amount", claimed1.compareTo(BigDecimal.TEN), equalTo(0));
		assertThat("Able to claim nothing further after full amount claimed",
				claimed2.compareTo(BigDecimal.ZERO), equalTo(0));
	}

	@Test
	public void claimCredit_negative() {
		// GIVEN
		Account account = dao.get(dao.save(createTestAccount(address)));
		insertAccountBalance(account.getId().getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);

		// WHEN
		BigDecimal claimed = dao.claimAccountBalanceCredit(account.getId().getId(),
				new BigDecimal("-1.11"));

		// THEN
		assertThat("Negative claim clamped to 0", claimed.compareTo(BigDecimal.ZERO), equalTo(0));
	}
}
