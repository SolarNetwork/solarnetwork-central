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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.PingTest;
import net.solarnetwork.domain.PingTestResult;
import net.solarnetwork.domain.PingTestResultDisplay;
import net.solarnetwork.web.domain.Response;

/**
 * A web controller for running a set of {@link PingTest} tests and returning
 * the results.
 * 
 * @author matt
 * @version 2.2
 */
@RequestMapping("/ping")
public class PingController {

	private static final ExecutorService EXECUTOR = Executors
			.newCachedThreadPool(new CustomizableThreadFactory("Ping-"));

	private List<PingTest> tests = null;

	private PingResults executeTests() {
		final Date now = new Date();
		Map<String, PingTestResultDisplay> results = null;
		List<PingTest> allTests = new ArrayList<>();
		if ( tests != null ) {
			allTests.addAll(tests);
		}
		if ( !allTests.isEmpty() ) {
			results = new TreeMap<String, PingTestResultDisplay>();
			for ( final PingTest t : allTests ) {
				final Date start = new Date();
				PingTest.Result pingTestResult = null;
				Future<PingTest.Result> f = null;
				try {
					f = EXECUTOR.submit(new Callable<PingTest.Result>() {

						@Override
						public PingTest.Result call() throws Exception {
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
	@JsonPropertyOrder({ "allGood", "date", "results" })
	public static class PingResults {

		private final Date date;
		private final Map<String, PingTestResultDisplay> results;
		private final boolean allGood;

		/**
		 * Construct with values.
		 * 
		 * @param date
		 *        The date the tests were executed at.
		 * @param results
		 *        The test results (or {@literal null} if none available).
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

	public List<PingTest> getTests() {
		return tests;
	}

	public void setTests(List<PingTest> tests) {
		this.tests = tests;
	}

}
