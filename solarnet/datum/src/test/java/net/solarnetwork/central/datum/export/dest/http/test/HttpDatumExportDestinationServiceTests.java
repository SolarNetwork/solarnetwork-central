/* ==================================================================
 * HttpDatumExportDestinationServiceTests.java - 20/03/2024 10:10:59 am
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

package net.solarnetwork.central.datum.export.dest.http.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.http.MultiPartFormData;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.export.dest.http.HttpDatumExportDestinationService;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.BasicDestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link HttpDatumExportDestinationService}.
 *
 * @author matt
 * @version 1.0
 */
public class HttpDatumExportDestinationServiceTests extends BaseHttpClientTests {

	private CloseableHttpClient client;
	private HttpDatumExportDestinationService service;

	@BeforeEach
	public void setup() throws Exception {
		client = HttpClients.createDefault();
		service = new HttpDatumExportDestinationService(client);
	}

	@AfterEach
	public void teardown() throws Exception {
		client.close();
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
			.containsExactly("method", "url", "username", "password", "multipartFilenameTemplate", "headersValue")
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
	public void basic() throws Exception {
		// GIVEN
		final Resource data = getTestDataResource();
		final String dataContent = FileCopyUtils
				.copyToString(new InputStreamReader(data.getInputStream(), UTF_8));
		final String method = "PUT";

		Handler handler = new Handler.Abstract.NonBlocking() {

			@Override
			public boolean handle(Request request, Response response, Callback callback)
					throws Exception {

				// @formatter:off
				then(request)
					.as("HTTP method from configuration")
					.returns(method, from(Request::getMethod))
					.as("HTTP path from URL")
					.returns("/save", from(r -> r.getHttpURI().getPath()))
					;
				// @formatter:on

				CompletableFuture<String> f = Content.Source.asStringAsync(request, UTF_8);
				f.whenComplete((content, failure) -> {
					if ( failure == null ) {
						// @formatter:off
						then(content)
							.as("Posted expected content")
							.isEqualTo(dataContent)
							;
						// @formatter:on
						callback.succeeded();
					} else {
						callback.failed(failure);
					}
				});

				return true;
			}
		};

		// WHEN
		Instant ts = LocalDateTime.of(2018, 4, 11, 11, 50).atZone(ZoneId.of("Pacific/Auckland"))
				.toInstant();

		BasicConfiguration config = new BasicConfiguration();
		config.setName(UUID.randomUUID().toString());

		BasicDestinationConfiguration destConfig = new BasicDestinationConfiguration();
		destConfig.setServiceIdentifier(service.getId());
		Map<String, Object> destProps = new HashMap<>();
		destProps.put("method", method);
		destConfig.setServiceProps(destProps);
		config.setDestinationConfiguration(destConfig);

		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setConfig(config);
		taskInfo.setId(UUID.randomUUID());
		taskInfo.setExportDate(ts);
		Map<String, Object> runtimeProps = config.createRuntimeProperties(taskInfo, null,
				new CsvDatumExportOutputFormatService());

		DatumExportResource rsrc = getExportResource(data);

		doWithHttpServer(handler, (server, port, baseUrl) -> {
			destProps.put("url", baseUrl + "/save");
			service.export(config, singleton(rsrc), runtimeProps, null);
			return null;
		});
	}

	@Test
	public void parameters() throws Exception {
		// GIVEN
		final UUID jobId = UUID.randomUUID();
		final String jobName = "My: Job";
		final Resource data = getTestDataResource();
		final String dataContent = FileCopyUtils
				.copyToString(new InputStreamReader(data.getInputStream(), UTF_8));
		final String method = "POST";

		final String jobNameUrlEncoded = URLEncoder.encode(jobName, UTF_8).replace("+", "%20");

		Handler handler = new Handler.Abstract.NonBlocking() {

			@Override
			public boolean handle(Request request, Response response, Callback callback)
					throws Exception {

				// @formatter:off
				then(request)
					.as("HTTP method from configuration")
					.returns(method, from(Request::getMethod))
					.as("HTTP path from URL with {name} parameter resolved to job name, URL encoded")
					.returns("/save/%s".formatted(jobNameUrlEncoded), from(r -> r.getHttpURI().getPath()))
					;

				HttpFields headers = request.getHeaders();
				then(headers.get("x-job-id"))
					.as("Custom header resolved with {id} parameter")
					.isEqualTo(jobId.toString())
					;
				then(headers.get("x-export-date"))
					.as("Custom header resolved with {date} parameter")
					.isEqualTo("2018-04-10")
					;
				then(headers.get("x-other-thing"))
					.as("Custom header resolved")
					.isEqualTo("abc123")
					;

				// @formatter:on

				CompletableFuture<String> f = Content.Source.asStringAsync(request, UTF_8);
				f.whenComplete((content, failure) -> {
					if ( failure == null ) {
						// @formatter:off
						then(content)
							.as("Posted expected content")
							.isEqualTo(dataContent)
							;
						// @formatter:on
						callback.succeeded();
					} else {
						callback.failed(failure);
					}
				});

				return true;
			}
		};

		// WHEN
		Instant ts = LocalDateTime.of(2018, 4, 11, 11, 50).atZone(ZoneId.of("Pacific/Auckland"))
				.toInstant();

		BasicConfiguration config = new BasicConfiguration();
		config.setName(jobName);

		Map<String, Object> destProps = new HashMap<>();
		destProps.put("method", method);
		destProps.put("headersValue", """
				x-job-id: {id},
				x-export-date: {date},
				x-other-thing: abc123
				""");

		BasicDestinationConfiguration destConfig = new BasicDestinationConfiguration();
		destConfig.setServiceIdentifier(service.getId());
		destConfig.setServiceProps(destProps);
		config.setDestinationConfiguration(destConfig);

		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setConfig(config);
		taskInfo.setId(jobId);
		taskInfo.setExportDate(ts);
		Map<String, Object> runtimeProps = config.createRuntimeProperties(taskInfo, null,
				new CsvDatumExportOutputFormatService());

		DatumExportResource rsrc = getExportResource(data);

		doWithHttpServer(handler, (server, port, baseUrl) -> {
			destProps.put("url", baseUrl + "/save/{name}");
			service.export(config, singleton(rsrc), runtimeProps, null);
			return null;
		});
	}

	@Test
	public void multipart() throws Exception {
		// GIVEN
		final UUID jobId = UUID.randomUUID();
		final Resource data = getTestDataResource();
		final String dataContent = FileCopyUtils
				.copyToString(new InputStreamReader(data.getInputStream(), UTF_8));
		final String method = "POST";
		final Path tmpDir = Files.createTempDirectory("multipart-");
		final DatumExportResource rsrc = getExportResource(data);

		Handler handler = new Handler.Abstract.NonBlocking() {

			@Override
			public boolean handle(Request request, Response response, Callback callback)
					throws Exception {

				HttpFields headers = request.getHeaders();
				String boundary = MultiPart.extractBoundary(headers.get(HttpHeader.CONTENT_TYPE));

				// @formatter:off
				then(request)
					.as("HTTP method from configuration")
					.returns(method, from(Request::getMethod))
					.as("HTTP path from URL")
					.returns("/save", from(r -> r.getHttpURI().getPath()))
					;

				then(boundary)
					.as("Request is multipart")
					.isNotNull()
					;
				// @formatter:on

				MultiPartFormData.Parser formData = new MultiPartFormData.Parser(boundary);
				formData.setFilesDirectory(tmpDir);

				try {
					process(formData.parse(request).join());
					callback.succeeded();
				} catch ( Exception x ) {
					Response.writeError(request, response, callback, x);
				}

				return true;
			}

			private void process(MultiPartFormData.Parts parts) throws IOException {
				ByteArrayOutputStream body = new ByteArrayOutputStream();

				for ( MultiPart.Part part : parts ) {
					String fileName = part.getFileName();
					HttpFields headers = part.getHeaders();
					String contentType = headers.get(HttpHeader.CONTENT_TYPE);

					// @formatter:off
					then(fileName)
						.as("Part filename resolved with parameters")
						.isEqualTo("data-export-2018-04-10.csv")
						;

					then(ContentType.parse(contentType).toString())
						.as("Part content type from export resource")
						.isEqualTo(ContentType.parse(rsrc.getContentType()).toString())
						;
					// @formatter:on

					try (InputStream inputStream = Content.Source
							.asInputStream(part.getContentSource())) {
						IO.copy(inputStream, body);
					}

					// @formatter:off
					then(body.toString(UTF_8))
						.as("Posted multipart data")
						.isEqualTo(dataContent)
						;
					// @formatter:on
				}
			}
		};

		// WHEN
		Instant ts = LocalDateTime.of(2018, 4, 11, 11, 50).atZone(ZoneId.of("Pacific/Auckland"))
				.toInstant();

		BasicConfiguration config = new BasicConfiguration();
		config.setName(UUID.randomUUID().toString());

		Map<String, Object> destProps = new HashMap<>();
		destProps.put("method", method);
		destProps.put("multipartFilenameTemplate", "data-export-{date}.{ext}");

		BasicDestinationConfiguration destConfig = new BasicDestinationConfiguration();
		destConfig.setServiceIdentifier(service.getId());
		destConfig.setServiceProps(destProps);
		config.setDestinationConfiguration(destConfig);

		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setConfig(config);
		taskInfo.setId(jobId);
		taskInfo.setExportDate(ts);
		Map<String, Object> runtimeProps = config.createRuntimeProperties(taskInfo, null,
				new CsvDatumExportOutputFormatService());

		doWithHttpServer(handler, (server, port, baseUrl) -> {
			destProps.put("url", baseUrl + "/save");
			service.export(config, singleton(rsrc), runtimeProps, null);
			return null;
		});
	}

}
