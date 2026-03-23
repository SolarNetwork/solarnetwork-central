/* ==================================================================
 * FtpDatumExportDesintationServiceTests.java - 25/03/2024 2:11:16 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.dest.ftp.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.export.dest.ftp.FtpDatumExportDestinationService;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.BasicDestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link FtpDatumExportDestinationService} class.
 *
 * @author matt
 * @version 1.0
 */
public class FtpDatumExportDesintationServiceTests {

	private FileSystem ftpServerFileSystem;
	private FakeFtpServer ftpServer;

	private FtpDatumExportDestinationService service;

	@BeforeEach
	public void setup() throws IOException {
		ftpServer = new FakeFtpServer();

		ftpServerFileSystem = new UnixFakeFileSystem();
		ftpServerFileSystem.add(new DirectoryEntry("/"));
		ftpServer.setFileSystem(ftpServerFileSystem);
		ftpServer.setServerControlPort(0);

		ftpServer.start();

		service = new FtpDatumExportDestinationService();
	}

	@AfterEach
	public void teardown() throws IOException {
		ftpServer.stop();
	}

	@Test
	public void settingSpecifiers() {
		// GIVEN

		// WHEN
		List<SettingSpecifier> specs = service.getSettingSpecifiers();

		// THEN
		// @formatter:off
		then(specs)
			.map((s) -> ((KeyedSettingSpecifier<?>)s).getKey())
			.as("Expected settings provided")
			.containsExactly("url", "username", "password", "implicitTls", "dataTls")
			;
		// @formatter:on
	}

	private Resource getTestDataResource() {
		return new ClassPathResource("test-datum-export-01.txt", getClass());
	}

	private DatumExportResource getExportResource(Resource r) {
		return new BasicDatumExportResource(r, "text/plain;charset=UTF-8");
	}

	@Test
	public void upload_withCredentials() throws IOException {
		// GIVEN
		final String username = randomString();
		final String password = randomString();
		ftpServer.addUserAccount(new UserAccount(username, password, "/"));

		final Resource data = getTestDataResource();

		// WHEN
		Instant ts = LocalDateTime.of(2018, 4, 11, 11, 50).atZone(ZoneId.of("Pacific/Auckland"))
				.toInstant();

		BasicConfiguration config = new BasicConfiguration();
		config.setName(UUID.randomUUID().toString());

		BasicDestinationConfiguration destConfig = new BasicDestinationConfiguration();
		destConfig.setServiceIdentifier(service.getId());
		Map<String, Object> destProps = new HashMap<>();
		destProps.put("url",
				"ftp://localhost:" + ftpServer.getServerControlPort() + "/export-{date}.{ext}");
		destProps.put("username", username);
		destProps.put("password", password);
		destConfig.setServiceProps(destProps);
		config.setDestinationConfiguration(destConfig);

		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setConfig(config);
		taskInfo.setId(UUID.randomUUID());
		taskInfo.setExportDate(ts);
		Map<String, Object> runtimeProps = config.createRuntimeProperties(taskInfo, null,
				new CsvDatumExportOutputFormatService());

		DatumExportResource rsrc = getExportResource(data);

		service.export(config, singleton(rsrc), runtimeProps, null);

		// THEN
		FileSystemEntry serverFile = ftpServer.getFileSystem().getEntry("/export-2018-04-10.csv");
		final String dataContent = FileCopyUtils
				.copyToString(new InputStreamReader(data.getInputStream(), UTF_8));

		// @formatter:off
		then(serverFile)
			.as("File exists on server")
			.isNotNull()
			.as("Size is full content")
			.returns((long)dataContent.length(), from(FileSystemEntry::getSize))
			.as("Is file")
			.asInstanceOf(type(FileEntry.class))
			.as("Content is upload data")
			.returns(dataContent, from(f -> {
				try {
					return FileCopyUtils.copyToString(new InputStreamReader(f.createInputStream(), StandardCharsets.UTF_8));
				} catch (IOException e)  {
					return "";
				}
			}))
			;
		// @formatter:on
	}

	@Test
	public void upload_badCredentials() throws IOException {
		// GIVEN
		final String username = randomString();
		final String password = randomString();
		ftpServer.addUserAccount(new UserAccount(username, "NOPE", "/"));

		final Resource data = getTestDataResource();

		// WHEN
		Instant ts = LocalDateTime.of(2018, 4, 11, 11, 50).atZone(ZoneId.of("Pacific/Auckland"))
				.toInstant();

		BasicConfiguration config = new BasicConfiguration();
		config.setName(UUID.randomUUID().toString());

		BasicDestinationConfiguration destConfig = new BasicDestinationConfiguration();
		destConfig.setServiceIdentifier(service.getId());
		Map<String, Object> destProps = new HashMap<>();
		destProps.put("url",
				"ftp://localhost:" + ftpServer.getServerControlPort() + "/export-{date}.{ext}");
		destProps.put("username", username);
		destProps.put("password", password);
		destConfig.setServiceProps(destProps);
		config.setDestinationConfiguration(destConfig);

		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setConfig(config);
		taskInfo.setId(UUID.randomUUID());
		taskInfo.setExportDate(ts);
		Map<String, Object> runtimeProps = config.createRuntimeProperties(taskInfo, null,
				new CsvDatumExportOutputFormatService());

		DatumExportResource rsrc = getExportResource(data);

		// WHEN
		thenThrownBy(() -> {
			service.export(config, singleton(rsrc), runtimeProps, null);
		}).isInstanceOf(IOException.class).hasMessage("Password rejected by server.");
	}

}
