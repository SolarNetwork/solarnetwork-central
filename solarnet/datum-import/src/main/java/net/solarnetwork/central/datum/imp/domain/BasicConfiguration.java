/* ==================================================================
 * BasicConfiguration.java - 7/11/2018 11:15:08 AM
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
import java.io.Serial;
import java.io.Serializable;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Basic implementation of {@link Configuration}.
 *
 * @author matt
 * @version 1.1
 */
public class BasicConfiguration implements Configuration, Serializable {

	@Serial
	private static final long serialVersionUID = 7933356242287626694L;

	private String name;
	private boolean stage;
	private @Nullable Integer batchSize;
	private @Nullable String groupKey;
	private @Nullable InputConfiguration inputConfiguration;

	/**
	 * Construct with values.
	 *
	 * @param name
	 *        the name
	 * @param stage
	 *        {@literal true} to stage the import
	 * @throws IllegalArgumentException
	 *         if {@code name} is {@code null}
	 */
	@JsonCreator
	public BasicConfiguration(@JsonProperty("name") String name, @JsonProperty("stage") boolean stage) {
		this(name, stage, null);
	}

	/**
	 * Construct with values.
	 *
	 * @param name
	 *        the name
	 * @param stage
	 *        {@literal true} to stage the import
	 * @param batchSize
	 *        the batch size
	 * @throws IllegalArgumentException
	 *         if {@code name} is {@code null}
	 */
	public BasicConfiguration(String name, boolean stage, @Nullable Integer batchSize) {
		super();
		this.name = requireNonNullArgument(name, "name");
		this.stage = stage;
		this.batchSize = batchSize;
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the configuration to copy
	 */
	public BasicConfiguration(Configuration other) {
		super();
		other = requireNonNullArgument(other, "other");
		setName(other.getName());
		setStage(other.isStage());
		setBatchSize(other.getBatchSize());
		if ( other.getInputConfiguration() != null ) {
			setInputConfiguration(new BasicInputConfiguration(other.getInputConfiguration()));
		}
	}

	@Override
	public String toString() {
		return "BasicConfiguration{name=" + name + ",batchSize=" + batchSize + ",inputConfiguration="
				+ inputConfiguration + "}";
	}

	@Override
	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

	@Override
	public final boolean isStage() {
		return stage;
	}

	public final void setStage(boolean stage) {
		this.stage = stage;
	}

	@Override
	public final @Nullable Integer getBatchSize() {
		return batchSize;
	}

	public final void setBatchSize(@Nullable Integer batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public final @Nullable InputConfiguration getInputConfiguration() {
		return inputConfiguration;
	}

	@JsonIgnore
	public final @Nullable BasicInputConfiguration getInputConfig() {
		return (inputConfiguration instanceof BasicInputConfiguration
				? ((BasicInputConfiguration) inputConfiguration)
				: null);
	}

	@JsonDeserialize(as = BasicInputConfiguration.class)
	public final void setInputConfiguration(@Nullable InputConfiguration inputConfiguration) {
		this.inputConfiguration = inputConfiguration;
	}

	@Override
	public final @Nullable String getGroupKey() {
		return groupKey;
	}

	public final void setGroupKey(@Nullable String groupKey) {
		this.groupKey = groupKey;
	}

}
