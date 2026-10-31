/* ==================================================================
 * SnfBillingGnucashConfig.java - 11/10/2026 3:04:01 pm
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

package net.solarnetwork.central.jobs.config;

import static net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration.GNUCASH;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.zaxxer.hikari.HikariDataSource;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfTaxCodeResolver;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.PaymentDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashAccountingSystem;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashSettings;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.GnucashDao;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.JdbcGnucashDao;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceAccountingSyncer;
import net.solarnetwork.central.user.billing.snf.jobs.PaymentAccountingSyncer;

/**
 * Configuration for recording SNF invoices and payments in a GnuCash book.
 *
 * <p>
 * The GnuCash tables can be in a database of their own, configured with the
 * {@code app.datasource-gnucash} properties. Without those, the tables must be
 * in the main database, in the schema named by the
 * {@code app.billing.gnucash.schema} property.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(SolarNetUserBillingConfiguration.SNF_BILLING + " & "
		+ SolarNetUserBillingConfiguration.SNF_BILLING_GNUCASH)
public class SnfBillingGnucashConfig {

	@Bean
	@Qualifier(GNUCASH)
	@ConditionalOnProperty("app.datasource-gnucash.url")
	@ConfigurationProperties("app.datasource-gnucash")
	public DataSourceProperties gnucashDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@Qualifier(GNUCASH)
	@ConditionalOnProperty("app.datasource-gnucash.url")
	@ConfigurationProperties("app.datasource-gnucash.hikari")
	public HikariDataSource gnucashDataSource(@Qualifier(GNUCASH) DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

	@Bean
	@Qualifier(GNUCASH)
	@ConditionalOnProperty("app.datasource-gnucash.url")
	public PlatformTransactionManager gnucashTransactionManager(
			@Qualifier(GNUCASH) DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	@ConfigurationProperties("app.billing.gnucash")
	public GnucashSettings gnucashSettings() {
		return new GnucashSettings();
	}

	@Bean
	public GnucashDao gnucashDao(GnucashSettings settings, DataSource dataSource,
			@Qualifier(GNUCASH) ObjectProvider<DataSource> gnucashDataSource) {
		var dao = new JdbcGnucashDao(
				new JdbcTemplate(gnucashDataSource.getIfAvailable(() -> dataSource)),
				settings.getSchema());
		dao.setLockTimeoutSecs(settings.getLockTimeoutSecs());
		return dao;
	}

	@Bean
	public SnfAccountingSystem gnucashAccountingSystem(GnucashSettings settings, GnucashDao dao,
			PlatformTransactionManager transactionManager,
			@Qualifier(GNUCASH) ObjectProvider<PlatformTransactionManager> gnucashTransactionManager,
			SnfInvoicingSystem invoicingSystem, TaxCodeDao taxCodeDao) {
		if ( !(invoicingSystem instanceof SnfTaxCodeResolver taxCodeResolver) ) {
			throw new IllegalStateException("The SNF invoicing system does not resolve tax codes.");
		}
		return new GnucashAccountingSystem(Clock.systemUTC(),
				new TransactionTemplate(
						gnucashTransactionManager.getIfAvailable(() -> transactionManager)),
				dao, settings, invoicingSystem, taxCodeResolver, taxCodeDao);
	}

	@Bean
	public InvoiceAccountingSyncer invoiceAccountingSyncer(AccountDao accountDao,
			SnfInvoiceDao invoiceDao, SnfAccountingSystem accountingSystem) {
		return new InvoiceAccountingSyncer(accountDao, invoiceDao, accountingSystem);
	}

	@Bean
	public PaymentAccountingSyncer paymentAccountingSyncer(AccountDao accountDao, PaymentDao paymentDao,
			SnfAccountingSystem accountingSystem) {
		return new PaymentAccountingSyncer(accountDao, paymentDao, accountingSystem);
	}

}
