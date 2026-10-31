/* ==================================================================
 * GnucashBookLockedException.java - 11/10/2026 2:45:40 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.gnucash;

import java.io.Serial;
import net.solarnetwork.central.RepeatableTaskException;

/**
 * Exception thrown when GnuCash has a book open, so it cannot be written to.
 *
 * @author matt
 * @version 1.0
 */
public class GnucashBookLockedException extends RepeatableTaskException {

	@Serial
	private static final long serialVersionUID = -2416322707052316573L;

	private final String hostname;
	private final int pid;

	/**
	 * Constructor.
	 *
	 * @param hostname
	 *        the name of the host GnuCash is running on
	 * @param pid
	 *        the process ID of GnuCash
	 */
	public GnucashBookLockedException(String hostname, int pid) {
		super("The GnuCash book is open in GnuCash on host [%s] (process %d).".formatted(hostname, pid));
		this.hostname = hostname;
		this.pid = pid;
	}

	/**
	 * Get the name of the host GnuCash is running on.
	 *
	 * @return the host name
	 */
	public final String getHostname() {
		return hostname;
	}

	/**
	 * Get the process ID of GnuCash.
	 *
	 * @return the process ID
	 */
	public final int getPid() {
		return pid;
	}

}
