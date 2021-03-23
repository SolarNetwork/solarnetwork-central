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
 * @version 1.1
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
	 * Get a batch loading size.
	 * 
	 * <p>
	 * This specifies how frequently datum will be committed to the database
	 * during the load process. If {@literal null} or anything less than
	 * {@literal 1}, an all-or-nothing approach will be used, and no datum will
	 * be committed unless the entire data set is parsed and loaded
	 * successfully. If anything greater than {@code 0}, then datum will be
	 * committed each time this many have been loaded, resulting in the
	 * potential for a partially loaded set of data if any error occurs during
	 * the load process.
	 * </p>
	 * 
	 * <p>
	 * In general configuring a batch size greater than {@literal 0} may allow
	 * the data to be loaded faster, but dealing with a partially loaded data
	 * set is left to the caller to handle.
	 * </p>
	 * 
	 * @return a batch size
	 */
	Integer getBatchSize();

	/**
	 * Get a job group key.
	 * 
	 * <p>
	 * A group key represents a grouping of related jobs, such that only one job
	 * within a given group should be allowed to execute at a time. This
	 * provides a way to synchronize multiple related jobs in a reliable manner.
	 * </p>
	 * 
	 * @return the group key, or {@literal null} for the "default" group
	 * @since 1.1
	 */
	String getGroupKey();

	/**
	 * Get the configuration of the input format of the data to import.
	 * 
	 * @return the input configuration
	 */
	InputConfiguration getInputConfiguration();

}
