/* ==================================================================
 * AccountTaskJobTests.java - 21/07/2020 11:37:30 AM
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

import static org.easymock.EasyMock.expect;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskHandler;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskJob;

/**
 * Test cases for the {@link AccountTaskJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AccountTaskJobTests {

	private PlatformTransactionManager txManager;
	private AccountTaskDao taskDao;
	private AccountTaskHandler genInvoiceTaskHandler;
	private AccountTaskHandler delInvoiceTaskHandler;
	private TestJob job;

	private static class TestJob extends AccountTaskJob {

		public TestJob(TransactionTemplate transactionTemplate, AccountTaskDao taskDao,
				AccountTaskHandler generateInvoiceTaskHandler,
				AccountTaskHandler deliverInvoiceTaskHandler) {
			super(transactionTemplate, taskDao, generateInvoiceTaskHandler, deliverInvoiceTaskHandler);
		}

	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(txManager, taskDao, genInvoiceTaskHandler, delInvoiceTaskHandler);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
		}
	}

	@After
	public void teardown() {
		EasyMock.verify(txManager, taskDao, genInvoiceTaskHandler, delInvoiceTaskHandler);
	}

	@Before
	public void setup() {
		txManager = EasyMock.createMock(PlatformTransactionManager.class);
		taskDao = EasyMock.createMock(AccountTaskDao.class);
		genInvoiceTaskHandler = EasyMock.createMock(AccountTaskHandler.class);
		delInvoiceTaskHandler = EasyMock.createMock(AccountTaskHandler.class);

		job = new TestJob(new TransactionTemplate(txManager), taskDao, genInvoiceTaskHandler,
				delInvoiceTaskHandler);
	}

	@Test
	public void genInvoice_singleThread_noTasks() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);
		expect(taskDao.claimAccountTask()).andReturn(null);
		txManager.commit(tx);

		// WHEN
		replayAll(tx);
		job.run();

		// THEN
	}

	@Test
	public void genInvoice_singleThread_oneTask() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);

		final Long accountId = UUID.randomUUID().getMostSignificantBits();
		AccountTask task = AccountTask.newTask(Instant.now().truncatedTo(ChronoUnit.DAYS),
				AccountTaskType.GenerateInvoice, accountId);

		// claim task
		expect(taskDao.claimAccountTask()).andReturn(task);

		// execute task
		expect(genInvoiceTaskHandler.handleTask(task)).andReturn(true);

		// complete task
		taskDao.delete(task);
		txManager.commit(tx);

		// look for another task to execute
		TransactionStatus tx2 = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx2);
		expect(taskDao.claimAccountTask()).andReturn(null);
		txManager.commit(tx2);

		// WHEN
		replayAll(tx, tx2);
		job.run();

		// THEN
	}

	@Test
	public void delInvoice_singleThread_oneTask() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);

		final Long accountId = UUID.randomUUID().getMostSignificantBits();
		AccountTask task = AccountTask.newTask(Instant.now().truncatedTo(ChronoUnit.DAYS),
				AccountTaskType.DeliverInvoice, accountId);

		// claim task
		expect(taskDao.claimAccountTask()).andReturn(task);

		// execute task
		expect(delInvoiceTaskHandler.handleTask(task)).andReturn(true);

		// complete task
		taskDao.delete(task);
		txManager.commit(tx);

		// look for another task to execute
		TransactionStatus tx2 = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx2);
		expect(taskDao.claimAccountTask()).andReturn(null);
		txManager.commit(tx2);

		// WHEN
		replayAll(tx, tx2);
		job.run();

		// THEN
	}
}
