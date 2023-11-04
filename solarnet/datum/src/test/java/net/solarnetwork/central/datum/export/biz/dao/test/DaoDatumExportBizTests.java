/* ==================================================================
 * DaoDatumExportBizTests.java - 3/11/2023 3:47:51 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.biz.dao.test;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.dao.SecurityTokenDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.biz.dao.DaoDatumExportBiz;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDataConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicOutputConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportResult;
import net.solarnetwork.central.datum.export.domain.DatumExportStatus;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.support.BaseDatumExportDestinationService;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.dao.BasicBulkExportResult;
import net.solarnetwork.dao.BulkExportingDao.ExportCallback;
import net.solarnetwork.dao.BulkExportingDao.ExportOptions;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link DaoDatumExportBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DaoDatumExportBizTests {

	@Mock
	private DatumExportTaskInfoDao datumExportTaskInfoDao;

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private SecurityTokenDao securityTokenDao;

	@Mock
	private TaskScheduler scheduler;

	@Captor
	private ArgumentCaptor<ExportOptions> exportOptionsCaptor;

	private final DatumExportOutputFormatService csvOutput = new CsvDatumExportOutputFormatService();

	private static final DateTimeFormatter CSV_INSTANT_FORMAT = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	private TestDatumExportDestinationService destService;

	private DaoDatumExportBiz service;

	@BeforeEach
	public void setup() {
		service = new DaoDatumExportBiz(datumExportTaskInfoDao, datumDao, securityTokenDao, scheduler,
				new SimpleAsyncTaskExecutor(), null);
		service.setOutputFormatServices(Arrays.asList(csvOutput));

		destService = new TestDatumExportDestinationService();
		service.setDestinationServices(Arrays.asList(destService));
	}

	private static final class TestDatumExportDestinationService
			extends BaseDatumExportDestinationService {

		private final List<DatumExportResource> exports = new ArrayList<>(4);

		/**
		 * Test destination service.
		 */
		public TestDatumExportDestinationService() {
			super(randomString());
		}

		@Override
		public String getDisplayName() {
			return "Test Dest";
		}

		@Override
		public List<SettingSpecifier> getSettingSpecifiers() {
			return Collections.emptyList();
		}

		@Override
		public void export(Configuration config, Iterable<DatumExportResource> resources,
				Map<String, ?> runtimeProperties, ProgressListener<DatumExportService> progressListener)
				throws IOException {
			if ( resources != null ) {
				for ( DatumExportResource r : resources ) {
					exports.add(r);
				}
			}
		}

	}

	@Test
	public void performExport_adhoc_withToken_noPolicy() throws IOException {
		// GIVEN
		final Long nodeId = randomLong();
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { nodeId });
		filter.setLocalStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 1, 2, 0, 0));

		final BasicDataConfiguration dataConf = new BasicDataConfiguration();
		dataConf.setName(randomString());
		dataConf.setServiceIdentifier(randomString());
		dataConf.setDatumFilter(filter);

		final BasicOutputConfiguration outputConf = new BasicOutputConfiguration();
		outputConf.setName(randomString());
		outputConf.setServiceIdentifier(csvOutput.getId());

		final BasicDestinationConfiguration destConf = new BasicDestinationConfiguration();
		destConf.setName(randomString());
		destConf.setServiceIdentifier(destService.getId());

		final BasicConfiguration conf = new BasicConfiguration(randomString(), ScheduleType.Adhoc, 0);
		conf.setDataConfiguration(dataConf);
		conf.setOutputConfiguration(outputConf);
		conf.setDestinationConfiguration(destConf);

		final Long userId = randomLong();
		final String tokenId = randomString();
		final DatumExportTaskInfo req = new DatumExportTaskInfo();
		req.setId(UUID.randomUUID());
		req.setUserId(userId);
		req.setTokenId(tokenId);
		req.setExportDate(Instant.now());
		req.setConfig(conf);

		// look up token
		final SecurityToken auth = new AuthenticatedToken(
				new User(tokenId, "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.User, userId, null);
		given(securityTokenDao.securityTokenForId(req.getTokenId())).willReturn(auth);

		// export datum
		final GeneralNodeDatumMatch d = new GeneralNodeDatumMatch();
		d.setCreated(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		d.setNodeId(filter.getNodeId());
		d.setSourceId(randomString());
		d.setSamples(new DatumSamples(singletonMap("a", 1), null, null));

		final BasicBulkExportResult exportResult = new BasicBulkExportResult(1);
		given(datumDao.bulkExport(any(), any())).will(i -> {
			ExportCallback<GeneralNodeDatumFilterMatch> cb = i.getArgument(0);
			cb.didBegin(1L);
			cb.handle(d);
			return exportResult;
		});

		// WHEN
		DatumExportStatus result = service.performExport(req);

		// THEN
		then(result).as("Future status provided").isNotNull();
		then(result.getJobId()).as("Job ID assigned").isNotNull();

		// @formatter:off
		then(result)
			.as("Export completes")
			.succeedsWithin(Duration.ofDays(1))
			.as("Result is not null")
			.isNotNull()
			.as("Export was successfull")
			.returns(true, from(DatumExportResult::isSuccess))
			;

		then(destService.exports)
			.as("Single resource exported")
			.hasSize(1)
			.element(0)
			.as("CVS export content type")
			.returns("text/csv;charset=UTF-8", from(DatumExportResource::getContentType))
			;
		
		verify(datumDao).bulkExport(any(), exportOptionsCaptor.capture());
		then(exportOptionsCaptor.getValue())
			.as("Options provided")
			.isNotNull()
			.extracting(ExportOptions::getParameters, map(String.class, Object.class))
			.as("Datum criteria filter provided")
			.extractingByKey("filter", type(BasicDatumCriteria.class))
			.as("User included in filter")
			.returns(userId, from(BasicDatumCriteria::getUserId))
			.as("Node included in filter")
			.returns(new Long[] {nodeId}, from(BasicDatumCriteria::getNodeIds))
			;
		// @formatter:on

		String csv = FileCopyUtils.copyToString(new InputStreamReader(
				destService.exports.get(0).getInputStream(), StandardCharsets.UTF_8));
		then(csv).as("Exported data").isEqualTo("""
				created,nodeId,sourceId,localDate,localTime,a\r
				%s,%d,%s,,,1\r
				""".formatted(CSV_INSTANT_FORMAT.format(d.getCreated()), d.getNodeId(),
				d.getSourceId()));
	}

	@Test
	public void performExport_adhoc_withToken_withPolicy() throws IOException {
		// GIVEN
		final Long nodeId = randomLong();
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { nodeId });
		filter.setLocalStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 1, 2, 0, 0));

		final BasicDataConfiguration dataConf = new BasicDataConfiguration();
		dataConf.setName(randomString());
		dataConf.setServiceIdentifier(randomString());
		dataConf.setDatumFilter(filter);

		final BasicOutputConfiguration outputConf = new BasicOutputConfiguration();
		outputConf.setName(randomString());
		outputConf.setServiceIdentifier(csvOutput.getId());

		final BasicDestinationConfiguration destConf = new BasicDestinationConfiguration();
		destConf.setName(randomString());
		destConf.setServiceIdentifier(destService.getId());

		final BasicConfiguration conf = new BasicConfiguration(randomString(), ScheduleType.Adhoc, 0);
		conf.setDataConfiguration(dataConf);
		conf.setOutputConfiguration(outputConf);
		conf.setDestinationConfiguration(destConf);

		final Long userId = randomLong();
		final String tokenId = randomString();
		final DatumExportTaskInfo req = new DatumExportTaskInfo();
		req.setId(UUID.randomUUID());
		req.setUserId(userId);
		req.setTokenId(tokenId);
		req.setExportDate(Instant.now());
		req.setConfig(conf);

		// look up token
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(nodeId))
				.build();
		final SecurityToken auth = new AuthenticatedToken(
				new User(tokenId, "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.User, userId, policy);
		given(securityTokenDao.securityTokenForId(req.getTokenId())).willReturn(auth);

		// export datum
		final GeneralNodeDatumMatch d = new GeneralNodeDatumMatch();
		d.setCreated(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		d.setNodeId(filter.getNodeId());
		d.setSourceId(randomString());
		d.setSamples(new DatumSamples(singletonMap("a", 1), null, null));

		final BasicBulkExportResult exportResult = new BasicBulkExportResult(1);
		given(datumDao.bulkExport(any(), any())).will(i -> {
			ExportCallback<GeneralNodeDatumFilterMatch> cb = i.getArgument(0);
			cb.didBegin(1L);
			cb.handle(d);
			return exportResult;
		});

		// WHEN
		DatumExportStatus result = service.performExport(req);

		// THEN
		then(result).as("Future status provided").isNotNull();
		then(result.getJobId()).as("Job ID assigned").isNotNull();

		// @formatter:off
		then(result)
			.as("Export completes")
			.succeedsWithin(Duration.ofDays(1))
			.as("Result is not null")
			.isNotNull()
			.as("Export was successfull")
			.returns(true, from(DatumExportResult::isSuccess))
			;

		then(destService.exports)
			.as("Single resource exported")
			.hasSize(1)
			.element(0)
			.as("CVS export content type")
			.returns("text/csv;charset=UTF-8", from(DatumExportResource::getContentType))
			;

		verify(datumDao).bulkExport(any(), exportOptionsCaptor.capture());
		then(exportOptionsCaptor.getValue())
			.as("Options provided")
			.isNotNull()
			.extracting(ExportOptions::getParameters, map(String.class, Object.class))
			.as("Datum criteria filter provided")
			.extractingByKey("filter", type(BasicDatumCriteria.class))
			.as("User included in filter")
			.returns(userId, from(BasicDatumCriteria::getUserId))
			.as("Node included in filter")
			.returns(new Long[] {nodeId}, BasicDatumCriteria::getNodeIds)
			;		
		// @formatter:on

		String csv = FileCopyUtils.copyToString(new InputStreamReader(
				destService.exports.get(0).getInputStream(), StandardCharsets.UTF_8));
		then(csv).as("Exported data").isEqualTo("""
				created,nodeId,sourceId,localDate,localTime,a\r
				%s,%d,%s,,,1\r
				""".formatted(CSV_INSTANT_FORMAT.format(d.getCreated()), d.getNodeId(),
				d.getSourceId()));
	}

	@Test
	public void performExport_adhoc_withToken_withPolicy_allNodesExcluded() throws IOException {
		// GIVEN
		final Long nodeId = randomLong();
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { nodeId });
		filter.setLocalStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 1, 2, 0, 0));

		final BasicDataConfiguration dataConf = new BasicDataConfiguration();
		dataConf.setName(randomString());
		dataConf.setServiceIdentifier(randomString());
		dataConf.setDatumFilter(filter);

		final BasicOutputConfiguration outputConf = new BasicOutputConfiguration();
		outputConf.setName(randomString());
		outputConf.setServiceIdentifier(csvOutput.getId());

		final BasicDestinationConfiguration destConf = new BasicDestinationConfiguration();
		destConf.setName(randomString());
		destConf.setServiceIdentifier(destService.getId());

		final BasicConfiguration conf = new BasicConfiguration(randomString(), ScheduleType.Adhoc, 0);
		conf.setDataConfiguration(dataConf);
		conf.setOutputConfiguration(outputConf);
		conf.setDestinationConfiguration(destConf);

		final Long userId = randomLong();
		final String tokenId = randomString();
		final DatumExportTaskInfo req = new DatumExportTaskInfo();
		req.setId(UUID.randomUUID());
		req.setTokenId(tokenId);
		req.setExportDate(Instant.now());
		req.setConfig(conf);

		// look up token; policy does NOT include the node ID in the filter
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(randomLong()))
				.build();
		final SecurityToken auth = new AuthenticatedToken(
				new User(tokenId, "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.User, userId, policy);
		given(securityTokenDao.securityTokenForId(req.getTokenId())).willReturn(auth);

		// WHEN
		DatumExportStatus result = service.performExport(req);

		// THEN
		then(result).as("Future status provided").isNotNull();
		then(result.getJobId()).as("Job ID assigned").isNotNull();

		// @formatter:off
		then(result)
			.as("Export completes")
			.succeedsWithin(Duration.ofDays(1))
			.as("Result is not null")
			.isNotNull()
			.as("Export failed because of auth exception")
			.returns(false, from(DatumExportResult::isSuccess))
			.extracting(DatumExportResult::getMessage)
			.asInstanceOf(InstanceOfAssertFactories.STRING)
			.as("Message provided shows auth exception")
			.containsSubsequence("AuthorizationException")
			;

		then(destService.exports)
			.as("No resource exported")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void performExport_adhoc_withToken_withPolicy_someNodesExcluded() throws IOException {
		// GIVEN
		final Long nodeId1 = randomLong();
		final Long nodeId2 = randomLong();
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { nodeId1, nodeId2 });
		filter.setLocalStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 1, 2, 0, 0));

		final BasicDataConfiguration dataConf = new BasicDataConfiguration();
		dataConf.setName(randomString());
		dataConf.setServiceIdentifier(randomString());
		dataConf.setDatumFilter(filter);

		final BasicOutputConfiguration outputConf = new BasicOutputConfiguration();
		outputConf.setName(randomString());
		outputConf.setServiceIdentifier(csvOutput.getId());

		final BasicDestinationConfiguration destConf = new BasicDestinationConfiguration();
		destConf.setName(randomString());
		destConf.setServiceIdentifier(destService.getId());

		final BasicConfiguration conf = new BasicConfiguration(randomString(), ScheduleType.Adhoc, 0);
		conf.setDataConfiguration(dataConf);
		conf.setOutputConfiguration(outputConf);
		conf.setDestinationConfiguration(destConf);

		final Long userId = randomLong();
		final String tokenId = randomString();
		final DatumExportTaskInfo req = new DatumExportTaskInfo();
		req.setId(UUID.randomUUID());
		req.setTokenId(tokenId);
		req.setExportDate(Instant.now());
		req.setConfig(conf);
		req.setUserId(userId);

		// look up token; policy does NOT include the node ID in the filter
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(singleton(nodeId1))
				.build();
		final SecurityToken auth = new AuthenticatedToken(
				new User(tokenId, "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.User, userId, policy);
		given(securityTokenDao.securityTokenForId(req.getTokenId())).willReturn(auth);

		// export datum
		final GeneralNodeDatumMatch d = new GeneralNodeDatumMatch();
		d.setCreated(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		d.setNodeId(nodeId1);
		d.setSourceId(randomString());
		d.setSamples(new DatumSamples(singletonMap("a", 1), null, null));

		final BasicBulkExportResult exportResult = new BasicBulkExportResult(1);
		given(datumDao.bulkExport(any(), any())).will(i -> {
			ExportCallback<GeneralNodeDatumFilterMatch> cb = i.getArgument(0);
			cb.didBegin(1L);
			cb.handle(d);
			return exportResult;
		});

		// WHEN
		DatumExportStatus result = service.performExport(req);

		// THEN
		then(result).as("Future status provided").isNotNull();
		then(result.getJobId()).as("Job ID assigned").isNotNull();

		// @formatter:off
		then(result)
			.as("Export completes")
			.succeedsWithin(Duration.ofDays(1))
			.as("Result is not null")
			.isNotNull()
			.as("Export succeeded with filtered nodes")
			.returns(true, from(DatumExportResult::isSuccess))
			;

		then(destService.exports)
			.as("No resource exported")
			.hasSize(1)
			;

		verify(datumDao).bulkExport(any(), exportOptionsCaptor.capture());
		then(exportOptionsCaptor.getValue())
			.as("Options provided")
			.isNotNull()
			.extracting(ExportOptions::getParameters, map(String.class, Object.class))
			.as("Datum criteria filter provided")
			.extractingByKey("filter", type(BasicDatumCriteria.class))
			.as("User included in filter")
			.returns(userId, from(BasicDatumCriteria::getUserId))
			.as("Single node ID (filtered by policy)")
			.returns(new Long[] {nodeId1}, from(BasicDatumCriteria::getNodeIds))
			;
		// @formatter:on
	}

}
