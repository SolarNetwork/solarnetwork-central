/* ==================================================================
 * RepeatableTaskException.java - Feb 17, 2011 3:49:29 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central;

import java.io.Serial;
import java.io.Serializable;
import org.jspecify.annotations.Nullable;

/**
 * An exception when some task that can be repeated safely has failed to signal
 * to the caller to retry the task.
 *
 * @author matt
 * @version 1.1
 */
public class RepeatableTaskException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 948738004538481858L;

	private final @Nullable Serializable id;

	/**
	 * Default constructor.
	 */
	public RepeatableTaskException() {
		super();
		this.id = null;
	}

	/**
	 * Construct with an ID value.
	 *
	 * @param id
	 *        An ID associated with this repeatable task.
	 * @since 1.1
	 */
	public RepeatableTaskException(@Nullable Serializable id) {
		super();
		this.id = id;
	}

	public RepeatableTaskException(String msg, @Nullable Throwable t) {
		super(msg, t);
		this.id = null;
	}

	/**
	 * Construct with values.
	 *
	 * @param msg
	 *        A message.
	 * @param t
	 *        A nested exception.
	 * @param id
	 *        An ID associated with this repeatable task.
	 * @since 1.1
	 */
	public RepeatableTaskException(String msg, @Nullable Throwable t, @Nullable Serializable id) {
		super(msg, t);
		this.id = id;
	}

	public RepeatableTaskException(String msg) {
		super(msg);
		this.id = null;
	}

	public RepeatableTaskException(Throwable t) {
		super(t);
		this.id = null;
	}

	/**
	 * Get the ID associated with the repeatable task.
	 *
	 * @return An ID, or {@code null} if none available.
	 */
	public @Nullable Serializable getId() {
		return id;
	}

}
