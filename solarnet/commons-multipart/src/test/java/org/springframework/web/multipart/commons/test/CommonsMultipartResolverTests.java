/* ==================================================================
 * CommonsMultipartResolverTests.java - 17/09/2024 11:56:04 am
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

package org.springframework.web.multipart.commons.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.MultipartPart;
import org.apache.hc.client5.http.entity.mime.MultipartPartBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * Test cases for the {@link CommonsMultipartResolver} class
 *
 * @author matt
 * @version 1.0
 */
public class CommonsMultipartResolverTests extends BaseHttpIntegrationTests {

	private static final Logger log = LoggerFactory.getLogger(CommonsMultipartResolverTests.class);

	private List<Path> tempFiles;

	@BeforeEach
	public void setup() {
		tempFiles = new ArrayList<>(2);
	}

	@AfterEach
	public void teardown() {
		for ( Path p : tempFiles ) {
			try {
				Files.deleteIfExists(p);
			} catch ( IOException e ) {
				// ignore
			}
		}
		MultipartController.uploadHandler = null;
	}

	@Test
	public void transferTo_file() throws IOException {
		// GIVEN
		final String data = "Hello, world.";
		final StringBody dataBody = new StringBody(data, ContentType.TEXT_PLAIN.withCharset(UTF_8));
		final String fileName = "test.txt";

		// @formatter:off
		final MultipartPart dataPart = MultipartPartBuilder.create(dataBody)
				.addHeader(CONTENT_DISPOSITION, "form-data",
						Arrays.asList(
								new BasicNameValuePair("name", "data"),
								new BasicNameValuePair("filename", fileName)))
				.build();

		final org.apache.hc.core5.http.HttpEntity reqBody = MultipartEntityBuilder.create()
				.setLaxMode()
				.setContentType(ContentType.MULTIPART_FORM_DATA)
				.addPart(dataPart)
				.build();
		// @formatter:off

		final URI uri = serverUri("/upload").build().toUri();
		final Path tmp = Files.createTempFile("mp-upload-", null);
		tempFiles.add(tmp);

		MultipartController.uploadHandler = mpFile -> {
			try {
				mpFile.transferTo(tmp.toFile());
			} catch ( IOException | IllegalStateException e ) {
				throw new RuntimeException(e);
			}
		};

		// WHEN
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost post = new HttpPost(uri);
			post.setEntity(reqBody);
			client.execute(post, response -> {
				log.debug("Executed request [{}] and got response status {}", uri, response.getCode());
				return null;
			});
		}

		// THEN
		// @formatter:off
		then(tmp)
			.as("Multipart data transferred to temp file")
			.isNotEmptyFile()
			.as("Upload contents transferred")
			.hasBinaryContent(data.getBytes(UTF_8))
			;
		// @formatter:on
	}

	@Test
	public void transferTo_path() throws IOException {
		// GIVEN
		final String data = "Hello, world.";
		final StringBody dataBody = new StringBody(data, ContentType.TEXT_PLAIN.withCharset(UTF_8));
		final String fileName = "test.txt";

		// @formatter:off
		final MultipartPart dataPart = MultipartPartBuilder.create(dataBody)
				.addHeader(CONTENT_DISPOSITION, "form-data",
						Arrays.asList(
								new BasicNameValuePair("name", "data"),
								new BasicNameValuePair("filename", fileName)))
				.build();

		final org.apache.hc.core5.http.HttpEntity reqBody = MultipartEntityBuilder.create()
				.setLaxMode()
				.setContentType(ContentType.MULTIPART_FORM_DATA)
				.addPart(dataPart)
				.build();
		// @formatter:off

		final URI uri = serverUri("/upload").build().toUri();
		final Path tmp = Files.createTempFile("mp-upload-", null);
		tempFiles.add(tmp);

		MultipartController.uploadHandler = mpFile -> {
			try {
				mpFile.transferTo(tmp);
			} catch ( IOException | IllegalStateException e ) {
				throw new RuntimeException(e);
			}
		};

		// WHEN
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost post = new HttpPost(uri);
			post.setEntity(reqBody);
			client.execute(post, response -> {
				log.debug("Executed request [{}] and got response status {}", uri, response.getCode());
				return null;
			});
		}

		// THEN
		// @formatter:off
		then(tmp)
			.as("Multipart data transferred to temp file")
			.isNotEmptyFile()
			.as("Upload contents transferred")
			.hasBinaryContent(data.getBytes(UTF_8))
			;
		// @formatter:on
	}

}
