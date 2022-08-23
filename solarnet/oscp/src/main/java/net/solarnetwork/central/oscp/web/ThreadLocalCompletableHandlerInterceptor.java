/* ==================================================================
 * ThreadLocalCompletableHandlerInterceptor.java - 19/08/2022 1:35:50 pm
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

package net.solarnetwork.central.oscp.web;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Helper HandlerInterceptor to support OSCP style "do something only after the
 * response has been sent" semantics.
 * 
 * <p>
 * The
 * {@link #afterCompletion(HttpServletRequest, HttpServletResponse, Object, Exception)}
 * method will look for a {@link CompletableFuture} in the provided
 * {@link ThreadLocal}; if available then the future will be completed with
 * {@code signal} and the and the {@code ThreadLocal} will be cleared.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ThreadLocalCompletableHandlerInterceptor<T> implements HandlerInterceptor {

	private final ThreadLocal<CompletableFuture<T>> threadLocal;
	private final T signal;

	/**
	 * Constructor.
	 * 
	 * @param threadLocal
	 *        the future supplier
	 * @param signal
	 *        the signal to give the future when the request completes
	 * @throws IllegalArgumentException
	 *         if {@code threadLocal} is {@literal null}
	 */
	public ThreadLocalCompletableHandlerInterceptor(ThreadLocal<CompletableFuture<T>> threadLocal,
			T signal) {
		super();
		this.threadLocal = requireNonNullArgument(threadLocal, "supplier");
		this.signal = signal;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		threadLocal.remove();
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) throws Exception {
		CompletableFuture<T> sent = threadLocal.get();
		if ( sent != null ) {
			sent.complete(signal);
			threadLocal.remove();
		}
	}

}
