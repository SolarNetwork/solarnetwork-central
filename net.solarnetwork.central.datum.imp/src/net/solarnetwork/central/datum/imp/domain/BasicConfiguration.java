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

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Basic implementation of {@link Configuration}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicConfiguration implements Configuration, Serializable {

	private static final long serialVersionUID = -2834408245202599062L;

	private String name;
	private boolean stage;
	private Integer batchSize;
	private InputConfiguration inputConfiguration;

	/**
	 * Default constructor.
	 */
	public BasicConfiguration() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the name
	 * @param stage
	 *        {@literal true} to stage the import
	 */
	public BasicConfiguration(String name, boolean stage) {
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
	 */
	public BasicConfiguration(String name, boolean stage, Integer batchSize) {
		super();
		setName(name);
		setStage(stage);
		setBatchSize(batchSize);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the configuration to copy
	 */
	public BasicConfiguration(Configuration other) {
		super();
		if ( other == null ) {
			return;
		}
		setName(other.getName());
		setStage(other.isStage());
		setBatchSize(other.getBatchSize());
		if ( other.getInputConfiguration() != null ) {
			setInputConfiguration(new BasicInputConfiguration(other.getInputConfiguration()));
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isStage() {
		return stage;
	}

	public void setStage(boolean stage) {
		this.stage = stage;
	}

	@Override
	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public InputConfiguration getInputConfiguration() {
		return inputConfiguration;
	}

	@JsonIgnore
	public BasicInputConfiguration getInputConfig() {
		return (inputConfiguration instanceof BasicInputConfiguration
				? ((BasicInputConfiguration) inputConfiguration)
				: null);
	}

	@JsonDeserialize(as = BasicInputConfiguration.class)
	public void setInputConfiguration(InputConfiguration inputConfiguration) {
		this.inputConfiguration = inputConfiguration;
	}

}
