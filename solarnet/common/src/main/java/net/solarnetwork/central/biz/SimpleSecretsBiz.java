/* ==================================================================
 * SimpleSecretsBiz.java - 27/08/2022 4:26:18 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor.CipherAlgorithm;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * A very basic implementation of {@link SecretsBiz} designed for testing and
 * development.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleSecretsBiz implements SecretsBiz {

	private static final String SECRETS_META = "secrets.meta";
	private static final String SECRETS_DATA = "secrets.data";

	private final Path dir;
	private final ConcurrentMap<String, String> data;

	private final String salt;
	private final BytesKeyGenerator iv;
	private final String password;

	public SimpleSecretsBiz(Path dir, String password) {
		super();
		this.dir = ObjectUtils.requireNonNullArgument(dir, "dir");
		this.data = new ConcurrentHashMap<>(8, 0.9f, 2);
		this.password = password;

		Path metaPath = dir.resolve(SECRETS_META);
		if ( Files.exists(metaPath) ) {
			try {
				Map<String, Object> meta = JsonUtils.getStringMap(Files.readString(metaPath));
				salt = (String) meta.get("salt");
				String ivString = (String) meta.get("iv");
				if ( salt == null || ivString == null ) {
					throw new RuntimeException("Missing metadata in [%s]".formatted(metaPath));
				}
				final byte[] ivData = HexFormat.of().parseHex(ivString);
				iv = new BytesKeyGenerator() {

					@Override
					public int getKeyLength() {
						return ivData.length;
					}

					@Override
					public byte[] generateKey() {
						return ivData;
					}
				};
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Error reading secrets metadata from [%s]".formatted(metaPath), e);
			}
		} else {
			try {
				byte[] saltBytes = new byte[8];
				SecureRandom.getInstanceStrong().nextBytes(saltBytes);
				salt = HexFormat.of().formatHex(saltBytes);
				iv = KeyGenerators.shared(16);
				byte[] ivData = iv.generateKey();
				Map<String, Object> meta = Map.of("salt", salt, "iv", HexFormat.of().formatHex(ivData));
				Path parent = metaPath.getParent();
				if ( !Files.isDirectory(parent) ) {
					Files.createDirectories(parent);
				}
				Files.writeString(metaPath, JsonUtils.getJSONString(meta, "{}"));
			} catch ( NoSuchAlgorithmException | IOException e ) {
				throw new RuntimeException("Error creating secrets metadata in [%s]".formatted(metaPath),
						e);
			}
		}

		Path dataPath = dir.resolve(SECRETS_DATA);
		if ( Files.isReadable(dataPath) ) {
			BytesEncryptor encryptor = new AesBytesEncryptor(password, salt, iv, CipherAlgorithm.GCM);
			try {
				byte[] enc = Files.readAllBytes(dataPath);
				Map<String, Object> map = JsonUtils
						.getStringMap(new String(encryptor.decrypt(enc), StandardCharsets.UTF_8));
				map.forEach((k, v) -> {
					data.put(k, v.toString());
				});
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Error loading encrypted secrets from [%s]".formatted(dataPath), e);
			}
		}
	}

	@Override
	public String getSecret(String secretName) {
		return data.get(secretName);
	}

	@Override
	public synchronized void putSecret(String secretName, String secretValue) {
		data.put(secretName, secretValue);
		saveData();
	}

	@Override
	public synchronized void deleteSecret(String secretName) {
		data.remove(secretName);
		saveData();
	}

	private void saveData() {
		Path dataPath = dir.resolve(SECRETS_DATA);
		try {
			String json = JsonUtils.getJSONString(data, "{}");
			BytesEncryptor encryptor = new AesBytesEncryptor(password, salt, iv, CipherAlgorithm.GCM);
			byte[] enc = encryptor.encrypt(json.getBytes(StandardCharsets.UTF_8));
			Files.write(dataPath, enc);
		} catch ( IOException e ) {
			throw new RuntimeException("Error saving encrypted secrets to [%s]".formatted(dataPath), e);
		}
	}

}
