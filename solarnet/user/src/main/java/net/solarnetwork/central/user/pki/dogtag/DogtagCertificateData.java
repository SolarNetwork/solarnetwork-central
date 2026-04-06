/* ==================================================================
 * DogtagCertificateData.java - Oct 14, 2014 9:12:30 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.pki.dogtag;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigInteger;
import org.jspecify.annotations.Nullable;

/**
 * Details about a certificate in Dogtag.
 * 
 * @author matt
 * @version 1.0
 */
public class DogtagCertificateData {

	private @Nullable BigInteger id;
	private @Nullable String pkcs7Chain;

	/**
	 * Constructor.
	 */
	public DogtagCertificateData() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param pkcs7Chain
	 *        the chain
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DogtagCertificateData(BigInteger id, String pkcs7Chain) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.pkcs7Chain = requireNonNullArgument(pkcs7Chain, "pkcs7Chain");
	}

	/**
	 * Get the ID.
	 * 
	 * @return the ID
	 */
	public final @Nullable BigInteger getId() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public final void setId(@Nullable BigInteger id) {
		this.id = id;
	}

	/**
	 * Get the PKCS#7 certificate chain.
	 * 
	 * @return the chain
	 */
	public final @Nullable String getPkcs7Chain() {
		return pkcs7Chain;
	}

	/**
	 * Set the PKCS#7 certificate chain.
	 * 
	 * @param pkcs7Chain
	 *        the chain to set
	 */
	public void setPkcs7Chain(@Nullable String pkcs7Chain) {
		this.pkcs7Chain = pkcs7Chain;
	}

}
