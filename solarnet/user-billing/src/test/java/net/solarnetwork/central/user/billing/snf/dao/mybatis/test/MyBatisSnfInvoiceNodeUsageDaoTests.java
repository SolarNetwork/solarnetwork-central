/* ==================================================================
 * MyBatisSnfInvoiceNodeUsageDaoTests.java - 28/05/2021 8:37:45 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceNodeUsage.nodeUsage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceNodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceRelatedPK;

/**
 * Test cases for the {@link MyBatisSnfInvoiceNodeUsageDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class MyBatisSnfInvoiceNodeUsageDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_NODE_DESCRIPTION = "My Test Node";
	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisSnfInvoiceDao invoiceDao;
	private MyBatisSnfInvoiceNodeUsageDao dao;

	private SnfInvoiceNodeUsage last;

	@BeforeEach
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		invoiceDao = new MyBatisSnfInvoiceDao();
		invoiceDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisSnfInvoiceNodeUsageDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		setupTestNode();

		last = null;
	}

	private SnfInvoice createTestInvoice() {
		Address address = addressDao.get(addressDao.save(createTestAddress()));
		Account account = accountDao.get(accountDao.save(createTestAccount(address)));

		setupTestUser(account.getUserId());
		setupTestUserNode(account.getUserId(), TEST_NODE_ID);
		saveNodeName(TEST_NODE_ID, TEST_NODE_DESCRIPTION);

		SnfInvoice entity = new SnfInvoice(account.getId().getId(), account.getUserId(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		entity.setAddress(address);
		entity.setCurrencyCode("NZD");
		entity.setStartDate(LocalDate.of(2019, 12, 1));
		entity.setEndDate(LocalDate.of(2020, 1, 1));
		return invoiceDao.get(invoiceDao.save(entity));
	}

	@Test
	public void insert() {
		SnfInvoice invoice = createTestInvoice();

		SnfInvoiceNodeUsage entity = nodeUsage(invoice.getId().getId(), TEST_NODE_ID,
				invoice.getCreated(), 2L, 3L, 4L, 5L, 6L);
		SnfInvoiceRelatedPK pk = dao.save(entity);
		assertThat("PK preserved", pk, is(equalTo(entity.getId())));
		last = entity;
	}

	@Test
	public void getByPK() {
		insert();
		SnfInvoiceNodeUsage entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), is(equalTo(last.getId())));
		assertThat("Created", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Sameness", entity.isSameAs(last), is(equalTo(true)));
		assertThat("Description populated with node name", entity.getDescription(),
				is(equalTo(TEST_NODE_DESCRIPTION)));
	}

}
