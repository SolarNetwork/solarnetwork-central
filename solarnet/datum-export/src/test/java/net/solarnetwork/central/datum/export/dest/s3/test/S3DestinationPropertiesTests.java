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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static software.amazon.awssdk.regions.Region.AP_SOUTHEAST_2;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.export.dest.s3.S3DestinationProperties;
import software.amazon.awssdk.services.s3.S3Uri;

/**
 * Test cases for the {@link S3DestinationProperties} class.
 * 
 * @author matt
 * @version 3.0
 */
public class S3DestinationPropertiesTests {

	@Test
	public void uriFromEndpointPathNoScheme() {
		// GIVEN
		S3DestinationProperties props = new S3DestinationProperties();
		props.setPath("s3-ap-southeast-2.amazonaws.com/my-bucket/folder");

		// WHEN
		S3Uri uri = props.getUri();

		// THEN
		// @formatter:off
		then(uri)
			.as("URI created from path")
			.isNotNull()
			.as("Region decoded")
			.returns(Optional.of(AP_SOUTHEAST_2), from(S3Uri::region))
			.as("Bucket decoded")			
			.returns(Optional.of("my-bucket"), from(S3Uri::bucket))
			.as("Key")
			.returns(Optional.of("folder"), from(S3Uri::key))
			;
		// @formatter:on
	}

	@Test
	public void uriFromEndpointPath() {
		// GIVEN
		S3DestinationProperties props = new S3DestinationProperties();
		props.setPath("http://s3-ap-southeast-2.amazonaws.com/my-bucket/folder");

		// WHEN
		S3Uri uri = props.getUri();

		// THEN
		// @formatter:off
		then(uri)
			.as("URI created from path")
			.isNotNull()
			.as("Region decoded")
			.returns(Optional.of(AP_SOUTHEAST_2), from(S3Uri::region))
			.as("Bucket decoded")			
			.returns(Optional.of("my-bucket"), from(S3Uri::bucket))
			.as("Key")
			.returns(Optional.of("folder"), from(S3Uri::key))
			.as("Scheme preserved")
			.returns("http", from((u) -> u.uri().getScheme()))
			;
		// @formatter:on
	}

	@Test
	public void uriFromBucketPath() {
		// GIVEN
		S3DestinationProperties props = new S3DestinationProperties();
		props.setPath("/my-bucket/folder");

		// WHEN
		S3Uri uri = props.getUri();

		// THEN
		// @formatter:off
		then(uri)
			.as("URI created from path")
			.isNotNull()
			.as("Region not specified")
			.returns(Optional.empty(), from(S3Uri::region))
			.as("Bucket decoded")			
			.returns(Optional.of("my-bucket"), from(S3Uri::bucket))
			.as("Key")
			.returns(Optional.of("folder"), from(S3Uri::key))
			;
		// @formatter:on
	}

}
