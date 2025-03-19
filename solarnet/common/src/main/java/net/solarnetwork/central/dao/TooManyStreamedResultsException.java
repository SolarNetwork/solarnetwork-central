/* ==================================================================
 * TooManyStreamedResultsException.java - 19/03/2025 4:21:43 pm
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

package net.solarnetwork.central.dao;

import java.io.IOException;

/**
 * Exception thrown when too many streams results have been processed.
 * 
 * @author matt
 * @version 1.0
 */
public class TooManyStreamedResultsException extends IOException {

	private static final long serialVersionUID = 1438273352253446682L;

	/**
	 * Constructor.
	 */
	public TooManyStreamedResultsException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *        the message
	 */
	public TooManyStreamedResultsException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *        the cause
	 */
	public TooManyStreamedResultsException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public TooManyStreamedResultsException(String message, Throwable cause) {
		super(message, cause);
	}

}
