/* ==================================================================
 * InvoiceGeneratorTests.java - 20/07/2020 3:14:22 PM
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

package net.solarnetwork.central.user.billing.snf.jobs.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.defaultOptions;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerator;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Test cases for the {@link InvoiceGenerator} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceGeneratorTests {

	private static final Long TEST_USER_ID = 1L;

	private AccountDao accountDao;
	private AccountTaskDao taskDao;
	private SnfInvoicingSystem invoicingSystem;
	private InvoiceGenerator generator;

	@Before
	public void setup() {
		accountDao = EasyMock.createMock(AccountDao.class);
		taskDao = EasyMock.createMock(AccountTaskDao.class);
		invoicingSystem = EasyMock.createMock(SnfInvoicingSystem.class);

		generator = new InvoiceGenerator(accountDao, taskDao, invoicingSystem);
	}

	private void replayAll() {
		EasyMock.replay(accountDao, taskDao, invoicingSystem);
	}

	@After
	public void teardown() {
		EasyMock.verify(accountDao, taskDao, invoicingSystem);
	}

	private static Address createAddress(String country, String timeZoneId) {
		final Address addr = new Address(randomUUID().getMostSignificantBits(), Instant.now());
		addr.setCountry(country);
		addr.setTimeZoneId(timeZoneId);
		return addr;
	}

	private static Account createAccount(Long userId, String locale, Address address) {
		final Account account = new Account(randomUUID().getMostSignificantBits(), userId,
				Instant.now());
		account.setLocale(locale);
		account.setAddress(address);
		return account;
	}

	@Test
	public void generateInitialInvoices_oneAccount_nz() {
		// GIVEN
		final LocalDate date = LocalDate.of(2019, 12, 1);

		// get account
		final Account account = createAccount(TEST_USER_ID, "en_NZ",
				createAddress("NZ", "Pacific/Auckland"));
		expect(accountDao.get(new UserLongPK(null, account.getId().getId()))).andReturn(account);

		// generate invoice for month ending on endDate
		SnfInvoice generatedInvoice = new SnfInvoice(randomUUID().getMostSignificantBits(),
				account.getUserId(), account.getId().getId(), Instant.now());
		expect(invoicingSystem.generateInvoice(TEST_USER_ID, date, date.plusMonths(1), defaultOptions()))
				.andReturn(generatedInvoice);

		// create "deliver invoice" task
		Capture<AccountTask> deliverTaskCaptor = new Capture<>();
		expect(taskDao.save(capture(deliverTaskCaptor))).andReturn(UUID.randomUUID());

		// WHEN
		replayAll();
		boolean result = generator
				.handleTask(AccountTask.newTask(date.atStartOfDay(account.getTimeZone()).toInstant(),
						AccountTaskType.GenerateInvoice, account.getId().getId()));

		// THEN
		assertThat("Task handled", result, equalTo(true));
		AccountTask deliverTask = deliverTaskCaptor.getValue();
		assertThat("Deliver task created", deliverTask, notNullValue());
		assertThat("Deliver task ID assigned", deliverTask.getId(), notNullValue());
		assertThat("Deliver task account same as generated invoice", deliverTask.getAccountId(),
				equalTo(account.getId().getId()));
		assertThat("Deliver task type is deliver", deliverTask.getTaskType(),
				equalTo(AccountTaskType.DeliverInvoice));
		assertThat("Deliver task ", deliverTask.getTaskData(),
				allOf(hasEntry("id", generatedInvoice.getId().getId()),
						hasEntry("userId", account.getUserId())));
	}

	@Test
	public void generateInitialInvoices_oneAccount_us() {
		// GIVEN
		final LocalDate date = LocalDate.of(2019, 12, 1);

		// get account
		final Account account = createAccount(TEST_USER_ID, "en_US",
				createAddress("US", "America/Los_Angeles"));
		expect(accountDao.get(new UserLongPK(null, account.getId().getId()))).andReturn(account);

		// generate invoice for month ending on endDate
		SnfInvoice generatedInvoice = new SnfInvoice(randomUUID().getMostSignificantBits(),
				account.getUserId(), account.getId().getId(), Instant.now());
		expect(invoicingSystem.generateInvoice(TEST_USER_ID, date, date.plusMonths(1), defaultOptions()))
				.andReturn(generatedInvoice);

		// create "deliver invoice" task
		Capture<AccountTask> deliverTaskCaptor = new Capture<>();
		expect(taskDao.save(capture(deliverTaskCaptor))).andReturn(UUID.randomUUID());

		// WHEN
		replayAll();
		boolean result = generator
				.handleTask(AccountTask.newTask(date.atStartOfDay(account.getTimeZone()).toInstant(),
						AccountTaskType.GenerateInvoice, account.getId().getId()));

		// THEN
		assertThat("Task handled", result, equalTo(true));
		AccountTask deliverTask = deliverTaskCaptor.getValue();
		assertThat("Deliver task created", deliverTask, notNullValue());
		assertThat("Deliver task ID assigned", deliverTask.getId(), notNullValue());
		assertThat("Deliver task account same as generated invoice", deliverTask.getAccountId(),
				equalTo(account.getId().getId()));
		assertThat("Deliver task type is deliver", deliverTask.getTaskType(),
				equalTo(AccountTaskType.DeliverInvoice));
		assertThat("Deliver task ", deliverTask.getTaskData(),
				allOf(hasEntry("id", generatedInvoice.getId().getId()),
						hasEntry("userId", account.getUserId())));
	}

}
