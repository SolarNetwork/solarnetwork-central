/* ==================================================================
 * VersionedMessageSourceSupport.java - 5 Sept 2026 7:45:59 pm
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

package net.solarnetwork.central.user.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.support.VersionedMessageDaoMessageSource;
import net.solarnetwork.common.tmpl.st4.MessageSourceGroup;
import net.solarnetwork.common.tmpl.st4.RecordModelAdaptor;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;
import net.solarnetwork.service.TemplateRenderer;

/**
 * Support for creating {@link ST4TemplateRenderer} instances using a
 * {@link VersionedMessageDaoMessageSource} for templates.
 *
 * <p>
 * The templates of other message bundles can be imported, so the templates of
 * the main bundle can use them. A template in the main bundle takes precedence
 * over an imported template of the same name.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public abstract class VersionedMessageSourceSupport {

	private final String[] bundleNames;
	private final List<String> importedBundleNames;
	private final String rootTemplateName;
	private final List<MimeType> mimeTypes;
	private final VersionedMessageDao messageDao;
	private final Cache<String, VersionedMessages> messageCache;
	private final Cache<String, ST4TemplateRenderer> templateCache;

	/**
	 * Constructor.
	 *
	 * @param bundleName
	 *        the message bundle name to use
	 * @param rootTemplateName
	 *        the root template name
	 * @param mimeType
	 *        the supported MIME type
	 * @param messageDao
	 *        the message DAO
	 * @param messageCache
	 *        the message cache
	 * @param templateCache
	 *        the template cache
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public VersionedMessageSourceSupport(String bundleName, String rootTemplateName, MimeType mimeType,
			VersionedMessageDao messageDao, Cache<String, VersionedMessages> messageCache,
			Cache<String, ST4TemplateRenderer> templateCache) {
		this(bundleName, List.of(), rootTemplateName, mimeType, messageDao, messageCache,
				templateCache);
	}

	/**
	 * Constructor.
	 *
	 * @param bundleName
	 *        the message bundle name to use
	 * @param importedBundleNames
	 *        the names of the message bundles whose templates are imported
	 * @param rootTemplateName
	 *        the root template name
	 * @param mimeType
	 *        the supported MIME type
	 * @param messageDao
	 *        the message DAO
	 * @param messageCache
	 *        the message cache
	 * @param templateCache
	 *        the template cache
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.1
	 */
	public VersionedMessageSourceSupport(String bundleName, List<String> importedBundleNames,
			String rootTemplateName, MimeType mimeType, VersionedMessageDao messageDao,
			Cache<String, VersionedMessages> messageCache,
			Cache<String, ST4TemplateRenderer> templateCache) {
		super();
		this.bundleNames = new String[] { requireNonNullArgument(bundleName, "bundleName") };
		this.importedBundleNames = List
				.copyOf(requireNonNullArgument(importedBundleNames, "importedBundleNames"));
		this.rootTemplateName = requireNonNullArgument(rootTemplateName, "rootTemplateName");
		this.messageDao = requireNonNullArgument(messageDao, "messageDao");
		this.mimeTypes = List.of(requireNonNullArgument(mimeType, "mimeType"));
		this.messageCache = requireNonNullArgument(messageCache, "messageCache");
		this.templateCache = requireNonNullArgument(templateCache, "templateCache");
	}

	/**
	 * Create a template renderer.
	 * 
	 * <p>
	 * The start and end delimiters for ST are both configured as the
	 * {@literal $} character.
	 * </p>
	 *
	 * <p>
	 * Renderers are cached by the versions of the messages of all the bundles
	 * they use, so a change to any of them results in a new renderer.
	 * </p>
	 * 
	 * @param version
	 *        the desired template version
	 * @param mimeType
	 *        the desired output MIME type
	 * @param locale
	 *        the desired locale
	 * @return the tempalte, or {@code null} if the MIME type is not supported
	 */
	protected @Nullable TemplateRenderer rendererForVersion(final Instant version,
			final MimeType mimeType, final Locale locale) {
		boolean mimeMatch = false;
		for ( MimeType allowed : mimeTypes ) {
			if ( allowed.isCompatibleWith(mimeType) ) {
				mimeMatch = true;
				break;
			}
		}
		if ( !mimeMatch ) {
			return null;
		}
		MessageSource messageSource = new VersionedMessageDaoMessageSource(messageDao, bundleNames,
				version, messageCache);
		final StringBuilder templateVersion = new StringBuilder(
				messageSource.getMessage("version", null, "", locale));
		final List<MessageSourceGroup> importedGroups = new ArrayList<>(importedBundleNames.size());
		for ( String importedBundleName : importedBundleNames ) {
			MessageSource importedMessageSource = new VersionedMessageDaoMessageSource(messageDao,
					new String[] { importedBundleName }, version, messageCache);
			templateVersion.append('|')
					.append(importedMessageSource.getMessage("version", null, "", locale));
			MessageSourceGroup importedGroup = new MessageSourceGroup(importedBundleName,
					importedMessageSource, '$', '$');

			// templates invoked by imported templates are created by the imported group, so it
			// needs the same model adaptors ST4TemplateRenderer registers on the main group
			importedGroup.registerModelAdaptor(Record.class, new RecordModelAdaptor());
			importedGroups.add(importedGroup);
		}
		final String templateCacheKey = templateVersion.toString();
		ST4TemplateRenderer renderer = templateCache.get(templateCacheKey);
		if ( renderer == null ) {
			MessageSourceGroup group = new MessageSourceGroup(bundleNames[0], messageSource, '$',
					'$');
			for ( MessageSourceGroup importedGroup : importedGroups ) {
				group.importTemplates(importedGroup);
			}
			renderer = new ST4TemplateRenderer(bundleNames[0], group, rootTemplateName, mimeTypes,
					StandardCharsets.UTF_8);
			templateCache.putIfAbsent(templateCacheKey, renderer);
		}
		return renderer;
	}

}
