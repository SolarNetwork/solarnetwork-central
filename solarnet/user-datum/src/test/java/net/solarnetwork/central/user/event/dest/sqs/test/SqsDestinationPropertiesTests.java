/* ==================================================================
 * SqsDestinationPropertiesTests.java - 16/06/2020 11:31:45 am
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

package net.solarnetwork.central.user.event.dest.sqs.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.central.user.event.dest.sqs.SqsDestinationProperties;

/**
 * Test cases for the {@link SqsDestinationProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SqsDestinationPropertiesTests {

	@Test
	public void create_fromServiceProps() {
		// GIVEN
		Map<String, Object> sProps = new HashMap<>(4);
		sProps.put("region", "foobar");
		sProps.put("queueName", "blah");
		sProps.put("accessKey", "root");
		sProps.put("secretKey", "123456");

		// WHEN
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(sProps);

		// THEN
		assertThat("Region populated", props.getRegion(), equalTo("foobar"));
		assertThat("Queue name populated", props.getQueueName(), equalTo("blah"));
		assertThat("Access key populated", props.getAccessKey(), equalTo("root"));
		assertThat("Secret key populated", props.getSecretKey(), equalTo("123456"));
	}

	@Test
	public void create_fromServiceProps_ignoreUnknownKeys() {
		// GIVEN
		Map<String, Object> sProps = new HashMap<>(4);
		sProps.put("region", "foobar");
		sProps.put("queueName", "blah");
		sProps.put("accessKey", "root");
		sProps.put("secretKey", "123456");
		sProps.put("wtf", "ha");

		// WHEN
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(sProps);

		// THEN
		assertThat("Region populated", props.getRegion(), equalTo("foobar"));
		assertThat("Queue name populated", props.getQueueName(), equalTo("blah"));
		assertThat("Access key populated", props.getAccessKey(), equalTo("root"));
		assertThat("Secret key populated", props.getSecretKey(), equalTo("123456"));
	}

	@Test
	public void create_fromServiceProps_nonStringProp() {
		// GIVEN
		Map<String, Object> sProps = new HashMap<>(4);
		sProps.put("region", "foobar");
		sProps.put("queueName", "blah");
		sProps.put("accessKey", "root");
		sProps.put("secretKey", 123456);

		// WHEN
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(sProps);

		// THEN
		assertThat("Region populated", props.getRegion(), equalTo("foobar"));
		assertThat("Queue name populated", props.getQueueName(), equalTo("blah"));
		assertThat("Access key populated", props.getAccessKey(), equalTo("root"));
		assertThat("Secret key populated", props.getSecretKey(), equalTo("123456"));
	}

}
