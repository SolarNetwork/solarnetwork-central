/* ==================================================================
 * ServerMeasurementConfigurationInput.java - 8/08/2023 5:48:28 am
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

package net.solarnetwork.central.user.dnp3.domain;

import static java.time.Instant.now;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DTO for DNP3 server control configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerMeasurementConfigurationInput
		extends BaseDnp3ConfigurationInput<ServerMeasurementConfiguration, UserLongIntegerCompositePK> {

	@NotNull
	private Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String sourceId;

	@NotNull
	@NotBlank
	@Size(max = 255)
	private String property;

	@NotNull
	private MeasurementType measurementType;

	private BigDecimal multiplier;

	private BigDecimal offset;

	private Integer scale;

	@Override
	public ServerMeasurementConfiguration toEntity(UserLongIntegerCompositePK id) {
		ServerMeasurementConfiguration conf = new ServerMeasurementConfiguration(
				requireNonNullArgument(id, "id"), now());
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(ServerMeasurementConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setProperty(property);
		conf.setMeasurementType(measurementType);
		conf.setMultiplier(multiplier);
		conf.setOffset(offset);
		conf.setScale(scale);
	}

	/**
	 * Get the datum node ID.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the datum node ID.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the datum source ID.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum source ID.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the datum property name.
	 * 
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Set the datum property name.
	 * 
	 * @param property
	 *        the property to set
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	/**
	 * Set the measurement type.
	 * 
	 * @return the measurementType
	 */
	public MeasurementType getMeasurementType() {
		return measurementType;
	}

	/**
	 * Get the measurement type.
	 * 
	 * @param measurementType
	 *        the measurement type to set
	 */
	public void setMeasurementType(MeasurementType measurementType) {
		this.measurementType = measurementType;
	}

	/**
	 * Get the decimal multiplier.
	 * 
	 * @return the multiplier
	 */
	public BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the decimal multiplier.
	 * 
	 * @param multiplier
	 *        the multiplier to set
	 */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the decimal offset.
	 * 
	 * @return the offset
	 */
	public BigDecimal getOffset() {
		return offset;
	}

	/**
	 * Set the decimal offset.
	 * 
	 * @param offset
	 *        the offset to set
	 */
	public void setOffset(BigDecimal offset) {
		this.offset = offset;
	}

	/**
	 * Get the decimal scale
	 * 
	 * @return the scale
	 */
	public Integer getScale() {
		return scale;
	}

	/**
	 * Set the decimal scale.
	 * 
	 * @param scale
	 *        the scale to set
	 */
	public void setScale(Integer scale) {
		this.scale = scale;
	}

}
