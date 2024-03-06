/* ==================================================================
 * ExceptionThrowingBoundedInputStream.java - 7/03/2024 6:40:27 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Bounded input stream that throws a {@lnk MaxUploadSizeExceededException} when
 * reading beyond the configured maximum length.
 * 
 * @author matt
 * @version 1.0
 */
public class MaxUploadSizeInputStream extends BoundedInputStream {

	/**
	 * Constructor.
	 *
	 * @param inputStream
	 *        the stream to wrap
	 * @param maxLength
	 *        the maximum length
	 */
	public MaxUploadSizeInputStream(InputStream inputStream, long maxLength) {
		super(inputStream, maxLength);
	}

	@Override
	protected void onMaxLength(long maxLength, long count) throws IOException {
		throw new MaxUploadSizeExceededException(getMaxLength());
	}

}
