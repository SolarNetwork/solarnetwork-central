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

import static net.solarnetwork.central.datum.v2.support.DatumJsonUtils.GENERAL_NODE_DATUM_TYPE;
import static net.solarnetwork.central.datum.v2.support.DatumJsonUtils.OBJECT_TYPE_FIELD;
import static net.solarnetwork.central.datum.v2.support.DatumJsonUtils.getStringFieldValue;
import static net.solarnetwork.central.datum.v2.support.DatumJsonUtils.parseDatum;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
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
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.TooManyStreamedResultsException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.support.AbstractFilteredResultsProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * JSON implementation of bulk upload service.
 *
 * @author matt
 * @version 3.6
 */
@Controller
@RequestMapping(value = { "/solarin/bulkCollector.do", "/solarin/u/bulkCollector.do" },
		consumes = "application/json")
public class BulkJsonDataCollector extends AbstractDataCollector {

	/** The InstructionStatus type. */
	public static final String INSTRUCTION_STATUS_TYPE = "InstructionStatus";

	/**
	 * The JSON field name for a location ID on a {@link GeneralLocationDatum}
	 * value.
	 */
	public static final String LOCATION_ID_FIELD = "locationId";

	/**
	 * The JSON field name for an instruction ID on a {@link Instruction} value.
	 *
	 * @since 2.2
	 */
	public static final String INSTRUCTION_ID_FIELD = "instructionId";

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
	public Result<?> handleRuntimeException(RuntimeException e, HttpServletResponse response) {
		log.error("RuntimeException in {} controller", getClass().getSimpleName(), e);
		return new Result<>(Boolean.FALSE, null, "Internal error", null);
	}

	/**
	 * Post new data.
	 *
	 * <p>
	 * If {@code encoding} contains {@code gzip} the InputStream itself is
	 * assumed to be compressed with GZip and encoded as Base64. Otherwise, the
	 * InputStream is assumed to be regular text (not compressed).
	 * </p>
	 *
	 * @param encoding
	 *        an optional encoding value
	 * @param in
	 *        the request input stream
	 * @param model
	 *        the model
	 * @return the result model
	 * @throws IOException
	 *         if any IO error occurs
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	public Result<BulkUploadResult> postData(
			@RequestHeader(value = "Content-Encoding", required = false) String encoding, InputStream in,
			Model model) throws IOException {
		AuthenticatedNode authNode = getAuthenticatedNode(true);

		InputStream input = in;
		if ( encoding != null && encoding.toLowerCase().contains("gzip") ) {
			input = new GZIPInputStream(in);
		}

		List<GeneralNodeDatum> parsedGeneralNodeDatum = new ArrayList<>();
		List<GeneralLocationDatum> parsedGeneralLocationDatum = new ArrayList<>();
		List<StreamDatum> parsedStreamDatum = new ArrayList<>();
		List<Object> resultDatum = new ArrayList<>();

		try {
			JsonNode tree = objectMapper.readTree(input);
			log.trace("Got JSON: {}", tree);
			if ( tree.isArray() ) {
				for ( JsonNode child : tree ) {
					Object o = handleNode(child);
					if ( o instanceof Datum ) {
						// convert to legacy form for compatibility between node 1.0/2.0
						o = DatumUtils.convertGeneralDatum((Datum) o);
					}
					if ( o instanceof StreamDatum ) {
						parsedStreamDatum.add((StreamDatum) o);
					} else if ( o instanceof GeneralNodeDatum ) {
						parsedGeneralNodeDatum.add((GeneralNodeDatum) o);
					} else if ( o instanceof GeneralLocationDatum ) {
						parsedGeneralLocationDatum.add((GeneralLocationDatum) o);
					} else if ( o instanceof Instruction ) {
						resultDatum.add(o);
					} else {
						log.warn("Discarding unknown JSON object {} parsed as {}", child, o);
					}
				}
			}
		} finally {
			if ( input != null ) {
				input.close();
			}
		}

		try {
			if ( !parsedStreamDatum.isEmpty() ) {
				getDataCollectorBiz().postStreamDatum(parsedStreamDatum);
				for ( StreamDatum d : parsedStreamDatum ) {
					resultDatum.add(new DatumPK(d.getStreamId(), d.getTimestamp()));
				}
			}
			if ( !parsedGeneralNodeDatum.isEmpty() ) {
				getDataCollectorBiz().postGeneralNodeDatum(parsedGeneralNodeDatum);
				for ( GeneralNodeDatum d : parsedGeneralNodeDatum ) {
					resultDatum.add(d.getId());
				}
			}
			if ( !parsedGeneralLocationDatum.isEmpty() ) {
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
				log.debug("RepeatableTaskException caused by: {}", root.getMessage());
			}
		}

		BulkUploadResult result = new BulkUploadResult();
		if ( !resultDatum.isEmpty() ) {
			result.setDatum(resultDatum);
			log.trace("Upload result datum: {}", resultDatum);
		}

		// add instructions for the node
		final InstructorBiz instructorBiz = getInstructorBiz();
		if ( instructorBiz != null ) {
			List<Instruction> instructions = new ArrayList<>(2);
			var filter = new SimpleInstructionFilter();
			filter.setNodeId(authNode.getNodeId());
			filter.setState(InstructionState.Queued);
			try (FilteredResultsProcessor<NodeInstruction> processor = new AbstractFilteredResultsProcessor<NodeInstruction>() {

				@Override
				public void handleResultItem(NodeInstruction resultItem) throws IOException {
					instructions.add(resultItem);
					if ( instructions.size() >= 100 ) {
						throw new TooManyStreamedResultsException();
					}
				}

			}) {
				instructorBiz.findFilteredNodeInstructions(filter, processor);
			} catch ( Exception e ) {
				// just stop processing and continue
			}

			if ( !instructions.isEmpty() ) {
				result.setInstructions(instructions);
			}
		}

		return new Result<>(result);
	}

	private Object handleNode(JsonNode node) {
		JsonNode instrId = node.get(INSTRUCTION_ID_FIELD);
		if ( (instrId != null && instrId.isNumber()) || INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(
				getStringFieldValue(node, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE)) ) {
			return handleInstructionStatus(node);
		}
		try {
			return parseDatum(objectMapper, node);
		} catch ( IOException e ) {
			log.warn("Unable to parse JSON {}: {}", node, e.getMessage());
		}
		return null;
	}

	private Instruction handleInstructionStatus(JsonNode node) {
		String instructionId = getStringFieldValue(node, INSTRUCTION_ID_FIELD, null);
		String state = getStringFieldValue(node, "state", null);
		if ( state == null ) {
			// fall back to legacy
			state = getStringFieldValue(node, "status", null);
		}
		Map<String, Object> resultParams = JsonUtils.getStringMapFromTree(node.get("resultParameters"));
		Instruction result = null;
		final InstructorBiz biz = getInstructorBiz();
		if ( instructionId != null && state != null && biz != null ) {
			Long id = Long.valueOf(instructionId);
			InstructionState s = InstructionState.valueOf(state);
			biz.updateInstructionState(id, s, resultParams);
			result = new Instruction();
			result.setId(id);
			result.setState(s);
			result.setResultParameters(resultParams);
			return result;
		}
		return result;
	}

}
