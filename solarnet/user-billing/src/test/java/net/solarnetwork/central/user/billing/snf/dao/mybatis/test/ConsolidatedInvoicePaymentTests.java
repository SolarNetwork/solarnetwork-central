/* ==================================================================
 * ConsolidatedInvoicePaymentTests.java - 15/10/2026 9:31:08 am
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
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Fixed;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAccountDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisAddressDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfConsolidatedInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisSnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;

/**
 * Test cases for the {@code solarbill.add_consol_invoice_payments()} database
 * function.
 *
 * @author matt
 * @version 1.0
 */
public class ConsolidatedInvoicePaymentTests extends AbstractMyBatisDaoTestSupport {

	private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

	private MyBatisAddressDao addressDao;
	private MyBatisAccountDao accountDao;
	private MyBatisSnfInvoiceDao invoiceDao;
	private MyBatisSnfInvoiceItemDao itemDao;
	private MyBatisSnfConsolidatedInvoiceDao consolidatedInvoiceDao;

	private Account primary;
	private Account member;
	private SnfInvoice primaryInvoice;
	private SnfInvoice memberInvoice;
	private SnfConsolidatedInvoice consolidated;

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

		consolidatedInvoiceDao = new MyBatisSnfConsolidatedInvoiceDao();
		consolidatedInvoiceDao.setSqlSessionTemplate(getSqlSessionTemplate());

		primary = createAccount(randomLong());
		member = createAccount(randomLong());

		// total 3.57
		primaryInvoice = createInvoice(primary, new BigDecimal("1.23"), new BigDecimal("2.34"));

		// total 4.56
		memberInvoice = createInvoice(member, new BigDecimal("4.56"));

		SnfConsolidatedInvoice entity = new SnfConsolidatedInvoice(primary.getAccountId(),
				primary.getUserId(), Instant.now(), MONTH, MONTH.plusMonths(1),
				primary.getCurrencyCode());
		entity.setAddress(primary.getAddress());
		entity.setInvoices(List.of(primaryInvoice, memberInvoice));
		UserLongCompositePK id = consolidatedInvoiceDao.save(entity);
		getSqlSessionTemplate().flushStatements();
		consolidated = entity.copyWithId(id);
	}

	private Account createAccount(Long userId) {
		Address addr = new Address(null, Instant.now(), userId, "Tester " + userId, "test@localhost",
				"NZ", "Pacific/Auckland");
		addr = addressDao.get(addressDao.save(addr));
		Account account = new Account(unassignedEntityIdKey(userId), Instant.now(), "NZD", "en_NZ");
		account.setAddress(addr);
		return accountDao.get(accountDao.save(account));
	}

	private SnfInvoice createInvoice(Account account, BigDecimal... amounts) {
		SnfInvoice invoice = new SnfInvoice(account.getAccountId(), account.getUserId(), Instant.now(),
				MONTH, MONTH.plusMonths(1), account.getCurrencyCode());
		invoice.setAddress(account.getAddress());
		UserLongCompositePK id = invoiceDao.save(invoice);
		for ( BigDecimal amount : amounts ) {
			itemDao.save(newItem(id.getEntityId(), Fixed, "test", BigDecimal.ONE, amount));
		}
		getSqlSessionTemplate().flushStatements();
		return invoiceDao.get(id);
	}

	private List<Map<String, Object>> pay(Long consolidatedId, BigDecimal amount, String extKey,
			String ref) {
		return jdbcTemplate.queryForList("""
				SELECT * FROM solarbill.add_consol_invoice_payments(
					  consolid => ?
					, pay_amount => ?
					, pay_ext_key => ?
					, pay_ref => ?
				)
				""", consolidatedId, amount, extKey, ref);
	}

	private List<Map<String, Object>> pay(BigDecimal amount, String extKey) {
		return pay(consolidated.getInvoiceId(), amount, extKey, null);
	}

	private BigDecimal paidAmount(SnfInvoice invoice) {
		return jdbcTemplate.queryForObject(
				"SELECT COALESCE(SUM(amount), 0) FROM solarbill.bill_invoice_payment WHERE inv_id = ?",
				BigDecimal.class, invoice.getInvoiceId());
	}

	private static Map<String, Object> payment(Account account, String amount) {
		return Map.of("acct_id", account.getAccountId(), "amount", new BigDecimal(amount));
	}

	private static Map<String, Object> amounts(Map<String, Object> row) {
		return Map.of("acct_id", row.get("acct_id"), "amount", row.get("amount"));
	}

	@Test
	public void pay_exact() {
		// GIVEN
		final String extKey = randomString();

		// WHEN
		List<Map<String, Object>> result = pay(new BigDecimal("8.13"), extKey);

		// THEN
		// @formatter:off
		then(result)
			.as("One payment per account invoice, in invoice order")
			.map(ConsolidatedInvoicePaymentTests::amounts)
			.containsExactly(payment(primary, "3.57"), payment(member, "4.56"))
			;
		then(result)
			.as("Payments reference the consolidated invoice number and share the external key")
			.allSatisfy(row -> then(row)
				.containsEntry("ref", SnfBillingUtils.invoiceNumForId(consolidated.getInvoiceId()))
				.containsEntry("ext_key", extKey))
			;
		then(paidAmount(primaryInvoice)).as("Primary account invoice paid").isEqualByComparingTo("3.57");
		then(paidAmount(memberInvoice)).as("Member account invoice paid").isEqualByComparingTo("4.56");
		// @formatter:on
		assertAccountBalance(primary.getAccountId(), new BigDecimal("3.57"), new BigDecimal("3.57"));
		assertAccountBalance(member.getAccountId(), new BigDecimal("4.56"), new BigDecimal("4.56"));
	}

	@Test
	public void pay_partial() {
		// WHEN
		List<Map<String, Object>> result = pay(new BigDecimal("5.00"), randomString());

		// THEN
		// @formatter:off
		then(result)
			.as("Oldest invoice paid in full, the remainder to the next")
			.map(ConsolidatedInvoicePaymentTests::amounts)
			.containsExactly(payment(primary, "3.57"), payment(member, "1.43"))
			;
		then(paidAmount(primaryInvoice)).as("Primary account invoice paid").isEqualByComparingTo("3.57");
		then(paidAmount(memberInvoice)).as("Member account invoice part paid").isEqualByComparingTo("1.43");
		// @formatter:on
		assertAccountBalance(member.getAccountId(), new BigDecimal("4.56"), new BigDecimal("1.43"));
	}

	@Test
	public void pay_partial_firstInvoiceOnly() {
		// WHEN
		List<Map<String, Object>> result = pay(new BigDecimal("2.00"), randomString());

		// THEN
		// @formatter:off
		then(result)
			.as("Only the oldest invoice paid, in part")
			.map(ConsolidatedInvoicePaymentTests::amounts)
			.containsExactly(payment(primary, "2.00"))
			;
		then(paidAmount(primaryInvoice)).as("Primary account invoice part paid").isEqualByComparingTo("2.00");
		then(paidAmount(memberInvoice)).as("Member account invoice not paid").isEqualByComparingTo("0");
		// @formatter:on
	}

	@Test
	public void pay_over() {
		// WHEN
		List<Map<String, Object>> result = pay(new BigDecimal("10.00"), randomString());

		// THEN
		// @formatter:off
		then(result)
			.as("Invoices paid in full, and the excess paid to the primary account")
			.map(ConsolidatedInvoicePaymentTests::amounts)
			.containsExactly(payment(primary, "3.57"), payment(member, "4.56"), payment(primary, "1.87"))
			;
		then(paidAmount(primaryInvoice)).as("Primary account invoice paid").isEqualByComparingTo("3.57");
		then(paidAmount(memberInvoice)).as("Member account invoice paid").isEqualByComparingTo("4.56");
		// @formatter:on
		assertAccountBalance(primary.getAccountId(), new BigDecimal("3.57"), new BigDecimal("5.44"));
		assertAccountBalance(member.getAccountId(), new BigDecimal("4.56"), new BigDecimal("4.56"));
	}

	@Test
	public void pay_alreadyPartPaid() {
		// GIVEN
		pay(new BigDecimal("2.00"), randomString());

		// WHEN
		List<Map<String, Object>> result = pay(new BigDecimal("6.13"), randomString());

		// THEN
		// @formatter:off
		then(result)
			.as("Only the unpaid amounts paid")
			.map(ConsolidatedInvoicePaymentTests::amounts)
			.containsExactly(payment(primary, "1.57"), payment(member, "4.56"))
			;
		then(paidAmount(primaryInvoice)).as("Primary account invoice paid").isEqualByComparingTo("3.57");
		then(paidAmount(memberInvoice)).as("Member account invoice paid").isEqualByComparingTo("4.56");
		// @formatter:on
	}

	@Test
	public void pay_reference() {
		// WHEN
		List<Map<String, Object>> result = pay(consolidated.getInvoiceId(), new BigDecimal("8.13"), null,
				"Bank deposit 123");

		// THEN
		// @formatter:off
		then(result)
			.as("Payments use the given reference")
			.hasSize(2)
			.allSatisfy(row -> then(row).containsEntry("ref", "Bank deposit 123"))
			;
		// @formatter:on
	}

	@Test
	public void pay_unknownConsolidatedInvoice() {
		// WHEN
		// @formatter:off
		thenExceptionOfType(DataIntegrityViolationException.class)
			.as("Unknown consolidated invoice rejected")
			.isThrownBy(() -> pay(-1L, new BigDecimal("8.13"), null, null))
			;
		// @formatter:on
	}

}
