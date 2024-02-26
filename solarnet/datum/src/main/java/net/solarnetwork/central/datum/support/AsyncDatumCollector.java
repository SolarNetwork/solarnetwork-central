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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import net.solarnetwork.central.datum.support.CollectorStats.BasicCount;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Data collector that processes datum and location datum asynchronously.
 *
 * <p>
 * This service works through the {@link Cache} API of datum objects, by
 * listening for cache creation events to add those datum to an internal
 * persistence queue for eventual storage via the {@link DatumEntityDao} API.
 * Once persisted there, the datum cache entry is removed. By configuring the
 * {@link Cache} with non-volatile persistence as well, the cache acts as a
 * write-ahead-log cache of datum updates. The internal persistence queue serves
 * as a limiter to avoid overwhelming the {@link DatumEntityDao} backend.
 * </p>
 *
 * @author matt
 * @version 2.2
 */
public class AsyncDatumCollector implements CacheEntryCreatedListener<Serializable, Serializable>,
		CacheEntryUpdatedListener<Serializable, Serializable>,
		CacheEntryRemovedListener<Serializable, Serializable>, PingTest, ServiceLifecycleObserver {

	/** The {@code concurrency} property default value. */
	public final int DEFAULT_CONCURRENCY = 2;

	/** The {@code queueSize} property default value. */
	public final int DEFAULT_QUEUE_SIZE = 200;

	/** The {@code shtudownWaitSecs} default value. */
	public final int DEFAULT_SHUTDOWN_WAIT_SECS = 15;

	/** The {@code datumCacheRemovalAlertThreshold} default value. */
	public final int DEFAULT_DATUM_CACHE_REMOVAL_ALERT_THRESHOLD = 500;

	private final double QUEUE_REFILL_THRESHOLD = 0.1;

	private final Cache<Serializable, Serializable> datumCache;
	private final DatumEntityDao datumDao;
	private final TransactionTemplate transactionTemplate;
	private final CollectorStats stats;
	private final CacheEntryListenerConfiguration<Serializable, Serializable> listenerConfiguration;
	private final ReentrantLock queueLock = new ReentrantLock();

	private UncaughtExceptionHandler exceptionHandler;
	private int concurrency;
	private int queueSize;
	private int shutdownWaitSecs;
	private int datumCacheRemovalAlertThreshold;

	private int queueRefillSize;
	private volatile boolean writeEnabled = false;
	private BlockingQueue<Serializable> queue;
	private ConcurrentMap<Serializable, Object> scratch; // prevent duplicate processing between threads
	private DatumWriterThread[] datumThreads;

	private final Logger log = LoggerFactory.getLogger(getClass());

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
	public AsyncDatumCollector(Cache<Serializable, Serializable> datumCache, DatumEntityDao datumDao,
			TransactionTemplate transactionTemplate) {
		this(datumCache, datumDao, transactionTemplate, new CollectorStats("AsyncDaoDatum", 200));
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
	public AsyncDatumCollector(Cache<Serializable, Serializable> datumCache, DatumEntityDao datumDao,
			TransactionTemplate transactionTemplate, CollectorStats stats) {
		super();
		this.datumCache = requireNonNullArgument(datumCache, "datumCache");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.transactionTemplate = requireNonNullArgument(transactionTemplate, "transactionTemplate");
		this.stats = requireNonNullArgument(stats, "stats");
		this.concurrency = DEFAULT_CONCURRENCY;
		this.shutdownWaitSecs = DEFAULT_SHUTDOWN_WAIT_SECS;
		this.listenerConfiguration = new MutableCacheEntryListenerConfiguration<Serializable, Serializable>(
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
		this.queue = new ArrayBlockingQueue<>(queueSize);
		this.scratch = new ConcurrentHashMap<>(threadCount, 0.9f, threadCount);
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
		if ( datumCache instanceof BufferingDelegatingCache ) {
			BufferingDelegatingCache<Serializable, Serializable> buf = (BufferingDelegatingCache<Serializable, Serializable>) datumCache;
			statMap.put("BufferSize", buf.getInternalSize());
			statMap.put("BufferCapacity", buf.getInternalCapacity());
			statMap.put("BufferWatermark", buf.getInternalSizeWatermark());
		}
		if ( lagDiff > datumCacheRemovalAlertThreshold ) {
			return new PingTestResult(false, String.format("Buffer removal lag %d > %d", lagDiff,
					datumCacheRemovalAlertThreshold), statMap);
		}
		return new PingTestResult(true, String.format("Processed %d datum; lag %d.", addCount, lagDiff),
				statMap);
	}

	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final class DatumWriterThread extends Thread {

		private final Object scratchValue;

		private DatumWriterThread() {
			super(String.format("DatumWriter-" + COUNTER.incrementAndGet()));
			scratchValue = new Object();
		}

		@Override
		public void run() {
			try {
				while ( writeEnabled ) {
					final Serializable key = queue.take();
					if ( key != null ) {
						log.trace("POLL: |{}", key);
						if ( scratch.putIfAbsent(key, scratchValue) == null ) {
							try {
								final Serializable entity = datumCache.get(key);
								try {
									if ( entity != null ) {
										log.trace("STORE: |{}", key);
										transactionTemplate
												.execute(new TransactionCallbackWithoutResult() {

													@Override
													protected void doInTransactionWithoutResult(
															TransactionStatus status) {
														if ( entity instanceof DatumEntity d ) {
															datumDao.store(d);
														} else if ( entity instanceof GeneralNodeDatum d ) {
															datumDao.store(d);
														} else if ( entity instanceof GeneralLocationDatum d ) {
															datumDao.store(d);
														}
													}
												});
										log.trace("REMOVE: |{}", key);
										datumCache.remove(key);
										if ( entity instanceof DatumEntity ) {
											stats.incrementAndGet(BasicCount.StreamDatumStored);
										} else if ( entity instanceof GeneralNodeDatum ) {
											stats.incrementAndGet(BasicCount.DatumStored);
										} else if ( entity instanceof GeneralLocationDatum ) {
											stats.incrementAndGet(BasicCount.LocationDatumStored);
										}
									} else {
										log.trace("MISS: |{}", key);
									}
								} catch ( Throwable t ) {
									if ( entity instanceof DatumEntity ) {
										stats.incrementAndGet(BasicCount.StreamDatumFail);
									} else if ( entity instanceof GeneralNodeDatum ) {
										stats.incrementAndGet(BasicCount.DatumFail);
									} else if ( entity instanceof GeneralLocationDatum ) {
										stats.incrementAndGet(BasicCount.LocationDatumFail);
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
							} finally {
								scratch.remove(key, scratchValue);
							}
						} else {
							log.trace("SCRATCH: |{}", key);
						}
					}

					// try to re-fill queue from cache
					if ( queueLock.tryLock(2, TimeUnit.SECONDS) ) {
						try {
							int currSize = queue.size();
							if ( currSize < queueRefillSize ) {
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
				// otta here
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
			stats.incrementAndGet(BasicCount.BufferAdds);
			if ( key instanceof DatumPK ) {
				stats.incrementAndGet(BasicCount.StreamDatumReceived);
			} else if ( key instanceof GeneralNodeDatumPK ) {
				stats.incrementAndGet(BasicCount.DatumReceived);
			} else {
				stats.incrementAndGet(BasicCount.LocationDatumReceived);
			}
			queueLock.lock();
			try {
				queue.offer(key);
			} finally {
				queueLock.unlock();
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
			stats.incrementAndGet(BasicCount.BufferRemovals);
			stats.incrementAndGet(BasicCount.BufferAdds);
		}
	}

	@Override
	public void onRemoved(
			Iterable<CacheEntryEvent<? extends Serializable, ? extends Serializable>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Serializable, ? extends Serializable> event : events ) {
			Serializable key = event.getKey();
			log.trace("CACHE_REM: |{}", key);
			long c = stats.incrementAndGet(BasicCount.BufferRemovals);
			if ( log.isTraceEnabled()
					&& (stats.getLogFrequency() > 0 && ((c % stats.getLogFrequency()) == 0)) ) {
				Set<Serializable> allKeys = StreamSupport.stream(datumCache.spliterator(), false)
						.filter(e -> e != null).map(e -> e.getKey()).collect(Collectors.toSet());
				log.trace("Datum cache keys: {}", allKeys);
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
		this.queueRefillSize = Math.max(1, (int) (QUEUE_REFILL_THRESHOLD * queueSize));
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

}
