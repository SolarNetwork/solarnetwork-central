/* ==================================================================
 * InstructionInputControllerTests.java - 9/05/2024 1:37:21 pm
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

package net.solarnetwork.central.din.app.web.api.test;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeType;
import org.springframework.web.context.request.WebRequest;
import net.solarnetwork.central.din.app.web.api.InstructionInputController;
import net.solarnetwork.central.inin.biz.InstructionInputEndpointBiz;
import net.solarnetwork.central.inin.security.AuthenticatedEndpointCredentials;
import net.solarnetwork.central.inin.security.SecurityUtils;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.web.MaxUploadSizeInputStream;
import net.solarnetwork.io.ProvidedOutputStream;

/**
 * Test cases for the {@link InstructionInputController}.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class InstructionInputControllerTests {

	private static final long MAX_INPUT_LENGTH = new SecureRandom().nextLong(4096, 8192);

	@Mock
	private InstructionInputEndpointBiz inputBiz;

	@Mock
	private WebRequest request;

	@Captor
	private ArgumentCaptor<InputStream> requestInputStreamCaptor;

	@Captor
	private ArgumentCaptor<OutputStream> responseOutputStreamCaptor;
	@Captor
	private ArgumentCaptor<Map<String, String>> responseParametersCaptor;

	private InstructionInputController controller;

	@BeforeEach
	public void setup() {
		controller = new InstructionInputController(inputBiz, MAX_INPUT_LENGTH);
	}

	private void becomeEndpoint(Long userId, UUID endpointId) {
		final String username = randomString();
		final String password = randomString();

		AuthenticatedEndpointCredentials user = new AuthenticatedEndpointCredentials(userId, endpointId,
				username, password, true, false, false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, username,
				SecurityUtils.ROLE_ININ);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Test
	public void response() throws IOException {
		// GIVEN
		final Long userId = CommonTestUtils.randomLong();
		final Long nodeId = CommonTestUtils.randomLong();
		final UUID endpointId = UUID.randomUUID();
		final String contentType = MediaType.APPLICATION_JSON_VALUE;
		final String contentEncoding = null;
		final String acceptEncoding = null;

		final NodeInstruction instr = new NodeInstruction(randomString(), Instant.now(), nodeId);
		final List<NodeInstruction> instrs = singletonList(instr);

		// no request parameters
		given(request.getParameterNames()).willReturn(emptyIterator());

		// import instruction
		final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
		given(inputBiz.importInstructions(eq(userId), eq(endpointId), eq(MediaType.APPLICATION_JSON),
				any(), eq(emptyMap()))).willReturn(instrs);

		// process instruction response
		inputBiz.generateResponse(eq(userId), eq(endpointId), eq(instrs), eq(MediaType.APPLICATION_JSON),
				any(), any());

		final MockHttpServletResponse response = new MockHttpServletResponse();

		// WHEN
		final String accept = "application/%s+json".formatted(randomString());
		becomeEndpoint(userId, endpointId);
		controller.postInstruction(endpointId, contentType, contentEncoding, request, in, accept,
				acceptEncoding, response);

		// THEN
		// @formatter:off
		then(inputBiz).should().importInstructions(eq(userId), eq(endpointId), eq(MediaType.APPLICATION_JSON), requestInputStreamCaptor.capture(),
				eq(emptyMap()));
		and.then(requestInputStreamCaptor.getValue())
			.as("Is size-limited input stream")
			.asInstanceOf(type(MaxUploadSizeInputStream.class))
			.returns(MAX_INPUT_LENGTH, from(BoundedInputStream::getMaxCount))
			;

		then(inputBiz).should().generateResponse(eq(userId), eq(endpointId), eq(instrs),
				eq(MimeType.valueOf(accept)), responseOutputStreamCaptor.capture(),
				responseParametersCaptor.capture());

		final String output = "Hello, %s".formatted(randomString());
		and.then(responseOutputStreamCaptor.getValue())
			.as("Provided output stream")
			.asInstanceOf(type(ProvidedOutputStream.class))
			.satisfies((pos) -> {
				// generate output
				pos.write(output.getBytes(StandardCharsets.UTF_8));
				pos.close();
			})
			;

		and.then(responseParametersCaptor.getValue())
			.as("Expected output parameters")
			.hasSize(0)
			;

		and.then(response.getHeader(HttpHeaders.CONTENT_TYPE))
			.as("Response content type from accept header")
			.isEqualTo(accept)
			;
		and.then(response.getHeader(HttpHeaders.CONTENT_ENCODING))
			.as("No response encoding")
			.isNull()
			;

		and.then(response.getContentAsByteArray())
			.as("Response content compressed")
			.containsExactly(output.getBytes(StandardCharsets.UTF_8))
			;
		// @formatter:on
	}

	@Test
	public void gzipResponse() throws IOException {
		// GIVEN
		final Long userId = CommonTestUtils.randomLong();
		final Long nodeId = CommonTestUtils.randomLong();
		final UUID endpointId = UUID.randomUUID();
		final String contentType = MediaType.APPLICATION_JSON_VALUE;
		final String contentEncoding = null;
		final String acceptEncoding = "gzip";

		final NodeInstruction instr = new NodeInstruction(randomString(), Instant.now(), nodeId);
		final List<NodeInstruction> instrs = singletonList(instr);

		// no request parameters
		given(request.getParameterNames()).willReturn(emptyIterator());

		// import instruction
		final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
		given(inputBiz.importInstructions(eq(userId), eq(endpointId), eq(MediaType.APPLICATION_JSON),
				any(), eq(emptyMap()))).willReturn(instrs);

		// process instruction response
		inputBiz.generateResponse(eq(userId), eq(endpointId), eq(instrs), eq(MediaType.APPLICATION_JSON),
				any(), any());

		final MockHttpServletResponse response = new MockHttpServletResponse();

		// WHEN
		final String accept = "application/%s+json".formatted(randomString());
		becomeEndpoint(userId, endpointId);
		controller.postInstruction(endpointId, contentType, contentEncoding, request, in, accept,
				acceptEncoding, response);

		// THEN
		// @formatter:off
		then(inputBiz).should().importInstructions(eq(userId), eq(endpointId), eq(MediaType.APPLICATION_JSON), requestInputStreamCaptor.capture(),
				eq(emptyMap()));
		and.then(requestInputStreamCaptor.getValue())
			.as("Is size-limited input stream")
			.asInstanceOf(type(MaxUploadSizeInputStream.class))
			.returns(MAX_INPUT_LENGTH, from(BoundedInputStream::getMaxCount))
			;

		then(inputBiz).should().generateResponse(eq(userId), eq(endpointId), eq(instrs),
				eq(MimeType.valueOf(accept)), responseOutputStreamCaptor.capture(),
				responseParametersCaptor.capture());

		final String output = "Hello, %s".formatted(randomString());
		and.then(responseOutputStreamCaptor.getValue())
			.as("Provided output stream")
			.asInstanceOf(type(ProvidedOutputStream.class))
			.satisfies((pos) -> {
				// generate output
				pos.write(output.getBytes(StandardCharsets.UTF_8));
				pos.close();
			})
			;

		and.then(responseParametersCaptor.getValue())
			.as("Expected output parameters")
			.hasSize(0)
			;

		and.then(response.getHeader(HttpHeaders.CONTENT_TYPE))
			.as("Response content type from accept header")
			.isEqualTo(accept)
			;
		and.then(response.getHeader(HttpHeaders.CONTENT_ENCODING))
			.as("gzip response encoding")
			.isEqualTo("gzip")
			;

		final var expectedResponseContent = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(expectedResponseContent)) {
			out.write(output.getBytes(StandardCharsets.UTF_8));
		}

		and.then(response.getContentAsByteArray())
			.as("Response content compressed")
			.containsExactly(expectedResponseContent.toByteArray())
			;
		// @formatter:on
	}

}
