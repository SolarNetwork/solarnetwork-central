/* ==================================================================
 * ResponseLengthUserServiceFilter.java - 5/05/2025 3:55:45 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web.support.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.http.HttpMethod.GET;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.support.ResponseLengthUserServiceFilter;

/**
 * Test cases for the {@link ResponseLengthUserServiceFilter} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ResponseLengthUserServiceFilterTests {

	@Mock
	private UserServiceAuditor auditor;

	@Mock
	private Filter nextFilter;

	@Mock
	private Servlet servlet;

	@Captor
	private ArgumentCaptor<ServletResponse> responseCaptor;

	@Captor
	private ArgumentCaptor<Integer> countCaptor;

	private Long userId;
	private String username;
	private String serviceName;
	private ResponseLengthUserServiceFilter filter;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		username = randomString();
		serviceName = randomString().substring(0, 4);
		filter = new ResponseLengthUserServiceFilter(auditor, serviceName);
	}

	@Test
	public void writeBytes() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		// WHEN
		SecurityUtils.becomeUser(username, "Test User", userId);
		filter.doFilter(req, res, chain);

		// THEN
		then(nextFilter).should().doFilter(same(req), responseCaptor.capture(), same(chain));

		// some response is generated, then stream closed
		final String[] data = new String[] { randomString(), randomString(), randomString() };
		int writtenCount = 0;
		try (ServletOutputStream out = responseCaptor.getValue().getOutputStream()) {
			for ( String s : data ) {
				byte[] b = s.getBytes(StandardCharsets.UTF_8);
				out.write(b);
				writtenCount += b.length;
			}
		}

		then(auditor).should(atLeastOnce()).auditUserService(eq(userId), eq(serviceName),
				countCaptor.capture());

		int countTotal = countCaptor.getAllValues().stream().mapToInt(Integer::intValue).sum();
		and.then(countTotal).as("Audit total for total bytes written").isEqualTo(writtenCount);
	}

	@Test
	public void writeChars() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		// WHEN
		SecurityUtils.becomeUser(username, "Test User", userId);
		filter.doFilter(req, res, chain);

		// THEN
		then(nextFilter).should().doFilter(same(req), responseCaptor.capture(), same(chain));

		// some response is generated, then stream closed
		final String[] data = new String[] { randomString(), randomString(), randomString() };
		int writtenCount = 0;
		try (PrintWriter out = responseCaptor.getValue().getWriter()) {
			for ( String s : data ) {
				out.write(s);
				writtenCount += s.length();
			}
		}

		then(auditor).should(atLeastOnce()).auditUserService(eq(userId), eq(serviceName),
				countCaptor.capture());

		int countTotal = countCaptor.getAllValues().stream().mapToInt(Integer::intValue).sum();
		and.then(countTotal).as("Audit total for total chars written").isEqualTo(writtenCount);
	}

	@Test
	public void println() throws ServletException, IOException {
		// GIVEN
		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		// WHEN
		SecurityUtils.becomeUser(username, "Test User", userId);
		filter.doFilter(req, res, chain);

		// THEN
		then(nextFilter).should().doFilter(same(req), responseCaptor.capture(), same(chain));

		// some response is generated, then stream closed
		final String[] data = new String[] { randomString(), randomString(), randomString() };
		int writtenCount = 0;
		try (PrintWriter out = responseCaptor.getValue().getWriter()) {
			for ( String s : data ) {
				out.println(s);
				writtenCount += s.length();
			}
		}

		then(auditor).should(atLeastOnce()).auditUserService(eq(userId), eq(serviceName),
				countCaptor.capture());

		int countTotal = countCaptor.getAllValues().stream().mapToInt(Integer::intValue).sum();
		and.then(countTotal).as("Audit total for total chars written + newlines")
				.isEqualTo(writtenCount + (data.length * System.lineSeparator().length()));
	}

}
