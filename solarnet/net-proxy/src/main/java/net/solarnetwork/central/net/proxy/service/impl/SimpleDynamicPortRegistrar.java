/* ==================================================================
 * SimpleDynamicPortRegistrar.java - 3/08/2023 6:46:39 am
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

package net.solarnetwork.central.net.proxy.service.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.net.proxy.util.ProxyUtils;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Simple implementation of {@link DynamicPortRegistrar}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDynamicPortRegistrar implements DynamicPortRegistrar, ServiceLifecycleObserver {

	/** The {@code retries} property default value. */
	public static final int DEFAULT_RETRIES = 2;

	private static final Logger log = LoggerFactory.getLogger(SimpleDynamicPortRegistrar.class);

	private final ConcurrentNavigableMap<Integer, Boolean> registeredPorts = new ConcurrentSkipListMap<>();

	private final Supplier<Integer> portSupplier;
	private final int retries;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This will use the {@link ProxyUtils#getFreePort()} method to supply port
	 * numbers.
	 * </p>
	 */
	public SimpleDynamicPortRegistrar() {
		this(ProxyUtils::getFreePort);
	}

	/**
	 * Constructor.
	 * 
	 * @param portSupplier
	 *        the port supplier
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleDynamicPortRegistrar(Supplier<Integer> portSupplier) {
		this(portSupplier, DEFAULT_RETRIES);
	}

	/**
	 * Constructor.
	 * 
	 * @param portSupplier
	 *        the port supplier
	 * @param retries
	 *        the number of times to retry allocating a new port if one cannot
	 *        be obtained
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleDynamicPortRegistrar(Supplier<Integer> portSupplier, int retries) {
		super();
		this.portSupplier = requireNonNullArgument(portSupplier, "portSupplier");
		this.retries = retries;
	}

	@Override
	public void serviceDidStartup() {
		// nothing
	}

	@Override
	public void serviceDidShutdown() {
		// free up all port registrations
		registeredPorts.clear();
	}

	@Override
	public int reserveNewPort() throws IllegalStateException {
		for ( int i = 0, max = Math.max(1, retries + 1); i < max; i++ ) {
			Integer port;
			try {
				port = portSupplier.get();
				if ( registeredPorts.putIfAbsent(port, Boolean.TRUE) == null ) {
					log.info("Reserved port {}", port);
					return port;
				}
			} catch ( RuntimeException e ) {
				// ignore
			}
			try {
				Thread.sleep(200L);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
		throw new IllegalStateException("Unused port not available.");
	}

	@Override
	public boolean releasePort(int port) {
		Boolean result = registeredPorts.remove(port);
		if ( result != null ) {
			log.info("Returned port {}", port);
		}
		return (result != null);
	}

}
