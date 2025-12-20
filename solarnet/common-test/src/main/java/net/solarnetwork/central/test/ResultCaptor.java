/* ==================================================================
 * ResultCaptor.java - 28/11/2025 6:17:03 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import net.solarnetwork.util.ObjectUtils;

/**
 * Helper to capture the result of an answer invotation.
 *
 * @param <T>
 *        the result type
 * @author matt
 * @version 1.0
 */
public class ResultCaptor<T> implements Answer<T> {

	private final Answer<T> delegate;
	private T result;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate answer
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ResultCaptor(Answer<T> delegate) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public T answer(InvocationOnMock invocation) throws Throwable {
		T res = delegate.answer(invocation);
		this.result = res;
		return res;
	}

	/**
	 * Get the result.
	 *
	 * @return the result
	 */
	public T getResult() {
		return result;
	}

}
