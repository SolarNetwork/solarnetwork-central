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

package net.solarnetwork.central.common.web.support.test;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.cache.Cache;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
 * @version 1.0
 */
public class JCacheContentCachingServiceTests {

	private Cache<String, CachedContent> cache;
	private JCacheContentCachingService service;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		cache = EasyMock.createMock(Cache.class);

		service = new JCacheContentCachingService(cache);
	}

	private void replayAll() {
		EasyMock.replay(cache);
	}

	@After
	public void teardown() {
		EasyMock.verify(cache);
	}

	@Test
	public void keyBasic() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");

		// when
		replayAll();
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath")));
	}

	@Test
	public void keyWithQueryParameter() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addParameter("foo", "bar");

		// when
		replayAll();
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

		// when
		replayAll();
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

		// when
		replayAll();
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("GET/somepath?123=c&a=b&foo=bar")));
	}

	@Test
	public void keyWithAuthV1() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/somepath");
		req.addHeader(HttpHeaders.AUTHORIZATION, "SolarNetworkWS foo:bar");

		// when
		replayAll();
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

		// when
		replayAll();
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

		// when
		replayAll();
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

		// when
		replayAll();
		String key = service.keyForRequest(req);

		// then
		assertThat("Cache key", key, equalTo(md5Hex("foo@GET/somepath?bim=bam&yin=yang")));
	}

	@Test
	public void cacheMiss() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final String key = "test.key";
		expect(cache.get(key)).andReturn(null);

		// when
		replayAll();
		boolean result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache miss", result, equalTo(false));
	}

	@Test
	public void cacheHitContentNotCompressed() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final SimpleCachedContent content = new SimpleCachedContent(headers, body.getBytes("UTF-8"),
				null);
		expect(cache.get(key)).andReturn(content);

		// when
		replayAll();
		boolean result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache hit", result, equalTo(true));
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
			FileCopyUtils.copy(new ByteArrayInputStream(content.getBytes("UTF-8")), out);
			return byos.toByteArray();
		}
	}

	private String decompress(byte[] content) throws IOException {
		try (ByteArrayOutputStream byos = new ByteArrayOutputStream();
				GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(content))) {
			FileCopyUtils.copy(in, byos);
			return new String(byos.toByteArray(), "UTF-8");
		}
	}

	@Test
	public void cacheHitContentCompressedNotAccepted() throws IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somepath");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final byte[] compressedBody = compress(body);
		final SimpleCachedContent content = new SimpleCachedContent(headers, compressedBody,
				"application/gzip");
		expect(cache.get(key)).andReturn(content);

		// when
		replayAll();
		boolean result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache hit", result, equalTo(true));
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

		MockHttpServletResponse response = new MockHttpServletResponse();

		final String key = "test.key";
		final HttpHeaders headers = new HttpHeaders();
		final String contentType = "text/plain;charset=UTF-8";
		headers.setContentType(MediaType.parseMediaType(contentType));
		final String body = "Hello, world.";
		final byte[] compressedBody = compress(body);
		final SimpleCachedContent content = new SimpleCachedContent(headers, compressedBody,
				"application/gzip");
		expect(cache.get(key)).andReturn(content);

		// when
		replayAll();
		boolean result = service.sendCachedResponse(key, request, response);

		// then
		assertThat("Cache hit", result, equalTo(true));
		assertThat("Response Content-Type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo(contentType));
		assertThat("Response Content-Encoding", response.getHeader(HttpHeaders.CONTENT_ENCODING),
				equalTo("application/gzip"));
		assertThat("Response Content-Length", response.getContentLength(),
				equalTo(compressedBody.length));
		assertThat("Response content", decompress(response.getContentAsByteArray()), equalTo(body));
	}
}
