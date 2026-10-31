/* ==================================================================
 * GnucashDao.java - 11/10/2026 2:45:40 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.snf.gnucash.GnucashBookLockedException;
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
 * DAO API for a GnuCash book stored in a SQL database.
 *
 * <p>
 * GnuCash loads a whole book into memory when it opens it, and does not see
 * changes made by others until it opens the book again. Changes must only be
 * made while GnuCash does not have the book open, so call {@link #lockBook()}
 * first, in the same transaction as the changes.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface GnucashDao {

	/**
	 * Lock the book for changes, until the current transaction ends.
	 *
	 * <p>
	 * GnuCash cannot open the book while the lock is held.
	 * </p>
	 *
	 * @throws GnucashBookLockedException
	 *         if GnuCash has the book open
	 */
	void lockBook();

	/**
	 * Get the versions of the book's tables.
	 *
	 * @return the table versions, keyed by table name
	 */
	Map<String, Integer> tableVersions();

	/**
	 * Get a currency.
	 *
	 * @param code
	 *        the currency code, such as {@code NZD}
	 * @return the currency, or {@code null} if not found
	 */
	@Nullable
	GncCommodity currency(String code);

	/**
	 * Get an account by its full name.
	 *
	 * @param path
	 *        the full account name, such as {@code Assets:Accounts Receivable}
	 * @return the account, or {@code null} if not found
	 */
	@Nullable
	GncAccount account(String path);

	/**
	 * Get an account by its GUID.
	 *
	 * @param guid
	 *        the account GUID
	 * @return the account, or {@code null} if not found
	 */
	@Nullable
	GncAccount accountForGuid(String guid);

	/**
	 * Get the customers with a given customer ID.
	 *
	 * <p>
	 * GnuCash does not require customer IDs to be unique.
	 * </p>
	 *
	 * @param id
	 *        the customer ID
	 * @return the matching customers, never {@code null}
	 */
	List<GncCustomer> customers(String id);

	/**
	 * Add a customer.
	 *
	 * @param customer
	 *        the customer to add
	 */
	void saveCustomer(GncCustomer customer);

	/**
	 * Get a visible tax table by name.
	 *
	 * @param name
	 *        the tax table name
	 * @return the tax table, or {@code null} if not found
	 */
	@Nullable
	GncTaxTable taxTable(String name);

	/**
	 * Get a tax table by GUID.
	 *
	 * @param guid
	 *        the tax table GUID
	 * @return the tax table, or {@code null} if not found
	 */
	@Nullable
	GncTaxTable taxTableForGuid(String guid);

	/**
	 * Get the hidden copies GnuCash made of a tax table when posting invoices.
	 *
	 * @param parentGuid
	 *        the GUID of the copied tax table
	 * @return the copies, never {@code null}
	 */
	List<GncTaxTable> postedTaxTableCopies(String parentGuid);

	/**
	 * Add a tax table, with its entries.
	 *
	 * @param table
	 *        the tax table to add
	 */
	void saveTaxTable(GncTaxTable table);

	/**
	 * Increment the reference count of a tax table.
	 *
	 * @param guid
	 *        the tax table GUID
	 */
	void incrementTaxTableRefcount(String guid);

	/**
	 * Get the business documents with a given ID.
	 *
	 * <p>
	 * GnuCash does not require document IDs to be unique.
	 * </p>
	 *
	 * @param id
	 *        the document ID
	 * @return the matching documents, never {@code null}
	 */
	List<GncInvoiceRef> invoices(String id);

	/**
	 * Add a business document.
	 *
	 * @param invoice
	 *        the document to add
	 */
	void saveInvoice(GncInvoice invoice);

	/**
	 * Add an invoice line.
	 *
	 * @param entry
	 *        the line to add
	 */
	void saveEntry(GncEntry entry);

	/**
	 * Add a transaction.
	 *
	 * @param transaction
	 *        the transaction to add
	 */
	void saveTransaction(GncTransaction transaction);

	/**
	 * Add a split.
	 *
	 * @param split
	 *        the split to add
	 */
	void saveSplit(GncSplit split);

	/**
	 * Add a lot.
	 *
	 * @param lot
	 *        the lot to add
	 */
	void saveLot(GncLot lot);

	/**
	 * Add a slot.
	 *
	 * @param slot
	 *        the slot to add
	 */
	void saveSlot(GncSlot slot);

	/**
	 * Get the objects that have a string slot with a given value.
	 *
	 * @param name
	 *        the slot name
	 * @param value
	 *        the slot value
	 * @return the GUIDs of the matching objects, never {@code null}
	 */
	List<String> objectsWithSlot(String name, String value);

	/**
	 * Get the open lots that hold credit for a customer in an account.
	 *
	 * <p>
	 * These are lots with a negative balance that either hold a payment made by
	 * the customer, or belong to one of the customer's business documents, such
	 * as a credit note. The splits are loaded only for payment lots. The lots
	 * are ordered by the post date of their earliest transaction.
	 * </p>
	 *
	 * @param ownerGuid
	 *        the GUID of the customer
	 * @param accountGuid
	 *        the GUID of the receivable account
	 * @return the lots, never {@code null}
	 */
	List<GncCreditLot> openCreditLots(String ownerGuid, String accountGuid);

	/**
	 * Update a split's value, amount, and lot.
	 *
	 * @param guid
	 *        the split GUID
	 * @param value
	 *        the value to set
	 * @param quantity
	 *        the amount to set
	 * @param lotGuid
	 *        the GUID of the lot to move the split to
	 */
	void updateSplit(String guid, GncNumeric value, GncNumeric quantity, @Nullable String lotGuid);

	/**
	 * Delete a lot and its slots.
	 *
	 * @param guid
	 *        the lot GUID
	 */
	void deleteLot(String guid);

}
