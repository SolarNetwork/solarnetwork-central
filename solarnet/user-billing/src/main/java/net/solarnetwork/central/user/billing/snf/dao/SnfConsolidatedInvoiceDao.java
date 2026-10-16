/* ==================================================================
 * SnfConsolidatedInvoiceDao.java - 15/10/2026 8:55:31 am
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

package net.solarnetwork.central.user.billing.snf.dao;

import java.util.List;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoiceFilter;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link SnfConsolidatedInvoice} entities.
 *
 * <p>
 * Saving a new consolidated invoice also saves its links to the account
 * invoices given by its {@code invoiceIds} property. Consolidated invoices
 * cannot be updated.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface SnfConsolidatedInvoiceDao
		extends GenericDao<SnfConsolidatedInvoice, UserLongCompositePK>,
		FilterableDao<SnfConsolidatedInvoice, UserLongCompositePK, SnfConsolidatedInvoiceFilter> {

	/**
	 * Sort descriptors to sort by date in descending order, followed by ID in
	 * ascending.
	 */
	// @formatter:off
	List<SortDescriptor> SORT_BY_INVOICE_DATE_DESCENDING = List.of(
			new SimpleSortDescriptor(SnfInvoiceDao.InvoiceSortKey.DATE.toString(), true),
			new SimpleSortDescriptor(StandardSortKey.ID.toString(), false));
	// @formatter:on

}
