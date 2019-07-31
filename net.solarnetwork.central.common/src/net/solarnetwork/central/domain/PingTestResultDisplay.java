/* ==================================================================
 * PingTestResultDisplay.java - 25/05/2015 10:40:07 am
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

package net.solarnetwork.central.domain;

import java.util.Date;

/**
 * Extension of {@link PingTestResult} to support the UI layer.
 * 
 * @author matt
 * @version 1.0
 */
public class PingTestResultDisplay extends PingTestResult {

	private final String pingTestId;
	private final String pingTestName;
	private final Date start;
	private final Date end;

	/**
	 * Construct from a test and a result.
	 * 
	 * @param test
	 *        The test.
	 * @param result
	 *        The result.
	 * @param start
	 *        The time the test started.
	 */
	@SuppressWarnings("deprecation")
	public PingTestResultDisplay(PingTest test, PingTestResult result, Date start) {
		super(result.isSuccess(), result.getMessage(), result.getProperties());
		this.pingTestId = test.getPingTestId();
		this.pingTestName = test.getPingTestName();
		this.start = start;
		this.end = new Date();
	}

	public String getPingTestId() {
		return pingTestId;
	}

	public String getPingTestName() {
		return pingTestName;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

}
