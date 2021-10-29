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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
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
 * @version 1.0
 */
public class MyBatisAccountTaskDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisAccountTaskDao dao;

	private Address address;
	private Account account;
	private AccountTask last;

	@Before
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
				account.getId().getId(), Collections.singletonMap("foo", "bar"));
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

}
