/* ==================================================================
 * SnfBillingJobsConfig.java - 8/11/2021 3:06:19 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.jobs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskJob;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceDeliverer;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerationTaskCreator;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerationTaskCreatorJob;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerator;
import net.solarnetwork.central.user.dao.UserDao;

/**
 * SNF billing jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("snf-billing")
public class SnfBillingJobsConfig {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private AccountTaskDao accountTaskDao;

	@Autowired
	private SnfInvoicingSystem snfInvoicingSystem;

	@Autowired
	private UserDao userDao;

	@Bean
	public InvoiceGenerator invoiceGenerator() {
		return new InvoiceGenerator(accountDao, accountTaskDao, snfInvoicingSystem);
	}

	@Bean
	public InvoiceDeliverer invoiceDeliverer() {
		return new InvoiceDeliverer(snfInvoicingSystem);
	}

	@ConfigurationProperties(prefix = "app.job.datum.billing.account-task")
	@Bean
	public ManagedJob accountTaskProcessorJob() {
		AccountTaskJob job = new AccountTaskJob(transactionTemplate, accountTaskDao, invoiceGenerator(),
				invoiceDeliverer());
		job.setParallelTaskExecutor(taskExecutor);
		job.setId("AccountTaskProcessor");
		return job;
	}

	@Bean
	public InvoiceGenerationTaskCreator invoiceGenerationTaskCreator() {
		return new InvoiceGenerationTaskCreator(userDao, snfInvoicingSystem, accountTaskDao);
	}

	@ConfigurationProperties(prefix = "app.job.datum.billing.invoice-gen")
	@Bean
	public ManagedJob invoiceGenerationTaskCreatorJob() {
		InvoiceGenerationTaskCreatorJob job = new InvoiceGenerationTaskCreatorJob(
				invoiceGenerationTaskCreator());
		job.setParallelTaskExecutor(taskExecutor);
		job.setId("InvoiceGenerationTaskCreator");
		return job;
	}

}
