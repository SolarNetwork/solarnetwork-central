/* ==================================================================
 * AsyncDatumCollectorTests.java - 25/03/2020 2:08:17 pm
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

package net.solarnetwork.central.datum.support.test;

import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.AsyncDatumCollector;
import net.solarnetwork.central.datum.support.CollectorStats;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.BasePK;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.central.support.JCacheFactoryBean;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.service.PingTest;

/**
 * Test cases for the {@link AsyncDatumCollector}.
 * 
 * @author matt
 * @version 2.0
 */
public class AsyncDatumCollectorTests_BufferingDelegatingCache implements UncaughtExceptionHandler {

	private static final String TEST_CACHE_NAME = "test-datum-buffer-persistence";

	private DatumEntityDao datumDao;
	private PlatformTransactionManager txManager;
	private CacheManager cacheManager;
	private BufferingDelegatingCache<Serializable, Serializable> datumCache;
	private Cache<Serializable, Serializable> delegateDatumCache;
	private CollectorStats stats;

	private AsyncDatumCollector collector;
	private List<Throwable> uncaughtExceptions;

	private static final Logger log = LoggerFactory.getLogger(AsyncDatumCollector.class);

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
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		txManager = EasyMock.createMock(PlatformTransactionManager.class);

		cacheManager = createCacheManager();
		JCacheFactoryBean<Serializable, Serializable> factory = new JCacheFactoryBean(cacheManager,
				BasePK.class, Object.class);
		factory.setName("Test Datum Buffer");
		factory.setHeapMaxEntries(10);
		factory.setDiskMaxSizeMB(10);
		factory.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		delegateDatumCache = factory.getObject();

		datumCache = new BufferingDelegatingCache<>(delegateDatumCache, 5);

		uncaughtExceptions = new ArrayList<>(2);

		stats = new CollectorStats("AsyncDatumCollector", 1);

		collector = new AsyncDatumCollector(datumCache, datumDao, new TransactionTemplate(txManager),
				stats);
		collector.setConcurrency(2);
		collector.setQueueSize(5);
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
		EasyMock.verify(datumDao, txManager);
		if ( !uncaughtExceptions.isEmpty() ) {
			throw uncaughtExceptions.get(0);
		}
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(datumDao, txManager);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptions.add(e);
	}

	private GeneralNodeDatum createDatum() {
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(UUID.randomUUID().getMostSignificantBits());
		d.setSourceId(UUID.randomUUID().toString());
		d.setCreated(Instant.now());
		d.setSamples(new DatumSamples());
		d.getSamples().putInstantaneousSampleValue("foo", 1);
		return d;
	}

	private GeneralLocationDatum createLocationDatum() {
		GeneralLocationDatum d = new GeneralLocationDatum();
		d.setLocationId(UUID.randomUUID().getMostSignificantBits());
		d.setSourceId(UUID.randomUUID().toString());
		d.setCreated(Instant.now());
		d.setSamples(new DatumSamples());
		d.getSamples().putInstantaneousSampleValue("bim", 1);
		return d;
	}

	private DatumEntity createStreamDatum() {
		DatumProperties p = DatumProperties.propertiesOf(decimalArray("1.23"), null, null, null);
		return new DatumEntity(UUID.randomUUID(), Instant.now(), Instant.now(), p);
	}

	@Test
	public void addNodeDatumToCache() throws Exception {
		// GIVEN
		GeneralNodeDatum d = createDatum();

		TransactionStatus txStatus = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(txStatus);
		expect(datumDao.store(d)).andReturn(new DatumPK(UUID.randomUUID(), d.getCreated()));
		txManager.commit(txStatus);

		// WHEN
		replayAll(txStatus);
		datumCache.put(d.getId(), d);

		// THEN
		Thread.sleep(1000); // give time for cache to call listener
		collector.shutdownAndWait();

		verify(txStatus);
	}

	@Test
	public void addNodeDatumToCache_manyThreads() throws Exception {
		// GIVEN
		ExecutorService executor = Executors.newWorkStealingPool(4);
		List<GeneralNodeDatum> datum = new ArrayList<>(50);
		Object[] txStatuses = new Object[50];
		for ( int i = 0; i < 50; i++ ) {
			GeneralNodeDatum d = createDatum();
			TransactionStatus txStatus = EasyMock.createMock(TransactionStatus.class);
			expect(txManager.getTransaction(anyObject())).andReturn(txStatus);
			expect(datumDao.store(d)).andReturn(new DatumPK(UUID.randomUUID(), d.getCreated()));
			txManager.commit(txStatus);
			txStatuses[i] = txStatus;
			datum.add(d);
		}

		// WHEN
		replayAll(txStatuses);
		for ( GeneralNodeDatum d : datum ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					datumCache.put(d.getId(), d);
				}

			});
		}

		// THEN
		Thread.sleep(1000); // give time for cache to call listener
		executor.shutdown();
		executor.awaitTermination(15, TimeUnit.SECONDS);
		collector.shutdownAndWait();

		verify(txStatuses);
	}

	@Test
	public void addNodeDatumToCache_manyThreads_overflow() throws Exception {
		// GIVEN
		ExecutorService executor = Executors.newWorkStealingPool(4);
		List<GeneralNodeDatum> datum = new ArrayList<>(50);
		Object[] txStatuses = new Object[50];
		for ( int i = 0; i < 50; i++ ) {
			GeneralNodeDatum d = createDatum();
			TransactionStatus txStatus = EasyMock.createMock(TransactionStatus.class);
			expect(txManager.getTransaction(anyObject())).andReturn(txStatus);
			expect(datumDao.store(d)).andAnswer(new IAnswer<DatumPK>() {

				@Override
				public DatumPK answer() throws Throwable {
					try {
						Thread.sleep(200);
					} catch ( InterruptedException e ) {
						// ignore
					}
					return new DatumPK(UUID.randomUUID(), d.getCreated());
				}
			});
			txManager.commit(txStatus);
			txStatuses[i] = txStatus;
			datum.add(d);
		}

		// WHEN
		replayAll(txStatuses);
		for ( GeneralNodeDatum d : datum ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					datumCache.put(d.getId(), d);
				}

			});
		}

		// THEN
		Thread.sleep(1000);
		PingTest.Result pingResult1 = collector.performPingTest();
		Thread.sleep(50 * 200 + 1000); // give time for cache to call listener
		executor.shutdown();
		executor.awaitTermination(15, TimeUnit.SECONDS);
		collector.shutdownAndWait();
		PingTest.Result pingResult2 = collector.performPingTest();

		log.info("Ping result 1: {}", pingResult1.getProperties());
		log.info("Ping result 2: {}", pingResult2.getProperties());

		verify(txStatuses);
	}

	@Test
	public void addNodeDatumToCache_manyThreads_overflow_duplicates() throws Exception {
		// GIVEN
		GeneralNodeDatum d1 = createDatum();
		ExecutorService executor = Executors.newWorkStealingPool(4);
		List<GeneralNodeDatum> datum = new ArrayList<>(50);
		UUID streamId = UUID.randomUUID();
		expect(txManager.getTransaction(anyObject())).andAnswer(new IAnswer<TransactionStatus>() {

			@Override
			public TransactionStatus answer() throws Throwable {
				return new SimpleTransactionStatus(true);
			}
		}).atLeastOnce();
		txManager.commit(anyObject(TransactionStatus.class));
		expectLastCall().atLeastOnce();

		expect(datumDao.store(d1)).andAnswer(new IAnswer<DatumPK>() {

			@Override
			public DatumPK answer() throws Throwable {
				try {
					Thread.sleep(200);
				} catch ( InterruptedException e ) {
					// ignore
				}
				return new DatumPK(streamId, d1.getCreated());
			}
		}).atLeastOnce();

		for ( int i = 0; i < 50; i++ ) {
			GeneralNodeDatum d = d1.clone();
			datum.add(d);
		}

		// WHEN
		replayAll();
		PingTest.Result pingResult1 = collector.performPingTest();
		for ( GeneralNodeDatum d : datum ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(50);
					} catch ( InterruptedException e ) {
						// ignore
					}
					datumCache.put(d.getId(), d);
				}

			});
		}

		// THEN
		Thread.sleep(1000);
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.SECONDS);
		Thread.sleep(2000); // give time for cache to call listener
		collector.shutdownAndWait();
		PingTest.Result pingResult2 = collector.performPingTest();

		log.info("Ping result 1: {}", pingResult1.getProperties());
		log.info("Ping result 2: {}", pingResult2.getProperties());

		assertThat("Added 50 duplicate datum", stats.get(CollectorStats.BasicCount.BufferAdds),
				equalTo(50L));
		assertThat("Removed 50 duplicate datum", stats.get(CollectorStats.BasicCount.BufferRemovals),
				equalTo(50L));
		assertThat("Received at least 1 datum", stats.get(CollectorStats.BasicCount.DatumReceived),
				greaterThanOrEqualTo(1L));
		assertThat("Stored at least 1 datum", stats.get(CollectorStats.BasicCount.DatumStored),
				greaterThanOrEqualTo(1L));
	}

	@Test
	public void addLocationDatumToCache() throws Exception {
		// GIVEN
		GeneralLocationDatum d = createLocationDatum();

		TransactionStatus txStatus = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(txStatus);
		expect(datumDao.store(d)).andReturn(new DatumPK(UUID.randomUUID(), d.getCreated()));
		txManager.commit(txStatus);

		// WHEN
		replayAll(txStatus);
		datumCache.put(d.getId(), d);

		// THEN
		Thread.sleep(1000); // give time for cache to call listener
		collector.shutdownAndWait();

		verify(txStatus);
	}

	@Test
	public void addNodeAndLocationDatumToCache_sameKeyValue() throws Exception {
		// GIVEN
		GeneralNodeDatum d1 = createDatum();
		d1.setNodeId(123L);
		GeneralLocationDatum d2 = createLocationDatum();

		TransactionStatus txStatus = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(txStatus).times(2);
		expect(datumDao.store(d1)).andReturn(new DatumPK(UUID.randomUUID(), d1.getCreated()));
		expect(datumDao.store(d2)).andReturn(new DatumPK(UUID.randomUUID(), d2.getCreated()));
		txManager.commit(txStatus);
		expectLastCall().times(2);

		// WHEN
		replayAll(txStatus);
		datumCache.put(d1.getId(), d1);
		datumCache.put(d2.getId(), d2);

		// THEN
		Thread.sleep(1000); // give time for cache to call listener
		collector.shutdownAndWait();

		verify(txStatus);

		assertThat("Added stat", stats.get(CollectorStats.BasicCount.BufferAdds), equalTo(2L));
		assertThat("Removed stat", stats.get(CollectorStats.BasicCount.BufferRemovals), equalTo(2L));
	}

	@Test
	public void shutdownAndRestoreCache() throws Exception {
		// GIVEN
		JCacheFactoryBean<String, Boolean> factory = new JCacheFactoryBean<>(cacheManager, String.class,
				Boolean.class);
		factory.setName(TEST_CACHE_NAME);
		factory.setHeapMaxEntries(3);
		factory.setDiskMaxSizeMB(1);
		factory.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		factory.setDiskPersistent(true);
		Cache<String, Boolean> testCache = factory.getObject();

		// WHEN
		replayAll();
		for ( int i = 0; i < 10; i++ ) {
			testCache.put(String.valueOf(i), Boolean.TRUE);
		}
		for ( int i = 0; i < 10; i += 2 ) {
			testCache.remove(String.valueOf(i));
		}
		testCache.close();
		//cacheManager.close();
		//cacheManager = createCacheManager();

		JCacheFactoryBean<String, Boolean> factory2 = new JCacheFactoryBean<>(cacheManager, String.class,
				Boolean.class);
		factory2.setName(TEST_CACHE_NAME);
		factory2.setHeapMaxEntries(3);
		factory2.setDiskMaxSizeMB(1);
		factory2.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		factory2.setDiskPersistent(true);
		Cache<String, Boolean> testCache2 = factory2.getObject();

		Set<String> loadedKeys = StreamSupport.stream(testCache2.spliterator(), false)
				.map(e -> e.getKey()).collect(Collectors.toSet());

		// THEN
		assertThat("Set re-lodaed persisted keys", loadedKeys,
				containsInAnyOrder("1", "3", "5", "7", "9"));
	}

	@Test
	public void addStreamDatumToCache() throws Exception {
		// GIVEN
		DatumEntity d = createStreamDatum();

		TransactionStatus txStatus = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(txStatus);
		expect(datumDao.store(d)).andReturn(d.getId());
		txManager.commit(txStatus);

		// WHEN
		replayAll(txStatus);
		datumCache.put(d.getId(), d);

		// THEN
		Thread.sleep(1000); // give time for cache to call listener
		collector.shutdownAndWait();

		verify(txStatus);
	}

}
