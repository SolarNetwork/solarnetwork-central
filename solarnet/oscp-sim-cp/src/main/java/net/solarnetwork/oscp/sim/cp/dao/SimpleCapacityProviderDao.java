/* ==================================================================
 * SimpleCapacityProviderDao.java - 23/08/2022 11:53:49 am
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

package net.solarnetwork.oscp.sim.cp.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Basic implementation of {@link CapacityProviderDao} that saves data to a
 * file.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCapacityProviderDao implements CapacityProviderDao, ServiceLifecycleObserver {

	private static final Logger log = LoggerFactory.getLogger(SimpleCapacityProviderDao.class);

	private static final String SYSTEMS_FILENAME = "systems";

	private final ConcurrentMap<UUID, SystemConfiguration> systems = new ConcurrentHashMap<>(4, 0.9f, 2);
	private final ObjectMapper mapper;
	private Path baseDir = Paths.get("var/data");

	public SimpleCapacityProviderDao(ObjectMapper mapper) {
		super();
		this.mapper = requireNonNullArgument(mapper, "mapper");
	}

	@Override
	public synchronized void serviceDidStartup() {
		SystemConfiguration[] confs = loadData(SYSTEMS_FILENAME, SystemConfiguration[].class);
		if ( confs != null ) {
			for ( SystemConfiguration conf : confs ) {
				systems.put(conf.getId(), conf);
			}
		}
	}

	@Override
	public void serviceDidShutdown() {
		// nothing
	}

	private <T> T loadData(String name, Class<T> type) {
		Path path = baseDir.resolve(name + ".json");
		if ( Files.isReadable(path) ) {
			try {
				return mapper.readValue(path.toFile(), type);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Error loading %s data from [%s]: %s".formatted(name, path, e.toString()));
			}
		}
		return null;
	}

	private void saveData(String name, Object data) {
		Path path = baseDir.resolve(name + ".json");
		try {
			mapper.writeValue(path.toFile(), data);
		} catch ( IOException e ) {
			throw new RuntimeException(
					"Error saving %s data to [%s]: %s".formatted(name, path, e.toString()));
		}
	}

	@Override
	public SystemConfiguration verifyAuthToken(String reqToken) {
		requireNonNullArgument(reqToken, "reqToken");
		return systems.values().stream().filter(e -> reqToken.equals(e.getInToken())).findAny()
				.orElseThrow(() -> new AuthorizationException(Reason.ACCESS_DENIED, reqToken));
	}

	@Override
	public SystemConfiguration systemConfiguration(UUID id) {
		return systems.get(id);
	}

	@Override
	public synchronized void saveSystemConfiguration(SystemConfiguration conf) {
		systems.put(requireNonNullArgument(conf, "conf").getId(), conf);
		saveData(SYSTEMS_FILENAME, systems.values());
	}

	@Override
	public synchronized int processExpiredHeartbeats(Function<SystemConfiguration, Instant> handler) {
		requireNonNullArgument(handler, "handler");
		final Instant now = Instant.now();
		int updated = 0;
		for ( SystemConfiguration conf : systems.values() ) {
			synchronized ( conf ) {
				if ( conf.isHeartbeatExpired(now) ) {
					try {
						Instant result = handler.apply(conf);
						if ( result != null ) {
							conf.setHeartbeatDate(result);
							saveSystemConfiguration(conf);
							updated++;
						}
					} catch ( Exception e ) {
						log.error("Heartbeat handler threw exception processing system {}: {}",
								conf.getId(), e.getMessage(), e);
					}
				}
			}
		}
		return updated;
	}

	/**
	 * Get the base directory path.
	 * 
	 * @return the base directory path
	 */
	public Path getBaseDir() {
		return baseDir;
	}

	/**
	 * Set the base directory path.
	 * 
	 * @param baseDir
	 *        the base directory to set
	 */
	public void setBaseDir(Path baseDir) {
		this.baseDir = baseDir;
	}

}
