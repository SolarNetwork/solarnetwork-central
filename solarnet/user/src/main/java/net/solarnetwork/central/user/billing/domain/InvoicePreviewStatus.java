/* ==================================================================
 * InvoicePreviewStatus.java - 22/10/2026 2:16:41 pm
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

package net.solarnetwork.central.user.billing.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.jspecify.annotations.Nullable;

/**
 * The status of a preview invoice that is generated in the background.
 *
 * @param state
 *        the preview state
 * @param invoice
 *        the preview invoice, when {@code state} is {@code Done} and there are
 *        charges, otherwise {@code null}
 * @author matt
 * @version 1.0
 */
public record InvoicePreviewStatus(InvoicePreviewState state, @Nullable Invoice invoice) {

	/**
	 * Constructor.
	 *
	 * @param state
	 *        the preview state
	 * @param invoice
	 *        the preview invoice, when {@code state} is {@code Done} and there
	 *        are charges, otherwise {@code null}
	 * @throws IllegalArgumentException
	 *         if {@code state} is {@code null}
	 */
	public InvoicePreviewStatus {
		requireNonNullArgument(state, "state");
	}

}
