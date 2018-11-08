/* ==================================================================
 * BasicDatumImportRequest.java - 9/11/2018 12:26:54 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.domain;

import java.util.UUID;
import org.joda.time.DateTime;

/**
 * Basic immutable implementation of {@link DatumImportRequest}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumImportRequest implements DatumImportRequest {

	private final UUID id;
	private final Configuration configuration;
	private final Long userId;
	private final DateTime importDate;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This will assign a new random UUID and the current date.
	 * </p>
	 * 
	 * @param configuration
	 *        the configuration
	 * @param userId
	 *        the ID of the requesting user
	 */
	public BasicDatumImportRequest(Configuration configuration, Long userId) {
		this(UUID.randomUUID(), configuration, userId, new DateTime());
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the unique ID
	 * @param configuration
	 *        the configuration
	 * @param userId
	 *        the ID of the requesting user
	 * @param importDate
	 *        the request date
	 */
	public BasicDatumImportRequest(UUID id, Configuration configuration, Long userId,
			DateTime importDate) {
		super();
		this.id = id;
		this.configuration = configuration;
		this.userId = userId;
		this.importDate = importDate;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public int compareTo(UUID o) {
		if ( o == id ) {
			return 0;
		} else if ( o == null ) {
			return -1;
		} else if ( id == null ) {
			return 1;
		}
		return id.compareTo(o);
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public DateTime getImportDate() {
		return importDate;
	}

}
