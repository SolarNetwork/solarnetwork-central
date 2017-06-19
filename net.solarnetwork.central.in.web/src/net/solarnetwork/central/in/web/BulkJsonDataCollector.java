/* ==================================================================
 * BulkJsonDataCollector.java - Aug 25, 2014 10:53:53 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.HardwareControlDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.web.domain.Response;

/**
 * JSON implementation of bulk upload service.
 * 
 * @author matt
 * @version 1.4
 */
@Controller
@RequestMapping(value = { "/bulkCollector.do", "/u/bulkCollector.do" }, consumes = "application/json")
public class BulkJsonDataCollector extends AbstractDataCollector {

	/** The JSON field name for an "object type". */
	public static final String OBJECT_TYPE_FIELD = "__type__";

	/** The InstructionStatus type. */
	public static final String INSTRUCTION_STATUS_TYPE = "InstructionStatus";

	/** The NodeControlInfo type. */
	public static final String NODE_CONTROL_INFO_TYPE = "NodeControlInfo";

	/** The {@link GeneralNodeDatum} or {@link GeneralLocationDatum} type. */
	public static final String GENERAL_NODE_DATUM_TYPE = "datum";

	/**
	 * The JSON field name for a location ID on a {@link GeneralLocationDatum}
	 * value.
	 */
	public static final String LOCATION_ID_FIELD = "locationId";

	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz
	 *        the {@link DataCollectorBiz} to use
	 * @param solarNodeDao
	 *        the {@link SolarNodeDao} to use
	 * @param objectMapper
	 *        the {@link ObjectMapper} to use
	 */
	@Autowired
	public BulkJsonDataCollector(DataCollectorBiz dataCollectorBiz, SolarNodeDao solarNodeDao,
			ObjectMapper objectMapper) {
		setDataCollectorBiz(dataCollectorBiz);
		setSolarNodeDao(solarNodeDao);
		this.objectMapper = objectMapper;
	}

	/**
	 * Handle a {@link RuntimeException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 */
	@ExceptionHandler(RuntimeException.class)
	@ResponseBody
	public Response<?> handleRuntimeException(RuntimeException e, HttpServletResponse response) {
		log.error("RuntimeException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Internal error", null);
	}

	/**
	 * Post new data.
	 * 
	 * <p>
	 * If {@code encoding} contains {@code gzip} the InputStream itself is
	 * assumed to be compressed with GZip and encoded as Base64. Otherwise the
	 * InputStream is assumed to be regular text (not compressed).
	 * </p>
	 * 
	 * @param encoding
	 *        an optional encoding value
	 * @param in
	 *        the request input stream
	 * @return the result model
	 * @throws IOException
	 *         if any IO error occurs
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	public Response<BulkUploadResult> postData(
			@RequestHeader(value = "Content-Encoding", required = false) String encoding, InputStream in,
			Model model) throws IOException {
		AuthenticatedNode authNode = getAuthenticatedNode(true);

		InputStream input = in;
		if ( encoding != null && encoding.toLowerCase().contains("gzip") ) {
			input = new GZIPInputStream(in);
		}

		List<Datum> parsedDatum = new ArrayList<Datum>();
		List<GeneralNodeDatum> parsedGeneralNodeDatum = new ArrayList<GeneralNodeDatum>();
		List<GeneralLocationDatum> parsedGeneralLocationDatum = new ArrayList<GeneralLocationDatum>();
		List<Object> resultDatum = new ArrayList<Object>();

		try {
			JsonNode tree = objectMapper.readTree(input);
			if ( tree.isArray() ) {
				for ( JsonNode child : tree ) {
					Object o = handleNode(child);
					if ( o instanceof GeneralNodeDatum ) {
						parsedGeneralNodeDatum.add((GeneralNodeDatum) o);
					} else if ( o instanceof GeneralLocationDatum ) {
						parsedGeneralLocationDatum.add((GeneralLocationDatum) o);
					} else if ( o instanceof Datum ) {
						parsedDatum.add((Datum) o);
					} else if ( o instanceof Instruction ) {
						resultDatum.add(o);
					}
				}
			}
		} finally {
			if ( input != null ) {
				input.close();
			}
		}

		try {
			if ( parsedDatum.size() > 0 ) {
				@SuppressWarnings("deprecation")
				List<Datum> postedDatum = getDataCollectorBiz().postDatum(parsedDatum);
				resultDatum.addAll(postedDatum);
			}
			if ( parsedGeneralNodeDatum.size() > 0 ) {
				getDataCollectorBiz().postGeneralNodeDatum(parsedGeneralNodeDatum);
				for ( GeneralNodeDatum d : parsedGeneralNodeDatum ) {
					resultDatum.add(d.getId());
				}
			}
			if ( parsedGeneralLocationDatum.size() > 0 ) {
				getDataCollectorBiz().postGeneralLocationDatum(parsedGeneralLocationDatum);
				for ( GeneralLocationDatum d : parsedGeneralLocationDatum ) {
					resultDatum.add(d.getId());
				}
			}
		} catch ( RepeatableTaskException e ) {
			if ( log.isDebugEnabled() ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.debug("RepeatableTaskException caused by: " + root.getMessage());
			}
		}

		BulkUploadResult result = new BulkUploadResult();
		if ( resultDatum.size() > 0 ) {
			result.setDatum(resultDatum);
		}

		// add instructions for the node
		InstructorBiz instructorBiz = (getInstructorBiz() == null ? null : getInstructorBiz().service());
		if ( instructorBiz != null ) {
			List<Instruction> instructions = instructorBiz
					.getActiveInstructionsForNode(authNode.getNodeId());
			if ( instructions != null && instructions.size() > 0 ) {
				result.setInstructions(instructions);
			}
		}

		return new Response<BulkUploadResult>(result);
	}

	private Object handleNode(JsonNode node) {
		String nodeType = getStringFieldValue(node, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE);
		if ( GENERAL_NODE_DATUM_TYPE.equalsIgnoreCase(nodeType) ) {
			// if we have a location ID, this is actually a GeneralLocationDatum
			final JsonNode locId = node.get(LOCATION_ID_FIELD);
			if ( locId != null && locId.isNumber() ) {
				return handleGeneralLocationDatum(node);
			}
			return handleGeneralNodeDatum(node);
		} else if ( INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(nodeType) ) {
			return handleInstructionStatus(node);
		} else if ( NODE_CONTROL_INFO_TYPE.equalsIgnoreCase(nodeType) ) {
			return handleNodeControlInfo(node);
		} else {
			return handleLegacyDatum(node);
		}
	}

	private String getStringFieldValue(JsonNode node, String fieldName, String placeholder) {
		JsonNode child = node.get(fieldName);
		return (child == null ? placeholder : child.asText());
	}

	private HardwareControlDatum handleNodeControlInfo(JsonNode node) {
		HardwareControlDatum datum = null;
		String controlId = getStringFieldValue(node, "controlId", null);
		String propertyName = getStringFieldValue(node, "propertyName", null);
		String value = getStringFieldValue(node, "value", null);
		String type = getStringFieldValue(node, "type", null);
		if ( type != null && value != null ) {
			datum = new HardwareControlDatum();

			NodeControlPropertyType t = NodeControlPropertyType.valueOf(type);
			switch (t) {
				case Boolean:
					if ( value.length() > 0 && (value.equals("1") || value.equalsIgnoreCase("yes")
							|| value.equalsIgnoreCase("true")) ) {
						datum.setIntegerValue(Integer.valueOf(1));
					} else {
						datum.setIntegerValue(Integer.valueOf(0));
					}
					break;

				case Integer:
					datum.setIntegerValue(Integer.valueOf(value));
					break;

				case Float:
				case Percent:
					datum.setFloatValue(Float.valueOf(value));
					break;

			}
			String sourceId = controlId;
			if ( propertyName != null ) {
				sourceId += ";" + propertyName;
			}
			datum.setSourceId(sourceId);
		}

		return datum;
	}

	private Instruction handleInstructionStatus(JsonNode node) {
		String instructionId = getStringFieldValue(node, "instructionId", null);
		String status = getStringFieldValue(node, "status", null);
		Map<String, Object> resultParams = JsonUtils.getStringMapFromTree(node.get("resultParameters"));
		Instruction result = null;
		InstructorBiz biz = getInstructorBiz().service();
		if ( instructionId != null && status != null && biz != null ) {
			Long id = Long.valueOf(instructionId);
			InstructionState state = InstructionState.valueOf(status);
			biz.updateInstructionState(id, state, resultParams);
			result = new Instruction();
			result.setId(id);
			result.setState(state);
			result.setResultParameters(resultParams);
			return result;
		}
		return result;
	}

	private Object handleLegacyDatum(JsonNode node) {
		String className = getStringFieldValue(node, OBJECT_TYPE_FIELD, null);
		if ( className == null ) {
			return null;
		}
		className = "net.solarnetwork.central.datum.domain." + className;

		Class<?> datumClass = null;
		Object datum = null;
		try {
			datumClass = Class.forName(className, true, Datum.class.getClassLoader());
			datum = objectMapper.treeToValue(node, datumClass);
		} catch ( ClassNotFoundException e ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Unable to load Datum class " + className + " specified in JSON");
			}
			return null;
		} catch ( IOException e ) {
			log.debug("Unable to parse JSON into {} class: {}", className, e.getMessage());
			return null;
		}

		if ( log.isTraceEnabled() ) {
			log.trace("Parsed datum " + datum + " from JSON");
		}

		return datum;
	}

	private GeneralNodeDatum handleGeneralNodeDatum(JsonNode node) {
		try {
			return objectMapper.treeToValue(node, GeneralNodeDatum.class);
		} catch ( IOException e ) {
			log.debug("Unable to parse JSON into GeneralNodeDatum: {}", e.getMessage());
			return null;
		}
	}

	private GeneralLocationDatum handleGeneralLocationDatum(JsonNode node) {
		try {
			return objectMapper.treeToValue(node, GeneralLocationDatum.class);
		} catch ( IOException e ) {
			log.debug("Unable to parse JSON into GeneralLocationDatum: {}", e.getMessage());
			return null;
		}
	}

}
