/* ==================================================================
 * DatumAuxiliaryMove.java - 28/02/2019 9:56:50 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.domain;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;

/**
 * DTO for a datum auxiliary move operation.
 *
 * @author matt
 * @version 1.0
 * @since 1.37
 */
public class DatumAuxiliaryMove {

	private @Nullable GeneralNodeDatumAuxiliaryPK from;
	private @Nullable GeneralNodeDatumAuxiliary to;

	public @Nullable GeneralNodeDatumAuxiliaryPK getFrom() {
		return from;
	}

	public void setFrom(@Nullable GeneralNodeDatumAuxiliaryPK from) {
		this.from = from;
	}

	public @Nullable GeneralNodeDatumAuxiliary getTo() {
		return to;
	}

	public void setTo(@Nullable GeneralNodeDatumAuxiliary to) {
		this.to = to;
	}

}
