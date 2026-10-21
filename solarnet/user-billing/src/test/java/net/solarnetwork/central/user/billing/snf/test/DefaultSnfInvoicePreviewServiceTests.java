/* ==================================================================
 * DefaultSnfInvoicePreviewServiceTests.java - 22/10/2026 3:12:44 pm
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

package net.solarnetwork.central.user.billing.snf.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewState;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicePreviewService;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreview;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreviewKey;

/**
 * Test cases for the {@link DefaultSnfInvoicePreviewService} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DefaultSnfInvoicePreviewServiceTests {

	private static final YearMonth MONTH = YearMonth.of(2026, 10);
	private static final LocalDate START = MONTH.atDay(1);
	private static final LocalDate END = MONTH.plusMonths(1).atDay(1);

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	private MutableClock clock;
	private List<Runnable> tasks;
	private AtomicBoolean reject;
	private SimpleCache<SnfInvoicePreviewKey, SnfInvoicePreview> cache;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.parse("2026-10-22T01:00:00Z"), ZoneOffset.UTC);
		tasks = new ArrayList<>();
		reject = new AtomicBoolean();
		cache = new SimpleCache<>("Test Preview Cache");
	}

	private DefaultSnfInvoicePreviewService service(int maxConcurrent) {
		final Executor executor = task -> {
			if ( reject.get() ) {
				throw new RejectedExecutionException("Test reject");
			}
			tasks.add(task);
		};
		return new DefaultSnfInvoicePreviewService(clock, invoicingSystem, executor, cache,
				maxConcurrent);
	}

	private void runTasks() {
		final List<Runnable> toRun = new ArrayList<>(tasks);
		tasks.clear();
		for ( Runnable task : toRun ) {
			task.run();
		}
	}

	private static SnfInvoicePreviewKey key(Long userId) {
		return new SnfInvoicePreviewKey(userId, MONTH, false);
	}

	private static SnfInvoice invoice(Long userId) {
		return new SnfInvoice(randomLong(), userId, Instant.now(), START, END, "NZD");
	}

	private static SnfInvoicingSystem.InvoiceGenerationOptions dryRun(boolean useCredit) {
		return argThat(o -> o != null && o.isDryRun() && o.isUseAccountCredit() == useCredit);
	}

	@Test
	public void accountPreview_start() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();
		final SnfInvoice invoice = invoice(userId);
		given(invoicingSystem.generateInvoice(eq(userId), eq(START), eq(END), dryRun(false)))
				.willReturn(invoice);

		// WHEN
		final SnfInvoicePreview started = service.accountPreview(key(userId));
		final Instant startDate = clock.instant();
		clock.add(Duration.ofSeconds(5));
		final SnfInvoicePreview running = service.accountPreview(key(userId));
		runTasks();
		final SnfInvoicePreview done = service.accountPreview(key(userId));

		// THEN
		and.then(started).as("Preview started").isEqualTo(SnfInvoicePreview.running(startDate));
		and.then(running).as("Preview still running, from its start date")
				.isEqualTo(SnfInvoicePreview.running(startDate));
		and.then(done).as("Preview done with generated invoice")
				.isEqualTo(SnfInvoicePreview.done(clock.instant(), invoice));
		and.then(tasks).as("No more tasks started once done").isEmpty();
		and.then(service.completedAccountPreview(key(userId))).as("Completed preview available")
				.isSameAs(done);
	}

	@Test
	public void accountPreview_useCredit() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();
		final SnfInvoice invoice = invoice(userId);
		given(invoicingSystem.generateInvoice(eq(userId), eq(START), eq(END), dryRun(true)))
				.willReturn(invoice);

		// WHEN
		service.accountPreview(new SnfInvoicePreviewKey(userId, MONTH, true));
		runTasks();
		final SnfInvoicePreview result = service
				.accountPreview(new SnfInvoicePreviewKey(userId, MONTH, true));

		// THEN
		and.then(result).as("Preview done with credit option invoice")
				.isEqualTo(SnfInvoicePreview.done(clock.instant(), invoice));
		and.then(service.completedAccountPreview(key(userId)))
				.as("Preview without credit is a different preview").isNull();
	}

	@Test
	public void accountPreview_noCharges() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();

		// WHEN
		service.accountPreview(key(userId));
		runTasks();
		final SnfInvoicePreview result = service.accountPreview(key(userId));

		// THEN
		then(invoicingSystem).should().generateInvoice(eq(userId), eq(START), eq(END),
				dryRun(false));
		and.then(result).as("Preview done without invoice")
				.isEqualTo(SnfInvoicePreview.done(clock.instant(), null));
	}

	@Test
	public void accountPreview_waitingWithoutPermit() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();

		// WHEN
		final SnfInvoicePreview started1 = service.accountPreview(key(userId1));
		final SnfInvoicePreview waiting2 = service.accountPreview(key(userId2));
		final int tasksWhileWaiting = tasks.size();
		runTasks();
		final SnfInvoicePreview started2 = service.accountPreview(key(userId2));

		// THEN
		and.then(started1.state()).as("First preview started").isEqualTo(InvoicePreviewState.Running);
		and.then(waiting2.state()).as("Second preview waiting, without permit")
				.isEqualTo(InvoicePreviewState.Waiting);
		and.then(tasksWhileWaiting).as("Waiting preview started no task").isOne();
		and.then(started2.state()).as("Second preview started once permit released")
				.isEqualTo(InvoicePreviewState.Running);
		and.then(tasks).as("Second preview task started").hasSize(1);
	}

	@Test
	public void accountPreview_failed() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();
		final Long userId2 = randomLong();
		given(invoicingSystem.generateInvoice(eq(userId), eq(START), eq(END), dryRun(false)))
				.willThrow(new RuntimeException("Test query timeout"));

		// WHEN
		service.accountPreview(key(userId));
		runTasks();
		final Instant failDate = clock.instant();
		final SnfInvoicePreview failed = service.accountPreview(key(userId));
		final int tasksAfterFailed = tasks.size();
		final SnfInvoicePreview other = service.accountPreview(key(userId2));

		// THEN
		and.then(failed).as("Preview failed").isEqualTo(SnfInvoicePreview.failed(failDate));
		and.then(tasksAfterFailed).as("Failed preview not started again").isZero();
		and.then(other.state()).as("Permit released after failure")
				.isEqualTo(InvoicePreviewState.Running);
		and.then(service.completedAccountPreview(key(userId))).as("Failed preview not completed")
				.isNull();
	}

	@Test
	public void accountPreview_failed_retryAfterDelay() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();
		final SnfInvoice invoice = invoice(userId);
		given(invoicingSystem.generateInvoice(eq(userId), eq(START), eq(END), dryRun(false)))
				.willThrow(new RuntimeException("Test query timeout")).willReturn(invoice);

		// WHEN
		service.accountPreview(key(userId));
		runTasks();
		clock.add(DefaultSnfInvoicePreviewService.DEFAULT_FAILURE_RETRY_DELAY.minusSeconds(1));
		final SnfInvoicePreview beforeDelay = service.accountPreview(key(userId));
		clock.add(Duration.ofSeconds(1));
		final SnfInvoicePreview afterDelay = service.accountPreview(key(userId));
		runTasks();
		final SnfInvoicePreview retried = service.accountPreview(key(userId));

		// THEN
		and.then(beforeDelay.state()).as("Failure kept until retry delay")
				.isEqualTo(InvoicePreviewState.Failed);
		and.then(afterDelay.state()).as("Preview started again after retry delay")
				.isEqualTo(InvoicePreviewState.Running);
		and.then(retried).as("Retried preview done")
				.isEqualTo(SnfInvoicePreview.done(clock.instant(), invoice));
	}

	@Test
	public void accountPreview_rejected() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();

		// WHEN
		reject.set(true);
		final SnfInvoicePreview rejected = service.accountPreview(key(userId));
		reject.set(false);
		final SnfInvoicePreview started = service.accountPreview(key(userId));

		// THEN
		and.then(rejected.state()).as("Rejected preview waiting").isEqualTo(InvoicePreviewState.Waiting);
		and.then(started.state()).as("Permit released and preview started on next request")
				.isEqualTo(InvoicePreviewState.Running);
		and.then(tasks).as("Preview task started").hasSize(1);
	}

	@Test
	public void accountPreview_interrupted() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId = randomLong();
		given(invoicingSystem.generateInvoice(eq(userId), eq(START), eq(END), dryRun(false)))
				.willAnswer(_ -> {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Test interrupted query");
				});

		// WHEN
		service.accountPreview(key(userId));
		try {
			runTasks();
		} finally {
			// clear the interrupt flag of the test thread
			Thread.interrupted();
		}
		final SnfInvoicePreview result = service.accountPreview(key(userId));

		// THEN
		and.then(result.state()).as("Interrupted preview not cached, so started again")
				.isEqualTo(InvoicePreviewState.Running);
		and.then(tasks).as("Preview task started again").hasSize(1);
	}

	@Test
	public void accountPreview_transactionTemplate() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final AtomicInteger txCount = new AtomicInteger();
		service.setTransactionTemplate(new TransactionOperations() {

			@Override
			public <T> T execute(TransactionCallback<T> action) {
				txCount.incrementAndGet();
				return action.doInTransaction(new SimpleTransactionStatus());
			}
		});
		final Long userId = randomLong();
		final SnfInvoice invoice = invoice(userId);
		given(invoicingSystem.generateInvoice(eq(userId), eq(START), eq(END), dryRun(false)))
				.willReturn(invoice);

		// WHEN
		service.accountPreview(key(userId));
		runTasks();
		final SnfInvoicePreview result = service.accountPreview(key(userId));

		// THEN
		and.then(txCount).as("Preview generated in transaction").hasValue(1);
		and.then(result).as("Preview done").isEqualTo(SnfInvoicePreview.done(clock.instant(), invoice));
	}

	@Test
	public void groupPreview_oneAccountAtATime() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final Long userId3 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2, userId3),
				false, null);
		final SnfInvoice invoice1 = invoice(userId1);
		final SnfInvoice invoice3 = invoice(userId3);
		given(invoicingSystem.generateInvoice(eq(userId1), eq(START), eq(END), dryRun(false)))
				.willReturn(invoice1);
		// no charges for the second account
		given(invoicingSystem.generateInvoice(eq(userId2), eq(START), eq(END), dryRun(false)))
				.willReturn(null);
		given(invoicingSystem.generateInvoice(eq(userId3), eq(START), eq(END), dryRun(false)))
				.willReturn(invoice3);

		// WHEN
		final List<SnfInvoicePreview> step1 = service.groupPreview(group, key(userId1));
		final List<SnfInvoicePreview> step1again = service.groupPreview(group, key(userId1));
		final int step1tasks = tasks.size();
		runTasks();
		final List<SnfInvoicePreview> step2 = service.groupPreview(group, key(userId1));
		runTasks();
		final List<SnfInvoicePreview> step3 = service.groupPreview(group, key(userId1));
		runTasks();
		final List<SnfInvoicePreview> done = service.groupPreview(group, key(userId1));

		// THEN
		and.then(step1).extracting(SnfInvoicePreview::state).as("First account started")
				.containsExactly(InvoicePreviewState.Running, InvoicePreviewState.Waiting,
						InvoicePreviewState.Waiting);
		and.then(step1again).extracting(SnfInvoicePreview::state)
				.as("Next account not started while first running")
				.containsExactly(InvoicePreviewState.Running, InvoicePreviewState.Waiting,
						InvoicePreviewState.Waiting);
		and.then(step1tasks).as("One account generated at a time, despite free permit").isOne();
		and.then(step2).extracting(SnfInvoicePreview::state).as("Second account started")
				.containsExactly(InvoicePreviewState.Done, InvoicePreviewState.Running,
						InvoicePreviewState.Waiting);
		and.then(step3).extracting(SnfInvoicePreview::state).as("Third account started")
				.containsExactly(InvoicePreviewState.Done, InvoicePreviewState.Done,
						InvoicePreviewState.Running);
		and.then(done).extracting(SnfInvoicePreview::invoice).as("All accounts done, in group order")
				.containsExactly(invoice1, null, invoice3);
		and.then(tasks).as("No more tasks started once done").isEmpty();
	}

	@Test
	public void groupPreview_usesRunningAccountPreview() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2), false,
				null);

		// WHEN
		service.accountPreview(key(userId1));
		final List<SnfInvoicePreview> result = service.groupPreview(group, key(userId1));

		// THEN
		and.then(result).extracting(SnfInvoicePreview::state)
				.as("Primary account preview reused, next account waits for it")
				.containsExactly(InvoicePreviewState.Running, InvoicePreviewState.Waiting);
		and.then(tasks).as("Only the account preview task started").hasSize(1);
	}

	@Test
	public void groupPreview_startsAfterCachedAccounts() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2), false,
				null);

		// WHEN
		service.accountPreview(key(userId1));
		runTasks();
		final List<SnfInvoicePreview> result = service.groupPreview(group, key(userId1));

		// THEN
		and.then(result).extracting(SnfInvoicePreview::state)
				.as("Cached primary account preview reused, next account started")
				.containsExactly(InvoicePreviewState.Done, InvoicePreviewState.Running);
		then(invoicingSystem).should(times(1)).generateInvoice(eq(userId1), any(), any(), any());
	}

	@Test
	public void groupPreview_continuesAfterFailure() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2), false,
				null);
		given(invoicingSystem.generateInvoice(eq(userId1), eq(START), eq(END), dryRun(false)))
				.willThrow(new RuntimeException("Test query timeout"));

		// WHEN
		service.groupPreview(group, key(userId1));
		runTasks();
		final List<SnfInvoicePreview> result = service.groupPreview(group, key(userId1));

		// THEN
		and.then(result).extracting(SnfInvoicePreview::state)
				.as("Failed account kept, next account started")
				.containsExactly(InvoicePreviewState.Failed, InvoicePreviewState.Running);
	}

	@Test
	public void groupPreview_waitingWithoutPermit() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(1);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2), false,
				null);

		// WHEN
		service.accountPreview(key(randomLong()));
		final List<SnfInvoicePreview> result = service.groupPreview(group, key(userId1));

		// THEN
		and.then(result).extracting(SnfInvoicePreview::state).as("No account started without permit")
				.containsExactly(InvoicePreviewState.Waiting, InvoicePreviewState.Waiting);
		and.then(tasks).as("Only the other account's task started").hasSize(1);
	}

	@Test
	public void groupPreview_continuesWhilePolled() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final Long userId3 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2, userId3),
				false, null);

		// WHEN
		service.groupPreview(group, key(userId1));
		clock.add(DefaultSnfInvoicePreviewService.DEFAULT_GROUP_POLL_TIMEOUT);
		runTasks();
		final int tasksAfterFirst = tasks.size();
		runTasks();
		final int tasksAfterSecond = tasks.size();
		runTasks();
		final List<SnfInvoicePreview> done = service.groupPreview(group, key(userId1));

		// THEN
		and.then(tasksAfterFirst).as("Next account started when first finished").isOne();
		and.then(tasksAfterSecond).as("Third account started when second finished").isOne();
		and.then(done).extracting(SnfInvoicePreview::state)
				.as("All accounts done without polling for each one")
				.containsExactly(InvoicePreviewState.Done, InvoicePreviewState.Done,
						InvoicePreviewState.Done);
		and.then(tasks).as("No more tasks started once done").isEmpty();
	}

	@Test
	public void groupPreview_stopsWhenNotPolled() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2), false,
				null);

		// WHEN
		service.groupPreview(group, key(userId1));
		clock.add(DefaultSnfInvoicePreviewService.DEFAULT_GROUP_POLL_TIMEOUT.plusSeconds(1));
		runTasks();
		final int tasksWhenNotPolled = tasks.size();
		final List<SnfInvoicePreview> polled = service.groupPreview(group, key(userId1));

		// THEN
		and.then(tasksWhenNotPolled).as("Next account not started once group no longer polled")
				.isZero();
		and.then(polled).extracting(SnfInvoicePreview::state)
				.as("Next account started by next poll")
				.containsExactly(InvoicePreviewState.Done, InvoicePreviewState.Running);
	}

	@Test
	public void groupPreview_continueSkipsRunningAccount() {
		// GIVEN
		final DefaultSnfInvoicePreviewService service = service(2);
		final Long userId1 = randomLong();
		final Long userId2 = randomLong();
		final var group = new InvoiceConsolidationGroup(userId1, List.of(userId1, userId2), false,
				null);

		// WHEN
		service.groupPreview(group, key(userId1));
		// the second account started on its own, such as by its Current Usage section
		service.accountPreview(key(userId2));
		runTasks();

		// THEN
		and.then(tasks).as("Group not continued to second account, as already running")
				.isEmpty();
	}

}
