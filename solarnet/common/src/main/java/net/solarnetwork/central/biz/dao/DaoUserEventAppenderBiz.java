/* ==================================================================
 * DaoUserEventAppenderBiz.java - 18/03/2026 1:41:00 pm
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

package net.solarnetwork.central.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.util.UuidGenerator;

/**
 * Implementation of {@link UserEventAppenderBiz} that uses a
 * {@link GenericWriteOnlyDao} to persist events.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserEventAppenderBiz implements UserEventAppenderBiz {

	private static final Logger log = LoggerFactory.getLogger(DaoUserEventAppenderBiz.class);

	private final GenericWriteOnlyDao<UserEvent, UserUuidPK> dao;
	private final UuidGenerator uuidGenerator;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to persist events to
	 * @param uuidGenerator
	 *        the UUID generator to use
	 * @throws IllegalArgumentException
	 *         if any argument except observer is {@code null}
	 */
	public DaoUserEventAppenderBiz(GenericWriteOnlyDao<UserEvent, UserUuidPK> dao,
			UuidGenerator uuidGenerator) {
		super();
		this.dao = requireNonNullArgument(dao, "dao");
		this.uuidGenerator = requireNonNullArgument(uuidGenerator, "uuidGenerator");
	}

	@Override
	public UserEvent addEvent(Long userId, LogEventInfo info) {
		final var event = new UserEvent(userId, uuidGenerator.generate(),
				requireNonNullArgument(info, "info").getTags(), info.getMessage(), info.getData());
		try {
			dao.persist(event);
		} catch ( Exception e ) {
			log.error("Error persisting UserEvent {}: {}", event, e);
		}
		return event;
	}

}
