/* ==================================================================
 * MultipartController.java - 17/09/2024 1:42:22 pm
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

package org.springframework.web.multipart.commons.test;

import java.io.IOException;
import java.util.function.Consumer;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * A web controller for tests.
 *
 * @author matt
 * @version 1.0
 */
@RestController
public class MultipartController {

	/** The handler for the {@link #upload(MultipartFile)} method. */
	static Consumer<MultipartFile> uploadHandler;

	/**
	 * Constructor.
	 */
	public MultipartController() {
		super();
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public HttpEntity<?> upload(@RequestPart("data") MultipartFile data) throws IOException {
		if ( uploadHandler != null ) {
			uploadHandler.accept(data);
		}
		return HttpEntity.EMPTY;
	}

}
