/* ==================================================================
 * SecurityTokenAuthenticationFilter_HttpSignatureTests.java - 19/09/2026 2:31:09 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.web.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.AntPathMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.security.web.SecurityTokenAuthenticationFilter;
import net.solarnetwork.central.security.web.config.SecurityTokenFilterSettings;
import net.solarnetwork.security.http.sig.ContentDigest;
import net.solarnetwork.security.http.sig.HttpSignatureBuilder;
import net.solarnetwork.security.http.sig.HttpSignatureFields;
import net.solarnetwork.security.http.sig.SignatureComponent;
import net.solarnetwork.web.jakarta.security.HttpSignatureAuthenticationEntryPoint;

/**
 * Test cases for the RFC 9421 HTTP Message Signatures support in the
 * {@link SecurityTokenAuthenticationFilter} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityTokenAuthenticationFilter_HttpSignatureTests {

	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_PASSWORD = "lsdjfpse9jfoeijfe09j";
	private static final String TEST_HOST = "host.example.com";
	private static final String TEST_PATH = "/mock/path/here";

	@Mock
	private FilterChain filterChain;

	@Mock
	private UserDetailsService userDetailsService;

	private MockHttpServletResponse response;
	private SecurityTokenFilterSettings settings;
	private SecurityTokenAuthenticationFilter filter;
	private User userDetails;

	@BeforeEach
	public void setup() {
		SecurityContextHolder.clearContext();
		response = new MockHttpServletResponse();
		settings = new SecurityTokenFilterSettings();
		userDetails = new User(TEST_AUTH_TOKEN, TEST_PASSWORD,
				List.of(new SimpleGrantedAuthority("ROLE_TEST")));
		given(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).willReturn(userDetails);
		filter = new SecurityTokenAuthenticationFilter(userDetailsService,
				new HttpSignatureAuthenticationEntryPoint(settings.getHttpSignatures()), null,
				new AntPathMatcher(), "/mock", settings);
	}

	private static MockHttpServletRequest request(String method, String path,
			@org.jspecify.annotations.Nullable String queryString) {
		final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setScheme("https");
		request.setSecure(true);
		request.setServerName(TEST_HOST);
		request.setServerPort(443);
		request.addHeader("Host", TEST_HOST);
		if ( queryString != null ) {
			request.setQueryString(queryString);
		}
		return request;
	}

	private static HttpSignatureBuilder builder(MockHttpServletRequest request) {
		final String query = request.getQueryString();
		// @formatter:off
		return new HttpSignatureBuilder(TEST_AUTH_TOKEN)
				.label("sn")
				.method(request.getMethod())
				.uri("https://" + TEST_HOST + request.getRequestURI()
						+ (query != null ? "?" + query : ""))
				.tag("solarnetwork")
				.created(Instant.now())
				;
		// @formatter:on
	}

	private static void sign(MockHttpServletRequest request, HttpSignatureBuilder builder) {
		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER,
				builder.signatureInputHeaderValue());
		request.addHeader(HttpSignatureFields.SIGNATURE_HEADER,
				builder.signatureHeaderValue(TEST_PASSWORD));
	}

	private void thenAuthenticated() {
		// @formatter:off
		then(response.getStatus())
			.as("Request is not rejected")
			.isEqualTo(HttpServletResponse.SC_OK)
			;
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		then(auth)
			.as("Authentication established")
			.isNotNull()
			;
		then(auth.getName())
			.as("Authenticated as the token")
			.isEqualTo(TEST_AUTH_TOKEN)
			;
		// @formatter:on
	}

	private void thenRejected(String messageFragment) throws Exception {
		// @formatter:off
		then(response.getStatus())
			.as("Request is rejected as unauthorized")
			.isEqualTo(HttpServletResponse.SC_UNAUTHORIZED)
			;
		then(response.getHeader("X-SN-ErrorMessage"))
			.as("Rejection reason names the problem")
			.contains(messageFragment)
			;
		then(SecurityContextHolder.getContext().getAuthentication())
			.as("No authentication established")
			.isNull()
			;
		// @formatter:on
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	public void validSignature() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
				SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
		verify(filterChain).doFilter(any(HttpServletRequest.class), same(response));
		// @formatter:off
		then(request.getHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER))
			.as("A derived signing key is used by default")
			.containsPattern("keyid=\"" + TEST_AUTH_TOKEN + ":\\d{8}\"")
			;
		// @formatter:on
	}

	@Test
	public void validSignature_withQueryString() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, "foo=bar&a=1");
		sign(request, builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
				SignatureComponent.PATH, SignatureComponent.QUERY));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
	}

	@Test
	public void validSignature_targetUriSatisfiesAuthorityPathAndQuery()
			throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, "foo=bar");
		sign(request,
				builder(request).covered(SignatureComponent.METHOD, SignatureComponent.TARGET_URI));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
	}

	@Test
	public void validSignature_directSecretKey() throws ServletException, IOException {
		// GIVEN
		// the other tests sign with the derived signing key the builder now defaults
		// to, so this covers a client that signs with the token secret itself
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		// @formatter:off
		sign(request, builder(request)
				.derivedSigningKey(false)
				.covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
						SignatureComponent.PATH))
				;
		// @formatter:on

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
		// @formatter:off
		then(request.getHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER))
			.as("The signature was made with a bare token ID key ID")
			.contains("keyid=\"" + TEST_AUTH_TOKEN + "\"")
			;
		// @formatter:on
	}

	@Test
	public void validSignature_postWithContentDigest() throws ServletException, IOException {
		// GIVEN
		final byte[] content = "{\"hello\":\"world\"}".getBytes(UTF_8);
		final MockHttpServletRequest request = request("POST", TEST_PATH, null);
		request.setContentType("application/json");
		request.setContent(content);
		final String digest = ContentDigest.fieldValue(ContentDigest.SHA_256, content);
		request.addHeader(ContentDigest.CONTENT_DIGEST_HEADER, digest);

		// @formatter:off
		sign(request, builder(request)
				.header("Content-Type", "application/json")
				.header(ContentDigest.CONTENT_DIGEST_HEADER, digest)
				.covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
						SignatureComponent.PATH, "content-type", "content-digest"))
				;
		// @formatter:on

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
	}

	@Test
	public void validSignature_postWithContentDigest_contentInputStream()
			throws ServletException, IOException {
		// GIVEN
		final byte[] content = "{\"hello\":\"world\"}".getBytes(UTF_8);
		final MockHttpServletRequest request = request("POST", TEST_PATH, null);
		request.setContentType("application/json");
		request.setContent(content);
		final String digest;
		try (var contentInput = new ByteArrayInputStream(content)) {
			digest = ContentDigest.fieldValue(ContentDigest.SHA_256, contentInput);
			request.addHeader(ContentDigest.CONTENT_DIGEST_HEADER, digest);
		}

		// @formatter:off
		sign(request, builder(request)
				.header("Content-Type", "application/json")
				.header(ContentDigest.CONTENT_DIGEST_HEADER, digest)
				.covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
						SignatureComponent.PATH, "content-type", "content-digest"))
				;
		// @formatter:on

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
	}

	@Test
	public void validSignature_multipleSignatures_tagSelectsOurs() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		final HttpSignatureBuilder ours = builder(request).covered(SignatureComponent.METHOD,
				SignatureComponent.AUTHORITY, SignatureComponent.PATH);
		// a proxy signature, covering less and tagged for something else
		// @formatter:off
		final HttpSignatureBuilder proxy = new HttpSignatureBuilder("proxy-key")
				.label("proxy_sig")
				.method("GET")
				.uri("https://" + TEST_HOST + TEST_PATH)
				.tag("proxy")
				.covered(SignatureComponent.AUTHORITY)
				;
		// @formatter:on

		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER,
				proxy.signatureInputHeaderValue() + ", " + ours.signatureInputHeaderValue());
		request.addHeader(HttpSignatureFields.SIGNATURE_HEADER,
				proxy.signatureHeaderValue("proxy-secret") + ", "
						+ ours.signatureHeaderValue(TEST_PASSWORD));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
	}

	@Test
	public void badSignature() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		final HttpSignatureBuilder builder = builder(request).covered(SignatureComponent.METHOD,
				SignatureComponent.AUTHORITY, SignatureComponent.PATH);
		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER,
				builder.signatureInputHeaderValue());
		request.addHeader(HttpSignatureFields.SIGNATURE_HEADER,
				builder.signatureHeaderValue("not-the-token-secret"));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("Bad credentials");
	}

	@Test
	public void tamperedPath() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		final HttpSignatureBuilder builder = builder(request).covered(SignatureComponent.METHOD,
				SignatureComponent.AUTHORITY, SignatureComponent.PATH);
		sign(request, builder);
		request.setRequestURI("/mock/path/elsewhere");

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("Bad credentials");
	}

	@Test
	public void missingMethodCoverage() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).covered(SignatureComponent.AUTHORITY, SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("[@method] component must be covered");
	}

	@Test
	public void missingQueryCoverage_whenQueryPresent() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, "nodeId=123");
		sign(request, builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
				SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("[@query] component must be covered");
	}

	@Test
	public void missingContentDigestCoverage_whenBodyPresent() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("POST", TEST_PATH, null);
		request.setContentType("application/json");
		request.setContent("{\"hello\":\"world\"}".getBytes(UTF_8));
		sign(request,
				builder(request).header("Content-Type", "application/json").covered(
						SignatureComponent.METHOD, SignatureComponent.AUTHORITY, SignatureComponent.PATH,
						"content-type"));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("[content-digest] component must be covered");
	}

	@Test
	public void contentDigestMismatch() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("POST", TEST_PATH, null);
		request.setContentType("application/json");
		request.setContent("{\"hello\":\"world\"}".getBytes(UTF_8));
		final String digest = ContentDigest.fieldValue(ContentDigest.SHA_256,
				"{\"hello\":\"mars\"}".getBytes(UTF_8));
		request.addHeader(ContentDigest.CONTENT_DIGEST_HEADER, digest);
		sign(request,
				builder(request).header("Content-Type", "application/json")
						.header(ContentDigest.CONTENT_DIGEST_HEADER, digest)
						.covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
								SignatureComponent.PATH, "content-type", "content-digest"));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("does not match the request content");
	}

	@Test
	public void missingSnHeaderCoverage() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		request.addHeader("X-SN-Date", "Tue, 20 Apr 2021 02:07:55 GMT");
		sign(request, builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
				SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("[x-sn-date] component must be covered");
	}

	@Test
	public void dateSkewTooLarge() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).created(Instant.now().minus(1, ChronoUnit.HOURS)).covered(
				SignatureComponent.METHOD, SignatureComponent.AUTHORITY, SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("Date skew too large");
	}

	@Test
	public void expiredSignature() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).expires(Instant.now().minus(1, ChronoUnit.MINUTES)).covered(
				SignatureComponent.METHOD, SignatureComponent.AUTHORITY, SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("expired");
	}

	@Test
	public void trailerParameterRejected() throws Exception {
		// GIVEN
		// the builder will not sign a trailer component, so the fields are hand-crafted
		// here the way a non-SolarNetwork client could send them
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		request.addHeader("X-Thing", "value");
		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER, """
				sn=("@method" "@authority" "@path" "x-thing";tr);created=%d;keyid="%s"\
				;tag="solarnetwork\"""".formatted(Instant.now().getEpochSecond(), TEST_AUTH_TOKEN));
		request.addHeader(HttpSignatureFields.SIGNATURE_HEADER, "sn=:AAAAAAAAAAAAAAAAAAAAAA==:");

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("trailer");
	}

	@Test
	public void untaggedSignatures_ambiguous() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		final HttpSignatureBuilder a = builder(request).tag(null).label("a").covered(
				SignatureComponent.METHOD, SignatureComponent.AUTHORITY, SignatureComponent.PATH);
		final HttpSignatureBuilder b = builder(request).tag(null).label("b").covered(
				SignatureComponent.METHOD, SignatureComponent.AUTHORITY, SignatureComponent.PATH);
		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER,
				a.signatureInputHeaderValue() + ", " + b.signatureInputHeaderValue());
		request.addHeader(HttpSignatureFields.SIGNATURE_HEADER,
				a.signatureHeaderValue(TEST_PASSWORD) + ", " + b.signatureHeaderValue(TEST_PASSWORD));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("tag the one to authenticate with");
	}

	@Test
	public void singleUntaggedSignature_accepted() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).tag(null).covered(SignatureComponent.METHOD,
				SignatureComponent.AUTHORITY, SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenAuthenticated();
	}

	@Test
	public void disabled_signatureIgnored() throws ServletException, IOException {
		// GIVEN
		settings.getHttpSignatures().setEnabled(false);
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
				SignatureComponent.PATH));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		// @formatter:off
		then(SecurityContextHolder.getContext().getAuthentication())
			.as("No authentication established when the scheme is disabled")
			.isNull()
			;
		// @formatter:on
		verifyNoInteractions(userDetailsService);
		verify(filterChain).doFilter(any(HttpServletRequest.class), same(response));
	}

	@Test
	public void missingSignatureField() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER,
				builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
						SignatureComponent.PATH).signatureInputHeaderValue());

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("Signature HTTP field is missing");
	}

	@Test
	public void unsupportedAlgorithmRejected() throws Exception {
		// GIVEN
		// the builder only signs with hmac-sha256, so the fields are hand-crafted here
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		request.addHeader(HttpSignatureFields.SIGNATURE_INPUT_HEADER, """
				sn=("@method" "@authority" "@path");created=%d;alg="rsa-pss-sha512"\
				;keyid="%s";tag="solarnetwork\"""".formatted(Instant.now().getEpochSecond(),
				TEST_AUTH_TOKEN));
		request.addHeader(HttpSignatureFields.SIGNATURE_HEADER, "sn=:AAAAAAAAAAAAAAAAAAAAAA==:");

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("rsa-pss-sha512");
	}

	@Test
	public void tamperedQuery_withTargetUriCoverage() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, "nodeId=123");
		sign(request,
				builder(request).covered(SignatureComponent.METHOD, SignatureComponent.TARGET_URI));
		request.setQueryString("nodeId=999");

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("Bad credentials");
	}

	@Test
	public void addedQuery_onSignatureThatOmittedIt() throws Exception {
		// GIVEN
		// a signature created for a request with no query string, replayed against the
		// same path with one added
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).covered(SignatureComponent.METHOD, SignatureComponent.AUTHORITY,
				SignatureComponent.PATH));
		request.setQueryString("nodeId=999");

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		thenRejected("[@query] component must be covered");
	}

	@Test
	public void unauthorized_advertisesAcceptSignature() throws Exception {
		// GIVEN
		final MockHttpServletRequest request = request("GET", TEST_PATH, null);
		sign(request, builder(request).covered(SignatureComponent.AUTHORITY));

		// WHEN
		filter.doFilter(request, response, filterChain);

		// THEN
		// @formatter:off
		then(response.getHeader(HttpSignatureFields.ACCEPT_SIGNATURE_HEADER))
			.as("A rejected request is told what an acceptable signature must cover")
			.isEqualTo("""
					sn=("@method" "@authority" "@path" "@query");created;keyid;tag="solarnetwork\"""")
			;
		// @formatter:on
	}

}
