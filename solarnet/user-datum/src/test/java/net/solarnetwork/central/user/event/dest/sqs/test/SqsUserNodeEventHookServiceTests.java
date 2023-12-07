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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.event.dest.sqs.SqsDestinationProperties;
import net.solarnetwork.central.user.event.dest.sqs.SqsStats;
import net.solarnetwork.central.user.event.dest.sqs.SqsUserNodeEventHookService;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Test cases for the {@link SqsUserNodeEventHookService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SqsUserNodeEventHookServiceTests {

	private static Map<String, Object> TEST_PROPS;

	@BeforeAll
	public static void setupClass() {
		Properties p = new Properties();
		try {
			InputStream in = SqsUserNodeEventHookServiceTests.class.getClassLoader()
					.getResourceAsStream("sqs-dest.properties");
			if ( in != null ) {
				p.load(in);
				in.close();
			} else {
				throw new RuntimeException("The sqs-dest.properties classpath resource is missing.");
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

	private static SqsClient createSqsClient() {
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(TEST_PROPS);
		SqsClientBuilder builder = SqsClient.builder().region(Region.of(props.getRegion()));
		String accessKey = props.getAccessKey();
		String secretKey = props.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	private static void drainQueue() {
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(TEST_PROPS);
		SqsClient client = createSqsClient();
		String queueUrl = null;
		try {
			GetQueueUrlResponse urlRes = client.getQueueUrl((b) -> b.queueName(props.getQueueName()));
			queueUrl = urlRes.queueUrl();
		} catch ( QueueDoesNotExistException e ) {
			throw new IllegalArgumentException(
					String.format("Queue [%s] does not exist (using region %s).", props.getQueueName(),
							props.getRegion()));
		}

		ReceiveMessageRequest req = ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(0)
				.build();
		while ( true ) {
			ReceiveMessageResponse res = client.receiveMessage(req);
			if ( res == null ) {
				break;
			}
			List<Message> msgs = res.messages();
			if ( msgs == null || msgs.isEmpty() ) {
				break;
			}
			List<DeleteMessageBatchRequestEntry> delEntries = new ArrayList<>(8);
			for ( Message msg : msgs ) {
				delEntries.add(DeleteMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
						.receiptHandle(msg.receiptHandle()).build());
			}
			DeleteMessageBatchRequest delReq = DeleteMessageBatchRequest.builder().queueUrl(queueUrl)
					.entries(delEntries).build();
			client.deleteMessageBatch(delReq);
		}
	}

	private static String getQueueMessage() {
		SqsDestinationProperties props = SqsDestinationProperties.ofServiceProperties(TEST_PROPS);
		SqsClient client = createSqsClient();
		String queueUrl = null;
		try {
			GetQueueUrlResponse urlRes = client.getQueueUrl((b) -> b.queueName(props.getQueueName()));
			queueUrl = urlRes.queueUrl();
		} catch ( QueueDoesNotExistException e ) {
			throw new IllegalArgumentException(
					String.format("Queue [%s] does not exist (using region %s).", props.getQueueName(),
							props.getRegion()));
		}

		ReceiveMessageRequest req = ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(0)
				.build();
		ReceiveMessageResponse res = client.receiveMessage(req);
		assertThat("Message received", res.messages(), hasSize(1));

		final String qUrl = queueUrl;
		client.deleteMessage(
				(b) -> b.queueUrl(qUrl).receiptHandle(res.messages().get(0).receiptHandle()));

		String msgBody = res.messages().get(0).body();
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
		eventProps.put("uid", UUID.randomUUID().toString());
		event.setTaskProperties(eventProps);

		// WHEN
		boolean result = service.processUserNodeEventHook(config, event);

		// THEN
		assertThat("Result OK", result, equalTo(true));

		String msgBody = getQueueMessage();
		Map<String, Object> msgData = JsonUtils.getStringMap(msgBody);
		assertThat("Message object property count", msgData.keySet(),
				containsInAnyOrder("hookId", "userId", "nodeId", "sourceId", "foo", "val", "uid"));
		assertThat("Hook ID prop", msgData, hasEntry("hookId", config.getId().getId().intValue()));
		assertThat("User ID prop", msgData, hasEntry("userId", config.getUserId().intValue()));
		assertThat("Node ID prop", msgData, hasEntry("nodeId", event.getNodeId().intValue()));
		assertThat("Source ID prop", msgData, hasEntry("sourceId", event.getSourceId()));
		assertThat("foo prop", msgData, hasEntry("foo", eventProps.get("foo")));
		assertThat("val prop", msgData, hasEntry("val", eventProps.get("val")));
		assertThat("uid prop", msgData, hasEntry("uid", eventProps.get("uid")));
	}

}
