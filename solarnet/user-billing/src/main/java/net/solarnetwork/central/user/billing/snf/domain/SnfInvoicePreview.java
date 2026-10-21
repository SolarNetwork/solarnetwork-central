/* ==================================================================
 * SnfInvoicePreview.java - 22/10/2026 2:34:18 pm
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

package net.solarnetwork.central.user.billing.snf.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewState;

/**
 * The state of an account preview invoice that is generated in the background.
 *
 * @param state
 *        the preview state
 * @param date
 *        when the preview reached {@code state}, or started {@code Running}
 * @param invoice
 *        the dry-run invoice, when {@code state} is {@code Done} and there
 *        are charges, otherwise {@code null}
 * @author matt
 * @version 1.0
 */
public record SnfInvoicePreview(InvoicePreviewState state, Instant date,
		@Nullable SnfInvoice invoice) {

	/**
	 * Constructor.
	 *
	 * @param state
	 *        the preview state
	 * @param date
	 *        when the preview reached {@code state}, or started
	 *        {@code Running}
	 * @param invoice
	 *        the dry-run invoice, when {@code state} is {@code Done} and there
	 *        are charges, otherwise {@code null}
	 * @throws IllegalArgumentException
	 *         if {@code state} or {@code date} is {@code null}
	 */
	public SnfInvoicePreview {
		requireNonNullArgument(state, "state");
		requireNonNullArgument(date, "date");
	}

	/**
	 * Create a {@code Waiting} preview.
	 *
	 * @param date
	 *        the date
	 * @return the preview
	 */
	public static SnfInvoicePreview waiting(Instant date) {
		return new SnfInvoicePreview(InvoicePreviewState.Waiting, date, null);
	}

	/**
	 * Create a {@code Running} preview.
	 *
	 * @param date
	 *        when the preview started
	 * @return the preview
	 */
	public static SnfInvoicePreview running(Instant date) {
		return new SnfInvoicePreview(InvoicePreviewState.Running, date, null);
	}

	/**
	 * Create a {@code Done} preview.
	 *
	 * @param date
	 *        when the preview finished
	 * @param invoice
	 *        the dry-run invoice, or {@code null} if there are no charges
	 * @return the preview
	 */
	public static SnfInvoicePreview done(Instant date, @Nullable SnfInvoice invoice) {
		return new SnfInvoicePreview(InvoicePreviewState.Done, date, invoice);
	}

	/**
	 * Create a {@code Failed} preview.
	 *
	 * @param date
	 *        when the preview failed
	 * @return the preview
	 */
	public static SnfInvoicePreview failed(Instant date) {
		return new SnfInvoicePreview(InvoicePreviewState.Failed, date, null);
	}

	/**
	 * Test if the preview is {@code Done}.
	 *
	 * @return {@code true} if {@code state} is {@code Done}
	 */
	public boolean isDone() {
		return state == InvoicePreviewState.Done;
	}

}
