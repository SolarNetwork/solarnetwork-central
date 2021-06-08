/* ==================================================================
 * MqttTestUtils.java - 9/06/2021 10:59:52 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.mqtt.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.springframework.util.FileCopyUtils;

/**
 * Testing utilities for MQTT tests.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttTestUtils {

	/**
	 * Compress into LZ4 (framed).
	 * 
	 * @param data
	 *        the data to compress
	 * @return the compressed data
	 */
	public static final byte[] compressLz4(byte[] data) {
		ByteArrayOutputStream byos = new ByteArrayOutputStream(1024);
		try (FramedLZ4CompressorOutputStream out = new FramedLZ4CompressorOutputStream(byos)) {
			FileCopyUtils.copy(data, out);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return byos.toByteArray();
	}

}
