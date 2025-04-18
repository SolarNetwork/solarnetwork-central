/* ==================================================================
 * ChargePointSettings.java - 27/02/2020 4:22:21 pm
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

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * OCPP settings for a specific charge point.
 *
 * <p>
 * The {@link #getId()} value represents the {@link CentralChargePoint} ID.
 * </p>
 *
 * @author matt
 * @version 1.2
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "chargePointId", "userId", "created", "publishToSolarIn", "publishToSolarFlux",
		"sourceIdTemplate" })
public class ChargePointSettings extends BasicLongEntity
		implements Differentiable<ChargePointSettings>, UserRelatedEntity<Long> {

	@Serial
	private static final long serialVersionUID = 3652167896343181957L;

	private final Long userId;
	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = true;
	private String sourceIdTemplate;

	/**
	 * Default constructor.
	 */
	public ChargePointSettings() {
		super();
		this.userId = null;
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 */
	public ChargePointSettings(Long userId) {
		this(null, userId, null);
	}

	/**
	 * Constructor.
	 *
	 * @param chargePointId
	 *        the charge point ID
	 * @param userId
	 *        the user ID
	 */
	public ChargePointSettings(Long chargePointId, Long userId) {
		this(chargePointId, userId, null);
	}

	/**
	 * Constructor.
	 *
	 * @param chargePointId
	 *        the charge point ID
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 */
	@JsonCreator
	public ChargePointSettings(
			@JsonProperty(value = "chargePointId", required = true) Long chargePointId,
			@JsonProperty(value = "userId", required = true) Long userId,
			@JsonProperty("created") Instant created) {
		super(chargePointId, created);
		this.userId = userId;
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
	public boolean isSameAs(ChargePointSettings other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return publishToSolarIn == other.publishToSolarIn
				&& publishToSolarFlux == other.publishToSolarFlux
				&& Objects.equals(sourceIdTemplate, other.sourceIdTemplate);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(ChargePointSettings other) {
		return !isSameAs(other);
	}

	/**
	 * Get the charge point ID.
	 *
	 * <p>
	 * This is an alias for {@link #getId()}.
	 * </p>
	 *
	 * @return the charge point ID
	 */
	public Long getChargePointId() {
		return getId();
	}

	@Override
	public Long getUserId() {
		return userId;
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
	 * @return the template, or {@literal null}
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Get the source ID template.
	 *
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

}
