/* ==================================================================
 * Configuration.java - 6/11/2018 4:36:30 PM
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

/**
 * A complete configuration for a datum import task.
 * 
 * @author matt
 * @version 1.0
 */
public interface Configuration {

	/**
	 * Get a name for this configuration.
	 * 
	 * @return a configuration name
	 */
	String getName();

	/**
	 * Flag to stage the import process instead of executing it.
	 * 
	 * <p>
	 * When {@literal true} the import process will start in the
	 * {@link DatumImportState#Staged} state and will not automatically get
	 * processed any further. When {@literal false} the import process will
	 * start in the {@link DatumImportState#Queued} state and get processed
	 * normally.
	 * </p>
	 * 
	 * @return {@literal true} to stage the import process
	 */
	boolean isStage();

	/**
	 * Get the configuration of the input format of the data to import.
	 * 
	 * @return the input configuration
	 */
	InputConfiguration getInputConfiguration();

}
