/* ==================================================================
 * ManagedJavaMailSenderTests.java - 6/05/2019 10:34:19 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.mail.SimpleMailMessage;
import net.solarnetwork.central.common.mail.javamail.ManagedJavaMailSender;
import net.solarnetwork.central.test.SystemPropertyMatchTestRule;

/**
 * Test cases for the {@link ManagedJavaMailSender} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ManagedJavaMailSenderTests extends AbstractJavaMailTestSupport {

	@ClassRule
	public static SystemPropertyMatchTestRule SMTP_RULE = new SystemPropertyMatchTestRule("smtp");

	@Test
	public void sendSimpleMail() {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(testProps.getProperty("to"));
		msg.setFrom(testProps.getProperty("from"));
		msg.setSubject("Simple mail send test");
		msg.setText("This is a test message.");
		sender.send(msg);
	}

}
