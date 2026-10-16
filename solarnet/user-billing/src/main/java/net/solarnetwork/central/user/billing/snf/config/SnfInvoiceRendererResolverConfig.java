/* ==================================================================
 * SnfInvoiceRendererResolverConfig.java - 3/11/2021 9:40:16 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.HTML;
import static net.solarnetwork.central.common.dao.config.VersionedMessageDaoConfig.VERSIONED_MESSAGES_CACHE;
import java.util.List;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.common.config.SolarNetCommonConfiguration;
import net.solarnetwork.central.common.config.VersionedQualifier;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.billing.snf.SnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.pdf.HtmlToPdfSnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.pdf.HtmlToPdfSnfInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.st4.VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.st4.VersionedMessageSourceSnfInvoiceRendererResolver;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;

/**
 * Configuration for SNF invoice renderer resolvers.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class SnfInvoiceRendererResolverConfig implements SolarNetUserBillingConfiguration {

	/**
	 * The message bundle name of the invoice HTML templates.
	 *
	 * @since 1.1
	 */
	public static final String INVOICE_HTML_BUNDLE_NAME = "/snf/text/html/invoice";

	/**
	 * The message bundle name of the consolidated invoice HTML templates.
	 *
	 * @since 1.1
	 */
	public static final String CONSOLIDATED_INVOICE_HTML_BUNDLE_NAME = "/snf/text/html/consolidated-invoice";

	@Autowired
	@Qualifier(VERSIONED_MESSAGES_CACHE)
	private Cache<String, VersionedMessageDao.VersionedMessages> versionedMessagesCache;

	@Autowired
	private VersionedMessageDao messageDao;

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@VersionedQualifier(value = USER_BILLING_INVOICE, version = HTML)
	@ConfigurationProperties(prefix = "app.billing.invoice.html-template-cache")
	public CacheSettings snfInvoiceHtmlTemplateCacheSettings() {
		CacheSettings settings = new CacheSettings();
		settings.setDiskMaxSizeMb(0);
		settings.setDiskPersistent(false);
		return settings;
	}

	@Bean
	@VersionedQualifier(value = USER_BILLING_INVOICE, version = HTML)
	public Cache<String, ST4TemplateRenderer> snfInvoiceHtmlTemplateCache(
			@VersionedQualifier(value = USER_BILLING_INVOICE, version = HTML) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, ST4TemplateRenderer.class,
				USER_BILLING_INVOICE + "-st4-html-template-renderers");
	}

	@Primary
	@Bean
	@Qualifier(HTML)
	public SnfInvoiceRendererResolver htmlSnfInvoiceRendererResolver(
			@VersionedQualifier(value = USER_BILLING_INVOICE,
					version = HTML) Cache<String, ST4TemplateRenderer> snfInvoiceHtmlTemplateCache) {
		return new VersionedMessageSourceSnfInvoiceRendererResolver(INVOICE_HTML_BUNDLE_NAME, "invoice",
				MimeTypeUtils.TEXT_HTML, messageDao, versionedMessagesCache,
				snfInvoiceHtmlTemplateCache);
	}

	@Qualifier(SolarNetCommonConfiguration.PDF)
	@Bean
	public SnfInvoiceRendererResolver pdfSnfInvoiceRendererResolver(
			@Qualifier(HTML) SnfInvoiceRendererResolver htmlSnfInvoiceRendererResolver) {
		return new HtmlToPdfSnfInvoiceRendererResolver(htmlSnfInvoiceRendererResolver);
	}

	/**
	 * The consolidated invoice HTML template cache settings.
	 *
	 * @return the settings
	 * @since 1.1
	 */
	@Bean
	@VersionedQualifier(value = USER_BILLING_CONSOLIDATED_INVOICE, version = HTML)
	@ConfigurationProperties(prefix = "app.billing.invoice.consolidated-html-template-cache")
	public CacheSettings snfConsolidatedInvoiceHtmlTemplateCacheSettings() {
		CacheSettings settings = new CacheSettings();
		settings.setDiskMaxSizeMb(0);
		settings.setDiskPersistent(false);
		return settings;
	}

	/**
	 * The consolidated invoice HTML template cache.
	 *
	 * <p>
	 * This must not be shared with the invoice template cache, because both
	 * are keyed by message versions only.
	 * </p>
	 *
	 * @param settings
	 *        the cache settings
	 * @return the cache
	 * @since 1.1
	 */
	@Bean
	@VersionedQualifier(value = USER_BILLING_CONSOLIDATED_INVOICE, version = HTML)
	public Cache<String, ST4TemplateRenderer> snfConsolidatedInvoiceHtmlTemplateCache(
			@VersionedQualifier(value = USER_BILLING_CONSOLIDATED_INVOICE,
					version = HTML) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, ST4TemplateRenderer.class,
				USER_BILLING_CONSOLIDATED_INVOICE + "-st4-html-template-renderers");
	}

	/**
	 * The consolidated invoice HTML renderer resolver.
	 *
	 * <p>
	 * The consolidated invoice templates import the invoice templates, to
	 * render the account invoices.
	 * </p>
	 *
	 * @param snfConsolidatedInvoiceHtmlTemplateCache
	 *        the template cache
	 * @return the resolver
	 * @since 1.1
	 */
	@Bean
	@Qualifier(HTML)
	public SnfConsolidatedInvoiceRendererResolver htmlSnfConsolidatedInvoiceRendererResolver(
			@VersionedQualifier(value = USER_BILLING_CONSOLIDATED_INVOICE,
					version = HTML) Cache<String, ST4TemplateRenderer> snfConsolidatedInvoiceHtmlTemplateCache) {
		return new VersionedMessageSourceSnfConsolidatedInvoiceRendererResolver(
				CONSOLIDATED_INVOICE_HTML_BUNDLE_NAME, List.of(INVOICE_HTML_BUNDLE_NAME),
				"consolidatedInvoice", MimeTypeUtils.TEXT_HTML, messageDao, versionedMessagesCache,
				snfConsolidatedInvoiceHtmlTemplateCache);
	}

	/**
	 * The consolidated invoice PDF renderer resolver.
	 *
	 * @param htmlSnfConsolidatedInvoiceRendererResolver
	 *        the HTML renderer resolver
	 * @return the resolver
	 * @since 1.1
	 */
	@Qualifier(SolarNetCommonConfiguration.PDF)
	@Bean
	public SnfConsolidatedInvoiceRendererResolver pdfSnfConsolidatedInvoiceRendererResolver(
			@Qualifier(HTML) SnfConsolidatedInvoiceRendererResolver htmlSnfConsolidatedInvoiceRendererResolver) {
		return new HtmlToPdfSnfConsolidatedInvoiceRendererResolver(
				htmlSnfConsolidatedInvoiceRendererResolver);
	}

}
