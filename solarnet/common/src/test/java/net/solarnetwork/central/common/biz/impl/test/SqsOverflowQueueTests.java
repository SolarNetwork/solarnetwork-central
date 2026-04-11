/* ==================================================================
 * SqsOverflowQueueTests.java - 18/03/2026 6:08:28 pm
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

package net.solarnetwork.central.common.biz.impl.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.biz.impl.IdentityJsonEntityCodec;
import net.solarnetwork.central.common.biz.impl.SqsOverflowQueue;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.central.support.UserEventBasicDeserializer;
import net.solarnetwork.central.support.UserEventBasicSerializer;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidGenerator;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Test cases for the {@link SqsOverflowQueue} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SqsOverflowQueueTests {

	public static final JacksonModule EVENT_MODULE;

	static {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(UserEvent.class, UserEventBasicSerializer.INSTANCE);
		m.addDeserializer(UserEvent.class, UserEventBasicDeserializer.INSTANCE);
		EVENT_MODULE = m;
	}

	private static final JsonMapper JSON_MAPPER = JsonUtils.JSON_OBJECT_MAPPER.rebuild()
			.addModule(EVENT_MODULE).build();

	private static final IdentityJsonEntityCodec<UserEvent, UserUuidPK> ENTITY_CODEC = new IdentityJsonEntityCodec<>(
			JSON_MAPPER, UserEvent.class);

	private static final Logger log = LoggerFactory.getLogger(SqsOverflowQueueTests.class);

	private static final UuidGenerator UUID_GENERATOR = TimeBasedV7UuidGenerator.INSTANCE_MICROS;

	@Mock
	private SqsAsyncClient sqsClient;

	@Mock
	private UncaughtExceptionHandler exceptionHandler;

	@Mock
	private GenericWriteOnlyDao<UserEvent, UserUuidPK> delegateDao;

	@Captor
	private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;

	@Captor
	private ArgumentCaptor<Throwable> throwableCaptor;

	@Captor
	private ArgumentCaptor<UserEvent> entityCaptor;

	@Captor
	private ArgumentCaptor<Datum> datumCaptor;

	private String sqsUrl;
	private BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> workQueue;
	private BlockingQueue<String> completedSqsMessageHandles;
	private StatTracker stats;
	private SqsOverflowQueue<UserEvent, UserUuidPK> collector;

	@BeforeEach
	public void setup() {
		sqsUrl = "http://sqs.localhost/%d/test".formatted(randomLong());
		workQueue = new ArrayBlockingQueue<>(4);
		completedSqsMessageHandles = new LinkedHashSetBlockingQueue<>(0);
		stats = new StatTracker("SqsOverflowQueue", null, log, 10);
		collector = new SqsOverflowQueue<>(stats, "test", sqsClient, sqsUrl, workQueue,
				completedSqsMessageHandles, delegateDao, ENTITY_CODEC);
		collector.setExceptionHandler(exceptionHandler);
		collector.setReadConcurrency(1);
		collector.setWriteConcurrency(1);
		collector.setWorkItemMaxWaitMs(200);
		collector.setShutdownWaitSecs(3600);
	}

	@AfterEach
	public void teardown() {
		collector.serviceDidShutdown();
	}

	/**
	 * Verify that when an exception occurs on the delegate DAO, the entity
	 * overflows to SQS.
	 */
	@Test
	public void exceptionOnStore() throws IOException {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread

		// read from SQS
		SendMessageResponse sendToSqsResponse = SendMessageResponse.builder().messageId(randomString())
				.build();
		CompletableFuture<SendMessageResponse> sendToSqsFuture = CompletableFuture
				.completedFuture(sendToSqsResponse);
		given(sqsClient.sendMessage(any(SendMessageRequest.class))).willReturn(sendToSqsFuture);

		Throwable t = new RuntimeException("boom!");
		given(delegateDao.persist(any())).willThrow(t);

		// WHEN
		collector.serviceDidStartup();

		UserEvent entity = new UserEvent(randomLong(), UUID_GENERATOR.generate(),
				new String[] { randomString() }, null, null);

		UserUuidPK result = collector.persist(entity);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).should().uncaughtException(any(), throwableCaptor.capture());
		and.then(throwableCaptor.getValue())
			.as("Exception passed to handler")
			.isSameAs(t)
			;

		then(delegateDao).shouldHaveNoMoreInteractions();

		then(sqsClient).should().sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getValue())
			.as("Sent datum to SQS")
			.isNotNull()
			.as("SQS message is JSON serialization of datum")
			.returns(JSON_MAPPER.writeValueAsString(entity), from(SendMessageRequest::messageBody))
			;

		and.then(result)
			.as("Result provided")
			.isEqualTo(entity.getId())
			;
		// @formatter:on
	}

	@Test
	public void exceptionOnStore_ignored() throws IOException {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setIgnoredDaoExceptions(Set.of(IllegalArgumentException.class));

		Throwable t = new IllegalArgumentException("boom!");
		given(delegateDao.persist(any())).willThrow(t);

		// WHEN
		collector.serviceDidStartup();

		UserEvent entity = new UserEvent(randomLong(), UUID_GENERATOR.generate(),
				new String[] { randomString() }, null, null);

		UserUuidPK result = collector.persist(entity);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).shouldHaveNoMoreInteractions();

		then(sqsClient).shouldHaveNoInteractions();

		and.then(result)
			.as("Result provided")
			.isEqualTo(entity.getId())
			;
		// @formatter:on
	}

	/**
	 * Verify that when a timeout occurs persisting in the delegate DAO, the
	 * datum overflows to SQS.
	 */
	@Test
	public void slowStore() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread

		// store datum (slowly)
		UserUuidPK entityId = new UserUuidPK(randomLong(), UUID_GENERATOR.generate());
		given(delegateDao.persist(any())).willAnswer(_ -> {
			log.info("Sleeping on delegateDao.persist() to simulate slow performance...");
			Thread.sleep(250);
			return entityId;
		});

		// overflow to SQS from timeout
		SendMessageResponse sendToSqsResponse = SendMessageResponse.builder().messageId(randomString())
				.build();
		CompletableFuture<SendMessageResponse> sendToSqsFuture = CompletableFuture
				.completedFuture(sendToSqsResponse);
		given(sqsClient.sendMessage(any(SendMessageRequest.class))).willReturn(sendToSqsFuture);

		// WHEN
		collector.serviceDidStartup();

		UserEvent entity = new UserEvent(entityId, new String[] { randomString() }, null, null);
		UserUuidPK result = collector.persist(entity);

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().persist(entityCaptor.capture());
		and.then(entityCaptor.getValue())
			.as("Given datum passed directly to delegate DAO")
			.isSameAs(entity)
			;

		then(sqsClient).should().sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getValue())
			.as("Sent datum to SQS because DAO persist() took too long")
			.isNotNull()
			.as("SQS message is JSON serialization of datum")
			.returns(JSON_MAPPER.writeValueAsString(entity), from(SendMessageRequest::messageBody))
			;

		and.then(result)
			.as("Result provided")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

	/**
	 * Verify that a SQS message is processed and stored.
	 */
	@Test
	public void readFromSqs() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread

		// the reader will long-poll for messages; will will return one, then none on subsequent calls
		UserUuidPK entityId = new UserUuidPK(randomLong(), UUID_GENERATOR.generate());
		UserEvent entity = new UserEvent(entityId, new String[] { randomString() }, null, null);

		// read from SQS
		final String datumMessageReceiptHandle = randomString();
		final Message datumMessage = Message.builder().messageId(randomString())
				.receiptHandle(datumMessageReceiptHandle).body(JSON_MAPPER.writeValueAsString(entity))
				.build();
		ReceiveMessageResponse recvFromSqsResponse = ReceiveMessageResponse.builder()
				.messages(datumMessage).build();
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFuture = CompletableFuture
				.completedFuture(recvFromSqsResponse);
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFutureEmpty = CompletableFuture
				.completedFuture(ReceiveMessageResponse.builder().build());
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(recvFromSqsFuture)
				.willReturn(recvFromSqsFutureEmpty);

		// persist in SN
		given(delegateDao.persist(any())).willReturn(entityId);

		// delete from SQS
		DeleteMessageBatchResponse delSqsMessageResponse = DeleteMessageBatchResponse.builder()
				.successful(
						DeleteMessageBatchResultEntry.builder().id(datumMessageReceiptHandle).build())
				.build();
		CompletableFuture<DeleteMessageBatchResponse> delSqsMessageFuture = CompletableFuture
				.completedFuture(delSqsMessageResponse);
		given(sqsClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(delSqsMessageFuture);

		// WHEN
		collector.serviceDidStartup();

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().persist(entityCaptor.capture());
		and.then(entityCaptor.getValue())
			.usingRecursiveComparison()
			.as("Entity parsed from SQS message")
			.isEqualTo(entity)
			;

		and.then(stats.get(SqsOverflowQueue.BasicCount.SqsQueueReceived))
			.as("Message received from SQS")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.WorkQueueAdds))
			.as("Message from SQS added to work queue")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsStored))
			.as("Work queue entity persisted")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.SqsQueueRemovals))
			.as("Message deleted from SQS after successful storage")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.WorkQueueRemovals))
			.as("Work queue entity removed from work queue")
			.isEqualTo(1)
			;
		// @formatter:on
	}

}
