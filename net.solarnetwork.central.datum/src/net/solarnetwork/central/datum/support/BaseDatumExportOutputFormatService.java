/* ==================================================================
 * BaseDatumExportOutputFormatService.java - 11/04/2018 12:23:27 PM
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

package net.solarnetwork.central.datum.support;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.central.datum.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.domain.BasicObjectIdentity;
import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseDatumExportOutputFormatService extends BasicObjectIdentity<String>
		implements DatumExportOutputFormatService {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private MessageSource messageSource;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the {@link Identity#getId()} to use
	 */
	public BaseDatumExportOutputFormatService(String id) {
		super(id);
	}

	@Override
	public String getSettingUID() {
		return getId();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
