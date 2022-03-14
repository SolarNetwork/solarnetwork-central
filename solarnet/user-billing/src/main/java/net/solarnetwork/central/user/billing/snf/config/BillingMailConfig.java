/* ==================================================================
 * BillingMailConfig.java - 1/11/2021 11:26:23 AM
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

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import net.solarnetwork.central.mail.support.DefaultMailService;
import net.solarnetwork.central.user.billing.snf.SnfInvoiceDeliverer;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.mail.MailSnfInvoiceDeliverer;

/**
 * Billing mail configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class BillingMailConfig {

	@Autowired
	private MailSender mailSender;

	@Autowired
	private SnfInvoicingSystem snfInvoicingSystem;

	@Autowired
	private Executor executor;

	@Value("${app.billing.invoice.mail.from:accounts@localhost}")
	private String invoiceFrom = "accounts@localhost";

	@Bean
	public SnfInvoiceDeliverer snfInvoiceDeliverer() {
		DefaultMailService mailService = new DefaultMailService(mailSender);
		mailService.setHtml(true);
		SimpleMailMessage template = new SimpleMailMessage();
		template.setFrom(invoiceFrom);
		mailService.setTemplateMessage(template);
		return new MailSnfInvoiceDeliverer(snfInvoicingSystem, mailService, executor);
	}

}
