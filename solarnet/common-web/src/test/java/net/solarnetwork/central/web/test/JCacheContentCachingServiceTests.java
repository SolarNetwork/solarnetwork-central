/* ==================================================================
 * JCacheContentCachingServiceTests.java - 1/10/2018 10:51:55 AM
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

package net.solarnetwork.central.web.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.web.support.CachedContent;
import net.solarnetwork.central.web.support.JCacheContentCachingService;
import net.solarnetwork.central.web.support.SimpleCachedContent;

/**
 * Test cases for the {@link JCacheContentCachingService} class.
 *
 * @author matt
 * @version 2.1
 */
public class JCacheContentCachingServiceTests {

	private Cache<String, CachedContent> cache;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup() {
		cache = EasyMock.createMock(Cache.class);

		expect(cache.getName()).andReturn("Test Cache").anyTimes();
		expect(cache.getConfiguration(anyObject()))
				.andThrow(new IllegalArgumentException("Configuration not supported.")).anyTimes();
	}

	private void replayAll() {
		EasyMock.replay(cache);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(cache);
	}

	@Test
	public void keyBasic() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath")));
	}

	@Test
	public void keyWithQueryParameter() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addParameter("foo", "bar");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath?foo=bar")));
	}

	@Test
	public void keyWithMultiValuedQueryParameter() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addParameter("foo", "bar");
		req.addParameter("foo", "baz");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath?foo=bar&foo=baz")));
	}

	@Test
	public void keyWithSortedQueryParameter() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addParameter("foo", "bar");
		req.addParameter("a", "b");
		req.addParameter("123", "c");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath?123=c&a=b&foo=bar")));
	}

	@Test
	public void keyWithAuthV1() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.AUTHORIZATION, "SolarNetworkWS foo:bar");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("foo@GET/somepath")));
	}

	@Test
	public void keyWithAuthV1AndQueryParameters() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.AUTHORIZATION, "SolarNetworkWS foo:bar");
		req.addParameter("bim", "bam");
		req.addParameter("yin", "yang");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("foo@GET/somepath?bim=bam&yin=yang")));
	}

	@Test
	public void keyWithAuthV2() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.AUTHORIZATION,
				"SNWS2 Credential=foo,SignedHeaders=Date,Signature=abc123");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("foo@GET/somepath")));
	}

	@Test
	public void keyWithAuthV2AndQueryParameters() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.AUTHORIZATION,
				"SNWS2 Credential=foo,SignedHeaders=Date,Signature=abc123");
		req.addParameter("bim", "bam");
		req.addParameter("yin", "yang");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("foo@GET/somepath?bim=bam&yin=yang")));
	}

	@Test
	public void keyWithAcceptTextCsv() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "text/csv");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+csv")));
	}

	@Test
	public void keyWithAcceptApplicationJson() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "application/json");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+json")));
	}

	@Test
	public void keyWithAcceptApplicationXml() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "application/xml");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+xml")));
	}

	@Test
	public void keyWithAcceptTextXml() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "text/xml");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+xml")));
	}

	@Test
	public void keyWithAcceptTextXmlApplicationJson() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "text/xml, application/json");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+xml")));
	}

	@Test
	public void keyWithAcceptTextHtml() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "text/html");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+text/html")));
	}

	@Test
	public void keyWithAcceptApplicationJsonWildcardQ() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "application/json, */*; q=0.01");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath+json")));
	}

	@Test
	public void keyWithAcceptWildcard() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.ACCEPT, "*/*");

		cache.registerCacheEntryListener(anyObject());

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath")));
	}

	@Test
	public void cacheMiss() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		MockHttpServletResponse response = new MockHttpServletResponse();

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		expect(cache.get(key)).andReturn(null);

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		CachedContent result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache miss", result, nullValue());
	}

	@Test
	public void cacheHitContentNotCompressed() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		MockHttpServletResponse response = new MockHttpServletResponse();

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final SimpleCachedContent content = new SimpleCachedContent(headers, body.getBytes(UTF_8),
				null);
		expect(cache.get(key)).andReturn(content);

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		CachedContent result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache hit", result, sameInstance(content));
		assertThat("Response Content-Type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo(contentType));
		assertThat("Response Content-Encoding", response.getHeader(HttpHeaders.CONTENT_ENCODING),
				nullValue());
		assertThat("Response Content-Length", response.getContentLength(), equalTo(body.length()));
		assertThat("Response content", response.getContentAsString(), equalTo(body));
	}

	private byte[] compress(String content) throws IOException {
		try (ByteArrayOutputStream byos = new ByteArrayOutputStream();
				GZIPOutputStream out = new GZIPOutputStream(byos)) {
			FileCopyUtils.copy(new ByteArrayInputStream(content.getBytes(UTF_8)), out);
			return byos.toByteArray();
		}
	}

	private String decompress(byte[] content) throws IOException {
		try (ByteArrayOutputStream byos = new ByteArrayOutputStream();
				GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(content))) {
			FileCopyUtils.copy(in, byos);
			return new String(byos.toByteArray(), UTF_8);
		}
	}

	@Test
	public void cacheHitContentCompressedNotAccepted() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		MockHttpServletResponse response = new MockHttpServletResponse();

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final byte[] compressedBody = compress(body);
		final SimpleCachedContent content = new SimpleCachedContent(headers, compressedBody, "gzip");
		expect(cache.get(key)).andReturn(content);

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		CachedContent result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache hit", result, sameInstance(content));
		assertThat("Response Content-Type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo(contentType));
		assertThat("Response Content-Encoding", response.getHeader(HttpHeaders.CONTENT_ENCODING),
				nullValue());
		assertThat("Response content", response.getContentAsString(), equalTo(body));
	}

	@Test
	public void cacheHitContentCompressedAccepted() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");

		cache.registerCacheEntryListener(anyObject());

		MockHttpServletResponse response = new MockHttpServletResponse();

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final byte[] compressedBody = compress(body);
		final SimpleCachedContent content = new SimpleCachedContent(headers, compressedBody, "gzip");
		expect(cache.get(key)).andReturn(content);

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		CachedContent result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache hit", result, sameInstance(content));
		assertThat("Response Content-Type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo(contentType));
		assertThat("Response Content-Encoding", response.getHeader(HttpHeaders.CONTENT_ENCODING),
				equalTo("gzip"));
		assertThat("Response Content-Length", response.getContentLength(),
				equalTo(compressedBody.length));
		assertThat("Response content", decompress(response.getContentAsByteArray()), equalTo(body));
	}

	@Test
	public void cacheContentTooSmallToCompress() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";

		final Capture<CachedContent> contentCaptor = new Capture<>();
		cache.put(eq(key), capture(contentCaptor));

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		service.cacheResponse(key, request, 200, headers,
				new ByteArrayInputStream(body.getBytes(UTF_8)));

		// then
		CachedContent content = contentCaptor.getValue();
		assertThat("Content cached", content, notNullValue());
		assertThat("Content length", content.getContentLength(), equalTo(body.length()));
		assertThat("Response Content-Encoding", content.getContentEncoding(), nullValue());

		byte[] cachedBody = FileCopyUtils.copyToByteArray(content.getContent());
		assertThat("Cached data", new String(cachedBody, UTF_8), equalTo("Hello, world."));
	}

	@Test
	public void cacheContentCompressed() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final byte[] compressedBody = compress(body);

		final Capture<CachedContent> contentCaptor = new Capture<>();
		cache.put(eq(key), capture(contentCaptor));

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		service.setCompressMinimumLength(8);
		service.cacheResponse(key, request, 200, headers,
				new ByteArrayInputStream(body.getBytes(UTF_8)));

		// then
		CachedContent content = contentCaptor.getValue();
		assertThat("Content cached", content, notNullValue());
		assertThat("Content length", content.getContentLength(), equalTo(compressedBody.length));
		assertThat("Content encoding", content.getContentEncoding(), equalTo("gzip"));

		byte[] cachedBody = FileCopyUtils.copyToByteArray(content.getContent());
		then(cachedBody).as("Compressed cached data").isEqualTo(compressedBody);
	}

	@Test
	public void cacheContentNotCompressible() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
		headers.setContentType(MediaType.parseMediaType(contentType));
		final byte[] body = new byte[] { (byte) 1, (byte) 2, (byte) 3 };

		final Capture<CachedContent> contentCaptor = new Capture<>();
		cache.put(eq(key), capture(contentCaptor));

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		service.cacheResponse(key, request, 200, headers, new ByteArrayInputStream(body));

		// then
		CachedContent content = contentCaptor.getValue();
		assertThat("Content cached", content, notNullValue());
		assertThat("Content length", content.getContentLength(), equalTo(body.length));
		assertThat("Response Content-Encoding", content.getContentEncoding(), nullValue());

		byte[] cachedBody = FileCopyUtils.copyToByteArray(content.getContent());
		assertThat("Cached data", HexFormat.of().formatHex(cachedBody), equalTo("010203"));
	}

	@Test
	public void cacheContentAlreadyEncoded() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");

		cache.registerCacheEntryListener(anyObject());

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		headers.set(HttpHeaders.CONTENT_ENCODING, "foo");
		final String body = "Hello, world.";

		final Capture<CachedContent> contentCaptor = new Capture<>();
		cache.put(eq(key), capture(contentCaptor));

		// when
		replayAll();
		JCacheContentCachingService service = new JCacheContentCachingService(cache);
		service.cacheResponse(key, request, 200, headers,
				new ByteArrayInputStream(body.getBytes(UTF_8)));

		// then
		CachedContent content = contentCaptor.getValue();
		assertThat("Content cached", content, notNullValue());
		assertThat("Content length", content.getContentLength(), equalTo(body.length()));
		assertThat("Content encoding", content.getContentEncoding(), equalTo("foo"));

		byte[] cachedBody = FileCopyUtils.copyToByteArray(content.getContent());
		assertThat("Cached data", new String(cachedBody, UTF_8), equalTo(body));
	}

}
