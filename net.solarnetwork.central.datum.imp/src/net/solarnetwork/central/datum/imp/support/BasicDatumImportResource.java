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

package net.solarnetwork.central.datum.imp.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.io.TransferrableResource;

/**
 * Basic implementation of {@link DatumImportResource} that delegates many
 * operations to a Spring {@link Resource}.
 * 
 * <p>
 * This class also implements {@link TransferrableResource}, and will delegate
 * that API to the delegate {@link Resource} as long as the delegate also
 * implements {@link TransferrableResource}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumImportResource implements DatumImportResource, TransferrableResource {

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
	 * Transfer the resource to a file.
	 * 
	 * <p>
	 * If the delegate {@code resource} also implements
	 * {@link TransferrableResource} then this method will delegate to that
	 * directly. Otherwise the data stream will be copied to the given file.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if ( delegate instanceof TransferrableResource ) {
			((TransferrableResource) delegate).transferTo(dest);
		} else {
			FileCopyUtils.copy(getInputStream(), new FileOutputStream(dest));
		}
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
