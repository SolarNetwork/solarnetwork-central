/* ==================================================================
 * CsvFilteredResultsProcessor.java - 18/11/2022 11:16:09 am
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.MimeType;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.codec.PropertySerializerRegistrar;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Basic {@link FilteredResultsProcessor} that serializes to CSV.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvFilteredResultsProcessor<R> extends AbstractFilteredResultsProcessor<R> {

	/** The {@literal text/csv} MIME type. */
	public static final MimeType TEXT_CSV_MIME_TYPE = MimeType.valueOf("text/csv");

	/**
	 * The default value for the <code>javaBeanIgnoreProperties</code> property.
	 */
	public static final Set<String> DEFAULT_JAVA_BEAN_IGNORE_PROPERTIES;
	static {
		DEFAULT_JAVA_BEAN_IGNORE_PROPERTIES = Collections.singleton("class");
	}

	/**
	 * The default value for the <code>javaBeanTreatAsStringValues</code>
	 * property.
	 */
	public static final Set<Class<?>> DEFAULT_JAVA_BEAN_STRING_VALUES;
	static {
		DEFAULT_JAVA_BEAN_STRING_VALUES = Collections.singleton(Class.class);
	}

	private final PropertySerializerRegistrar propertySerializerRegistrar;
	private final Set<String> javaBeanIgnoreProperties;
	private final Set<Class<?>> javaBeanTreatAsStringValues;
	private final boolean includeHeader;

	/** The MimeType. */
	private final MimeType mimeType;

	/** The output destination. */
	private final ICsvListWriter writer;

	/**
	 * A mapping of column names to associatd column index; linked so insertion
	 * order is column order.
	 */
	private Map<String, Integer> columnOrder = new LinkedHashMap<>(8);

	/** The count of output columns. */
	private int columnCount = 0;

	/** The current rowNum index. */
	private long rowNum = -1;

	/** A cache of properties that should be ignored. */
	private Map<Class<?>, Map<String, Boolean>> ignoredProperties = new HashMap<>(8);

	/** A cache of property orders. */
	private Map<Class<?>, String[]> propertyOrder = new HashMap<>(8);

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * The media type will be set to {@literal text/csv}; the
	 * {@code includeHeader} property will be set to {@literal true}.
	 * </p>
	 * 
	 * @param out
	 *        the output stream to write to
	 */
	public CsvFilteredResultsProcessor(Writer out) {
		this(out, true);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The media type will be set to {@literal text/csv}.
	 * </p>
	 * 
	 * @param out
	 *        the output stream to write to
	 * @param includeHeader
	 *        {@literal true} to include a header rowNum in the output
	 */
	public CsvFilteredResultsProcessor(Writer out, boolean includeHeader) {
		this(out, TEXT_CSV_MIME_TYPE, includeHeader, null, DEFAULT_JAVA_BEAN_IGNORE_PROPERTIES,
				DEFAULT_JAVA_BEAN_STRING_VALUES);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The media type will be set to {@literal text/csv}.
	 * </p>
	 * 
	 * @param out
	 *        the output stream to write to
	 * @param mimeType
	 *        the MIME type to use
	 * @param includeHeader
	 *        {@literal true} to include a header rowNum in the output
	 * @param propertySerializerRegistrar
	 *        a registrar to serialize properties with
	 */
	public CsvFilteredResultsProcessor(Writer out, MimeType mimeType, boolean includeHeader,
			PropertySerializerRegistrar propertySerializerRegistrar) {
		this(out, mimeType, includeHeader, propertySerializerRegistrar,
				DEFAULT_JAVA_BEAN_IGNORE_PROPERTIES, DEFAULT_JAVA_BEAN_STRING_VALUES);
	}

	/**
	 * Constructor.
	 * 
	 * @param out
	 *        the output stream to write to
	 * @param mimeType
	 *        the MIME type to use
	 * @param includeHeader
	 *        {@literal true} to include a header rowNum in the output
	 * @param propertySerializerRegistrar
	 *        a registrar to serialize properties with
	 * @param javaBeanIgnoreProperties
	 *        a set of JavaBean property names to ignore
	 * @param javaBeanTreatAsStringValues
	 *        a set of JavaBean property names to treat as strings
	 */
	public CsvFilteredResultsProcessor(Writer out, MimeType mimeType, boolean includeHeader,
			PropertySerializerRegistrar propertySerializerRegistrar,
			Set<String> javaBeanIgnoreProperties, Set<Class<?>> javaBeanTreatAsStringValues) {
		super();
		this.writer = new CsvListWriter(requireNonNullArgument(out, "out"),
				CsvPreference.STANDARD_PREFERENCE);
		this.mimeType = mimeType;
		this.includeHeader = includeHeader;
		this.propertySerializerRegistrar = propertySerializerRegistrar;
		this.javaBeanIgnoreProperties = requireNonNullArgument(javaBeanIgnoreProperties,
				"javaBeanIgnoreProperties");
		this.javaBeanTreatAsStringValues = requireNonNullArgument(javaBeanTreatAsStringValues,
				"javaBeanTreatAsStringValues");
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.flush();
		writer.close();
	}

	@Override
	public MimeType getMimeType() {
		return mimeType;
	}

	@Override
	public void handleResultItem(R item) throws IOException {
		if ( item == null ) {
			return;
		}

		Map<String, Object> rowProperties = extractProperties(item);
		if ( rowProperties == null ) {
			return;
		}

		rowNum += 1;

		if ( rowNum == 0 && includeHeader ) {
			// writer header based only one first item's properties
			writer.writeHeader(columnOrder.keySet().stream().toArray(String[]::new));
		}

		Object[] row = new Object[columnCount];
		int i = 0;
		for ( String key : columnOrder.keySet() ) {
			row[i++] = rowProperties.get(key);
		}
		writer.write(row);
	}

	private void updateColumnOrder(String key) {
		columnOrder.computeIfAbsent(key, aKey -> {
			int size = columnOrder.size();
			columnCount = size + 1;
			return size;
		});
	}

	private Map<String, Object> extractProperties(Object item) {
		if ( propertySerializerRegistrar != null ) {
			// try whole-bean serialization first
			item = propertySerializerRegistrar.serializeProperty("rowNum", item.getClass(), item, item);
			if ( item == null ) {
				return null;
			}
		}
		if ( item instanceof Map<?, ?> map ) {
			Map<String, Object> result = new HashMap<>(map.size());
			for ( Entry<?, ?> e : map.entrySet() ) {
				Object k = e.getKey();
				if ( k == null ) {
					continue;
				}
				String key = k.toString();
				Object val = getRowPropertyValue(item, key, e.getValue(), null);
				if ( val != null ) {
					updateColumnOrder(key);
					result.put(key, val);
				}
			}
		} else {
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
			PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
			final Class<?> clazz = item.getClass();
			if ( descriptors != null ) {
				String[] propOrder = propertyOrder.computeIfAbsent(clazz, k -> {
					JsonPropertyOrder order = AnnotationUtils.findAnnotation(clazz,
							JsonPropertyOrder.class);
					if ( order != null ) {
						return order.value();
					}
					return new String[0];
				});
				if ( propOrder.length > 0 ) {
					Arrays.sort(descriptors, (l, r) -> {
						int lIdx = -1;
						int rIdx = -1;
						for ( int i = 0; i < propOrder.length && lIdx < 0 && rIdx < 0; i++ ) {
							if ( propOrder[i].equals(l.getName()) ) {
								lIdx = i;
							} else if ( propOrder[i].equals(r.getName()) ) {
								rIdx = i;
							}
						}
						return Integer.compare(rIdx, lIdx);
					});
				}
				Map<String, Object> result = new HashMap<>(descriptors.length);
				for ( PropertyDescriptor desc : descriptors ) {
					String key = desc.getName();
					if ( javaBeanIgnoreProperties.contains(key) ) {
						continue;
					}
					if ( wrapper.isReadableProperty(key) && !shouldIgnoreProperty(item, key, desc) ) {
						Object val = wrapper.getPropertyValue(key);
						val = getRowPropertyValue(item, key, val, wrapper);
						if ( val != null ) {
							updateColumnOrder(key);
							result.put(key, val);
						}
					}
				}
				return result;
			}
		}
		return null;
	}

	/**
	 * Return {@literal true} if a given JavaBean property should be ignored due
	 * to annotations.
	 * 
	 * <p>
	 * The {@link JsonIgnore}, {@link SerializeIgnore}, and
	 * {@link JsonIgnoreProperties} annotations are supported.
	 * </p>
	 * 
	 * @param item
	 *        the object
	 * @param key
	 *        the property name
	 * @param desc
	 *        the property descriptor
	 * @return {@literal true} if the property should be ignored
	 */
	private boolean shouldIgnoreProperty(Object item, String key, PropertyDescriptor desc) {
		Map<String, Boolean> classMap = ignoredProperties.get(item.getClass());
		if ( classMap != null && classMap.containsKey(key) ) {
			return classMap.get(key);
		}
		boolean result = false;
		Method m = desc.getReadMethod();
		if ( AnnotationUtils.getAnnotation(m, JsonIgnore.class) != null
				|| AnnotationUtils.getAnnotation(m, SerializeIgnore.class) != null ) {
			result = true;
		} else {
			JsonIgnoreProperties annot = AnnotationUtils.findAnnotation(item.getClass(),
					JsonIgnoreProperties.class);
			if ( annot != null ) {
				String[] ignored = annot.value();
				if ( ignored != null ) {
					for ( String prop : ignored ) {
						if ( prop.equals(key) ) {
							result = true;
							break;
						}
					}
				}
			}
		}
		ignoredProperties.computeIfAbsent(item.getClass(), k -> new HashMap<>(8)).put(key, result);
		return result;
	}

	private Object getRowPropertyValue(Object row, String name, Object val, BeanWrapper wrapper) {
		if ( val != null ) {
			if ( getPropertySerializerRegistrar() != null ) {
				val = getPropertySerializerRegistrar().serializeProperty(name, val.getClass(), row, val);
			} else if ( wrapper != null ) {
				// Spring does not apply PropertyEditors on read methods, so manually handle
				PropertyEditor editor = wrapper.findCustomEditor(null, name);
				if ( editor != null ) {
					editor.setValue(val);
					val = editor.getAsText();
				}
			}
			if ( val instanceof Enum<?> || javaBeanTreatAsStringValues != null
					&& javaBeanTreatAsStringValues.contains(val.getClass()) ) {
				val = val.toString();
			}
		}
		return val;
	}

	/**
	 * Get the property serializer registrar.
	 * 
	 * @return the registrar
	 */
	public PropertySerializerRegistrar getPropertySerializerRegistrar() {
		return propertySerializerRegistrar;
	}

	/**
	 * Get the JavaBean properties to ignore.
	 * 
	 * @return the properties
	 */
	public Set<String> getJavaBeanIgnoreProperties() {
		return javaBeanIgnoreProperties;
	}

	/**
	 * Get the JavaBean classes to treat as strings.
	 * 
	 * @return the class set
	 */
	public Set<Class<?>> getJavaBeanTreatAsStringValues() {
		return javaBeanTreatAsStringValues;
	}

	/**
	 * Get the "include header" option.
	 * 
	 * @return {@literal true} to include a header rowNum in the output;
	 *         defaults to {@literal true}
	 */
	public boolean isIncludeHeader() {
		return includeHeader;
	}

}
