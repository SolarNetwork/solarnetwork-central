/* ==================================================================
 * GnucashSyncException.java - 11/10/2026 2:45:40 pm
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

/**
 * Exception thrown when an invoice cannot be recorded in GnuCash until
 * something is fixed, such as a missing account or a tax amount that does not
 * match.
 *
 * @author matt
 * @version 1.0
 */
public class GnucashSyncException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 4880862521407779264L;

	/**
	 * Constructor.
	 *
	 * @param message
	 *        the message
	 */
	public GnucashSyncException(String message) {
		super(message);
	}

}
