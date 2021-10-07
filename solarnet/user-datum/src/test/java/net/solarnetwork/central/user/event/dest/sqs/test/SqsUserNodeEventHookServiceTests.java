/* ==================================================================
 * SqsUserNodeEventHookServiceTests.java - 16/06/2020 11:41:40 am
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

import static java.time.Instant.now;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import net.solarnetwork.central.user.event.dest.sqs.SqsDestinationProperties;
import net.solarnetwork.central.user.event.dest.sqs.SqsStats;
import net.solarnetwork.central.user.event.dest.sqs.SqsUserNodeEventHookService;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link SqsUserNodeEventHookService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SqsUserNodeEventHookServiceTests {

	private static Map<String, Object> TEST_PROPS;

	@BeforeClass
	public static void setupClass() {
		Properties p = new Properties();
		try {
			InputStream in = SqsUserNodeEventHookServiceTests.class.getClassLoader()
					.getResourceAsStream("sqs-dest.properties");
			if ( in != null ) {
				p.load(in);
				in.close();
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		Map<String, Object> m = new LinkedHashMap<>(p.size());
		for ( Map.Entry<Object, Object> me : p.entrySet() ) {
			m.put(me.getKey().toString(), me.getValue());
		}
		TEST_PROPS = m;

		drainQueue();
	}

	@Test
	public void settingSpecifiers() {
		// given
		SqsUserNodeEventHookService service = new SqsUserNodeEventHookService(new SqsStats("Test", 1));

		//when
		List<SettingSpecifier> specs = service.getSettingSpecifiers();

		// then
		assertThat("Setting specs provided", specs, hasSize(4));

		Set<String> keys = specs.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toSet());
		assertThat("Setting keys", keys,
				containsInAnyOrder("accessKey", "secretKey", "region", "queueName"));
	}

	private static AmazonSQS createSqsClient() {
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(TEST_PROPS);
		AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard().withRegion(props.getRegion());
		String accessKey = props.getAccessKey();
		String secretKey = props.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder = builder.withCredentials(
					new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
		}
		return builder.build();
	}

	private static void drainQueue() {
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(TEST_PROPS);
		AmazonSQS client = createSqsClient();
		String queueUrl = null;
		try {
			GetQueueUrlResult urlRes = client.getQueueUrl(props.getQueueName());
			queueUrl = urlRes.getQueueUrl();
		} catch ( QueueDoesNotExistException e ) {
			throw new IllegalArgumentException(
					String.format("Queue [%s] does not exist (using region %s).", props.getQueueName(),
							props.getRegion()));
		}

		ReceiveMessageRequest req = new ReceiveMessageRequest(queueUrl);
		req.setWaitTimeSeconds(0);
		while ( true ) {
			ReceiveMessageResult res = client.receiveMessage(req);
			if ( res == null ) {
				break;
			}
			List<Message> msgs = res.getMessages();
			if ( msgs == null || msgs.isEmpty() ) {
				break;
			}
			List<DeleteMessageBatchRequestEntry> delEntries = new ArrayList<>(8);
			for ( Message msg : msgs ) {
				delEntries.add(new DeleteMessageBatchRequestEntry(UUID.randomUUID().toString(),
						msg.getReceiptHandle()));
			}
			DeleteMessageBatchRequest delReq = new DeleteMessageBatchRequest(queueUrl, delEntries);
			client.deleteMessageBatch(delReq);
		}
	}

	private static String getQueueMessage() {
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(TEST_PROPS);
		AmazonSQS client = createSqsClient();
		String queueUrl = null;
		try {
			GetQueueUrlResult urlRes = client.getQueueUrl(props.getQueueName());
			queueUrl = urlRes.getQueueUrl();
		} catch ( QueueDoesNotExistException e ) {
			throw new IllegalArgumentException(
					String.format("Queue [%s] does not exist (using region %s).", props.getQueueName(),
							props.getRegion()));
		}

		ReceiveMessageRequest req = new ReceiveMessageRequest(queueUrl);
		req.setWaitTimeSeconds(0);
		ReceiveMessageResult res = client.receiveMessage(req);
		assertThat("Message received", res.getMessages(), hasSize(1));
		client.deleteMessage(queueUrl, res.getMessages().get(0).getReceiptHandle());

		String msgBody = res.getMessages().get(0).getBody();
		return msgBody;
	}

	@Test
	public void publishEvent() {
		// GIVEN
		SqsUserNodeEventHookService service = new SqsUserNodeEventHookService(new SqsStats("Test", 1));

		UserNodeEventHookConfiguration config = new UserNodeEventHookConfiguration(1L, 2L, now());
		config.setServiceProps(TEST_PROPS);

		UserNodeEventTask event = new UserNodeEventTask(UUID.randomUUID(), now());
		event.setHookId(config.getId().getId());
		event.setUserId(config.getUserId());
		event.setNodeId(3L);
		event.setSourceId("test.soruce");

		Map<String, Object> eventProps = new LinkedHashMap<>(2);
		eventProps.put("foo", "bar");
		eventProps.put("val", 123);
		event.setTaskProperties(eventProps);

		// WHEN
		boolean result = service.processUserNodeEventHook(config, event);

		// THEN
		assertThat("Result OK", result, equalTo(true));

		String msgBody = getQueueMessage();
		Map<String, Object> msgData = JsonUtils.getStringMap(msgBody);
		assertThat("Message object property count", msgData.keySet(), hasSize(6));
		assertThat("Hook ID prop", msgData, hasEntry("hookId", config.getId().getId().intValue()));
		assertThat("User ID prop", msgData, hasEntry("userId", config.getUserId().intValue()));
		assertThat("Node ID prop", msgData, hasEntry("nodeId", event.getNodeId().intValue()));
		assertThat("Source ID prop", msgData, hasEntry("sourceId", event.getSourceId()));
		assertThat("foo prop", msgData, hasEntry("foo", eventProps.get("foo")));
		assertThat("val prop", msgData, hasEntry("val", eventProps.get("val")));
	}

}
