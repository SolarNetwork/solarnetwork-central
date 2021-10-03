/* ==================================================================
 * BasicDatumImportReceipt.java - 11/11/2018 8:17:48 AM
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

package net.solarnetwork.central.datum.imp.domain;

/**
 * Basic immutable implementation of {@link DatumImportReceipt}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicDatumImportReceipt implements DatumImportReceipt {

	private final String jobId;
	private final DatumImportState jobState;
	private final String groupKey;

	/**
	 * Constructor.
	 * 
	 * @param jobId
	 *        the job ID
	 * @param jobState
	 *        the job state
	 */
	public BasicDatumImportReceipt(String jobId, DatumImportState jobState) {
		this(jobId, jobState, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param jobId
	 *        the job ID
	 * @param jobState
	 *        the job state
	 * @param groupKey
	 *        the group key
	 * @since 1.1
	 */
	public BasicDatumImportReceipt(String jobId, DatumImportState jobState, String groupKey) {
		super();
		this.jobId = jobId;
		this.jobState = jobState;
		this.groupKey = groupKey;
	}

	@Override
	public String getJobId() {
		return jobId;
	}

	@Override
	public DatumImportState getJobState() {
		return jobState;
	}

	@Override
	public String getGroupKey() {
		return groupKey;
	}

}
