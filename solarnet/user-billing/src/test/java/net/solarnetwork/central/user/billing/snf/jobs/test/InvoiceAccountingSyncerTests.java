/* ==================================================================
 * InvoiceAccountingSyncerTests.java - 11/10/2026 3:02:52 pm
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

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceAccountingSyncer;

/**
 * Test cases for the {@link InvoiceAccountingSyncer} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class InvoiceAccountingSyncerTests {

	private static final Long USER_ID = 1L;
	private static final Long ACCOUNT_ID = 2L;
	private static final Long INVOICE_ID = 3L;

	@Mock
	private AccountDao accountDao;

	@Mock
	private SnfInvoiceDao invoiceDao;

	@Mock
	private SnfAccountingSystem accountingSystem;

	private InvoiceAccountingSyncer syncer;

	@BeforeEach
	public void setup() {
		syncer = new InvoiceAccountingSyncer(accountDao, invoiceDao, accountingSystem);
	}

	private static AccountTask task(Map<String, Object> data) {
		return AccountTask.newTask(Instant.now(), AccountTaskType.SyncInvoiceToAccounting, ACCOUNT_ID,
				data);
	}

	@Test
	public void sync() {
		// GIVEN
		final Account account = new Account(ACCOUNT_ID, USER_ID, Instant.now(), "NZD", "en-NZ");
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		final SnfInvoice invoice = new SnfInvoice(INVOICE_ID, USER_ID, ACCOUNT_ID, Instant.now(),
				LocalDate.now(), LocalDate.now(), "NZD");
		given(invoiceDao.get(new UserLongCompositePK(USER_ID, INVOICE_ID))).willReturn(invoice);
		given(accountingSystem.syncInvoice(invoice, account)).willReturn(true);

		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", INVOICE_ID, "userId", USER_ID)));

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(accountingSystem).should().syncInvoice(invoice, account);
		// @formatter:on
	}

	@Test
	public void sync_stringParameters() {
		// GIVEN
		final Account account = new Account(ACCOUNT_ID, USER_ID, Instant.now(), "NZD", "en-NZ");
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		final SnfInvoice invoice = new SnfInvoice(INVOICE_ID, USER_ID, ACCOUNT_ID, Instant.now(),
				LocalDate.now(), LocalDate.now(), "NZD");
		given(invoiceDao.get(new UserLongCompositePK(USER_ID, INVOICE_ID))).willReturn(invoice);

		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", "3", "userId", "1")));

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(accountingSystem).should().syncInvoice(invoice, account);
		// @formatter:on
	}

	@Test
	public void missingParameter() {
		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", INVOICE_ID)));

		// THEN
		// @formatter:off
		and.then(result).as("Task with no user dropped").isTrue();
		then(accountingSystem).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void invoiceNotFound() {
		// GIVEN
		final Account account = new Account(ACCOUNT_ID, USER_ID, Instant.now(), "NZD", "en-NZ");
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", INVOICE_ID, "userId", USER_ID)));

		// THEN
		// @formatter:off
		and.then(result).as("Task for unknown invoice dropped").isTrue();
		then(accountingSystem).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void accountingSystemUnavailable() {
		// GIVEN
		final Account account = new Account(ACCOUNT_ID, USER_ID, Instant.now(), "NZD", "en-NZ");
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		final SnfInvoice invoice = new SnfInvoice(INVOICE_ID, USER_ID, ACCOUNT_ID, Instant.now(),
				LocalDate.now(), LocalDate.now(), "NZD");
		given(invoiceDao.get(any())).willReturn(invoice);
		given(accountingSystem.syncInvoice(invoice, account))
				.willThrow(new RepeatableTaskException("Book open."));

		// WHEN
		// @formatter:off
		thenExceptionOfType(RepeatableTaskException.class)
			.as("Exception passed on, so the job tries again later")
			.isThrownBy(() -> syncer.handleTask(task(Map.of("id", INVOICE_ID, "userId", USER_ID))))
			;
		// @formatter:on
	}

}
