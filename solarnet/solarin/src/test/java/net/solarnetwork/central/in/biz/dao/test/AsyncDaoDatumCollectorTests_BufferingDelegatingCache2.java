/* ==================================================================
 * AsyncDaoDatumCollectorTests.java - 25/03/2020 2:08:17 pm
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

package net.solarnetwork.central.in.biz.dao.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.domain.BasePK;
import net.solarnetwork.central.in.biz.dao.AsyncDaoDatumCollector;
import net.solarnetwork.central.in.biz.dao.CollectorStats;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.central.support.JCacheFactoryBean;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.service.PingTest;

/**
 * Test cases for the {@link AsyncDaoDatumCollector}.
 * 
 * @author matt
 * @version 2.0
 */
public class AsyncDaoDatumCollectorTests_BufferingDelegatingCache2 implements UncaughtExceptionHandler {

	private static final String TEST_CACHE_NAME = "test-datum-buffer-persistence";

	private static final int BUFFER_CAPACITY = 200;

	private static final RandomGenerator RNG = new SecureRandom();

	private TestDatumDao datumDao;
	private TestTxManager txManager;
	private CacheManager cacheManager;
	private NonClosingBufferingDelegatingCache datumCache;
	private Cache<Serializable, Serializable> delegateDatumCache;
	private CollectorStats stats;

	private AsyncDaoDatumCollector collector;
	private List<Throwable> uncaughtExceptions;
	private List<Object> stored;

	private static final Logger log = LoggerFactory.getLogger(AsyncDaoDatumCollector.class);

	public static CacheManager createCacheManager() {
		try {
			File path = Files.createTempDirectory("net.solarnetwork.central.in.biz.dao.test").toFile();
			path.deleteOnExit();
			EhcacheCachingProvider cachingProvider = (EhcacheCachingProvider) Caching
					.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
			DefaultConfiguration configuration = new DefaultConfiguration(
					cachingProvider.getDefaultClassLoader(), new DefaultPersistenceConfiguration(path));
			return cachingProvider.getCacheManager(cachingProvider.getDefaultURI(), configuration);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setup() throws Exception {
		datumDao = new TestDatumDao();
		txManager = new TestTxManager();

		cacheManager = createCacheManager();
		JCacheFactoryBean<Serializable, Serializable> factory = new JCacheFactoryBean(cacheManager,
				BasePK.class, Object.class);
		factory.setName("Test Datum Buffer");
		factory.setDiskMaxSizeMB(10);
		factory.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		delegateDatumCache = factory.getObject();

		datumCache = new NonClosingBufferingDelegatingCache(delegateDatumCache, BUFFER_CAPACITY);

		uncaughtExceptions = new ArrayList<>(2);
		stored = Collections.synchronizedList(new ArrayList<>(1000));

		stats = new CollectorStats("AsyncDaoDatumCollector", 100);

		collector = new AsyncDaoDatumCollector(datumCache, datumDao, new TransactionTemplate(txManager),
				stats);
		collector.setConcurrency(2);
		collector.setExceptionHandler(this);
		collector.setShutdownWaitSecs(3600);
		collector.serviceDidStartup();
	}

	@After
	public void teardown() throws Throwable {
		collector.shutdownAndWait();
		try {
			cacheManager.destroyCache(TEST_CACHE_NAME);
		} catch ( Exception e ) {
			// ignore
		}
		log.info(stats.toString());
		if ( !uncaughtExceptions.isEmpty() ) {
			throw uncaughtExceptions.get(0);
		}
		datumCache.reallyClose();
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptions.add(e);
	}

	private GeneralNodeDatum createDatum(Long nodeId, String sourceId, Instant ts) {
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(nodeId);
		d.setSourceId(sourceId);
		d.setCreated(ts);

		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("f", 1);
		d.setSamples(s);
		return d;
	}

	private void doStore(Entity<?> o) {
		try {
			// simulate taking some time
			long time = 20;
			if ( RNG.nextDouble() > 0.96 ) {
				log.info("Consumer: random long thread sleep {}...", o.getId());
				time = 200;
			}
			Thread.sleep(time);
		} catch ( InterruptedException e ) {
			// ignore
		}
		stored.add(o);
		log.debug("STORED: |{}", o.getId());
	}

	private void thenBufferStatsEquals(int size, int watermark, int lag) {
		// @formatter:off
		then(datumCache).asInstanceOf(type(NonClosingBufferingDelegatingCache.class))
			.as("Buffer size")
			.returns(size, from(NonClosingBufferingDelegatingCache::getInternalSize))
			.as("Buffer watermark")
			.returns(watermark, from(NonClosingBufferingDelegatingCache::getInternalSizeWatermark))
			.as("Lag")
			.returns(lag, (cache) -> {
				return (int)(stats.get(CollectorStats.BasicCount.BufferAdds) - stats.get(CollectorStats.BasicCount.BufferRemovals));
			})
			;
		// @formatter:on
	}

	/**
	 * Try to simulate a common runtime pattern, with a "rush" of datum produced
	 * with variable-speed writer throughput.
	 * 
	 * <p>
	 * The goal of the test is to demonstrate that even with a overflowing
	 * buffer cache, spooling to the disk cache, that eventually all datum are
	 * processed as the writer threads catch up.
	 * </p>
	 * 
	 * @throws Exception
	 *         if any error occurs
	 */
	@Test
	public void addNodeDatumToCache_manyThreads_overflow_continuousAdd() throws Exception {
		// GIVEN
		ExecutorService executor = Executors.newFixedThreadPool(4);

		// WHEN
		final Long nodeId = 1L;
		final String sourceId = "s";
		final Instant startTs = Instant.now().truncatedTo(ChronoUnit.DAYS);
		final AtomicInteger producerCounter = new AtomicInteger();
		final AtomicInteger putCounter = new AtomicInteger();
		final int producerCount = 4;
		final int maxCount = 3_000;

		GeneralNodeDatum[] puts = new GeneralNodeDatum[maxCount];

		for ( int i = 0; i < producerCount; i++ ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						int count = producerCounter.incrementAndGet();
						if ( count > maxCount ) {
							log.info("Producer: maximum reached: {}", maxCount);
							return;
						}

						GeneralNodeDatum d = createDatum(nodeId, sourceId, startTs.plusMillis(count));
						datumCache.put(d.getId(), d);
						putCounter.incrementAndGet();
						log.debug("PUT: |{}", d.getId());
						puts[count - 1] = d;
						long sleep = count > maxCount * 0.75 ? 30
								: Math.max(0, (count - maxCount / 2) / 4);
						if ( sleep > 0 ) {
							log.debug("Producer: sleep {}", sleep);
							try {
								Thread.sleep(sleep);
							} catch ( InterruptedException e ) {
								// ignore
							}
						}
					}
				}

			});
		}

		Thread.sleep(1_000);
		PingTest.Result pingResult1 = collector.performPingTest();

		// let producers go until max reached
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		// wait for stores to stop happening...
		final int unchangedTries = 3;
		int storedCount = -1;
		int unchangeRemaining = unchangedTries;
		while ( true ) {
			Thread.sleep(1_000L);
			int newStoredCount = stored.size();
			if ( newStoredCount == storedCount ) {
				if ( --unchangeRemaining < 1 ) {
					break;
				}
			} else {
				unchangeRemaining = unchangedTries;
			}
			storedCount = newStoredCount;
		}

		collector.shutdownAndWait();

		PingTest.Result pingResult2 = collector.performPingTest();
		log.info("Ping result 1: {}", pingResult1.getProperties());
		log.info("Ping result 2: {}", pingResult2.getProperties());

		int putCount = putCounter.get();
		log.info("Put: {}, store: {}", putCount, storedCount);

		// THEN
		then(storedCount).as("Stored all that produced").isEqualTo(putCount);
		thenBufferStatsEquals(0, BUFFER_CAPACITY, 0);
	}

	private static class NonClosingBufferingDelegatingCache
			extends BufferingDelegatingCache<Serializable, Serializable> {

		private NonClosingBufferingDelegatingCache(Cache<Serializable, Serializable> delegate,
				int internalCapacity) {
			super(delegate, internalCapacity);
		}

		@Override
		public synchronized void close() {
			log.debug("Ignoring cache close...");
		}

		private void reallyClose() {
			super.close();
		}
	}

	private final class TestDatumDao implements DatumEntityDao {

		@Override
		public Class<? extends DatumEntity> getObjectType() {
			return null;
		}

		@Override
		public DatumPK save(DatumEntity entity) {
			stored.add(entity);
			return null;
		}

		@Override
		public DatumEntity get(DatumPK id) {
			return null;
		}

		@Override
		public Collection<DatumEntity> getAll(List<SortDescriptor> sorts) {
			return null;
		}

		@Override
		public void delete(DatumEntity entity) {
		}

		@Override
		public LoadingContext<GeneralNodeDatum> createBulkLoadingContext(LoadingOptions options,
				LoadingExceptionHandler<GeneralNodeDatum> exceptionHandler) {
			return null;
		}

		@Override
		public ExportResult bulkExport(ExportCallback<GeneralNodeDatumFilterMatch> callback,
				ExportOptions options) {
			return null;
		}

		@Override
		public ObjectDatumStreamFilterResults<Datum, DatumPK> findFiltered(DatumCriteria filter,
				List<SortDescriptor> sorts, Integer offset, Integer max) {
			return null;
		}

		@Override
		public void findFilteredStream(DatumCriteria filter,
				StreamDatumFilteredResultsProcessor processor, List<SortDescriptor> sortDescriptors,
				Integer offset, Integer max) throws IOException {

		}

		@Override
		public DatumPK store(DatumEntity datum) {
			doStore(datum);
			return null;
		}

		@Override
		public DatumPK store(GeneralNodeDatum datum) {
			doStore(datum);
			return null;
		}

		@Override
		public DatumPK store(GeneralLocationDatum datum) {
			doStore(datum);
			return null;
		}

		@Override
		public Iterable<DatumDateInterval> findAvailableInterval(ObjectStreamCriteria filter) {
			return null;
		}

	}

	private static class TestTxManager implements PlatformTransactionManager {

		private final List<TransactionStatus> commits = Collections
				.synchronizedList(new ArrayList<>(1000));

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition)
				throws TransactionException {
			return new SimpleTransactionStatus(true);
		}

		@Override
		public void commit(TransactionStatus status) throws TransactionException {
			commits.add(status);
		}

		@Override
		public void rollback(TransactionStatus status) throws TransactionException {

		}

	}

}
