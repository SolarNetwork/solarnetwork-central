/* ==================================================================
 * BaseSqsIntegrationTestsSupport.java - 19/03/2026 5:34:31 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support.test;

import static net.solarnetwork.central.test.CommonTestUtils.basicTable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.SqsProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
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
 * Base class to support SQS integration tests.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseSqsIntegrationTestsSupport implements UncaughtExceptionHandler {

	/** SQS properties, loaded at runtime. */
	protected static SqsProperties SQS_PROPS;

	@BeforeAll
	public static void setupClass() {
		Properties p = new Properties();
		try {
			InputStream in = BaseSqsIntegrationTestsSupport.class.getClassLoader()
					.getResourceAsStream("sqs-datum-queue.properties");
			if ( in != null ) {
				p.load(in);
				in.close();
			} else {
				throw new RuntimeException(
						"The sqs-datum-queue.properties classpath resource is missing.");
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		Map<String, Object> m = new LinkedHashMap<>(p.size());
		for ( Map.Entry<Object, Object> me : p.entrySet() ) {
			m.put(me.getKey().toString(), me.getValue());
		}
		SQS_PROPS = SqsProperties.ofServiceProperties(m);
	}

	/** Class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected SqsAsyncClient client;
	protected List<Throwable> uncaughtExceptions;

	@BeforeEach
	public void baseSetup() {
		drainQueue(false);

		client = createSqsAsyncClient();

		uncaughtExceptions = new ArrayList<>(2);
	}

	@AfterEach
	public void baseTeardown() {
		drainQueue(true);
	}

	@Override
	public final void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptions.add(e);
	}

	protected static SqsAsyncClient createSqsAsyncClient() {
		SqsAsyncClientBuilder builder = SqsAsyncClient.builder()
				.region(Region.of(SQS_PROPS.getRegion()));
		String accessKey = SQS_PROPS.getAccessKey();
		String secretKey = SQS_PROPS.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	private static SqsClient createSqsClient() {
		SqsClientBuilder builder = SqsClient.builder().region(Region.of(SQS_PROPS.getRegion()));
		String accessKey = SQS_PROPS.getAccessKey();
		String secretKey = SQS_PROPS.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	protected final void drainQueue(boolean logMessages) {
		SqsClient client = createSqsClient();
		String queueUrl = null;
		try {
			GetQueueUrlResponse urlRes = client
					.getQueueUrl((b) -> b.queueName(SQS_PROPS.getQueueName()));
			queueUrl = urlRes.queueUrl();
			log.info("SQS queue URL: {}", queueUrl);
		} catch ( QueueDoesNotExistException e ) {
			throw new IllegalArgumentException(
					String.format("Queue [%s] does not exist (using region %s).",
							SQS_PROPS.getQueueName(), SQS_PROPS.getRegion()));
		}

		ReceiveMessageRequest req = ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(0)
				.maxNumberOfMessages(10).build();
		while ( true ) {
			ReceiveMessageResponse res = client.receiveMessage(req);
			if ( res == null ) {
				break;
			}
			List<Message> msgs = res.messages();
			log.info("SQS queue {} discovered {} messages to drain.", queueUrl, msgs.size());
			if ( logMessages && msgs.size() > 0 ) {
				Map<String, String> map = new LinkedHashMap<>(msgs.size());
				for ( Message msg : msgs ) {
					map.put(msg.messageId(), msg.body());
				}
				log.info("Purging messages:\n{}", basicTable(map, "Message ID", "Message Body"));
			}
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

}
