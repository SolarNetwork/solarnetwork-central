/* ==================================================================
 * MyBatisSnfConsolidatedInvoiceDaoTests.java - 15/10/2026 9:12:44 am
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Fixed;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.dao.SnfConsolidatedInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfConsolidatedInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisSnfConsolidatedInvoiceDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class MyBatisSnfConsolidatedInvoiceDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisSnfInvoiceDao invoiceDao;
	private MyBatisSnfInvoiceItemDao itemDao;
	private MyBatisSnfConsolidatedInvoiceDao dao;

	private Account primary;
	private Account member;

	@BeforeEach
	public void setUp() throws Exception {
		addressDao = new MyBatisAddressDao();
		addressDao.setSqlSessionTemplate(getSqlSessionTemplate());

		accountDao = new MyBatisAccountDao();
		accountDao.setSqlSessionTemplate(getSqlSessionTemplate());

		invoiceDao = new MyBatisSnfInvoiceDao();
		invoiceDao.setSqlSessionTemplate(getSqlSessionTemplate());

		itemDao = new MyBatisSnfInvoiceItemDao();
		itemDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisSnfConsolidatedInvoiceDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		primary = createAccount(randomLong());
		member = createAccount(randomLong());
	}

	private Account createAccount(Long userId) {
		Address addr = new Address(null, Instant.now(), userId, "Tester " + userId, "test@localhost",
				"NZ", "Pacific/Auckland");
		addr = addressDao.get(addressDao.save(addr));
		Account account = new Account(unassignedEntityIdKey(userId), Instant.now(), "NZD", "en_NZ");
		account.setAddress(addr);
		return accountDao.get(accountDao.save(account));
	}

	private SnfInvoice createInvoice(Account account, LocalDate startDate, BigDecimal... amounts) {
		SnfInvoice invoice = new SnfInvoice(account.getAccountId(), account.getUserId(), Instant.now(),
				startDate, startDate.plusMonths(1), account.getCurrencyCode());
		invoice.setAddress(account.getAddress());
		UserLongCompositePK id = invoiceDao.save(invoice);
		for ( BigDecimal amount : amounts ) {
			itemDao.save(newItem(id.getEntityId(), Fixed, "test", BigDecimal.ONE, amount));
		}
		getSqlSessionTemplate().flushStatements();
		return invoiceDao.get(id);
	}

	private SnfConsolidatedInvoice createConsolidatedInvoice(LocalDate startDate,
			SnfInvoice... invoices) {
		SnfConsolidatedInvoice entity = new SnfConsolidatedInvoice(primary.getAccountId(),
				primary.getUserId(), Instant.now(), startDate, startDate.plusMonths(1),
				primary.getCurrencyCode());
		entity.setAddress(primary.getAddress());
		entity.setInvoices(List.of(invoices));
		UserLongCompositePK id = dao.save(entity);
		getSqlSessionTemplate().flushStatements();
		return entity.copyWithId(id);
	}

	@Test
	public void insert() {
		// GIVEN
		final SnfInvoice primaryInvoice = createInvoice(primary, MONTH, new BigDecimal("1.23"));
		final SnfInvoice memberInvoice = createInvoice(member, MONTH, new BigDecimal("2.34"));

		SnfConsolidatedInvoice entity = new SnfConsolidatedInvoice(primary.getAccountId(),
				primary.getUserId(), Instant.now(), MONTH, MONTH.plusMonths(1),
				primary.getCurrencyCode());
		entity.setAddress(primary.getAddress());
		entity.setInvoices(List.of(primaryInvoice, memberInvoice));

		// WHEN
		UserLongCompositePK id = dao.save(entity);
		getSqlSessionTemplate().flushStatements();

		// THEN
		// @formatter:off
		then(id)
			.as("Primary account user ID")
			.returns(primary.getUserId(), from(UserLongCompositePK::getUserId))
			.as("Invoice ID assigned")
			.returns(true, from(UserLongCompositePK::entityIdIsAssigned))
			;
		then(jdbcTemplate.queryForList(
				"SELECT inv_id FROM solarbill.bill_consol_invoice_member WHERE consol_id = ? ORDER BY inv_id",
				Long.class, id.getEntityId()))
			.as("Account invoices linked")
			.containsExactly(primaryInvoice.getInvoiceId(), memberInvoice.getInvoiceId())
			;
		then(id.getEntityId())
			.as("ID from the account invoice sequence, so after the account invoices")
			.isGreaterThan(memberInvoice.getInvoiceId())
			;
		// @formatter:on
	}

	@Test
	public void getByPK() {
		// GIVEN
		final SnfInvoice primaryInvoice = createInvoice(primary, MONTH, new BigDecimal("1.23"));
		final SnfInvoice memberInvoice = createInvoice(member, MONTH, new BigDecimal("2.34"));
		final SnfConsolidatedInvoice last = createConsolidatedInvoice(MONTH, primaryInvoice,
				memberInvoice);

		// WHEN
		SnfConsolidatedInvoice result = dao.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Consolidated invoice found")
			.isNotNull()
			.as("ID")
			.returns(last.getId(), from(SnfConsolidatedInvoice::getId))
			.as("Primary account")
			.returns(primary.getAccountId(), from(SnfConsolidatedInvoice::getAccountId))
			.as("Primary account address")
			.returns(primary.getAddress().getId(), from(e -> e.getAddress().getId()))
			.as("Account invoice IDs, with their own users, in invoice ID order")
			.returns(List.of(primaryInvoice.getId(), memberInvoice.getId()),
					from(SnfConsolidatedInvoice::getInvoiceIds))
			.as("Account invoices not loaded")
			.returns(null, from(SnfConsolidatedInvoice::getInvoices))
			;
		then(result.isSameAs(last))
			.as("Saved properties returned")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void getByPK_otherUser() {
		// GIVEN
		final SnfInvoice memberInvoice = createInvoice(member, MONTH, new BigDecimal("2.34"));
		final SnfConsolidatedInvoice last = createConsolidatedInvoice(MONTH, memberInvoice);

		// WHEN
		SnfConsolidatedInvoice result = dao
				.get(new UserLongCompositePK(member.getUserId(), last.getInvoiceId()));

		// THEN
		// @formatter:off
		then(result)
			.as("Consolidated invoice belongs to the primary account user only")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void insert_invoiceAlreadyConsolidated() {
		// GIVEN
		final SnfInvoice memberInvoice = createInvoice(member, MONTH, new BigDecimal("2.34"));
		createConsolidatedInvoice(MONTH, memberInvoice);

		// WHEN
		// @formatter:off
		thenExceptionOfType(DataIntegrityViolationException.class)
			.as("An account invoice can be in only one consolidated invoice")
			.isThrownBy(() -> createConsolidatedInvoice(MONTH, memberInvoice))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forAccount() {
		// GIVEN
		final SnfInvoice inv1 = createInvoice(primary, MONTH, new BigDecimal("1.23"));
		final SnfInvoice inv2 = createInvoice(member, MONTH, new BigDecimal("2.34"));
		final SnfConsolidatedInvoice cinv1 = createConsolidatedInvoice(MONTH, inv1, inv2);

		final LocalDate month2 = MONTH.plusMonths(1);
		final SnfInvoice inv3 = createInvoice(primary, month2, new BigDecimal("3.45"));
		final SnfInvoice inv4 = createInvoice(member, month2, new BigDecimal("4.56"));
		final SnfConsolidatedInvoice cinv2 = createConsolidatedInvoice(month2, inv3, inv4);

		// another primary account's consolidated invoice
		final Account other = createAccount(randomLong());
		final SnfInvoice inv5 = createInvoice(other, MONTH, new BigDecimal("5.67"));
		SnfConsolidatedInvoice otherInvoice = new SnfConsolidatedInvoice(other.getAccountId(),
				other.getUserId(), Instant.now(), MONTH, MONTH.plusMonths(1), other.getCurrencyCode());
		otherInvoice.setAddress(other.getAddress());
		otherInvoice.setInvoices(List.of(inv5));
		dao.save(otherInvoice);
		getSqlSessionTemplate().flushStatements();

		// WHEN
		FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> results = dao.findFiltered(
				SnfConsolidatedInvoiceFilter.forAccount(primary.getAccountId()),
				SnfConsolidatedInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, null, null);

		// THEN
		// @formatter:off
		then(results.getTotalResults())
			.as("Total results provided")
			.isEqualTo(2L)
			;
		then(results.getResults())
			.as("Consolidated invoices of the primary account returned, newest first")
			.extracting(SnfConsolidatedInvoice::getId)
			.containsExactly(cinv2.getId(), cinv1.getId())
			;
		then(results.getResults())
			.as("Account invoice IDs of each consolidated invoice returned")
			.extracting(SnfConsolidatedInvoice::getInvoiceIds)
			.containsExactly(List.of(inv3.getId(), inv4.getId()), List.of(inv1.getId(), inv2.getId()))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forAccount_paged() {
		// GIVEN
		final SnfInvoice inv1 = createInvoice(primary, MONTH, new BigDecimal("1.23"));
		final SnfInvoice inv2 = createInvoice(member, MONTH, new BigDecimal("2.34"));
		createConsolidatedInvoice(MONTH, inv1, inv2);

		final LocalDate month2 = MONTH.plusMonths(1);
		final SnfInvoice inv3 = createInvoice(primary, month2, new BigDecimal("3.45"));
		final SnfInvoice inv4 = createInvoice(member, month2, new BigDecimal("4.56"));
		final SnfConsolidatedInvoice cinv2 = createConsolidatedInvoice(month2, inv3, inv4);

		// WHEN
		FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> results = dao.findFiltered(
				SnfConsolidatedInvoiceFilter.forAccount(primary.getAccountId()),
				SnfConsolidatedInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, 0L, 1);

		// THEN
		// @formatter:off
		then(results.getTotalResults())
			.as("Total results provided")
			.isEqualTo(2L)
			;
		then(results.getReturnedResultCount())
			.as("One result returned")
			.isEqualTo(1)
			;
		then(results.getResults())
			.as("Newest consolidated invoice returned, with all its account invoices")
			.singleElement()
			.returns(cinv2.getId(), from(SnfConsolidatedInvoice::getId))
			.returns(List.of(inv3.getId(), inv4.getId()), from(SnfConsolidatedInvoice::getInvoiceIds))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forInvoice() {
		// GIVEN
		final SnfInvoice inv1 = createInvoice(primary, MONTH, new BigDecimal("1.23"));
		final SnfInvoice inv2 = createInvoice(member, MONTH, new BigDecimal("2.34"));
		final SnfConsolidatedInvoice cinv1 = createConsolidatedInvoice(MONTH, inv1, inv2);

		final LocalDate month2 = MONTH.plusMonths(1);
		final SnfInvoice inv3 = createInvoice(primary, month2, new BigDecimal("3.45"));
		createConsolidatedInvoice(month2, inv3);

		// WHEN
		FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> results = dao.findFiltered(
				SnfConsolidatedInvoiceFilter.forInvoice(inv2.getInvoiceId()), null, null, null);

		// THEN
		// @formatter:off
		then(results.getResults())
			.as("The consolidated invoice of the account invoice returned, with all its account invoices")
			.singleElement()
			.returns(cinv1.getId(), from(SnfConsolidatedInvoice::getId))
			.returns(List.of(inv1.getId(), inv2.getId()), from(SnfConsolidatedInvoice::getInvoiceIds))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forUserAndPeriod() {
		// GIVEN
		final SnfInvoice inv1 = createInvoice(primary, MONTH, new BigDecimal("1.23"));
		createConsolidatedInvoice(MONTH, inv1);

		final LocalDate month2 = MONTH.plusMonths(1);
		final SnfInvoice inv2 = createInvoice(primary, month2, new BigDecimal("2.34"));
		final SnfConsolidatedInvoice cinv2 = createConsolidatedInvoice(month2, inv2);

		// WHEN
		SnfConsolidatedInvoiceFilter filter = SnfConsolidatedInvoiceFilter.forUser(primary.getUserId());
		filter.setStartDate(month2);
		filter.setEndDate(month2.plusMonths(1));
		FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> results = dao.findFiltered(filter,
				null, null, null);

		// THEN
		// @formatter:off
		then(results.getResults())
			.as("The consolidated invoice for the period returned")
			.extracting(SnfConsolidatedInvoice::getId)
			.containsExactly(cinv2.getId())
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_unpaidOnly() {
		// GIVEN
		final SnfConsolidatedInvoice paid = createConsolidatedInvoice(MONTH,
				createInvoice(primary, MONTH, new BigDecimal("1.23")),
				createInvoice(member, MONTH, new BigDecimal("2.34")));
		final LocalDate month2 = MONTH.plusMonths(1);
		final SnfConsolidatedInvoice partlyPaid = createConsolidatedInvoice(month2,
				createInvoice(primary, month2, new BigDecimal("3.45")),
				createInvoice(member, month2, new BigDecimal("4.56")));
		final LocalDate month3 = MONTH.plusMonths(2);
		final SnfConsolidatedInvoice unpaid = createConsolidatedInvoice(month3,
				createInvoice(primary, month3, new BigDecimal("5.67")));

		jdbcTemplate.queryForList(
				"SELECT * FROM solarbill.add_consol_invoice_payments(consolid => ?, pay_amount => ?)",
				paid.getInvoiceId(), new BigDecimal("3.57"));
		jdbcTemplate.queryForList(
				"SELECT * FROM solarbill.add_consol_invoice_payments(consolid => ?, pay_amount => ?)",
				partlyPaid.getInvoiceId(), new BigDecimal("4.00"));

		final SnfConsolidatedInvoiceFilter filter = SnfConsolidatedInvoiceFilter
				.forAccount(primary.getAccountId());
		filter.setUnpaidOnly(true);

		// WHEN
		FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> results = dao.findFiltered(filter,
				null, null, null);

		// THEN
		// @formatter:off
		then(results.getResults())
			.as("Consolidated invoices with unpaid account invoices returned, newest first")
			.extracting(SnfConsolidatedInvoice::getId)
			.containsExactly(unpaid.getId(), partlyPaid.getId())
			;
		then(results.getTotalResults())
			.as("Total counts unpaid consolidated invoices only")
			.isEqualTo(2L)
			;
		// @formatter:on
	}

}
