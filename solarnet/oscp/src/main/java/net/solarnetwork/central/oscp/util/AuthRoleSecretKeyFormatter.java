/* ==================================================================
 * AuthRoleSecretKeyFormatter.java - 28/08/2022 8:25:01 am
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

package net.solarnetwork.central.oscp.util;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.expandTemplateString;
import java.util.Map;
import java.util.function.Function;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.util.StringUtils;

/**
 * Generates a secret key name for an authorization role based on a string
 * template.
 * 
 * <p>
 * The template will be passed to
 * {@link StringUtils#expandTemplateString(String, Map)} with the following
 * parameters:
 * </p>
 * 
 * <ul>
 * <li><b>role</b> - the auth role alias, e.g. {@code cp}</li>
 * <li><b>userId</b> - the user ID</li>
 * <li><b>configId</b> - the configuration ID</li>
 * </ul>
 * 
 * <p>
 * Note that {@literal null} values are not supported.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public final class AuthRoleSecretKeyFormatter implements Function<AuthRoleInfo, String> {

	/** A template pattern for configuration secret names. */
	public static final String DEFAULT_CONFIG_SECRETS_NAME_TEMPLATE = "oscp/config/role/{role}/user/{userId}/id/{configId}";

	/** A default instance using the default template. */
	public static final Function<AuthRoleInfo, String> INSTANCE = new AuthRoleSecretKeyFormatter();

	private String template = DEFAULT_CONFIG_SECRETS_NAME_TEMPLATE;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * The {@link #DEFAULT_CONFIG_SECRETS_NAME_TEMPLATE} template will be used.
	 * </p>
	 */
	public AuthRoleSecretKeyFormatter() {
		this(DEFAULT_CONFIG_SECRETS_NAME_TEMPLATE);
	}

	/**
	 * Constructor.
	 * 
	 * @param template
	 */
	public AuthRoleSecretKeyFormatter(String template) {
		super();
		this.template = requireNonNullArgument(template, "template");
	}

	@Override
	public String apply(AuthRoleInfo role) {
		Map<String, Object> params = Map.of("role", role.role().getAlias(), "userId", role.userId(),
				"configId", role.entityId());
		return expandTemplateString(template, params);

	}

}
