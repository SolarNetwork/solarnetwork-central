/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.multipart.commons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

/**
 * <p>High level API for processing file uploads.</p>
 *
 * <p>This class handles multiple files per single HTML widget, sent using
 * {@code multipart/mixed} encoding type, as specified by
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.  Use {@link
 * #parseRequest(HttpServletRequest)} to acquire a list of {@link
 * org.apache.tomcat.util.http.fileupload.FileItem}s associated with a given HTML
 * widget.</p>
 *
 * <p>How the data for individual parts is stored is determined by the factory
 * used to create them; a given part may be in memory, on disk, or somewhere
 * else.</p>
 */
public class ServletFileUpload extends FileUpload {

    /**
     * Constant for HTTP POST method.
     */
    private static final String POST_METHOD = "POST";

    // ---------------------------------------------------------- Class methods

    /**
     * Utility method that determines whether the request contains multipart
     * content.
     *
     * @param request The servlet request to be evaluated. Must be non-null.
     *
     * @return {@code true} if the request is multipart;
     *         {@code false} otherwise.
     */
    public static final boolean isMultipartContent(
            final HttpServletRequest request) {
        if (!POST_METHOD.equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return CommonsMultipartUtils.isMultipartContent(new ServletRequestContext(request));
    }

    // ----------------------------------------------------------- Constructors

    /**
     * Constructs an uninitialized instance of this class. A factory must be
     * configured, using {@code setFileItemFactory()}, before attempting
     * to parse requests.
     *
     * @see ServletFileUpload#ServletFileUpload(FileItemFactory)
     */
    public ServletFileUpload() {
    }

    /**
     * Constructs an instance of this class which uses the supplied factory to
     * create {@code FileItem} instances.
     *
     * @see FileUpload#FileUpload()
     * @param fileItemFactory The factory to use for creating file items.
     */
    public ServletFileUpload(final FileItemFactory fileItemFactory) {
    	super();
        setFileItemFactory(fileItemFactory);
    }

    // --------------------------------------------------------- Public methods

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant {@code multipart/form-data} stream.
     *
     * @param request The servlet request to be parsed.
     *
     * @return A list of {@code FileItem} instances parsed from the
     *         request, in the order that they were transmitted.
     *
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     */
    public List<FileItem> parseRequest(final HttpServletRequest request)
    throws FileUploadException {
        return parseRequest(new ServletRequestContext(request));
    }

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant {@code multipart/form-data} stream.
     *
     * @param request The servlet request to be parsed.
     *
     * @return A map of {@code FileItem} instances parsed from the request.
     *
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     *
     * @since 1.3
     */
    public Map<String, List<FileItem>> parseParameterMap(final HttpServletRequest request)
            throws FileUploadException {
        return parseParameterMap(new ServletRequestContext(request));
    }

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant {@code multipart/form-data} stream.
     *
     * @param request The servlet request to be parsed.
     *
     * @return An iterator to instances of {@code FileItemStream}
     *         parsed from the request, in the order that they were
     *         transmitted.
     *
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     * @throws IOException An I/O error occurred. This may be a network
     *   error while communicating with the client or a problem while
     *   storing the uploaded content.
     */
    public FileItemIterator getItemIterator(final HttpServletRequest request)
    throws FileUploadException, IOException {
        return super.getItemIterator(new ServletRequestContext(request));
    }

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant {@code multipart/form-data} stream.
     *
     * @param ctx The context for the request to be parsed.
     *
     * @return A map of {@code FileItem} instances parsed from the request.
     *
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     *
     * @since 1.3
     */
    public Map<String, List<FileItem>> parseParameterMap(final RequestContext ctx)
            throws FileUploadException {
        final List<FileItem> items = parseRequest(ctx);
        final Map<String, List<FileItem>> itemsMap = new HashMap<>(items.size());

        for (final FileItem fileItem : items) {
            final String fieldName = fileItem.getFieldName();
            List<FileItem> mappedItems = itemsMap.get(fieldName);

            if (mappedItems == null) {
                mappedItems = new ArrayList<>();
                itemsMap.put(fieldName, mappedItems);
            }

            mappedItems.add(fileItem);
        }

        return itemsMap;
    }

}
