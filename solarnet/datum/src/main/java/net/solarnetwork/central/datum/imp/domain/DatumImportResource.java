/* ==================================================================
 * DatumImportResource.java - 11/04/2018 9:44:43 AM
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

import java.io.IOException;
import java.io.InputStream;

/**
 * API for a resource to be imported.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumImportResource {

	/**
	 * Get the MIME content type of the resource.
	 * 
	 * @return the content type
	 */
	String getContentType();

	/**
	 * Get an {@link InputStream} to the resource.
	 * 
	 * @return an InputStream to the data for the resource
	 * @throws IOException
	 *         if the stream cannot be resolved
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * Determine the content length for this resource.
	 * 
	 * @return the conent length, in bytes
	 * @throws IOException
	 *         if the length cannot be resolved
	 */
	long contentLength() throws IOException;

	/**
	 * Get the modification date of the resource, in milliseconds since the
	 * epoch.
	 * 
	 * @return the modification date
	 * @throws IOException
	 *         if the date cannot be resolved
	 */
	long lastModified() throws IOException;

}
