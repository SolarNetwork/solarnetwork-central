/* ==================================================================
 * MqttStats.java - 11/06/2018 7:43:25 PM
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

package net.solarnetwork.central.in.mqtt;

import java.util.concurrent.atomic.AtomicLongArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics for MQTT processing.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttStats {

	/** Counted fields. */
	public enum Counts {

		MessagesReceived(0, "messages received"),

		NodeDatumReceived(1, "node datum received"),

		LocationDatumReceived(2, "location datum received"),

		InstructionStatusReceived(3, "instruction status received");

		private final int index;
		private final String description;

		private Counts(int index, String description) {
			this.index = index;
			this.description = description;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(MqttStats.class);

	private final AtomicLongArray counts;
	private int logFrequency;
	private String uid;

	/**
	 * Construct with a unique ID.
	 * 
	 * @param uid
	 *        the UID
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 */
	public MqttStats(String uid, int logFrequency) {
		super();
		this.uid = uid;
		this.logFrequency = logFrequency;
		this.counts = new AtomicLongArray(Counts.values().length);
	}

	/**
	 * Set the log frequency.
	 * 
	 * @param logFrequency
	 *        the frequency
	 */
	public void setLogFrequency(int logFrequency) {
		this.logFrequency = logFrequency;
	}

	/**
	 * Set the unique ID.
	 * 
	 * @param uid
	 *        the unique ID
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * Get a current count value.
	 * 
	 * @param count
	 *        the count to get
	 * @return the current count value
	 */
	public long get(Counts count) {
		return counts.get(count.index);
	}

	/**
	 * Increment and get the current count value.
	 * 
	 * @param count
	 *        the count to increment and get
	 * @return the incremented count value
	 */
	public long incrementAndGet(Counts count) {
		long c = counts.incrementAndGet(count.index);
		if ( log.isInfoEnabled() && ((c % logFrequency) == 0) ) {
			log.info("MQTT {} {}: {}", uid, count.description, c);
		}
		return c;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("MqttStats{\n");
		for ( Counts c : Counts.values() ) {
			buf.append(String.format("%30s: %d\n", c.description, get(c)));
		}
		buf.append("}");
		return buf.toString();
	}

}
