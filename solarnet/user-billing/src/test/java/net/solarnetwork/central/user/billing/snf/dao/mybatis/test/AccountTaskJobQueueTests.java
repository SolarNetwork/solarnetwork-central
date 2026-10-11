/* ==================================================================
 * AccountTaskJobQueueTests.java - 11/10/2026 2:23:46 pm
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis.test;

import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.DeliverInvoice;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.GenerateInvoice;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.SyncInvoiceToAccounting;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountTaskDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskHandler;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskJob;

/**
 * Test cases for the {@link AccountTaskJob} class working with the task queue
 * in the database.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AccountTaskJobQueueTests extends AbstractMyBatisDaoTestSupport {

	private static final int MAX_ITERATIONS = 5;

	@Mock
	private AccountTaskHandler generateInvoiceHandler;

	@Mock
	private AccountTaskHandler deliverInvoiceHandler;

	@Mock
	private AccountTaskHandler syncInvoiceHandler;

	private MyBatisAccountTaskDao taskDao;
	private Account account;
	private AccountTaskJob job;

	@BeforeEach
	public void setup() {
		MyBatisAddressDao addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		MyBatisAccountDao accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		taskDao = new MyBatisAccountTaskDao();
		taskDao.setSqlSessionTemplate(getSqlSessionTemplate());

		Account a = createTestAccount(addressDao.get(addressDao.save(createTestAddress())));
		account = accountDao.get(accountDao.save(a));

		job = new AccountTaskJob(null, taskDao, generateInvoiceHandler, deliverInvoiceHandler);
		job.setParallelism(1);
		job.setMaximumIterations(MAX_ITERATIONS);
	}

	private Instant execAt(AccountTask task) {
		getSqlSessionTemplate().flushStatements();
		return jdbcTemplate.queryForObject(
				"SELECT exec_at FROM solarbill.bill_account_task WHERE id = ?", Timestamp.class,
				task.getId()).toInstant();
	}

	private AccountTask saveTask(AccountTaskType type, Instant created) {
		AccountTask task = AccountTask.newTask(created, type, account.getAccountId(),
				Map.of("foo", "bar"));
		taskDao.save(task);
		return task;
	}

	@Test
	public void handlerAsksForRetry_handledOncePerRun() {
		// GIVEN
		final AccountTask task = saveTask(DeliverInvoice, Instant.now());
		final Duration retryDelay = Duration.ofMinutes(15);
		job.setRetryDelay(retryDelay);

		given(deliverInvoiceHandler.handleTask(any())).willReturn(false);

		// WHEN
		final Instant start = Instant.now();
		job.run();

		// THEN
		// @formatter:off
		then(deliverInvoiceHandler).should(times(1)).handleTask(any());
		and.then(taskDao.get(task.getId()))
			.as("Task kept for a later attempt")
			.isNotNull()
			;
		and.then(execAt(task))
			.as("Task deferred by the retry delay")
			.isCloseTo(start.plus(retryDelay), within(1, ChronoUnit.MINUTES))
			;
		// @formatter:on
	}

	@Test
	public void handlerAsksForRetry_laterTaskStillHandled() {
		// GIVEN
		final Instant now = Instant.now();
		final AccountTask retryTask = saveTask(DeliverInvoice, now.minus(1, ChronoUnit.HOURS));
		final AccountTask laterTask = saveTask(GenerateInvoice, now);

		given(deliverInvoiceHandler.handleTask(any())).willReturn(false);
		given(generateInvoiceHandler.handleTask(any())).willReturn(true);

		// WHEN
		job.run();

		// THEN
		// @formatter:off
		then(generateInvoiceHandler).should().handleTask(any());
		and.then(taskDao.get(laterTask.getId()))
			.as("Later task completed")
			.isNull()
			;
		and.then(taskDao.get(retryTask.getId()))
			.as("Retry task kept for a later attempt")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void repeatableException_handledOncePerRun() {
		// GIVEN
		final AccountTask task = saveTask(DeliverInvoice, Instant.now());

		given(deliverInvoiceHandler.handleTask(any()))
				.willThrow(new RepeatableTaskException("Mail server unavailable."));

		// WHEN
		job.run();

		// THEN
		// @formatter:off
		then(deliverInvoiceHandler).should(times(1)).handleTask(any());
		and.then(taskDao.get(task.getId()))
			.as("Task kept for a later attempt")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void handlerThrows_laterTaskStillHandled() {
		// GIVEN
		final Instant now = Instant.now();
		final AccountTask failTask = saveTask(DeliverInvoice, now.minus(1, ChronoUnit.HOURS));
		final AccountTask laterTask = saveTask(GenerateInvoice, now);

		given(deliverInvoiceHandler.handleTask(any()))
				.willThrow(new IllegalStateException("Invoice data invalid."));
		given(generateInvoiceHandler.handleTask(any())).willReturn(true);

		// WHEN
		job.run();

		// THEN
		// @formatter:off
		then(deliverInvoiceHandler).should(times(1)).handleTask(any());
		then(generateInvoiceHandler).should().handleTask(any());
		and.then(taskDao.get(laterTask.getId()))
			.as("Later task completed")
			.isNull()
			;
		and.then(taskDao.get(failTask.getId()))
			.as("Failed task kept for a later attempt")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void syncInvoice_handled() {
		// GIVEN
		job.setSyncInvoiceTaskHandler(syncInvoiceHandler);
		final AccountTask task = saveTask(SyncInvoiceToAccounting, Instant.now());

		given(syncInvoiceHandler.handleTask(any())).willReturn(true);

		// WHEN
		job.run();

		// THEN
		// @formatter:off
		then(syncInvoiceHandler).should().handleTask(any());
		and.then(taskDao.get(task.getId()))
			.as("Task completed")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void syncInvoice_noHandler() {
		// GIVEN
		final AccountTask task = saveTask(SyncInvoiceToAccounting, Instant.now());

		// WHEN
		final Instant start = Instant.now();
		job.run();

		// THEN
		// @formatter:off
		and.then(taskDao.get(task.getId()))
			.as("Task kept for when an accounting system is available")
			.isNotNull()
			;
		and.then(execAt(task))
			.as("Task deferred")
			.isAfter(start)
			;
		// @formatter:on
	}

}
