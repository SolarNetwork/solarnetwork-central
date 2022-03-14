/* ==================================================================
 * MyBatisSnfInvoiceDaoTests.java - 21/07/2020 3:28:34 PM
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis.test;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.reverseOrder;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Credit;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Fixed;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.PaymentType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisSnfInvoiceDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisSnfInvoiceDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_PROD_KEY = UUID.randomUUID().toString();

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisSnfInvoiceItemDao itemDao;
	private MyBatisSnfInvoiceDao dao;

	private SnfInvoice last;

	@Before
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		itemDao = new MyBatisSnfInvoiceItemDao();
		itemDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisSnfInvoiceDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
	}

	@Test
	public void insert() {
		Address address = addressDao.get(addressDao.save(createTestAddress()));
		Account account = accountDao.get(accountDao.save(createTestAccount(address)));
		SnfInvoice entity = new SnfInvoice(account.getId().getId(), account.getUserId(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		entity.setAddress(address);
		entity.setCurrencyCode("NZD");
		entity.setStartDate(LocalDate.of(2019, 12, 1));
		entity.setEndDate(LocalDate.of(2020, 1, 1));
		UserLongPK pk = dao.save(entity);
		assertThat("PK created", pk.getId(), notNullValue());
		getSqlSessionTemplate().flushStatements();
		last = entity;
	}

	@Test
	public void getByPK() {
		insert();
		SnfInvoice entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("InvoiceImpl sameness", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void getByPK_withItems() {
		// GIVEN
		insert();
		final SnfInvoice invoice = dao.get(last.getId());

		SnfInvoiceItem item1 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
				new BigDecimal("1.23"));
		item1.setMetadata(Collections.singletonMap("just", "testing"));
		SnfInvoiceItem item2 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
				new BigDecimal("2.34"));
		SnfInvoiceItem item3 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
				new BigDecimal("3.45"));
		final List<SnfInvoiceItem> items = Arrays.asList(item1, item2, item3);
		for ( SnfInvoiceItem item : items ) {
			itemDao.save(item);
		}

		// WHEN
		final SnfInvoice entity = dao.get(invoice.getId());

		// THEN
		assertThat("Items returned", entity.getItems(), hasSize(3));
		assertThat("Items count matches returned size", entity.getItemCount(), equalTo(3));
		Map<UUID, SnfInvoiceItem> itemMap = entity.itemMap();
		for ( SnfInvoiceItem item : items ) {
			SnfInvoiceItem other = itemMap.remove(item.getId());
			assertThat("Returned item same as saved", other.isSameAs(item), equalTo(true));
		}
		assertThat("Expected items returned", itemMap.keySet(), hasSize(0));
		assertAccountBalance(invoice.getAccountId(),
				item1.getAmount().add(item2.getAmount()).add(item3.getAmount()), BigDecimal.ZERO);
	}

	private List<SnfInvoice> createMonthlyInvoices(Account account, Address address, String currencyCode,
			LocalDate start, int count) {
		List<SnfInvoice> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			SnfInvoice invoice = new SnfInvoice(account.getId().getId(), account.getUserId(),
					Instant.ofEpochMilli(System.currentTimeMillis()));
			invoice.setAddress(address);
			invoice.setCurrencyCode(currencyCode);
			invoice.setStartDate(start.plusMonths(i));
			invoice.setEndDate(start.plusMonths(i + 1));
			UserLongPK invoiceId = dao.save(invoice);

			SnfInvoiceItem item1 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
					new BigDecimal("1.23"));
			item1.setMetadata(Collections.singletonMap("just", "testing"));
			SnfInvoiceItem item2 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
					new BigDecimal("2.34"));
			SnfInvoiceItem item3 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
					new BigDecimal("3.45"));
			for ( SnfInvoiceItem item : asList(item1, item2, item3) ) {
				itemDao.save(item);
			}

			result.add(dao.get(invoiceId));
		}
		return result;
	}

	private SnfInvoice createInvoiceWithCreditUse(Account account, Address address, String currencyCode,
			LocalDate date, BigDecimal fixed, BigDecimal credit) {
		SnfInvoice invoice = new SnfInvoice(account.getId().getId(), account.getUserId(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		invoice.setAddress(address);
		invoice.setCurrencyCode(currencyCode);
		invoice.setStartDate(date);
		invoice.setEndDate(date.plusMonths(1));
		UserLongPK invoiceId = dao.save(invoice);

		SnfInvoiceItem item1 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE, fixed);
		SnfInvoiceItem item2 = newItem(invoice, Credit, AccountBalance.ACCOUNT_CREDIT_KEY,
				BigDecimal.ONE, credit);
		for ( SnfInvoiceItem item : asList(item1, item2) ) {
			itemDao.save(item);
		}

		return dao.get(invoiceId);
	}

	private SnfInvoice createInvoiceWithCreditGrant(Account account, Address address,
			String currencyCode, LocalDate date, BigDecimal credit) {
		SnfInvoice invoice = new SnfInvoice(account.getId().getId(), account.getUserId(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		invoice.setAddress(address);
		invoice.setCurrencyCode(currencyCode);
		invoice.setStartDate(date);
		invoice.setEndDate(date.plusMonths(1));
		UserLongPK invoiceId = dao.save(invoice);

		SnfInvoiceItem item1 = newItem(invoice, Credit, "account-credit-add", BigDecimal.ONE, credit);
		itemDao.save(item1);

		return dao.get(invoiceId);
	}

	@Test
	public void filterForUser_sortDefault() {
		// GIVEN
		insert();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forUser(last.getUserId());
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter, null, null, null);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result count", result.getReturnedResultCount(), equalTo(4));
		assertThat("Total results provided", result.getTotalResults(), equalTo(4L));

		List<SnfInvoice> expectedInvoices = Stream
				.concat(Collections.singleton(last).stream(), others.stream())
				.sorted(Collections.reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(Collectors.toList());

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned results", invoices, hasSize(4));
		for ( int i = 0; i < 4; i++ ) {
			SnfInvoice invoice = invoices.get(i);
			SnfInvoice expected = expectedInvoices.get(i);
			assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
			assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
					equalTo(true));
		}
	}

	@Test
	public void filterForUser_sortDefault_paged() {
		// GIVEN
		insert();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		final List<SnfInvoice> expectedInvoices = Stream
				.concat(Collections.singleton(last).stream(), others.stream())
				.sorted(Collections.reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(Collectors.toList());

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forUser(last.getUserId());

		for ( int offset = 0; offset < 6; offset += 2 ) {
			final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter, null, offset,
					2);

			// THEN
			final int expectedCount = (offset < 4 ? 2 : 0);
			assertThat("Result returned", result, notNullValue());
			assertThat("Returned result page count", result.getReturnedResultCount(),
					equalTo(expectedCount));
			assertThat("Total results provided", result.getTotalResults(), equalTo(4L));

			List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
			assertThat("Returned page results", invoices, hasSize(expectedCount));
			for ( int i = 0; i < expectedCount; i++ ) {
				SnfInvoice invoice = invoices.get(i);
				SnfInvoice expected = expectedInvoices.get(offset + i);
				assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
				assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
						equalTo(true));
			}
		}
	}

	@Test
	public void filterForAccount_sortDefault() {
		// GIVEN
		insert();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter, null, null, null);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result count", result.getReturnedResultCount(), equalTo(4));
		assertThat("Total results provided", result.getTotalResults(), equalTo(4L));

		List<SnfInvoice> expectedInvoices = Stream
				.concat(Collections.singleton(last).stream(), others.stream())
				.sorted(Collections.reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(Collectors.toList());

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned results", invoices, hasSize(4));
		for ( int i = 0; i < 4; i++ ) {
			SnfInvoice invoice = invoices.get(i);
			SnfInvoice expected = expectedInvoices.get(i);
			assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
			assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
					equalTo(true));
		}
	}

	@Test
	public void filterForAccount_sortDefault_paged() {
		// GIVEN
		insert();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		final List<SnfInvoice> expectedInvoices = Stream
				.concat(Collections.singleton(last).stream(), others.stream())
				.sorted(Collections.reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(Collectors.toList());

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());

		for ( int offset = 0; offset < 6; offset += 2 ) {
			final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter, null, offset,
					2);

			// THEN
			final int expectedCount = (offset < 4 ? 2 : 0);
			assertThat("Result returned", result, notNullValue());
			assertThat("Returned result page count", result.getReturnedResultCount(),
					equalTo(expectedCount));
			assertThat("Total results provided", result.getTotalResults(), equalTo(4L));

			List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
			assertThat("Returned page results", invoices, hasSize(expectedCount));
			for ( int i = 0; i < expectedCount; i++ ) {
				SnfInvoice invoice = invoices.get(i);
				SnfInvoice expected = expectedInvoices.get(offset + i);
				assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
				assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
						equalTo(true));
			}
		}
	}

	@Test
	public void findLatestAccount() {
		// GIVEN
		insert();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		final List<SnfInvoice> expectedInvoices = Stream
				.concat(singleton(last).stream(), others.stream())
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0, 1);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(), equalTo(1));
		assertThat("Total results provided", result.getTotalResults(), equalTo(4L));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned page results", invoices, hasSize(1));
		SnfInvoice invoice = invoices.get(0);
		SnfInvoice expected = expectedInvoices.get(0);
		assertThat("InvoiceImpl returned in order", invoice, equalTo(expected));
		assertThat("InvoiceImpl data preserved", invoice.isSameAs(expected), equalTo(true));
	}

	@Test
	public void findLatestAccount_latestInZero() {
		// GIVEN
		insert();
		Account account = accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId()));
		List<SnfInvoice> others = createMonthlyInvoices(account, last.getAddress(), "NZD",
				last.getStartDate().plusMonths(1), 3);
		BigDecimal lastFixed = new BigDecimal("4.56");
		SnfInvoice zero = createInvoiceWithCreditUse(account, last.getAddress(), "NZD",
				last.getStartDate().plusMonths(4), lastFixed, lastFixed.negate());

		final List<SnfInvoice> expectedInvoices = Stream
				.concat(Stream.concat(singleton(last).stream(), others.stream()),
						singleton(zero).stream())
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());
		log.debug("All invoices: [\n\t{}\n]",
				expectedInvoices.stream().map(SnfInvoice::toString).collect(joining("\n\t")));

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0, 1);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(), equalTo(1));
		assertThat("Total results provided", result.getTotalResults(), equalTo(5L));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned page results", invoices, hasSize(1));
		SnfInvoice invoice = invoices.get(0);
		assertThat("InvoiceImpl returned in order", invoice, equalTo(zero));
		assertThat("InvoiceImpl data preserved", invoice.isSameAs(zero), equalTo(true));
	}

	@Test
	public void findLatestAccount_ignoreLatestCreditOnly() {
		// GIVEN
		insert();
		Account account = accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId()));
		List<SnfInvoice> others = createMonthlyInvoices(account, last.getAddress(), "NZD",
				last.getStartDate().plusMonths(1), 3);
		BigDecimal creditAdd = new BigDecimal("-4.56");
		SnfInvoice credit = createInvoiceWithCreditGrant(account, last.getAddress(), "NZD",
				last.getStartDate().plusMonths(4), creditAdd);
		log.debug("Credit invoice added: {}", credit);

		final List<SnfInvoice> expectedInvoices = Stream
				.concat(singleton(last).stream(), others.stream())
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());
		log.debug("All invoices: [\n\t{}\n]",
				expectedInvoices.stream().map(SnfInvoice::toString).collect(joining("\n\t")));

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		filter.setIgnoreCreditOnly(true);
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0, 1);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(), equalTo(1));
		assertThat("Total results provided", result.getTotalResults(), equalTo(3L));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned page results", invoices, hasSize(1));
		SnfInvoice invoice = invoices.get(0);
		assertThat("InvoiceImpl returned in order ignoring credit add", invoice,
				equalTo(expectedInvoices.get(0)));
		assertThat("InvoiceImpl data preserved", invoice.isSameAs(expectedInvoices.get(0)),
				equalTo(true));
	}

	private UUID insertPayment(Long accountId, PaymentType type, BigDecimal amount,
			String currencyCode) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into solarbill.bill_payment (id,acct_id,pay_type,amount,currency) VALUES (?::uuid,?,?,?,?)",
				id.toString(), accountId, type.getCode(), amount, currencyCode);
		return id;
	}

	private void insertInvoicePayment(Long accountId, UUID paymentId, Long invoiceId,
			BigDecimal amount) {
		jdbcTemplate.update(
				"insert into solarbill.bill_invoice_payment (acct_id,pay_id,inv_id,amount) VALUES (?,?::uuid,?,?)",
				accountId, paymentId.toString(), invoiceId, amount);
	}

	private SnfInvoice insertWithItems() {
		insert();
		final SnfInvoice invoice = dao.get(last.getId());

		SnfInvoiceItem item1 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
				new BigDecimal("1.23"));
		item1.setMetadata(Collections.singletonMap("just", "testing"));
		SnfInvoiceItem item2 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
				new BigDecimal("2.34"));
		SnfInvoiceItem item3 = newItem(invoice, Fixed, TEST_PROD_KEY, BigDecimal.ONE,
				new BigDecimal("3.45"));
		final List<SnfInvoiceItem> items = Arrays.asList(item1, item2, item3);
		for ( SnfInvoiceItem item : items ) {
			itemDao.save(item);
		}
		last = dao.get(invoice.getId());
		return last;
	}

	@Test
	public void findUnpaid_none() {
		// GIVEN
		final SnfInvoice invoice = insertWithItems();

		final BigDecimal paymentAmount = invoice.getItems().stream().map(SnfInvoiceItem::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// make payment for invoice amount
		final UUID paymentId = insertPayment(last.getAccountId(), PaymentType.Payment, paymentAmount,
				invoice.getCurrencyCode());

		// associate payment with invoice
		insertInvoicePayment(last.getAccountId(), paymentId, invoice.getId().getId(), paymentAmount);

		debugRows("solarbill.bill_invoice_item", "inv_id,id");
		debugRows("solarbill.bill_invoice_payment", "inv_id");

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		filter.setUnpaidOnly(Boolean.TRUE);
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0, 1);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(), equalTo(0));
		assertThat("Total results provided", result.getTotalResults(), equalTo(0L));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned results", invoices, hasSize(0));
	}

	@Test
	public void findUnpaid_onlyLatest() {
		// GIVEN
		insert();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		final List<SnfInvoice> expectedInvoices = Stream
				.concat(singleton(dao.get(last.getId())).stream(), others.stream())
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());

		final List<UUID> paymentIds = new ArrayList<>(expectedInvoices.size());
		// make full payments on all invoices except latest
		for ( int i = 1; i < expectedInvoices.size(); i++ ) {
			SnfInvoice invoice = expectedInvoices.get(i);
			final BigDecimal paymentAmount = invoice.getItems().stream().map(SnfInvoiceItem::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			// make payment for invoice amount
			final UUID paymentId = insertPayment(last.getAccountId(), PaymentType.Payment, paymentAmount,
					invoice.getCurrencyCode());

			// associate payment with invoice
			insertInvoicePayment(last.getAccountId(), paymentId, invoice.getId().getId(), paymentAmount);
			paymentIds.add(paymentId);
		}

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0, 1);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(), equalTo(1));
		assertThat("Total results provided", result.getTotalResults(), equalTo(4L));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		assertThat("Returned page results", invoices, hasSize(1));
		SnfInvoice invoice = invoices.get(0);
		SnfInvoice expected = expectedInvoices.get(0);
		assertThat("InvoiceImpl returned in order", invoice, equalTo(expected));
		assertThat("InvoiceImpl data preserved", invoice.isSameAs(expected), equalTo(true));
	}

	@Test
	public void findUnpaid_mixedSome() {
		// GIVEN
		final SnfInvoice oldest = insertWithItems();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		// make full payments on all just oldest and next-to-oldest
		final int expectedCount = 2;
		final List<SnfInvoice> expectedInvoices = Stream
				.concat(singleton(oldest).stream(), singleton(others.get(1)).stream())
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());

		final List<UUID> paymentIds = new ArrayList<>(expectedInvoices.size());
		final List<SnfInvoice> paidInvoices = others.stream().filter(e -> !expectedInvoices.contains(e))
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());
		for ( SnfInvoice invoice : paidInvoices ) {
			final BigDecimal paymentAmount = invoice.getItems().stream().map(SnfInvoiceItem::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			// make payment for invoice amount
			final UUID paymentId = insertPayment(last.getAccountId(), PaymentType.Payment, paymentAmount,
					invoice.getCurrencyCode());

			// associate payment with invoice
			insertInvoicePayment(last.getAccountId(), paymentId, invoice.getId().getId(), paymentAmount);
			paymentIds.add(paymentId);
		}

		debugRows("solarbill.bill_invoice_item", "inv_id,id");
		debugRows("solarbill.bill_invoice_payment", "inv_id");

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		filter.setUnpaidOnly(true);
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, null, null);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(),
				equalTo(expectedCount));
		assertThat("Total results provided", result.getTotalResults(), equalTo((long) expectedCount));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		for ( int i = 0; i < expectedCount; i++ ) {
			SnfInvoice invoice = invoices.get(i);
			SnfInvoice expected = expectedInvoices.get(i);
			assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
			assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
					equalTo(true));
		}
	}

	@Test
	public void findUnpaid_partialPayment() {
		// GIVEN
		final SnfInvoice oldest = insertWithItems();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		// make full payment on oldest, partial payment on 2nd to oldest
		final int expectedCount = 3;
		final List<SnfInvoice> paidInvoices = singletonList(oldest);

		final List<UUID> paymentIds = new ArrayList<>(paidInvoices.size());
		final List<SnfInvoice> expectedInvoices = others.stream().filter(e -> !paidInvoices.contains(e))
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());

		for ( SnfInvoice invoice : Arrays.asList(oldest, others.get(0)) ) {
			final BigDecimal paymentAmount = invoice.getItems().stream().map(SnfInvoiceItem::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.subtract(paymentIds.isEmpty() ? BigDecimal.ZERO : BigDecimal.ONE);

			// make payment for invoice amount
			final UUID paymentId = insertPayment(last.getAccountId(), PaymentType.Payment, paymentAmount,
					invoice.getCurrencyCode());

			// associate payment with invoice
			insertInvoicePayment(last.getAccountId(), paymentId, invoice.getId().getId(), paymentAmount);
			paymentIds.add(paymentId);
		}

		debugRows("solarbill.bill_invoice_item", "inv_id,id");
		debugRows("solarbill.bill_invoice_payment", "inv_id");

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		filter.setUnpaidOnly(true);
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, null, null);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(),
				equalTo(expectedCount));
		assertThat("Total results provided", result.getTotalResults(), equalTo((long) expectedCount));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		for ( int i = 0; i < expectedCount; i++ ) {
			SnfInvoice invoice = invoices.get(i);
			SnfInvoice expected = expectedInvoices.get(i);
			assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
			assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
					equalTo(true));
		}
	}

	@Test
	public void findUnpaid_singlePayment_partialCover() {
		// GIVEN
		final SnfInvoice oldest = insertWithItems();
		List<SnfInvoice> others = createMonthlyInvoices(
				accountDao.get(new UserLongPK(last.getUserId(), last.getAccountId())), last.getAddress(),
				"NZD", last.getStartDate().plusMonths(1), 3);

		// make full payments on oldest and partial payment on 2nd to oldest, so 3 unpaid
		final int expectedCount = 3;
		final List<SnfInvoice> paidInvoices = singletonList(oldest);

		final List<SnfInvoice> expectedInvoices = others.stream().filter(e -> !paidInvoices.contains(e))
				.sorted(reverseOrder(SnfInvoice.SORT_BY_DATE)).collect(toList());

		// make payment for invoice amount
		final BigDecimal paymentAmount = new BigDecimal("14.02"); // 2 cents sort of 2 invoices
		final UUID paymentId = insertPayment(last.getAccountId(), PaymentType.Payment, paymentAmount,
				"NZD");

		// associate full payment with first invoice
		insertInvoicePayment(oldest.getAccountId(), paymentId, oldest.getId().getId(),
				new BigDecimal("7.02"));

		// associate partial payment with 2nd invoice
		insertInvoicePayment(oldest.getAccountId(), paymentId, others.get(0).getId().getId(),
				new BigDecimal("7.00"));

		debugRows("solarbill.bill_invoice_item", "inv_id,id");
		debugRows("solarbill.bill_invoice_payment", "inv_id");

		// WHEN
		SnfInvoiceFilter filter = SnfInvoiceFilter.forAccount(last.getAccountId());
		filter.setUnpaidOnly(true);
		final FilterResults<SnfInvoice, UserLongPK> result = dao.findFiltered(filter,
				SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, null, null);

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Returned result page count", result.getReturnedResultCount(),
				equalTo(expectedCount));
		assertThat("Total results provided", result.getTotalResults(), equalTo((long) expectedCount));

		List<SnfInvoice> invoices = stream(result.spliterator(), false).collect(toList());
		for ( int i = 0; i < expectedCount; i++ ) {
			SnfInvoice invoice = invoices.get(i);
			SnfInvoice expected = expectedInvoices.get(i);
			assertThat(format("Invoice %d returned in order", i), invoice, equalTo(expected));
			assertThat(format("Invoice %d data preserved", i), invoice.isSameAs(expected),
					equalTo(true));
		}
	}
}
