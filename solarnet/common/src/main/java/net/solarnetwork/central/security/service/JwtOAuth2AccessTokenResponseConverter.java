/* ==================================================================
 * JwtOAuth2AccessTokenResponseConverter.java - 23/11/2024 6:27:24 am
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

package net.solarnetwork.central.security.service;

import static net.solarnetwork.util.CollectionUtils.getMapString;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import net.solarnetwork.util.ObjectUtils;

/**
 * Decode {@code OAuth2AccessTokenResponse} with support for extracting
 * attributes from a JWT token.
 * 
 * <p>
 * Some OAuth providers do not provide a {@code expires_in} value in their token
 * response. If such a provider returns the token as a JWT, this class will
 * attempt to extract the {@code exp} token claim date and generate a
 * {@code expires_in} value from that.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class JwtOAuth2AccessTokenResponseConverter
		implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

	private final InstantSource clock;
	private final Converter<Map<String, Object>, OAuth2AccessTokenResponse> delegate;

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the instant source to use
	 * @param delegate
	 *        the delegate converter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JwtOAuth2AccessTokenResponseConverter(InstantSource clock,
			Converter<Map<String, Object>, OAuth2AccessTokenResponse> delegate) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public OAuth2AccessTokenResponse convert(Map<String, Object> source) {
		// check if no "expires_in" parameter provided, and if not see if token is JWT with expiration date
		if ( !source.containsKey(OAuth2ParameterNames.EXPIRES_IN)
				&& source.containsKey(OAuth2ParameterNames.ACCESS_TOKEN) ) {
			try {
				JWT jwt = JWTParser.parse(getMapString(OAuth2ParameterNames.ACCESS_TOKEN, source));
				JWTClaimsSet claims = jwt.getJWTClaimsSet();
				if ( claims != null ) {
					Date exp = claims.getExpirationTime();
					if ( exp != null ) {
						Map<String, Object> newSource = new LinkedHashMap<>(source);
						newSource.put(OAuth2ParameterNames.EXPIRES_IN,
								ChronoUnit.SECONDS.between(clock.instant(), exp.toInstant()));
						source = newSource;
					}
				}
			} catch ( Exception e ) {
				// assume not a JWT, so ignore
			}
		}
		return delegate.convert(source);
	}

}
