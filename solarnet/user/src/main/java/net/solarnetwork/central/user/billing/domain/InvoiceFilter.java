/* ==================================================================
 * InvoiceFilter.java - 25/08/2017 2:37:09 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.domain;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.Filter;

/**
 * Filter for Invoice.
 * 
 * @author matt
 * @version 1.1
 */
public interface InvoiceFilter extends Filter {

	/**
	 * Get the ID of the user to find invoices for.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

	/**
	 * Flag to filter based on the invoice being unpaid or not.
	 * 
	 * @return {@literal true} for only unpaid invoices; {@literal false} for
	 *         only fully paid invoices; {@code null} for both
	 */
	@Nullable
	Boolean getUnpaid();

	/**
	 * Flag to include consolidated invoices.
	 *
	 * <p>
	 * When {@code true}, the results include the consolidated invoices billed to
	 * the user, in place of the user's invoices that are part of a consolidated
	 * invoice. A consolidated invoice is billed to the primary account of an
	 * invoice consolidation group, so the invoices of the other accounts of the
	 * group are left out, with nothing in their place.
	 * </p>
	 *
	 * @return {@code true} to include consolidated invoices in place of the
	 *         invoices they are made of
	 * @since 1.1
	 */
	default @Nullable Boolean getIncludeConsolidated() {
		return null;
	}

}
