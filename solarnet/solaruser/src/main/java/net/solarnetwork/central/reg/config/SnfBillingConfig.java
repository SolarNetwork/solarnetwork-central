/* ==================================================================
 * SnfBillingConfig.java - 1/11/2021 10:35:27 AM
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration.SNF_INVOICE_PREVIEW;
import static net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration.SNF_INVOICE_PREVIEW_GROUP;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicePreviewService;
import net.solarnetwork.central.user.billing.snf.SnfInvoicePreviewService;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreview;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreviewGroup;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreviewKey;

/**
 * SNF billing configuration.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile(SolarNetUserBillingConfiguration.SNF_BILLING)
@ComponentScan(basePackageClasses = SolarNetUserBillingConfiguration.class)
public class SnfBillingConfig {

	@Autowired
	private CacheManager cacheManager;

	@Value("${app.billing.preview.max-concurrent:2}")
	private int previewMaxConcurrent = DefaultSnfInvoicePreviewService.DEFAULT_MAX_CONCURRENT;

	@Value("${app.billing.preview.failure-retry-delay-secs:60}")
	private long previewFailureRetryDelaySecs = DefaultSnfInvoicePreviewService.DEFAULT_FAILURE_RETRY_DELAY
			.toSeconds();

	/**
	 * The preview invoice cache settings.
	 *
	 * @return the settings
	 * @since 1.1
	 */
	@Bean
	@Qualifier(SNF_INVOICE_PREVIEW)
	@ConfigurationProperties(prefix = "app.billing.preview.cache")
	public CacheSettings snfInvoicePreviewCacheSettings() {
		// heap only, as the cached invoices are not serializable; long enough
		// to keep the first accounts of a large group until the last is done
		var settings = new CacheSettings();
		settings.setTtl(3600L);
		settings.setHeapMaxEntries(5000L);
		settings.setDiskMaxSizeMb(0L);
		return settings;
	}

	/**
	 * The preview invoice cache.
	 *
	 * @param settings
	 *        the settings
	 * @return the cache
	 * @since 1.1
	 */
	@Bean
	@Qualifier(SNF_INVOICE_PREVIEW)
	public Cache<SnfInvoicePreviewKey, SnfInvoicePreview> snfInvoicePreviewCache(
			@Qualifier(SNF_INVOICE_PREVIEW) CacheSettings settings) {
		return settings.createCache(cacheManager, SnfInvoicePreviewKey.class,
				SnfInvoicePreview.class, SNF_INVOICE_PREVIEW);
	}

	/**
	 * The consolidated invoice preview group cache settings.
	 *
	 * @return the settings
	 * @since 1.1
	 */
	@Bean
	@Qualifier(SNF_INVOICE_PREVIEW_GROUP)
	@ConfigurationProperties(prefix = "app.billing.preview.group-cache")
	public CacheSettings snfInvoicePreviewGroupCacheSettings() {
		// heap only, as the cached groups are not serializable
		var settings = new CacheSettings();
		settings.setTtl(60L);
		settings.setHeapMaxEntries(1000L);
		settings.setDiskMaxSizeMb(0L);
		return settings;
	}

	/**
	 * The consolidated invoice preview group cache.
	 *
	 * @param settings
	 *        the settings
	 * @return the cache
	 * @since 1.1
	 */
	@Bean
	@Qualifier(SNF_INVOICE_PREVIEW_GROUP)
	public Cache<Long, SnfInvoicePreviewGroup> snfInvoicePreviewGroupCache(
			@Qualifier(SNF_INVOICE_PREVIEW_GROUP) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, SnfInvoicePreviewGroup.class,
				SNF_INVOICE_PREVIEW_GROUP);
	}

	/**
	 * The service to generate preview invoices in the background.
	 *
	 * @param invoicingSystem
	 *        the invoicing system
	 * @param taskExecutorService
	 *        the app-wide executor to generate previews on
	 * @param cache
	 *        the preview invoice cache
	 * @param transactionManager
	 *        the transaction manager
	 * @return the service
	 * @since 1.1
	 */
	@Bean
	public SnfInvoicePreviewService snfInvoicePreviewService(SnfInvoicingSystem invoicingSystem,
			@Qualifier("taskExecutorService") ExecutorService taskExecutorService,
			@Qualifier(SNF_INVOICE_PREVIEW) Cache<SnfInvoicePreviewKey, SnfInvoicePreview> cache,
			PlatformTransactionManager transactionManager) {
		var service = new DefaultSnfInvoicePreviewService(Clock.systemUTC(), invoicingSystem,
				taskExecutorService, cache, previewMaxConcurrent);
		var tx = new TransactionTemplate(transactionManager);
		tx.setReadOnly(true);
		service.setTransactionTemplate(tx);
		service.setFailureRetryDelay(Duration.ofSeconds(previewFailureRetryDelaySecs));
		return service;
	}

}
