/* ==================================================================
 * AesPasswordEncoder.java - 8/10/2024 12:59:34 pm
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

package net.solarnetwork.central.security;

import static org.springframework.security.crypto.encrypt.AesBytesEncryptor.CipherAlgorithm.GCM;
import java.util.Base64;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.service.PasswordEncoder;
import net.solarnetwork.util.ObjectUtils;

/**
 * Implementation of {@link PasswordEncoder} using <b>symmetric</b> encryption.
 * 
 * <p>
 * For typical password-handling scenarios where only password comparisons are
 * required, an <b>asymmetric</b> encoder like the
 * {@link net.solarnetwork.pki.bc.BCPBKDF2PasswordEncoder} should be used. This
 * implementation should be used only when the plain text version of the
 * password is required for further processing, such as decrypting a
 * previously-encrypted secret.
 * 
 * @author matt
 * @version 1.0
 */
public class PrefixedTextEncryptor implements TextEncryptor {

	private final String prefix;
	private final BytesEncryptor encryptor;

	/**
	 * Create a new encryptor using AES.
	 * 
	 * @param password
	 *        the password
	 * @param salt
	 *        the salt
	 * @return the encryptor
	 */
	public static PrefixedTextEncryptor aesTextEncryptor(String password, CharSequence salt) {
		AesBytesEncryptor delegate = new AesBytesEncryptor(password, salt, null, GCM);
		return new PrefixedTextEncryptor("{AES}", delegate);
	}

	/**
	 * Constructor.
	 * 
	 * @param prefix
	 *        a prefix to prepend to encrypted values
	 * @param encryptor
	 *        the encryptor to use
	 */
	public PrefixedTextEncryptor(String prefix, BytesEncryptor encryptor) {
		super();
		this.prefix = ObjectUtils.requireNonNullArgument(prefix, "prefix");
		this.encryptor = ObjectUtils.requireNonNullArgument(encryptor, "encryptor");
	}

	@Override
	public String encrypt(String text) {
		if ( text == null ) {
			return null;
		}
		if ( text.startsWith(prefix) ) {
			// already encrypted
			return text;
		}

		byte[] cipherText = encryptor.encrypt(Utf8.encode(text));
		return prefix + Base64.getUrlEncoder().encodeToString(cipherText);
	}

	@Override
	public String decrypt(String encryptedText) {
		if ( encryptedText == null ) {
			return null;
		}
		if ( !encryptedText.startsWith(prefix) ) {
			// note encrypted
			return encryptedText;
		}
		byte[] cipherText = Base64.getUrlDecoder().decode(encryptedText.substring(prefix.length()));
		return Utf8.decode(encryptor.decrypt(cipherText));
	}

	/**
	 * Get the configured prefix.
	 * 
	 * @return the prefix
	 */
	public final String getPrefix() {
		return prefix;
	}

}
