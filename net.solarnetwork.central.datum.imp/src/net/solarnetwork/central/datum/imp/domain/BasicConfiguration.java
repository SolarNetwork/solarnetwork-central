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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Basic implementation of {@link Configuration}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicConfiguration implements Configuration, Serializable {

	private static final long serialVersionUID = -5243790644288188821L;

	private String name;
	private boolean stage;
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
		super();
		setName(name);
		setStage(stage);
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
		this.name = other.getName();
		this.stage = other.isStage();
		this.inputConfiguration = other.getInputConfiguration();
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
	public InputConfiguration getInputConfiguration() {
		return inputConfiguration;
	}

	@JsonDeserialize(as = BasicInputConfiguration.class)
	public void setInputConfiguration(InputConfiguration inputConfiguration) {
		this.inputConfiguration = inputConfiguration;
	}

}
