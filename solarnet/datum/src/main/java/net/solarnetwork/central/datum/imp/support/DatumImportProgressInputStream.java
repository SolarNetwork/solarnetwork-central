/* ==================================================================
 * DatumImportProgressInputStream.java - 8/11/2018 6:28:27 AM
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

import java.io.InputStream;
import org.apache.commons.io.input.CountingInputStream;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.service.ProgressListener;

/**
 * {@link InputStream} that tracks the overall progress of reading the stream.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumImportProgressInputStream extends CountingInputStream {

	private final long expectedLength;
	private final DatumImportService progressContext;
	private final ProgressListener<DatumImportService> progressListener;

	/**
	 * Constructor.
	 * 
	 * @param in
	 *        the stream to track the progress reading
	 * @param expectedLength
	 *        the expected length of the stream
	 * @param progressContext
	 *        the progress context
	 * @param progressListener
	 *        the progress listener
	 */
	public DatumImportProgressInputStream(InputStream in, long expectedLength,
			DatumImportService progressContext, ProgressListener<DatumImportService> progressListener) {
		super(in);
		this.expectedLength = expectedLength;
		this.progressContext = progressContext;
		this.progressListener = progressListener;
	}

	@Override
	protected synchronized void afterRead(int n) {
		super.afterRead(n);
		if ( n > 0 && progressListener != null ) {
			progressListener.progressChanged(this.progressContext,
					Math.min(1.0, (double) getByteCount() / expectedLength));
		}
	}

}
