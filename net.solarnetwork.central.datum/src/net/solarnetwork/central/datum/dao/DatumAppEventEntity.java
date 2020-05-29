/* ==================================================================
 * DatumAppEventEntity.java - 29/05/2020 5:24:19 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao;

import java.util.Collections;
import java.util.Map;
import net.solarnetwork.central.datum.domain.DatumAppEvent;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.dao.Entity;

/**
 * Entity for datum events.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public class DatumAppEventEntity extends BasicEntity<DatumAppEventKey>
		implements DatumAppEvent, Entity<DatumAppEventKey> {

	private final Map<String, ?> eventProperties;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param eventProperties
	 *        the event properties, or {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public DatumAppEventEntity(DatumAppEventKey id, Map<String, ?> eventProperties) {
		super(id, id.getCreated());
		if ( eventProperties == null ) {
			eventProperties = Collections.emptyMap();
		}
		this.eventProperties = eventProperties;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the event to copy
	 * @throws NullPointerException
	 *         if {code other} is {@literal null}
	 * @throws IllegalArgumentException
	 *         if
	 *         {@link DatumAppEventKey#DatumAppEventKey(String, java.time.Instant, Long, String)}
	 *         throws one
	 */
	public DatumAppEventEntity(DatumAppEvent other) {
		this(new DatumAppEventKey(other.getTopic(), other.getCreated(), other.getNodeId(),
				other.getSourceId()), other.getEventProperties());
	}

	@Override
	public String getTopic() {
		return getId().getTopic();
	}

	@Override
	public Long getNodeId() {
		return getId().getNodeId();
	}

	@Override
	public String getSourceId() {
		return getId().getSourceId();
	}

	@Override
	public Map<String, ?> getEventProperties() {
		return eventProperties;
	}

}
