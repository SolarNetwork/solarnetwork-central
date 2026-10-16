/* ==================================================================
 * HtmlToPdfSnfConsolidatedInvoiceRendererResolver.java - 15/10/2026 1:01:44 pm
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

package net.solarnetwork.central.user.billing.snf.pdf;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.user.billing.snf.SnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.service.TemplateRenderer;

/**
 * {@link SnfConsolidatedInvoiceRendererResolver} that resolves
 * {@link HtmlToPdfTemplateRenderer} instances.
 *
 * @author matt
 * @version 1.0
 */
public class HtmlToPdfSnfConsolidatedInvoiceRendererResolver
		implements SnfConsolidatedInvoiceRendererResolver {

	private final SnfConsolidatedInvoiceRendererResolver htmlRendererResolver;

	/**
	 * Constructor.
	 *
	 * @param htmlRendererResolver
	 *        the renderer resolver for HTML output
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public HtmlToPdfSnfConsolidatedInvoiceRendererResolver(
			SnfConsolidatedInvoiceRendererResolver htmlRendererResolver) {
		super();
		this.htmlRendererResolver = requireNonNullArgument(htmlRendererResolver, "htmlRendererResolver");
	}

	@Override
	public @Nullable TemplateRenderer rendererForConsolidatedInvoice(SnfConsolidatedInvoice invoice,
			MimeType mimeType, Locale locale) {
		if ( !HtmlToPdfTemplateRenderer.PDF_MIME_TYPE.isCompatibleWith(mimeType) ) {
			return null;
		}
		TemplateRenderer renderer = htmlRendererResolver.rendererForConsolidatedInvoice(invoice,
				MimeTypeUtils.TEXT_HTML, locale);
		if ( renderer == null || !renderer.supportsMimeType(MimeTypeUtils.TEXT_HTML) ) {
			return null;
		}
		return new HtmlToPdfTemplateRenderer(renderer);
	}

}
