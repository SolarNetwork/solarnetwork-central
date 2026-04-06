/* ==================================================================
 * SqsOverflowQueue.java - 18/03/2026 1:43:29 pm
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

package net.solarnetwork.central.common.biz.impl;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.support.EntityCodec;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Implementation of a message queue that uses a SQS queue for "overflow"
 * handling durability, delegating actual persistence to another
 * {@link GenericWriteOnlyDao} instance.
 *
 * <p>
 * The goal of this DAO is to persist entities directly into a delegate
 * {@link DatumWriteOnlyDao} (the delegate is presumed to actually persist the
 * entities) by way of a configurable number of "writer" threads. This limits
 * the number of entities being persisted concurrently. Datum to be persisted
 * are added to an internal work queue, which should be configured with a finite
 * size (like an {@link java.util.concurrent.ArrayBlockingQueue}). The "writer"
 * threads pull from this queue and persist the entities with the configured
 * delegate DAO.
 * </p>
 * <p>
 * If an entity cannot be added to the work queue, or does not get persisted
 * within {@code workItemMaxWaitMs}ms, then the entity "overflows" to a SQS
 * queue. A configurable number of "reader" threads poll for SQS messages, parse
 * them back into entities, and then attempt to persist each entity again. If
 * the SQS entity is successfully processed, its corresponding message is
 * deleted from the SQS queue.
 * </p>
 * <p>
 * This design is meant to prioritize saving entities directly, without added to
 * the SQS queue, for maximum performance. There is a small chance for data
 * loss, however, for entities added to the internal work queue but have not yet
 * been persisted and have not yet "overflowed" to SQS. Configuring a smaller
 * work queue and/or shorter {@code workItemMaxWaitMs} reduces the amount of
 * possible data loss, at the expense of an overall decrease in throughput.
 * </p>
 * <p>
 * This design also means some entities will be persisted multiple times. First
 * from the chance of a timeout while waiting for a entities that is actively
 * being persisted, and thus "overflows" to SQS even though the entities was
 * successfully persisted. Second, from the nature of SQS itself, which might
 * deliver the same message multiple times.
 * </p>
 *
 * @param <T>
 *        the message entity type
 * @param <K>
 *        the message entity key type
 * @author matt
 * @version 1.0
 */
public class SqsOverflowQueue<T, K>
		implements GenericWriteOnlyDao<T, K>, PingTest, ServiceLifecycleObserver {

	/** The {@code workItemMaxWaitMs} property default value. */
	public static final long DEFAULT_WORK_ITEM_MAX_WAIT_MS = 5000L;

	/** The {@code readConcurrency} property default value. */
	public static final int DEFAULT_READ_CONCURRENCY = 1;

	/** The {@code writeConcurrency} property default value. */
	public static final int DEFAULT_WRITE_CONCURRENCY = 2;

	/** The {@code readMaxMessageCount} property default value. */
	public static final int DEFAULT_READ_MAX_MESSAGE_COUNT = 10;

	/** The {@code readMaxWaitTimeSecs} property default value. */
	public static final int DEFAULT_READ_MAX_WAIT_TIME_SECS = 20;

	/** The {@code readSleepMinMs} property default value. */
	public static final long DEFAULT_READ_SLEEP_MIN_MS = 0L;

	/** The {@code readSlseepMaxMs} property default value. */
	public static final long DEFAULT_READ_SLEEP_MAX_MS = 30_000L;

	/** The {@code readSleepThrottleStepMs} property default value. */
	public static final long DEFAULT_READ_SLEEP_THROTTLE_STEP_MS = 1_000L;

	/**
	 * Ping test status property for a "duplicate" entities processing integer
	 * percent.
	 */
	public static final String OBJECTS_DUPLICATE_PERCENT_STATUS_PROP = "ObjectsDuplicatePercent";

	/** Ping test status property for the work queue available capacity. */
	public static final String WORK_QUEUE_AVAILABLE_CAPACITY_STATUS_PROP = "WorkQueueAvailableCapacity";

	/**
	 * Ping test status property for the approximate hidden message count in the
	 * SQS queue.
	 */
	public static final String SQS_QUEUE_PROCESSING_MESSAGE_COUNT_STATUS_PROP = "SqsQueueProcessingMessageCount";

	/**
	 * Ping test status property for the approximate message count in the SQS
	 * queue.
	 */
	public static final String SQS_QUEUE_MESSAGE_COUNT_STATUS_PROP = "SqsQueueMessageCount";

	/** The {@code pingTestName} property default value. */
	public static final String DEFAULT_PING_TEST_NAME = "SQS Overflow Queue";

	private static final Logger log = LoggerFactory.getLogger(SqsOverflowQueue.class);

	private static final AtomicInteger READER_COUNTER = new AtomicInteger(0);
	private static final AtomicInteger WRITER_COUNTER = new AtomicInteger(0);

	private final StatTracker stats;
	private final String identity;
	private final SqsAsyncClient sqsClient;
	private final String sqsQueueUrl;
	private final BlockingQueue<WorkItem<T, K>> queue;
	private final GenericWriteOnlyDao<T, K> dao;
	private final EntityCodec<T, K, String> entityCodec;

	private final BlockingQueue<String> completedSqsMessageHandles;

	private long workItemMaxWaitMs = DEFAULT_WORK_ITEM_MAX_WAIT_MS;
	private int readConcurrency = DEFAULT_READ_CONCURRENCY;
	private int writeConcurrency = DEFAULT_WRITE_CONCURRENCY;
	private int readMaxMessageCount = DEFAULT_READ_MAX_MESSAGE_COUNT;
	private int readMaxWaitTimeSecs = DEFAULT_READ_MAX_WAIT_TIME_SECS;
	private long readSleepMinMs = DEFAULT_READ_SLEEP_MIN_MS;
	private long readSleepMaxMs = DEFAULT_READ_SLEEP_MAX_MS;
	private long readSleepThrottleStepMs = DEFAULT_READ_SLEEP_THROTTLE_STEP_MS;
	private int shutdownWaitSecs;
	private @Nullable UncaughtExceptionHandler exceptionHandler;
	private String pingTestName = DEFAULT_PING_TEST_NAME;

	private @Nullable List<DaoWriterThread> writerThreads;
	private @Nullable List<QueueReaderThread> readerThreads;
	private volatile boolean writeEnabled = false;

	/** Basic counted fields. */
	public enum BasicCount {

		/** An overall count of objects received. */
		ObjectsReceived,

		/** An overall count of objects persisted. */
		ObjectsStored,

		/** An overall count of objects that failed to be persisted. */
		ObjectsFailed,

		/** An overall count of objects that failed to be processed at all. */
		ObjectsDiscarded,

		/** SQS queue message additions. */
		SqsQueueAdds,

		/** SQS queue message addition failures. */
		SqsQueueFail,

		/** SQS queue messages received by readers. */
		SqsQueueReceived,

		/** SQS queue message removals. */
		SqsQueueRemovals,

		/** Work queue additions. */
		WorkQueueAdds,

		/** Work queue removals. */
		WorkQueueRemovals,

		/** Work queue removals found to be cancelled. */
		WorkQueueCancels,

		;

	}

	/**
	 * A temporary work item.
	 */
	public record WorkItem<T, K>(T entity, CompletableFuture<K> future) {

	}

	/**
	 * Constructor.
	 *
	 * @param identity
	 *        the service identity (e.g. ping test ID)
	 * @param sqsClient
	 *        the SQS client
	 * @param sqsQueueUrl
	 *        the SQS queue URL to use for messages
	 * @param queue
	 *        the temporary queue to use
	 * @param dao
	 *        the delegate DAO
	 * @param entityCodec
	 *        the service to serialize/deserialize entities to/from SQS messages
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SqsOverflowQueue(String identity, SqsAsyncClient sqsClient, String sqsQueueUrl,
			BlockingQueue<WorkItem<T, K>> queue, GenericWriteOnlyDao<T, K> dao,
			EntityCodec<T, K, String> entityCodec) {
		this(new StatTracker("SqsDatumCollector", null, log, 200), identity, sqsClient, sqsQueueUrl,
				queue, new LinkedHashSetBlockingQueue<>(9), dao, entityCodec);
	}

	/**
	 * Constructor.
	 *
	 * @param stats
	 *        the stats to use
	 * @param identity
	 *        the service identity (e.g. ping test ID)
	 * @param sqsClient
	 *        the SQS client
	 * @param sqsQueueUrl
	 *        the SQS queue URL to use for messages
	 * @param queue
	 *        the temporary queue to use
	 * @param completedSqsMessageHandles
	 *        a blocking queue to buffer message handles for deletion; the queue
	 *        size should be no more than the maximum allowed in a single SQS
	 *        delete request (10)
	 * @param dao
	 *        the delegate DAO
	 * @param entityCodec
	 *        the service to serialize/deserialize entities to/from SQS messages
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SqsOverflowQueue(StatTracker stats, String identity, SqsAsyncClient sqsClient,
			String sqsQueueUrl, BlockingQueue<WorkItem<T, K>> queue,
			BlockingQueue<String> completedSqsMessageHandles, GenericWriteOnlyDao<T, K> dao,
			EntityCodec<T, K, String> entityCodec) {
		super();
		this.stats = requireNonNullArgument(stats, "stats");
		this.identity = requireNonNullArgument(identity, "identity");
		this.sqsClient = requireNonNullArgument(sqsClient, "sqsClient");
		this.sqsQueueUrl = requireNonNullArgument(sqsQueueUrl, "sqsQueueUrl");
		this.queue = requireNonNullArgument(queue, "queue");
		this.completedSqsMessageHandles = requireNonNullArgument(completedSqsMessageHandles,
				"completedSqsMessageHandles");
		this.dao = requireNonNullArgument(dao, "dao");
		this.entityCodec = requireNonNullArgument(entityCodec, "entityCodec");
	}

	/**
	 * Call after configured to start up processing.
	 */
	@Override
	public synchronized void serviceDidStartup() {
		final int writeThreadCount = getWriteConcurrency();
		final UncaughtExceptionHandler exHandler = getExceptionHandler();
		if ( writerThreads != null || readerThreads != null ) {
			serviceDidShutdown();
		}
		writeEnabled = true;
		writerThreads = new ArrayList<>(writeThreadCount);
		for ( int i = 0; i < writeThreadCount; i++ ) {
			var thread = new DaoWriterThread();
			if ( exHandler != null ) {
				thread.setUncaughtExceptionHandler(exHandler);
			}
			writerThreads.add(thread);
			thread.start();
		}

		final int readThreadCount = getReadConcurrency();
		readerThreads = new ArrayList<>(readThreadCount);
		for ( int i = 0; i < readThreadCount; i++ ) {
			var thread = new QueueReaderThread();
			if ( exHandler != null ) {
				thread.setUncaughtExceptionHandler(exHandler);
			}
			readerThreads.add(thread);
			thread.start();
		}
	}

	/**
	 * Call when no longer needed.
	 */
	@Override
	public synchronized void serviceDidShutdown() {
		doShutdown();
		readerThreads = null;
		writerThreads = null;
	}

	private void doShutdown() {
		writeEnabled = false;
		if ( readerThreads != null ) {
			for ( QueueReaderThread t : readerThreads ) {
				t.interrupt();
			}
		}
		if ( writerThreads != null ) {
			for ( DaoWriterThread t : writerThreads ) {
				t.interrupt();
			}
		}
		flushSqsHandledMessages();
	}

	/**
	 * Shutdown and wait for all threads to finish.
	 */
	public synchronized void shutdownAndWait() {
		doShutdown();
		if ( readerThreads != null ) {
			for ( QueueReaderThread t : readerThreads ) {
				try {
					t.join(TimeUnit.SECONDS.toMillis(shutdownWaitSecs));
				} catch ( InterruptedException e ) {
					// ignore
				}
				if ( t.isAlive() ) {
					t.interrupt();
				}
			}
			readerThreads = null;
		}
		if ( writerThreads != null ) {
			for ( DaoWriterThread t : writerThreads ) {
				try {
					t.join(TimeUnit.SECONDS.toMillis(shutdownWaitSecs));
				} catch ( InterruptedException e ) {
					// ignore
				}
				if ( t.isAlive() ) {
					t.interrupt();
				}
			}
			writerThreads = null;
		}
	}

	@Override
	public String getPingTestId() {
		return identity;
	}

	@Override
	public String getPingTestName() {
		return pingTestName;
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public Result performPingTest() throws Exception {
		// test SQS connectivity by getting queue URL
		boolean sqsConnected = false;
		String msgCount = null;
		String msgHiddenCount = null;
		try {
			GetQueueAttributesResponse resp = sqsClient.getQueueAttributes(req -> {
				req.queueUrl(sqsQueueUrl).attributeNames(
						QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
						QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE);
			}).get(900L, TimeUnit.MILLISECONDS);
			sqsConnected = true;
			msgCount = resp.attributesAsStrings()
					.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES.toString());
			msgHiddenCount = resp.attributesAsStrings()
					.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE.toString());
		} catch ( Exception e ) {
			Throwable t = e.getCause();
			log.warn("Failed to request SQS queue [{}] attributes: {}", sqsQueueUrl,
					(t != null ? t.toString() : e.toString()));
		}

		long recvCount = stats.get(BasicCount.ObjectsReceived);
		long storCount = stats.get(BasicCount.ObjectsStored);
		Map<String, Number> statMap = new TreeMap<>();
		for ( var e : stats.allStatistics().entrySet() ) {
			statMap.put(e.getKey(), e.getValue());
		}
		statMap.put(OBJECTS_DUPLICATE_PERCENT_STATUS_PROP,
				storCount > recvCount
						? (recvCount > 0
								? (int) Math.round(100 * (double) (storCount - recvCount) / recvCount)
								: 100)
						: 0);
		statMap.put(WORK_QUEUE_AVAILABLE_CAPACITY_STATUS_PROP, queue.remainingCapacity());
		if ( msgCount != null ) {
			statMap.put(SQS_QUEUE_MESSAGE_COUNT_STATUS_PROP, new BigInteger(msgCount));
		}
		if ( msgHiddenCount != null ) {
			statMap.put(SQS_QUEUE_PROCESSING_MESSAGE_COUNT_STATUS_PROP, new BigInteger(msgHiddenCount));
		}
		if ( !sqsConnected ) {
			return new PingTestResult(false, "SQS connection failed.", statMap);
		}
		final List<DaoWriterThread> writers = this.writerThreads;
		final List<QueueReaderThread> readers = this.readerThreads;
		int writersAlive = 0;
		int readersAlive = 0;
		if ( writeEnabled ) {
			if ( writers != null ) {
				for ( DaoWriterThread t : writers ) {
					if ( t.isAlive() ) {
						writersAlive++;
					}
				}
			}
			if ( readers != null ) {
				for ( QueueReaderThread t : readers ) {
					if ( t.isAlive() ) {
						readersAlive++;
					}
				}
			}
			if ( (writers != null && writersAlive < writers.size())
					|| (readers != null && readersAlive < readers.size()) ) {
				return new PingTestResult(false,
						String.format("Not all threads running: %d/%d writers, %d/%d readers.",
								writersAlive, (writers != null ? writers.size() : 0), readersAlive,
								(readers != null ? readers.size() : 0)),
						statMap);
			}
		}
		return new PingTestResult(true,
				String.format("Processed %d entities using %d writers, %d readers.", recvCount,
						writers != null ? writers.size() : 0, readers != null ? readers.size() : 0),
				statMap);
	}

	/**
	 * Force all pending handled messages to be deleted from SQS.
	 *
	 * @see #sqsDeleteMessage(String, boolean)
	 */
	private void flushSqsHandledMessages() {
		sqsDeleteMessage(null, true);
	}

	/**
	 * Delete an SQS message.
	 *
	 * @param receiptHandle
	 *        the SQS message receipt handle to delete
	 * @see #sqsDeleteMessage(String, boolean)
	 */
	private void sqsDeleteMessage(final String receiptHandle) {
		assert receiptHandle != null;
		sqsDeleteMessage(receiptHandle, false);
	}

	@Override
	public @Nullable K persist(T entity) {
		stats.increment(BasicCount.ObjectsReceived);
		CompletableFuture<K> f = new CompletableFuture<>();
		if ( queue.offer(new WorkItem<T, K>(entity, f)) ) {
			stats.increment(BasicCount.WorkQueueAdds);
			if ( workItemMaxWaitMs > 0 ) {
				// wait to complete within timeout, then send to SQS
				try {
					return f.get(workItemMaxWaitMs, TimeUnit.MILLISECONDS);
				} catch ( Exception e ) {
					f.cancel(false);
					f = sendToSqs(entity, new CompletableFuture<K>());
				}
			}
		} else {
			var _ = sendToSqs(entity, f);
		}
		try {
			return f.get();
		} catch ( Exception e ) {
			Throwable cause = e.getCause();
			if ( cause instanceof RuntimeException re ) {
				throw re;
			}
			throw new RuntimeException(cause);
		}
	}

	private CompletableFuture<K> sendToSqs(T entity, CompletableFuture<K> f) {
		try {
			final String json = entityCodec.serialize(entity);
			final var sendMsgRequest = SendMessageRequest.builder().queueUrl(sqsQueueUrl)
					.messageBody(json).build();

			var _ = sqsClient.sendMessage(sendMsgRequest).handle((resp, ex) -> {
				if ( ex == null ) {
					stats.increment(BasicCount.SqsQueueAdds);
					f.complete(entityCodec.entityId(entity));
				} else {
					if ( ex.getCause() instanceof AwsServiceException e ) {
						log.warn("AWS error: {}; HTTP code {}; AWS code {}; request ID {}",
								e.getMessage(), e.statusCode(), e.awsErrorDetails().errorCode(),
								e.requestId());
						f.completeExceptionally(new RemoteServiceException(
								"Error adding entities [%s] to SQS queue [%s]: %s".formatted(entity,
										sqsQueueUrl, e.toString()),
								e));
					} else if ( ex.getCause() instanceof SdkClientException e ) {
						log.warn("Error communicating with AWS: {}", e.getMessage());
						f.completeExceptionally(new RemoteServiceException(
								"Error adding entities [%s] to SQS queue [%s]: %s".formatted(entity,
										sqsQueueUrl, e.toString()),
								e));
					} else {
						f.completeExceptionally(new RemoteServiceException(
								"Error adding entities [%s] to SQS queue [%s]: %s".formatted(entity,
										sqsQueueUrl, ex.toString()),
								ex));
					}
				}
				return resp;
			});
		} catch ( Exception e ) {
			stats.increment(BasicCount.SqsQueueFail);
			// last ditch: write directly to DAO
			try {
				var id = persistEntityInternal(entity);
				f.complete(id);
			} catch ( Exception e2 ) {
				// give up
				stats.increment(BasicCount.ObjectsDiscarded);
				log.warn("Failed to persist [{}] after failing to send to SQS queue: {}", entity, e2,
						e2);
				f.completeExceptionally(e);
			}
		}
		return f;
	}

	private @Nullable K persistEntityInternal(T entity) {
		final K id = dao.persist(entity);
		stats.increment(BasicCount.ObjectsStored);
		return id;
	}

	/**
	 * Handle a completed message by deleting from SQS.
	 *
	 * <p>
	 * Once completed (persisted) the SQS message should be deleted. This method
	 * collects the provided {@code receiptHandle} values into batches to
	 * improve throughput. Pass {@code true} for the {@code force} argument to
	 * flush all pending deletes.
	 * </p>
	 *
	 * @param receiptHandle
	 *        the message receipt handle that was completed, or {@code null} if
	 *        forcing a request
	 * @param force
	 *        {@code true} to force the deletion of all pending handles
	 */
	private void sqsDeleteMessage(final @Nullable String receiptHandle, final boolean force) {
		final boolean rejected = (receiptHandle != null
				? !completedSqsMessageHandles.offer(receiptHandle)
				: false);
		if ( rejected || force ) {
			List<String> handleIds = new ArrayList<>(10);
			completedSqsMessageHandles.drainTo(handleIds, 9);
			if ( rejected && receiptHandle != null ) {
				handleIds.add(receiptHandle);
			}

			if ( handleIds.isEmpty() ) {
				return;
			}

			log.debug("Deleting {} messages from SQS queue.", handleIds.size());

			Map<String, String> batchIdToReceiptHandlers = new HashMap<>(10);
			List<DeleteMessageBatchRequestEntry> entries = handleIds.stream().map(s -> {
				String id = UUID.randomUUID().toString();
				batchIdToReceiptHandlers.put(id, receiptHandle);
				return DeleteMessageBatchRequestEntry.builder().id(id).receiptHandle(s).build();
			}).toList();

			DeleteMessageBatchRequest deleteRequest = DeleteMessageBatchRequest.builder()
					.queueUrl(sqsQueueUrl).entries(entries).build();

			var _ = sqsClient.deleteMessageBatch(deleteRequest).handle((resp, ex) -> {
				if ( ex == null ) {
					if ( resp != null ) {
						if ( resp.hasFailed() ) {
							resp.failed().forEach(entry -> {
								String handleId = nonnull(batchIdToReceiptHandlers.get(entry.id()),
										"Batch handleId");
								log.warn(
										"Failed to delete message from SQS queue (will retry): {}; receiptHandle: {}",
										entry.message(), handleId);
								sqsDeleteMessage(handleId);
							});
						}
						if ( resp.hasSuccessful() ) {
							stats.increment(BasicCount.SqsQueueRemovals, resp.successful().size());
						}
					}
				} else if ( ex.getCause() instanceof AwsServiceException e ) {
					log.warn(
							"AWS error deleting entities from SQS queue [{}]: {}; HTTP code {}; AWS code {}; request ID {}",
							sqsQueueUrl, e.getMessage(), e.statusCode(), e.awsErrorDetails().errorCode(),
							e.requestId());
				} else if ( ex.getCause() instanceof SdkClientException e ) {
					log.warn("Error communicating with AWS SQS queue [{}]: {}", sqsQueueUrl,
							e.getMessage());
				} else {
					log.warn("Error deleting entities from from SQS queue [{}]: {}", sqsQueueUrl,
							ex.toString());
				}
				return resp;
			});
		}
	}

	/**
	 * Thread for long-polling the SQS queue for messages to persist.
	 */
	private final class QueueReaderThread extends Thread {

		private long sleep = readSleepMinMs;

		private QueueReaderThread() {
			super("%s-QueueReader-%d".formatted(identity, READER_COUNTER.incrementAndGet()));
		}

		@Override
		public void run() {
			while ( writeEnabled ) {
				// @formatter:off
				ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
						.queueUrl(sqsQueueUrl)
						.maxNumberOfMessages(readMaxMessageCount)
						.waitTimeSeconds(readMaxWaitTimeSecs)
						.build();
				// @formatter:on
				try {
					ReceiveMessageResponse resp = sqsClient.receiveMessage(receiveMessageRequest).get();
					if ( resp.hasMessages() ) {
						List<Message> msgs = resp.messages();
						stats.increment(BasicCount.SqsQueueReceived, msgs.size());
						int accepted = 0;
						List<String> rejectedReceiptHandles = new ArrayList<>(msgs.size());
						try {
							for ( Message msg : msgs ) {
								T o = entityCodec.deserialize(msg.body());
								CompletableFuture<K> f = new CompletableFuture<>();
								if ( o != null && queue.offer(new WorkItem<T, K>(o, f)) ) {
									stats.increment(BasicCount.WorkQueueAdds);
									var _ = f.thenAccept(_ -> {
										sqsDeleteMessage(msg.receiptHandle());
									});
									accepted++;
								} else {
									// adjust visibility to 0 to allow reprocessing
									rejectedReceiptHandles.add(msg.receiptHandle());
								}
							}
						} finally {
							if ( !rejectedReceiptHandles.isEmpty() ) {
								var _ = sqsClient.changeMessageVisibilityBatch(changeVizReq -> {
									List<ChangeMessageVisibilityBatchRequestEntry> entries = rejectedReceiptHandles
											.stream().map(id -> {
												return ChangeMessageVisibilityBatchRequestEntry.builder()
														.id(UUID.randomUUID().toString())
														.receiptHandle(id).visibilityTimeout(0).build();
											}).toList();
									changeVizReq.queueUrl(sqsQueueUrl).entries(entries);
								}).handle((changeVizResp, changeVizEx) -> {
									if ( changeVizEx == null ) {
										log.debug(
												"Un-hid {} messages received from SQS queue but rejected by work queue.",
												rejectedReceiptHandles.size());
									} else {
										Throwable t = changeVizEx.getCause();
										log.warn(
												"Failed to un-hide {} messages received from SQS queue but rejected by work queue: {}",
												rejectedReceiptHandles.size(),
												(t != null ? t.toString() : changeVizEx.toString()));
									}
									return changeVizResp;
								});
							}
							int rejected = msgs.size() - accepted;
							double rejectedRatio = (rejected > 0
									? ((double) rejected / (double) msgs.size())
									: 0.0);
							if ( rejected > 0 && sleep < readSleepMaxMs ) {
								sleep = Math.min(
										sleep + (long) (readSleepThrottleStepMs * rejectedRatio),
										readSleepMaxMs);
								log.info(
										"Increased read throttle from SQS queue to {}ms after {} work queue rejections.",
										sleep, rejected);
							} else if ( rejected < 1 && sleep > readSleepMinMs ) {
								sleep = Math.max(sleep - readSleepThrottleStepMs, readSleepMinMs);
								log.info(
										"Decreased read throttle from SQS queue to {}ms after all {} work queue items accepted.",
										sleep, accepted);
							}
						}
					}
				} catch ( Exception ex ) {
					final Throwable t = (ex.getCause() != null ? ex.getCause() : ex);
					if ( t instanceof AwsServiceException e ) {
						log.warn(
								"AWS error processing SQS queue [{}]: {}; HTTP code {}; AWS code {}; request ID {}",
								sqsQueueUrl, e.getMessage(), e.statusCode(),
								e.awsErrorDetails().errorCode(), e.requestId());
						HttpStatus status = HttpStatus.resolve(e.statusCode());
						if ( status != null && status.is4xxClientError() ) {
							log.error(
									"Fatal configuration error with entities collector SQS queue [{}]: {}",
									sqsQueueUrl, e.getMessage());
							return;
						}
					} else if ( t instanceof SdkClientException e ) {
						log.warn("Error communicating with AWS SQS queue [{}]: {}", sqsQueueUrl,
								e.getMessage());
					} else if ( !(t instanceof InterruptedException) ) {
						log.error("Fatal error in entity collector SQS queue [{}]: {}", sqsQueueUrl,
								t.toString(), t);
						return;
					}
					if ( sleep < readSleepMaxMs ) {
						sleep = Math.min(sleep + readSleepThrottleStepMs, readSleepMaxMs);
						log.info("Increased read throttle from SQS queue to {}ms after exception: {}",
								sleep, t.getMessage());
					}
				}
				if ( writeEnabled && sleep > 0 ) {
					try {
						Thread.sleep(sleep);
					} catch ( InterruptedException e ) {
						// continue
					}
				}
			}
			log.info("Reader thread exiting for SQS queue [{}]", sqsQueueUrl);
		}
	}

	/**
	 * Thread for persisting entities into database.
	 */
	private final class DaoWriterThread extends Thread {

		private DaoWriterThread() {
			super("%s-DaoWriter-%d".formatted(identity, WRITER_COUNTER.incrementAndGet()));
		}

		@Override
		public void run() {
			while ( writeEnabled ) {
				final WorkItem<T, K> item;
				try {
					item = queue.take();
				} catch ( InterruptedException e ) {
					continue;
				}
				stats.increment(BasicCount.WorkQueueRemovals, true);

				if ( item.future.isDone() ) {
					stats.increment(BasicCount.WorkQueueCancels, true);
					continue;
				}
				try {
					var id = persistEntityInternal(item.entity);
					item.future.complete(id);
				} catch ( Throwable t ) {
					stats.increment(BasicCount.ObjectsFailed);
					log.warn("Error storing entity {}: {}", item.entity, t.getMessage(), t);
					UncaughtExceptionHandler exHandler = getUncaughtExceptionHandler();
					if ( exHandler != null ) {
						try {
							exHandler.uncaughtException(this, t);
						} catch ( Exception e ) {
							log.error(
									"Exception handler [{}] threw exception after error storing entity {}",
									exHandler, item.entity, e);
						}
					}
					item.future.completeExceptionally(t);
				}
			}
			log.info("Writer thread exiting.");
		}

	}

	/**
	 * Get the number of reader threads to use.
	 *
	 * @return the number of reader threads; defaults to
	 *         {@link #DEFAULT_READ_CONCURRENCY}
	 */
	public final int getReadConcurrency() {
		return readConcurrency;
	}

	/**
	 * Set the number of reader threads to use.
	 *
	 * @param readConcurrency
	 *        the number of reader threads, or {@code 0} to disable reading;
	 *        anything less than {@literal 0} will be treated as {@literal 0}
	 */
	public final void setReadConcurrency(int readConcurrency) {
		if ( readConcurrency < 0 ) {
			readConcurrency = 0;
		}
		this.readConcurrency = readConcurrency;
	}

	/**
	 * Get the number of writer threads to use.
	 *
	 * @return the number of writer threads; defaults to
	 *         {@link #DEFAULT_WRITE_CONCURRENCY}
	 */
	public final int getWriteConcurrency() {
		return writeConcurrency;
	}

	/**
	 * Set the number of writer threads to use.
	 *
	 * @param writeConcurrency
	 *        the number of writer threads; anything less than {@literal 1} will
	 *        be treated as {@literal 1}
	 */
	public final void setWriteConcurrency(int writeConcurrency) {
		if ( writeConcurrency < 1 ) {
			writeConcurrency = 1;
		}
		this.writeConcurrency = writeConcurrency;
	}

	/**
	 * Get an exception handler for the background threads.
	 *
	 * @return the configured handler
	 */
	public final @Nullable UncaughtExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	/**
	 * Set an exception handler for the background threads.
	 *
	 * @param exceptionHandler
	 *        the handler to use
	 */
	public final void setExceptionHandler(@Nullable UncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Get the maximum number of seconds to wait for threads to finish during
	 * shutdown.
	 *
	 * @return the wait secs
	 */
	public final int getShutdownWaitSecs() {
		return shutdownWaitSecs;
	}

	/**
	 * Set the maximum number of seconds to wait for threads to finish during
	 * shutdown.
	 *
	 * @param shutdownWaitSecs
	 *        the wait secs; anything less than {@literal 0} will be treated as
	 *        {@literal 0}
	 */
	public final void setShutdownWaitSecs(int shutdownWaitSecs) {
		if ( shutdownWaitSecs < 0 ) {
			shutdownWaitSecs = 0;
		}
		this.shutdownWaitSecs = shutdownWaitSecs;
	}

	/**
	 * Get the maximum amount of time to wait for a work item to be processed.
	 *
	 * @return the maximum time, in milliseconds
	 */
	public final long getWorkItemMaxWaitMs() {
		return workItemMaxWaitMs;
	}

	/**
	 * Set the maximum amount of time to wait for a work item to be processed.
	 *
	 * @param workItemMaxWaitMs
	 *        the maximum time to set, in milliseconds
	 */
	public final void setWorkItemMaxWaitMs(long workItemMaxWaitMs) {
		this.workItemMaxWaitMs = workItemMaxWaitMs;
	}

	/**
	 * Get the maximum number of SQS messages to read per request.
	 *
	 * @return the count; defaults to {@link #DEFAULT_READ_MAX_MESSAGE_COUNT}
	 */
	public final int getReadMaxMessageCount() {
		return readMaxMessageCount;
	}

	/**
	 * Set the maximum number of SQS messages to read per request.
	 *
	 * @param readMaxMessageCount
	 *        the count to set; see AWS documentation for valid range (e.g.
	 *        1-10)
	 */
	public final void setReadMaxMessageCount(int readMaxMessageCount) {
		this.readMaxMessageCount = readMaxMessageCount;
	}

	/**
	 * Get the maximum SQS receive wait time, in seconds.
	 *
	 * @return the seconds; defaults to {@link #DEFAULT_READ_MAX_WAIT_TIME_SECS}
	 */
	public final int getReadMaxWaitTimeSecs() {
		return readMaxWaitTimeSecs;
	}

	/**
	 * Set the maximum SQS receive wait time, in seconds.
	 *
	 * @param readMaxWaitTimeSecs
	 *        the seconds to set; see AWS documentation for valid range (e.g.
	 *        1-20)
	 */
	public final void setReadMaxWaitTimeSecs(int readMaxWaitTimeSecs) {
		this.readMaxWaitTimeSecs = readMaxWaitTimeSecs;
	}

	/**
	 * Get the minimum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @return the minimum sleep amount, in milliseconds
	 */
	public final long getReadSleepMinMs() {
		return readSleepMinMs;
	}

	/**
	 * Set the minimum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @param readSleepMinMs
	 *        the minimum sleep amount to set, in milliseconds
	 */
	public final void setReadSleepMinMs(long readSleepMinMs) {
		this.readSleepMinMs = readSleepMinMs;
	}

	/**
	 * Get the maximum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @return the minimum sleep amount, in milliseconds
	 */
	public final long getReadSleepMaxMs() {
		return readSleepMaxMs;
	}

	/**
	 * Set the maximum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @param readSleepMaxMs
	 *        the maximum sleep amount to set, in milliseconds
	 */
	public final void setReadSleepMaxMs(long readSleepMaxMs) {
		this.readSleepMaxMs = readSleepMaxMs;
	}

	/**
	 * Get the amount of time to increase pausing after SQS receive requests for
	 * each received message that is rejected from the work queue, or to
	 * decrease after successfully offering all messages to the work queue.
	 *
	 * @return the step amount, in milliseconds; defaults to
	 *         {@link #DEFAULT_READ_SLEEP_THROTTLE_STEP_MS}
	 */
	public final long getReadSleepThrottleStepMs() {
		return readSleepThrottleStepMs;
	}

	/**
	 * Set the amount of time to increase pausing after SQS receive requests for
	 * each received message that is rejected from the work queue, or to
	 * decrease after successfully offering all messages to the work queue.
	 *
	 * <p>
	 * This amount of time is used to slow down or speed up the reading of
	 * messages from SQS. If messages are being read but when offered to the
	 * work queue are rejected (because the queue is full) then this amount will
	 * be <b>added</b> to the "sleep" time enforced before requesting more
	 * messages from SQS. Conversely, if all messages received from SQS in a
	 * single request are accepted into the work queue, this amount will be
	 * <b>subtracted</b> to the "sleep" time.
	 * </p>
	 *
	 * @param readSleepThrottleStepMs
	 *        the step amount to set, in milliseconds
	 */
	public final void setReadSleepThrottleStepMs(long readSleepThrottleStepMs) {
		this.readSleepThrottleStepMs = readSleepThrottleStepMs;
	}

	/**
	 * Set the ping test name.
	 *
	 * @param pingTestName
	 *        the name to set; if {@code null} then
	 *        {@link #DEFAULT_PING_TEST_NAME} will be used
	 */
	public final void setPingTestName(String pingTestName) {
		this.pingTestName = (pingTestName != null ? pingTestName : DEFAULT_PING_TEST_NAME);
	}

}
