/* ==================================================================
 * BillingSystemConfig.java - 1/11/2021 10:51:41 AM
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
import java.util.List;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.snf.DefaultSnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceDeliverer;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceRendererResolver;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.NodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceItemDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.dao.TaxCodeDao;

/**
 * SNF BillingSystem configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SnfBillingSystemConfig {

	@Autowired
	@Qualifier(VERSIONED_MESSAGES_CACHE)
	private Cache<String, VersionedMessageDao.VersionedMessages> versionedMessagesCache;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private SnfInvoiceDao invoiceDao;

	@Autowired
	private SnfInvoiceItemDao invoiceItemDao;

	@Autowired
	private SnfInvoiceNodeUsageDao invoiceNodeUsageDao;

	@Autowired
	private NodeUsageDao usageDao;

	@Autowired
	private TaxCodeDao taxCodeDao;

	@Autowired
	@Lazy
	private List<SnfInvoiceDeliverer> snfInvoiceDeliverers;

	@Autowired
	private List<SnfInvoiceRendererResolver> snfInvoiceRendererResolvers;

	@Autowired
	private VersionedMessageDao messageDao;

	@Value("${app.billing.delivery-timeout-secs:60}")
	private int deliveryTimeoutSecs = 60;

	@Bean
	public BillingSystem snfBillingSystem() {
		SnfBillingSystem system = new SnfBillingSystem(snfInvoicingSystem(), accountDao, invoiceDao);
		return system;
	}

	@Bean
	public SnfInvoicingSystem snfInvoicingSystem() {
		DefaultSnfInvoicingSystem system = new DefaultSnfInvoicingSystem(accountDao, invoiceDao,
				invoiceItemDao, invoiceNodeUsageDao, usageDao, taxCodeDao, messageDao);
		system.setMessageCache(versionedMessagesCache);
		system.setDeliveryTimeoutSecs(deliveryTimeoutSecs);
		system.setDeliveryServices(snfInvoiceDeliverers);
		system.setRendererResolvers(snfInvoiceRendererResolvers);
		return system;
	}

}
