/* ==================================================================
 * DatumInputReaderFeedback.java - 8/11/2018 10:03:34 AM
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
 * API for feedback on reading input data.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumInputReaderFeedback {

	/**
	 * Get the feedback message.
	 * 
	 * @return the message
	 */
	String getMessage();

	/**
	 * Get the line number to reference, from the input data.
	 * 
	 * @return the line number, or {@literal null} if not known
	 */
	Long getLineNumber();

	/**
	 * Get the original line from the input data.
	 * 
	 * @return the line, or {@literal null} if not available
	 */
	String getLine();

	/**
	 * Get the number of datum successfully loaded, if partially loaded data is
	 * supported.
	 * 
	 * @return the successfully loaded datum count, or {@code null} if not known
	 */
	Long getLoadedCount();

}
