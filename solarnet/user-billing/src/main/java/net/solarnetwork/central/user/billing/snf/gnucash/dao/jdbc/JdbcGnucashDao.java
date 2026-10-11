/* ==================================================================
 * JdbcGnucashDao.java - 11/10/2026 2:49:19 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc;

import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getTimestamp;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashBookLockedException;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.GnucashDao;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.DeleteGncLot;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncCustomer;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncEntry;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncInvoice;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncLot;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncSlot;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncSplit;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncTaxTable;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncTaxTableEntry;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.InsertGncTransaction;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.LockGnucashBook;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncAccount;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncCommodity;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncCustomer;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncInvoiceRef;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncLotSplits;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncOpenCreditLots;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGncTaxTables;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGnucashLock;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.SelectGnucashTableVersions;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.UpdateGncSplit;
import net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.UpdateGncTaxTableRefcount;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAccount;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCommodity;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCreditLot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCustomer;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncEntry;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncInvoice;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncInvoiceRef;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncLot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncNumeric;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSlot;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSplit;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTable;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTransaction;

/**
 * JDBC implementation of {@link GnucashDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcGnucashDao implements GnucashDao {

	/** The {@code schema} default value. */
	public static final String DEFAULT_SCHEMA = "public";

	/** The {@code lockTimeoutSecs} default value. */
	public static final int DEFAULT_LOCK_TIMEOUT_SECS = 30;

	private static final String CREDIT_NOTE_TITLE = "Credit Note";
	private static final String INVOICE_TITLE = "Invoice";

	private final JdbcOperations jdbcOps;
	private final String schema;
	private int lockTimeoutSecs = DEFAULT_LOCK_TIMEOUT_SECS;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations for the GnuCash database
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code schema} is not a plain
	 *         SQL identifier
	 */
	public JdbcGnucashDao(JdbcOperations jdbcOps, String schema) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.schema = GnucashSqlUtils.schemaName(schema);
	}

	@Override
	public void lockBook() {
		jdbcOps.execute(new LockGnucashBook(schema, lockTimeoutSecs),
				(PreparedStatementCallback<Boolean>) stmt -> stmt.execute());
		jdbcOps.query(new SelectGnucashLock(schema), (RowCallbackHandler) rs -> {
			throw new GnucashBookLockedException(rs.getString(1), rs.getInt(2));
		});
	}

	@Override
	public Map<String, Integer> tableVersions() {
		final Map<String, Integer> result = new LinkedHashMap<>(32);
		jdbcOps.query(new SelectGnucashTableVersions(schema), rs -> {
			result.put(rs.getString(1), rs.getInt(2));
		});
		return result;
	}

	@Override
	public @Nullable GncCommodity currency(String code) {
		return first(jdbcOps.query(
				new SelectGncCommodity(schema, GncCommodity.CURRENCY_NAMESPACE, code),
				SelectGncCommodity.ROW_MAPPER));
	}

	@Override
	public @Nullable GncAccount account(String path) {
		return first(jdbcOps.query(new SelectGncAccount(schema, false, path),
				SelectGncAccount.ROW_MAPPER));
	}

	@Override
	public @Nullable GncAccount accountForGuid(String guid) {
		return first(jdbcOps.query(new SelectGncAccount(schema, true, guid),
				SelectGncAccount.ROW_MAPPER));
	}

	@Override
	public List<GncCustomer> customers(String id) {
		return jdbcOps.query(new SelectGncCustomer(schema, id), SelectGncCustomer.ROW_MAPPER);
	}

	@Override
	public void saveCustomer(GncCustomer customer) {
		jdbcOps.update(new InsertGncCustomer(schema, customer));
	}

	@Override
	public @Nullable GncTaxTable taxTable(String name) {
		return first(taxTables(SelectGncTaxTables.Criteria.Name, name));
	}

	@Override
	public @Nullable GncTaxTable taxTableForGuid(String guid) {
		return first(taxTables(SelectGncTaxTables.Criteria.Guid, guid));
	}

	@Override
	public List<GncTaxTable> postedTaxTableCopies(String parentGuid) {
		return taxTables(SelectGncTaxTables.Criteria.Parent, parentGuid);
	}

	private List<GncTaxTable> taxTables(SelectGncTaxTables.Criteria criteria, String value) {
		final List<GncTaxTable> result = jdbcOps
				.query(new SelectGncTaxTables(schema, criteria, value), SelectGncTaxTables.EXTRACTOR);
		return (result != null ? result : List.of());
	}

	@Override
	public void saveTaxTable(GncTaxTable table) {
		jdbcOps.update(new InsertGncTaxTable(schema, table));
		for ( var entry : table.entries() ) {
			jdbcOps.update(new InsertGncTaxTableEntry(schema, table.guid(), entry));
		}
	}

	@Override
	public void incrementTaxTableRefcount(String guid) {
		jdbcOps.update(new UpdateGncTaxTableRefcount(schema, guid, 1));
	}

	@Override
	public List<GncInvoiceRef> invoices(String id) {
		return jdbcOps.query(new SelectGncInvoiceRef(schema, id), SelectGncInvoiceRef.ROW_MAPPER);
	}

	@Override
	public void saveInvoice(GncInvoice invoice) {
		jdbcOps.update(new InsertGncInvoice(schema, invoice));
	}

	@Override
	public void saveEntry(GncEntry entry) {
		jdbcOps.update(new InsertGncEntry(schema, entry));
	}

	@Override
	public void saveTransaction(GncTransaction transaction) {
		jdbcOps.update(new InsertGncTransaction(schema, transaction));
	}

	@Override
	public void saveSplit(GncSplit split) {
		jdbcOps.update(new InsertGncSplit(schema, split));
	}

	@Override
	public void saveLot(GncLot lot) {
		jdbcOps.update(new InsertGncLot(schema, lot));
	}

	@Override
	public void saveSlot(GncSlot slot) {
		jdbcOps.update(new InsertGncSlot(schema, slot));
	}

	@Override
	public List<GncCreditLot> openCreditLots(String ownerGuid, String accountGuid) {
		final List<GncCreditLot> lots = new ArrayList<>(4);
		jdbcOps.query(new SelectGncOpenCreditLots(schema, ownerGuid, accountGuid), rs -> {
			final String docGuid = rs.getString(2);
			final String docTitle = (docGuid != null
					? (rs.getLong(4) != 0 ? CREDIT_NOTE_TITLE : INVOICE_TITLE) + " "
							+ rs.getString(3)
					: null);
			final BigDecimal balance = rs.getBigDecimal(5);
			final Instant firstPost = nonnull(getTimestamp(rs, 6), "First post date");
			final Instant lastPost = nonnull(getTimestamp(rs, 7), "Last post date");
			lots.add(new GncCreditLot(rs.getString(1), balance, firstPost, lastPost, docGuid,
					docTitle, List.of()));
		});
		// load the splits of the payment lots, which may be moved to apply the payments
		for ( ListIterator<GncCreditLot> itr = lots.listIterator(); itr.hasNext(); ) {
			final GncCreditLot lot = itr.next();
			if ( !lot.isDocument() ) {
				itr.set(new GncCreditLot(lot.guid(), lot.balance(), lot.firstPostDate(),
						lot.lastPostDate(), null, null, jdbcOps.query(
								new SelectGncLotSplits(schema, lot.guid()), SelectGncLotSplits.ROW_MAPPER)));
			}
		}
		return lots;
	}

	@Override
	public void updateSplit(String guid, GncNumeric value, GncNumeric quantity,
			@Nullable String lotGuid) {
		jdbcOps.update(new UpdateGncSplit(schema, guid, value, quantity, lotGuid));
	}

	@Override
	public void deleteLot(String guid) {
		jdbcOps.update(new DeleteGncLot(schema, guid));
	}

	private static <T> @Nullable T first(@Nullable List<T> list) {
		return (list != null && !list.isEmpty() ? list.getFirst() : null);
	}

	/**
	 * Get the maximum number of seconds to wait to lock the book.
	 *
	 * @return the timeout, in seconds; defaults to
	 *         {@link #DEFAULT_LOCK_TIMEOUT_SECS}
	 */
	public final int getLockTimeoutSecs() {
		return lockTimeoutSecs;
	}

	/**
	 * Set the maximum number of seconds to wait to lock the book.
	 *
	 * <p>
	 * GnuCash holds its own lock on the book only briefly while claiming it,
	 * so a long wait is not expected.
	 * </p>
	 *
	 * @param lockTimeoutSecs
	 *        the timeout to set, in seconds, or {@literal 0} for no limit
	 */
	public final void setLockTimeoutSecs(int lockTimeoutSecs) {
		this.lockTimeoutSecs = lockTimeoutSecs;
	}

}
