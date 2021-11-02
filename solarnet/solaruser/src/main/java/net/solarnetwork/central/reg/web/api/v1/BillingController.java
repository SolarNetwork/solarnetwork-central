/* ==================================================================
 * BillingController.java - 25/08/2017 7:32:16 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1;

import static java.lang.String.format;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.web.domain.Response.response;
import java.time.YearMonth;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BasicInvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilterCommand;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;
import net.solarnetwork.central.user.billing.support.LocalizedInvoiceMatchFilterResults;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for billing management.
 * 
 * @author matt
 * @version 2.0
 */
@RestController("v1BillingController")
@RequestMapping(value = { "/u/sec/billing", "/v1/sec/user/billing" })
@GlobalExceptionRestController
public class BillingController {

	private final BillingBiz billingBiz;

	/**
	 * Constructor.
	 * 
	 * @param billingBiz
	 *        the billing biz to use
	 */
	public BillingController(@Autowired(required = false) BillingBiz billingBiz) {
		super();
		this.billingBiz = requireNonNullArgument(billingBiz, "billingBiz");
	}

	private BillingBiz billingBiz() {
		if ( billingBiz == null ) {
			throw new UnsupportedOperationException("Billing service not available.");
		}
		return billingBiz;
	}

	/**
	 * Get billing system info for the current user.
	 * 
	 * @param locale
	 *        the Locale of the request
	 * @return the billing system info
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/systemInfo")
	public Response<BillingSystemInfo> billingSystemInfoForUser(Locale locale) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		BillingBiz biz = billingBiz();
		BillingSystem system = biz.billingSystemForUser(userId);
		BillingSystemInfo info = (system != null ? system.getInfo(locale) : null);
		return response(info);
	}

	/**
	 * Get a single invoice with full details.
	 * 
	 * @param invoiceId
	 *        the ID of the invoice to get
	 * @param userId
	 *        the optional user ID to get the invoice for; if not provided the
	 *        current actor's ID is used
	 * @param locale
	 *        the request locale
	 * @return the invoice
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/{invoiceId}", method = RequestMethod.GET)
	public Response<Invoice> getInvoice(@PathVariable("invoiceId") String invoiceId,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		Invoice result = biz.getInvoice(userId, invoiceId, locale);

		// localize the response
		if ( result != null && !(result instanceof LocalizedInvoiceInfo) ) {
			if ( locale == null ) {
				locale = Locale.getDefault();
			}
			result = new LocalizedInvoice(result, locale);
		}

		return response(result);
	}

	/**
	 * Render an invoice.
	 * 
	 * @param invoiceId
	 *        the invoice ID to render
	 * @param accept
	 *        an optional output type, defaults to {@literal text/html}
	 * @param userId
	 *        the optional user ID to get the invoice for; if not provided the
	 *        current actor's ID is used
	 * @param locale
	 *        the request locale
	 * @return the rendered invoice entity
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/{invoiceId}/render", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderInvoice(@PathVariable("invoiceId") String invoiceId,
			@RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = "text/html") String accept,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		MediaType outputType = acceptTypes.isEmpty() ? MediaType.TEXT_HTML
				: acceptTypes.get(0).removeQualityValue();
		Resource result = biz.renderInvoice(userId, invoiceId, outputType, locale);
		if ( result != null ) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(outputType);
			if ( !outputType.isCompatibleWith(MediaType.TEXT_HTML) ) {
				// add "attachment" header with suggested file name based on invoice
				headers.set(HttpHeaders.CONTENT_DISPOSITION,
						format("attachment; filename=\"%s\"", result.getFilename()));
			}
			return new ResponseEntity<Resource>(result, headers, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	/**
	 * Render an invoice as PDF.
	 * 
	 * @param invoiceId
	 *        the invoice ID to render
	 * @param userId
	 *        the optional user ID to get the invoice for; if not provided the
	 *        current actor's ID is used
	 * @param locale
	 *        the request locale
	 * @return the rendered invoice entity
	 * @since 1.3
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/{invoiceId}/render/pdf", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderPdfInvoice(@PathVariable("invoiceId") String invoiceId,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		return renderInvoice(invoiceId, "application/pdf", userId, locale);
	}

	/**
	 * Find matching invoices.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param locale
	 *        the locale
	 * @return the search results
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/list", method = RequestMethod.GET)
	public Response<FilterResults<InvoiceMatch>> findFilteredInvoices(InvoiceFilterCommand filter,
			Locale locale) {
		BillingBiz biz = billingBiz();
		if ( filter.getUserId() == null ) {
			filter.setUserId(SecurityUtils.getCurrentActorUserId());
		}
		FilterResults<InvoiceMatch> results = biz.findFilteredInvoices(filter,
				filter.getSortDescriptors(), filter.getOffset(), filter.getMax());

		// localize the response
		if ( results.getReturnedResultCount() != null && results.getReturnedResultCount() > 0 ) {
			if ( locale == null ) {
				locale = Locale.getDefault();
			}
			results = new LocalizedInvoiceMatchFilterResults(results, locale);
		}

		return response(results);
	}

	/** A YYYY-MM date format to use for parsing a billing month. */
	public static final DateTimeFormatter MONTH_FORMAT;
	static {
		// @formatter:off
		MONTH_FORMAT = new DateTimeFormatterBuilder()
				.parseStrict()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .toFormatter()
                .withChronology(IsoChronology.INSTANCE);
		// @formatter:on
	}

	/**
	 * Preview the current billing cycle's invoice.
	 * 
	 * @param userId
	 *        the optional user ID to get the invoice for; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the rendered invoice entity
	 * @since 1.4
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/preview", method = RequestMethod.GET)
	public Response<Invoice> previewInvoice(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		YearMonth date = null;
		if ( month != null && !month.isEmpty() ) {
			date = MONTH_FORMAT.parse(month, YearMonth::from);
		}

		Invoice result = biz.getPreviewInvoice(userId,
				new BasicInvoiceGenerationOptions(date, useCredit), locale);

		// localize the response
		if ( result != null && !(result instanceof LocalizedInvoiceInfo) ) {
			if ( locale == null ) {
				locale = Locale.getDefault();
			}
			result = new LocalizedInvoice(result, locale);
		}

		return response(result);
	}

	/**
	 * Render a preview of the current billing cycle's invoice.
	 * 
	 * @param accept
	 *        an optional output type, defaults to {@literal text/html}
	 * @param userId
	 *        the optional user ID to get the invoice for; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the rendered invoice entity
	 * @since 1.4
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/preview/render", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderPreviewInvoice(
			@RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = "text/html") String accept,
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		MediaType outputType = acceptTypes.isEmpty() ? MediaType.TEXT_HTML
				: acceptTypes.get(0).removeQualityValue();
		YearMonth date = null;
		if ( month != null && !month.isEmpty() ) {
			date = MONTH_FORMAT.parse(month, YearMonth::from);
		}
		Resource result = biz.previewInvoice(userId, new BasicInvoiceGenerationOptions(date, useCredit),
				outputType, locale);
		if ( result != null ) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(outputType);
			if ( !outputType.isCompatibleWith(MediaType.TEXT_HTML) ) {
				// add "attachment" header with suggested file name based on invoice
				headers.set(HttpHeaders.CONTENT_DISPOSITION,
						format("attachment; filename=\"%s\"", result.getFilename()));
			}
			return new ResponseEntity<Resource>(result, headers, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	/**
	 * Render a preview of the current billing cycle's invoice as a PDF.
	 * 
	 * @param userId
	 *        the optional user ID to get the invoice for; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the rendered invoice entity
	 * @since 1.4
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/preview/render/pdf", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderPreviewInvoicePdf(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		return renderPreviewInvoice("application/pdf", userId, month, useCredit, locale);
	}

}
