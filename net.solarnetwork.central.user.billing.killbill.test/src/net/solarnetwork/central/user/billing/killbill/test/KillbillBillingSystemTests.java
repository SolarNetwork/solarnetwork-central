/* ==================================================================
 * KillbillBillingSystemTests.java - 28/08/2017 6:02:24 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.InvoiceFilterCommand;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.killbill.KillbillBillingSystem;
import net.solarnetwork.central.user.billing.killbill.KillbillClient;
import net.solarnetwork.central.user.billing.killbill.UserDataProperties;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Invoice;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * Test cases for the {@link KillbillBillingSystem} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillBillingSystemTests {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_USER_EMAIL = "test@localhost";
	private static final String TEST_ACCOUNT_KEY = "TEST_KEY";
	private static final String TEST_ACCOUNT_ID = UUID.randomUUID().toString();
	private static final String TEST_INVOICE_ID = UUID.randomUUID().toString();

	private KillbillClient client;
	private UserDao userDao;
	private KillbillBillingSystem system;

	@Before
	public void setup() {
		client = EasyMock.createMock(KillbillClient.class);
		userDao = EasyMock.createMock(UserDao.class);
		system = new KillbillBillingSystem(client, userDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(client, userDao);
	}

	private void replayAll() {
		EasyMock.replay(client, userDao);
	}

	private static void assertFilterResults(FilterResults<?> results, Integer returnedCount,
			Integer startingOffset, Long totalCount, Collection<?> data) {
		assertThat("Results", results, notNullValue());
		assertThat("Result returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Result returned count", results.getStartingOffset(), equalTo(0));
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		List<?> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Result list", resultList, equalTo(data));
	}

	@Test
	public void accountingSystemKey() {
		replayAll();
		assertThat("Accounting key", system.getAccountingSystemKey(),
				equalTo(KillbillBillingSystem.ACCOUNTING_SYSTEM_KEY));
	}

	@Test
	public void infoNoLocale() {
		replayAll();
		BillingSystemInfo info = system.getInfo(null);
		assertThat("Info", info, notNullValue());
		assertThat("Info system key", info.getAccountingSystemKey(),
				equalTo(KillbillBillingSystem.ACCOUNTING_SYSTEM_KEY));
	}

	@Test
	public void findUnpaidInvoices() {
		// given
		InvoiceFilterCommand filter = new InvoiceFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setUnpaid(true);

		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.putInternalDataValue(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_ACCOUNT_KEY);
		expect(userDao.get(TEST_USER_ID)).andReturn(user);

		Account account = new Account(TEST_ACCOUNT_ID);
		expect(client.accountForExternalKey(TEST_ACCOUNT_KEY)).andReturn(account);

		List<Invoice> invoices = Collections.singletonList(new Invoice(TEST_INVOICE_ID));
		expect(client.listInvoices(account, true)).andReturn(invoices);

		// when
		replayAll();

		FilterResults<InvoiceMatch> results = system.findFilteredInvoices(filter, null, null, null);

		// then
		assertFilterResults(results, 1, 0, 1L, invoices);
	}

}
