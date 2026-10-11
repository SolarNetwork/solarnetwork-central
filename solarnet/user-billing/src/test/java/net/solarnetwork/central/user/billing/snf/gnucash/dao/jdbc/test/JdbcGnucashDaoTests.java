/* ==================================================================
 * JdbcGnucashDaoTests.java - 11/10/2026 2:58:49 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenNoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashAccountingSystem;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashBookLockedException;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.JdbcGnucashDao;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAddress;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCreditLot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCustomer;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncNumeric;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTable;
import net.solarnetwork.central.user.billing.snf.gnucash.test.AbstractGnucashTestSupport;

/**
 * Test cases for the {@link JdbcGnucashDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcGnucashDaoTests extends AbstractGnucashTestSupport {

	private JdbcGnucashDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcGnucashDao(jdbcTemplate, SCHEMA);
	}

	@Test
	public void lockBook_notOpen() {
		thenNoException().isThrownBy(dao::lockBook);
	}

	@Test
	public void lockBook_open() {
		// GIVEN
		insertLock();

		// WHEN
		// @formatter:off
		thenExceptionOfType(GnucashBookLockedException.class)
			.isThrownBy(dao::lockBook)
			.returns("test-host", from(GnucashBookLockedException::getHostname))
			.returns(1234, from(GnucashBookLockedException::getPid))
			;
		// @formatter:on
	}

	@Test
	public void tableVersions() {
		// WHEN
		var result = dao.tableVersions();

		// THEN
		// @formatter:off
		then(result)
			.as("Versions of the supported tables are the supported ones")
			.containsAllEntriesOf(GnucashAccountingSystem.SUPPORTED_TABLE_VERSIONS)
			;
		// @formatter:on
	}

	@Test
	public void currency() {
		// WHEN
		var result = dao.currency("NZD");

		// THEN
		// @formatter:off
		then(result)
			.as("Currency found")
			.isEqualTo(nzd)
			;
		then(dao.currency("EUR"))
			.as("Unknown currency not found")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void account_path() {
		// WHEN
		var result = dao.account("Assets:Accounts Receivable");

		// THEN
		// @formatter:off
		then(result)
			.as("Account found by full name")
			.isEqualTo(receivable)
			;
		then(dao.account("Accounts Receivable"))
			.as("Account not found by its own name only")
			.isNull()
			;
		then(dao.account("Liabilities:Tax:GST on Sales"))
			.as("Deeper account found by full name")
			.isEqualTo(gst)
			;
		// @formatter:on
	}

	@Test
	public void account_guid() {
		// WHEN
		var result = dao.accountForGuid(incomeUsd.guid());

		// THEN
		// @formatter:off
		then(result)
			.as("Account found by GUID, with full name")
			.isEqualTo(incomeUsd)
			;
		// @formatter:on
	}

	@Test
	public void customer_saveAndFind() {
		// GIVEN
		var customer = new GncCustomer(guid(), "SN_123", "Example Ltd", "", true, nzd.guid(), true,
				GncCustomer.TAX_INCLUDED_USE_GLOBAL, gstTable.guid(),
				new GncAddress("Example Ltd", "1 Main St", "Auckland 1010", "New Zealand", "", "",
						"", "accounts@example.com"),
				GncAddress.EMPTY);

		// WHEN
		dao.saveCustomer(customer);
		List<GncCustomer> result = dao.customers("SN_123");

		// THEN
		// @formatter:off
		then(result)
			.as("Saved customer found by ID")
			.containsExactly(customer)
			;
		then(dao.customers("SN_124"))
			.as("Other customer ID not found")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void taxTable_byName() {
		// WHEN
		var result = dao.taxTable("GST (Sales)");

		// THEN
		// @formatter:off
		then(result)
			.as("Tax table found by name, with entries")
			.isEqualTo(gstTable)
			;
		// @formatter:on
	}

	@Test
	public void taxTable_postedCopy() {
		// GIVEN
		GncTaxTable copy = gstTable.postedCopy(guid());

		// WHEN
		dao.saveTaxTable(copy);
		List<GncTaxTable> copies = dao.postedTaxTableCopies(gstTable.guid());

		// THEN
		// @formatter:off
		then(copies)
			.as("Saved copy found by parent")
			.containsExactly(copy)
			;
		then(copies.getFirst().hasSameEntries(gstTable))
			.as("Copy has the parent's entries")
			.isTrue()
			;
		then(dao.taxTable("GST (Sales)"))
			.as("Hidden copy not found by name")
			.isEqualTo(gstTable)
			;
		// @formatter:on
	}

	@Test
	public void taxTable_incrementRefcount() {
		// WHEN
		dao.incrementTaxTableRefcount(gstTable.guid());

		// THEN
		// @formatter:off
		then(dao.taxTableForGuid(gstTable.guid()))
			.as("Reference count incremented")
			.returns(1L, from(GncTaxTable::refcount))
			;
		// @formatter:on
	}

	@Test
	public void openCreditLots_prepayment() {
		// GIVEN
		String customerGuid = insertCustomer("SN_1", nzd, gstTable);
		Instant date = Instant.parse("2026-08-21T10:59:00Z");
		String lotGuid = insertPrepayment(customerGuid, new BigDecimal("129.45"), date);

		// WHEN
		List<GncCreditLot> result = dao.openCreditLots(customerGuid, receivable.guid());

		// THEN
		// @formatter:off
		then(result)
			.as("Prepayment lot found")
			.hasSize(1)
			.element(0)
			.returns(lotGuid, from(GncCreditLot::guid))
			.returns(false, from(GncCreditLot::isDocument))
			.returns(date, from(GncCreditLot::firstPostDate))
			.satisfies(lot -> {
				then(lot.credit()).as("Credit available").isEqualByComparingTo("129.45");
				then(lot.splits()).as("Receivable split loaded").hasSize(1)
					.element(0).satisfies(s -> {
						then(s.split().quantity()).as("Split amount").isEqualTo(new GncNumeric(-12945, 100));
						then(s.txnType()).as("Payment transaction").isEqualTo("P");
					});
			})
			;
		then(dao.openCreditLots(customerGuid, receivableUsd.guid()))
			.as("No credit in other account")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void updateSplitAndDeleteLot() {
		// GIVEN
		String customerGuid = insertCustomer("SN_1", nzd, gstTable);
		Instant date = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String lotGuid = insertPrepayment(customerGuid, new BigDecimal("10.00"), date);
		GncCreditLot lot = dao.openCreditLots(customerGuid, receivable.guid()).getFirst();
		String otherLotGuid = insertPrepayment(customerGuid, new BigDecimal("1.00"), date);

		// WHEN
		var split = lot.splits().getFirst().split();
		dao.updateSplit(split.guid(), new GncNumeric(-500, 100), new GncNumeric(-500, 100),
				otherLotGuid);
		dao.deleteLot(lotGuid);

		// THEN
		// @formatter:off
		then(rows("SELECT value_num, quantity_num, lot_guid FROM splits WHERE guid = ?", split.guid()))
			.as("Split updated")
			.singleElement()
			.satisfies(r -> {
				then(r).containsEntry("value_num", -500L).containsEntry("quantity_num", -500L)
					.containsEntry("lot_guid", otherLotGuid);
			})
			;
		then(rows("SELECT * FROM lots WHERE guid = ?", lotGuid))
			.as("Lot deleted")
			.isEmpty()
			;
		then(rows("SELECT * FROM slots WHERE obj_guid = ?", lotGuid))
			.as("Lot slots deleted")
			.isEmpty()
			;
		then(rows("""
				SELECT s.* FROM slots s
				WHERE s.name LIKE 'gncOwner/%'
					AND NOT EXISTS (SELECT 1 FROM slots f WHERE f.guid_val = s.obj_guid)
				"""))
			.as("Lot frame slots deleted")
			.isEmpty()
			;
		// @formatter:on
	}

}
