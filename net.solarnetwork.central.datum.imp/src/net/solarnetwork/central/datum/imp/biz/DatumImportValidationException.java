/* ==================================================================
 * DatumImportValidationException.java - 8/11/2018 9:57:45 AM
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

package net.solarnetwork.central.datum.imp.biz;

/**
 * Exception thrown when a validation error occurs while importing data.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportValidationException extends RuntimeException
		implements DatumInputReaderFeedback {

	private static final long serialVersionUID = -8929349116080288637L;

	private final Integer lineNumber;
	private final String line;

	/**
	 * Construct with a message.
	 * 
	 * @param message
	 *        the message
	 */
	public DatumImportValidationException(String message) {
		super(message);
		this.lineNumber = null;
		this.line = null;
	}

	/**
	 * Construct with a message and nested exception.
	 * 
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public DatumImportValidationException(String message, Throwable cause) {
		this(message, cause, null, null);
	}

	/**
	 * Construct with a message, nested exception, and input line related
	 * details.
	 * 
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 * @param lineNumber
	 *        the line number the input error occurred on
	 * @param line
	 *        the original line of input data being processed
	 */
	public DatumImportValidationException(String message, Throwable cause, Integer lineNumber,
			String line) {
		super(message, cause);
		this.lineNumber = lineNumber;
		this.line = line;
	}

	@Override
	public Integer getLineNumber() {
		return lineNumber;
	}

	@Override
	public String getLine() {
		return line;
	}

}
