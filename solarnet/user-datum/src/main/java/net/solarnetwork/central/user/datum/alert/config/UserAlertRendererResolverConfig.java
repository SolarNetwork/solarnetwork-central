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

package net.solarnetwork.central.user.datum.alert.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.HTML;
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
import net.solarnetwork.central.common.config.VersionedQualifier;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.biz.UserAlertRendererResolver;
import net.solarnetwork.central.user.datum.alert.support.VersionedMessageSourceUserAlertRendererResolver;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;

/**
 * Configuration for SNF invoice renderer resolvers.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class UserAlertRendererResolverConfig implements SolarNetUserDatumAlertConfiguration {

	@Autowired
	@Qualifier(VERSIONED_MESSAGES_CACHE)
	private Cache<String, VersionedMessageDao.VersionedMessages> versionedMessagesCache;

	@Autowired
	private VersionedMessageDao messageDao;

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@VersionedQualifier(value = USER_ALERT_DATUM_STALE, version = HTML)
	@ConfigurationProperties(prefix = "app.user-alert.stale-data.html-template-cache")
	public CacheSettings userAlertHtmlTemplateCacheSettings() {
		CacheSettings settings = new CacheSettings();
		settings.setDiskMaxSizeMb(0);
		settings.setDiskPersistent(false);
		return settings;
	}

	@Bean
	@VersionedQualifier(value = USER_ALERT_DATUM_STALE, version = HTML)
	public Cache<String, ST4TemplateRenderer> userAlertHtmlTemplateCache(
			@VersionedQualifier(value = USER_ALERT_DATUM_STALE, version = HTML) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, ST4TemplateRenderer.class,
				USER_ALERT_DATUM_STALE + "-st4-template-renderers");
	}

	@Primary
	@Bean
	@VersionedQualifier(value = USER_ALERT_DATUM_STALE, version = HTML)
	public UserAlertRendererResolver htmlUserAlertRendererResolver(
			@VersionedQualifier(value = USER_ALERT_DATUM_STALE,
					version = HTML) Cache<String, ST4TemplateRenderer> userAlertHtmlTemplateCache) {
		return new VersionedMessageSourceUserAlertRendererResolver("/snf/text/html/stale-datum-alert",
				"alert", MimeTypeUtils.TEXT_HTML, messageDao, versionedMessagesCache,
				userAlertHtmlTemplateCache);
	}

}
