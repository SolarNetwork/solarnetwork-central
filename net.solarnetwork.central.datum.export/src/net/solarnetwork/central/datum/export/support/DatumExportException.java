/* ==================================================================
 * DatumExportException.java - 21/04/2018 5:16:26 PM
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

package net.solarnetwork.central.datum.export.support;

import java.util.UUID;

/**
 * An exception related to the datum export process.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumExportException extends RuntimeException {

	private static final long serialVersionUID = -9084429939147833562L;

	private final String jobId;
	private final UUID requestId;

	/**
	 * @param message
	 * @param cause
	 */
	public DatumExportException(String jobId, UUID requestId, String message, Throwable cause) {
		super(message, cause);
		this.jobId = jobId;
		this.requestId = requestId;
	}

	/**
	 * Get the job execution ID related to this exception.
	 * 
	 * @return the job ID
	 */
	public String getJobId() {
		return jobId;
	}

	/**
	 * Get the job request ID related to this exception.
	 * 
	 * @return the request ID
	 */
	public UUID getRequestId() {
		return requestId;
	}

}
