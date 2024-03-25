/* ==================================================================
 * FtpDestinationProperties.java - 25/03/2024 2:08:49 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.dest.ftp;

/**
 * Service properties for the FTP export destination.
 *
 * @author matt
 * @version 1.0
 */
public class FtpDestinationProperties {

	/** The {@code dataTls} property default value. */
	public static final boolean DEFAUT_DATA_TLS = true;

	private String url;
	private String username;
	private String password;
	private boolean implicitTls;
	private boolean dataTls = DEFAUT_DATA_TLS;

	/**
	 * Constructor.
	 */
	public FtpDestinationProperties() {
		super();
	}

	/**
	 * Test if the configuration appears valid.
	 *
	 * @return {@literal true} if the configuration appears valid
	 */
	public boolean isValid() {
		return (url != null && !url.isBlank());
	}

	/**
	 * Get the HTTP URL.
	 *
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the HTTP URL.
	 *
	 * @param url
	 *        the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 *
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username != null && !username.isBlank() ? username : null;
	}

	/**
	 * Get the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 *
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password != null && !password.isBlank() ? password : null;
	}

	/**
	 * Test if the {@code username} and {@code password} properties are
	 * configured.
	 *
	 * @return {@literal true} if both {@code username} and {@code password} are
	 *         configured
	 */
	public boolean hasCredentials() {
		return (username != null && !username.isBlank() && password != null && !password.isBlank());
	}

	/**
	 * Get the TLS mode.
	 *
	 * @return {@link true} if TLS is implied for {@literal ftps://} URLs, e.g.
	 *         ports 990 (control) 989 (data)
	 */
	public boolean isImplicitTls() {
		return implicitTls;
	}

	/**
	 * Set the TLS mode.
	 *
	 * @param implicitTls
	 *        {@link true} if TLS is implied for {@literal ftps://} URLs, e.g.
	 *        ports 990 (control) 989 (data)
	 */
	public void setImplicitTls(boolean implicitTls) {
		this.implicitTls = implicitTls;
	}

	/**
	 * Get the data TLS mode.
	 *
	 * @return {@literal true} to use TLS for data transfers on {code ftps}
	 *         connections
	 */
	public boolean isDataTls() {
		return dataTls;
	}

	/**
	 * Set the data TLS mode.
	 *
	 * @param dataTls
	 *        {@literal true} to use TLS for data transfers on {code ftps}
	 *        connections
	 */
	public void setDataTls(boolean dataTls) {
		this.dataTls = dataTls;
	}

}
