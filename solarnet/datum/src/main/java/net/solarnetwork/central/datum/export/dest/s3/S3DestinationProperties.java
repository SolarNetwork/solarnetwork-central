/* ==================================================================
 * S3DestinationProperties.java - 11/04/2018 6:37:43 AM
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

package net.solarnetwork.central.datum.export.dest.s3;

import com.amazonaws.services.s3.AmazonS3URI;

/**
 * Service properties for the S3 export destination.
 * 
 * @author matt
 * @version 1.1
 */
public class S3DestinationProperties {

	/** The default value for the {@code filenameTemplate} property. */
	public static final String DEFAULT_FILENAME_TEMPLATE = "data-export-{date}.{ext}";

	/**
	 * The default value for the {@code storageClass} property.
	 * 
	 * @since 1.1
	 */
	public static final String DEFAULT_STORAGE_CLASS = "STANDARD";

	/** A filename parameter for the export date. */
	public static final String FILENAME_PARAM_DATE = "date";

	/** A filename parameter for the export extension. */
	public static final String FILENAME_PARAM_EXTENSION = "ext";

	private String path;
	private String filenameTemplate = DEFAULT_FILENAME_TEMPLATE;
	private String accessKey;
	private String secretKey;
	private String storageClass = DEFAULT_STORAGE_CLASS;

	private AmazonS3URI uri;

	/**
	 * Test if the configuration appears valid.
	 * 
	 * <p>
	 * This simply tests for non-null property values.
	 * </p>
	 * 
	 * @return {@literal true} if the configuration appears valid
	 */
	public boolean isValid() {
		return (path != null && path.trim().length() > 0 && filenameTemplate != null
				&& filenameTemplate.trim().length() > 0);
	}

	/**
	 * Get the S3 path to export to.
	 * 
	 * @return the S3 path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the S3 path to export to.
	 * 
	 * <p>
	 * This should be a fully-qualified S3 path such as
	 * <code>region-endpoint/bucket-name/prefix</code>.
	 * </p>
	 * 
	 * @param path
	 *        the S3 path to export to
	 */
	public synchronized void setPath(String path) {
		this.path = path;
		this.uri = null;
	}

	/**
	 * Get a URI from the configured path.
	 * 
	 * @return the URI
	 */
	public synchronized AmazonS3URI getUri() {
		AmazonS3URI result = uri;
		if ( result == null && path != null ) {
			String absUri = path;
			if ( absUri.startsWith("/") ) {
				absUri = "s3:/" + absUri;
			} else if ( absUri.indexOf("://") < 0 ) {
				absUri = "https://" + absUri;
			}
			result = new AmazonS3URI(absUri);
			uri = result;
		}
		return result;
	}

	/**
	 * Get a template for the output filename.
	 * 
	 * @return the filename template
	 */
	public String getFilenameTemplate() {
		return filenameTemplate;
	}

	/**
	 * Set a template for the output filename.
	 * 
	 * <p>
	 * This template is allowed to contain parameters in the form
	 * <code>{key}</code>, which are replaced at runtime by the value of a
	 * parameter <code>key</code>, or an empty string if no such parameter
	 * exists.
	 * </p>
	 * 
	 * @param filenameTemplate
	 *        the filename template to use
	 */
	public void setFilenameTemplate(String filenameTemplate) {
		this.filenameTemplate = filenameTemplate;
	}

	/**
	 * Get the S3 access key.
	 * 
	 * @return the access key
	 */
	public String getAccessKey() {
		return accessKey;
	}

	/**
	 * Set the S3 access key.
	 * 
	 * @param accessKey
	 *        the key to use
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * Get the S3 secret key.
	 * 
	 * @return the S3 secret key
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * Set the S3 secret key.
	 * 
	 * @param secretKey
	 *        the S3 secret key to use
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * Get the S3 storage class to use.
	 * 
	 * @return the S3 storage class; defaults to {@link #DEFAULT_STORAGE_CLASS}
	 * @since 1.1
	 */
	public String getStorageClass() {
		return storageClass;
	}

	/**
	 * Set the S3 storage class to use.
	 * 
	 * @param storageClass
	 *        the S3 storage class to set
	 * @since 1.1
	 */
	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}

}
