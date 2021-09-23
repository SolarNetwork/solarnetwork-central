/* ==================================================================
 * ClasspathResourceMessageTemplateDataSource.java - Jan 14, 2010 9:47:22 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.mail.support;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.text.WordUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.mail.MessageTemplateDataSource;
import net.solarnetwork.util.StringMerger;

/**
 * {@link MessageTemplateDataSource} based on a locale-specific classpath
 * resource.
 * 
 * <p>
 * The {@link #getMessageTemplate()} will load a classpath resource located at
 * the {@code resource} path passed to the class constructor. The resource path
 * must have a file extension, and first this method will insert
 * <code>_<em>lang</em></code> before the file extension and attempt to use that
 * resource, where <em>lang</em> is the language value returned by
 * {@link Locale#getLanguage()} on the {@code Locale} object passed to the class
 * constructor.
 * </p>
 * 
 * <p>
 * If the language-specific resource is not found, it will try to use the
 * resource path exactly as configured. If that resource cannot be found, a
 * {@code RuntimeException} will be thrown.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class ClasspathResourceMessageTemplateDataSource extends SimpleMessageDataSource
		implements MessageTemplateDataSource {

	private final Locale locale;
	private final String resource;
	private final Map<String, ?> model;
	private ClassLoader classLoader;
	private Integer wordWrapCharacterIndex;

	/**
	 * Construct with values.
	 * 
	 * @param locale
	 *        the locale to use when locating the message resource
	 * @param subject
	 *        the subject to use
	 * @param resource
	 *        the resource path to the message template
	 * @param model
	 *        the mail merge model to use
	 */
	public ClasspathResourceMessageTemplateDataSource(Locale locale, String subject, String resource,
			Map<String, ?> model) {
		super(subject, null);
		this.locale = locale;
		this.resource = resource;
		this.model = model;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public Resource getMessageTemplate() {
		// first try via locale lang
		String resourcePath = StringUtils.stripFilenameExtension(resource) + '_'
				+ this.locale.getLanguage() + '.' + StringUtils.getFilenameExtension(resource);
		ClassLoader loader = classLoader;
		if ( loader == null ) {
			try {
				loader = Thread.currentThread().getContextClassLoader();
			} catch ( Throwable ex ) {
				// ignore
			}
			if ( loader == null ) {
				loader = getClass().getClassLoader();
			}
		}
		if ( loader.getResource(resourcePath) == null ) {
			// try without lang
			resourcePath = this.resource;
			if ( loader.getResource(resourcePath) == null ) {
				throw new RuntimeException("Resource [" + this.resource + "] not available.");
			}
		}
		return new ClassPathResource(resourcePath, loader);
	}

	@Override
	public Map<String, ?> getModel() {
		return model;
	}

	@Override
	public String getBody() {
		try {
			String msgText = StringMerger.mergeResource(getMessageTemplate(), getModel());
			int wrapColumn = (getWordWrapCharacterIndex() != null
					? getWordWrapCharacterIndex().intValue()
					: 0);
			if ( wrapColumn > 0 ) {
				// WordUtils doesn't preserve paragraphs, so first split text into paragraph strings and wrap each of those
				StringBuilder buf = new StringBuilder();
				String[] paragraphs = msgText.split("\n{2,}");
				for ( String para : paragraphs ) {
					if ( buf.length() > 0 ) {
						buf.append("\n\n");
					}
					// we also replace all single \n within the paragraph with spaces, in case the message was already hard-wrapped
					buf.append(WordUtils.wrap(para.replace("\n", " "), wrapColumn));
				}
				msgText = buf.toString();
			}
			return msgText;
		} catch ( IOException e ) {
			throw new RuntimeException(
					"Unable to merge resource [" + getMessageTemplate().getFilename() + ']', e);
		}
	}

	/**
	 * Get the custom ClassLoader to use when resolving the template resource.
	 * 
	 * @return classLoader The ClassLoader to use.
	 * @since 1.1
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Set a custom ClassLoader to use when resolving the template resource.
	 * 
	 * @param classLoader
	 *        The ClassLoader to use.
	 * @since 1.1
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public Integer getWordWrapCharacterIndex() {
		return wordWrapCharacterIndex;
	}

	/**
	 * Set the word wrap character index.
	 * 
	 * @param wordWrapCharacterIndex
	 *        The word wrap character index to set.
	 * @see MessageTemplateDataSource#getWordWrapCharacterIndex()
	 * @since 1.1
	 */
	public void setWordWrapCharacterIndex(Integer wordWrapCharacterIndex) {
		this.wordWrapCharacterIndex = wordWrapCharacterIndex;
	}

}
