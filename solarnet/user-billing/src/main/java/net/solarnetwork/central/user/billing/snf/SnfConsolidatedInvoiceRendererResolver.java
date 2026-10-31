/* ==================================================================
 * SnfConsolidatedInvoiceRendererResolver.java - 15/10/2026 12:52:10 pm
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

import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeType;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.service.TemplateRenderer;

/**
 * API for resolving a {@link TemplateRenderer} for rendering a consolidated
 * invoice.
 *
 * @author matt
 * @version 1.0
 */
public interface SnfConsolidatedInvoiceRendererResolver {

	/**
	 * Resolve a renderer for a given consolidated invoice and output
	 * characteristics.
	 *
	 * @param invoice
	 *        the consolidated invoice to be rendered
	 * @param mimeType
	 *        the desired output MIME type
	 * @param locale
	 *        the output locale
	 * @return the renderer, or {@code null} if none can be resolved
	 */
	@Nullable
	TemplateRenderer rendererForConsolidatedInvoice(SnfConsolidatedInvoice invoice, MimeType mimeType,
			Locale locale);

}
