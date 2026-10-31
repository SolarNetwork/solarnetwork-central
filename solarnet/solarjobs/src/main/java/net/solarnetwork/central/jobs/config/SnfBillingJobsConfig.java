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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.jobs.AccountTaskJob;
import net.solarnetwork.central.user.billing.snf.jobs.ConsolidatedInvoiceDeliverer;
import net.solarnetwork.central.user.billing.snf.jobs.ConsolidatedInvoiceGenerator;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceAccountingSyncer;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceDeliverer;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerationTaskCreator;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerationTaskCreatorJob;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceGenerator;
import net.solarnetwork.central.user.billing.snf.jobs.PaymentAccountingSyncer;
import net.solarnetwork.central.user.dao.UserDao;

/**
 * SNF billing jobs configuration.
 *
 * @author matt
 * @version 1.2
 */
@Configuration(proxyBeanMethods = false)
@Profile(SolarNetUserBillingConfiguration.SNF_BILLING)
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

	@Autowired
	private InvoiceConsolidationResolver invoiceConsolidationResolver;

	@Bean
	public InvoiceGenerator invoiceGenerator(
			ObjectProvider<InvoiceAccountingSyncer> invoiceAccountingSyncer) {
		var generator = new InvoiceGenerator(accountDao, accountTaskDao, snfInvoicingSystem);
		generator.setSyncInvoicesToAccounting(invoiceAccountingSyncer.getIfAvailable() != null);
		return generator;
	}

	@Bean
	public InvoiceDeliverer invoiceDeliverer() {
		return new InvoiceDeliverer(snfInvoicingSystem);
	}

	/**
	 * The consolidated invoice generator.
	 *
	 * @param invoiceAccountingSyncer
	 *        the invoice accounting syncer, if available
	 * @return the generator
	 * @since 1.2
	 */
	@Bean
	public ConsolidatedInvoiceGenerator consolidatedInvoiceGenerator(
			ObjectProvider<InvoiceAccountingSyncer> invoiceAccountingSyncer) {
		var generator = new ConsolidatedInvoiceGenerator(accountDao, accountTaskDao, snfInvoicingSystem,
				invoiceConsolidationResolver);
		generator.setSyncInvoicesToAccounting(invoiceAccountingSyncer.getIfAvailable() != null);
		return generator;
	}

	/**
	 * The consolidated invoice deliverer.
	 *
	 * @return the deliverer
	 * @since 1.2
	 */
	@Bean
	public ConsolidatedInvoiceDeliverer consolidatedInvoiceDeliverer() {
		return new ConsolidatedInvoiceDeliverer(snfInvoicingSystem);
	}

	@ConfigurationProperties(prefix = "app.job.billing.account-task")
	@Bean
	public ManagedJob accountTaskProcessorJob(InvoiceGenerator generator, InvoiceDeliverer deliverer,
			ConsolidatedInvoiceGenerator consolidatedGenerator,
			ConsolidatedInvoiceDeliverer consolidatedDeliverer,
			ObjectProvider<InvoiceAccountingSyncer> invoiceAccountingSyncer,
			ObjectProvider<PaymentAccountingSyncer> paymentAccountingSyncer) {
		AccountTaskJob job = new AccountTaskJob(transactionTemplate, accountTaskDao, generator,
				deliverer);
		job.setSyncInvoiceTaskHandler(invoiceAccountingSyncer.getIfAvailable());
		job.setSyncPaymentTaskHandler(paymentAccountingSyncer.getIfAvailable());
		job.setGenerateConsolidatedInvoiceTaskHandler(consolidatedGenerator);
		job.setDeliverConsolidatedInvoiceTaskHandler(consolidatedDeliverer);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@Bean
	public InvoiceGenerationTaskCreator invoiceGenerationTaskCreator() {
		var creator = new InvoiceGenerationTaskCreator(userDao, snfInvoicingSystem, accountTaskDao);
		creator.setConsolidationResolver(invoiceConsolidationResolver);
		return creator;
	}

	@ConfigurationProperties(prefix = "app.job.billing.invoice-gen")
	@Bean
	public ManagedJob invoiceGenerationTaskCreatorJob(InvoiceGenerationTaskCreator taskCreator) {
		InvoiceGenerationTaskCreatorJob job = new InvoiceGenerationTaskCreatorJob(taskCreator);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
