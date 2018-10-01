/* ==================================================================
 * CachedContent.java - 1/10/2018 7:30:24 AM
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

package net.solarnetwork.central.web.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import org.springframework.util.MultiValueMap;

/**
 * API for cached content items.
 * 
 * @author matt
 * @version 1.0
 */
public interface CachedContent extends Serializable {

	/**
	 * Get header information about the content.
	 * 
	 * @return the headers
	 */
	MultiValueMap<String, String> getHeaders();

	/**
	 * Get metadata about the content.
	 * 
	 * @return the metadata, never {@literal null}
	 */
	Map<String, ?> getMetadata();

	/**
	 * Get the content encoding.
	 * 
	 * @return the encoding, for example {@literal gzip} or {@literal null}
	 */
	String getContentEncoding();

	/**
	 * Get the content.
	 * 
	 * @return access to the content, or {@literal null} if none
	 */
	InputStream getContent() throws IOException;

	/**
	 * Get the length of the content.
	 * 
	 * @return the length
	 */
	int getContentLength();
}
