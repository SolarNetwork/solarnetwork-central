/* ==================================================================
 * UserSettings.java - 10/10/2022 7:54:59 am
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

package net.solarnetwork.central.oscp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * OSCP settings for a SolarNet user.
 * 
 * <p>
 * The {@link #getId()} value represents the SolarNet user ID.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "created", "publishToSolarIn", "publishToSolarFlux", "nodeId",
		"sourceIdTemplate" })
public class UserSettings extends BasicLongEntity implements CopyingIdentity<Long, UserSettings>,
		Differentiable<UserSettings>, UserRelatedEntity<Long>, DatumPublishSettings {

	private static final long serialVersionUID = -6867441421770638409L;

	/** The default {@code sourceIdTemplate} value. */
	public static final String DEFAULT_SOURCE_ID_TEMPLATE = "/oscp/{role}/{action}/{cp}/{co}/{cgIdentifier}";

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

	private Instant modified;
	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = true;
	private String sourceIdTemplate;
	private Long nodeId;

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

	@Override
	public UserSettings copyWithId(Long id) {
		var copy = new UserSettings(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserSettings entity) {
		entity.setModified(modified);
		entity.setPublishToSolarIn(publishToSolarIn);
		entity.setPublishToSolarFlux(publishToSolarFlux);
		entity.setSourceIdTemplate(sourceIdTemplate);
		entity.setNodeId(nodeId);
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
		// @formatter:off
		return publishToSolarIn == other.publishToSolarIn
				&& publishToSolarFlux == other.publishToSolarFlux
				&& Objects.equals(sourceIdTemplate, other.sourceIdTemplate)
				&& Objects.equals(nodeId, other.nodeId);
		// @formatter:on
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
	 * Get the last modification date.
	 * 
	 * @return the modified
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * SGet the last modification date.
	 * 
	 * @param modified
	 *        the modified to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	@Override
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

	@Override
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

	@Override
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

}
