/* ==================================================================
 * VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver.java - 15/10/2026 12:55:31 pm
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

package net.solarnetwork.central.user.billing.snf.st4;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeType;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.user.billing.snf.SnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.support.VersionedMessageSourceSupport;
import net.solarnetwork.common.tmpl.st4.MessageSourceGroup;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;
import net.solarnetwork.service.TemplateRenderer;

/**
 * {@link SnfConsolidatedInvoiceRendererResolver} that resolves
 * {@link ST4TemplateRenderer} instances using a {@link MessageSourceGroup} for
 * templates.
 *
 * <p>
 * The templates of other message bundles can be imported, such as the account
 * invoice templates, so the consolidated invoice templates can render the
 * account invoices with them.
 * </p>
 *
 * <p>
 * The start and end delimiters for ST are both configured as the {@literal $}
 * character.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver
		extends VersionedMessageSourceSupport implements SnfConsolidatedInvoiceRendererResolver {

	/**
	 * Constructor.
	 *
	 * @param bundleName
	 *        the message bundle name to use
	 * @param importedBundleNames
	 *        the names of the message bundles whose templates are imported
	 * @param rootTemplateName
	 *        the root template name
	 * @param mimeType
	 *        the supported MIME type
	 * @param messageDao
	 *        the message DAO
	 * @param messageCache
	 *        the message cache
	 * @param templateCache
	 *        the template cache
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver(String bundleName,
			List<String> importedBundleNames, String rootTemplateName, MimeType mimeType,
			VersionedMessageDao messageDao, Cache<String, VersionedMessages> messageCache,
			Cache<String, ST4TemplateRenderer> templateCache) {
		super(bundleName, importedBundleNames, rootTemplateName, mimeType, messageDao, messageCache,
				templateCache);
	}

	@Override
	public @Nullable TemplateRenderer rendererForConsolidatedInvoice(SnfConsolidatedInvoice invoice,
			MimeType mimeType, Locale locale) {
		final ZoneId zone = invoice.getTimeZone();
		final Instant version = invoice.getStartDate().atStartOfDay(zone != null ? zone : ZoneOffset.UTC)
				.toInstant();
		return rendererForVersion(version, mimeType, locale);
	}

}
