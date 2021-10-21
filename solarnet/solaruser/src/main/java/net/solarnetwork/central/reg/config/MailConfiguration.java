/* ==================================================================
 * MailConfiguration.java - 21/10/2021 11:02:32 AM
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.DefaultMailService;

/**
 * Mail configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class MailConfiguration {

	@Autowired
	private MailSender mailSender;

	@ConfigurationProperties(prefix = "app.user.reg.mail.template")
	@Bean
	public SimpleMailMessage mailTemplate() {
		return new SimpleMailMessage();
	}

	@Bean
	public MailService mailService() {
		DefaultMailService service = new DefaultMailService(mailSender);
		service.setTemplateMessage(mailTemplate());
		return service;
	}
}
