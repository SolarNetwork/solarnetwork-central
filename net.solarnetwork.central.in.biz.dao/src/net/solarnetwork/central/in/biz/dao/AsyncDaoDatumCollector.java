/* ==================================================================
 * AsyncDaoDatumCollector.java - 25/03/2020 10:43:56 am
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

package net.solarnetwork.central.in.biz.dao;

import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Iterator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.PingTest;
import net.solarnetwork.domain.PingTestResult;

/**
 * Data collector that processes datum and location datum asynchronously.
 * 
 * @author matt
 * @version 1.2
 */
public class AsyncDaoDatumCollector
		implements CacheEntryCreatedListener<Serializable, Serializable>, PingTest {

	/** The {@code concurrency} property default value. */
	public final int DEFAULT_CONCURRENCY = 2;

	/** The {@code queueSize} property default value. */
	public final int DEFAULT_QUEUE_SIZE = 200;

	/** The {@code shtudownWaitSecs} default value. */
	public final int DEFAULT_SHUTDOWN_WAIT_SECS = 15;

	/** The {@code datumCacheRemovalAlertThreshold} default value. */
	public final int DEFAULT_DATUM_CACHE_REMOVAL_ALERT_THRESHOLD = 500;

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
	public AsyncDaoDatumCollector(Cache<Serializable, Serializable> datumCache, DatumEntityDao datumDao,
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
	public AsyncDaoDatumCollector(Cache<Serializable, Serializable> datumCache, DatumEntityDao datumDao,
			TransactionTemplate transactionTemplate, CollectorStats stats) {
		super();
		if ( datumCache == null ) {
			throw new IllegalArgumentException("The datumCache parameter must not be null.");
		}
		this.datumCache = datumCache;
		if ( datumDao == null ) {
			throw new IllegalArgumentException("The datumDao parameter must not be null.");
		}
		this.datumDao = datumDao;
		if ( transactionTemplate == null ) {
			throw new IllegalArgumentException("The transactionTemplate parameter must not be null.");
		}
		this.transactionTemplate = transactionTemplate;
		if ( stats == null ) {
			throw new IllegalArgumentException("The stats parameter must not be null.");
		}
		this.stats = stats;
		this.concurrency = DEFAULT_CONCURRENCY;
		this.shutdownWaitSecs = DEFAULT_SHUTDOWN_WAIT_SECS;
		this.queueSize = DEFAULT_QUEUE_SIZE;
		this.listenerConfiguration = new MutableCacheEntryListenerConfiguration<Serializable, Serializable>(
				new SingletonFactory<CacheEntryListener<Serializable, Serializable>>(this), null, false,
				false);
	}

	/**
	 * Call after configured to start up processing.
	 */
	public synchronized void startup() {
		final int threadCount = getConcurrency();
		final UncaughtExceptionHandler exHandler = getExceptionHandler();
		if ( datumThreads != null ) {
			shutdown();
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
	public synchronized void shutdown() {
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
		long addCount = stats.get(CollectorStats.BasicCount.BufferAdds);
		long removeCount = stats.get(CollectorStats.BasicCount.BufferRemovals);
		long lagDiff = addCount - removeCount;
		Map<String, Long> statMap = new LinkedHashMap<>(CollectorStats.BasicCount.values().length);
		for ( CollectorStats.BasicCount s : CollectorStats.BasicCount.values() ) {
			statMap.put(s.toString(), stats.get(s));
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
					if ( key == null ) {
						continue;
					}
					if ( scratch.putIfAbsent(key, scratchValue) != null ) {
						continue;
					}
					final Serializable entity = datumCache.get(key);
					if ( entity == null ) {
						scratch.remove(key, scratchValue);
						continue;
					}
					try {
						log.trace("Storing datum {}", key);
						transactionTemplate.execute(new TransactionCallbackWithoutResult() {

							@Override
							protected void doInTransactionWithoutResult(TransactionStatus status) {
								if ( entity instanceof DatumEntity ) {
									datumDao.save((DatumEntity) entity);
								} else if ( entity instanceof GeneralNodeDatum ) {
									datumDao.store((GeneralNodeDatum) entity);
								} else if ( entity instanceof GeneralLocationDatum ) {
									datumDao.store((GeneralLocationDatum) entity);
								}
							}
						});
						datumCache.remove(key);
						long c = stats.incrementAndGet(CollectorStats.BasicCount.BufferRemovals);
						if ( entity instanceof DatumEntity ) {
							stats.incrementAndGet(CollectorStats.BasicCount.StreamDatumStored);
						} else if ( entity instanceof GeneralNodeDatum ) {
							stats.incrementAndGet(CollectorStats.BasicCount.DatumStored);
						} else if ( entity instanceof GeneralLocationDatum ) {
							stats.incrementAndGet(CollectorStats.BasicCount.LocationDatumStored);
						}
						if ( log.isTraceEnabled() && (stats.getLogFrequency() > 0
								&& ((c % stats.getLogFrequency()) == 0)) ) {
							Set<Serializable> allKeys = StreamSupport
									.stream(datumCache.spliterator(), false).filter(e -> e != null)
									.map(e -> e.getKey()).collect(Collectors.toSet());
							log.trace("Datum cache keys: {}", allKeys);
						}
					} catch ( Throwable t ) {
						if ( entity instanceof DatumEntity ) {
							stats.incrementAndGet(CollectorStats.BasicCount.StreamDatumFail);
						} else if ( entity instanceof GeneralNodeDatum ) {
							stats.incrementAndGet(CollectorStats.BasicCount.DatumFail);
						} else if ( entity instanceof GeneralLocationDatum ) {
							stats.incrementAndGet(CollectorStats.BasicCount.LocationDatumFail);
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
					} finally {
						scratch.remove(key, scratchValue);
					}

					// try to re-fill queue from cache
					if ( queueLock.tryLock(2, TimeUnit.SECONDS) ) {
						try {
							if ( queue.size() < Math.max(1.0, queueSize * 0.1) ) {
								for ( Iterator<Entry<Serializable, Serializable>> itr = datumCache
										.iterator(); itr.hasNext(); ) {
									Entry<Serializable, Serializable> e = itr.next();
									if ( e != null && !queue.offer(e.getKey()) ) {
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
	public void onCreated(Iterable<CacheEntryEvent<? extends Serializable, ? extends Serializable>> itr)
			throws CacheEntryListenerException {
		queueLock.lock();
		try {
			for ( CacheEntryEvent<? extends Serializable, ? extends Serializable> event : itr ) {
				Serializable key = event.getKey();
				log.trace("Datum cached: {}", key);
				stats.incrementAndGet(CollectorStats.BasicCount.BufferAdds);
				if ( key instanceof DatumPK ) {
					stats.incrementAndGet(CollectorStats.BasicCount.StreamDatumReceived);
				} else if ( key instanceof GeneralNodeDatumPK ) {
					stats.incrementAndGet(CollectorStats.BasicCount.DatumReceived);
				} else {
					stats.incrementAndGet(CollectorStats.BasicCount.LocationDatumReceived);
				}
				queue.offer(key);
			}
		} finally {
			queueLock.unlock();
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
	 * {@link CollectorStats.BasicCount#BufferAdds} and
	 * {@link CollectorStats.BasicCount#BufferRemovals} statistics. If the
	 * {@code BufferRemovals} count lags behind {@code BufferAdds} it means
	 * datum are not getting persisted fast enough. Passing this threshold will
	 * trigger a failure {@link PingTest} result in {@link #performPingTest()}.
	 * </p>
	 * 
	 * @param datumCacheRemovalAlertThreshold
	 *        the threshold to set
	 */
	public void setDatumCacheRemovalAlertThreshold(int datumCacheRemovalAlertThreshold) {
		this.datumCacheRemovalAlertThreshold = datumCacheRemovalAlertThreshold;
	}

}
