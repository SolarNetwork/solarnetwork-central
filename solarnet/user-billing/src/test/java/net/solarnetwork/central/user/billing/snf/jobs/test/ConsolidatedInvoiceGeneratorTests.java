/* ==================================================================
 * ConsolidatedInvoiceGeneratorTests.java - 15/10/2026 11:41:56 am
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.jobs.ConsolidatedInvoiceGenerator;

/**
 * Test cases for the {@link ConsolidatedInvoiceGenerator} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ConsolidatedInvoiceGeneratorTests {

	private static final YearMonth MONTH = YearMonth.of(2026, 9);
	private static final LocalDate START_DATE = MONTH.atDay(1);
	private static final LocalDate END_DATE = MONTH.plusMonths(1).atDay(1);
	private static final Long PRIMARY_USER_ID = 1L;
	private static final Long PRIMARY_ACCOUNT_ID = 10L;
	private static final Long MEMBER_USER_ID = 2L;
	private static final Long MEMBER_ACCOUNT_ID = 20L;

	@Mock
	private AccountDao accountDao;

	@Mock
	private AccountTaskDao taskDao;

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Mock
	private InvoiceConsolidationResolver consolidationResolver;

	@Captor
	private ArgumentCaptor<AccountTask> taskCaptor;

	private ConsolidatedInvoiceGenerator generator;

	private Account primary;

	@BeforeEach
	public void setup() {
		generator = new ConsolidatedInvoiceGenerator(accountDao, taskDao, invoicingSystem,
				consolidationResolver);

		primary = new Account(PRIMARY_ACCOUNT_ID, PRIMARY_USER_ID, Instant.now(), "NZD", "en-NZ");
		primary.setAddress(
				new Address(PRIMARY_USER_ID, "Primary", "primary@localhost", "NZ", "Pacific/Auckland"));
	}

	private static AccountTask task() {
		return AccountTask.newTask(Instant.now(), AccountTaskType.GenerateConsolidatedInvoice,
				PRIMARY_ACCOUNT_ID, ConsolidatedInvoiceGenerator.taskData(MONTH));
	}

	private static SnfConsolidatedInvoice consolidatedInvoice() {
		SnfInvoice primaryInvoice = new SnfInvoice(100L, PRIMARY_USER_ID, PRIMARY_ACCOUNT_ID,
				Instant.now(), START_DATE, END_DATE, "NZD");
		SnfInvoice memberInvoice = new SnfInvoice(101L, MEMBER_USER_ID, MEMBER_ACCOUNT_ID, Instant.now(),
				START_DATE, END_DATE, "NZD");
		SnfConsolidatedInvoice invoice = new SnfConsolidatedInvoice(200L, PRIMARY_USER_ID,
				PRIMARY_ACCOUNT_ID, Instant.now(), START_DATE, END_DATE, "NZD");
		invoice.setInvoices(List.of(primaryInvoice, memberInvoice));
		return invoice;
	}

	private void givenGroup(boolean deliverMemberInvoices) {
		final InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(PRIMARY_USER_ID,
				List.of(PRIMARY_USER_ID, MEMBER_USER_ID), deliverMemberInvoices);
		given(consolidationResolver.groupForUser(PRIMARY_USER_ID)).willReturn(group);
		given(invoicingSystem.generateConsolidatedInvoice(group, MONTH, defaultOptions()))
				.willReturn(consolidatedInvoice());
		given(taskDao.save(any())).willReturn(UUID.randomUUID());
	}

	@Test
	public void generate() {
		// GIVEN
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);
		givenGroup(false);

		// WHEN
		boolean result = generator.handleTask(task());

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(taskDao).should().save(taskCaptor.capture());
		and.then(taskCaptor.getValue())
			.as("Consolidated invoice delivered to the primary account")
			.returns(AccountTaskType.DeliverConsolidatedInvoice, from(AccountTask::getTaskType))
			.returns(PRIMARY_ACCOUNT_ID, from(AccountTask::getAccountId))
			.returns(Map.of("userId", PRIMARY_USER_ID, "id", 200L), from(AccountTask::getTaskData))
			;
		// @formatter:on
	}

	@Test
	public void generate_deliverMemberInvoices_syncToAccounting() {
		// GIVEN
		generator.setSyncInvoicesToAccounting(true);
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);
		givenGroup(true);

		// WHEN
		boolean result = generator.handleTask(task());

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		then(taskDao).should(times(4)).save(taskCaptor.capture());
		and.then(taskCaptor.getAllValues())
			.as("Consolidated invoice delivered, member invoice delivered, account invoices synced")
			.satisfiesExactly(
				t -> and.then(t)
					.returns(AccountTaskType.DeliverConsolidatedInvoice, from(AccountTask::getTaskType))
					.returns(PRIMARY_ACCOUNT_ID, from(AccountTask::getAccountId))
					.returns(Map.of("userId", PRIMARY_USER_ID, "id", 200L), from(AccountTask::getTaskData)),
				t -> and.then(t)
					.returns(AccountTaskType.SyncInvoiceToAccounting, from(AccountTask::getTaskType))
					.returns(PRIMARY_ACCOUNT_ID, from(AccountTask::getAccountId))
					.returns(Map.of("userId", PRIMARY_USER_ID, "id", 100L), from(AccountTask::getTaskData)),
				t -> and.then(t)
					.returns(AccountTaskType.DeliverInvoice, from(AccountTask::getTaskType))
					.returns(MEMBER_ACCOUNT_ID, from(AccountTask::getAccountId))
					.returns(Map.of("userId", MEMBER_USER_ID, "id", 101L), from(AccountTask::getTaskData)),
				t -> and.then(t)
					.returns(AccountTaskType.SyncInvoiceToAccounting, from(AccountTask::getTaskType))
					.returns(MEMBER_ACCOUNT_ID, from(AccountTask::getAccountId))
					.returns(Map.of("userId", MEMBER_USER_ID, "id", 101L), from(AccountTask::getTaskData))
			)
			;
		// @formatter:on
	}

	@Test
	public void generate_noCharges() {
		// GIVEN
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);
		final InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(PRIMARY_USER_ID,
				List.of(PRIMARY_USER_ID, MEMBER_USER_ID), false);
		given(consolidationResolver.groupForUser(PRIMARY_USER_ID)).willReturn(group);

		// WHEN
		boolean result = generator.handleTask(task());

		// THEN
		// @formatter:off
		and.then(result).as("Task handled").isTrue();
		// @formatter:on
		then(invoicingSystem).should().generateConsolidatedInvoice(group, MONTH, defaultOptions());
		then(taskDao).shouldHaveNoInteractions();
	}

	@Test
	public void generate_groupHeld() {
		// GIVEN
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);
		given(consolidationResolver.groupForUser(PRIMARY_USER_ID))
				.willReturn(new InvoiceConsolidationGroup(PRIMARY_USER_ID,
						List.of(PRIMARY_USER_ID, MEMBER_USER_ID), false,
						new DefaultMessageSourceResolvable(
								new String[] { "error.consolidated.noBillingAccount" },
								new Object[] { MEMBER_USER_ID },
								"User {0,number,#} is not enrolled for billing.")));

		// WHEN
		boolean result = generator.handleTask(task());

		// THEN
		// @formatter:off
		and.then(result).as("Task not handled while the group is invalid, so it is tried again").isFalse();
		// @formatter:on
		then(invoicingSystem).shouldHaveNoInteractions();
		then(taskDao).shouldHaveNoInteractions();
	}

	@Test
	public void generate_noGroup() {
		// GIVEN
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);
		given(consolidationResolver.groupForUser(PRIMARY_USER_ID)).willReturn(null);

		// WHEN
		boolean result = generator.handleTask(task());

		// THEN
		// @formatter:off
		and.then(result).as("Task handled without a group").isTrue();
		// @formatter:on
		then(invoicingSystem).shouldHaveNoInteractions();
		then(taskDao).shouldHaveNoInteractions();
	}

	@Test
	public void generate_noLongerPrimary() {
		// GIVEN
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);
		given(consolidationResolver.groupForUser(PRIMARY_USER_ID))
				.willReturn(new InvoiceConsolidationGroup(3L, List.of(3L, PRIMARY_USER_ID), false));

		// WHEN
		boolean result = generator.handleTask(task());

		// THEN
		// @formatter:off
		and.then(result).as("Task handled once the user is in another group, not as its primary").isTrue();
		// @formatter:on
		then(invoicingSystem).shouldHaveNoInteractions();
		then(taskDao).shouldHaveNoInteractions();
	}

	@Test
	public void generate_noMonth() {
		// GIVEN
		given(accountDao.getForAccountId(PRIMARY_ACCOUNT_ID)).willReturn(primary);

		// WHEN
		boolean result = generator.handleTask(AccountTask.newTask(Instant.now(),
				AccountTaskType.GenerateConsolidatedInvoice, PRIMARY_ACCOUNT_ID));

		// THEN
		// @formatter:off
		and.then(result).as("Invalid task handled, so it is removed").isTrue();
		// @formatter:on
		then(consolidationResolver).shouldHaveNoInteractions();
		then(invoicingSystem).should(never()).generateConsolidatedInvoice(any(), any(), any());
	}

}
