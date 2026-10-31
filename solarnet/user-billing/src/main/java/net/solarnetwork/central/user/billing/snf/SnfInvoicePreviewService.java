/* ==================================================================
 * SnfInvoicePreviewService.java - 22/10/2026 2:38:05 pm
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

package net.solarnetwork.central.user.billing.snf;

import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreview;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreviewKey;

/**
 * API for generating account preview invoices in the background.
 *
 * <p>
 * The usage queries of a preview invoice can take longer than an HTTP request
 * is allowed to. The methods of this service never wait for them: they start
 * generating a preview when needed, and return its current state. A caller asks
 * again later to get the finished preview.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface SnfInvoicePreviewService {

	/**
	 * Get the preview invoice of an account, starting to generate it if needed.
	 *
	 * <p>
	 * This method does not wait for the preview to be generated. If the preview
	 * cannot be started now, a {@code Waiting} preview is returned, and the
	 * preview is started by a later call.
	 * </p>
	 *
	 * @param key
	 *        the preview key
	 * @return the preview, never {@code null}
	 */
	SnfInvoicePreview accountPreview(SnfInvoicePreviewKey key);

	/**
	 * Get the preview invoices of the accounts of a group, starting to generate
	 * the next one if needed.
	 *
	 * <p>
	 * The accounts are generated one at a time, in group order: the next
	 * account is started only when no account of the group is being generated.
	 * An implementation may start the next account as soon as one finishes,
	 * while the group is still being asked for. This method does not wait for
	 * any preview to be generated.
	 * </p>
	 *
	 * @param group
	 *        the group
	 * @param key
	 *        the preview key of the primary account of the group, whose month
	 *        and credit option apply to all accounts of the group
	 * @return the preview of each account of the group, in group order
	 */
	List<SnfInvoicePreview> groupPreview(InvoiceConsolidationGroup group, SnfInvoicePreviewKey key);

	/**
	 * Get the finished preview invoice of an account, without starting to
	 * generate it.
	 *
	 * @param key
	 *        the preview key
	 * @return the preview, if it is {@code Done}, otherwise {@code null}
	 */
	@Nullable
	SnfInvoicePreview completedAccountPreview(SnfInvoicePreviewKey key);

}
