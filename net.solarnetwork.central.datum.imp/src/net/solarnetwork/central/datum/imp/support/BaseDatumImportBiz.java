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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.io.TransferrableResource;

/**
 * Abstract class for basic {@link DatumImportBiz} support.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseDatumImportBiz implements DatumImportBiz {

	private File workDirectory = defaultWorkDirectory();
	private List<DatumImportInputFormatService> inputServices;

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
		return inputServices;
	}

	@Override
	public DatumImportStatus submitDatumImportRequest(DatumImportRequest request,
			DatumImportResource resource) {
		// TODO Auto-generated method stub
		return null;
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
		if ( workDirectory == null ) {
			throw new IllegalArgumentException("The workDirectory must not be null");
		}
		this.workDirectory = workDirectory;
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
	 *        the path of the directory to set
	 * @throws IllegalArgumentException
	 *         if {@code path} is {@literal null}
	 */
	public void setWorkPath(String path) {
		if ( path == null ) {
			throw new IllegalArgumentException("The path must not be null");
		}
		setWorkDirectory(new File(path));
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

}
