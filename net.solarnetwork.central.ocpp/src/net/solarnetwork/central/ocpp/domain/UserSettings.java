/* ==================================================================
 * CentralSystemUserSettings.java - 27/02/2020 4:02:55 pm
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

package net.solarnetwork.central.ocpp.domain;

import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * OCPP settings for a SolarNet user.
 * 
 * <p>
 * The {@link #getId()} value represents the SolarNet user ID.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class UserSettings extends BasicLongEntity implements Differentiable<UserSettings> {

	/** The default {@code sourceIdTemplate} value. */
	public static final String DEFAULT_SOURCE_ID_TEMPLATE = "/ocpp/{chargePointId}/{connectorId}/{location}";

	private String sourceIdTemplate = DEFAULT_SOURCE_ID_TEMPLATE;

	/**
	 * Default constructor.
	 */
	public UserSettings() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 */
	public UserSettings(Long userId, Instant created) {
		super(userId, created);
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 * 
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method.
	 * </p>
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(UserSettings other) {
		if ( other == null ) {
			return false;
		}
		return Objects.equals(sourceIdTemplate, other.sourceIdTemplate);
	}

	@Override
	public boolean differsFrom(UserSettings other) {
		return !isSameAs(other);
	}

	/**
	 * Get the user ID.
	 * 
	 * <p>
	 * This is an alias for {@link #getId()}.
	 * </p>
	 * 
	 * @return the user ID
	 */
	public Long getUserId() {
		return getId();
	}

	/**
	 * Set the source ID template.
	 * 
	 * @return the template, never {@literal null}; defaults to
	 *         {@link #DEFAULT_SOURCE_ID_TEMPLATE}
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Get the source ID template.
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 * @throws IllegalArgumentException
	 *         if {@code sourceIdTemplate} is {@literal null}
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		if ( sourceIdTemplate == null ) {
			throw new IllegalArgumentException("The sourceIdTemplate parameter must not be null.");
		}
		this.sourceIdTemplate = sourceIdTemplate;
	}

}
