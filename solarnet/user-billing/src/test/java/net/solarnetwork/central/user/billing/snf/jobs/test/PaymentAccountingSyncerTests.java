/* ==================================================================
 * PaymentAccountingSyncerTests.java - 14/10/2026 11:02:44 am
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.PaymentDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.InvoicePayment;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentType;
import net.solarnetwork.central.user.billing.snf.jobs.PaymentAccountingSyncer;

/**
 * Test cases for the {@link PaymentAccountingSyncer} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class PaymentAccountingSyncerTests {

	private static final Long USER_ID = 1L;
	private static final Long ACCOUNT_ID = 2L;
	private static final UUID PAYMENT_ID = UUID.randomUUID();
	private static final Long INVOICE_ID = 3L;

	@Mock
	private AccountDao accountDao;

	@Mock
	private PaymentDao paymentDao;

	@Mock
	private SnfAccountingSystem accountingSystem;

	private PaymentAccountingSyncer syncer;

	@BeforeEach
	public void setup() {
		syncer = new PaymentAccountingSyncer(accountDao, paymentDao, accountingSystem);
	}

	private static AccountTask task(Map<String, Object> data) {
		return AccountTask.newTask(Instant.now(), AccountTaskType.SyncPaymentToAccounting, ACCOUNT_ID,
				data);
	}

	private static Account account() {
		return new Account(ACCOUNT_ID, USER_ID, Instant.now(), "NZD", "en-NZ");
	}

	private static Payment payment() {
		return new Payment(PAYMENT_ID, USER_ID, ACCOUNT_ID, Instant.now(), PaymentType.Payment,
				new BigDecimal("12.34"), "NZD");
	}

	@Test
	public void sync() {
		// GIVEN
		final Account account = account();
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		final UserUuidPK id = new UserUuidPK(USER_ID, PAYMENT_ID);
		final Payment payment = payment();
		given(paymentDao.get(id)).willReturn(payment);

		final List<InvoicePayment> invoicePayments = List.of(new InvoicePayment(UUID.randomUUID(),
				USER_ID, ACCOUNT_ID, PAYMENT_ID, INVOICE_ID, Instant.now()));
		given(paymentDao.findInvoicePayments(id)).willReturn(invoicePayments);
		given(accountingSystem.syncPayment(payment, invoicePayments, account)).willReturn(true);

		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", PAYMENT_ID.toString(), "userId", USER_ID)));

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(accountingSystem).should().syncPayment(payment, invoicePayments, account);
		// @formatter:on
	}

	@Test
	public void sync_stringUserId() {
		// GIVEN
		final Account account = account();
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		final UserUuidPK id = new UserUuidPK(USER_ID, PAYMENT_ID);
		final Payment payment = payment();
		given(paymentDao.get(id)).willReturn(payment);
		given(paymentDao.findInvoicePayments(id)).willReturn(List.of());

		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", PAYMENT_ID.toString(), "userId", "1")));

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(accountingSystem).should().syncPayment(payment, List.of(), account);
		// @formatter:on
	}

	@Test
	public void missingParameter() {
		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", PAYMENT_ID.toString())));

		// THEN
		// @formatter:off
		and.then(result).as("Task with no user dropped").isTrue();
		then(accountingSystem).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void invalidPaymentId() {
		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", "not-a-uuid", "userId", USER_ID)));

		// THEN
		// @formatter:off
		and.then(result).as("Task with invalid payment ID dropped").isTrue();
		then(accountingSystem).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void paymentNotFound() {
		// GIVEN
		given(accountDao.getForUser(USER_ID)).willReturn(account());

		// WHEN
		boolean result = syncer.handleTask(task(Map.of("id", PAYMENT_ID.toString(), "userId", USER_ID)));

		// THEN
		// @formatter:off
		and.then(result).as("Task for unknown payment dropped").isTrue();
		then(accountingSystem).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void accountingSystemUnavailable() {
		// GIVEN
		final Account account = account();
		given(accountDao.getForUser(USER_ID)).willReturn(account);

		final Payment payment = payment();
		given(paymentDao.get(any())).willReturn(payment);
		given(paymentDao.findInvoicePayments(any())).willReturn(List.of());
		given(accountingSystem.syncPayment(payment, List.of(), account))
				.willThrow(new RepeatableTaskException("Book open."));

		// WHEN
		// @formatter:off
		thenExceptionOfType(RepeatableTaskException.class)
			.as("Exception passed on, so the job tries again later")
			.isThrownBy(() -> syncer.handleTask(task(Map.of("id", PAYMENT_ID.toString(),
					"userId", USER_ID))))
			;
		// @formatter:on
	}

}
