/* ==================================================================
 * AsyncDatumCollector.java - 25/03/2020 10:43:56 am
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

package net.solarnetwork.central.datum.support;

import static net.solarnetwork.central.datum.support.DatumUtils.convertGeneralDatum;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.FactoryBuilder.SingletonFactory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumPK;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;

/**
 * Data collector that processes datum and location datum asynchronously.
 *
 * <p>
 * This service works through the {@link Cache} API of datum objects, by
 * listening for cache creation events to add those datum to an internal
 * persistence queue for eventual storage via the {@link DatumWriteOnlyDao} API.
 * Once persisted there, the datum cache entry is removed. By configuring the
 * {@link Cache} with non-volatile persistence as well, the cache acts as a
 * write-ahead-log cache of datum updates. The internal persistence queue serves
 * as a limiter to avoid overwhelming the {@link DatumWriteOnlyDao} back-end.
 * </p>
 *
 * <p>
 * Note this class also implements {@link DatumWriteOnlyDao}. The methods of
 * that API store the datum in the configured cache.
 * </p>
 *
 * @author matt
 * @version 2.9
 * @deprecated since 2.9, use {@link SqsDatumCollector}
 */
@Deprecated
public class AsyncDatumCollector implements CacheEntryCreatedListener<Serializable, Serializable>,
		CacheEntryUpdatedListener<Serializable, Serializable>,
		CacheEntryRemovedListener<Serializable, Serializable>, PingTest, ServiceLifecycleObserver,
		DatumWriteOnlyDao {

	/** The {@code concurrency} property default value. */
	public static final int DEFAULT_CONCURRENCY = 2;

	/** The {@code queueSize} property default value. */
	public static final int DEFAULT_QUEUE_SIZE = 200;

	/** The {@code shutdownWaitSecs} default value. */
	public static final int DEFAULT_SHUTDOWN_WAIT_SECS = 30;

	/** The {@code datumCacheRemovalAlertThreshold} default value. */
	public static final int DEFAULT_DATUM_CACHE_REMOVAL_ALERT_THRESHOLD = 500;

	/** The {@code queueRefillThreshold} property default value. */
	public static final double DEFAULT_QUEUE_REFILL_THRESHOLD = 0.1;

	/** The {@code queueRefillWaitMs} property default value. */
	public static final long DEFAULT_QUEUE_REFILL_WAIT_MS = 20L;

	/** Basic counted fields. */
	public enum BasicCount {

		/** Buffer additions. */
		BufferAdds,

		/** Buffer removals. */
		BufferRemovals,

		/** Daum received. */
		DatumReceived,

		/** Datum persisted. */
		DatumStored,

		/** Datum persistence failures. */
		DatumFail,

		/** Location datum received. */
		LocationDatumReceived,

		/** Location datum persisted. */
		LocationDatumStored,

		/** Location datum persistence failures. */
		LocationDatumFail,

		/** Stream datum received. */
		StreamDatumReceived,

		/** Stream datum persisted. */
		StreamDatumStored,

		/** Stream datum persistence failures. */
		StreamDatumFail,

		/** Work queue additions. */
		WorkQueueAdds,

		/** Work queue removals. */
		WorkQueueRemovals,

		/** Work queue refills. */
		WorkQueueRefills,

		;

	}

	private final Cache<Serializable, Serializable> datumCache;
	private final DatumWriteOnlyDao datumDao;
	private final TransactionTemplate transactionTemplate;
	private final StatTracker stats;
	private final CacheEntryListenerConfiguration<Serializable, Serializable> listenerConfiguration;
	private final ReentrantLock queueLock = new ReentrantLock();

	private UncaughtExceptionHandler exceptionHandler;
	private int concurrency;
	private int queueSize;
	private int shutdownWaitSecs;
	private int datumCacheRemovalAlertThreshold;

	private int queueRefillSize;
	private double queueRefillThreshold = DEFAULT_QUEUE_REFILL_THRESHOLD;
	private long queueRefillWaitMs = DEFAULT_QUEUE_REFILL_WAIT_MS;
	private volatile boolean writeEnabled = false;
	private BlockingQueue<Serializable> queue;
	private DatumWriterThread[] datumThreads;

	private static final Logger log = LoggerFactory.getLogger(AsyncDatumCollector.class);

	/**
	 * Constructor.
	 *
	 * @param datumCache
	 *        the cache to use
	 * @param datumDao
	 *        the datum DAO
	 * @param transactionTemplate
	 *        the transaction template
	 */
	public AsyncDatumCollector(Cache<Serializable, Serializable> datumCache, DatumWriteOnlyDao datumDao,
			TransactionTemplate transactionTemplate) {
		this(datumCache, datumDao, transactionTemplate,
				new StatTracker("AsyncDaoDatum", null, log, 200));
	}

	/**
	 * Constructor.
	 *
	 * @param datumCache
	 *        the cache to use
	 * @param datumDao
	 *        the datum DAO
	 * @param transactionTemplate
	 *        the transaction template
	 * @param stats
	 *        the stats to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AsyncDatumCollector(Cache<Serializable, Serializable> datumCache, DatumWriteOnlyDao datumDao,
			TransactionTemplate transactionTemplate, StatTracker stats) {
		super();
		this.datumCache = requireNonNullArgument(datumCache, "datumCache");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.transactionTemplate = requireNonNullArgument(transactionTemplate, "transactionTemplate");
		this.stats = requireNonNullArgument(stats, "stats");
		this.concurrency = DEFAULT_CONCURRENCY;
		this.shutdownWaitSecs = DEFAULT_SHUTDOWN_WAIT_SECS;
		this.listenerConfiguration = new MutableCacheEntryListenerConfiguration<>(
				new SingletonFactory<CacheEntryListener<Serializable, Serializable>>(this), null, false,
				false);
		setQueueSize(DEFAULT_QUEUE_SIZE);
	}

	/**
	 * Call after configured to start up processing.
	 */
	@Override
	public synchronized void serviceDidStartup() {
		final int threadCount = getConcurrency();
		final UncaughtExceptionHandler exHandler = getExceptionHandler();
		if ( datumThreads != null ) {
			serviceDidShutdown();
		}
		writeEnabled = true;
		this.queue = new LinkedHashSetBlockingQueue<>(queueSize);
		datumThreads = new DatumWriterThread[threadCount];
		for ( int i = 0; i < threadCount; i++ ) {
			datumThreads[i] = new DatumWriterThread();
			if ( exHandler != null ) {
				datumThreads[i].setUncaughtExceptionHandler(exHandler);
			}
			datumThreads[i].start();
		}
		datumCache.registerCacheEntryListener(listenerConfiguration);
	}

	/**
	 * Call when no longer needed.
	 */
	@Override
	public synchronized void serviceDidShutdown() {
		doShutdown();
		datumThreads = null;
	}

	private void doShutdown() {
		writeEnabled = false;
		if ( datumThreads != null ) {
			for ( DatumWriterThread t : datumThreads ) {
				t.interrupt();
			}
		}
		if ( !datumCache.isClosed() ) {
			datumCache.deregisterCacheEntryListener(listenerConfiguration);
			datumCache.close();
		}
	}

	/**
	 * Shutdown and wait for all threads to finish.
	 */
	public synchronized void shutdownAndWait() {
		doShutdown();
		if ( datumThreads != null ) {
			for ( DatumWriterThread t : datumThreads ) {
				try {
					t.join(TimeUnit.SECONDS.toMillis(shutdownWaitSecs));
				} catch ( InterruptedException e ) {
					// ignore
				}
				if ( t.isAlive() ) {
					t.interrupt();
				}
			}
		}
		datumThreads = null;
	}

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "Async DAO Datum Collector";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public Result performPingTest() throws Exception {
		// verify buffer removals not lagging behind additions
		long addCount = stats.get(BasicCount.BufferAdds);
		long removeCount = stats.get(BasicCount.BufferRemovals);
		long lagDiff = addCount - removeCount;
		Map<String, Number> statMap = new LinkedHashMap<>(BasicCount.values().length);
		for ( BasicCount s : BasicCount.values() ) {
			statMap.put(s.toString(), stats.get(s));
		}
		if ( datumCache instanceof BufferingDelegatingCache<Serializable, Serializable> buf ) {
			statMap.put("BufferSize", buf.getInternalSize());
			statMap.put("BufferCapacity", buf.getInternalCapacity());
			statMap.put("BufferWatermark", buf.getInternalSizeWatermark());
		}
		if ( lagDiff > datumCacheRemovalAlertThreshold ) {
			return new PingTestResult(false, String.format("Buffer removal lag %d > %d", lagDiff,
					datumCacheRemovalAlertThreshold), statMap);
		}
		final DatumWriterThread[] workers = this.datumThreads;
		return new PingTestResult(true, String.format("Processed %d datum using %d workers; lag %d.",
				addCount, workers != null ? workers.length : 0, lagDiff), statMap);
	}

	@Override
	public DatumPK persist(GeneralObjectDatum<? extends GeneralObjectDatumKey> entity) {
		GeneralObjectDatumKey id = requireNonNullArgument(entity.getId(), "entity.id");
		datumCache.put(id, (Serializable) entity);
		// note the stream ID is not known at this point
		return new ObjectDatumPK(id.getKind(), id.getObjectId(), id.getSourceId(), id.getTimestamp(),
				null);
	}

	@Override
	public DatumPK store(net.solarnetwork.domain.datum.Datum datum) {
		if ( datum == null || datum.getObjectId() == null || datum.getSourceId() == null ) {
			return null;
		}
		var d = convertGeneralDatum(datum);
		return persist(d);
	}

	@Override
	public DatumPK store(StreamDatum datum) {
		DatumPK id = switch (datum) {
			case DatumEntity d -> requireNonNullArgument(d.getId(), "entity.id");
			default -> new DatumPK(requireNonNullArgument(datum.getStreamId(), "datum.streamId"),
					requireNonNullArgument(datum.getTimestamp(), "datum.timestamp"));
		};
		datumCache.put(id, (Serializable) datum);
		return id;
	}

	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final class DatumWriterThread extends Thread {

		private DatumWriterThread() {
			super(String.format("DatumWriter-" + COUNTER.incrementAndGet()));
		}

		@Override
		public void run() {
			try {
				while ( writeEnabled ) {
					final Serializable key = queue.take();
					if ( key != null ) {
						stats.increment(BasicCount.WorkQueueRemovals, true);
						log.trace("POLL: |{}", key);
						final Serializable entity = datumCache.getAndRemove(key);
						try {
							if ( entity != null ) {
								log.trace("STORE: |{}", key);
								transactionTemplate.execute(new TransactionCallbackWithoutResult() {

									@Override
									protected void doInTransactionWithoutResult(
											TransactionStatus status) {
										if ( entity instanceof DatumEntity d ) {
											datumDao.store(d);
										} else if ( entity instanceof GeneralObjectDatum<?> d ) {
											datumDao.persist(d);
										}
									}
								});
								if ( entity instanceof DatumEntity ) {
									stats.increment(BasicCount.StreamDatumStored);
								} else if ( entity instanceof GeneralNodeDatum ) {
									stats.increment(BasicCount.DatumStored);
								} else if ( entity instanceof GeneralLocationDatum ) {
									stats.increment(BasicCount.LocationDatumStored);
								}
							} else {
								log.trace("MISS: |{}", key);
							}
						} catch ( Throwable t ) {
							if ( entity != null ) {
								datumCache.put(key, entity);
							}
							if ( entity instanceof DatumEntity ) {
								stats.increment(BasicCount.StreamDatumFail);
							} else if ( entity instanceof GeneralNodeDatum ) {
								stats.increment(BasicCount.DatumFail);
							} else if ( entity instanceof GeneralLocationDatum ) {
								stats.increment(BasicCount.LocationDatumFail);
							}
							Throwable root = t;
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							log.warn("Error storing datum {}: {}", key, root.toString());
							UncaughtExceptionHandler exHandler = getUncaughtExceptionHandler();
							if ( exHandler != null ) {
								exHandler.uncaughtException(this, t);
							}
						}
					}

					// try to re-fill queue from cache if queue below queueRefillSize
					if ( queueRefillWaitMs > 0
							&& queueLock.tryLock(queueRefillWaitMs, TimeUnit.MILLISECONDS) ) {
						try {
							int currSize = queue.size();
							if ( currSize < queueRefillSize ) {
								log.trace("REFILL: |{}/{}", currSize, queueSize);
								stats.increment(BasicCount.WorkQueueRefills, true);
								for ( Entry<Serializable, Serializable> e : datumCache ) {
									if ( e == null ) {
										continue;
									}
									if ( !queue.offer(e.getKey()) ) {
										break;
									}
								}
							}
						} finally {
							queueLock.unlock();
						}
					}
				}
			} catch ( InterruptedException e ) {
				// outta here
			}
		}
	}

	/**
	 * Get the number of threads to use.
	 *
	 * @return the number of threads
	 */
	public int getConcurrency() {
		return concurrency;
	}

	@Override
	public void onCreated(
			Iterable<CacheEntryEvent<? extends Serializable, ? extends Serializable>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Serializable, ? extends Serializable> event : events ) {
			Serializable key = event.getKey();
			log.trace("CACHE_CRE: |{}", key);
			stats.increment(BasicCount.BufferAdds, true);
			if ( key instanceof DatumPK ) {
				stats.increment(BasicCount.StreamDatumReceived);
			} else if ( key instanceof GeneralNodeDatumPK ) {
				stats.increment(BasicCount.DatumReceived);
			} else {
				stats.increment(BasicCount.LocationDatumReceived);
			}
			if ( queue.offer(key) ) {
				stats.increment(BasicCount.WorkQueueAdds, true);
			}
		}
	}

	@Override
	public void onUpdated(
			Iterable<CacheEntryEvent<? extends Serializable, ? extends Serializable>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Serializable, ? extends Serializable> event : events ) {
			Serializable key = event.getKey();
			log.trace("CACHE_UPT: |{}", key);
			stats.increment(BasicCount.BufferRemovals, true);
			stats.increment(BasicCount.BufferAdds, true);
		}
	}

	@Override
	public void onRemoved(
			Iterable<CacheEntryEvent<? extends Serializable, ? extends Serializable>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Serializable, ? extends Serializable> event : events ) {
			Serializable key = event.getKey();
			log.trace("CACHE_REM: |{}", key);
			stats.increment(BasicCount.BufferRemovals, true);
			if ( log.isTraceEnabled() ) {
				long c = stats.get(BasicCount.BufferRemovals);
				if ( stats.getLogFrequency() > 0 && ((c % stats.getLogFrequency()) == 0) ) {
					Set<Serializable> allKeys = StreamSupport.stream(datumCache.spliterator(), false)
							.filter(Objects::nonNull).map(Entry::getKey).collect(Collectors.toSet());
					log.trace("Datum cache keys: {}", allKeys);
				}
			}
		}
	}

	/**
	 * Set the number of threads to use.
	 *
	 * @param concurrency
	 *        the number of threads; anything less than {@literal 1} will be
	 *        treated as {@literal 1}
	 */
	public void setConcurrency(int concurrency) {
		if ( concurrency < 1 ) {
			concurrency = 1;
		}
		this.concurrency = concurrency;
	}

	/**
	 * Get the datum cache.
	 *
	 * @return the cache
	 */
	public Cache<Serializable, Serializable> getDatumCache() {
		return datumCache;
	}

	/**
	 * Get an exception handler for the background threads.
	 *
	 * @return the configured handler
	 */
	public UncaughtExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	/**
	 * Set an exception handler for the background threads.
	 *
	 * @param exceptionHandler
	 *        the handler to use
	 */
	public void setExceptionHandler(UncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Get the queue size.
	 *
	 * @return the queue size
	 */
	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * Set the queue size.
	 *
	 * @param queueSize
	 *        the queue size; anything less than {@literal 1} will be treated as
	 *        {@literal 1}
	 */
	public void setQueueSize(int queueSize) {
		if ( queueSize < 1 ) {
			queueSize = 1;
		}
		this.queueSize = queueSize;
		setupQueueRefillSize(queueSize, queueRefillThreshold);
	}

	private void setupQueueRefillSize(int queueSize, double queueRefillThreshold) {
		this.queueRefillSize = Math.max(1, (int) (queueRefillThreshold * queueSize));
	}

	/**
	 * Get the percentage full threshold that triggers a "refill" from the
	 * cache.
	 *
	 * @return the threshold; defaults to
	 *         {@link #DEFAULT_QUEUE_REFILL_THRESHOLD}
	 * @since 2.4
	 */
	public double getQueueRefillThreshold() {
		return queueRefillThreshold;
	}

	/**
	 * Set the percentage full threshold that triggers a "refill" from the
	 * cache.
	 *
	 * @param queueRefillThreshold
	 *        the threshold to set
	 * @since 2.4
	 */
	public void setQueueRefillThreshold(double queueRefillThreshold) {
		this.queueRefillThreshold = queueRefillThreshold;
		setupQueueRefillSize(queueSize, queueRefillThreshold);
	}

	/**
	 * Get the maximum number of seconds to wait for threads to finish during
	 * shutdown.
	 *
	 * @return the wait secs
	 */
	public int getShutdownWaitSecs() {
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
	public void setShutdownWaitSecs(int shutdownWaitSecs) {
		if ( shutdownWaitSecs < 0 ) {
			shutdownWaitSecs = 0;
		}
		this.shutdownWaitSecs = shutdownWaitSecs;
	}

	/**
	 * Get the datum cache removal alert threshold.
	 *
	 * @return the threshold
	 */
	public int getDatumCacheRemovalAlertThreshold() {
		return datumCacheRemovalAlertThreshold;
	}

	/**
	 * Set the datum cache removal alert threshold.
	 *
	 * <p>
	 * This threshold represents the <i>difference</i> between the
	 * {@link BasicCount#BufferAdds} and {@link BasicCount#BufferRemovals}
	 * statistics. If the {@code BufferRemovals} count lags behind
	 * {@code BufferAdds} it means datum are not getting persisted fast enough.
	 * Passing this threshold will trigger a failure {@link PingTest} result in
	 * {@link #performPingTest()}.
	 * </p>
	 *
	 * @param datumCacheRemovalAlertThreshold
	 *        the threshold to set
	 */
	public void setDatumCacheRemovalAlertThreshold(int datumCacheRemovalAlertThreshold) {
		this.datumCacheRemovalAlertThreshold = datumCacheRemovalAlertThreshold;
	}

	/**
	 * Get the amount of time to wait to refill the queue after processing each
	 * datum.
	 *
	 * @return the wait time, in milliseconds; defaults to
	 *         {@link #DEFAULT_QUEUE_REFILL_WAIT_MS}
	 * @since 2.7
	 */
	public long getQueueRefillWaitMs() {
		return queueRefillWaitMs;
	}

	/**
	 * Set the amount of time to wait to refill the queue after processing each
	 * datum.
	 *
	 * @param queueRefillWaitMs
	 *        the wait time, in milliseconds
	 * @throws IllegalArgumentException
	 *         if {@code queueRefillWaitMs} is less than 0
	 * @since 2.7
	 */
	public void setQueueRefillWaitMs(long queueRefillWaitMs) {
		if ( queueRefillWaitMs < 0 ) {
			throw new IllegalArgumentException("The queueRefillWaitMs value must be 0 or more.");
		}
		this.queueRefillWaitMs = queueRefillWaitMs;
	}

}
