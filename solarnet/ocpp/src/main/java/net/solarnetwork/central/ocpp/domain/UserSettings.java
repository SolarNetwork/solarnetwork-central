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
import java.util.regex.Pattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
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
 * @version 1.1
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "created", "publishToSolarIn", "publishToSolarFlux", "sourceIdTemplate" })
public class UserSettings extends BasicLongEntity
		implements Differentiable<UserSettings>, UserRelatedEntity<Long> {

	private static final long serialVersionUID = 5060316663324383986L;

	/** The default {@code sourceIdTemplate} value. */
	public static final String DEFAULT_SOURCE_ID_TEMPLATE = "/ocpp/cp/{chargerIdentifier}/{connectorId}/{location}";

	/**
	 * A regular expression for finding sequences of more than one {@literal /}
	 * character or one or more {@literal /} character at the end of the string.
	 * 
	 * <p>
	 * This is designed to be used to normalize source IDs resolved using the
	 * configured {@code sourceIdTemplate} where a placeholder might be missing
	 * at runtime, resulting in an empty path segment. It can be used like this:
	 * </p>
	 * 
	 * <blockquote> <code>
	 * SOURCE_ID_SLASH_PAT.matcher(<i>str</i>).replaceAll("")
	 * </code> </blockquote>
	 */
	public static final Pattern SOURCE_ID_EMPTY_SEGMENT_PAT = Pattern.compile("((?<=/)/+|/+$)");

	/**
	 * Replace matches found with {@link #SOURCE_ID_EMPTY_SEGMENT_PAT} with a
	 * single {@literal /}.
	 * 
	 * @param sourceId
	 *        the source ID to remove empty path segments from
	 * @return the resulting source ID
	 */
	public static String removeEmptySourceIdSegments(String sourceId) {
		return SOURCE_ID_EMPTY_SEGMENT_PAT.matcher(sourceId).replaceAll("");
	}

	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = true;
	private String sourceIdTemplate;

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
	 */
	public UserSettings(Long userId) {
		super(userId, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 */
	@JsonCreator
	public UserSettings(@JsonProperty(value = "userId", required = true) Long userId,
			@JsonProperty("created") Instant created) {
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
	@Override
	public Long getUserId() {
		return getId();
	}

	/**
	 * Get the "publish to SolarIn" toggle.
	 * 
	 * @return {@literal true} if data from this charge point should be
	 *         published to SolarIn; defaults to {@literal true}
	 */
	public boolean isPublishToSolarIn() {
		return publishToSolarIn;
	}

	/**
	 * Set the "publish to SolarIn" toggle.
	 * 
	 * @param publishToSolarIn
	 *        {@literal true} if data from this charge point should be published
	 *        to SolarIn
	 */
	public void setPublishToSolarIn(boolean publishToSolarIn) {
		this.publishToSolarIn = publishToSolarIn;
	}

	/**
	 * Get the "publish to SolarFlux" toggle.
	 * 
	 * @return {@literal true} if data from this charge point should be
	 *         published to SolarFlux; defaults to {@literal true}
	 */
	public boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 * 
	 * @param publishToSolarFlux
	 *        {@literal true} if data from this charge point should be published
	 *        to SolarFlux
	 */
	public void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
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
