/* ==================================================================
 * SnfInvoicingSystemTests.java - 22/07/2020 9:19:57 AM
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

package net.solarnetwork.central.user.billing.snf.test;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.dryRunOptions;
import static net.solarnetwork.central.user.billing.snf.test.SnfMatchers.matchesFilter;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceNodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the {@link SnfBillingSystem} implementation of
 * {@link SnfInvoicingSystem}.
 * 
 * @author matt
 * @version 2.0
 */
public class SnfInvoicingSystemTests extends AbstractSnfBililngSystemTest {

	@Test
	public void findLatestInvoice_none() {
		// GIVEN
		UserLongPK pk = new UserLongPK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Capture<SnfInvoiceFilter> filterCaptor = new Capture<>();
		expect(invoiceDao.findFiltered(capture(filterCaptor),
				same(SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING), eq(0), eq(1)))
						.andReturn(new BasicFilterResults<>(emptyList()));

		// WHEN
		replayAll();
		SnfInvoice invoice = invoicingSystem.findLatestInvoiceForAccount(pk);

		// THEN
		assertThat("InvoiceImpl not found.", invoice, nullValue());
		SnfInvoiceFilter filter = filterCaptor.getValue();
		assertThat("Query filter was by account ID", filter.getAccountId(), equalTo(pk.getId()));
	}

	@Test
	public void findLatestInvoice_found() {
		// GIVEN
		UserLongPK pk = new UserLongPK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Capture<SnfInvoiceFilter> filterCaptor = new Capture<>();
		SnfInvoice inv = new SnfInvoice(pk.getId());
		expect(invoiceDao.findFiltered(capture(filterCaptor),
				same(SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING), eq(0), eq(1)))
						.andReturn(new BasicFilterResults<>(singleton(inv)));

		// WHEN
		replayAll();
		SnfInvoice invoice = invoicingSystem.findLatestInvoiceForAccount(pk);

		// THEN
		assertThat("InvoiceImpl returned from DAO.", invoice, sameInstance(inv));
		SnfInvoiceFilter filter = filterCaptor.getValue();
		assertThat("Query filter was by account ID", filter.getAccountId(), equalTo(pk.getId()));
	}

	@Test
	public void accountForUser_none() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		expect(accountDao.getForUser(userId)).andReturn(null);

		// WHEN
		replayAll();
		Account result = invoicingSystem.accountForUser(userId);

		// THEN
		assertThat("Account not found.", result, nullValue());
	}

	@Test
	public void accountForUser_found() {
		// GIVEN
		final Account account = new Account(userId, Instant.now());
		expect(accountDao.getForUser(userId)).andReturn(account);

		// WHEN
		replayAll();
		Account result = invoicingSystem.accountForUser(userId);

		// THEN
		assertThat("DAO result returned.", result, sameInstance(account));
	}

	private static void assertUsageItem(SnfInvoice invoice, SnfInvoiceItem item, BigInteger quantity,
			BigDecimal amount) {
		assertThat(item.getKey() + " Item ID generated", item.getId(), notNullValue());
		assertThat(item.getKey() + " Item invoice ID", item.getInvoiceId(),
				equalTo(invoice.getId().getId()));
		assertThat(item.getKey() + " Item type", item.getItemType(), equalTo(InvoiceItemType.Usage));
		assertThat(item.getKey() + " Item quantity", item.getQuantity(),
				equalTo(new BigDecimal(quantity)));
		assertThat(item.getKey() + " Item amount", item.getAmount(), equalTo(amount));
	}

	private static void assertTaxItem(SnfInvoice invoice, SnfInvoiceItem item, BigDecimal amount) {
		assertThat(item.getKey() + " Item ID generated", item.getId(), notNullValue());
		assertThat(item.getKey() + " Item invoice ID", item.getInvoiceId(),
				equalTo(invoice.getId().getId()));
		assertThat(item.getKey() + " Item type", item.getItemType(), equalTo(InvoiceItemType.Tax));
		assertThat(item.getKey() + " Item quantity", item.getQuantity(), equalTo(BigDecimal.ONE));
		assertThat(item.getKey() + " Item amount", item.getAmount(), equalTo(amount));
	}

	private static void assertCreditItem(SnfInvoice invoice, SnfInvoiceItem item, BigDecimal amount,
			BigDecimal remainingCredit) {
		assertThat(item.getKey() + " Item ID generated", item.getId(), notNullValue());
		assertThat(item.getKey() + " Item invoice ID", item.getInvoiceId(),
				equalTo(invoice.getId().getId()));
		assertThat(item.getKey() + " Item type", item.getItemType(), equalTo(InvoiceItemType.Credit));
		assertThat(item.getKey() + " Item quantity", item.getQuantity(), equalTo(BigDecimal.ONE));
		assertThat(item.getKey() + " Item amount", item.getAmount(), equalTo(amount));
		assertThat(item.getKey() + " Item metadata available credit",
				item.getMetadata().get(SnfInvoiceItem.META_AVAILABLE_CREDIT),
				equalTo(remainingCredit.toPlainString()));
	}

	private static void assertInvoiceNodeUsage(SnfInvoice invoice, SnfInvoiceNodeUsage usage,
			Long nodeId, BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored) {
		assertThat(format("Invoice %d node usage node ID", invoice.getId().getId()), usage.getNodeId(),
				is(equalTo(nodeId)));
		assertThat(format("Invoice %d node usage datumPropertiesIn", invoice.getId().getId()),
				usage.getDatumPropertiesIn(), is(equalTo(datumPropertiesIn)));
		assertThat(format("Invoice %d node usage datumOut", invoice.getId().getId()),
				usage.getDatumOut(), is(equalTo(datumOut)));
		assertThat(format("Invoice %d node usage datumDaysStored", invoice.getId().getId()),
				usage.getDatumDaysStored(), is(equalTo(datumDaysStored)));
	}

	private static void assertInvoiceNodeUsage(SnfInvoice invoice, SnfInvoiceNodeUsage usage,
			NodeUsage expected) {
		assertInvoiceNodeUsage(invoice, usage, expected.getId(), expected.getDatumPropertiesIn(),
				expected.getDatumOut(), expected.getDatumDaysStored());
	}

	@Test
	public void generateInvoice_basic_dryRun() {
		// GIVEN
		final Address addr = new Address();
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		final Account account = new Account(randomUUID().getMostSignificantBits(), userId,
				Instant.now());
		account.setAddress(addr);
		expect(accountDao.getForUser(userId, endDate)).andReturn(account);

		final NodeUsage usage = new NodeUsage();
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumPropertiesInCost(new BigDecimal("1.23"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumOutCost(new BigDecimal("2.34"));
		usage.setDatumDaysStored(new BigInteger("345"));
		usage.setDatumDaysStoredCost(new BigDecimal("3.45"));
		usage.setTotalCost(new BigDecimal("7.02"));

		expect(usageDao.findUsageForAccount(userId, startDate, endDate)).andReturn(singletonList(usage));

		final NodeUsage nodeUsage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumDaysStored(new BigInteger("345"));

		expect(usageDao.findNodeUsageForAccount(userId, startDate, endDate))
				.andReturn(singletonList(nodeUsage));

		Capture<TaxCodeFilter> taxCodeFilterCaptor = new Capture<>();
		BasicFilterResults<TaxCode, Long> taxCodeResults = new BasicFilterResults<>(emptyList());
		expect(taxCodeDao.findFiltered(EasyMock.capture(taxCodeFilterCaptor), isNull(), isNull(),
				isNull())).andReturn(taxCodeResults);

		// WHEN
		replayAll();
		SnfInvoice invoice = invoicingSystem.generateInvoice(userId, startDate, endDate,
				dryRunOptions());

		// THEN
		assertThat("Invoice created", invoice, notNullValue());
		assertThat("Invoice has draft ID", invoice.getId(),
				equalTo(new UserLongPK(userId, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID)));
		assertThat("Invoice items created for all usage", invoice.getItems(), hasSize(3));

		Map<String, SnfInvoiceItem> itemMap = invoice.getItemsByKey();
		assertThat("Invoice item mapping contains all items", itemMap.keySet(), contains(
				NodeUsage.DATUM_PROPS_IN_KEY, NodeUsage.DATUM_OUT_KEY, NodeUsage.DATUM_DAYS_STORED_KEY));

		SnfInvoiceItem item;
		item = itemMap.get(NodeUsage.DATUM_PROPS_IN_KEY);
		assertUsageItem(invoice, item, usage.getDatumPropertiesIn(), usage.getDatumPropertiesInCost());
		item = itemMap.get(NodeUsage.DATUM_OUT_KEY);
		assertUsageItem(invoice, item, usage.getDatumOut(), usage.getDatumOutCost());
		item = itemMap.get(NodeUsage.DATUM_DAYS_STORED_KEY);
		assertUsageItem(invoice, item, usage.getDatumDaysStored(), usage.getDatumDaysStoredCost());

		assertThat("Filtered for appropriate tax codes", taxCodeFilterCaptor.getValue(), matchesFilter(
				invoice.getStartDate().atStartOfDay(addr.getTimeZone()).toInstant(), "NZ"));

		assertThat("Invoice node usage items created", invoice.getUsages(), hasSize(1));
		SnfInvoiceNodeUsage invoiceNodeUsage = invoice.getUsages().iterator().next();
		assertInvoiceNodeUsage(invoice, invoiceNodeUsage, nodeUsage);
	}

	@Test
	public void generateInvoice_withTax_dryRun() {
		// GIVEN
		final Address addr = new Address();
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		final Account account = new Account(randomUUID().getMostSignificantBits(), userId,
				Instant.now());
		account.setAddress(addr);
		expect(accountDao.getForUser(userId, endDate)).andReturn(account);

		final NodeUsage usage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumPropertiesInCost(new BigDecimal("1.23"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumOutCost(new BigDecimal("2.34"));
		usage.setDatumDaysStored(new BigInteger("345"));
		usage.setDatumDaysStoredCost(new BigDecimal("3.45"));
		usage.setTotalCost(new BigDecimal("7.02"));

		expect(usageDao.findUsageForAccount(userId, startDate, endDate)).andReturn(singletonList(usage));

		final NodeUsage nodeUsage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumDaysStored(new BigInteger("345"));

		expect(usageDao.findNodeUsageForAccount(userId, startDate, endDate))
				.andReturn(singletonList(nodeUsage));

		Capture<TaxCodeFilter> taxCodeFilterCaptor = new Capture<>();
		TaxCode datumPropsTax = new TaxCode("NZ", NodeUsage.DATUM_PROPS_IN_KEY, "GST",
				new BigDecimal("0.10"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		TaxCode datumOutTax = new TaxCode("NZ", NodeUsage.DATUM_OUT_KEY, "GST", new BigDecimal("0.20"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		TaxCode datumStoredTax = new TaxCode("NZ", NodeUsage.DATUM_DAYS_STORED_KEY, "GST",
				new BigDecimal("0.30"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		BasicFilterResults<TaxCode, Long> taxCodeResults = new BasicFilterResults<>(
				asList(datumPropsTax, datumOutTax, datumStoredTax));
		expect(taxCodeDao.findFiltered(EasyMock.capture(taxCodeFilterCaptor), isNull(), isNull(),
				isNull())).andReturn(taxCodeResults);

		final BigDecimal expectedTax = usage.getDatumPropertiesInCost().multiply(datumPropsTax.getRate())
				.add(usage.getDatumOutCost().multiply(datumOutTax.getRate()))
				.add(usage.getDatumDaysStoredCost().multiply(datumStoredTax.getRate()))
				.setScale(2, RoundingMode.HALF_UP);

		// WHEN
		replayAll();
		SnfInvoice invoice = invoicingSystem.generateInvoice(userId, startDate, endDate,
				dryRunOptions());

		// THEN
		assertThat("Invoice created", invoice, notNullValue());
		assertThat("Invoice has draft ID", invoice.getId(),
				equalTo(new UserLongPK(userId, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID)));
		assertThat("Invoice items created for all usage and GST tax", invoice.getItems(), hasSize(4));

		Map<String, SnfInvoiceItem> itemMap = invoice.getItemsByKey();
		assertThat("Invoice item mapping contains all items", itemMap.keySet(),
				contains(NodeUsage.DATUM_PROPS_IN_KEY, NodeUsage.DATUM_OUT_KEY,
						NodeUsage.DATUM_DAYS_STORED_KEY, "GST"));

		SnfInvoiceItem item;
		item = itemMap.get(NodeUsage.DATUM_PROPS_IN_KEY);
		assertUsageItem(invoice, item, usage.getDatumPropertiesIn(), usage.getDatumPropertiesInCost());
		item = itemMap.get(NodeUsage.DATUM_OUT_KEY);
		assertUsageItem(invoice, item, usage.getDatumOut(), usage.getDatumOutCost());
		item = itemMap.get(NodeUsage.DATUM_DAYS_STORED_KEY);
		assertUsageItem(invoice, item, usage.getDatumDaysStored(), usage.getDatumDaysStoredCost());
		item = itemMap.get("GST");
		assertTaxItem(invoice, item, expectedTax);

		assertThat("Filtered for appropriate tax codes", taxCodeFilterCaptor.getValue(), matchesFilter(
				invoice.getStartDate().atStartOfDay(addr.getTimeZone()).toInstant(), "NZ"));

		assertThat("Invoice node usage items created", invoice.getUsages(), hasSize(1));
		SnfInvoiceNodeUsage invoiceNodeUsage = invoice.getUsages().iterator().next();
		assertInvoiceNodeUsage(invoice, invoiceNodeUsage, nodeUsage);
	}

	@Test
	public void generateInvoice_withTax_withFullCredit_dryRun() {
		// GIVEN
		final Address addr = new Address();
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		final Account account = new Account(randomUUID().getMostSignificantBits(), userId,
				Instant.now());
		account.setAddress(addr);
		expect(accountDao.getForUser(userId, endDate)).andReturn(account);

		final NodeUsage usage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumPropertiesInCost(new BigDecimal("1.23"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumOutCost(new BigDecimal("2.34"));
		usage.setDatumDaysStored(new BigInteger("345"));
		usage.setDatumDaysStoredCost(new BigDecimal("3.45"));
		usage.setTotalCost(new BigDecimal("7.02"));

		expect(usageDao.findUsageForAccount(userId, startDate, endDate)).andReturn(singletonList(usage));

		final NodeUsage nodeUsage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumDaysStored(new BigInteger("345"));

		expect(usageDao.findNodeUsageForAccount(userId, startDate, endDate))
				.andReturn(singletonList(nodeUsage));

		Capture<TaxCodeFilter> taxCodeFilterCaptor = new Capture<>();
		TaxCode datumPropsTax = new TaxCode("NZ", NodeUsage.DATUM_PROPS_IN_KEY, "GST",
				new BigDecimal("0.10"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		TaxCode datumOutTax = new TaxCode("NZ", NodeUsage.DATUM_OUT_KEY, "GST", new BigDecimal("0.20"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		TaxCode datumStoredTax = new TaxCode("NZ", NodeUsage.DATUM_DAYS_STORED_KEY, "GST",
				new BigDecimal("0.30"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		BasicFilterResults<TaxCode, Long> taxCodeResults = new BasicFilterResults<>(
				asList(datumPropsTax, datumOutTax, datumStoredTax));
		expect(taxCodeDao.findFiltered(EasyMock.capture(taxCodeFilterCaptor), isNull(), isNull(),
				isNull())).andReturn(taxCodeResults);

		final BigDecimal expectedTax = usage.getDatumPropertiesInCost().multiply(datumPropsTax.getRate())
				.add(usage.getDatumOutCost().multiply(datumOutTax.getRate()))
				.add(usage.getDatumDaysStoredCost().multiply(datumStoredTax.getRate()))
				.setScale(2, RoundingMode.HALF_UP);

		final BigDecimal expectedTotal = usage.getTotalCost().add(expectedTax);

		expect(accountDao.claimAccountBalanceCredit(account.getId().getId(),
				usage.getTotalCost().add(expectedTax))).andReturn(expectedTotal);
		final AccountBalance remainingBalance = new AccountBalance(account.getId(), Instant.now(),
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("22.33"));
		expect(accountDao.getBalanceForUser(account.getUserId())).andReturn(remainingBalance);

		// WHEN
		replayAll();
		final SnfInvoicingOptions options = dryRunOptions();
		options.setUseAccountCredit(true);
		SnfInvoice invoice = invoicingSystem.generateInvoice(userId, startDate, endDate, options);

		// THEN
		assertThat("Invoice created", invoice, notNullValue());
		assertThat("Invoice has draft ID", invoice.getId(),
				equalTo(new UserLongPK(userId, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID)));
		assertThat("Invoice items created for all usage and GST tax and credit", invoice.getItems(),
				hasSize(5));

		Map<String, SnfInvoiceItem> itemMap = invoice.getItemsByKey();
		assertThat("Invoice item mapping contains all items", itemMap.keySet(),
				contains(NodeUsage.DATUM_PROPS_IN_KEY, NodeUsage.DATUM_OUT_KEY,
						NodeUsage.DATUM_DAYS_STORED_KEY, "GST", AccountBalance.ACCOUNT_CREDIT_KEY));

		SnfInvoiceItem item;
		item = itemMap.get(NodeUsage.DATUM_PROPS_IN_KEY);
		assertUsageItem(invoice, item, usage.getDatumPropertiesIn(), usage.getDatumPropertiesInCost());
		item = itemMap.get(NodeUsage.DATUM_OUT_KEY);
		assertUsageItem(invoice, item, usage.getDatumOut(), usage.getDatumOutCost());
		item = itemMap.get(NodeUsage.DATUM_DAYS_STORED_KEY);
		assertUsageItem(invoice, item, usage.getDatumDaysStored(), usage.getDatumDaysStoredCost());
		item = itemMap.get("GST");
		assertTaxItem(invoice, item, expectedTax);
		item = itemMap.get(AccountBalance.ACCOUNT_CREDIT_KEY);
		assertCreditItem(invoice, item, expectedTotal.negate(), remainingBalance.getAvailableCredit());

		assertThat("Invoice total has credit applied so zero balance",
				invoice.getTotalAmount().compareTo(BigDecimal.ZERO), equalTo(0));

		assertThat("Filtered for appropriate tax codes", taxCodeFilterCaptor.getValue(), matchesFilter(
				invoice.getStartDate().atStartOfDay(addr.getTimeZone()).toInstant(), "NZ"));

		assertThat("Invoice node usage items created", invoice.getUsages(), hasSize(1));
		SnfInvoiceNodeUsage invoiceNodeUsage = invoice.getUsages().iterator().next();
		assertInvoiceNodeUsage(invoice, invoiceNodeUsage, nodeUsage);
	}

	@Test
	public void generateInvoice_withTax_withPartialCredit_dryRun() {
		// GIVEN
		final Address addr = new Address();
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		final Account account = new Account(randomUUID().getMostSignificantBits(), userId,
				Instant.now());
		account.setAddress(addr);
		expect(accountDao.getForUser(userId, endDate)).andReturn(account);

		final NodeUsage usage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumPropertiesInCost(new BigDecimal("1.23"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumOutCost(new BigDecimal("2.34"));
		usage.setDatumDaysStored(new BigInteger("345"));
		usage.setDatumDaysStoredCost(new BigDecimal("3.45"));
		usage.setTotalCost(new BigDecimal("7.02"));

		expect(usageDao.findUsageForAccount(userId, startDate, endDate)).andReturn(singletonList(usage));

		final NodeUsage nodeUsage = new NodeUsage(randomUUID().getMostSignificantBits());
		usage.setDatumPropertiesIn(new BigInteger("123"));
		usage.setDatumOut(new BigInteger("234"));
		usage.setDatumDaysStored(new BigInteger("345"));

		expect(usageDao.findNodeUsageForAccount(userId, startDate, endDate))
				.andReturn(singletonList(nodeUsage));

		Capture<TaxCodeFilter> taxCodeFilterCaptor = new Capture<>();
		TaxCode datumPropsTax = new TaxCode("NZ", NodeUsage.DATUM_PROPS_IN_KEY, "GST",
				new BigDecimal("0.10"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		TaxCode datumOutTax = new TaxCode("NZ", NodeUsage.DATUM_OUT_KEY, "GST", new BigDecimal("0.20"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		TaxCode datumStoredTax = new TaxCode("NZ", NodeUsage.DATUM_DAYS_STORED_KEY, "GST",
				new BigDecimal("0.30"),
				LocalDate.of(2020, 1, 1).atStartOfDay(addr.getTimeZone()).toInstant(), null);
		BasicFilterResults<TaxCode, Long> taxCodeResults = new BasicFilterResults<>(
				asList(datumPropsTax, datumOutTax, datumStoredTax));
		expect(taxCodeDao.findFiltered(EasyMock.capture(taxCodeFilterCaptor), isNull(), isNull(),
				isNull())).andReturn(taxCodeResults);

		final BigDecimal expectedTax = usage.getDatumPropertiesInCost().multiply(datumPropsTax.getRate())
				.add(usage.getDatumOutCost().multiply(datumOutTax.getRate()))
				.add(usage.getDatumDaysStoredCost().multiply(datumStoredTax.getRate()))
				.setScale(2, RoundingMode.HALF_UP);

		final BigDecimal expectedTotal = usage.getTotalCost().add(expectedTax);

		final BigDecimal partialCredit = new BigDecimal("5.67");

		expect(accountDao.claimAccountBalanceCredit(account.getId().getId(),
				usage.getTotalCost().add(expectedTax))).andReturn(partialCredit);
		final AccountBalance remainingBalance = new AccountBalance(account.getId(), Instant.now(),
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.00"));
		expect(accountDao.getBalanceForUser(account.getUserId())).andReturn(remainingBalance);

		// WHEN
		replayAll();
		final SnfInvoicingOptions options = dryRunOptions();
		options.setUseAccountCredit(true);
		SnfInvoice invoice = invoicingSystem.generateInvoice(userId, startDate, endDate, options);

		// THEN
		assertThat("Invoice created", invoice, notNullValue());
		assertThat("Invoice has draft ID", invoice.getId(),
				equalTo(new UserLongPK(userId, DefaultSnfInvoicingSystem.DRAFT_INVOICE_ID)));
		assertThat("Invoice items created for all usage and GST tax and credit", invoice.getItems(),
				hasSize(5));

		Map<String, SnfInvoiceItem> itemMap = invoice.getItemsByKey();
		assertThat("Invoice item mapping contains all items", itemMap.keySet(),
				contains(NodeUsage.DATUM_PROPS_IN_KEY, NodeUsage.DATUM_OUT_KEY,
						NodeUsage.DATUM_DAYS_STORED_KEY, "GST", AccountBalance.ACCOUNT_CREDIT_KEY));

		SnfInvoiceItem item;
		item = itemMap.get(NodeUsage.DATUM_PROPS_IN_KEY);
		assertUsageItem(invoice, item, usage.getDatumPropertiesIn(), usage.getDatumPropertiesInCost());
		item = itemMap.get(NodeUsage.DATUM_OUT_KEY);
		assertUsageItem(invoice, item, usage.getDatumOut(), usage.getDatumOutCost());
		item = itemMap.get(NodeUsage.DATUM_DAYS_STORED_KEY);
		assertUsageItem(invoice, item, usage.getDatumDaysStored(), usage.getDatumDaysStoredCost());
		item = itemMap.get("GST");
		assertTaxItem(invoice, item, expectedTax);
		item = itemMap.get(AccountBalance.ACCOUNT_CREDIT_KEY);
		assertCreditItem(invoice, item, partialCredit.negate(), remainingBalance.getAvailableCredit());

		assertThat("Invoice total has credit applied so balance reduced by credit amount",
				invoice.getTotalAmount().compareTo(expectedTotal.subtract(partialCredit)), equalTo(0));

		assertThat("Filtered for appropriate tax codes", taxCodeFilterCaptor.getValue(), matchesFilter(
				invoice.getStartDate().atStartOfDay(addr.getTimeZone()).toInstant(), "NZ"));

		assertThat("Invoice node usage items created", invoice.getUsages(), hasSize(1));
		SnfInvoiceNodeUsage invoiceNodeUsage = invoice.getUsages().iterator().next();
		assertInvoiceNodeUsage(invoice, invoiceNodeUsage, nodeUsage);
	}
}
