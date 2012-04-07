/* ==================================================================
 * ValidationException.java - Dec 18, 2009 4:31:14 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central;

import org.springframework.validation.Errors;

/**
 * Exception for validation errors.
 * 
 * @author matt
 * @version $Id$
 */
public class ValidationException extends RuntimeException {
	
	private static final long serialVersionUID = -3397320092893511250L;

	private Errors errors;
	
	/**
	 * Default constructor.
	 */
	public ValidationException() {
		this(null);
	}
	
	/**
	 * Constructor with Errors.
	 * @param errors
	 */
	public ValidationException(Errors errors) {
		super();
		this.errors = errors;
	}

	/**
	 * @return Returns the errors.
	 */
	public Errors getErrors() {
		return errors;
	}

}
