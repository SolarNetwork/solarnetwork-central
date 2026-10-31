/* ==================================================================
 * InvoiceGenerator_AccountingSyncTests.java - 11/10/2026 3:02:52 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.defaultOptions;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerator;

/**
 * Test cases for the {@link InvoiceGenerator} class adding tasks to record
 * invoices in the accounting system.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class InvoiceGenerator_AccountingSyncTests {

	private static final Long USER_ID = 1L;
	private static final Long ACCOUNT_ID = 2L;
	private static final Long INVOICE_ID = 3L;

	@Mock
	private AccountDao accountDao;

	@Mock
	private AccountTaskDao taskDao;

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Captor
	private ArgumentCaptor<AccountTask> taskCaptor;

	private InvoiceGenerator generator;

	@BeforeEach
	public void setup() {
		generator = new InvoiceGenerator(accountDao, taskDao, invoicingSystem);
	}

	@Test
	public void generate_syncEnabled() {
		// GIVEN
		generator.setSyncInvoicesToAccounting(true);

		final LocalDate date = LocalDate.of(2026, 9, 1);
		final Address address = new Address(USER_ID, "Tester", "test@localhost", "NZ",
				"Pacific/Auckland");
		final Account account = new Account(ACCOUNT_ID, USER_ID, Instant.now(), "NZD", "en-NZ");
		account.setAddress(address);
		given(accountDao.getForAccountId(ACCOUNT_ID)).willReturn(account);

		final SnfInvoice invoice = new SnfInvoice(INVOICE_ID, USER_ID, ACCOUNT_ID, Instant.now(), date,
				date.plusMonths(1), "NZD");
		given(invoicingSystem.generateInvoice(USER_ID, date, date.plusMonths(1), defaultOptions()))
				.willReturn(invoice);
		given(taskDao.save(any())).willReturn(UUID.randomUUID());

		// WHEN
		boolean result = generator
				.handleTask(AccountTask.newTask(date.atStartOfDay(account.getTimeZone()).toInstant(),
						AccountTaskType.GenerateInvoice, ACCOUNT_ID));

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(taskDao).should(times(2)).save(taskCaptor.capture());
		and.then(taskCaptor.getAllValues())
			.as("Delivery and accounting tasks added for the invoice")
			.satisfiesExactly(
				t -> and.then(t).returns(AccountTaskType.DeliverInvoice, from(AccountTask::getTaskType)),
				t -> and.then(t).returns(AccountTaskType.SyncInvoiceToAccounting, from(AccountTask::getTaskType)))
			.allSatisfy(t -> and.then(t)
				.returns(ACCOUNT_ID, from(AccountTask::getAccountId))
				.returns(Map.of("id", INVOICE_ID, "userId", USER_ID), from(AccountTask::getTaskData)))
			;
		// @formatter:on
	}

}
