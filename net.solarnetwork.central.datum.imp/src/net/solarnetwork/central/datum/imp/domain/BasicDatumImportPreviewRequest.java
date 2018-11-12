/* ==================================================================
 * BasicDatumImportPreviewRequest.java - 13/11/2018 8:02:02 AM
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
 * Basic immutable implementation of {@link DatumImportPreviewRequest}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumImportPreviewRequest implements DatumImportPreviewRequest {

	private final Long userId;
	private final String jobId;
	private final int previewCount;

	/**
	 * @param userId
	 * @param jobId
	 * @param previewCount
	 */
	public BasicDatumImportPreviewRequest(Long userId, String jobId, int previewCount) {
		super();
		this.userId = userId;
		this.jobId = jobId;
		this.previewCount = previewCount;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public String getJobId() {
		return jobId;
	}

	@Override
	public int getPreviewCount() {
		return previewCount;
	}

}
