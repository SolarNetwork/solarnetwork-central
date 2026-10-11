/* ==================================================================
 * GnucashAccountingSystemTests.java - 11/10/2026 3:01:25 pm
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

import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Credit;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Tax;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Usage;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfTaxCodeResolver;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashAccountingSystem;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashBookLockedException;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashSettings;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashSyncException;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.JdbcGnucashDao;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the {@link GnucashAccountingSystem} class, recording invoices
 * in the {@code gnucashtest} database.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class GnucashAccountingSystemTests extends AbstractGnucashTestSupport {

	private static final Long USER_ID = 123L;
	private static final Long ACCOUNT_ID = 456L;
	private static final String CUSTOMER_ID = "SN_123";
	private static final Instant CREATED = Instant.parse("2026-10-05T04:30:00Z");
	private static final LocalDate START = LocalDate.of(2026, 9, 1);
	private static final Instant POST_DATE = Instant.parse("2026-10-05T10:59:00Z");

	@Mock
	private SnfInvoicingSystem invoicingSystem;

	@Mock
	private SnfTaxCodeResolver taxCodeResolver;

	@Mock
	private TaxCodeDao taxCodeDao;

	private GnucashSettings settings;
	private GnucashAccountingSystem service;

	@BeforeEach
	public void setup() {
		settings = new GnucashSettings();
		settings.getCurrencies().put("NZD", currency("Assets:Accounts Receivable",
				"Income:Subscription", "GST (Sales)", "Posted Datum Metrics"));
		settings.getCurrencies().put("USD", currency("Assets:Accounts Receivable USD",
				"Income:Subscription USD", "GST Zero (Sales)", "USD Posted Datum Metrics"));
		service = new GnucashAccountingSystem(Clock.fixed(CREATED, ZoneOffset.UTC),
				transactionTemplate, new JdbcGnucashDao(jdbcTemplate, SCHEMA), settings,
				invoicingSystem, taxCodeResolver, taxCodeDao);
	}

	private static GnucashSettings.CurrencySettings currency(String receivable, String income,
			String taxTable, String summary) {
		var s = new GnucashSettings.CurrencySettings();
		s.setReceivableAccount(receivable);
		s.setIncomeAccount(income);
		s.setTaxTable(taxTable);
		s.setSummaryDescription(summary);
		return s;
	}

	private Account account(String currency) {
		var addr = new Address(USER_ID, "Example Ltd", "accounts@example.com", "NZ",
				"Pacific/Auckland");
		addr.setStreet(new String[] { "Level 1", "1 Main Street" });
		addr.setLocality("Auckland");
		addr.setPostalCode("1010");
		var account = new Account(ACCOUNT_ID, USER_ID, CREATED, currency, "en-NZ");
		account.setAddress(addr);
		return account;
	}

	private SnfInvoice invoice(Long id, String currency, Instant created, LocalDate start) {
		var invoice = new SnfInvoice(id, USER_ID, ACCOUNT_ID, created, start, start.plusMonths(1),
				currency);
		invoice.setAddress(account(currency).getAddress());
		return invoice;
	}

	private static SnfInvoiceItem item(SnfInvoice invoice, InvoiceItemType type, String key,
			String quantity, String amount) {
		return SnfInvoiceItem.newItem(invoice, type, key, new BigDecimal(quantity),
				new BigDecimal(amount));
	}

	private static SnfInvoice withItems(SnfInvoice invoice, SnfInvoiceItem... items) {
		invoice.setItems(new LinkedHashSet<>(Arrays.asList(items)));
		return invoice;
	}

	/**
	 * An NZD invoice: GST is charged on two of the three items.
	 */
	private SnfInvoice nzdInvoice(Long id) {
		var invoice = invoice(id, "NZD", CREATED, START);
		return withItems(invoice, item(invoice, Usage, "datum-props-in", "1234567", "7.12"),
				item(invoice, Usage, "datum-out", "1000", "2.50"),
				item(invoice, Usage, "c2c-data", "5", "1.00"), item(invoice, Tax, "GST", "1", "1.44"));
	}

	private void givenMessages(Account account) {
		var messages = new StaticMessageSource();
		messages.addMessage("datum-props-in.item", account.locale(), "Posted Properties");
		messages.addMessage("datum-out.item", account.locale(), "Queried Datum");
		messages.addMessage("c2c-data.item", account.locale(), "Cloud Integrations Data");
		given(invoicingSystem.messageSourceForInvoice(any())).willReturn(messages);
	}

	private void givenTaxCodes(String... itemKeys) {
		given(taxCodeResolver.taxCodeFilterForInvoice(any())).willReturn(new TaxCodeFilter());
		given(taxCodeDao.findFiltered(any())).willReturn(new BasicFilterResults<>(Arrays
				.stream(itemKeys).map(k -> new TaxCode("NZ", k, "GST", new BigDecimal("0.15"),
						Instant.EPOCH, null))
				.toList()));
	}

	private Map<String, Object> invoiceRow(String id) {
		return jdbcTemplate.queryForMap("SELECT * FROM invoices WHERE id = ?", id);
	}

	private Map<String, Map<String, Object>> slots(String objGuid) {
		return rows("SELECT * FROM slots WHERE obj_guid = ?", objGuid).stream()
				.collect(Collectors.toMap(r -> (String) r.get("name"), r -> r));
	}

	private Object frameGuidValue(String objGuid, String frame) {
		var frameGuid = (String) slots(objGuid).get(frame).get("guid_val");
		return slots(frameGuid).get(frame + "/invoice-guid").get("guid_val");
	}

	/** Split values keyed by account GUID, in cents. */
	private Map<String, Long> splitValues(Object txGuid) {
		return rows("SELECT account_guid, value_num, value_denom FROM splits WHERE tx_guid = ?",
				txGuid).stream().collect(Collectors.toMap(r -> (String) r.get("account_guid"),
						r -> (Long) r.get("value_num") * 100 / (Long) r.get("value_denom"),
						Long::sum));
	}

	private BigDecimal lotBalance(Object lotGuid) {
		BigDecimal result = jdbcTemplate.queryForObject("""
				SELECT COALESCE(SUM(quantity_num::NUMERIC / quantity_denom), 0)
				FROM splits WHERE lot_guid = ?
				""", BigDecimal.class, lotGuid);
		return (result != null ? result : BigDecimal.ZERO);
	}

	@Test
	public void nzdInvoice_itemized() {
		// GIVEN
		final String customerGuid = insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in", "datum-out");
		final SnfInvoice invoice = nzdInvoice(1931L);

		// WHEN
		boolean result = service.syncInvoice(invoice, account);

		// THEN
		then(result).as("Invoice added").isTrue();

		final Map<String, Object> inv = invoiceRow("1931");
		// @formatter:off
		then(inv)
			.as("Invoice posted for customer to receivable account")
			.containsEntry("owner_type", 2)
			.containsEntry("owner_guid", customerGuid)
			.containsEntry("currency", nzd.guid())
			.containsEntry("post_acc", receivable.guid())
			.containsEntry("billing_id", "INV-1HN")
			.containsEntry("notes", "")
			.containsEntry("active", 1)
			;
		then(instant(inv.get("date_opened")))
			.as("Opened on the SNF invoice date, at GnuCash's day-neutral time")
			.isEqualTo(POST_DATE)
			;
		then(instant(inv.get("date_posted")))
			.as("Posted on the SNF invoice date")
			.isEqualTo(POST_DATE)
			;
		then(slots((String) inv.get("guid")).get("credit-note"))
			.as("Not a credit note")
			.containsEntry("int64_val", 0L)
			;

		final var copies = rows("SELECT * FROM taxtables WHERE parent = ?", gstTable.guid());
		then(copies)
			.as("Hidden copy of GST table made for posting")
			.singleElement()
			.satisfies(t -> then(t).containsEntry("invisible", 1).containsEntry("name", "GST (Sales)"))
			;
		final Object taxTableCopyGuid = copies.getFirst().get("guid");

		final var entries = rows("""
				SELECT description, quantity_num, quantity_denom, i_price_num, i_price_denom
					, i_acct, i_taxable, i_taxincluded, i_taxtable, date
				FROM entries WHERE invoice = ? ORDER BY i_price_num DESC
				""", inv.get("guid"));
		then(entries)
			.as("One line per SNF usage item")
			.hasSize(3)
			.allSatisfy(e -> {
				then(e).containsEntry("i_acct", income.guid()).containsEntry("quantity_num", 1L)
						.containsEntry("i_taxincluded", 0);
				then(instant(e.get("date"))).as("Line dated noon on the billing period start, NZ time")
						.isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
			})
			;
		then(entries.get(0))
			.as("Taxed item, with quantity in description")
			.containsEntry("description", "Posted Properties (1,234,567)")
			.containsEntry("i_price_num", 712L)
			.containsEntry("i_price_denom", 100L)
			.containsEntry("i_taxable", 1)
			.containsEntry("i_taxtable", taxTableCopyGuid)
			;
		then(entries.get(1))
			.as("Second taxed item")
			.containsEntry("description", "Queried Datum (1,000)")
			.containsEntry("i_taxable", 1)
			;
		then(entries.get(2))
			.as("Item SNF did not tax is not taxable")
			.containsEntry("description", "Cloud Integrations Data (5)")
			.containsEntry("i_taxable", 0)
			.containsEntry("i_taxtable", null)
			;

		final Object txGuid = inv.get("post_txn");
		then(jdbcTemplate.queryForMap("SELECT * FROM transactions WHERE guid = ?", txGuid))
			.as("Transaction numbered with invoice ID and described with customer name")
			.containsEntry("num", "1931")
			.containsEntry("description", CUSTOMER_NAME)
			.containsEntry("currency_guid", nzd.guid())
			;
		final var txSlots = slots((String) txGuid);
		then(txSlots.get("trans-txn-type")).containsEntry("string_val", "I");
		then(txSlots.get("trans-read-only")).containsEntry("string_val",
				GnucashAccountingSystem.POSTED_READ_ONLY_REASON);
		then(txSlots.get("date-posted")).containsEntry("gdate_val", Date.valueOf("2026-10-05"));
		then(instant(txSlots.get("trans-date-due").get("timespec_val")))
			.as("Due a month after the invoice date")
			.isEqualTo(Instant.parse("2026-11-05T10:59:00Z"))
			;
		then(frameGuidValue((String) txGuid, "gncInvoice"))
			.as("Transaction linked to invoice")
			.isEqualTo(inv.get("guid"))
			;

		then(splitValues(txGuid))
			.as("Income and GST credited, receivable debited with the SNF total")
			.containsOnly(Map.entry(income.guid(), -1062L), Map.entry(gst.guid(), -144L),
					Map.entry(receivable.guid(), 1206L))
			;
		then(rows("SELECT action, memo, reconcile_state FROM splits WHERE tx_guid = ?", txGuid))
			.allSatisfy(s -> then(s).containsEntry("action", "Invoice").containsEntry("memo", "")
					.containsEntry("reconcile_state", "n"))
			;

		final Object lotGuid = inv.get("post_lot");
		then(lotBalance(lotGuid)).as("Receivable lot holds the invoice total").isEqualByComparingTo("12.06");
		then(slots((String) lotGuid).get("title")).containsEntry("string_val", "Invoice 1931");
		then(frameGuidValue((String) lotGuid, "gncInvoice"))
			.as("Lot linked to invoice")
			.isEqualTo(inv.get("guid"))
			;
		// @formatter:on
	}

	@Test
	public void nzdInvoice_reusesPostedTaxTableCopy() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in", "datum-out");

		// WHEN
		service.syncInvoice(nzdInvoice(1931L), account);
		service.syncInvoice(nzdInvoice(1932L), account);

		// THEN
		// @formatter:off
		then(rows("SELECT * FROM taxtables WHERE parent = ?", gstTable.guid()))
			.as("Second invoice uses the copy made for the first")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void usdInvoice_zeroRated() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, usd, gstZeroTable);
		final Account account = account("USD");
		givenMessages(account);
		final SnfInvoice invoice = invoice(1921L, "USD", CREATED, START);
		withItems(invoice, item(invoice, Usage, "datum-props-in", "1000000", "305.46"));

		// WHEN
		service.syncInvoice(invoice, account);

		// THEN
		final Map<String, Object> inv = invoiceRow("1921");
		final Object txGuid = inv.get("post_txn");
		// @formatter:off
		then(inv).containsEntry("post_acc", receivableUsd.guid()).containsEntry("currency", usd.guid());
		then(rows("SELECT i_taxable, i_taxtable FROM entries WHERE invoice = ?", inv.get("guid")))
			.as("Line taxable with a copy of the zero-rated table")
			.singleElement()
			.satisfies(e -> then(e).containsEntry("i_taxable", 1).doesNotContainEntry("i_taxtable", null))
			;
		then(splitValues(txGuid))
			.as("USD income and receivable, and zero GST")
			.containsOnly(Map.entry(incomeUsd.guid(), -30546L), Map.entry(gst.guid(), 0L),
					Map.entry(receivableUsd.guid(), 30546L))
			;
		then(rows("SELECT quantity_num, quantity_denom FROM splits WHERE tx_guid = ? AND account_guid = ?",
				txGuid, gst.guid()))
			.as("Zero GST amount in the NZD account needs no exchange rate")
			.singleElement()
			.satisfies(s -> then(s).containsEntry("quantity_num", 0L).containsEntry("quantity_denom", 100L))
			;
		// @formatter:on
	}

	@Test
	public void nzdInvoice_summary() {
		// GIVEN
		settings.setItemized(false);
		insertCustomer(CUSTOMER_ID, nzd, gstTable);
		givenTaxCodes("datum-props-in", "datum-out");

		// WHEN
		service.syncInvoice(nzdInvoice(1931L), account("NZD"));

		// THEN
		final Map<String, Object> inv = invoiceRow("1931");
		// @formatter:off
		then(rows("""
				SELECT description, i_price_num, i_price_denom, i_taxable FROM entries
				WHERE invoice = ? ORDER BY i_taxable DESC
				""", inv.get("guid")))
			.as("One summary line for taxed items and one for the rest")
			.satisfiesExactly(
				e -> then(e).containsEntry("description", "Posted Datum Metrics")
						.containsEntry("i_price_num", 962L).containsEntry("i_taxable", 1),
				e -> then(e).containsEntry("description", "Posted Datum Metrics")
						.containsEntry("i_price_num", 1L).containsEntry("i_price_denom", 1L)
						.containsEntry("i_taxable", 0))
			;
		then(splitValues(inv.get("post_txn")))
			.as("Same totals as itemized")
			.containsOnly(Map.entry(income.guid(), -1062L), Map.entry(gst.guid(), -144L),
					Map.entry(receivable.guid(), 1206L))
			;
		// @formatter:on
	}

	@Test
	public void taxMismatch() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in", "datum-out", "c2c-data");

		// WHEN
		// @formatter:off
		thenExceptionOfType(GnucashSyncException.class)
			.as("GnuCash would also tax c2c-data, so its tax does not match")
			.isThrownBy(() -> service.syncInvoice(nzdInvoice(1931L), account))
			.withMessageContaining("1.59 tax")
			;
		then(rows("SELECT * FROM invoices"))
			.as("Nothing written")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void alreadyPresent() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in", "datum-out");
		service.syncInvoice(nzdInvoice(1931L), account);

		// WHEN
		boolean result = service.syncInvoice(nzdInvoice(1931L), account);

		// THEN
		// @formatter:off
		then(result).as("Invoice already present").isFalse();
		then(rows("SELECT * FROM invoices WHERE id = '1931'")).as("Not added again").hasSize(1);
		// @formatter:on
	}

	@Test
	public void alreadyPresent_otherCustomer() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, nzd, gstTable);
		String otherGuid = insertCustomer("SN_999", nzd, gstTable);
		jdbcTemplate.update("""
				INSERT INTO invoices (guid, id, notes, active, currency, owner_type, owner_guid
					, billing_id, charge_amt_num, charge_amt_denom)
				VALUES (?, '1931', '', 1, ?, 2, ?, '', 0, 1)
				""", guid(), nzd.guid(), otherGuid);

		// WHEN
		// @formatter:off
		thenExceptionOfType(GnucashSyncException.class)
			.isThrownBy(() -> service.syncInvoice(nzdInvoice(1931L), account("NZD")))
			.withMessageContaining("does not belong to customer SN_123")
			;
		// @formatter:on
	}

	@Test
	public void createCustomer() {
		// GIVEN
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in", "datum-out");

		// WHEN
		service.syncInvoice(nzdInvoice(1931L), account);

		// THEN
		// @formatter:off
		final var customers = rows("SELECT * FROM customers WHERE id = ?", CUSTOMER_ID);
		then(customers)
			.as("Customer created from billing account")
			.singleElement()
			.satisfies(c -> then(c)
				.containsEntry("name", "Example Ltd")
				.containsEntry("currency", nzd.guid())
				.containsEntry("tax_override", 1)
				.containsEntry("taxtable", gstTable.guid())
				.containsEntry("addr_name", "Example Ltd")
				.containsEntry("addr_addr1", "Level 1")
				.containsEntry("addr_addr2", "1 Main Street")
				.containsEntry("addr_addr3", "Auckland 1010")
				.containsEntry("addr_addr4", "New Zealand")
				.containsEntry("addr_email", "accounts@example.com")
				.containsEntry("shipaddr_name", "")
				.containsEntry("terms", null)
				.containsEntry("active", 1))
			;
		then(jdbcTemplate.queryForObject("SELECT refcount FROM taxtables WHERE guid = ?", Long.class,
				gstTable.guid()))
			.as("Tax table reference added for customer")
			.isEqualTo(1L)
			;
		then(invoiceRow("1931"))
			.as("Invoice owned by new customer")
			.containsEntry("owner_guid", customers.getFirst().get("guid"))
			;
		// @formatter:on
	}

	@Test
	public void createCustomer_disabled() {
		// GIVEN
		settings.setCreateCustomers(false);

		// WHEN
		// @formatter:off
		thenExceptionOfType(GnucashSyncException.class)
			.isThrownBy(() -> service.syncInvoice(nzdInvoice(1931L), account("NZD")))
			.withMessage("GnuCash has no customer SN_123.")
			;
		// @formatter:on
	}

	@Test
	public void bookOpen() {
		// GIVEN
		insertLock();

		// WHEN
		// @formatter:off
		thenExceptionOfType(GnucashBookLockedException.class)
			.isThrownBy(() -> service.syncInvoice(nzdInvoice(1931L), account("NZD")))
			;
		// @formatter:on
	}

	/**
	 * An NZD invoice of 113.78, less account credit.
	 */
	private SnfInvoice creditedInvoice(Long id, String credit) {
		var invoice = invoice(id, "NZD", CREATED, START);
		return withItems(invoice, item(invoice, Usage, "datum-props-in", "1000", "98.94"),
				item(invoice, Tax, "GST", "1", "14.84"),
				item(invoice, Credit, AccountBalance.ACCOUNT_CREDIT_KEY, "1", "-" + credit));
	}

	@Test
	public void credit_partOfPrepayment() {
		// GIVEN
		final String customerGuid = insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final String prepaymentLot = insertPrepayment(customerGuid, new BigDecimal("129.45"),
				Instant.parse("2026-08-21T10:59:00Z"));
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in");

		// WHEN
		service.syncInvoice(creditedInvoice(1931L, "113.78"), account);

		// THEN
		final Map<String, Object> inv = invoiceRow("1931");
		// @formatter:off
		then(splitValues(inv.get("post_txn")))
			.as("Invoice posted for the full amount")
			.containsEntry(receivable.guid(), 11378L)
			;
		then(lotBalance(inv.get("post_lot")))
			.as("Invoice paid in full by the prepayment")
			.isEqualByComparingTo("0")
			;
		then(lotBalance(prepaymentLot))
			.as("Rest of the prepayment still available")
			.isEqualByComparingTo("-15.67")
			;
		then(rows("""
				SELECT s.value_num, s.lot_guid FROM splits s
				WHERE s.tx_guid = (SELECT tx_guid FROM splits WHERE lot_guid = ? LIMIT 1)
					AND s.account_guid = ?
				ORDER BY s.value_num
				""", prepaymentLot, receivable.guid()))
			.as("Payment's receivable split split between the invoice and its own lot")
			.satisfiesExactly(
				s -> then(s).containsEntry("value_num", -11378L).containsEntry("lot_guid", inv.get("post_lot")),
				s -> then(s).containsEntry("value_num", -1567L).containsEntry("lot_guid", prepaymentLot))
			;
		// @formatter:on
	}

	@Test
	public void credit_allOfPrepayment() {
		// GIVEN
		final String customerGuid = insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final String prepaymentLot = insertPrepayment(customerGuid, new BigDecimal("50.00"),
				Instant.parse("2026-08-21T10:59:00Z"));
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in");

		// WHEN
		service.syncInvoice(creditedInvoice(1931L, "50.00"), account);

		// THEN
		final Map<String, Object> inv = invoiceRow("1931");
		// @formatter:off
		then(lotBalance(inv.get("post_lot")))
			.as("Invoice balance is the SNF invoice total")
			.isEqualByComparingTo("63.78")
			;
		then(rows("SELECT * FROM lots WHERE guid = ?", prepaymentLot))
			.as("Emptied prepayment lot deleted, as GnuCash does")
			.isEmpty()
			;
		then(rows("SELECT * FROM slots WHERE obj_guid = ?", prepaymentLot))
			.as("Prepayment lot slots deleted")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void credit_notEnough() {
		// GIVEN
		final String customerGuid = insertCustomer(CUSTOMER_ID, nzd, gstTable);
		insertPrepayment(customerGuid, new BigDecimal("10.00"), Instant.parse("2026-08-21T10:59:00Z"));
		final Account account = account("NZD");
		givenMessages(account);
		givenTaxCodes("datum-props-in");

		// WHEN
		// @formatter:off
		thenExceptionOfType(GnucashSyncException.class)
			.isThrownBy(() -> service.syncInvoice(creditedInvoice(1931L, "50.00"), account))
			.withMessageContaining("has only 10")
			;
		then(rows("SELECT * FROM invoices")).as("Nothing written").isEmpty();
		// @formatter:on
	}

	private SnfInvoice creditGrant(Long id, String amount, LocalDate date) {
		var invoice = invoice(id, "NZD", CREATED, date);
		var item = SnfInvoiceItem.newItem(id, Credit, AccountBalance.ACCOUNT_CREDIT_ADD_KEY,
				BigDecimal.ONE, new BigDecimal(amount).negate(), CREATED,
				Map.of(GnucashAccountingSystem.META_DESCRIPTION, "Credit for outage"));
		return withItems(invoice, item);
	}

	@Test
	public void creditGrant_creditNote() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, nzd, gstTable);

		// WHEN
		service.syncInvoice(creditGrant(1940L, "100.00", LocalDate.of(2026, 10, 8)), account("NZD"));

		// THEN
		final Map<String, Object> cn = invoiceRow("1940");
		// @formatter:off
		then(slots((String) cn.get("guid")).get("credit-note"))
			.as("Added as a credit note")
			.containsEntry("int64_val", 1L)
			;
		then(instant(cn.get("date_posted")))
			.as("Posted on the credit date")
			.isEqualTo(Instant.parse("2026-10-08T10:59:00Z"))
			;
		then(rows("SELECT * FROM entries WHERE invoice = ?", cn.get("guid")))
			.as("One untaxed line, with the quantity negated as GnuCash stores credit notes")
			.singleElement()
			.satisfies(e -> then(e)
				.containsEntry("description", "Credit for outage")
				.containsEntry("quantity_num", -1L)
				.containsEntry("i_price_num", 100L)
				.containsEntry("i_price_denom", 1L)
				.containsEntry("i_acct", income.guid())
				.containsEntry("i_taxable", 0))
			;
		then(splitValues(cn.get("post_txn")))
			.as("Receivable credited and income debited")
			.containsOnly(Map.entry(receivable.guid(), -10000L), Map.entry(income.guid(), 10000L))
			;
		then(rows("SELECT DISTINCT action FROM splits WHERE tx_guid = ?", cn.get("post_txn")))
			.singleElement()
			.satisfies(s -> then(s).containsEntry("action", "Credit Note"))
			;
		then(slots((String) cn.get("post_lot")).get("title"))
			.containsEntry("string_val", "Credit Note 1940")
			;
		then(lotBalance(cn.get("post_lot"))).as("Credit open").isEqualByComparingTo("-100");
		// @formatter:on
	}

	@Test
	public void creditGrant_taxable() {
		// GIVEN
		settings.setCreditNoteTaxable(true);
		insertCustomer(CUSTOMER_ID, nzd, gstTable);

		// WHEN
		service.syncInvoice(creditGrant(1940L, "115.00", LocalDate.of(2026, 10, 8)), account("NZD"));

		// THEN
		final Map<String, Object> cn = invoiceRow("1940");
		// @formatter:off
		then(rows("SELECT i_taxable, i_taxincluded FROM entries WHERE invoice = ?", cn.get("guid")))
			.as("Taxable line, with tax included in the credit amount")
			.singleElement()
			.satisfies(e -> then(e).containsEntry("i_taxable", 1).containsEntry("i_taxincluded", 1))
			;
		then(splitValues(cn.get("post_txn")))
			.as("GST reduced by the tax in the credit")
			.containsOnly(Map.entry(receivable.guid(), -11500L), Map.entry(income.guid(), 10000L),
					Map.entry(gst.guid(), 1500L))
			;
		// @formatter:on
	}

	@Test
	public void credit_creditNote() {
		// GIVEN
		insertCustomer(CUSTOMER_ID, nzd, gstTable);
		final Account account = account("NZD");
		service.syncInvoice(creditGrant(1940L, "100.00", LocalDate.of(2026, 10, 8)), account);
		givenMessages(account);
		givenTaxCodes("datum-props-in");

		// WHEN
		service.syncInvoice(creditedInvoice(1941L, "100.00"), account);

		// THEN
		final Map<String, Object> cn = invoiceRow("1940");
		final Map<String, Object> inv = invoiceRow("1941");
		// @formatter:off
		then(lotBalance(inv.get("post_lot")))
			.as("Invoice balance is the SNF invoice total")
			.isEqualByComparingTo("13.78")
			;
		then(lotBalance(cn.get("post_lot")))
			.as("Credit note used up")
			.isEqualByComparingTo("0")
			;
		final var links = rows("""
				SELECT s.value_num, s.lot_guid, s.action, s.memo, t.post_date, t.num FROM splits s
				INNER JOIN transactions t ON t.guid = s.tx_guid
				INNER JOIN slots k ON k.obj_guid = t.guid AND k.name = 'trans-txn-type'
				WHERE k.string_val = 'L'
				ORDER BY s.value_num
				""");
		then(links)
			.as("Lot link transaction offsets the invoice and credit note")
			.satisfiesExactly(
				s -> then(s).containsEntry("value_num", -10000L).containsEntry("lot_guid", inv.get("post_lot")),
				s -> then(s).containsEntry("value_num", 10000L).containsEntry("lot_guid", cn.get("post_lot")))
			.allSatisfy(s -> then(s)
				.containsEntry("action", "Lot Link")
				.containsEntry("num", "")
				.containsEntry("memo", "Offset between documents: Credit Note 1940 - Invoice 1941"))
			;
		then(instant(links.getFirst().get("post_date")))
			.as("Link dated with the later document")
			.isEqualTo(Instant.parse("2026-10-08T10:59:00Z"))
			;
		// @formatter:on
	}

	@Test
	public void dayNeutral() {
		// @formatter:off
		then(GnucashAccountingSystem.dayNeutral(LocalDate.of(2026, 10, 5), ZoneId.of("Pacific/Auckland")))
			.as("10:59 UTC for New Zealand")
			.isEqualTo(Instant.parse("2026-10-05T10:59:00Z"))
			;
		then(GnucashAccountingSystem.dayNeutral(LocalDate.of(2026, 10, 5), ZoneId.of("Pacific/Kiritimati")))
			.as("Moved earlier for a zone 14 hours ahead")
			.isEqualTo(Instant.parse("2026-10-05T09:59:00Z"))
			;
		then(GnucashAccountingSystem.dayNeutral(LocalDate.of(2026, 10, 5), ZoneId.of("Pacific/Pago_Pago")))
			.as("Moved later for a zone 11 hours behind")
			.isEqualTo(Instant.parse("2026-10-05T11:59:00Z"))
			;
		then(GnucashAccountingSystem.localNoon(LocalDate.of(2026, 10, 5), ZoneId.of("Pacific/Auckland")))
			.as("Noon NZDT")
			.isEqualTo(Instant.parse("2026-10-04T23:00:00Z"))
			;
		// @formatter:on
	}

}
