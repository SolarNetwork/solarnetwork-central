/* ==================================================================
 * DatumImportException.java - 16/11/2018 5:58:09 AM
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
 * General exception for the datum import process.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportException extends RuntimeException implements DatumInputReaderFeedback {

	private static final long serialVersionUID = 182518709233660990L;

	private final Long lineNumber;
	private final String line;
	private final Long loadedCount;

	/**
	 * Construct with a message.
	 * 
	 * @param message
	 *        the message
	 */
	public DatumImportException(String message) {
		super(message);
		this.lineNumber = null;
		this.line = null;
		this.loadedCount = null;
	}

	/**
	 * Construct with a message and nested exception.
	 * 
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public DatumImportException(String message, Throwable cause) {
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
	public DatumImportException(String message, Throwable cause, Long lineNumber, String line) {
		this(message, cause, lineNumber, line, null);
	}

	/**
	 * Construct with a full details.
	 * 
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 * @param lineNumber
	 *        the line number the input error occurred on
	 * @param line
	 *        the original line of input data being processed
	 * @param loadedCount
	 *        the loaded count
	 */
	public DatumImportException(String message, Throwable cause, Long lineNumber, String line,
			Long loadedCount) {
		super(message, cause);
		this.lineNumber = lineNumber;
		this.line = line;
		this.loadedCount = loadedCount;
	}

	@Override
	public Long getLineNumber() {
		return lineNumber;
	}

	@Override
	public String getLine() {
		return line;
	}

	@Override
	public Long getLoadedCount() {
		return loadedCount;
	}

}
