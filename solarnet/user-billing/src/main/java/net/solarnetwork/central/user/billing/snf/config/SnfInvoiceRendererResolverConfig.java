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

import static net.solarnetwork.central.common.dao.config.VersionedMessageDaoConfig.VERSIONED_MESSAGES_CACHE;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.pdf.HtmlToPdfSnfInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.st4.VersionedMessageSourceSnfInvoiceRendererResolver;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;

/**
 * Configuration for SNF invoice renderer resolvers.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SnfInvoiceRendererResolverConfig {

	@Autowired
	@Qualifier(VERSIONED_MESSAGES_CACHE)
	private Cache<String, VersionedMessageDao.VersionedMessages> versionedMessagesCache;

	@Autowired
	private VersionedMessageDao messageDao;

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@ConfigurationProperties(prefix = "app.billing.invoice.html-template-cache")
	public CacheSettings snfInvoiceHtmlTemplateCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	public Cache<String, ST4TemplateRenderer> snfInvoiceHtmlTemplateCache() {
		CacheSettings settings = snfInvoiceHtmlTemplateCacheSettings();
		return settings.createCache(cacheManager, String.class, ST4TemplateRenderer.class,
				"st4-template-renderers");
	}

	@Qualifier("html")
	@Primary
	@Bean
	public SnfInvoiceRendererResolver htmlSnfInvoiceRendererResolver() {
		return new VersionedMessageSourceSnfInvoiceRendererResolver("/snf/text/html/invoice", "invoice",
				MimeTypeUtils.TEXT_HTML, messageDao, versionedMessagesCache,
				snfInvoiceHtmlTemplateCache());
	}

	@Qualifier("pdf")
	@Bean
	public SnfInvoiceRendererResolver pdfSnfInvoiceRendererResolver() {
		return new HtmlToPdfSnfInvoiceRendererResolver(htmlSnfInvoiceRendererResolver());
	}

}
