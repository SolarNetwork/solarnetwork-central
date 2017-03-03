/* ==================================================================
 * AuthenticationDataTests.java - 1/03/2017 8:06:11 PM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.security.web.test;

import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import net.solarnetwork.central.security.web.AuthenticationData;
import net.solarnetwork.central.security.web.SecurityHttpServletRequestWrapper;

/**
 * Unit tests for the {@link AuthenticationData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthenticationDataTests {

	@Test
	public void generateHttpDate() {
		Date date = new Date(1488322800000L);
		String result = AuthenticationData.httpDate(date);
		Assert.assertEquals("Tue, 28 Feb 2017 23:00:00 GMT", result);
	}

	@Test
	public void nullSafeHeaderValueExists() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader("Foo", "bar");
		String result = AuthenticationData.nullSafeHeaderValue(req, "foo");
		Assert.assertEquals("bar", result);
	}

	@Test
	public void nullSafeHeaderValueDoesNotExist() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		String result = AuthenticationData.nullSafeHeaderValue(req, "foo");
		Assert.assertEquals("", result);
	}

	@Test
	public void validateContentMD5Hex() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Content-MD5", "080aef839b95facf73ec599375e92d47");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test(expected = BadCredentialsException.class)
	public void invalidateContentMD5Hex() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Content-MD5", "080aef839b95facf73ec599375e9FFFF");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test
	public void validateContentMD5Base64() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Content-MD5", "CArvg5uV+s9z7FmTdektRw==");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test(expected = BadCredentialsException.class)
	public void invalidateContentMD5Base64() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Content-MD5", "SGVsbG8sIHdvcmxkCg==");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test
	public void validateContentSHA256Hex() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Digest",
				"sha-256=f8c3bf62a9aa3e6fc1619c250e48abe7519373d3edf41be62eb5dc45199af2ef");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test(expected = BadCredentialsException.class)
	public void invalidateContentSHA256Hex() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Digest",
				"sha-256=f8c3bf62a9aa3e6fc1619c250e48abe7519373d3edf41be62eb5dc45199af2eFF");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test
	public void validateContentSHA256Base64() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Digest", "sha-256=+MO/YqmqPm/BYZwlDkir51GTc9Pt9BvmLrXcRRma8u8=");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}

	@Test(expected = BadCredentialsException.class)
	public void invalidateContentSHA256Base64() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setContentType("text/plain");
		req.setContent("Hello, world.".getBytes("UTF-8"));
		req.addHeader("Digest", "sha-256=SufDtqwL7/Zx76jPVzhhUcBuWMpTp42D82EHMWzsEl8=");
		AuthenticationData.validateContentDigest(new SecurityHttpServletRequestWrapper(req, 1024));
	}
}
