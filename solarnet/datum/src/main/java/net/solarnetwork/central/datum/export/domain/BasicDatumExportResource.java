/* ==================================================================
 * BasicDatumExportResource.java - 11/04/2018 11:58:27 AM
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

package net.solarnetwork.central.datum.export.domain;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * Basic implementation of {@link DatumExportResource} that delegates many
 * operations to a Spring {@link Resource}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class BasicDatumExportResource implements DatumExportResource {

	private final Resource delegate;
	private final String contentType;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the resource to delegate to
	 * @param contentType
	 *        the content type
	 */
	public BasicDatumExportResource(Resource delegate, String contentType) {
		super();
		this.delegate = delegate;
		this.contentType = contentType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicDatumExportResource{");
		if ( contentType != null ) {
			builder.append("contentType=");
			builder.append(contentType);
			builder.append(", ");
		}
		if ( delegate != null ) {
			builder.append("resource=");
			builder.append(delegate);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return delegate.getInputStream();
	}

	@Override
	public long contentLength() throws IOException {
		return delegate.contentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return delegate.lastModified();
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	/**
	 * Get the delegate resource.
	 * 
	 * @return the delegate
	 */
	public Resource getDelegate() {
		return delegate;
	}

}
