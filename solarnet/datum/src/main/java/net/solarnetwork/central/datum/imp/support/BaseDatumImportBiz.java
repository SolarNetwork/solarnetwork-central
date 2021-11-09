/* ==================================================================
 * BaseDatumImportBiz.java - 10/11/2018 11:04:56 AM
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

package net.solarnetwork.central.datum.imp.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportResult;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.event.AppEventPublisher;
import net.solarnetwork.io.TransferrableResource;
import net.solarnetwork.service.IdentifiableConfiguration;

/**
 * Abstract class for basic {@link DatumImportBiz} support.
 * 
 * @author matt
 * @version 2.0
 */
public abstract class BaseDatumImportBiz implements DatumImportBiz {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private File workDirectory = defaultWorkDirectory();
	private List<DatumImportInputFormatService> inputServices;
	private AppEventPublisher eventPublisher;

	private static File defaultWorkDirectory() {
		String path = System.getProperty("java.io.tmpdir");
		if ( path != null ) {
			return new File(path);
		}
		// fall back to temp file parent dir
		File f;
		try {
			f = File.createTempFile("test-", ".dat");
			File d = f.getAbsoluteFile().getParentFile();
			if ( d != null ) {
				return d;
			}
		} catch ( IOException e ) {
			// ignore and move on
		}
		return new File("/var/tmp");
	}

	@Override
	public Iterable<DatumImportInputFormatService> availableInputFormatServices() {
		return (inputServices != null ? inputServices : Collections.emptyList());
	}

	/**
	 * Get the path to the import data file associated with a datum import ID.
	 * 
	 * @param id
	 *        the ID to get the file for
	 * @return the file
	 */
	protected File getImportDataFile(UserUuidPK id) {
		String fileName = id.getUserId() + "-" + id.getId().toString();
		return new File(getWorkDirectory(), fileName);
	}

	/**
	 * Save an import resource to a file in the work directory.
	 * 
	 * <p>
	 * If {@code resource} is a {@link TransferrableResource} then
	 * {@link TransferrableResource#transferTo(File)} will be used to save the
	 * resource, which may result in moving the resource rather than copying it.
	 * Otherwise, the resource data is copied to the work directory.
	 * </p>
	 * 
	 * @param resource
	 *        the resource to save
	 * @param id
	 *        the ID associated with the resource
	 * @return the file the resource was saved to, which will be obtained from
	 *         {@link #getImportDataFile(UserUuidPK)}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected File saveToWorkDirectory(DatumImportResource resource, UserUuidPK id) throws IOException {
		File f = getImportDataFile(id);
		if ( resource instanceof TransferrableResource ) {
			((TransferrableResource) resource).transferTo(f);
		} else {
			FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(f));
		}
		return f;
	}

	/**
	 * Find a specific service referenced by a service configuration.
	 * 
	 * @param <T>
	 *        the service type
	 * @param collection
	 *        the collection of services to search in
	 * @param config
	 *        the service configuration to find a matching service for
	 * @return the found service, or {@literal null} if not found
	 */
	protected <T extends Identity<String>> T optionalService(List<T> collection,
			IdentifiableConfiguration config) {
		if ( collection == null || config == null ) {
			return null;
		}
		String id = config.getServiceIdentifier();
		if ( id == null ) {
			return null;
		}
		for ( T service : collection ) {
			if ( id.equals(service.getId()) ) {
				return service;
			}
		}
		return null;
	}

	/**
	 * Post a job status changed event.
	 * 
	 * @param status
	 *        the updated status
	 * @param result
	 *        any associated job result
	 */
	protected void postJobStatusChangedEvent(DatumImportStatus status, DatumImportResult result) {
		if ( status == null ) {
			return;
		}
		final AppEventPublisher ea = getEventPublisher();
		if ( ea == null ) {
			return;
		}
		ea.postEvent(status.asJobStatusChagnedEvent(result));
	}

	/**
	 * Get the directory where temporary work files should be stored.
	 * 
	 * @return the work directory
	 */
	public File getWorkDirectory() {
		return workDirectory;
	}

	/**
	 * Set the directory where temporary work files should be stored.
	 * 
	 * <p>
	 * This is the location where import data resources will be copied to, so be
	 * writable and have sufficient space to hold those resources.
	 * </p>
	 * 
	 * @param workDirectory
	 *        the directory to use
	 * @throws IllegalArgumentException
	 *         if {@code workDirectory} is {@literal null}
	 */
	public void setWorkDirectory(File workDirectory) {
		this.workDirectory = requireNonNullArgument(workDirectory, "workDirectory");
	}

	/**
	 * Get the work directory, as a string.
	 * 
	 * @return the work directory as a string
	 */
	public String getWorkPath() {
		File d = getWorkDirectory();
		return (d != null ? d.getAbsolutePath() : null);
	}

	/**
	 * Set the work directory, as a string.
	 * 
	 * @param path
	 *        the path of the directory to set, or an empty string to use a
	 *        default work directory
	 * @throws IllegalArgumentException
	 *         if {@code path} is {@literal null}
	 */
	public void setWorkPath(String path) {
		if ( path == null ) {
			throw new IllegalArgumentException("The path must not be null");
		}
		setWorkDirectory(path.isEmpty() ? defaultWorkDirectory() : new File(path));
	}

	/**
	 * Get the input services.
	 * 
	 * @return the inputServices the input services
	 */
	public List<DatumImportInputFormatService> getInputServices() {
		return inputServices;
	}

	/**
	 * Set the input services to use.
	 * 
	 * @param inputServices
	 *        the services to set
	 */
	public void setInputServices(List<DatumImportInputFormatService> inputServices) {
		this.inputServices = inputServices;
	}

	/**
	 * Get the event admin service.
	 * 
	 * @return the service
	 */
	public AppEventPublisher getEventPublisher() {
		return eventPublisher;
	}

	/**
	 * Configure an {@link EventAdmin} service for posting status events.
	 * 
	 * @param eventPublisher
	 *        the optional event admin service
	 */
	public void setEventPublisher(AppEventPublisher eventAdmin) {
		this.eventPublisher = eventAdmin;
	}
}
