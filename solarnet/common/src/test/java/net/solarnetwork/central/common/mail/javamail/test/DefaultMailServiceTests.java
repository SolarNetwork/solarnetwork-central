/* ==================================================================
 * DefaultMailServiceTests.java - 27/07/2020 9:55:31 AM
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

package net.solarnetwork.central.common.mail.javamail.test;

import static java.util.Collections.singleton;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.mail.support.DefaultMailService;
import net.solarnetwork.central.mail.support.SimpleMessageDataSource;
import net.solarnetwork.central.test.SystemPropertyMatchTestRule;

/**
 * Test cases for the {@link DefaultMailService} class using JavaMailSender.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultMailServiceTests extends AbstractJavaMailTestSupport {

	@ClassRule
	public static SystemPropertyMatchTestRule SMTP_RULE = new SystemPropertyMatchTestRule("smtp");

	private DefaultMailService mailService;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		mailService = new DefaultMailService(sender);

		SimpleMailMessage template = new SimpleMailMessage();
		template.setSubject("Test message");
		template.setFrom(testProps.getProperty("from"));
		mailService.setTemplateMessage(template);
	}

	@Test
	public void sendSimpleMail() {
		BasicMailAddress address = new BasicMailAddress(null, testProps.getProperty("to"));
		mailService.sendMail(address, new SimpleMessageDataSource(null, "Test message body."));
	}

	@Test
	public void sendHtmlMail() {
		mailService.setHtml(true);
		BasicMailAddress address = new BasicMailAddress(null, testProps.getProperty("to"));
		mailService.sendMail(address, new SimpleMessageDataSource("Test HTML",
				"<html><body>Test <b>HTML</b> message body.</body></html>"));
	}

	@Test
	public void sendAttachment() {
		BasicMailAddress address = new BasicMailAddress(null, testProps.getProperty("to"));
		Resource att = new ClassPathResource("test-data.csv", getClass());
		mailService.sendMail(address, new SimpleMessageDataSource("Test with attachment",
				"Test message body.\n", singleton(att)));
	}

	@Test
	public void sendHtmlWithAttachment() {
		mailService.setHtml(true);
		BasicMailAddress address = new BasicMailAddress(null, testProps.getProperty("to"));
		Resource att = new ClassPathResource("test-data.csv", getClass());
		mailService.sendMail(address, new SimpleMessageDataSource("Test HTML with attachment",
				"<html><body>Test <b>HTML</b> message body.</body></html>", singleton(att)));
	}

}
