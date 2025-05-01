/* ==================================================================
 * SqsDatumCollectorTests.java - 30/04/2025 8:12:53 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.SqsDatumCollector;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumPK;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.StatTracker;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Test cases for the {@link SqsDatumCollector} .
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SqsDatumCollectorTests {

	private static final Logger log = LoggerFactory.getLogger(SqsDatumCollectorTests.class);

	@Mock
	private SqsAsyncClient sqsClient;

	@Mock
	private UncaughtExceptionHandler exceptionHandler;

	@Mock
	private DatumWriteOnlyDao delegateDao;

	@Captor
	private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;

	@Captor
	private ArgumentCaptor<Throwable> throwableCaptor;

	@Captor
	private ArgumentCaptor<StreamDatum> streamDatumCaptor;

	@Captor
	private ArgumentCaptor<Datum> datumCaptor;

	private String sqsUrl;
	private BlockingQueue<SqsDatumCollector.WorkItem> workQueue;
	private ObjectMapper sqsObjectMapper;
	private StatTracker stats;
	private SqsDatumCollector collector;

	@BeforeEach
	public void setup() {
		sqsUrl = "http://sqs.localhost/%d/test".formatted(randomLong());
		workQueue = new ArrayBlockingQueue<>(4);
		sqsObjectMapper = DatumJsonUtils.newDatumObjectMapper();
		stats = new StatTracker("SqsDatumCollector", null, log, 10);
		collector = new SqsDatumCollector(sqsClient, sqsUrl, sqsObjectMapper, workQueue, delegateDao,
				stats);
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
	 * Verify that when an exception occurs on the delegate DAO, the datum
	 * overflows to SQS.
	 */
	@Test
	public void exceptionOnStore() throws IOException {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread

		SendMessageResponse sendToSqsResponse = SendMessageResponse.builder().messageId(randomString())
				.build();
		CompletableFuture<SendMessageResponse> sendToSqsFuture = CompletableFuture
				.completedFuture(sendToSqsResponse);
		given(sqsClient.sendMessage(any(SendMessageRequest.class))).willReturn(sendToSqsFuture);

		Throwable t = new RuntimeException("boom!");
		given(delegateDao.store(any(StreamDatum.class))).willThrow(t);

		// WHEN
		collector.serviceDidStartup();

		DatumEntity d = new DatumEntity(UUID.randomUUID(), now().truncatedTo(ChronoUnit.SECONDS), now(),
				propertiesOf(new BigDecimal[] { BigDecimal.ONE }, null, null, null));
		DatumPK result = collector.store(d);

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
			.returns(sqsObjectMapper.writeValueAsString(d), from(SendMessageRequest::messageBody))
			;

		and.then(result)
			.as("Result provided")
			.isEqualTo(new DatumPK(d.getStreamId(), d.getTimestamp()))
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
		DatumPK datumId = new DatumPK(UUID.randomUUID(), now().truncatedTo(ChronoUnit.SECONDS));
		given(delegateDao.store(any(StreamDatum.class))).willAnswer(invocation -> {
			log.info("Sleeping on delegateDao.store() to simulate slow performance...");
			Thread.sleep(250);
			return datumId;
		});

		// overflow to SQS from timeout
		SendMessageResponse sendToSqsResponse = SendMessageResponse.builder().messageId(randomString())
				.build();
		CompletableFuture<SendMessageResponse> sendToSqsFuture = CompletableFuture
				.completedFuture(sendToSqsResponse);
		given(sqsClient.sendMessage(any(SendMessageRequest.class))).willReturn(sendToSqsFuture);

		// WHEN
		collector.serviceDidStartup();

		DatumEntity d = new DatumEntity(datumId, now(),
				propertiesOf(new BigDecimal[] { BigDecimal.ONE }, null, null, null));
		DatumPK result = collector.store(d);

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().store(streamDatumCaptor.capture());
		and.then(streamDatumCaptor.getValue())
			.as("Given datum passed directly to delegate DAO")
			.isSameAs(d)
			;

		then(sqsClient).should().sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getValue())
			.as("Sent datum to SQS because DAO store() took too long")
			.isNotNull()
			.as("SQS message is JSON serialization of datum")
			.returns(sqsObjectMapper.writeValueAsString(d), from(SendMessageRequest::messageBody))
			;

		and.then(result)
			.as("Result provided")
			.isEqualTo(new DatumPK(d.getStreamId(), d.getTimestamp()))
			;
		// @formatter:on
	}

	/**
	 * Verify that a SQS StreamDatum message is processed and stored.
	 */
	@Test
	public void readFromSqs_StreamDatum() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread

		// the reader will long-poll for messages; will will return one, then none on subsequent calls
		final DatumPK datumId = new DatumPK(UUID.randomUUID(), now().truncatedTo(ChronoUnit.SECONDS));
		final DatumEntity d = new DatumEntity(datumId, now(),
				propertiesOf(new BigDecimal[] { BigDecimal.ONE }, null, null, null));

		final Message datumMessage = Message.builder().messageId(randomString())
				.body(sqsObjectMapper.writeValueAsString(d)).build();
		ReceiveMessageResponse recvFromSqsResponse = ReceiveMessageResponse.builder()
				.messages(datumMessage).build();
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFuture = CompletableFuture
				.completedFuture(recvFromSqsResponse);
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFutureEmpty = CompletableFuture
				.completedFuture(ReceiveMessageResponse.builder().build());
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(recvFromSqsFuture)
				.willReturn(recvFromSqsFutureEmpty);

		given(delegateDao.store(any(StreamDatum.class))).willReturn(datumId);

		// WHEN
		collector.serviceDidStartup();

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().store(streamDatumCaptor.capture());
		and.then(streamDatumCaptor.getValue())
			.as("Stream ID parsed from SQS message")
			.returns(d.getId().getStreamId(), from(StreamDatum::getStreamId))
			.as("Timestamp parsed from SQS message")
			.returns(d.getId().getTimestamp(), from(StreamDatum::getTimestamp))
			.as("Datum properties parsed from SQS message")
			.returns(d.getProperties(), from(StreamDatum::getProperties))
			;
		// @formatter:on
	}

	/**
	 * Verify that a SQS Node Datum message is processed and stored.
	 */
	@Test
	public void readFromSqs_GeneralDatum_NodeKind() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread

		// the reader will long-poll for messages; will will return one, then none on subsequent calls
		final GeneralDatum d = GeneralDatum.nodeDatum(randomLong(), randomString(),
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				new DatumSamples(Map.of("a", 1), Map.of("b", 2), null));

		final Message datumMessage = Message.builder().messageId(randomString())
				.body(sqsObjectMapper.writeValueAsString(d)).build();
		ReceiveMessageResponse recvFromSqsResponse = ReceiveMessageResponse.builder()
				.messages(datumMessage).build();
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFuture = CompletableFuture
				.completedFuture(recvFromSqsResponse);
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFutureEmpty = CompletableFuture
				.completedFuture(ReceiveMessageResponse.builder().build());
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(recvFromSqsFuture)
				.willReturn(recvFromSqsFutureEmpty);

		// store datum
		given(delegateDao.store(any(Datum.class))).willReturn(new ObjectDatumPK(d.getKind(),
				d.getObjectId(), d.getSourceId(), d.getTimestamp(), null));

		// WHEN
		collector.serviceDidStartup();

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().store(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Object kind parsed from SQS message")
			.returns(d.getKind(), from(Datum::getKind))
			.as("Object ID parsed from SQS message")
			.returns(d.getObjectId(), from(Datum::getObjectId))
			.as("Source ID parsed from SQS message")
			.returns(d.getSourceId(), from(Datum::getSourceId))
			.as("Timestamp parsed from SQS message")
			.returns(d.getTimestamp(), from(Datum::getTimestamp))
			.as("Datum samples parsed from SQS message")
			.returns(d.getSamples(), from(Datum::asSampleOperations))
			;
		// @formatter:on
	}

	/**
	 * Verify that a SQS Location Datum message is processed and stored.
	 */
	@Test
	public void readFromSqs_GeneralDatum_LocationKind() throws Exception {
		// GIVEN
		collector.setReadConcurrency(1); // enable read thread

		// the reader will long-poll for messages; will will return one, then none on subsequent calls
		final GeneralDatum d = GeneralDatum.locationDatum(randomLong(), randomString(),
				now().truncatedTo(ChronoUnit.MILLIS),
				new DatumSamples(Map.of("a", 1), Map.of("b", 2), null));

		final Message datumMessage = Message.builder().messageId(randomString())
				.body(sqsObjectMapper.writeValueAsString(d)).build();
		ReceiveMessageResponse recvFromSqsResponse = ReceiveMessageResponse.builder()
				.messages(datumMessage).build();
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFuture = CompletableFuture
				.completedFuture(recvFromSqsResponse);
		CompletableFuture<ReceiveMessageResponse> recvFromSqsFutureEmpty = CompletableFuture
				.completedFuture(ReceiveMessageResponse.builder().build());
		given(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(recvFromSqsFuture)
				.willReturn(recvFromSqsFutureEmpty);

		// store datum
		given(delegateDao.store(any(Datum.class))).willReturn(new ObjectDatumPK(d.getKind(),
				d.getObjectId(), d.getSourceId(), d.getTimestamp(), null));

		// WHEN
		collector.serviceDidStartup();

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().store(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Object kind parsed from SQS message")
			.returns(d.getKind(), from(Datum::getKind))
			.as("Object ID parsed from SQS message")
			.returns(d.getObjectId(), from(Datum::getObjectId))
			.as("Source ID parsed from SQS message")
			.returns(d.getSourceId(), from(Datum::getSourceId))
			.as("Timestamp parsed from SQS message")
			.returns(d.getTimestamp(), from(Datum::getTimestamp))
			.as("Datum samples parsed from SQS message")
			.returns(d.getSamples(), from(Datum::asSampleOperations))
			;
		// @formatter:on
	}

	/**
	 * Verify that a GeneralObjectDatum entity, when serialized to SQS, is
	 * converted to a GeneralDatum.
	 */
	@Test
	public void slowStore_GeneralObjectDatum() throws Exception {
		// GIVEN
		collector.setReadConcurrency(0); // disable read thread

		// store datum (slowly)
		final GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(randomLong());
		d.setSourceId(randomString());
		d.setCreated(now().truncatedTo(ChronoUnit.MILLIS));
		d.setSamples(new DatumSamples(Map.of("a", 1), Map.of("b", 2), null));
		final DatumPK datumId = new ObjectDatumPK(d.getId().getKind(), d.getId().getObjectId(),
				d.getId().getSourceId(), d.getId().getTimestamp(), null);
		given(delegateDao.persist(same(d))).willAnswer(invocation -> {
			log.info("Sleeping on delegateDao.store() to simulate slow performance...");
			Thread.sleep(250);
			return datumId;
		});

		// overflow to SQS from timeout
		SendMessageResponse sendToSqsResponse = SendMessageResponse.builder().messageId(randomString())
				.build();
		CompletableFuture<SendMessageResponse> sendToSqsFuture = CompletableFuture
				.completedFuture(sendToSqsResponse);
		given(sqsClient.sendMessage(any(SendMessageRequest.class))).willReturn(sendToSqsFuture);

		// WHEN
		collector.serviceDidStartup();

		DatumPK result = collector.persist(d);

		Thread.sleep(400);

		collector.shutdownAndWait();

		// THEN
		// @formatter:off
		then(exceptionHandler).shouldHaveNoInteractions();

		then(delegateDao).should().persist(same(d));

		then(sqsClient).should().sendMessage(sendMessageRequestCaptor.capture());
		and.then(sendMessageRequestCaptor.getValue())
			.as("Sent datum to SQS because DAO store() took too long")
			.isNotNull()
			.as("SQS message is GeneralDatum JSON serialization of datum")
			.returns(sqsObjectMapper.writeValueAsString(new GeneralDatum(
					new DatumId(d.getId().getKind(),
							d.getId().getObjectId(),
							d.getId().getSourceId(),
							d.getId().getTimestamp()),
					d.getSamples())), from(SendMessageRequest::messageBody))
			;

		and.then(result)
			.as("Result provided")
			.isEqualTo(datumId)
			.as("Result not from DAO, as took too long")
			.isNotSameAs(datumId)
			;
		// @formatter:on
	}
}
