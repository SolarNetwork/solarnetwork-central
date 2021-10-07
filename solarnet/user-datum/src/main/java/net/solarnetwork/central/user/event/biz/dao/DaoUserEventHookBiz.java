/* ==================================================================
 * DaoUserEventHookBiz.java - 11/06/2020 9:34:34 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.biz.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.domain.UserRelatedIdentifiableConfiguration;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.dao.UserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.StringUtils;

/**
 * DAO implementation of {@link UserEventHookBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoUserEventHookBiz implements UserEventHookBiz {

	private final UserNodeEventHookConfigurationDao nodeEventHookConfigurationDao;

	private OptionalServiceCollection<UserNodeEventHookService> nodeEventHookServices;
	private OptionalServiceCollection<DatumAppEventProducer> datumEventProducers;
	private MessageSource messageSource;

	/**
	 * Constructor.
	 * 
	 * @param nodeEventHookConfigurationDao
	 *        the node event hook DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserEventHookBiz(UserNodeEventHookConfigurationDao nodeEventHookConfigurationDao) {
		super();
		if ( nodeEventHookConfigurationDao == null ) {
			throw new IllegalArgumentException(
					"The nodeEventHookConfigurationDao argument must not be null.");
		}
		this.nodeEventHookConfigurationDao = nodeEventHookConfigurationDao;
	}

	@Override
	public Iterable<UserNodeEventHookService> availableNodeEventHookServices() {
		OptionalServiceCollection<UserNodeEventHookService> svcs = getNodeEventHookServices();
		return (svcs != null ? svcs.services() : Collections.emptyList());
	}

	@Override
	public Iterable<DatumAppEventProducer> availableDatumEventProducers() {
		OptionalServiceCollection<DatumAppEventProducer> svcs = getDatumEventProducers();
		return (svcs != null ? svcs.services() : Collections.emptyList());
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableDatumEventTopics(Locale locale) {
		OptionalServiceCollection<DatumAppEventProducer> svcs = getDatumEventProducers();
		Iterable<DatumAppEventProducer> producers = (svcs != null ? svcs.services()
				: Collections.emptyList());
		List<LocalizedServiceInfo> results = new ArrayList<>(10);
		Set<String> handledTopics = new HashSet<>(10);
		for ( DatumAppEventProducer producer : producers ) {
			Set<String> topics = producer.getProducedDatumAppEventTopics();
			for ( String topic : topics ) {
				// don't add duplicate topics... first come, first serve
				if ( handledTopics.contains(topic) ) {
					continue;
				}
				handledTopics.add(topic);

				String name = topic;
				String desc = null;

				// first look if we have a "standard" i18n message
				if ( messageSource != null ) {
					name = messageSource.getMessage("event.topic." + topic + ".title", null, topic,
							locale);
					desc = messageSource.getMessage("event.topic." + topic + ".desc", null, null,
							locale);
				}
				if ( desc == null ) {
					MessageSource serviceMessageSource = producer.getMessageSource();
					if ( serviceMessageSource != null ) {
						name = serviceMessageSource.getMessage("event.topic." + topic + ".title", null,
								topic, locale);
						desc = serviceMessageSource.getMessage("event.topic." + topic + ".desc", null,
								null, locale);
					}
				}

				results.add(new BasicLocalizedServiceInfo(topic, locale, name, desc, null));
			}
		}
		Collections.sort(results, LocalizedServiceInfo.SORT_BY_NAME);
		return results;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserRelatedIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		if ( userId == null ) {
			throw new IllegalArgumentException("The userId argument must not be null.");
		}
		if ( id == null ) {
			throw new IllegalArgumentException("The id argument must not be null.");
		}
		if ( UserNodeEventHookConfiguration.class.isAssignableFrom(configurationClass) ) {
			T result = (T) nodeEventHookConfigurationDao.get(new UserLongPK(userId, id));
			if ( result == null ) {
				throw new DataRetrievalFailureException(
						String.format("%s entity not found with ID %d for user %d",
								configurationClass.getName(), id, userId));
			}
			return result;
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	private Iterable<? extends SettingSpecifierProvider> providersForServiceProperties(
			Class<? extends UserRelatedIdentifiableConfiguration> configurationClass) {
		if ( UserNodeEventHookConfiguration.class.isAssignableFrom(configurationClass) ) {
			return availableNodeEventHookServices();
		}
		return Collections.emptyList();
	}

	private List<SettingSpecifier> settingsForService(String identifier,
			Iterable<? extends SettingSpecifierProvider> providers) {
		if ( identifier != null && providers != null ) {
			for ( SettingSpecifierProvider provider : providers ) {
				if ( identifier.equals(provider.getSettingUid()) ) {
					return provider.getSettingSpecifiers();
				}
			}
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private <T extends UserRelatedIdentifiableConfiguration> T mergeServiceProperties(T entity) {
		if ( entity == null || entity.getId() == null || entity.getId().getId() == null ) {
			return entity;
		}
		Map<String, ?> serviceProps = entity.getServiceProperties();
		if ( serviceProps == null || serviceProps.isEmpty() ) {
			return entity;
		}
		UserRelatedIdentifiableConfiguration existing = configurationForUser(entity.getUserId(),
				entity.getClass(), entity.getId().getId());
		if ( existing == null ) {
			return entity;
		}
		Map<String, ?> existingServiceProps = existing.getServiceProperties();
		if ( existingServiceProps == null || existingServiceProps.isEmpty() ) {
			return entity;
		}
		Iterable<? extends SettingSpecifierProvider> providers = providersForServiceProperties(
				entity.getClass());
		List<SettingSpecifier> settings = settingsForService(entity.getServiceIdentifier(), providers);
		Set<String> secureEntrySettings = SettingUtils.secureKeys(settings);
		for ( String secureKey : secureEntrySettings ) {
			Object val = serviceProps.get(secureKey);
			String secureVal = (val != null ? val.toString() : "");
			if ( secureVal.isEmpty()
					|| StringUtils.DIGEST_PREFIX_PATTERN.matcher(secureVal).matches() ) {
				// secure value is provided that is empty or is already a digest value; do not change existing value
				Object existingVal = existingServiceProps.get(secureKey);
				if ( existingVal != null ) {
					((Map<String, Object>) serviceProps).put(secureKey, existingVal);
				}
			}
		}
		return entity;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserLongPK saveConfiguration(UserRelatedIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserNodeEventHookConfiguration ) {
			return nodeEventHookConfigurationDao
					.save((UserNodeEventHookConfiguration) mergeServiceProperties(configuration));
		}
		throw new IllegalArgumentException("Unsupported configuration type: " + configuration);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteConfiguration(UserRelatedIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserNodeEventHookConfiguration ) {
			nodeEventHookConfigurationDao.delete((UserNodeEventHookConfiguration) configuration);
		} else {
			throw new IllegalArgumentException("Unsupported configuration type: " + configuration);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserRelatedIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		if ( UserNodeEventHookConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) nodeEventHookConfigurationDao.findConfigurationsForUser(userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	/**
	 * Get the node event hook service collection.
	 * 
	 * @return the service collection
	 * @since 1.1
	 */
	public OptionalServiceCollection<UserNodeEventHookService> getNodeEventHookServices() {
		return nodeEventHookServices;
	}

	/**
	 * Set the node event hook service collection.
	 * 
	 * @param nodeEventHookServices
	 *        the service collection to set
	 * @since 1.1
	 */
	public void setNodeEventHookServices(
			OptionalServiceCollection<UserNodeEventHookService> nodeEventHookServices) {
		this.nodeEventHookServices = nodeEventHookServices;
	}

	/**
	 * Get the datum event producer collection.
	 * 
	 * @return the collection
	 * @since 1.1
	 */
	public OptionalServiceCollection<DatumAppEventProducer> getDatumEventProducers() {
		return datumEventProducers;
	}

	/**
	 * Set the datum event producer collection.
	 * 
	 * @param datumEventProducers
	 *        the collection to set
	 * @since 1.1
	 */
	public void setDatumEventProducers(
			OptionalServiceCollection<DatumAppEventProducer> datumEventProducers) {
		this.datumEventProducers = datumEventProducers;
	}

	/**
	 * Get a message source for resolving messages with.
	 * 
	 * @return the message source
	 * @since 1.1
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a message source for resolving messages with.
	 * 
	 * @param messageSource
	 *        the message source
	 * @since 1.1
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
