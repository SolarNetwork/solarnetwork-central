/* ==================================================================
 * TimeBasedUuidGenerator.java - 2/08/2022 5:32:23 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.UUID;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import net.solarnetwork.util.UuidGenerator;

/**
 * UUID generator using time-based v1 UUIDs.
 * 
 * @author matt
 * @version 1.1
 */
public class TimeBasedUuidGenerator implements UuidGenerator {

	private final TimeBasedGenerator generator;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A time-based generator based on the Ethernet address of the host machine
	 * will be used.
	 * </p>
	 */
	public TimeBasedUuidGenerator() {
		this(Generators.timeBasedGenerator(EthernetAddress.fromInterface()));
	}

	/**
	 * Constructor.
	 * 
	 * @param generator
	 *        the generator to use
	 */
	public TimeBasedUuidGenerator(TimeBasedGenerator generator) {
		super();
		this.generator = requireNonNullArgument(generator, "generator");
	}

	@Override
	public UUID generate() {
		return generator.generate();
	}

}
