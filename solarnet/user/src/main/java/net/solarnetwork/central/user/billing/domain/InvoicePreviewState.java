/* ==================================================================
 * InvoicePreviewState.java - 22/10/2026 2:14:06 pm
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

package net.solarnetwork.central.user.billing.domain;

/**
 * The state of an invoice preview that is generated in the background.
 *
 * @author matt
 * @version 1.0
 */
public enum InvoicePreviewState {

	/**
	 * The preview has not started yet; ask again later to start it.
	 */
	Waiting,

	/** The preview is being generated. */
	Running,

	/** The preview is available. */
	Done,

	/** The preview could not be generated. */
	Failed;

	/**
	 * Get the state of a preview made of other previews.
	 *
	 * @param states
	 *        the states of the previews the preview is made of
	 * @return {@code Failed} if any state is {@code Failed}, otherwise
	 *         {@code Done} if all states are {@code Done} (including when
	 *         {@code states} is empty), otherwise {@code Running} if any state
	 *         is {@code Running}, otherwise {@code Waiting}
	 */
	public static InvoicePreviewState combined(Iterable<InvoicePreviewState> states) {
		boolean done = true;
		boolean running = false;
		for ( InvoicePreviewState state : states ) {
			switch (state) {
				case Failed -> {
					return Failed;
				}
				case Running -> {
					running = true;
					done = false;
				}
				case Waiting -> done = false;
				case Done -> {
					// nothing to do
				}
			}
		}
		return (done ? Done : running ? Running : Waiting);
	}

}
