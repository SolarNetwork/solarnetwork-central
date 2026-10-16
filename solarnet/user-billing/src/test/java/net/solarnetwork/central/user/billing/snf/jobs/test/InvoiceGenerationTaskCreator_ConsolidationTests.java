/* ==================================================================
 * InvoiceGenerationTaskCreator_ConsolidationTests.java - 15/10/2026 11:58:32 am
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
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the {@link InvoiceGenerationTaskCreator} class with invoice
 * consolidation groups.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class InvoiceGenerationTaskCreator_ConsolidationTests {

	private static final ZoneId NZ = ZoneId.of("Pacific/Auckland");
	private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

	@Mock
	private UserDao userDao;

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Mock
	private AccountTaskDao accountTaskDao;

	@Mock
	private InvoiceConsolidationResolver consolidationResolver;

	@Captor
	private ArgumentCaptor<AccountTask> taskCaptor;

	private InvoiceGenerationTaskCreator creator;

	@BeforeEach
	public void setup() {
		creator = new InvoiceGenerationTaskCreator(userDao, invoicingSystem, accountTaskDao);
		creator.setConsolidationResolver(consolidationResolver);
	}

	private void givenUsers(Long... userIds) {
		List<UserFilterMatch> users = Arrays.stream(userIds)
				.map(id -> (UserFilterMatch) new UserMatch(id, "user%d@localhost".formatted(id)))
				.toList();
		given(userDao.findFiltered(any(), isNull(), eq(0L),
				eq(InvoiceGenerationTaskCreator.DEFAULT_BATCH_SIZE)))
				.willReturn(new BasicFilterResults<>(users));
	}

	private Account givenAccount(Long userId, ZoneId zone, LocalDate invoicedThrough) {
		Address address = new Address(userId, "User " + userId, "user@localhost", "NZ",
				zone.getId());
		Account account = new Account(userId * 10, userId, Instant.now(), "NZD", "en-NZ");
		account.setAddress(address);
		given(invoicingSystem.accountForUser(userId)).willReturn(account);

		SnfInvoice latest = null;
		if ( invoicedThrough != null ) {
			latest = new SnfInvoice(userId * 100, userId, account.getAccountId(), Instant.now(),
					invoicedThrough.minusMonths(1), invoicedThrough, "NZD");
			latest.setAddress(address);
		}
		given(invoicingSystem.findLatestInvoiceForAccount(account.id())).willReturn(latest);
		return account;
	}

	@Test
	public void groupPrimary_oneTaskPerMonth() {
		// GIVEN
		final LocalDate endDate = LocalDate.of(2026, 11, 1);
		givenUsers(1L, 2L, 3L);

		final InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(1L, List.of(1L, 2L),
				false);
		given(consolidationResolver.groupForUser(1L)).willReturn(group);
		given(consolidationResolver.groupForUser(2L)).willReturn(group);

		// the primary needs October, the member September and October
		final Account primary = givenAccount(1L, NZ, LocalDate.of(2026, 10, 1));
		givenAccount(2L, LA, LocalDate.of(2026, 9, 1));

		// a user not in a group still needs October
		final Account other = givenAccount(3L, NZ, LocalDate.of(2026, 10, 1));

		// WHEN
		creator.createTasks(endDate);

		// THEN
		then(accountTaskDao).should(times(3)).save(taskCaptor.capture());
		// @formatter:off
		and.then(taskCaptor.getAllValues())
			.as("One consolidated task per month for the group, an individual task for the other user")
			.satisfiesExactly(
				t -> and.then(t)
					.returns(AccountTaskType.GenerateConsolidatedInvoice, from(AccountTask::getTaskType))
					.returns(primary.getAccountId(), from(AccountTask::getAccountId))
					.returns(LocalDate.of(2026, 9, 1).atStartOfDay(NZ).toInstant(), from(AccountTask::getCreated))
					.returns(Map.of("month", "2026-09"), from(AccountTask::getTaskData)),
				t -> and.then(t)
					.returns(AccountTaskType.GenerateConsolidatedInvoice, from(AccountTask::getTaskType))
					.returns(primary.getAccountId(), from(AccountTask::getAccountId))
					.returns(LocalDate.of(2026, 10, 1).atStartOfDay(NZ).toInstant(), from(AccountTask::getCreated))
					.returns(Map.of("month", "2026-10"), from(AccountTask::getTaskData)),
				t -> and.then(t)
					.returns(AccountTaskType.GenerateInvoice, from(AccountTask::getTaskType))
					.returns(other.getAccountId(), from(AccountTask::getAccountId))
					.returns(LocalDate.of(2026, 10, 1).atStartOfDay(NZ).toInstant(), from(AccountTask::getCreated))
			)
			;
		// @formatter:on
	}

	@Test
	public void groupUpToDate() {
		// GIVEN
		final LocalDate endDate = LocalDate.of(2026, 11, 1);
		givenUsers(1L, 2L);

		final InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(1L, List.of(1L, 2L),
				false);
		given(consolidationResolver.groupForUser(1L)).willReturn(group);
		given(consolidationResolver.groupForUser(2L)).willReturn(group);
		givenAccount(1L, NZ, endDate);
		givenAccount(2L, NZ, endDate);

		// WHEN
		creator.createTasks(endDate);

		// THEN
		then(accountTaskDao).shouldHaveNoInteractions();
	}

	@Test
	public void heldGroup_noTasks() {
		// GIVEN
		final LocalDate endDate = LocalDate.of(2026, 11, 1);
		givenUsers(1L, 2L);
		final InvoiceConsolidationGroup group = new InvoiceConsolidationGroup(1L, List.of(1L, 2L),
				false,
				new DefaultMessageSourceResolvable(
						new String[] { "error.consolidated.currencyMismatch" },
						new Object[] { 2L, "USD", "NZD" }, "User {0,number,#} is billed in {1}, not {2}."));
		given(consolidationResolver.groupForUser(1L)).willReturn(group);
		given(consolidationResolver.groupForUser(2L)).willReturn(group);

		// WHEN
		creator.createTasks(endDate);

		// THEN
		then(invoicingSystem).shouldHaveNoInteractions();
		then(accountTaskDao).shouldHaveNoInteractions();
	}

}
