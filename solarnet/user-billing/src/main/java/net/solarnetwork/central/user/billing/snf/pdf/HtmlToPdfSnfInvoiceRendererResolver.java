/* ==================================================================
 * HtmlToPdfSnfInvoiceRendererResolver.java - 8/08/2020 6:29:15 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import java.util.Locale;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.service.TemplateRenderer;

/**
 * {@link SnfInvoiceRendererResolver} that resolves
 * {@link HtmlToPdfTemplateRenderer} instances.
 * 
 * @author matt
 * @version 2.0
 */
public class HtmlToPdfSnfInvoiceRendererResolver implements SnfInvoiceRendererResolver {

	private final SnfInvoiceRendererResolver htmlRendererResolver;

	/**
	 * Constructor.
	 * 
	 * @param htmlRendererResolver
	 *        the renderer resolver for HTML output
	 */
	public HtmlToPdfSnfInvoiceRendererResolver(SnfInvoiceRendererResolver htmlRendererResolver) {
		super();
		if ( htmlRendererResolver == null ) {
			throw new IllegalArgumentException("The htmlRendererResolver argument must be provided.");
		}
		this.htmlRendererResolver = htmlRendererResolver;
	}

	@Override
	public TemplateRenderer rendererForInvoice(SnfInvoice invoice, MimeType mimeType, Locale locale) {
		if ( !HtmlToPdfTemplateRenderer.PDF_MIME_TYPE.isCompatibleWith(mimeType) ) {
			return null;
		}
		TemplateRenderer renderer = htmlRendererResolver.rendererForInvoice(invoice,
				MimeTypeUtils.TEXT_HTML, locale);
		if ( renderer == null || !renderer.supportsMimeType(MimeTypeUtils.TEXT_HTML) ) {
			return null;
		}
		return new HtmlToPdfTemplateRenderer(renderer);
	}

}
