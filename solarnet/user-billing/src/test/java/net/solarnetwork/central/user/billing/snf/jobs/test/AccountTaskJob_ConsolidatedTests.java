/* ==================================================================
 * AccountTaskJob_ConsolidatedTests.java - 15/10/2026 12:10:47 pm
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskHandler;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskJob;

/**
 * Test cases for the {@link AccountTaskJob} class handling consolidated
 * invoice tasks.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class AccountTaskJob_ConsolidatedTests {

	private static final Long ACCOUNT_ID = 1L;

	@Mock
	private AccountTaskDao taskDao;

	@Mock
	private AccountTaskHandler generateInvoiceTaskHandler;

	@Mock
	private AccountTaskHandler deliverInvoiceTaskHandler;

	@Mock
	private AccountTaskHandler generateConsolidatedInvoiceTaskHandler;

	@Mock
	private AccountTaskHandler deliverConsolidatedInvoiceTaskHandler;

	private AccountTaskJob job;

	@BeforeEach
	public void setup() {
		job = new AccountTaskJob(null, taskDao, generateInvoiceTaskHandler,
				deliverInvoiceTaskHandler);
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);
	}

	@Test
	public void generateConsolidatedInvoice() {
		// GIVEN
		job.setGenerateConsolidatedInvoiceTaskHandler(generateConsolidatedInvoiceTaskHandler);
		final AccountTask task = AccountTask.newTask(Instant.now(),
				AccountTaskType.GenerateConsolidatedInvoice, ACCOUNT_ID);
		given(taskDao.claimAccountTask()).willReturn(task, (AccountTask) null);
		given(generateConsolidatedInvoiceTaskHandler.handleTask(task)).willReturn(true);

		// WHEN
		job.run();

		// THEN
		then(taskDao).should().delete(task);
		then(generateInvoiceTaskHandler).shouldHaveNoInteractions();
	}

	@Test
	public void generateConsolidatedInvoice_noHandler() {
		// GIVEN
		final AccountTask task = AccountTask.newTask(Instant.now(),
				AccountTaskType.GenerateConsolidatedInvoice, ACCOUNT_ID);
		given(taskDao.claimAccountTask()).willReturn(task, (AccountTask) null);

		// WHEN
		job.run();

		// THEN
		then(taskDao).should().deferAccountTask(eq(task.getId()), any());
		then(taskDao).should(never()).delete(any());
	}

	@Test
	public void deliverConsolidatedInvoice() {
		// GIVEN
		job.setDeliverConsolidatedInvoiceTaskHandler(deliverConsolidatedInvoiceTaskHandler);
		final AccountTask task = AccountTask.newTask(Instant.now(),
				AccountTaskType.DeliverConsolidatedInvoice, ACCOUNT_ID);
		given(taskDao.claimAccountTask()).willReturn(task, (AccountTask) null);
		given(deliverConsolidatedInvoiceTaskHandler.handleTask(task)).willReturn(true);

		// WHEN
		job.run();

		// THEN
		then(taskDao).should().delete(task);
		then(deliverInvoiceTaskHandler).shouldHaveNoInteractions();
	}

	@Test
	public void deliverConsolidatedInvoice_retry() {
		// GIVEN
		job.setDeliverConsolidatedInvoiceTaskHandler(deliverConsolidatedInvoiceTaskHandler);
		final AccountTask task = AccountTask.newTask(Instant.now(),
				AccountTaskType.DeliverConsolidatedInvoice, ACCOUNT_ID);
		given(taskDao.claimAccountTask()).willReturn(task, (AccountTask) null);
		given(deliverConsolidatedInvoiceTaskHandler.handleTask(task)).willReturn(false);

		// WHEN
		job.run();

		// THEN
		then(taskDao).should().deferAccountTask(eq(task.getId()), any());
		then(taskDao).should(never()).delete(any());
	}

}
