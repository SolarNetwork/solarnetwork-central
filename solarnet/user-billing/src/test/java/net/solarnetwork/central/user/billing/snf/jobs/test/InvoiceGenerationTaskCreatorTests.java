/* ==================================================================
 * InvoiceGenerationTaskCreatorTests.java - 21/07/2020 10:18:28 AM
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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.UserFilter;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerationTaskCreator;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.central.user.domain.UserMatch;

/**
 * Test cases for the {@link InvoiceGenerationTaskCreator} class.
 * 
 * @author matt
 * @version 2.0
 */
public class InvoiceGenerationTaskCreatorTests {

	private static final Long TEST_USER_ID = 1L;
	private static final String TEST_EMAIL = "test@localhost";

	private UserDao userDao;
	private SnfInvoicingSystem invoicingSystem;
	private AccountTaskDao accountTaskDao;
	private InvoiceGenerationTaskCreator creator;

	@Before
	public void setup() {
		userDao = EasyMock.createMock(UserDao.class);
		invoicingSystem = EasyMock.createMock(SnfInvoicingSystem.class);
		accountTaskDao = EasyMock.createMock(AccountTaskDao.class);

		creator = new InvoiceGenerationTaskCreator(userDao, invoicingSystem, accountTaskDao);
	}

	private void replayAll() {
		EasyMock.replay(userDao, invoicingSystem, accountTaskDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(userDao, invoicingSystem, accountTaskDao);
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
	public void generateInitialInvoiceGenerationTask_oneAccount_nz() {
		// GIVEN
		final LocalDate endDate = LocalDate.of(2020, 1, 1);
		final Capture<UserFilter> userFilterCaptor = new Capture<>();
		final UserMatch user = new UserMatch(TEST_USER_ID, TEST_EMAIL);
		final FilterResults<UserFilterMatch> userMatches = new BasicFilterResults<>(asList(user));

		// find users configured with SNF billing
		expect(userDao.findFiltered(capture(userFilterCaptor), isNull(), eq(0),
				eq(InvoiceGenerationTaskCreator.DEFAULT_BATCH_SIZE))).andReturn(userMatches);

		// get Account for found user
		final Account account = createAccount(TEST_USER_ID, "en_NZ",
				createAddress("NZ", "Pacific/Auckland"));
		expect(invoicingSystem.accountForUser(TEST_USER_ID)).andReturn(account);

		// get latest invoice for account (there is none)
		expect(invoicingSystem.findLatestInvoiceForAccount(account.getId())).andReturn(null);

		// create task for month ending on endDate
		final Capture<AccountTask> taskCaptor = new Capture<>();
		expect(accountTaskDao.save(capture(taskCaptor))).andReturn(null);

		// WHEN
		replayAll();
		creator.createTasks(endDate);

		// THEN
		UserFilter userFilter = userFilterCaptor.getValue();
		assertThat("User filter queried for SNF accounts", userFilter.getInternalData(), hasEntry(
				BillingDataConstants.ACCOUNTING_DATA_PROP, SnfBillingSystem.ACCOUNTING_SYSTEM_KEY));

		AccountTask task = taskCaptor.getValue();
		assertThat("Task created", task, notNullValue());
		assertThat("Task type", task.getTaskType(), equalTo(AccountTaskType.GenerateInvoice));
		assertThat("Task account", task.getAccountId(), equalTo(account.getId().getId()));
		assertThat("Task date", task.getCreated(),
				equalTo(endDate.atStartOfDay(account.getTimeZone()).minusMonths(1).toInstant()));
		assertThat("Task data not set", task.getTaskData(), nullValue());
	}

	@Test
	public void generateInitialInvoiceGenerationTask_oneAccount_us() {
		// GIVEN
		final LocalDate endDate = LocalDate.of(2020, 1, 1);
		final Capture<UserFilter> userFilterCaptor = new Capture<>();
		final UserMatch user = new UserMatch(TEST_USER_ID, TEST_EMAIL);
		final FilterResults<UserFilterMatch> userMatches = new BasicFilterResults<>(asList(user));

		// find users configured with SNF billing
		expect(userDao.findFiltered(capture(userFilterCaptor), isNull(), eq(0),
				eq(InvoiceGenerationTaskCreator.DEFAULT_BATCH_SIZE))).andReturn(userMatches);

		// get Account for found user
		final Account account = createAccount(TEST_USER_ID, "en_US",
				createAddress("US", "America/Los_Angeles"));
		expect(invoicingSystem.accountForUser(TEST_USER_ID)).andReturn(account);

		// get latest invoice for account (there is none)
		expect(invoicingSystem.findLatestInvoiceForAccount(account.getId())).andReturn(null);

		// create task for month ending on endDate
		final Capture<AccountTask> taskCaptor = new Capture<>();
		expect(accountTaskDao.save(capture(taskCaptor))).andReturn(null);

		// WHEN
		replayAll();
		creator.createTasks(endDate);

		// THEN
		UserFilter userFilter = userFilterCaptor.getValue();
		assertThat("User filter queried for SNF accounts", userFilter.getInternalData(), hasEntry(
				BillingDataConstants.ACCOUNTING_DATA_PROP, SnfBillingSystem.ACCOUNTING_SYSTEM_KEY));

		AccountTask task = taskCaptor.getValue();
		assertThat("Task created", task, notNullValue());
		assertThat("Task type", task.getTaskType(), equalTo(AccountTaskType.GenerateInvoice));
		assertThat("Task account", task.getAccountId(), equalTo(account.getId().getId()));
		assertThat("Task date", task.getCreated(),
				equalTo(endDate.atStartOfDay(account.getTimeZone()).minusMonths(1).toInstant()));
		assertThat("Task data not set", task.getTaskData(), nullValue());
	}

	@Test
	public void generateCatchUpInvoiceGenerationTasks_oneAccount_utc() {
		// GIVEN
		final LocalDate endDate = LocalDate.of(2020, 1, 1);
		final Capture<UserFilter> userFilterCaptor = new Capture<>();
		final UserMatch user = new UserMatch(TEST_USER_ID, TEST_EMAIL);
		final FilterResults<UserFilterMatch> userMatches = new BasicFilterResults<>(asList(user));

		// find users configured with SNF billing
		expect(userDao.findFiltered(capture(userFilterCaptor), isNull(), eq(0),
				eq(InvoiceGenerationTaskCreator.DEFAULT_BATCH_SIZE))).andReturn(userMatches);

		// get Account for found user
		final Account account = createAccount(TEST_USER_ID, "en_GB", createAddress("GB", "UTC"));
		expect(invoicingSystem.accountForUser(TEST_USER_ID)).andReturn(account);

		// get latest invoice for account, which is a few months behind
		SnfInvoice lastInvoice = new SnfInvoice(randomUUID().getMostSignificantBits(),
				account.getUserId(), account.getId().getId(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		lastInvoice.setStartDate(LocalDate.of(2019, 9, 1));
		lastInvoice.setEndDate(LocalDate.of(2019, 10, 1));
		lastInvoice.setAddress(account.getAddress());
		expect(invoicingSystem.findLatestInvoiceForAccount(account.getId())).andReturn(lastInvoice);

		// create tasks for months ending between lastInvoice.endDate and endDate (2019-11 - 2020-01)
		final Capture<AccountTask> taskCaptor = new Capture<>(CaptureType.ALL);
		for ( int i = 0; i < 3; i++ ) {
			expect(accountTaskDao.save(capture(taskCaptor))).andReturn(null);
		}

		// WHEN
		replayAll();
		creator.createTasks(endDate);

		// THEN
		UserFilter userFilter = userFilterCaptor.getValue();
		assertThat("User filter queried for SNF accounts", userFilter.getInternalData(), hasEntry(
				BillingDataConstants.ACCOUNTING_DATA_PROP, SnfBillingSystem.ACCOUNTING_SYSTEM_KEY));

		List<AccountTask> tasks = taskCaptor.getValues();
		assertThat("3 tasks created for 3 month range", tasks, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			AccountTask task = tasks.get(i);
			assertThat(format("Task %d created", i), task, notNullValue());
			assertThat(format("Task %d type", i), task.getTaskType(),
					equalTo(AccountTaskType.GenerateInvoice));
			assertThat(format("Task %d account", i), task.getAccountId(),
					equalTo(account.getId().getId()));
			assertThat(format("Task %d date", i), task.getCreated(), equalTo(
					endDate.atStartOfDay(account.getTimeZone()).minusMonths((2 - i) + 1).toInstant()));
			assertThat(format("Task %d data not set", i), task.getTaskData(), nullValue());
		}
	}

}
