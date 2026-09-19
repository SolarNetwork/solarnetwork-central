/* ==================================================================
 * HttpExchange.java - 17/05/2026 11:47:47 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.http;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

/**
 * An HTTP request/response pair.
 * 
 * @param <REQ>
 *        the request content type
 * @param <RES>
 *        the response content type
 * @param request
 *        the request entity
 * @param response
 *        the response entity
 * @author matt
 * @version 1.0
 */
public record HttpExchange<REQ, RES>(RequestEntity<REQ> request, ResponseEntity<RES> response) {

}
