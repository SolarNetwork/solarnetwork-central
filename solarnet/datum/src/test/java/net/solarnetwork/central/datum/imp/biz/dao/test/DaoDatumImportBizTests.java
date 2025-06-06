/* ==================================================================
 * DaoDatumImportBizTests.java - 11/11/2018 12:32:33 PM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.biz.dao.test;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.datum.imp.biz.dao.DaoDatumImportBiz.EMPTY_INPUT_RESOURCE_META;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.util.FileCopyUtils.copy;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import net.solarnetwork.central.dao.SecurityTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.biz.dao.DaoDatumImportBiz;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportReceipt;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportResult;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatServiceImportContext;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.BulkLoadingDao;
import net.solarnetwork.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.service.ResourceStorageService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link DaoDatumImportBiz} class.
 *
 * @author matt
 * @version 2.1
 */
public class DaoDatumImportBizTests {

	private static final Long TEST_USER_ID = -1L;
	private static final Long TEST_NODE_ID = -2L;
	private static final Long TEST_NODE_ID_2 = -3L;
	private static final String TEST_SOURCE_ID = "test.source";

	private TaskScheduler scheduledExecutorService;
	private AsyncTaskExecutor executorSercvice;
	private SolarNodeOwnershipDao userNodeDao;
	private SecurityTokenDao securityTokenDao;
	private DatumImportJobInfoDao jobInfoDao;
	private DatumEntityDao datumDao;
	private ResourceStorageService resourceStorageService;

	@SuppressWarnings("unchecked")
	private final BulkLoadingDao.LoadingContext<GeneralNodeDatum> loadingContext = EasyMock
			.createMock(BulkLoadingDao.LoadingContext.class);

	private class TestDaoDatumImportBiz extends DaoDatumImportBiz {

		private TestDaoDatumImportBiz(TaskScheduler scheduler, AsyncTaskExecutor executor,
				SolarNodeOwnershipDao userNodeDao, SecurityTokenDao securityTokenDao,
				DatumImportJobInfoDao jobInfoDao, DatumEntityDao datumDao) {
			super(scheduler, executor, userNodeDao, securityTokenDao, jobInfoDao, datumDao);
		}

		@Override
		public File getImportDataFile(UserUuidPK id) {
			return super.getImportDataFile(id);
		}

	}

	private TestDaoDatumImportBiz biz;

	@BeforeEach
	public void setup() {
		scheduledExecutorService = EasyMock.createMock(TaskScheduler.class);
		executorSercvice = EasyMock.createMock(AsyncTaskExecutor.class);

		jobInfoDao = EasyMock.createMock(DatumImportJobInfoDao.class);
		userNodeDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		securityTokenDao = EasyMock.createMock(SecurityTokenDao.class);
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		resourceStorageService = EasyMock.createMock(ResourceStorageService.class);

		biz = new TestDaoDatumImportBiz(scheduledExecutorService, executorSercvice, userNodeDao,
				securityTokenDao, jobInfoDao, datumDao);
		biz.setPreviewExecutor(executorSercvice);
	}

	private void replayAll() {
		EasyMock.replay(scheduledExecutorService, executorSercvice, userNodeDao, securityTokenDao,
				jobInfoDao, datumDao, loadingContext, resourceStorageService);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(scheduledExecutorService, executorSercvice, userNodeDao, jobInfoDao, datumDao,
				loadingContext, resourceStorageService);
	}

	@Test
	public void submitDatumImportRequest() throws IOException {
		// given
		BasicInputConfiguration inputConfiguration = new BasicInputConfiguration(TEST_USER_ID);
		inputConfiguration.setName("Test CSV Input");
		inputConfiguration.setTimeZoneId("UTC");
		inputConfiguration.setServiceIdentifier("foo");
		inputConfiguration.setServiceProps(Collections.singletonMap("foo", "bar"));
		BasicConfiguration configuration = new BasicConfiguration("Test Import", false);
		configuration.setInputConfiguration(inputConfiguration);
		BasicDatumImportRequest request = new BasicDatumImportRequest(configuration, TEST_USER_ID);
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-01.csv", getClass()), "text/csv");

		Capture<DatumImportJobInfo> jobInfoCaptor = new Capture<>();
		expect(jobInfoDao.save(capture(jobInfoCaptor))).andAnswer(new IAnswer<UserUuidPK>() {

			@Override
			public UserUuidPK answer() throws Throwable {
				return jobInfoCaptor.getValue().getId();
			}
		});
		expect(jobInfoDao.get(assertWith(new Assertion<UserUuidPK>() {

			@Override
			public void check(UserUuidPK argument) throws Throwable {
				assertThat(argument, equalTo(jobInfoCaptor.getValue().getId()));
			}
		}))).andAnswer(new IAnswer<DatumImportJobInfo>() {

			@Override
			public DatumImportJobInfo answer() throws Throwable {
				return jobInfoCaptor.getValue();
			}

		});

		// when
		replayAll();
		DatumImportReceipt receipt = biz.submitDatumImportRequest(request, resource);

		// then
		assertThat("Receipt returned", receipt, notNullValue());

		DatumImportJobInfo info = jobInfoCaptor.getValue();
		assertThat("Info user ID", info.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Configuration copied", info.getConfiguration(),
				allOf(notNullValue(), not(sameInstance(configuration))));
		assertThat("Configuration name", info.getConfiguration().getName(),
				equalTo(configuration.getName()));
		assertThat("Input configuration copied", info.getConfiguration().getInputConfiguration(),
				allOf(notNullValue(), not(sameInstance(inputConfiguration))));
		assertThat("Input configuration name", info.getConfiguration().getInputConfiguration().getName(),
				equalTo(inputConfiguration.getName()));
		assertThat("Input configuration zone",
				info.getConfiguration().getInputConfiguration().getTimeZoneId(),
				equalTo(inputConfiguration.getTimeZoneId()));
		assertThat("Input configuration service ID",
				info.getConfiguration().getInputConfiguration().getServiceIdentifier(),
				equalTo(inputConfiguration.getServiceIdentifier()));
		assertThat("Input configuration service props",
				info.getConfiguration().getInputConfiguration().getServiceProperties(),
				equalTo(inputConfiguration.getServiceProperties()));
		assertThat("Active token ID empty", info.getTokenId(), nullValue());

		File dataFile = biz.getImportDataFile(info.getId());
		assertThat("Data copied to file in work dir",
				Arrays.equals(copyToByteArray(dataFile),
						copyToByteArray(getClass().getResourceAsStream("test-data-01.csv"))),
				equalTo(true));
		dataFile.delete();
	}

	@Test
	public void submitDatumImportRequest_withToken() throws IOException {
		// given
		BasicInputConfiguration inputConfiguration = new BasicInputConfiguration(TEST_USER_ID);
		inputConfiguration.setName("Test CSV Input");
		inputConfiguration.setTimeZoneId("UTC");
		inputConfiguration.setServiceIdentifier("foo");
		inputConfiguration.setServiceProps(Collections.singletonMap("foo", "bar"));
		BasicConfiguration configuration = new BasicConfiguration("Test Import", false);
		configuration.setInputConfiguration(inputConfiguration);
		BasicDatumImportRequest request = new BasicDatumImportRequest(configuration, TEST_USER_ID);
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-01.csv", getClass()), "text/csv");

		Capture<DatumImportJobInfo> jobInfoCaptor = new Capture<>();
		expect(jobInfoDao.save(capture(jobInfoCaptor))).andAnswer(new IAnswer<UserUuidPK>() {

			@Override
			public UserUuidPK answer() throws Throwable {
				return jobInfoCaptor.getValue().getId();
			}
		});
		expect(jobInfoDao.get(assertWith(new Assertion<UserUuidPK>() {

			@Override
			public void check(UserUuidPK argument) throws Throwable {
				assertThat(argument, equalTo(jobInfoCaptor.getValue().getId()));
			}
		}))).andAnswer(new IAnswer<DatumImportJobInfo>() {

			@Override
			public DatumImportJobInfo answer() throws Throwable {
				return jobInfoCaptor.getValue();
			}

		});

		// when
		replayAll();
		final String tokenId = UUID.randomUUID().toString();
		SecurityUtils.becomeToken(tokenId, SecurityTokenType.User, TEST_USER_ID, null);
		DatumImportReceipt receipt = null;
		try {
			receipt = biz.submitDatumImportRequest(request, resource);
		} finally {
			SecurityUtils.removeAuthentication();
		}

		// then
		assertThat("Receipt returned", receipt, notNullValue());

		DatumImportJobInfo info = jobInfoCaptor.getValue();
		assertThat("Info user ID", info.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Configuration copied", info.getConfiguration(),
				allOf(notNullValue(), not(sameInstance(configuration))));
		assertThat("Configuration name", info.getConfiguration().getName(),
				equalTo(configuration.getName()));
		assertThat("Input configuration copied", info.getConfiguration().getInputConfiguration(),
				allOf(notNullValue(), not(sameInstance(inputConfiguration))));
		assertThat("Input configuration name", info.getConfiguration().getInputConfiguration().getName(),
				equalTo(inputConfiguration.getName()));
		assertThat("Input configuration zone",
				info.getConfiguration().getInputConfiguration().getTimeZoneId(),
				equalTo(inputConfiguration.getTimeZoneId()));
		assertThat("Input configuration service ID",
				info.getConfiguration().getInputConfiguration().getServiceIdentifier(),
				equalTo(inputConfiguration.getServiceIdentifier()));
		assertThat("Input configuration service props",
				info.getConfiguration().getInputConfiguration().getServiceProperties(),
				equalTo(inputConfiguration.getServiceProperties()));
		assertThat("Active token ID saved", info.getTokenId(), equalTo(tokenId));

		File dataFile = biz.getImportDataFile(info.getId());
		assertThat("Data copied to file in work dir",
				Arrays.equals(copyToByteArray(dataFile),
						copyToByteArray(getClass().getResourceAsStream("test-data-01.csv"))),
				equalTo(true));
		dataFile.delete();
	}

	@Test
	public void submitDatumImportRequest_withResourceStorage() throws IOException {
		// given
		biz.setResourceStorageService(resourceStorageService);
		BasicInputConfiguration inputConfiguration = new BasicInputConfiguration(TEST_USER_ID);
		inputConfiguration.setName("Test CSV Input");
		inputConfiguration.setTimeZoneId("UTC");
		inputConfiguration.setServiceIdentifier("foo");
		inputConfiguration.setServiceProps(Collections.singletonMap("foo", "bar"));
		BasicConfiguration configuration = new BasicConfiguration("Test Import", false);
		configuration.setInputConfiguration(inputConfiguration);
		BasicDatumImportRequest request = new BasicDatumImportRequest(configuration, TEST_USER_ID);
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ClassPathResource("test-data-01.csv", getClass()), "text/csv");

		expect(resourceStorageService.isConfigured()).andReturn(true);

		CompletableFuture<Boolean> savedFuture = CompletableFuture.completedFuture(true);
		Capture<String> resourceNameCaptor = new Capture<>();
		Capture<Resource> resourceCaptor = new Capture<>();
		expect(resourceStorageService.saveResource(capture(resourceNameCaptor), capture(resourceCaptor),
				eq(true), anyObject())).andReturn(savedFuture);

		Capture<DatumImportJobInfo> jobInfoCaptor = new Capture<>();
		expect(jobInfoDao.save(capture(jobInfoCaptor))).andAnswer(new IAnswer<UserUuidPK>() {

			@Override
			public UserUuidPK answer() throws Throwable {
				return jobInfoCaptor.getValue().getId();
			}
		});
		expect(jobInfoDao.get(assertWith(new Assertion<UserUuidPK>() {

			@Override
			public void check(UserUuidPK argument) throws Throwable {
				assertThat(argument, equalTo(jobInfoCaptor.getValue().getId()));
			}
		}))).andAnswer(new IAnswer<DatumImportJobInfo>() {

			@Override
			public DatumImportJobInfo answer() throws Throwable {
				return jobInfoCaptor.getValue();
			}

		});

		// when
		replayAll();
		DatumImportReceipt receipt = biz.submitDatumImportRequest(request, resource);

		// then
		assertThat("Receipt returned", receipt, notNullValue());

		assertThat("Resource saved to storage with user ID prefix", resourceNameCaptor.getValue(),
				startsWith(TEST_USER_ID + "-"));
		assertThat("Resource saved to storage has same file name",
				resourceCaptor.getValue().getFilename(), equalTo(resourceNameCaptor.getValue()));

		DatumImportJobInfo info = jobInfoCaptor.getValue();
		assertThat("Info user ID", info.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Configuration copied", info.getConfiguration(),
				allOf(notNullValue(), not(sameInstance(configuration))));
		assertThat("Configuration name", info.getConfiguration().getName(),
				equalTo(configuration.getName()));
		assertThat("Input configuration copied", info.getConfiguration().getInputConfiguration(),
				allOf(notNullValue(), not(sameInstance(inputConfiguration))));
		assertThat("Input configuration name", info.getConfiguration().getInputConfiguration().getName(),
				equalTo(inputConfiguration.getName()));
		assertThat("Input configuration zone",
				info.getConfiguration().getInputConfiguration().getTimeZoneId(),
				equalTo(inputConfiguration.getTimeZoneId()));
		assertThat("Input configuration service ID",
				info.getConfiguration().getInputConfiguration().getServiceIdentifier(),
				equalTo(inputConfiguration.getServiceIdentifier()));
		assertThat("Input configuration service props",
				info.getConfiguration().getInputConfiguration().getServiceProperties(),
				equalTo(inputConfiguration.getServiceProperties()));

		File dataFile = biz.getImportDataFile(info.getId());
		assertThat("Data copied to file in work dir",
				Arrays.equals(copyToByteArray(dataFile),
						copyToByteArray(getClass().getResourceAsStream("test-data-01.csv"))),
				equalTo(true));
		dataFile.delete();
	}

	@Test
	public void submitDatumImportRequest_withResourceStorage_emptydata() throws IOException {
		// given
		biz.setResourceStorageService(resourceStorageService);
		BasicInputConfiguration inputConfiguration = new BasicInputConfiguration(TEST_USER_ID);
		inputConfiguration.setName("Test CSV Input");
		inputConfiguration.setTimeZoneId("UTC");
		inputConfiguration.setServiceIdentifier("foo");
		inputConfiguration.setServiceProps(Collections.singletonMap("foo", "bar"));
		BasicConfiguration configuration = new BasicConfiguration("Test Import", false);
		configuration.setInputConfiguration(inputConfiguration);
		BasicDatumImportRequest request = new BasicDatumImportRequest(configuration, TEST_USER_ID);
		BasicDatumImportResource resource = new BasicDatumImportResource(
				new ByteArrayResource(new byte[0], "test-data-01.csv"), "text/csv");

		Capture<DatumImportJobInfo> jobInfoCaptor = new Capture<>();
		expect(jobInfoDao.save(capture(jobInfoCaptor))).andAnswer(new IAnswer<UserUuidPK>() {

			@Override
			public UserUuidPK answer() throws Throwable {
				return jobInfoCaptor.getValue().getId();
			}
		});
		expect(jobInfoDao.get(assertWith(new Assertion<UserUuidPK>() {

			@Override
			public void check(UserUuidPK argument) throws Throwable {
				assertThat(argument, equalTo(jobInfoCaptor.getValue().getId()));
			}
		}))).andAnswer(new IAnswer<DatumImportJobInfo>() {

			@Override
			public DatumImportJobInfo answer() throws Throwable {
				return jobInfoCaptor.getValue();
			}

		});

		// WHEN
		replayAll();
		DatumImportReceipt receipt = biz.submitDatumImportRequest(request, resource);

		// THEN
		then(receipt).as("Receipt returned").isNotNull();

		DatumImportJobInfo info = jobInfoCaptor.getValue();
		// @formatter:off
		then(info)
			.as("Job info saved to DAO")
			.isNotNull()
			.as("User ID returned")
			.returns(TEST_USER_ID, from(DatumImportJobInfo::getUserId))
			.as("Percent complete")
			.returns(0.0, from(DatumImportJobInfo::getPercentComplete))
			.as("Metadata created with empty flag")
			.returns(Map.of(EMPTY_INPUT_RESOURCE_META, true), from(DatumImportJobInfo::getMetadata))
			.satisfies(e -> {
				then(e.getImportDate())
					.as("Import date set")
					.isNotNull()
					;
				then(e.getConfiguration())
					.as("Configuration present")
					.isNotNull()
					.as("Configuration cloned")
					.isNotSameAs(configuration)
					;
			})
			.extracting(DatumImportJobInfo::getConfiguration)
			.usingRecursiveComparison()
			.ignoringFields("inputConfiguration.userId") // not serialized to JSON
			.as("Configuration preserved")
			.isEqualTo(configuration)
			;

		then(biz.getImportDataFile(info.getId()))
			.as("With empty data input, no data file created")
			.doesNotExist()
			;
		// @formatter:on
	}

	private static class TestInputService extends BaseDatumImportInputFormatService {

		private final List<GeneralNodeDatum> data;

		private TestInputService(List<GeneralNodeDatum> data) {
			super("foo");
			this.data = data;
		}

		@Override
		public ImportContext createImportContext(InputConfiguration config, DatumImportResource resource,
				ProgressListener<DatumImportService> progressListener) throws IOException {
			return new BaseDatumImportInputFormatServiceImportContext(config, resource,
					progressListener) {

				@Override
				public Iterator<GeneralNodeDatum> iterator() {
					final Iterator<GeneralNodeDatum> itr = data.iterator();
					return new Iterator<GeneralNodeDatum>() {

						private int count = 0;

						@Override
						public boolean hasNext() {
							// TODO Auto-generated method stub
							return itr.hasNext();
						}

						@Override
						public GeneralNodeDatum next() {
							GeneralNodeDatum d = itr.next();
							count++;
							if ( progressListener != null ) {
								progressListener.progressChanged(TestInputService.this,
										count / (double) data.size());
							}
							return d;
						}

					};
				}

			};
		}

		@Override
		public String getDisplayName() {
			return "Test Input Service";
		}

		@Override
		public List<SettingSpecifier> getSettingSpecifiers() {
			return null;
		}

	}

	private List<GeneralNodeDatum> sampleData(int count, Instant start) {
		List<GeneralNodeDatum> data = new ArrayList<>(4);
		long wh = (long) (Math.random() * 1000000000.0);
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setNodeId(TEST_NODE_ID);
			d.setCreated(start.plus(i, ChronoUnit.MINUTES));
			d.setSourceId(TEST_SOURCE_ID);

			DatumSamples s = new DatumSamples();
			int watts = (int) (Math.random() * 50000);
			s.putInstantaneousSampleValue("watts", watts);
			wh += wh + (long) (watts / 60.0);
			s.putAccumulatingSampleValue("wattHours", wh);
			d.setSamples(s);
			data.add(d);
		}
		return data;
	}

	private DatumImportJobInfo createTestJobInfo(UserUuidPK pk) {
		BasicInputConfiguration inputConfiguration = new BasicInputConfiguration(TEST_USER_ID);
		inputConfiguration.setName("Test CSV Input");
		inputConfiguration.setTimeZoneId("UTC");
		inputConfiguration.setServiceIdentifier("foo");
		inputConfiguration.setServiceProps(Collections.singletonMap("foo", "bar"));
		BasicConfiguration configuration = new BasicConfiguration("Test Import", false);
		configuration.setInputConfiguration(inputConfiguration);
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(pk);
		info.setConfig(configuration);
		info.setImportDate(Instant.now());
		info.setImportState(DatumImportState.Queued);
		return info;
	}

	@Test
	public void previewRequest() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(100,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.DAYS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.setImportState(DatumImportState.Staged);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK>>> taskCaptor = new Capture<>();
		CompletableFuture<FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK>> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership }).anyTimes();

		String jobId = pk.getId().toString();

		// when
		replayAll();
		BasicDatumImportPreviewRequest request = new BasicDatumImportPreviewRequest(TEST_USER_ID, jobId,
				10);
		Future<FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK>> preview = biz
				.previewStagedImportRequest(request);

		// pretend to perform work via executor service
		FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK> result = taskCaptor.getValue()
				.call();

		// then
		assertThat("Preview future available", preview, notNullValue());
		assertThat("Preview result available", result, notNullValue());
		assertThat("Preview starting offset", result.getStartingOffset(), equalTo(0L));
		assertThat("Preview returned result count", result.getReturnedResultCount(), equalTo(10));
		assertThat("Preview returned total count", result.getTotalResults(), equalTo(100L));
		assertThat("Preview result data", result.getResults(), notNullValue());

		List<GeneralNodeDatumComponents> previewData = StreamSupport
				.stream(result.getResults().spliterator(), false).collect(toList());
		assertThat("Preview data count", previewData, hasSize(10));
	}

	@Test
	public void performImport() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		DatumImportJobInfo info = createTestJobInfo(pk);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership }).anyTimes();

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Capture<GeneralNodeDatum> loadedDataCaptor = new Capture<GeneralNodeDatum>(CaptureType.ALL);
		loadingContext.load(capture(loadedDataCaptor));
		expectLastCall().times(data.size());

		expect(loadingContext.getLoadedCount()).andAnswer(new IAnswer<Long>() {

			private long count = 0;

			@Override
			public Long answer() throws Throwable {
				return ++count;
			}
		}).times(data.size());

		loadingContext.commit();

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());
		for ( int i = 0; i < data.size(); i++ ) {
			assertThat("Loaded data PK " + i, loadedDataCaptor.getValues().get(i).getId(),
					equalTo(data.get(i).getId()));
			assertThat("Loaded data samples " + i, loadedDataCaptor.getValues().get(i).getSamples(),
					equalTo(data.get(i).getSamples()));
			assertThat("Loaded data posted data set to import date", data.get(i).getPosted(),
					equalTo(info.getImportDate()));
		}

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.SingleTransaction));
		assertThat("Loading batch size", loadingOpts.getBatchSize(), nullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import succeeded", result.isSuccess(), equalTo(true));
		assertThat("Import message", result.getMessage(), equalTo("Loaded " + data.size() + " datum."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void performImport_withToken() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		final String tokenId = UUID.randomUUID().toString();

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.setTokenId(tokenId);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership1 = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		SolarNodeOwnership ownership2 = new BasicSolarNodeOwnership(TEST_NODE_ID_2, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership1, ownership2 }).anyTimes();

		// auth token has policy that allows only one node ID
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(TEST_NODE_ID))
				.withSourceIds(singleton(TEST_SOURCE_ID)).build();
		UserDetails userDetails = new User(tokenId, "secret", createAuthorityList("ROLE_USER"));
		AuthenticatedToken authToken = new AuthenticatedToken(userDetails, SecurityTokenType.User,
				TEST_USER_ID, policy);
		expect(securityTokenDao.securityTokenForId(tokenId)).andReturn(authToken);

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Capture<GeneralNodeDatum> loadedDataCaptor = new Capture<GeneralNodeDatum>(CaptureType.ALL);
		loadingContext.load(capture(loadedDataCaptor));
		expectLastCall().times(data.size());

		expect(loadingContext.getLoadedCount()).andAnswer(new IAnswer<Long>() {

			private long count = 0;

			@Override
			public Long answer() throws Throwable {
				return ++count;
			}
		}).times(data.size());

		loadingContext.commit();

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());
		for ( int i = 0; i < data.size(); i++ ) {
			assertThat("Loaded data PK " + i, loadedDataCaptor.getValues().get(i).getId(),
					equalTo(data.get(i).getId()));
			assertThat("Loaded data samples " + i, loadedDataCaptor.getValues().get(i).getSamples(),
					equalTo(data.get(i).getSamples()));
			assertThat("Loaded data posted data set to import date", data.get(i).getPosted(),
					equalTo(info.getImportDate()));
		}

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.SingleTransaction));
		assertThat("Loading batch size", loadingOpts.getBatchSize(), nullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import succeeded", result.isSuccess(), equalTo(true));
		assertThat("Import message", result.getMessage(), equalTo("Loaded " + data.size() + " datum."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void performImport_withToken_nodeNotAuthorized() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		final String tokenId = UUID.randomUUID().toString();

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.setTokenId(tokenId);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership1 = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		SolarNodeOwnership ownership2 = new BasicSolarNodeOwnership(TEST_NODE_ID_2, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership1, ownership2 }).anyTimes();

		// auth token has policy that allows only OTHER node ID
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(TEST_NODE_ID_2))
				.withSourceIds(singleton(TEST_SOURCE_ID)).build();
		UserDetails userDetails = new User(tokenId, "secret", createAuthorityList("ROLE_USER"));
		AuthenticatedToken authToken = new AuthenticatedToken(userDetails, SecurityTokenType.User,
				TEST_USER_ID, policy);
		expect(securityTokenDao.securityTokenForId(tokenId)).andReturn(authToken);

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Long committedCount = 0L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.SingleTransaction));
		assertThat("Loading batch size", loadingOpts.getBatchSize(), nullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import failed", result.isSuccess(), equalTo(false));
		assertThat("Import message", result.getMessage(),
				equalTo("Not authorized to load data for node " + TEST_NODE_ID + "."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void performImport_withToken_sourceNotAuthorized() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		final String tokenId = UUID.randomUUID().toString();

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.setTokenId(tokenId);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership1 = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		SolarNodeOwnership ownership2 = new BasicSolarNodeOwnership(TEST_NODE_ID_2, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership1, ownership2 }).anyTimes();

		// auth token has policy that allows only node ID and some OTHER source
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(TEST_NODE_ID))
				.withSourceIds(singleton("foo")).build();
		UserDetails userDetails = new User(tokenId, "secret", createAuthorityList("ROLE_USER"));
		AuthenticatedToken authToken = new AuthenticatedToken(userDetails, SecurityTokenType.User,
				TEST_USER_ID, policy);
		expect(securityTokenDao.securityTokenForId(tokenId)).andReturn(authToken);

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Long committedCount = 0L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.SingleTransaction));
		assertThat("Loading batch size", loadingOpts.getBatchSize(), nullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import failed", result.isSuccess(), equalTo(false));
		assertThat("Import message", result.getMessage(),
				equalTo("Not authorized to load data for source " + TEST_SOURCE_ID + "."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void performImport_withToken_sourceAuthorizedPattern() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		final String tokenId = UUID.randomUUID().toString();

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.setTokenId(tokenId);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership1 = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		SolarNodeOwnership ownership2 = new BasicSolarNodeOwnership(TEST_NODE_ID_2, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership1, ownership2 }).anyTimes();

		// auth token has policy that allows one node and source pattern
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(TEST_NODE_ID))
				.withSourceIds(singleton("test.*")).build();
		UserDetails userDetails = new User(tokenId, "secret", createAuthorityList("ROLE_USER"));
		AuthenticatedToken authToken = new AuthenticatedToken(userDetails, SecurityTokenType.User,
				TEST_USER_ID, policy);
		expect(securityTokenDao.securityTokenForId(tokenId)).andReturn(authToken);

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Capture<GeneralNodeDatum> loadedDataCaptor = new Capture<GeneralNodeDatum>(CaptureType.ALL);
		loadingContext.load(capture(loadedDataCaptor));
		expectLastCall().times(data.size());

		expect(loadingContext.getLoadedCount()).andAnswer(new IAnswer<Long>() {

			private long count = 0;

			@Override
			public Long answer() throws Throwable {
				return ++count;
			}
		}).times(data.size());

		loadingContext.commit();

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());
		for ( int i = 0; i < data.size(); i++ ) {
			assertThat("Loaded data PK " + i, loadedDataCaptor.getValues().get(i).getId(),
					equalTo(data.get(i).getId()));
			assertThat("Loaded data samples " + i, loadedDataCaptor.getValues().get(i).getSamples(),
					equalTo(data.get(i).getSamples()));
			assertThat("Loaded data posted data set to import date", data.get(i).getPosted(),
					equalTo(info.getImportDate()));
		}

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.SingleTransaction));
		assertThat("Loading batch size", loadingOpts.getBatchSize(), nullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import succeeded", result.isSuccess(), equalTo(true));
		assertThat("Import message", result.getMessage(), equalTo("Loaded " + data.size() + " datum."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void performImport_fetchResource() throws Exception {
		// given
		biz.setResourceStorageService(resourceStorageService);
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		InputStream dataFileStream = getClass().getResourceAsStream("test-data-01.csv");

		DatumImportJobInfo info = createTestJobInfo(pk);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		expect(resourceStorageService.isConfigured()).andReturn(true).atLeastOnce();

		Resource r = new AbstractResource() {

			@Override
			public InputStream getInputStream() throws IOException {
				return dataFileStream;
			}

			@Override
			public String getDescription() {
				return "Remote resource";
			}
		};
		CompletableFuture<Iterable<Resource>> storageListingFuture = CompletableFuture
				.completedFuture(Collections.singleton(r));
		expect(resourceStorageService.listResources(dataFile.getName())).andReturn(storageListingFuture);

		expect(resourceStorageService.getUid()).andReturn("Magic Storage").anyTimes();

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership }).anyTimes();

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Capture<GeneralNodeDatum> loadedDataCaptor = new Capture<GeneralNodeDatum>(CaptureType.ALL);
		loadingContext.load(capture(loadedDataCaptor));
		expectLastCall().times(data.size());

		expect(loadingContext.getLoadedCount()).andAnswer(new IAnswer<Long>() {

			private long count = 0;

			@Override
			public Long answer() throws Throwable {
				return ++count;
			}
		}).times(data.size());

		loadingContext.commit();

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		Set<String> deletedResourcePaths = Collections.singleton("Yeah, baby!");
		Capture<Iterable<String>> deleteResourcePathsCaptor = new Capture<>();
		expect(resourceStorageService.deleteResources(capture(deleteResourcePathsCaptor)))
				.andReturn(CompletableFuture.completedFuture(deletedResourcePaths));

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());
		for ( int i = 0; i < data.size(); i++ ) {
			assertThat("Loaded data PK " + i, loadedDataCaptor.getValues().get(i).getId(),
					equalTo(data.get(i).getId()));
			assertThat("Loaded data samples " + i, loadedDataCaptor.getValues().get(i).getSamples(),
					equalTo(data.get(i).getSamples()));
			assertThat("Loaded data posted data set to import date", data.get(i).getPosted(),
					equalTo(info.getImportDate()));
		}

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.SingleTransaction));
		assertThat("Loading batch size", loadingOpts.getBatchSize(), nullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import succeeded", result.isSuccess(), equalTo(true));
		assertThat("Import message", result.getMessage(), equalTo("Loaded " + data.size() + " datum."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));

		List<String> deleteResourcePaths = StreamSupport
				.stream(deleteResourcePathsCaptor.getValue().spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Requested resources to delete", deleteResourcePaths,
				Matchers.contains(dataFile.getName()));
	}

	@Test
	public void performImport_emptyResource() throws Exception {
		// GIVEN
		biz.setResourceStorageService(resourceStorageService);
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.setMetadata(Map.of(EMPTY_INPUT_RESOURCE_META, true));
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership }).anyTimes();

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Capture<GeneralNodeDatum> loadedDataCaptor = new Capture<GeneralNodeDatum>(CaptureType.ALL);
		loadingContext.load(capture(loadedDataCaptor));
		expectLastCall().times(data.size());

		expect(loadingContext.getLoadedCount()).andAnswer(new IAnswer<Long>() {

			private long count = 0;

			@Override
			public Long answer() throws Throwable {
				return ++count;
			}
		}).times(data.size());

		loadingContext.commit();

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// WHEN
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// THEN
		then(status).as("Status returned").isNotNull();

		// @formatter:off
		for ( int i = 0; i < data.size(); i++ ) {
			then(loadedDataCaptor.getValues().get(i))
				.as("Loaded data PK %d", i)
				.returns(data.get(i).getId(), from(GeneralNodeDatum::getId))
				.as("Loaded data samples %d", i)
				.returns(data.get(i).getSamples(), from(GeneralNodeDatum::getSamples))
				.as("Loaded data %d posted date set to import date", i)
				.returns(info.getImportDate(), from(GeneralNodeDatum::getPosted))
				;
		}

		then(loadingOptionsCaptor.getValue())
			.as("Loading tx mode")
			.returns(LoadingTransactionMode.SingleTransaction, from(BulkLoadingDao.LoadingOptions::getTransactionMode))
			.as("Loading batch size")
			.returns(null, from(BulkLoadingDao.LoadingOptions::getBatchSize))
			;

		then(result)
			.as("Import result available")
			.isNotNull()
			.satisfies(r -> {
				then(r.getCompletionDate())
					.as("Import completion date set")
					.isNotNull()
					;
			})
			.as("Import succeeded")
			.returns(true, from(DatumImportResult::isSuccess))
			.as("Import message formatted")
			.returns("Loaded " + data.size() + " datum.", from(DatumImportResult::getMessage))
			.as("Import loaded count")
			.returns(committedCount, from(DatumImportResult::getLoadedCount))
			;
		// @formatter:on
	}

	@Test
	public void performImportWithBatch() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		DatumImportJobInfo info = createTestJobInfo(pk);
		info.getConfig().setBatchSize(2);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node owned by job's user
		SolarNodeOwnership ownership = new BasicSolarNodeOwnership(TEST_NODE_ID, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership }).anyTimes();

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Capture<GeneralNodeDatum> loadedDataCaptor = new Capture<GeneralNodeDatum>(CaptureType.ALL);
		loadingContext.load(capture(loadedDataCaptor));
		expectLastCall().times(data.size());

		expect(loadingContext.getLoadedCount()).andAnswer(new IAnswer<Long>() {

			private long count = 0;

			@Override
			public Long answer() throws Throwable {
				return ++count;
			}
		}).times(data.size());

		loadingContext.commit();

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());
		for ( int i = 0; i < data.size(); i++ ) {
			assertThat("Loaded data PK " + i, loadedDataCaptor.getValues().get(i).getId(),
					equalTo(data.get(i).getId()));
			assertThat("Loaded data samples " + i, loadedDataCaptor.getValues().get(i).getSamples(),
					equalTo(data.get(i).getSamples()));
			assertThat("Loaded data posted data set to import date", data.get(i).getPosted(),
					equalTo(info.getImportDate()));
		}

		BulkLoadingDao.LoadingOptions loadingOpts = loadingOptionsCaptor.getValue();
		assertThat("Loading tx mode", loadingOpts.getTransactionMode(),
				equalTo(LoadingTransactionMode.BatchTransactions));
		assertThat("Loading batch size", loadingOpts.getBatchSize(),
				equalTo(info.getConfig().getBatchSize()));

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import succeeded", result.isSuccess(), equalTo(true));
		assertThat("Import message", result.getMessage(), equalTo("Loaded " + data.size() + " datum."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void performImportUnauthorizedNodeId() throws Exception {
		// given
		List<GeneralNodeDatum> data = sampleData(5,
				Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		biz.setInputServices(singletonList(new TestInputService(data)));
		UserUuidPK pk = new UserUuidPK(TEST_USER_ID, UUID.randomUUID());
		File dataFile = biz.getImportDataFile(pk);
		copy(copyToByteArray(getClass().getResourceAsStream("test-data-01.csv")), dataFile);

		DatumImportJobInfo info = createTestJobInfo(pk);
		expect(jobInfoDao.get(pk)).andReturn(info);

		Capture<Callable<DatumImportResult>> taskCaptor = new Capture<>();
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		expect(executorSercvice.submit(capture(taskCaptor))).andReturn(future);

		// allow updating the status as job progresses
		expect(jobInfoDao.save(info)).andReturn(pk).anyTimes();

		// make test node NOT owned by job's user
		SolarNodeOwnership ownership = new BasicSolarNodeOwnership(TEST_NODE_ID_2, TEST_USER_ID, "NZ",
				ZoneId.of("Pacific/Auckland"), false, false);
		expect(userNodeDao.ownershipsForUserId(TEST_USER_ID))
				.andReturn(new SolarNodeOwnership[] { ownership }).anyTimes();

		Capture<BulkLoadingDao.LoadingOptions> loadingOptionsCaptor = new Capture<>();
		expect(datumDao.createBulkLoadingContext(capture(loadingOptionsCaptor), anyObject()))
				.andReturn(loadingContext);

		Long committedCount = 5L;
		expect(loadingContext.getCommittedCount()).andReturn(committedCount);

		loadingContext.close();

		// when
		replayAll();
		DatumImportStatus status = biz.performImport(pk);

		// pretend to perform work via executor service
		DatumImportResult result = taskCaptor.getValue().call();

		// then
		assertThat("Status returned", status, notNullValue());

		assertThat("Import result available", result, notNullValue());
		assertThat("Import completion date set", result.getCompletionDate(), notNullValue());
		assertThat("Import failed", result.isSuccess(), equalTo(false));
		assertThat("Import message", result.getMessage(),
				equalTo("Not authorized to load data for node " + TEST_NODE_ID + "."));
		assertThat("Import loaded count", result.getLoadedCount(), equalTo(committedCount));
	}

	@Test
	public void deleteForUser() {
		UUID uuid = UUID.randomUUID();
		Capture<Set<DatumImportState>> statesCaptor = new Capture<>();

		expect(jobInfoDao.deleteForUser(eq(TEST_USER_ID), eq(singleton(uuid)), capture(statesCaptor)))
				.andReturn(1);

		List<DatumImportJobInfo> result = new ArrayList<>();
		expect(jobInfoDao.findForUser(TEST_USER_ID, null)).andReturn(result);

		// when
		replayAll();
		Set<String> jobIds = Collections.singleton(uuid.toString());
		Collection<DatumImportStatus> results = biz.deleteDatumImportJobsForUser(TEST_USER_ID, jobIds);

		assertThat("Queried states", statesCaptor.getValue(),
				Matchers.containsInAnyOrder(DatumImportState.Completed, DatumImportState.Retracted,
						DatumImportState.Queued, DatumImportState.Staged, DatumImportState.Unknown));
		assertThat("Results empty", results, hasSize(0));
	}

	@Test
	public void deleteForUserExecutingSkipped() {
		UUID uuid = UUID.randomUUID();
		Capture<Set<DatumImportState>> statesCaptor = new Capture<>();

		expect(jobInfoDao.deleteForUser(eq(TEST_USER_ID), eq(singleton(uuid)), capture(statesCaptor)))
				.andReturn(0);

		// after delete, DAO returns list that still includes the requested job ID, along with
		// another job not requested; we need to verify that the Biz filters out the non-requested job
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(TEST_USER_ID, uuid));
		info.setImportState(DatumImportState.Executing);
		DatumImportJobInfo info2 = new DatumImportJobInfo();
		info2.setId(new UserUuidPK(TEST_USER_ID, UUID.randomUUID()));
		info2.setImportState(DatumImportState.Queued);
		List<DatumImportJobInfo> result = new ArrayList<>(Arrays.asList(info, info2));
		expect(jobInfoDao.findForUser(TEST_USER_ID, null)).andReturn(result);

		// when
		replayAll();
		Set<String> jobIds = Collections.singleton(uuid.toString());
		Collection<DatumImportStatus> results = biz.deleteDatumImportJobsForUser(TEST_USER_ID, jobIds);

		assertThat("Queried states", statesCaptor.getValue(),
				Matchers.containsInAnyOrder(DatumImportState.Completed, DatumImportState.Retracted,
						DatumImportState.Queued, DatumImportState.Staged, DatumImportState.Unknown));
		assertThat("Results count", results, hasSize(1));
		DatumImportStatus status = results.iterator().next();
		assertThat("Result is requested info", status.getJobId(),
				equalTo(info.getId().getId().toString()));
	}
}
