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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;

/**
 * Basic immutable implementation of {@link DatumImportRequest}.
 *
 * @author matt
 * @version 2.0
 */
public class BasicDatumImportRequest implements DatumImportRequest {

	private final Configuration configuration;
	private final Long userId;
	private final Instant importDate;

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicDatumImportRequest(Configuration configuration, Long userId) {
		this(configuration, userId, Instant.now());
	}

	/**
	 * Constructor.
	 *
	 * @param configuration
	 *        the configuration
	 * @param userId
	 *        the ID of the requesting user
	 * @param importDate
	 *        the request date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicDatumImportRequest(Configuration configuration, Long userId, Instant importDate) {
		super();
		this.configuration = requireNonNullArgument(configuration, "configuration");
		this.userId = requireNonNullArgument(userId, "userId");
		this.importDate = requireNonNullArgument(importDate, "importDate");
	}

	@Override
	public final Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public final Long getUserId() {
		return userId;
	}

	@Override
	public final Instant getImportDate() {
		return importDate;
	}

}
