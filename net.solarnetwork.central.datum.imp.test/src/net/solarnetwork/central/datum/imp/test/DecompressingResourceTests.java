/* ==================================================================
 * DecompressingResourceTests.java - 9/11/2018 7:24:19 AM
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

package net.solarnetwork.central.datum.imp.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.imp.support.DecompressingResource;

/**
 * Test cases for the {@link DecompressingResource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DecompressingResourceTests {

	@Test
	public void unknownCompressionFormat() throws IOException {
		DecompressingResource r = new DecompressingResource(
				new ClassPathResource("text-file.txt", getClass()));
		String data = FileCopyUtils.copyToString(new InputStreamReader(r.getInputStream(), "UTF-8"));
		assertThat(data, equalTo("Hello, world."));
		assertThat(r.getCompressionType(), equalTo(DecompressingResource.NO_KNOWN_COMPRESSION_TYPE));
	}

	@Test
	public void gzipCompressionFormat() throws IOException {
		DecompressingResource r = new DecompressingResource(
				new ClassPathResource("text-file.txt.gz", getClass()));
		String data = FileCopyUtils.copyToString(new InputStreamReader(r.getInputStream(), "UTF-8"));
		assertThat(data, equalTo("Hello, world."));
		assertThat(r.getCompressionType(), equalTo("gz"));
	}

	@Test
	public void xzCompressionFormat() throws IOException {
		DecompressingResource r = new DecompressingResource(
				new ClassPathResource("text-file.txt.xz", getClass()));
		String data = FileCopyUtils.copyToString(new InputStreamReader(r.getInputStream(), "UTF-8"));
		assertThat(data, equalTo("Hello, world."));
		assertThat(r.getCompressionType(), equalTo("xz"));
	}

	@Test
	public void lz4CompressionFormat() throws IOException {
		DecompressingResource r = new DecompressingResource(
				new ClassPathResource("text-file.txt.lz4", getClass()));
		String data = FileCopyUtils.copyToString(new InputStreamReader(r.getInputStream(), "UTF-8"));
		assertThat(data, equalTo("Hello, world!\n"));
		assertThat(r.getCompressionType(), equalTo("lz4-framed"));
	}

}
