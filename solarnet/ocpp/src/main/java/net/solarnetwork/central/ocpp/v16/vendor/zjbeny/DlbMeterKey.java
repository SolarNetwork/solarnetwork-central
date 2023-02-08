/* ==================================================================
 * DlbMeterKey.java - 7/02/2023 11:11:40 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.v16.vendor.zjbeny;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A DLB Meter Key.
 * 
 * @param name
 *        the key name
 * @param phase
 *        the optional phase ({@literal null} if not phased)
 * @author matt
 * @version 1.0
 */
public record DlbMeterKey(DlbMeterKeyName name, String phase) {

	/** A pattern to match an un-phased key. */
	public static final Pattern KEY_PATTERN = Pattern.compile("(?:Current|Power)\\.(\\w+)");

	/** A pattern to match a phased key. */
	public static final Pattern PHASED_KEY_PATTERN = Pattern
			.compile("(?:Current|Power)\\.(\\w+)\\.L(\\d)");

	/**
	 * Test if this key represents a phased name.
	 * 
	 * @return {@literal true} if this key is phased
	 */
	public boolean isPhased() {
		return phase != null;
	}

	/**
	 * Attempt to parse a {@link DlbMeterKey} instance from a key value.
	 * 
	 * @param key
	 *        the key value to parse
	 * @return the key instance, or {@literal null} if one cannot be extracted
	 */
	public static DlbMeterKey forKey(String key) {
		Matcher m = KEY_PATTERN.matcher(key);
		if ( m.matches() ) {
			try {
				DlbMeterKeyName name = DlbMeterKeyName.valueOf(m.group(1));
				return new DlbMeterKey(name, null);
			} catch ( NullPointerException | IllegalArgumentException e ) {
				return null;
			}
		}
		m = PHASED_KEY_PATTERN.matcher(key);
		if ( m.matches() ) {
			try {
				DlbMeterKeyName name = DlbMeterKeyName.valueOf(m.group(1));
				String phase = m.group(2);
				return new DlbMeterKey(name, phase);
			} catch ( NullPointerException | IllegalArgumentException e ) {
				return null;
			}
		}
		return null;
	}
}
