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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
import net.solarnetwork.central.support.EntityCodec;
import net.solarnetwork.central.support.SqsOverflowQueueSettings;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.central.support.UserEventBasicDeserializer;
import net.solarnetwork.central.support.UserEventBasicSerializer;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidGenerator;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
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
 * @version 1.1
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
	private ArgumentCaptor<ReceiveMessageRequest> receiveMessageRequestCaptor;

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

		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsIgnored))
			.as("Ignored persistence failure counted as ignored")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsFailed))
			.as("Ignored persistence failure not counted as a failure")
			.isEqualTo(0)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsStored))
			.as("Ignored persistence failure not counted as stored")
			.isEqualTo(0)
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


	/**
	 * Verify that an exception that is <b>not</b> assignable to any of the
	 * configured ignored exceptions is treated as a persistence failure, and the
	 * entity overflows to SQS.
	 */
	@Test
	public void exceptionOnStore_notIgnored() throws IOException {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setIgnoredDaoExceptions(Set.of(IllegalStateException.class));
	
		SendMessageResponse sendToSqsResponse = SendMessageResponse.builder().messageId(randomString())
				.build();
		given(sqsClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(sendToSqsResponse));
	
		// note this is NOT assignable to the configured IllegalStateException
		Throwable t = new IllegalArgumentException("boom!");
		given(delegateDao.persist(any())).willThrow(t);
	
		// WHEN
		collector.serviceDidStartup();
	
		UserEvent entity = newEvent();
	
		UserUuidPK result = collector.persist(entity);
	
		collector.shutdownAndWait();
	
		// THEN
		// @formatter:off
		then(exceptionHandler).should().uncaughtException(any(), throwableCaptor.capture());
		and.then(throwableCaptor.getValue())
			.as("Non-ignored exception passed to handler")
			.isSameAs(t)
			;
	
		then(sqsClient).should().sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getValue())
			.as("Entity overflowed to SQS because the exception was not ignored")
			.isNotNull()
			.as("SQS message is JSON serialization of entity")
			.returns(JSON_MAPPER.writeValueAsString(entity), from(SendMessageRequest::messageBody))
			;
	
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsFailed))
			.as("Non-ignored persistence failure counted as a failure")
			.isEqualTo(1)
			;
	
		and.then(result)
			.as("Result provided from SQS overflow")
			.isEqualTo(entity.getId())
			;
		// @formatter:on
	}
	
	/**
	 * Verify that a SQS message that cannot be deserialized does not stop the
	 * reader thread from processing subsequent messages.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void readFromSqs_malformedMessage() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread
		collector.setReadSleepMinMs(20);
		collector.setReadSleepThrottleStepMs(20);
	
		// the same malformed message, redelivered: SQS reports a rising receive count
		// until the queue redrive policy moves it to the dead-letter queue
		final String badMessageId = randomString();
		final Message badMessage = Message.builder().messageId(badMessageId)
				.receiptHandle(randomString()).body("{not valid json")
				.attributes(Map.of(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "1"))
				.build();
		final Message badMessageRedelivered = Message.builder().messageId(badMessageId)
				.receiptHandle(randomString()).body("{not valid json")
				.attributes(Map.of(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "2"))
				.build();
	
		final UserEvent entity = newEvent();
		final Message goodMessage = Message.builder().messageId(randomString())
				.receiptHandle(randomString()).body(JSON_MAPPER.writeValueAsString(entity)).build();
	
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().messages(badMessage).build()))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().messages(badMessageRedelivered).build()))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().messages(goodMessage).build()))
				.willReturn(CompletableFuture
						.completedFuture(ReceiveMessageResponse.builder().build()));
	
		// these are only exercised once the reader survives the malformed message
		lenient().when(delegateDao.persist(any())).thenReturn(entity.getId());
		lenient().when(sqsClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						DeleteMessageBatchResponse.builder().build()));
		lenient().when(sqsClient.changeMessageVisibilityBatch(any(Consumer.class)))
				.thenReturn(CompletableFuture.completedFuture(
						ChangeMessageVisibilityBatchResponse.builder().build()));
	
		// WHEN
		collector.serviceDidStartup();
	
		// THEN
		// @formatter:off
		then(sqsClient).should(timeout(3_000).atLeast(3))
			.receiveMessage(any(ReceiveMessageRequest.class))
			;
	
		then(delegateDao).should(timeout(3_000)).persist(entityCaptor.capture());
		and.then(entityCaptor.getValue())
			.usingRecursiveComparison()
			.as("Message following the malformed one still parsed and persisted")
			.isEqualTo(entity)
			;

		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsDiscarded))
			.as("Malformed message counted once, not once per redelivery")
			.isEqualTo(1)
			;

		then(sqsClient).should(never())
			.changeMessageVisibilityBatch(any(Consumer.class))
			;
		// @formatter:on
	}
	
	/**
	 * The outcome of {@link #runDeleteBatchPartialFailure(boolean)}.
	 *
	 * @param failedHandle
	 *        the receipt handle of the message whose delete failed
	 * @param requests
	 *        the delete batch requests issued, in order
	 */
	private record DeleteBatchScenario(String failedHandle,
			List<DeleteMessageBatchRequest> requests) {
	
	}
	
	/**
	 * Process two SQS messages, failing the delete of the first as part of a
	 * batch delete request.
	 *
	 * @param senderFault
	 *        the {@code senderFault} value to report on the failed entry
	 * @return the scenario outcome
	 */
	private DeleteBatchScenario runDeleteBatchPartialFailure(boolean senderFault) throws Exception {
		// a handle queue with capacity 1, so a flushed batch holds a handle other
		// than the one whose offer triggered the flush
		final BlockingQueue<String> handles = new LinkedHashSetBlockingQueue<>(1);
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				workQueue, handles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(1);
		queue.setWriteConcurrency(1);
		queue.setShutdownWaitSecs(3600);
	
		final String handleA = "handle-A-" + randomString();
		final String handleB = "handle-B-" + randomString();
	
		final UserEvent entityA = newEvent();
		final UserEvent entityB = newEvent();
	
		final Message msgA = Message.builder().messageId(randomString()).receiptHandle(handleA)
				.body(JSON_MAPPER.writeValueAsString(entityA)).build();
		final Message msgB = Message.builder().messageId(randomString()).receiptHandle(handleB)
				.body(JSON_MAPPER.writeValueAsString(entityB)).build();
	
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().messages(msgA, msgB).build()))
				.willReturn(CompletableFuture
						.completedFuture(ReceiveMessageResponse.builder().build()));
	
		// persist slowly, so the reader registers both completion callbacks before
		// the writer completes the first work item, keeping the delete order stable
		given(delegateDao.persist(any())).willAnswer(inv -> {
			Thread.sleep(100);
			return ((UserEvent) inv.getArgument(0)).getId();
		});
	
		final List<DeleteMessageBatchRequest> deleteRequests = Collections
				.synchronizedList(new ArrayList<>(2));
		given(sqsClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willAnswer(inv -> {
			final DeleteMessageBatchRequest req = inv.getArgument(0);
			deleteRequests.add(req);
			if ( deleteRequests.size() > 1 ) {
				return CompletableFuture
						.completedFuture(DeleteMessageBatchResponse.builder().build());
			}
			// fail just the entry for handle A
			var failed = req.entries().stream().filter(e -> handleA.equals(e.receiptHandle()))
					.map(e -> BatchResultErrorEntry.builder().id(e.id()).senderFault(senderFault)
							.code(senderFault ? "ReceiptHandleIsInvalid" : "InternalError")
							.message("simulated failure").build())
					.toList();
			var successful = req.entries().stream().filter(e -> !handleA.equals(e.receiptHandle()))
					.map(e -> DeleteMessageBatchResultEntry.builder().id(e.id()).build()).toList();
			return CompletableFuture.completedFuture(DeleteMessageBatchResponse.builder()
					.failed(failed).successful(successful).build());
		});
	
		queue.serviceDidStartup();
	
		then(delegateDao).should(timeout(5_000).times(2)).persist(any());
		Thread.sleep(200); // let the delete batching settle
	
		queue.shutdownAndWait(); // forces a flush of anything still pending
	
		return new DeleteBatchScenario(handleA, deleteRequests);
	}
	
	/**
	 * Verify that when a message fails to delete from SQS as part of a batch
	 * request, the receipt handle of the message that actually failed is the one
	 * retried.
	 */
	@Test
	public void deleteFromSqs_partialFailure_retriesFailedHandle() throws Exception {
		// GIVEN / WHEN
		final DeleteBatchScenario scenario = runDeleteBatchPartialFailure(false);
	
		// THEN
		// @formatter:off
		and.then(scenario.requests())
			.as("Initial batch request, then a flush of the retried handle")
			.hasSize(2)
			;
		and.then(scenario.requests().get(1).entries())
			.as("Flushed batch holds the single retried handle")
			.hasSize(1)
			.as("The retried handle is the one that failed to delete, not the one that"
						+ " triggered the batch")
			.allMatch(e -> scenario.failedHandle().equals(e.receiptHandle()))
			;
		// @formatter:on
	}
	
	/**
	 * Verify that a delete failure reported as a sender fault, which can never
	 * succeed on retry, is not retried.
	 */
	@Test
	public void deleteFromSqs_partialFailure_senderFaultNotRetried() throws Exception {
		// GIVEN / WHEN
		final DeleteBatchScenario scenario = runDeleteBatchPartialFailure(true);
	
		// THEN
		// @formatter:off
		and.then(scenario.requests())
			.as("A sender fault cannot succeed on retry, so no handle is re-queued and"
						+ " nothing remains to flush at shutdown")
			.hasSize(1)
			;
		// @formatter:on
	}

	/**
	 * Verify that a forced flush empties the pending handle queue however many
	 * delete requests that takes, and that no request exceeds the SQS maximum.
	 */
	@Test
	public void deleteFromSqs_forcedFlushEmptiesQueueLargerThanOneBatch() throws Exception {
		// GIVEN
		// a handle queue that can hold more than one delete request's worth
		final int messageCount = 12;
		final BlockingQueue<String> handles = new LinkedHashSetBlockingQueue<>(20);
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> workItems = new ArrayBlockingQueue<>(
				messageCount * 2);
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				workItems, handles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(1);
		queue.setWriteConcurrency(1);
		queue.setReadMaxMessageCount(messageCount);
		queue.setShutdownWaitSecs(3600);
	
		final List<String> receiptHandles = new ArrayList<>(messageCount);
		final List<Message> msgs = new ArrayList<>(messageCount);
		for ( int i = 0; i < messageCount; i++ ) {
			String handle = "handle-%d-%s".formatted(i, randomString());
			receiptHandles.add(handle);
			msgs.add(Message.builder().messageId(randomString()).receiptHandle(handle)
					.body(JSON_MAPPER.writeValueAsString(newEvent())).build());
		}
	
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().messages(msgs).build()))
				.willReturn(CompletableFuture
						.completedFuture(ReceiveMessageResponse.builder().build()));
	
		given(delegateDao.persist(any()))
				.willAnswer(inv -> ((UserEvent) inv.getArgument(0)).getId());
	
		final List<DeleteMessageBatchRequest> deleteRequests = Collections
				.synchronizedList(new ArrayList<>(4));
		given(sqsClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willAnswer(inv -> {
			deleteRequests.add(inv.getArgument(0));
			return CompletableFuture
					.completedFuture(DeleteMessageBatchResponse.builder().build());
		});
	
		// WHEN
		queue.serviceDidStartup();
		then(delegateDao).should(timeout(5_000).times(messageCount)).persist(any());
		Thread.sleep(200); // let the handles accumulate
	
		queue.shutdownAndWait(); // forces a flush of everything pending
	
		// THEN
		// @formatter:off
		and.then(deleteRequests)
			.as("More than one delete request was needed for %d handles".formatted(messageCount))
			.hasSizeGreaterThan(1)
			.allSatisfy(r -> and.then(r.entries())
					.as("No request exceeds the SQS per-request maximum of 10")
					.hasSizeLessThanOrEqualTo(10))
			;
	
		and.then(deleteRequests.stream().flatMap(r -> r.entries().stream())
					.map(DeleteMessageBatchRequestEntry::receiptHandle).toList())
			.as("Every handled message was deleted from SQS, with none left pending")
			.containsExactlyInAnyOrderElementsOf(receiptHandles)
			;
	
		and.then(handles)
			.as("Nothing left in the pending handle queue")
			.isEmpty()
			;
		// @formatter:on
	}

	/**
	 * Verify that {@link SqsOverflowQueue#shutdownAndWait()} returns promptly
	 * when the shutdown wait is configured as zero, rather than joining forever.
	 */
	@Test
	public void shutdownAndWait_zeroWaitReturnsPromptly() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setShutdownWaitSecs(0); // i.e. "do not wait"
	
		final CountDownLatch persisting = new CountDownLatch(1);
		given(delegateDao.persist(any())).willAnswer(inv -> {
			persisting.countDown();
			// ignore interrupts, to simulate a write that cannot be cancelled
			final long end = System.currentTimeMillis() + 2_000L;
			for ( long now = System.currentTimeMillis(); now < end; now = System
					.currentTimeMillis() ) {
				try {
					Thread.sleep(end - now);
				} catch ( InterruptedException e ) {
					// ignore
				}
			}
			return ((UserEvent) inv.getArgument(0)).getId();
		});
	
		collector.serviceDidStartup();
		workQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
	
		and.then(persisting.await(3, TimeUnit.SECONDS))
				.as("Writer thread is busy persisting")
				.isTrue();
	
		// WHEN
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			final Future<?> shutdown = executor.submit(collector::shutdownAndWait);
	
			// THEN
			// @formatter:off
			and.thenCode(() -> shutdown.get(500, TimeUnit.MILLISECONDS))
				.as("shutdownAndWait() returns promptly with a zero shutdown wait")
				.doesNotThrowAnyException()
				;
			// @formatter:on
		} finally {
			executor.shutdownNow();
		}
	}
	
	/**
	 * Verify that the shutdown wait is a total budget shared by all threads,
	 * rather than applied to each thread in turn.
	 */
	@Test
	public void shutdownAndWait_waitIsTotalNotPerThread() throws Exception {
		// GIVEN
		final int writerCount = 4;
		collector.setReadConcurrency(0); // disable read thread
		collector.setWriteConcurrency(writerCount);
		collector.setShutdownWaitSecs(1);
	
		final CountDownLatch persisting = new CountDownLatch(writerCount);
		given(delegateDao.persist(any())).willAnswer(inv -> {
			persisting.countDown();
			// ignore interrupts, so every writer outlives the shutdown wait
			final long end = System.currentTimeMillis() + 6_000L;
			for ( long now = System.currentTimeMillis(); now < end; now = System
					.currentTimeMillis() ) {
				try {
					Thread.sleep(end - now);
				} catch ( InterruptedException e ) {
					// ignore
				}
			}
			return ((UserEvent) inv.getArgument(0)).getId();
		});
	
		collector.serviceDidStartup();
		for ( int i = 0; i < writerCount; i++ ) {
			workQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
		}
	
		and.then(persisting.await(5, TimeUnit.SECONDS))
				.as("All writer threads are busy persisting")
				.isTrue();
	
		// WHEN
		final long start = System.nanoTime();
		collector.shutdownAndWait();
		final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
	
		// THEN
		// @formatter:off
		and.then(durationMs)
			.as("Waited about the 1s budget in total, not 1s for each of the %d threads"
						.formatted(writerCount))
			.isLessThan(2_500L)
			;
		// @formatter:on
	}

	/**
	 * Verify that work items still queued at shutdown are not silently dropped:
	 * their futures must be completed, one way or another.
	 */
	@Test
	public void shutdown_pendingWorkItemsCompleted() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
	
		// only exercised if shutdown drains remaining work to SQS
		lenient().when(sqsClient.sendMessage(any(SendMessageRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						SendMessageResponse.builder().messageId(randomString()).build()));
	
		final CountDownLatch persisting = new CountDownLatch(1);
		given(delegateDao.persist(any())).willAnswer(inv -> {
			persisting.countDown();
			Thread.sleep(300);
			return ((UserEvent) inv.getArgument(0)).getId();
		});
	
		collector.serviceDidStartup();
	
		// occupy the single writer thread
		workQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
		and.then(persisting.await(3, TimeUnit.SECONDS))
				.as("Writer thread is busy persisting")
				.isTrue();
	
		// queue more work behind it
		final List<CompletableFuture<UserUuidPK>> pending = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			var f = new CompletableFuture<UserUuidPK>();
			pending.add(f);
			workQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), f));
		}
	
		// WHEN
		collector.serviceDidShutdown();
	
		Thread.sleep(500); // allow any shutdown draining to finish
	
		// THEN
		// @formatter:off
		and.then(pending)
			.as("Every work item pending at shutdown had its future completed, rather"
						+ " than being silently dropped")
			.allMatch(CompletableFuture::isDone)
			;
		// @formatter:on
	}
	
	/**
	 * Verify that an ignored exception thrown by the "last ditch" direct DAO
	 * write, after failing to send the entity to SQS, is counted as ignored.
	 */
	@Test
	public void sendToSqsFails_exceptionOnStore_ignored() throws Exception {
		// GIVEN
		// a work queue that never accepts, so persist() goes straight to SQS
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> fullQueue = new LinkedHashSetBlockingQueue<>(
				0);
	
		// a codec that cannot serialize, so the SQS send fails before any request
		final EntityCodec<UserEvent, UserUuidPK, String> brokenCodec = new EntityCodec<>() {
	
			@Override
			public String serialize(UserEvent entity) {
				throw new IllegalStateException("cannot serialize");
			}
	
			@Override
			public UserEvent deserialize(String json) {
				throw new UnsupportedOperationException();
			}
	
			@Override
			public UserUuidPK entityId(UserEvent entity) {
				return entity.getId();
			}
	
		};
	
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				fullQueue, completedSqsMessageHandles, delegateDao, brokenCodec);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(0); // disable read thread
		queue.setWriteConcurrency(1);
		queue.setShutdownWaitSecs(3600);
		queue.setIgnoredDaoExceptions(Set.of(IllegalArgumentException.class));
	
		given(delegateDao.persist(any())).willThrow(new IllegalArgumentException("boom!"));
	
		// WHEN
		queue.serviceDidStartup();
	
		final UserEvent entity = newEvent();
		final UserUuidPK result = queue.persist(entity);
	
		queue.shutdownAndWait();
	
		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();
		then(sqsClient).shouldHaveNoInteractions();
	
		and.then(result)
			.as("Result provided after the ignored persistence failure")
			.isEqualTo(entity.getId())
			;
	
		and.then(stats.get(SqsOverflowQueue.BasicCount.SqsQueueFail))
			.as("SQS send failure counted")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsIgnored))
			.as("Ignored persistence failure on the last-ditch DAO write counted")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsDiscarded))
			.as("Ignored persistence failure not counted as discarded")
			.isEqualTo(0)
			;
		// @formatter:on
	}

	/**
	 * Verify that work items the writer threads cannot drain within the shutdown
	 * wait are overflowed to SQS, rather than dropped.
	 */
	@Test
	public void shutdown_undrainedWorkItemsOverflowToSqs() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setWriteConcurrency(1);
		collector.setShutdownWaitSecs(0); // no time for the writer to drain
	
		given(sqsClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(
						SendMessageResponse.builder().messageId(randomString()).build()));
	
		final CountDownLatch persisting = new CountDownLatch(1);
		given(delegateDao.persist(any())).willAnswer(inv -> {
			persisting.countDown();
			// ignore interrupts, so the writer cannot pick up any more work
			final long end = System.currentTimeMillis() + 3_000L;
			for ( long now = System.currentTimeMillis(); now < end; now = System
					.currentTimeMillis() ) {
				try {
					Thread.sleep(end - now);
				} catch ( InterruptedException e ) {
					// ignore
				}
			}
			return ((UserEvent) inv.getArgument(0)).getId();
		});
	
		collector.serviceDidStartup();
	
		// occupy the only writer thread
		workQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
		and.then(persisting.await(3, TimeUnit.SECONDS))
				.as("Writer thread is busy persisting")
				.isTrue();
	
		// queue work the writer will never get to
		final List<UserEvent> stranded = new ArrayList<>(3);
		final List<CompletableFuture<UserUuidPK>> pending = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			var entity = newEvent();
			var f = new CompletableFuture<UserUuidPK>();
			stranded.add(entity);
			pending.add(f);
			workQueue.put(new SqsOverflowQueue.WorkItem<>(entity, f));
		}
	
		// WHEN
		collector.serviceDidShutdown();
	
		// THEN
		// @formatter:off
		then(sqsClient).should(times(3)).sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getAllValues())
			.as("Every stranded entity was overflowed to SQS")
			.map(SendMessageRequest::messageBody)
			.containsExactlyInAnyOrderElementsOf(
						stranded.stream().map(JSON_MAPPER::writeValueAsString).toList())
			;
	
		and.then(pending)
			.as("Every stranded work item had its future completed")
			.allMatch(CompletableFuture::isDone)
			;
	
		and.then(stats.get(SqsOverflowQueue.BasicCount.SqsQueueAdds))
			.as("Overflowed entities counted as SQS additions")
			.isEqualTo(3)
			;
		// @formatter:on
	}

	/**
	 * Verify that a failure sending to SQS is counted, and falls back to a
	 * direct DAO write on the calling thread.
	 */
	@Test
	public void sendToSqsAsyncFailure_fallsBackToDao() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setWriteConcurrency(1);
	
		// work queue never accepts, so persist() goes straight to SQS
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> fullQueue = new LinkedHashSetBlockingQueue<>(
				0);
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				fullQueue, completedSqsMessageHandles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(0);
		queue.setWriteConcurrency(1);
		queue.setShutdownWaitSecs(3600);
	
		// the SQS send fails asynchronously
		given(sqsClient.sendMessage(any(SendMessageRequest.class))).willReturn(
				CompletableFuture.failedFuture(SdkClientException.create("no route to host")));
	
		final UserEvent entity = newEvent();
		given(delegateDao.persist(any())).willReturn(entity.getId());
	
		// WHEN
		queue.serviceDidStartup();
		final UserUuidPK result = queue.persist(entity);
		queue.shutdownAndWait();
	
		// THEN
		// @formatter:off
		and.then(result)
			.as("Entity persisted directly after the SQS send failed")
			.isEqualTo(entity.getId())
			;
	
		then(delegateDao).should().persist(entityCaptor.capture());
		and.then(entityCaptor.getValue())
			.as("The entity was handed to the delegate DAO")
			.isSameAs(entity)
			;
	
		and.then(stats.get(SqsOverflowQueue.BasicCount.SqsQueueFail))
			.as("Asynchronous SQS send failure counted")
			.isEqualTo(1)
			;
		and.then(stats.get(SqsOverflowQueue.BasicCount.ObjectsStored))
			.as("Entity counted as stored by the fallback")
			.isEqualTo(1)
			;
		// @formatter:on
	}
	
	/**
	 * Verify that a SQS send that never completes does not block the caller
	 * indefinitely.
	 */
	@Test
	public void sendToSqsNeverCompletes_boundedBySqsSendMaxWait() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
	
		// work queue never accepts, so persist() goes straight to SQS
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> fullQueue = new LinkedHashSetBlockingQueue<>(
				0);
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				fullQueue, completedSqsMessageHandles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(0);
		queue.setWriteConcurrency(1);
		queue.setShutdownWaitSecs(3600);
		queue.setSqsSendMaxWaitMs(300);
	
		// a send that never completes, like a hung connection
		given(sqsClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(new CompletableFuture<>());
	
		final UserEvent entity = newEvent();
		given(delegateDao.persist(any())).willReturn(entity.getId());
	
		// WHEN
		queue.serviceDidStartup();
		final long start = System.nanoTime();
		final UserUuidPK result = queue.persist(entity);
		final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		queue.shutdownAndWait();
	
		// THEN
		// @formatter:off
		and.then(durationMs)
			.as("Caller released after the SQS send wait, rather than blocking forever")
			.isLessThan(3_000L)
			;
		and.then(result)
			.as("Entity persisted directly once the SQS send timed out")
			.isEqualTo(entity.getId())
			;
		// @formatter:on
	}
	
	/**
	 * Verify that an entity a writer thread persists within the work item wait
	 * is never sent to SQS.
	 */
	@Test
	public void store_completedWithinWaitNotSentToSqs() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setWorkItemMaxWaitMs(2_000);
	
		final UserEvent entity = newEvent();
	
		given(delegateDao.persist(any())).willAnswer(inv -> {
			Thread.sleep(50);
			return entity.getId();
		});
	
		// WHEN
		collector.serviceDidStartup();
		final UserUuidPK result = collector.persist(entity);
		collector.shutdownAndWait();
	
		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided by the writer thread")
			.isEqualTo(entity.getId())
			;
	
		then(sqsClient).should(never()).sendMessage(any(SendMessageRequest.class));
	
		and.then(stats.get(SqsOverflowQueue.BasicCount.SqsQueueAdds))
			.as("Entity persisted directly, so never paid for SQS")
			.isEqualTo(0)
			;
		// @formatter:on
	}

	/**
	 * Verify that the reader does not request more messages than the work queue
	 * can accept.
	 */
	@Test
	public void readFromSqs_requestsNoMoreThanWorkQueueCapacity() throws Exception {
		// GIVEN
		// a work queue holding 2 items with room for 2; the single writer takes one
		// and wedges on it, leaving exactly 1 free slot for the reader
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> smallQueue = new ArrayBlockingQueue<>(
				2);
		smallQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
		smallQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
	
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				smallQueue, completedSqsMessageHandles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(1);
		queue.setWriteConcurrency(1);
		queue.setReadMaxMessageCount(10);
		queue.setReadSleepMinMs(50);
		queue.setShutdownWaitSecs(0);
	
		given(delegateDao.persist(any())).willAnswer(_ -> {
			wedgeForever();
			return null;
		});
	
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(
				CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));
	
		// WHEN
		queue.serviceDidStartup();
		then(sqsClient).should(timeout(3_000).atLeastOnce())
				.receiveMessage(receiveMessageRequestCaptor.capture());
		queue.shutdownAndWait();
	
		// THEN
		// @formatter:off
		and.then(receiveMessageRequestCaptor.getAllValues())
			.as("Requested only the 1 free work queue slot, not the configured 10")
			.allMatch(r -> r.maxNumberOfMessages() == 1)
			;
		// @formatter:on
	}
	
	/**
	 * Verify that the reader stops issuing receive requests while the work queue
	 * is full.
	 */
	@Test
	public void readFromSqs_doesNotReadWhileWorkQueueFull() throws Exception {
		// GIVEN
		// a zero-capacity work queue, so no writer can ever free a slot
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> fullQueue = new LinkedHashSetBlockingQueue<>(
				0);
	
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				fullQueue, completedSqsMessageHandles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(1);
		queue.setWriteConcurrency(1);
		queue.setShutdownWaitSecs(3600);
	
		// WHEN
		queue.serviceDidStartup();
		Thread.sleep(500);
		queue.shutdownAndWait();
	
		// THEN
		// @formatter:off
		then(sqsClient).should(never()).receiveMessage(any(ReceiveMessageRequest.class));
		// @formatter:on
	}
	
	/**
	 * Verify that the read throttle relaxes again while the SQS queue is empty,
	 * rather than staying where a transient error left it.
	 */
	@Test
	public void readFromSqs_throttleRelaxesWhileQueueEmpty() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread
		collector.setReadSleepMinMs(10);
		collector.setReadSleepMaxMs(5_000);
		collector.setReadSleepThrottleStepMs(1_000);
	
		// one transient error, which raises the throttle, then an empty queue
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture
						.failedFuture(SdkClientException.create("connection reset")))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().build()));
	
		// WHEN
		collector.serviceDidStartup();
	
		// the throttle has to decay back to readSleepMinMs for the reader to poll
		// this often; stuck at the 1s the error raised it to, it would manage about 4
		then(sqsClient).should(timeout(4_000).atLeast(20))
				.receiveMessage(any(ReceiveMessageRequest.class));
	
		collector.shutdownAndWait();
	}
	
	/**
	 * Verify that messages the work queue rejects are returned to SQS with a
	 * backoff, rather than made visible again immediately.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void readFromSqs_rejectedMessagesReturnedWithBackoff() throws Exception {
		// GIVEN
		// a work queue with one slot, occupied by an item the single writer takes
		// and wedges on, so the slot frees once and then stays taken
		final BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> smallQueue = new ArrayBlockingQueue<>(
				1);
		smallQueue.put(new SqsOverflowQueue.WorkItem<>(newEvent(), new CompletableFuture<>()));
		final var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "test", sqsClient, sqsUrl,
				smallQueue, completedSqsMessageHandles, delegateDao, ENTITY_CODEC);
		queue.setExceptionHandler(exceptionHandler);
		queue.setReadConcurrency(1);
		queue.setWriteConcurrency(1);
		queue.setReadSleepMinMs(1_000);
		queue.setReadSleepThrottleStepMs(1_000);
		queue.setShutdownWaitSecs(0);
	
		given(delegateDao.persist(any())).willAnswer(_ -> {
			wedgeForever();
			return null;
		});
	
		final String rejectedHandle = "handle-rejected-" + randomString();
		final Message msgA = Message.builder().messageId(randomString())
				.receiptHandle(randomString()).body(JSON_MAPPER.writeValueAsString(newEvent())).build();
		final Message msgB = Message.builder().messageId(randomString())
				.receiptHandle(rejectedHandle).body(JSON_MAPPER.writeValueAsString(newEvent())).build();
	
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().messages(msgA, msgB).build()))
				.willReturn(CompletableFuture.completedFuture(
						ReceiveMessageResponse.builder().build()));
	
		final List<ChangeMessageVisibilityBatchRequest> vizRequests = Collections
				.synchronizedList(new ArrayList<>(1));
		given(sqsClient.changeMessageVisibilityBatch(any(Consumer.class))).willAnswer(inv -> {
			Consumer<ChangeMessageVisibilityBatchRequest.Builder> c = inv.getArgument(0);
			var b = ChangeMessageVisibilityBatchRequest.builder();
			c.accept(b);
			vizRequests.add(b.build());
			return CompletableFuture.completedFuture(
					ChangeMessageVisibilityBatchResponse.builder().build());
		});
	
		// WHEN
		queue.serviceDidStartup();
		then(sqsClient).should(timeout(3_000)).changeMessageVisibilityBatch(any(Consumer.class));
		queue.shutdownAndWait();
	
		// THEN
		// @formatter:off
		and.then(vizRequests)
			.as("One visibility request for the rejected message")
			.hasSize(1)
			;
		and.then(vizRequests.get(0).entries())
			.as("Only the message the work queue rejected was returned")
			.hasSize(1)
			.allSatisfy(e -> {
				and.then(e.receiptHandle())
					.as("The rejected message handle")
					.isEqualTo(rejectedHandle)
					;
				and.then(e.visibilityTimeout())
					.as("Returned with a backoff, not made visible immediately")
					.isGreaterThan(0)
					;
			})
			;
		// @formatter:on
	}

	/**
	 * Verify that an entity submitted after shutdown still reaches SQS, promptly,
	 * rather than waiting out the work item timeout on a queue no writer thread
	 * is draining.
	 */
	@Test
	public void persistAfterShutdown_goesStraightToSqs() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread
		collector.setWorkItemMaxWaitMs(5_000);
	
		given(sqsClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(
						SendMessageResponse.builder().messageId(randomString()).build()));
	
		collector.serviceDidStartup();
		collector.shutdownAndWait();
	
		final UserEvent entity = newEvent();
	
		// WHEN
		final long start = System.nanoTime();
		final UserUuidPK result = collector.persist(entity);
		final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
	
		// THEN
		// @formatter:off
		and.then(durationMs)
			.as("Sent to SQS at once, rather than waiting out the 5s work item timeout")
			.isLessThan(2_000L)
			;
	
		then(sqsClient).should().sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getValue())
			.as("Entity sent to SQS after shutdown")
			.returns(JSON_MAPPER.writeValueAsString(entity), from(SendMessageRequest::messageBody))
			;
	
		and.then(result)
			.as("Result provided")
			.isEqualTo(entity.getId())
			;
	
		then(delegateDao).shouldHaveNoInteractions();
	
		and.then(workQueue)
			.as("Nothing left stranded in the work queue")
			.isEmpty()
			;
		// @formatter:on
	}
	
	/**
	 * Verify that an unexpected error reading from SQS does not stop the reader
	 * thread.
	 */
	@Test
	public void readFromSqs_survivesUnexpectedError() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread
		collector.setReadSleepMinMs(20);
		collector.setReadSleepThrottleStepMs(20);
		collector.setReadSleepMaxMs(100);
	
		// neither an AWS nor an interrupt failure, i.e. a bug rather than a fault
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(
				CompletableFuture.failedFuture(new IllegalStateException("unexpected")));
	
		// WHEN
		collector.serviceDidStartup();
	
		// THEN
		// @formatter:off
		then(sqsClient).should(timeout(3_000).atLeast(3))
			.receiveMessage(any(ReceiveMessageRequest.class))
			;
		// @formatter:on
	}

	/**
	 * Verify the ping test reports a service that is not running as unhealthy,
	 * rather than reporting success without checking any threads.
	 */
	@Test
	public void pingTest_notRunning() throws Exception {
		// GIVEN
		given(sqsClient.getQueueAttributes(ArgumentMatchers.<Consumer<GetQueueAttributesRequest.Builder>> any()))
				.willReturn(CompletableFuture.completedFuture(
						GetQueueAttributesResponse.builder().build()));
	
		// WHEN
		// never started, so no writer or reader threads exist
		PingTest.Result result = collector.performPingTest();
	
		// THEN
		// @formatter:off
		and.then(result)
			.as("Ping fails when the service is not running")
			.returns(false, from(PingTest.Result::isSuccess))
			.as("Reason given")
			.returns("Service not running.", from(PingTest.Result::getMessage))
			;
		// @formatter:on
	}
	
	/**
	 * Verify the ping test succeeds while the service is running.
	 */
	@Test
	public void pingTest_running() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1);
		collector.setWriteConcurrency(2);
		given(sqsClient.getQueueAttributes(ArgumentMatchers.<Consumer<GetQueueAttributesRequest.Builder>> any()))
				.willReturn(CompletableFuture.completedFuture(
						GetQueueAttributesResponse.builder().build()));
		lenient().when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
				CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));
	
		// WHEN
		collector.serviceDidStartup();
		PingTest.Result result = collector.performPingTest();
		collector.shutdownAndWait();
	
		// THEN
		// @formatter:off
		and.then(result)
			.as("Ping succeeds while all threads are running")
			.returns(true, from(PingTest.Result::isSuccess))
			;
		and.then(result.getMessage())
			.as("Thread counts reported")
			.contains("2 writers", "1 readers")
			;
		// @formatter:on
	}
	
	/**
	 * Verify the SQS receive properties are clamped to the ranges SQS allows.
	 */
	@Test
	public void sqsReceivePropertiesClampedToValidRange() {
		// WHEN
		collector.setReadMaxMessageCount(50);
		collector.setReadMaxWaitTimeSecs(300);
	
		// THEN
		// @formatter:off
		and.then(collector.getReadMaxMessageCount())
			.as("Message count clamped to the SQS maximum")
			.isEqualTo(10)
			;
		and.then(collector.getReadMaxWaitTimeSecs())
			.as("Wait time clamped to the SQS maximum")
			.isEqualTo(20)
			;
	
		collector.setReadMaxMessageCount(0);
		collector.setReadMaxWaitTimeSecs(-5);
	
		and.then(collector.getReadMaxMessageCount())
			.as("Message count clamped to at least one message")
			.isEqualTo(1)
			;
		and.then(collector.getReadMaxWaitTimeSecs())
			.as("Wait time clamped to zero, i.e. no long polling")
			.isEqualTo(0)
			;
		// @formatter:on
	}
	
	/**
	 * Verify a sub-second configured read wait does not truncate to zero, which
	 * would turn off long polling and busy-poll the SQS queue.
	 */
	@Test
	public void settings_subSecondReadWaitDoesNotDisableLongPolling() {
		// GIVEN
		var settings = new SqsOverflowQueueSettings();
		settings.setReadMaxWaitTime(Duration.ofMillis(500));
		settings.setShutdownWait(Duration.ofMillis(500));
	
		// WHEN
		settings.configure(collector);
	
		// THEN
		// @formatter:off
		and.then(collector.getReadMaxWaitTimeSecs())
			.as("A 500ms read wait rounds up to 1s rather than truncating to no long polling")
			.isEqualTo(1)
			;
		and.then(collector.getShutdownWaitSecs())
			.as("A 500ms shutdown wait rounds up to 1s rather than truncating to no wait")
			.isEqualTo(1)
			;
	
		settings.setReadMaxWaitTime(Duration.ZERO);
		settings.configure(collector);
		and.then(collector.getReadMaxWaitTimeSecs())
			.as("An explicit zero still means no long polling")
			.isEqualTo(0)
			;
		// @formatter:on
	}
	
	/**
	 * Verify the convenience constructor derives its configuration from the
	 * service identity.
	 */
	@Test
	public void convenienceConstructor_usesIdentity() {
		// GIVEN
		final String identity = "MyQueue-" + randomString();

		// WHEN
		var queue = new SqsOverflowQueue<UserEvent, UserUuidPK>(identity, sqsClient, sqsUrl, workQueue,
				delegateDao, ENTITY_CODEC);

		// THEN
		// @formatter:off
		and.then(queue.getPingTestId())
			.as("Ping test ID is the given identity")
			.isEqualTo(identity)
			;
		and.then(queue.getSqsSendMaxWaitMs())
			.as("Configured with the property defaults")
			.isEqualTo(SqsOverflowQueue.DEFAULT_SQS_SEND_MAX_WAIT_MS)
			;
		// @formatter:on
	}

	/**
	 * Block the calling thread, ignoring interrupts, for longer than any test
	 * needs, so a writer thread that takes a work item never releases its slot.
	 */
	private static void wedgeForever() {
		final long end = System.currentTimeMillis() + 10_000L;
		for ( long now = System.currentTimeMillis(); now < end; now = System.currentTimeMillis() ) {
			try {
				Thread.sleep(end - now);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
	}

	private UserEvent newEvent() {
		return new UserEvent(randomLong(), UUID_GENERATOR.generate(),
				new String[] { randomString() }, null, null);
	}

}
