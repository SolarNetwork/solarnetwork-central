/* ==================================================================
 * BasicDatumImportResult.java - 11/11/2018 7:36:20 AM
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

package net.solarnetwork.central.datum.imp.support;

import org.joda.time.DateTime;
import net.solarnetwork.central.datum.imp.domain.DatumImportResult;

/**
 * Basic implementation of {@link DatumImportResult}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumImportResult implements DatumImportResult {

	private final boolean success;
	private final String message;
	private final DateTime completionDate;

	/**
	 * Constructor.
	 * 
	 * @param success
	 *        the success flag
	 * @param message
	 *        a message
	 * @param completionDate
	 *        the completion date
	 */
	public BasicDatumImportResult(boolean success, String message, DateTime completionDate) {
		super();
		this.success = success;
		this.message = message;
		this.completionDate = completionDate;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the result to copy
	 */
	public BasicDatumImportResult(DatumImportResult other) {
		this((other != null ? other.isSuccess() : false), (other != null ? other.getMessage() : null),
				(other != null ? other.getCompletionDate() : null));
	}

	@Override
	public boolean isSuccess() {
		return success;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public DateTime getCompletionDate() {
		return completionDate;
	}

}
