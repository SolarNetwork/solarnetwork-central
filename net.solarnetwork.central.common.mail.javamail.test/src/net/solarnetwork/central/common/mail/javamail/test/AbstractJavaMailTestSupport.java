/* ==================================================================
 * AbstractJavaMailTestSupport.java - 27/07/2020 9:57:46 AM
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

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.junit.Before;
import net.solarnetwork.central.common.mail.javamail.ManagedJavaMailSender;
import net.solarnetwork.util.ClassUtils;

/**
 * Support for JavaMailSender based tests.
 * 
 * @author matt
 * @version 1.0
 */
public class AbstractJavaMailTestSupport {

	protected ManagedJavaMailSender sender;
	protected Properties testProps;

	@Before
	public void setup() throws Exception {
		Properties props = new Properties();
		try (Reader in = new InputStreamReader(
				new BufferedInputStream(
						getClass().getClassLoader().getResourceAsStream("test-mail.properties")),
				"UTF-8")) {
			props.load(in);
		}
		sender = new ManagedJavaMailSender();
		Map<String, Object> p = new LinkedHashMap<>();
		for ( Entry<Object, Object> me : props.entrySet() ) {
			p.put(me.getKey().toString(), me.getValue());
		}
		ClassUtils.setBeanProperties(sender, p);

		testProps = new Properties();
		try (Reader in = new InputStreamReader(new BufferedInputStream(
				getClass().getClassLoader().getResourceAsStream("test.properties")), "UTF-8")) {
			testProps.load(in);
		}
	}

}
