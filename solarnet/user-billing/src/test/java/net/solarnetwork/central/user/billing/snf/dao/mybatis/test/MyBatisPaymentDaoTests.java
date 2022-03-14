/* ==================================================================
 * MyBatisPaymentDaoTests.java - 29/07/2020 7:30:11 AM
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

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisPaymentDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.billing.snf.domain.PaymentType;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisPaymentDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisPaymentDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisPaymentDao dao;

	private Address address;
	private Account account;
	private Payment last;

	@Before
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisPaymentDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		last = null;

		address = addressDao.get(addressDao.save(createTestAddress()));
		account = accountDao.get(accountDao.save(createTestAccount(address)));
	}

	@Test
	public void insert() {
		Payment entity = new Payment(randomUUID(), account.getUserId(), account.getId().getId(), now());
		entity.setAmount(new BigDecimal("12345.67"));
		entity.setCurrencyCode(account.getCurrencyCode());
		entity.setExternalKey(randomUUID().toString());
		entity.setPaymentType(PaymentType.Payment);
		entity.setReference(randomUUID().toString());

		UserUuidPK pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		assertAccountBalance(entity.getAccountId(), BigDecimal.ZERO, entity.getAmount());
		last = entity;
	}

	@Test
	public void getByPK() {
		insert();
		Payment entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Entity sameness", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void filterForUser_sortDefault() {
		// GIVEN
		List<Payment> entities = new ArrayList<>(5);
		LocalDate date = LocalDate.of(2020, 1, 5);
		for ( int i = 0; i < 5; i++ ) {
			Payment entity = new Payment(randomUUID(), account.getUserId(), account.getId().getId(),
					date.atStartOfDay(address.getTimeZone()).toInstant());
			entity.setAmount(new BigDecimal(Math.random() * 1000.0).setScale(2, RoundingMode.HALF_UP));
			entity.setCurrencyCode(account.getCurrencyCode());
			entity.setExternalKey(randomUUID().toString());
			entity.setPaymentType(PaymentType.Payment);
			entity.setReference(randomUUID().toString());
			dao.save(entity);
			entities.add(entity);
			date = date.plusMonths(1);
		}

		// WHEN
		PaymentFilter filter = PaymentFilter.forUser(account.getUserId());
		final FilterResults<Payment, UserUuidPK> result = dao.findFiltered(filter, null, null, null);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result count", result.getReturnedResultCount(), equalTo(5));
		assertThat("Total results unknown", result.getTotalResults(), nullValue());

		List<Payment> expectedPayments = entities.stream()
				.sorted(Collections.reverseOrder(Payment.SORT_BY_DATE)).collect(Collectors.toList());

		List<Payment> payments = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned results", payments, hasSize(expectedPayments.size()));
		for ( int i = 0; i < expectedPayments.size(); i++ ) {
			Payment payment = payments.get(i);
			Payment expected = expectedPayments.get(i);
			assertThat(format("Payment %d returned in order", i), payment, equalTo(expected));
			assertThat(format("Payment %d data preserved", i), payment.isSameAs(expected),
					equalTo(true));
		}
	}

}
