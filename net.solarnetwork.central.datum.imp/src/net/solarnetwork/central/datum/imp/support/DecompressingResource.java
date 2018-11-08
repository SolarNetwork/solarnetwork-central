/* ==================================================================
 * DecompressingResource.java - 9/11/2018 7:06:20 AM
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

/**
 * A {@link Resource} that can decompress another {@link Resource}.
 * 
 * <p>
 * This implementation uses the Apache Commons Compression library to return
 * {@link InputStream} instances from {@link #getInputStream()} that perform
 * compression detection and automatic decompression. If a compression type
 * cannot be detected then the raw stream will be returned and the compression
 * type will be set to {@link #NO_KNOWN_COMPRESSION_TYPE}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DecompressingResource extends AbstractResource {

	private static final Logger log = LoggerFactory.getLogger(DecompressingResource.class);

	/**
	 * A special type returned by {@link #getCompressionType()} if the type is
	 * not a know compression format, or appears not to be compressed.
	 */
	public static final String NO_KNOWN_COMPRESSION_TYPE = "";

	private final Resource source;

	private String compressionType;

	/**
	 * Constructor.
	 * 
	 * @param source
	 *        the source (compressed) resource
	 */
	public DecompressingResource(Resource source) {
		this(source, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *        the source (compressed) resource
	 * @param compressionType
	 *        the compression type to use, or {@literal null} to try to
	 *        automatically detect the type
	 */
	public DecompressingResource(Resource source, String compressionType) {
		super();
		this.source = source;
		this.compressionType = compressionType;
	}

	@Override
	public String getDescription() {
		return "DecompressingResource{source=" + source + "}";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		BufferedInputStream in = new BufferedInputStream(source.getInputStream());
		if ( compressionType == null ) {
			try {
				compressionType = CompressorStreamFactory.detect(in);
			} catch ( CompressorException e ) {
				log.debug("No known compression type detected in {}, will return raw stream", source);
				// ignore and treat as "not compressed"
				compressionType = NO_KNOWN_COMPRESSION_TYPE;
			}
		}
		if ( NO_KNOWN_COMPRESSION_TYPE.equals(compressionType) ) {
			return in;
		}
		try {
			return new CompressorStreamFactory().createCompressorInputStream(compressionType, in);
		} catch ( CompressorException e ) {
			throw new IOException(
					"Error handling compression of resource " + source + ": " + e.getMessage());
		}
	}

	@Override
	public URL getURL() throws IOException {
		return source.getURL();
	}

	@Override
	public File getFile() throws IOException {
		return source.getFile();
	}

	@Override
	public String getFilename() {
		return source.getFilename();
	}

	/**
	 * Get the detected compression type.
	 * 
	 * @return the detected compression type
	 */
	public String getCompressionType() {
		if ( compressionType == null ) {
			// call getInputStream for detection
			try (InputStream in = getInputStream()) {
				// nothing here
			} catch ( IOException e ) {
				// ignore here
			}
		}
		return compressionType;
	}

}
