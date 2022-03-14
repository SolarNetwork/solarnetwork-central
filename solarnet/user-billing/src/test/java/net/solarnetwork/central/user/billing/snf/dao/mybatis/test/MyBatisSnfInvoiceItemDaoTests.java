/* ==================================================================
 * MyBatisSnfInvoiceItemDaoTests.java - 21/07/2020 5:04:41 PM
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
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;

/**
 * Test cases for the {@link MyBatisSnfInvoiceItemDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisSnfInvoiceItemDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_PROD_KEY = UUID.randomUUID().toString();

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisSnfInvoiceDao invoiceDao;
	private MyBatisSnfInvoiceItemDao dao;

	private SnfInvoiceItem last;

	@Before
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		invoiceDao = new MyBatisSnfInvoiceDao();
		invoiceDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisSnfInvoiceItemDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		last = null;
	}

	private SnfInvoice createTestInvoice() {
		Address address = addressDao.get(addressDao.save(createTestAddress()));
		Account account = accountDao.get(accountDao.save(createTestAccount(address)));
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

		SnfInvoiceItem entity = SnfInvoiceItem.newItem(invoice.getId().getId(), InvoiceItemType.Fixed,
				TEST_PROD_KEY, BigDecimal.ONE, new BigDecimal("3.45"));
		UUID pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		assertAccountBalance(invoice.getAccountId(), entity.getAmount(), BigDecimal.ZERO);
		last = entity;
	}

	@Test
	public void getByPK() {
		insert();
		SnfInvoiceItem entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("InvoiceImpl sameness", entity.isSameAs(last), equalTo(true));
	}

}
