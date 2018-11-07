/* ==================================================================
 * BasicDatumImportResource.java - 8/11/2018 6:18:07 AM
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
import org.springframework.core.io.Resource;

/**
 * Basic implementation of {@link DatumImportResource} that delegates many
 * operations to a Spring {@link Resource}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumImportResource implements DatumImportResource {

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
	public BasicDatumImportResource(Resource delegate, String contentType) {
		super();
		this.delegate = delegate;
		this.contentType = contentType;
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
