/* ==================================================================
 * S3DestinationPropertiesTests.java - 11/04/2018 10:38:08 AM
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

package net.solarnetwork.central.datum.export.dest.s3.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import com.amazonaws.services.s3.AmazonS3URI;
import net.solarnetwork.central.datum.export.dest.s3.S3DestinationProperties;

/**
 * Test cases for the {@link S3DestinationProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
public class S3DestinationPropertiesTests {

	@Test
	public void uriFromEndpointPathNoScheme() {
		S3DestinationProperties props = new S3DestinationProperties();
		props.setPath("s3-ap-southeast-2.amazonaws.com/my-bucket/folder");
		AmazonS3URI uri = props.getUri();
		assertThat("URI created from path", uri, notNullValue());
		assertThat("Region available", uri.getRegion(), equalTo("ap-southeast-2"));
		assertThat("Bucket", uri.getBucket(), equalTo("my-bucket"));
		assertThat("Key", uri.getKey(), equalTo("folder"));
	}

	@Test
	public void uriFromEndpointPath() {
		S3DestinationProperties props = new S3DestinationProperties();
		props.setPath("http://s3-ap-southeast-2.amazonaws.com/my-bucket/folder");
		AmazonS3URI uri = props.getUri();
		assertThat("URI created from path", uri, notNullValue());
		assertThat("Region available", uri.getRegion(), equalTo("ap-southeast-2"));
		assertThat("Bucket", uri.getBucket(), equalTo("my-bucket"));
		assertThat("Key", uri.getKey(), equalTo("folder"));
		assertThat("Scheme preserved", uri.getURI().getScheme(), equalTo("http"));
	}

	@Test
	public void uriFromBucketPath() {
		S3DestinationProperties props = new S3DestinationProperties();
		props.setPath("/my-bucket/folder");
		AmazonS3URI uri = props.getUri();
		assertThat("URI created from path", uri, notNullValue());
		assertThat("Region not specified", uri.getRegion(), nullValue());
		assertThat("Bucket", uri.getBucket(), equalTo("my-bucket"));
		assertThat("Key", uri.getKey(), equalTo("folder"));
	}

}
