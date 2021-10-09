/* ==================================================================
 * UserNodeEventTaskProcessorJobTests.java - 10/06/2020 9:06:59 am
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

package net.solarnetwork.central.user.event.dao.jobs.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.central.user.event.dao.jobs.UserNodeEventTaskProcessorJob.DEFAULT_TOPIC;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.expiry.Duration;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.support.JCacheFactoryBean;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.dao.UserNodeEventTaskDao;
import net.solarnetwork.central.user.event.dao.jobs.UserNodeEventTaskProcessorJob;
import net.solarnetwork.central.user.event.domain.UserNodeEvent;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.central.user.event.domain.UserNodeEventTaskState;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link UserNodeEventTaskProcessorJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeEventTaskProcessorJobTests {

	private static final String TEST_SERVICE_CACHE_NAME = "Test UserNodeEventHookService";
	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_SERVICE_ID = "test.service";

	private EventAdmin eventAdmin;
	private PlatformTransactionManager txManager;
	private UserNodeEventTaskDao taskDao;
	private UserNodeEventHookService hookService;
	private CacheManager cacheManager;
	private Cache<String, UserNodeEventHookService> serviceCache;
	private TestJob job;

	private static class TestJob extends UserNodeEventTaskProcessorJob {

		public TestJob(EventAdmin eventAdmin, TransactionTemplate transactionTemplate,
				UserNodeEventTaskDao taskDao,
				OptionalServiceCollection<UserNodeEventHookService> hookServices) {
			super(eventAdmin, transactionTemplate, taskDao, hookServices);
		}

		// allow tests to call this directly to simplify
		@Override
		public boolean handleJob(Event job) throws Exception {
			return super.handleJob(job);
		}

	}

	@Before
	public void setup() throws Exception {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		txManager = EasyMock.createMock(PlatformTransactionManager.class);
		taskDao = EasyMock.createMock(UserNodeEventTaskDao.class);
		hookService = EasyMock.createMock(UserNodeEventHookService.class);

		cacheManager = createCacheManager();

		JCacheFactoryBean<String, UserNodeEventHookService> factory = new JCacheFactoryBean<>(
				cacheManager, String.class, UserNodeEventHookService.class);
		factory.setName(TEST_SERVICE_CACHE_NAME);
		factory.setHeapMaxEntries(100);
		factory.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Created);
		factory.setExpiryDuration(new Duration(TimeUnit.SECONDS, 2));
		factory.afterPropertiesSet();
		serviceCache = factory.getObject();

		job = new TestJob(eventAdmin, new TransactionTemplate(txManager), taskDao,
				new StaticOptionalServiceCollection<>(asList(hookService)));
	}

	public static CacheManager createCacheManager() {
		try {
			File path = Files.createTempDirectory("net.solarnetwork.central.user.event.dao.jobs.test")
					.toFile();
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

	private void replayAll(Object... mocks) {
		EasyMock.replay(eventAdmin, txManager, taskDao, hookService);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
		}
	}

	@After
	public void teardown() {
		if ( serviceCache != null ) {
			serviceCache.close();
		}
		cacheManager.destroyCache(TEST_SERVICE_CACHE_NAME);
		EasyMock.verify(eventAdmin, txManager, taskDao, hookService);
	}

	private void assertTaskNotCompleted(UserNodeEventTask task) {
		assertTaskResult(task, false, false);
	}

	private void assertTaskResult(UserNodeEventTask task, boolean completed, boolean success) {
		if ( completed ) {
			assertThat("Task completed", task.getCompleted(), notNullValue());
			assertThat("Task success", task.getSuccess(), equalTo(success));
			assertThat("Task status", task.getStatus(), equalTo(UserNodeEventTaskState.Completed));
		} else {
			assertThat("Task not completed", task.getCompleted(), nullValue());
			assertThat("Task without success", task.getSuccess(), nullValue());
			assertThat("Task without status", task.getStatus(), nullValue());
		}
	}

	@Test
	public void run_singleThread_noTasks() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(null);
		txManager.commit(tx);

		// WHEN
		replayAll(tx);
		Event jobEvent = new Event("test", Collections.emptyMap());
		boolean result = job.handleJob(jobEvent);

		// THEN
		assertThat("Job handled successfully", result, equalTo(true));
	}

	@Test
	public void run_singleThread_oneTask_missingService() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);

		UserNodeEvent event = new UserNodeEvent(UUID.randomUUID(), Instant.now());
		UserNodeEventTask task = new UserNodeEventTask(event.getId(), event.getCreated());
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(
				UUID.randomUUID().getMostSignificantBits(), TEST_USER_ID, Instant.now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		task.setHookId(conf.getId().getId());
		event.setTask(task);
		event.setConfig(conf);

		// claim task
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(event);

		// execute task
		expect(hookService.getId()).andReturn("this.service.not.found").atLeastOnce();

		// complete task
		taskDao.taskCompleted(task);
		txManager.commit(tx);

		// look for another task to execute
		TransactionStatus tx2 = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx2);
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(null);
		txManager.commit(tx2);

		// WHEN
		replayAll(tx, tx2);
		Event jobEvent = new Event("test", Collections.emptyMap());
		boolean result = job.handleJob(jobEvent);

		// THEN
		assertThat("Job handled successfully", result, equalTo(true));
		assertTaskResult(task, true, false);
	}

	@Test
	public void run_singleThread_oneTask() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);

		UserNodeEvent event = new UserNodeEvent(UUID.randomUUID(), Instant.now());
		UserNodeEventTask task = new UserNodeEventTask(event.getId(), event.getCreated());
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(
				UUID.randomUUID().getMostSignificantBits(), TEST_USER_ID, Instant.now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		task.setHookId(conf.getId().getId());
		event.setTask(task);
		event.setConfig(conf);

		// claim task
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(event);

		// execute task
		expect(hookService.getId()).andReturn(TEST_SERVICE_ID).atLeastOnce();
		expect(hookService.processUserNodeEventHook(conf, task)).andReturn(true);

		// complete task
		taskDao.taskCompleted(task);
		txManager.commit(tx);

		// look for another task to execute
		TransactionStatus tx2 = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx2);
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(null);
		txManager.commit(tx2);

		// WHEN
		replayAll(tx, tx2);
		Event jobEvent = new Event("test", Collections.emptyMap());
		boolean result = job.handleJob(jobEvent);

		// THEN
		assertThat("Job handled successfully", result, equalTo(true));
		assertTaskResult(task, true, true);
	}

	@Test
	public void run_singleThread_oneTask_TimeoutException() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);
		job.setServiceTimeout(500L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);

		UserNodeEvent event = new UserNodeEvent(UUID.randomUUID(), Instant.now());
		UserNodeEventTask task = new UserNodeEventTask(event.getId(), event.getCreated());
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(
				UUID.randomUUID().getMostSignificantBits(), TEST_USER_ID, Instant.now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		task.setHookId(conf.getId().getId());
		event.setTask(task);
		event.setConfig(conf);

		// claim task
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(event);

		// execute task, but never return
		final CountDownLatch latch = new CountDownLatch(1);
		expect(hookService.getId()).andReturn(TEST_SERVICE_ID).atLeastOnce();
		expect(hookService.processUserNodeEventHook(conf, task)).andAnswer(new IAnswer<Boolean>() {

			@Override
			public Boolean answer() throws Throwable {
				latch.await();
				return true;
			}
		});

		// rollback
		txManager.rollback(tx);

		// look for another task to execute
		TransactionStatus tx2 = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx2);
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(null);
		txManager.commit(tx2);

		// WHEN
		replayAll(tx, tx2);
		Event jobEvent = new Event("test", Collections.emptyMap());
		boolean result = job.handleJob(jobEvent);
		latch.countDown();

		// THEN
		assertThat("Job handled successfully", result, equalTo(true));
		assertTaskNotCompleted(task);
	}

	@Test
	public void run_singleThread_oneTask_RepeatableTaskException() throws Exception {
		// GIVEN
		job.setParallelism(1);
		job.setMaximumWaitMs(5000L);
		job.setServiceTimeout(500L);

		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx);

		UserNodeEvent event = new UserNodeEvent(UUID.randomUUID(), Instant.now());
		UserNodeEventTask task = new UserNodeEventTask(event.getId(), event.getCreated());
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(
				UUID.randomUUID().getMostSignificantBits(), TEST_USER_ID, Instant.now());
		conf.setServiceIdentifier(TEST_SERVICE_ID);
		task.setHookId(conf.getId().getId());
		event.setTask(task);
		event.setConfig(conf);

		// claim task
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(event);

		// execute task, but never return
		expect(hookService.getId()).andReturn(TEST_SERVICE_ID).atLeastOnce();
		expect(hookService.processUserNodeEventHook(conf, task))
				.andThrow(new RepeatableTaskException("Test"));

		// rollback
		txManager.rollback(tx);

		// look for another task to execute
		TransactionStatus tx2 = EasyMock.createMock(TransactionStatus.class);
		expect(txManager.getTransaction(EasyMock.anyObject())).andReturn(tx2);
		expect(taskDao.claimQueuedTask(DEFAULT_TOPIC)).andReturn(null);
		txManager.commit(tx2);

		// WHEN
		replayAll(tx, tx2);
		Event jobEvent = new Event("test", Collections.emptyMap());
		boolean result = job.handleJob(jobEvent);

		// THEN
		assertThat("Job handled successfully", result, equalTo(true));
		assertTaskNotCompleted(task);
	}

}
