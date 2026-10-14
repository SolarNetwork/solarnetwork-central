/* ==================================================================
 * SnfBillingGnucashConfigTests.java - 11/10/2026 3:05:36 pm
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

package net.solarnetwork.central.jobs.config.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import java.sql.SQLException;
import java.time.Period;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import com.zaxxer.hikari.HikariDataSource;
import net.solarnetwork.central.jobs.config.SnfBillingGnucashConfig;
import net.solarnetwork.central.user.billing.snf.SnfAccountingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfTaxCodeResolver;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.PaymentDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashSettings;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.GnucashDao;
import net.solarnetwork.central.user.billing.snf.jobs.InvoiceAccountingSyncer;
import net.solarnetwork.central.user.billing.snf.jobs.PaymentAccountingSyncer;

/**
 * Test cases for the {@link SnfBillingGnucashConfig} class.
 *
 * <p>
 * The {@code gnucashDatabase} test connects to the database set by the
 * {@code gnucash.test.url}, {@code gnucash.test.username}, and
 * {@code gnucash.test.password} properties, loaded from {@code application.yml}
 * the way Spring Boot loads it, so an {@code application.yml} in the working
 * directory can change them.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class SnfBillingGnucashConfigTests {

	/**
	 * The beans the configuration needs from the rest of the application.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	public static class AppConfig {

		@Bean
		@Primary
		public DataSource dataSource() throws SQLException {
			DataSource ds = mock(DataSource.class);
			given(ds.getConnection()).willThrow(new SQLException("Main database used."));
			return ds;
		}

		@Bean
		@Primary
		public PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}

		@Bean
		public SnfInvoicingSystem snfInvoicingSystem() {
			return mock(SnfInvoicingSystem.class,
					withSettings().extraInterfaces(SnfTaxCodeResolver.class));
		}

		@Bean
		public TaxCodeDao taxCodeDao() {
			return mock(TaxCodeDao.class);
		}

		@Bean
		public AccountDao accountDao() {
			return mock(AccountDao.class);
		}

		@Bean
		public SnfInvoiceDao snfInvoiceDao() {
			return mock(SnfInvoiceDao.class);
		}

		@Bean
		public PaymentDao paymentDao() {
			return mock(PaymentDao.class);
		}

	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(AppConfig.class, SnfBillingGnucashConfig.class);

	private static void activateProfiles(ConfigurableApplicationContext ctx) {
		ctx.getEnvironment().setActiveProfiles("snf-billing", "snf-billing-gnucash");
	}

	@Test
	public void notActive() {
		runner.withInitializer(
				ctx -> ctx.getEnvironment().setActiveProfiles("snf-billing")).run(ctx -> {
					// @formatter:off
					then(ctx)
						.as("Nothing configured without the GnuCash profile")
						.doesNotHaveBean(GnucashDao.class)
						.doesNotHaveBean(InvoiceAccountingSyncer.class)
						.doesNotHaveBean(PaymentAccountingSyncer.class)
						;
					// @formatter:on
				});
	}

	@Test
	public void mainDatabase() {
		runner.withInitializer(SnfBillingGnucashConfigTests::activateProfiles)
				.withPropertyValues(
						"app.billing.gnucash.currencies.NZD.receivable-account=Assets:Receivable",
						"app.billing.gnucash.currencies.NZD.payment-account=Assets:Bank",
						"app.billing.gnucash.due-period=P2M")
				.run(ctx -> {
					// @formatter:off
					then(ctx)
						.as("Accounting beans configured")
						.hasSingleBean(GnucashDao.class)
						.hasSingleBean(SnfAccountingSystem.class)
						.hasSingleBean(InvoiceAccountingSyncer.class)
						.hasSingleBean(PaymentAccountingSyncer.class)
						.as("No GnuCash database configured")
						.doesNotHaveBean("gnucashDataSource")
						.doesNotHaveBean("gnucashTransactionManager")
						;
					then(ctx.getBean(GnucashSettings.class))
						.as("Settings bound")
						.returns(Period.ofMonths(2), from(GnucashSettings::getDuePeriod))
						.extracting(s -> s.currency("NZD"))
						.as("Currency settings bound")
						.returns("Assets:Receivable", from(GnucashSettings.CurrencySettings::getReceivableAccount))
						.returns("Assets:Bank", from(GnucashSettings.CurrencySettings::getPaymentAccount))
						;
					thenThrownBy(() -> ctx.getBean(GnucashDao.class).tableVersions())
						.as("GnuCash tables read from the main database")
						.hasRootCauseMessage("Main database used.")
						;
					// @formatter:on
				});
	}

	@Test
	public void gnucashDatabase() {
		runner.withInitializer(new ConfigDataApplicationContextInitializer())
				.withInitializer(SnfBillingGnucashConfigTests::activateProfiles)
				.withPropertyValues("app.datasource-gnucash.url=${gnucash.test.url}",
						"app.datasource-gnucash.username=${gnucash.test.username}",
						"app.datasource-gnucash.password=${gnucash.test.password}",
						"app.datasource-gnucash.hikari.minimum-idle=0")
				.run(ctx -> {
					final String url = ctx.getEnvironment().getRequiredProperty("gnucash.test.url");
					// @formatter:off
					then(ctx)
						.as("GnuCash database configured")
						.hasBean("gnucashDataSource")
						.hasBean("gnucashTransactionManager")
						;
					then(ctx.getBean("gnucashDataSource", HikariDataSource.class))
						.as("Connection pool for the GnuCash database")
						.returns(url, from(HikariDataSource::getJdbcUrl))
						.returns(0, from(HikariDataSource::getMinimumIdle))
						;
					then(ctx.getBean(GnucashDao.class).tableVersions())
						.as("GnuCash tables read from the GnuCash database")
						.containsEntry("invoices", 4)
						;
					// @formatter:on
				});
	}

}
