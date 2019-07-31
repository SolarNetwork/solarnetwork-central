/* ==================================================================
 * PingController.java - 25/05/2015 10:19:49 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.domain.PingTestResult;
import net.solarnetwork.domain.PingTestResultDisplay;
import net.solarnetwork.web.domain.Response;

/**
 * A web controller for running a set of {@link PingTest} tests and returning
 * the results.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("deprecation")
@RequestMapping("/ping")
public class PingController {

	private List<net.solarnetwork.central.domain.PingTest> pingTests = null;
	private List<net.solarnetwork.domain.PingTest> tests = null;

	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1);
	private final ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0,
			TimeUnit.MILLISECONDS, queue);

	private PingResults executeTests() {
		final Date now = new Date();
		Map<String, PingTestResultDisplay> results = null;
		List<net.solarnetwork.domain.PingTest> allTests = new ArrayList<>();
		if ( pingTests != null ) {
			allTests.addAll(pingTests);
		}
		if ( tests != null ) {
			allTests.addAll(tests);
		}
		if ( !allTests.isEmpty() ) {
			results = new LinkedHashMap<String, PingTestResultDisplay>(allTests.size());
			for ( final net.solarnetwork.domain.PingTest t : allTests ) {
				final Date start = new Date();
				net.solarnetwork.domain.PingTest.Result pingTestResult = null;
				Future<net.solarnetwork.domain.PingTest.Result> f = null;
				try {
					f = executorService.submit(new Callable<net.solarnetwork.domain.PingTest.Result>() {

						@Override
						public net.solarnetwork.domain.PingTest.Result call() throws Exception {
							return t.performPingTest();
						}
					});
					pingTestResult = f.get(t.getPingTestMaximumExecutionMilliseconds(),
							TimeUnit.MILLISECONDS);
				} catch ( TimeoutException e ) {
					if ( f != null ) {
						f.cancel(true);
					}
					pingTestResult = new PingTestResult(false, "Timeout: no result provided within "
							+ t.getPingTestMaximumExecutionMilliseconds() + "ms");
				} catch ( Throwable e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					pingTestResult = new PingTestResult(false, "Exception: " + root.toString());
				} finally {
					results.put(t.getPingTestId(), new PingTestResultDisplay(t, pingTestResult, start));
				}
			}
		}
		return new PingResults(now, results);
	}

	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Response<PingResults> executePingTest() {
		return Response.response(executeTests());
	}

	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String executePingTest(Model model) {
		PingResults results = executeTests();
		model.addAttribute("results", results);
		return "ping";
	}

	/**
	 * An overall test results class.
	 */
	public static class PingResults {

		private final Date date;
		private final Map<String, net.solarnetwork.domain.PingTestResultDisplay> results;
		private final boolean allGood;

		/**
		 * Construct with values.
		 * 
		 * @param date
		 *        The date the tests were executed at.
		 * @param results
		 *        The test results (or <em>null</em> if none available).
		 */
		public PingResults(Date date, Map<String, PingTestResultDisplay> results) {
			super();
			this.date = date;
			boolean allOK = true;
			if ( results == null ) {
				this.results = Collections.emptyMap();
				allOK = false;
			} else {
				this.results = results;
				for ( PingTestResultDisplay r : results.values() ) {
					if ( !r.isSuccess() ) {
						allOK = false;
						break;
					}
				}
			}
			allGood = allOK;
		}

		/**
		 * Get a map of test ID to test results.
		 * 
		 * @return All test results.
		 */
		public Map<String, PingTestResultDisplay> getResults() {
			return results;
		}

		/**
		 * Get the date the tests were executed.
		 * 
		 * @return The date.
		 */
		public Date getDate() {
			return date;
		}

		/**
		 * Return <em>true</em> if there are test results available and all the
		 * results return <em>true</em> for {@link PingTestResult#isSuccess()}.
		 * 
		 * @return Boolean flag.
		 */
		public boolean isAllGood() {
			return allGood;
		}

	}

	public List<net.solarnetwork.central.domain.PingTest> getPingTests() {
		return pingTests;
	}

	public void setPingTests(List<net.solarnetwork.central.domain.PingTest> pingTests) {
		this.pingTests = pingTests;
	}

	public List<net.solarnetwork.domain.PingTest> getTests() {
		return tests;
	}

	public void setTests(List<net.solarnetwork.domain.PingTest> tests) {
		this.tests = tests;
	}

}
