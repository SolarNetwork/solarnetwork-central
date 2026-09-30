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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.solarnetwork.domain.Unique;
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
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
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
 * {@link GenericWriteOnlyDao} (the delegate is presumed to actually persist the
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
 * @version 1.2
 */
public class SqsOverflowQueue<T, K>
		implements GenericWriteOnlyDao<T, K>, PingTest, ServiceLifecycleObserver {

	/** The {@code workItemMaxWaitMs} property default value. */
	public static final long DEFAULT_WORK_ITEM_MAX_WAIT_MS = 5000L;

	/**
	 * The {@code sqsSendMaxWaitMs} property default value.
	 *
	 * @since 1.2
	 */
	public static final long DEFAULT_SQS_SEND_MAX_WAIT_MS = 2_000L;

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

	/**
	 * The {@code pingTestTimeoutMs} property default value.
	 * 
	 * @since 1.1
	 */
	public static final long DEFAULT_PING_TEST_TIMEOUT_MS = 2_000L;

	/**
	 * The {@code shutdownWaitSecs} property default value.
	 *
	 * @since 1.2
	 */
	public static final int DEFAULT_SHUTDOWN_WAIT_SECS = 30;

	/**
	 * The work queue poll timeout, so writer threads notice a shutdown while
	 * draining.
	 */
	private static final long WRITER_QUEUE_POLL_MS = 1_000L;

	/** The maximum number of messages SQS allows in one receive request. */
	private static final int SQS_MAX_RECEIVE_MESSAGE_COUNT = 10;

	/** The maximum number of messages SQS allows in one delete request. */
	private static final int SQS_MAX_DELETE_BATCH_SIZE = 10;

	/** The minimum pause before re-checking a full work queue. */
	private static final long WORK_QUEUE_FULL_PAUSE_MS = 100L;

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
	private long sqsSendMaxWaitMs = DEFAULT_SQS_SEND_MAX_WAIT_MS;
	private int readConcurrency = DEFAULT_READ_CONCURRENCY;
	private int writeConcurrency = DEFAULT_WRITE_CONCURRENCY;
	private int readMaxMessageCount = DEFAULT_READ_MAX_MESSAGE_COUNT;
	private int readMaxWaitTimeSecs = DEFAULT_READ_MAX_WAIT_TIME_SECS;
	private long readSleepMinMs = DEFAULT_READ_SLEEP_MIN_MS;
	private long readSleepMaxMs = DEFAULT_READ_SLEEP_MAX_MS;
	private long readSleepThrottleStepMs = DEFAULT_READ_SLEEP_THROTTLE_STEP_MS;
	private int shutdownWaitSecs = DEFAULT_SHUTDOWN_WAIT_SECS;
	private @Nullable UncaughtExceptionHandler exceptionHandler;
	private String pingTestName = DEFAULT_PING_TEST_NAME;
	private long pingTestTimeoutMs = DEFAULT_PING_TEST_TIMEOUT_MS;
	private @Nullable Set<Class<? extends Throwable>> ignoredDaoExceptions;

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

		/**
		 * An overall count of objects whose persistence failure was ignored, per
		 * the configured {@code ignoredDaoExceptions}.
		 *
		 * @since 1.2
		 */
		ObjectsIgnored,

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
	 *        a blocking queue to buffer message handles for deletion; its
	 *        capacity is how many handles accumulate before a delete request is
	 *        sent, and a capacity above the SQS per-request maximum of 10 gains
	 *        nothing, as each request can delete at most that many
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
	 *
	 * <p>
	 * Delegates to {@link #shutdownAndWait()}, so work already accepted is
	 * given {@code shutdownWaitSecs} seconds to be persisted and anything left
	 * over is overflowed to SQS.
	 * </p>
	 */
	@Override
	public synchronized void serviceDidShutdown() {
		shutdownAndWait();
	}

	/**
	 * Stop accepting work and stop reading from SQS.
	 *
	 * <p>
	 * The writer threads are interrupted only to wake them from a queue poll so
	 * they re-check their loop condition: that condition keeps them draining
	 * whatever is already in the work queue, so entities that can still be
	 * persisted directly are not pushed to SQS.
	 * </p>
	 */
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
	}

	/**
	 * Shutdown and wait for all threads to finish.
	 *
	 * <p>
	 * Waits up to {@code shutdownWaitSecs} seconds in <b>total</b> for all
	 * threads to finish, then abandons any still running. A
	 * {@code shutdownWaitSecs} of {@literal 0} means do not wait at all.
	 * </p>
	 */
	public synchronized void shutdownAndWait() {
		doShutdown();
		final long expire = System.nanoTime() + TimeUnit.SECONDS.toNanos(shutdownWaitSecs);
		int abandoned = 0;
		if ( readerThreads != null ) {
			abandoned += joinAll(readerThreads, expire);
			readerThreads = null;
		}
		if ( writerThreads != null ) {
			abandoned += joinAll(writerThreads, expire);
			writerThreads = null;
		}
		if ( abandoned > 0 ) {
			log.warn("Abandoned {} thread(s) still running after waiting {}s for SQS queue [{}].",
					abandoned, shutdownWaitSecs, sqsQueueUrl);
		}
		flushSqsHandledMessages();
		drainWorkQueue(expire);
	}

	/**
	 * Overflow any work items left in the work queue to SQS.
	 *
	 * <p>
	 * Called once the writer threads have stopped, so that nothing accepted by
	 * {@link #persist(Object)} is dropped, and no caller is left waiting on a
	 * future that will never complete.
	 * </p>
	 *
	 * @param expire
	 *        the shutdown deadline, as a {@link System#nanoTime()} value
	 */
	private void drainWorkQueue(long expire) {
		final List<WorkItem<T, K>> remaining = new ArrayList<>(queue.size());
		queue.drainTo(remaining);
		if ( remaining.isEmpty() ) {
			return;
		}
		log.info("Overflowing {} work item(s) to SQS queue [{}] at shutdown.", remaining.size(),
				sqsQueueUrl);
		final List<CompletableFuture<K>> pending = new ArrayList<>(remaining.size());
		for ( WorkItem<T, K> item : remaining ) {
			stats.increment(BasicCount.WorkQueueRemovals, true);
			if ( item.future.isDone() ) {
				stats.increment(BasicCount.WorkQueueCancels, true);
				continue;
			}
			pending.add(sendToSqs(item.entity, item.future));
		}
		if ( pending.isEmpty() ) {
			return;
		}
		// wait for the sends to land, so nothing is lost if the process exits as
		// soon as shutdown returns
		final long remainingMs = Math.max(WRITER_QUEUE_POLL_MS,
				TimeUnit.NANOSECONDS.toMillis(expire - System.nanoTime()));
		try {
			CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(remainingMs,
					TimeUnit.MILLISECONDS);
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		} catch ( Exception e ) {
			log.warn("Error overflowing {} work item(s) to SQS queue [{}] at shutdown: {}",
					pending.size(), sqsQueueUrl, e.toString());
		}
	}

	/**
	 * Wait for threads to finish, up to a deadline shared by all of them.
	 *
	 * <p>
	 * Any thread still alive at the deadline is interrupted again and left to
	 * finish on its own.
	 * </p>
	 *
	 * @param threads
	 *        the threads to wait for
	 * @param expire
	 *        the deadline, as a {@link System#nanoTime()} value
	 * @return the number of threads still alive at the deadline
	 */
	private static int joinAll(List<? extends Thread> threads, long expire) {
		int alive = 0;
		for ( Thread t : threads ) {
			final long remainingMs = TimeUnit.NANOSECONDS.toMillis(expire - System.nanoTime());
			if ( remainingMs > 0 ) {
				try {
					// note join(0) would wait forever, so only called with a positive value
					t.join(remainingMs);
				} catch ( InterruptedException e ) {
					// restore the flag; subsequent join() calls then return immediately,
					// so the remaining threads are abandoned rather than waited on
					Thread.currentThread().interrupt();
				}
			}
			if ( t.isAlive() ) {
				alive++;
				t.interrupt();
			}
		}
		return alive;
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
		return pingTestTimeoutMs;
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
			}).get(Math.max(100, pingTestTimeoutMs - 100L), TimeUnit.MILLISECONDS);
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

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * An entity is never rejected or discarded here: if the work queue cannot
	 * take it, or this service is not running and so has no writer threads to
	 * drain the work queue, the entity is sent to the SQS queue instead, falling
	 * back to a direct DAO write if that fails.
	 * </p>
	 */
	@Override
	public @Nullable K persist(T entity) {
		stats.increment(BasicCount.ObjectsReceived);
		CompletableFuture<K> f = new CompletableFuture<>();
		if ( writeEnabled && queue.offer(new WorkItem<T, K>(entity, f)) ) {
			stats.increment(BasicCount.WorkQueueAdds);
			if ( workItemMaxWaitMs > 0 ) {
				// wait to complete within timeout, then send to SQS
				try {
					return f.get(workItemMaxWaitMs, TimeUnit.MILLISECONDS);
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
					var _ = f.cancel(false);
					throw persistException(e);
				} catch ( Exception e ) {
					if ( !f.cancel(false) && !f.isCompletedExceptionally() ) {
						// a writer completed it while timing out, so no need to pay for SQS
						return f.getNow(null);
					}
					f = sendToSqs(entity, new CompletableFuture<K>());
				}
			}
		} else {
			// the work queue is full, or has no writer threads draining it
			var _ = sendToSqs(entity, f);
		}
		try {
			return (sqsSendMaxWaitMs > 0 ? f.get(sqsSendMaxWaitMs, TimeUnit.MILLISECONDS) : f.get());
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw persistException(e);
		} catch ( Exception e ) {
			return persistAfterSqsFailure(entity, e);
		}
	}

	/**
	 * Last ditch attempt to persist an entity directly, after it could not be
	 * handed off to the SQS queue.
	 *
	 * <p>
	 * Runs on the calling thread, rather than on the SQS client thread that
	 * reports the failure, so that a slow delegate DAO cannot block the client's
	 * I/O threads.
	 * </p>
	 *
	 * @param entity
	 *        the entity to persist
	 * @param sqsException
	 *        the failure that prevented the entity reaching SQS
	 * @return the entity ID
	 * @throws RuntimeException
	 *         if the entity cannot be persisted either
	 */
	private @Nullable K persistAfterSqsFailure(T entity, Exception sqsException) {
		try {
			return persistEntityInternal(entity);
		} catch ( Exception e ) {
			if ( isIgnoredPersistException(entity, e) ) {
				stats.increment(BasicCount.ObjectsIgnored);
				return entityId(entity);
			}
			// give up
			stats.increment(BasicCount.ObjectsDiscarded);
			log.warn("Failed to persist [{}] after failing to send to SQS queue [{}]: {}", entity,
					sqsQueueUrl, e.toString(), e);
			throw persistException(sqsException);
		}
	}

	/**
	 * Unwrap an exception from waiting on a work item into one to throw.
	 *
	 * @param e
	 *        the exception
	 * @return the exception to throw, never {@code null}
	 */
	private static RuntimeException persistException(Exception e) {
		final Throwable cause = (e.getCause() != null ? e.getCause() : e);
		if ( cause instanceof RuntimeException re ) {
			return re;
		}
		return new RuntimeException(cause);
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
					sqsSendFailed(entity, f, ex);
				}
				return resp;
			});
		} catch ( Exception e ) {
			sqsSendFailed(entity, f, e);
		}
		return f;
	}

	/**
	 * Complete a work item future exceptionally after the entity could not be
	 * sent to the SQS queue.
	 *
	 * @param entity
	 *        the entity that could not be sent
	 * @param f
	 *        the future to complete
	 * @param ex
	 *        the failure
	 */
	private void sqsSendFailed(T entity, CompletableFuture<K> f, Throwable ex) {
		stats.increment(BasicCount.SqsQueueFail);
		final Throwable cause = (ex.getCause() != null ? ex.getCause() : ex);
		if ( cause instanceof AwsServiceException e ) {
			log.warn(
					"AWS error adding entity to SQS queue [{}]: {}; HTTP code {}; AWS code {}; request ID {}",
					sqsQueueUrl, e.getMessage(), e.statusCode(), e.awsErrorDetails().errorCode(),
					e.requestId());
		} else if ( cause instanceof SdkClientException e ) {
			log.warn("Error communicating with AWS SQS queue [{}]: {}", sqsQueueUrl, e.getMessage());
		} else {
			log.warn("Error adding entity to SQS queue [{}]: {}", sqsQueueUrl, cause.toString());
		}
		f.completeExceptionally(new RemoteServiceException("Error adding entity [%s] to SQS queue [%s]: %s"
				.formatted(entity, sqsQueueUrl, cause.toString()), cause));
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
		if ( rejected ) {
			// the pending queue would not take the handle, so send a batch including it
			sendDeleteBatch(receiptHandle);
		} else if ( force ) {
			// emptying the queue can take more than one batch; bound the loop so that a
			// delete failure re-queueing its handle cannot spin here
			int batches = (completedSqsMessageHandles.size() / SQS_MAX_DELETE_BATCH_SIZE) + 1;
			while ( batches-- > 0 && sendDeleteBatch(null) ) {
				// send another batch
			}
		}
	}

	/**
	 * Send one batch of completed message handles to be deleted from SQS.
	 *
	 * @param extraHandle
	 *        a handle to include that is not in the pending queue, or
	 *        {@code null} to send pending handles only
	 * @return {@literal true} if a request was sent
	 */
	private boolean sendDeleteBatch(final @Nullable String extraHandle) {
		final List<String> handleIds = new ArrayList<>(SQS_MAX_DELETE_BATCH_SIZE);
		completedSqsMessageHandles.drainTo(handleIds,
				SQS_MAX_DELETE_BATCH_SIZE - (extraHandle != null ? 1 : 0));
		if ( extraHandle != null ) {
			handleIds.add(extraHandle);
		}

		if ( handleIds.isEmpty() ) {
			return false;
		}

		log.debug("Deleting {} messages from SQS queue.", handleIds.size());

		Map<String, String> batchIdToReceiptHandles = new HashMap<>(SQS_MAX_DELETE_BATCH_SIZE);
		List<DeleteMessageBatchRequestEntry> entries = handleIds.stream().map(s -> {
			String id = UUID.randomUUID().toString();
			batchIdToReceiptHandles.put(id, s);
			return DeleteMessageBatchRequestEntry.builder().id(id).receiptHandle(s).build();
		}).toList();

		DeleteMessageBatchRequest deleteRequest = DeleteMessageBatchRequest.builder()
				.queueUrl(sqsQueueUrl).entries(entries).build();

		var _ = sqsClient.deleteMessageBatch(deleteRequest).handle((resp, ex) -> {
			if ( ex == null ) {
				if ( resp != null ) {
					if ( resp.hasFailed() ) {
						resp.failed().forEach(entry -> {
							final String handle = batchIdToReceiptHandles.get(entry.id());
							if ( handle == null ) {
								log.warn(
										"Unknown entry [{}] in SQS queue [{}] delete response, cannot retry: {} {}",
										entry.id(), sqsQueueUrl, entry.code(), entry.message());
							} else if ( Boolean.TRUE.equals(entry.senderFault()) ) {
								// a sender fault cannot succeed on retry, for example an
								// expired receipt handle; the message will be redelivered
								// and reprocessed instead
								log.warn(
										"Failed to delete message from SQS queue [{}], will not retry: {} {}",
										sqsQueueUrl, entry.code(), entry.message());
							} else {
								log.warn(
										"Failed to delete message from SQS queue [{}], will retry: {} {}",
										sqsQueueUrl, entry.code(), entry.message());
								sqsDeleteMessage(handle);
							}
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
		return true;
	}

	/**
	 * Get the approximate number of times a message has been received.
	 *
	 * @param msg
	 *        the message
	 * @return the approximate receive count, or {@literal 1} if not available
	 */
	private static long approximateReceiveCount(Message msg) {
		final String count = msg.attributes()
				.get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
		if ( count != null ) {
			try {
				return Long.parseLong(count);
			} catch ( NumberFormatException e ) {
				log.debug("Unparsable {} attribute on SQS message [{}]: {}",
						MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, msg.messageId(), count);
			}
		}
		return 1;
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
				final int capacity = queue.remainingCapacity();
				if ( capacity < 1 ) {
					// the work queue cannot accept anything, so do not pay to receive
					// messages that would only have to be returned to the queue
					log.debug("Work queue full, not reading from SQS queue [{}].", sqsQueueUrl);
					adjustThrottle(1.0);
					pause(Math.max(sleep, WORK_QUEUE_FULL_PAUSE_MS));
					continue;
				}
				// never request more than the work queue can take, nor more than SQS allows
				final int maxMessages = Math.min(Math.min(readMaxMessageCount, capacity),
						SQS_MAX_RECEIVE_MESSAGE_COUNT);
				// @formatter:off
				ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
							.queueUrl(sqsQueueUrl)
							.maxNumberOfMessages(maxMessages)
							.waitTimeSeconds(readMaxWaitTimeSecs)
							.messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
							.build();
				// @formatter:on
				try {
					ReceiveMessageResponse resp = sqsClient.receiveMessage(receiveMessageRequest).get();
					final List<Message> msgs = (resp.hasMessages() ? resp.messages() : List.of());
					final List<String> rejectedReceiptHandles = new ArrayList<>(msgs.size());
					if ( !msgs.isEmpty() ) {
						stats.increment(BasicCount.SqsQueueReceived, msgs.size());
						for ( Message msg : msgs ) {
							final T o;
							try {
								o = entityCodec.deserialize(msg.body());
							} catch ( Exception e ) {
								// this message can never be processed; leave it alone so the
								// queue visibility timeout paces the redelivery, and the queue
								// redrive policy moves it to the dead-letter queue. It will be
								// seen once per redelivery, so only count and log it in full
								// the first time.
								if ( approximateReceiveCount(msg) < 2 ) {
									stats.increment(BasicCount.ObjectsDiscarded);
									log.warn(
											"Discarding unparsable message [{}] from SQS queue [{}]: {}; body: {}",
											msg.messageId(), sqsQueueUrl, e.toString(), msg.body());
								} else {
									log.debug("Discarding unparsable message [{}] from SQS queue [{}]: {}",
											msg.messageId(), sqsQueueUrl, e.toString());
								}
								continue;
							}
							CompletableFuture<K> f = new CompletableFuture<>();
							if ( o != null && queue.offer(new WorkItem<T, K>(o, f)) ) {
								stats.increment(BasicCount.WorkQueueAdds);
								var _ = f.thenAccept(_ -> {
									sqsDeleteMessage(msg.receiptHandle());
								});
							} else {
								rejectedReceiptHandles.add(msg.receiptHandle());
							}
						}
					}
					adjustThrottle(msgs.isEmpty() ? 0.0
							: (double) rejectedReceiptHandles.size() / (double) msgs.size());
					if ( !rejectedReceiptHandles.isEmpty() ) {
						returnToQueue(rejectedReceiptHandles);
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
						// keep reading: an unexpected error here is a bug, and exiting would
						// leave the SQS queue undrained until the application restarts
						log.error("Unexpected error reading from SQS queue [{}]: {}", sqsQueueUrl,
								t.toString(), t);
					}
					adjustThrottle(1.0);
				}
				pause(sleep);
			}
			log.info("Reader thread exiting for SQS queue [{}]", sqsQueueUrl);
		}
	
		/**
		 * Adjust the read throttle.
		 *
		 * @param rejectedRatio
		 *        the proportion of received messages the work queue would not
		 *        accept, or {@literal 0} to relax the throttle
		 */
		private void adjustThrottle(double rejectedRatio) {
			if ( rejectedRatio > 0.0 ) {
				if ( sleep < readSleepMaxMs ) {
					sleep = Math.min(
							sleep + (long) Math.max(1.0, readSleepThrottleStepMs * rejectedRatio),
							readSleepMaxMs);
					log.info("Increased read throttle from SQS queue [{}] to {}ms.", sqsQueueUrl, sleep);
				}
			} else if ( sleep > readSleepMinMs ) {
				sleep = Math.max(sleep - readSleepThrottleStepMs, readSleepMinMs);
				log.info("Decreased read throttle from SQS queue [{}] to {}ms.", sqsQueueUrl, sleep);
			}
		}
	
		/**
		 * Pause before the next SQS receive request.
		 *
		 * @param ms
		 *        the time to pause, in milliseconds
		 */
		private void pause(long ms) {
			if ( writeEnabled && ms > 0 ) {
				try {
					Thread.sleep(ms);
				} catch ( InterruptedException e ) {
					// continue
				}
			}
		}
	
		/**
		 * Make messages the work queue could not accept visible again, after a
		 * backoff.
		 *
		 * @param receiptHandles
		 *        the receipt handles of the messages to return
		 */
		private void returnToQueue(List<String> receiptHandles) {
			// back off rather than un-hiding immediately: a zero visibility timeout
			// makes the message available again at once, costing a receive request per
			// retry for as long as the work queue stays full
			final int backoffSecs = (int) Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(sleep));
			var _ = sqsClient.changeMessageVisibilityBatch(req -> {
				List<ChangeMessageVisibilityBatchRequestEntry> entries = receiptHandles.stream()
							.map(handle -> ChangeMessageVisibilityBatchRequestEntry.builder()
									.id(UUID.randomUUID().toString()).receiptHandle(handle)
									.visibilityTimeout(backoffSecs).build())
							.toList();
				req.queueUrl(sqsQueueUrl).entries(entries);
			}).handle((resp, ex) -> {
				if ( ex == null ) {
					log.debug("Returned {} message(s) to SQS queue [{}] for retry in {}s.",
							receiptHandles.size(), sqsQueueUrl, backoffSecs);
				} else {
					final Throwable t = (ex.getCause() != null ? ex.getCause() : ex);
					log.warn("Failed to return {} message(s) to SQS queue [{}] for retry: {}",
							receiptHandles.size(), sqsQueueUrl, t.toString());
				}
				return resp;
			});
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
			// keep going while shutting down, until the work queue is drained, so
			// queued entities are persisted directly rather than pushed to SQS
			while ( writeEnabled || !queue.isEmpty() ) {
				final WorkItem<T, K> item;
				try {
					item = queue.poll(WRITER_QUEUE_POLL_MS, TimeUnit.MILLISECONDS);
				} catch ( InterruptedException e ) {
					continue;
				}
				if ( item == null ) {
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
					if ( isIgnoredPersistException(item.entity, t) ) {
						stats.increment(BasicCount.ObjectsIgnored);
						item.future.complete(entityId(item.entity));
					} else {
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
			}
			log.info("Writer thread exiting.");
		}

	}

	/**
	 * Test if an exception that occurred during persistence should be ignored.
	 *
	 * <p>
	 * An exception is ignored only if it is an instance of one of the
	 * configured {@link #getIgnoredDaoExceptions()}, in which case the entity is
	 * treated as if it had been persisted successfully.
	 * </p>
	 *
	 * @param entity
	 *        the entity being persisted
	 * @param t
	 *        the exception
	 * @return {@literal true} if {@code t} should be ignored
	 */
	private boolean isIgnoredPersistException(T entity, Throwable t) {
		final Set<Class<? extends Throwable>> ignored = getIgnoredDaoExceptions();
		if ( ignored == null ) {
			return false;
		}
		for ( Class<? extends Throwable> ignore : ignored ) {
			if ( ignore.isInstance(t) ) {
				log.debug("Ignoring exception storing entity {}: {}", entity, t.getMessage(), t);
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the ID of an entity, if the entity provides one.
	 *
	 * @param entity
	 *        the entity to get the ID for
	 * @return the entity ID, or {@code null} if the entity does not provide one
	 */
	private @Nullable K entityId(T entity) {
		if ( entity instanceof Unique<?> ) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Unique<K> unq = (Unique) entity;
			return unq.id();
		}
		return null;
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
	 *        the wait secs, or {@literal 0} to not wait at all; anything less
	 *        than {@literal 0} will be treated as {@literal 0}
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
	 * Get the maximum amount of time to wait for an entity to be handed off to
	 * the SQS queue.
	 *
	 * @return the maximum time, in milliseconds; defaults to
	 *         {@link #DEFAULT_SQS_SEND_MAX_WAIT_MS}
	 * @since 1.2
	 */
	public final long getSqsSendMaxWaitMs() {
		return sqsSendMaxWaitMs;
	}

	/**
	 * Set the maximum amount of time to wait for an entity to be handed off to
	 * the SQS queue.
	 *
	 * <p>
	 * Together with {@code workItemMaxWaitMs} this bounds the total time
	 * {@link #persist(Object)} can take. Anything less than {@literal 1} means
	 * wait indefinitely.
	 * </p>
	 *
	 * @param sqsSendMaxWaitMs
	 *        the maximum time to set, in milliseconds
	 * @since 1.2
	 */
	public final void setSqsSendMaxWaitMs(long sqsSendMaxWaitMs) {
		this.sqsSendMaxWaitMs = sqsSendMaxWaitMs;
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

	/**
	 * Get the ping test timeout, in milliseconds.
	 * 
	 * @return the timeout; defaults to {@link #DEFAULT_PING_TEST_TIMEOUT_MS}
	 * @since 1.1
	 */
	public final long getPingTestTimeoutMs() {
		return pingTestTimeoutMs;
	}

	/**
	 * Set the ping test timeout, in milliseconds.
	 * 
	 * @param pingTestTimeoutMs
	 *        the timeout to set
	 * @since 1.1
	 */
	public final void setPingTestTimeoutMs(long pingTestTimeoutMs) {
		this.pingTestTimeoutMs = pingTestTimeoutMs;
	}

	/**
	 * Get the collection of exceptions to ignore when persisting events.
	 * 
	 * @return the exceptions, or {@code null}
	 * @since 1.1
	 */
	public final @Nullable Set<Class<? extends Throwable>> getIgnoredDaoExceptions() {
		return ignoredDaoExceptions;
	}

	/**
	 * Set a collection of exceptions to ignore when persisting events.
	 * 
	 * @param ignoredDaoExceptions
	 *        the exceptions to set
	 * @since 1.1
	 */
	public final void setIgnoredDaoExceptions(
			@Nullable Set<Class<? extends Throwable>> ignoredDaoExceptions) {
		this.ignoredDaoExceptions = ignoredDaoExceptions;
	}

}
