/* ==================================================================
 * AbstractGnucashTestSupport.java - 11/10/2026 2:58:06 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAccount;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCommodity;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCustomer;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncNumeric;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTable;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTableEntry;

/**
 * Base class for tests that use a GnuCash book in the {@code gnucashtest}
 * database.
 *
 * <p>
 * Each test runs in a transaction that is rolled back. The database starts with
 * an empty book, so a book like SolarNetwork's is set up before each test. The
 * connection is set by the {@code gnucash.test.url},
 * {@code gnucash.test.username}, and {@code gnucash.test.password} properties,
 * loaded from {@code application.yml} the way Spring Boot tests load it, so an
 * {@code application.yml} in the working directory can change them.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringJUnitConfig(classes = AbstractGnucashTestSupport.GnucashTestConfig.class,
		initializers = ConfigDataApplicationContextInitializer.class)
@Transactional
@Rollback
public abstract class AbstractGnucashTestSupport {

	/**
	 * Configuration for the GnuCash test database.
	 */
	@Configuration(proxyBeanMethods = false)
	public static class GnucashTestConfig {

		@Bean
		public DataSource gnucashDataSource(@Value("${gnucash.test.url}") String url,
				@Value("${gnucash.test.username}") String username,
				@Value("${gnucash.test.password}") String password) {
			return new DriverManagerDataSource(url, username, password);
		}

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		public TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
			return new TransactionTemplate(txManager);
		}

	}

	/** The schema of the GnuCash tables. */
	protected static final String SCHEMA = "public";

	/** The test customer name. */
	protected static final String CUSTOMER_NAME = "Test Customer";

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	protected TransactionTemplate transactionTemplate;

	/** The book's root account GUID. */
	protected String rootGuid;

	/** The NZD currency. */
	protected GncCommodity nzd;

	/** The USD currency. */
	protected GncCommodity usd;

	/** The NZD receivable account. */
	protected GncAccount receivable;

	/** The USD receivable account. */
	protected GncAccount receivableUsd;

	/** The NZD income account. */
	protected GncAccount income;

	/** The USD income account. */
	protected GncAccount incomeUsd;

	/** The NZD bank account. */
	protected GncAccount bank;

	/** The USD bank account. */
	protected GncAccount bankUsd;

	/** The NZD GST account. */
	protected GncAccount gst;

	/** The 15% GST tax table. */
	protected GncTaxTable gstTable;

	/** The zero-rated GST tax table. */
	protected GncTaxTable gstZeroTable;

	/**
	 * Set up a book like SolarNetwork's.
	 */
	@BeforeEach
	public void setupBook() {
		rootGuid = jdbcTemplate.queryForObject("SELECT root_account_guid FROM books LIMIT 1",
				String.class);
		nzd = insertCurrency("NZD");
		usd = insertCurrency("USD");
		final GncAccount assets = insertAccount("Assets", "ASSET", nzd, null, true);
		receivable = insertAccount("Accounts Receivable", "RECEIVABLE", nzd, assets, false);
		receivableUsd = insertAccount("Accounts Receivable USD", "RECEIVABLE", usd, assets, false);
		bank = insertAccount("Bank", "BANK", nzd, assets, false);
		bankUsd = insertAccount("Bank USD", "BANK", usd, assets, false);
		final GncAccount incomeParent = insertAccount("Income", "INCOME", nzd, null, true);
		income = insertAccount("Subscription", "INCOME", nzd, incomeParent, false);
		incomeUsd = insertAccount("Subscription USD", "INCOME", usd, incomeParent, false);
		final GncAccount liabilities = insertAccount("Liabilities", "LIABILITY", nzd, null, true);
		final GncAccount tax = insertAccount("Tax", "LIABILITY", nzd, liabilities, true);
		gst = insertAccount("GST on Sales", "LIABILITY", nzd, tax, false);
		gstTable = insertTaxTable("GST (Sales)", gst, new BigDecimal("15"));
		gstZeroTable = insertTaxTable("GST Zero (Sales)", gst, BigDecimal.ZERO);
	}

	/**
	 * Create a new GnuCash GUID.
	 *
	 * @return the GUID
	 */
	protected static String guid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Convert an instant to a GnuCash timestamp value.
	 *
	 * @param ts
	 *        the instant
	 * @return the timestamp value
	 */
	protected static LocalDateTime gncTimestamp(Instant ts) {
		return LocalDateTime.ofInstant(ts, ZoneOffset.UTC);
	}

	/**
	 * Convert a GnuCash timestamp column value to an instant.
	 *
	 * @param value
	 *        the column value
	 * @return the instant
	 */
	protected static @Nullable Instant instant(@Nullable Object value) {
		if ( value instanceof Timestamp ts ) {
			return ts.toLocalDateTime().toInstant(ZoneOffset.UTC);
		}
		return null;
	}

	/**
	 * Query for rows.
	 *
	 * @param sql
	 *        the SQL
	 * @param args
	 *        the arguments
	 * @return the rows
	 */
	protected List<Map<String, Object>> rows(String sql, Object... args) {
		return jdbcTemplate.queryForList(sql, args);
	}

	/**
	 * Add a currency.
	 *
	 * @param code
	 *        the currency code
	 * @return the currency
	 */
	protected GncCommodity insertCurrency(String code) {
		final String guid = guid();
		jdbcTemplate.update("""
				INSERT INTO commodities (guid, namespace, mnemonic, fullname, cusip, fraction
					, quote_flag, quote_source, quote_tz)
				VALUES (?, 'CURRENCY', ?, ?, '', 100, 1, 'currency', '')
				""", guid, code, code);
		return new GncCommodity(guid, GncCommodity.CURRENCY_NAMESPACE, code, 100);
	}

	/**
	 * Add an account.
	 *
	 * @param name
	 *        the name
	 * @param type
	 *        the type
	 * @param commodity
	 *        the commodity
	 * @param parent
	 *        the parent, or {@code null} for the root account
	 * @param placeholder
	 *        {@code true} for a placeholder account
	 * @return the account
	 */
	protected GncAccount insertAccount(String name, String type, GncCommodity commodity,
			@Nullable GncAccount parent, boolean placeholder) {
		final String guid = guid();
		jdbcTemplate.update("""
				INSERT INTO accounts (guid, name, account_type, commodity_guid, commodity_scu
					, non_std_scu, parent_guid, code, description, hidden, placeholder)
				VALUES (?, ?, ?, ?, 100, 0, ?, '', '', 0, ?)
				""", guid, name, type, commodity.guid(), (parent != null ? parent.guid() : rootGuid),
				placeholder ? 1 : 0);
		return new GncAccount(guid, (parent != null ? parent.path() + ":" + name : name), type,
				commodity.guid(), 100);
	}

	/**
	 * Add a percentage tax table, the way GnuCash stores it.
	 *
	 * @param name
	 *        the name
	 * @param account
	 *        the tax account
	 * @param percent
	 *        the percentage
	 * @return the tax table
	 */
	protected GncTaxTable insertTaxTable(String name, GncAccount account, BigDecimal percent) {
		final String guid = guid();
		jdbcTemplate.update("""
				INSERT INTO taxtables (guid, name, refcount, invisible, parent)
				VALUES (?, ?, 0, 0, NULL)
				""", guid, name);
		jdbcTemplate.update("""
				INSERT INTO taxtable_entries (taxtable, account, amount_num, amount_denom, type)
				VALUES (?, ?, ?, 100000, 2)
				""", guid, account.guid(), percent.movePointRight(5).longValueExact());
		return new GncTaxTable(guid, name, 0, false, null,
				List.of(new GncTaxTableEntry(account.guid(),
						new GncNumeric(percent.movePointRight(5).longValueExact(), 100000),
						GncTaxTableEntry.TYPE_PERCENT)));
	}

	/**
	 * Add a customer.
	 *
	 * @param id
	 *        the customer ID
	 * @param currency
	 *        the currency
	 * @param taxTable
	 *        the tax table, or {@code null}
	 * @return the customer GUID
	 */
	protected String insertCustomer(String id, GncCommodity currency, @Nullable GncTaxTable taxTable) {
		final String guid = guid();
		jdbcTemplate.update("""
				INSERT INTO customers (guid, name, id, notes, active, discount_num, discount_denom
					, credit_num, credit_denom, currency, tax_override
					, addr_name, addr_addr1, addr_addr2, addr_addr3, addr_addr4
					, addr_phone, addr_fax, addr_email
					, shipaddr_name, shipaddr_addr1, shipaddr_addr2, shipaddr_addr3, shipaddr_addr4
					, shipaddr_phone, shipaddr_fax, shipaddr_email, terms, tax_included, taxtable)
				VALUES (?, ?, ?, '', 1, 0, 1, 0, 1, ?, ?, ?, '', '', '', '', '', '', ''
					, '', '', '', '', '', '', '', '', NULL, ?, ?)
				""", guid, CUSTOMER_NAME, id, currency.guid(), taxTable != null ? 1 : 0, CUSTOMER_NAME,
				GncCustomer.TAX_INCLUDED_NO, (taxTable != null ? taxTable.guid() : null));
		return guid;
	}

	/**
	 * Add a lock row, as GnuCash does when it opens the book.
	 */
	protected void insertLock() {
		jdbcTemplate.update("INSERT INTO gnclock (hostname, pid) VALUES ('test-host', 1234)");
	}

	/**
	 * Add a slot.
	 *
	 * @param objGuid
	 *        the object GUID
	 * @param name
	 *        the name
	 * @param type
	 *        the slot type
	 * @param int64
	 *        the integer value
	 * @param string
	 *        the string value
	 * @param guidVal
	 *        the GUID value
	 */
	protected void insertSlot(String objGuid, String name, int type, long int64, @Nullable String string,
			@Nullable String guidVal) {
		jdbcTemplate.update("""
				INSERT INTO slots (obj_guid, name, slot_type, int64_val, string_val, double_val
					, timespec_val, guid_val, numeric_val_num, numeric_val_denom, gdate_val)
				VALUES (?, ?, ?, ?, ?, NULL, '1970-01-01 00:00:00', ?, 0, 1, NULL)
				""", objGuid, name, type, int64, string, guidVal);
	}

	/**
	 * Add a customer payment that is not applied to any invoice, the way
	 * GnuCash stores a pre-payment.
	 *
	 * @param customerGuid
	 *        the customer GUID
	 * @param amount
	 *        the amount paid
	 * @param date
	 *        the payment date
	 * @return the GUID of the payment lot
	 */
	protected String insertPrepayment(String customerGuid, BigDecimal amount, Instant date) {
		final String txGuid = guid();
		final String lotGuid = guid();
		final long cents = amount.movePointRight(2).longValueExact();
		jdbcTemplate.update("""
				INSERT INTO transactions (guid, currency_guid, num, post_date, enter_date, description)
				VALUES (?, ?, '', ?, ?, ?)
				""", txGuid, nzd.guid(), gncTimestamp(date), gncTimestamp(date), CUSTOMER_NAME);
		insertSlot(txGuid, "trans-txn-type", 4, 0, "P", null);
		jdbcTemplate.update("INSERT INTO lots (guid, account_guid, is_closed) VALUES (?, ?, -1)",
				lotGuid, receivable.guid());
		final String frameGuid = guid();
		insertSlot(lotGuid, "gncOwner", 9, 0, null, frameGuid);
		insertSlot(frameGuid, "gncOwner/owner-type", 1, 2, null, null);
		insertSlot(frameGuid, "gncOwner/owner-guid", 5, 0, null, customerGuid);
		final String split = """
				INSERT INTO splits (guid, tx_guid, account_guid, memo, action, reconcile_state
					, reconcile_date, value_num, value_denom, quantity_num, quantity_denom, lot_guid)
				VALUES (?, ?, ?, '', 'Payment', 'n', '1970-01-01 00:00:00', ?, 100, ?, 100, ?)
				""";
		jdbcTemplate.update(split, guid(), txGuid, bank.guid(), cents, cents, null);
		jdbcTemplate.update(split, guid(), txGuid, receivable.guid(), -cents, -cents, lotGuid);
		return lotGuid;
	}

}
