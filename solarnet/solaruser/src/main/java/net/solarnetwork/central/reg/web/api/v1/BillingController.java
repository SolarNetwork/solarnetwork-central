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
import static net.solarnetwork.domain.Result.success;
import java.time.YearMonth;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
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
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BasicInvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.ConsolidatedInvoicePreviewStatus;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilterCommand;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewStatus;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;
import net.solarnetwork.central.user.billing.support.LocalizedInvoiceMatchFilterResults;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Web service API for billing management.
 *
 * @author matt
 * @version 2.4
 */
@RestController("v1BillingController")
@RequestMapping(value = { "/u/sec/billing", "/api/v1/sec/user/billing" })
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
		this.billingBiz = billingBiz;
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
	@RequestMapping(method = RequestMethod.GET, value = "/systemInfo", params = "!key")
	public Result<BillingSystemInfo> billingSystemInfoForUser(Locale locale) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		BillingBiz biz = billingBiz();
		BillingSystem system = biz.billingSystemForUser(userId);
		BillingSystemInfo info = (system != null ? system.getInfo(locale) : null);
		return success(info);
	}

	private static Invoice localized(Invoice result, @Nullable Locale locale) {
		if ( result instanceof LocalizedInvoiceInfo ) {
			return result;
		}
		return new LocalizedInvoice(result, (locale != null ? locale : Locale.getDefault()));
	}

	private static FilterResults<InvoiceMatch, String> localized(
			FilterResults<InvoiceMatch, String> results, @Nullable Locale locale) {
		if ( results.getReturnedResultCount() < 1 ) {
			return results;
		}
		return new LocalizedInvoiceMatchFilterResults(results,
				(locale != null ? locale : Locale.getDefault()));
	}

	private static MediaType outputType(String accept) {
		List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		return (acceptTypes.isEmpty() ? MediaType.TEXT_HTML
				: acceptTypes.getFirst().removeQualityValue());
	}

	private static @Nullable YearMonth month(@Nullable String month) {
		if ( month == null || month.isEmpty() ) {
			return null;
		}
		return MONTH_FORMAT.parse(month, YearMonth::from);
	}

	private static ResponseEntity<Resource> resourceResponse(@Nullable Resource result,
			MediaType outputType) {
		if ( result == null ) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(outputType);
		if ( !outputType.isCompatibleWith(MediaType.TEXT_HTML) ) {
			// add "attachment" header with suggested file name based on invoice
			headers.set(HttpHeaders.CONTENT_DISPOSITION,
					format("attachment; filename=\"%s\"", result.getFilename()));
		}
		return new ResponseEntity<>(result, headers, HttpStatus.OK);
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
	public Result<Invoice> getInvoice(@PathVariable String invoiceId,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		Invoice result = biz.getInvoice(userId, invoiceId, locale);
		return success(result != null ? localized(result, locale) : null);
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
	public ResponseEntity<Resource> renderInvoice(@PathVariable String invoiceId,
			@RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = "text/html") String accept,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		MediaType outputType = outputType(accept);
		return resourceResponse(biz.renderInvoice(userId, invoiceId, outputType, locale), outputType);
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
	public ResponseEntity<Resource> renderPdfInvoice(@PathVariable String invoiceId,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		return renderInvoice(invoiceId, "application/pdf", userId, locale);
	}

	/**
	 * Find matching invoices.
	 *
	 * <p>
	 * With the {@code includeConsolidated} criteria, the results include the
	 * consolidated invoices billed to the user, in place of the invoices they
	 * are made of.
	 * </p>
	 *
	 * @param filter
	 *        the search criteria
	 * @param locale
	 *        the locale
	 * @return the search results
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/list", method = RequestMethod.GET)
	public Result<FilterResults<InvoiceMatch, String>> findFilteredInvoices(InvoiceFilterCommand filter,
			Locale locale) {
		BillingBiz biz = billingBiz();
		if ( filter.getUserId() == null ) {
			filter.setUserId(SecurityUtils.getCurrentActorUserId());
		}
		FilterResults<InvoiceMatch, String> results = biz.findFilteredInvoices(filter,
				filter.getSortDescriptors(), filter.getOffset(), filter.getMax());
		return success(localized(results, locale));
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
	public Result<Invoice> previewInvoice(@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		Invoice result = biz.getPreviewInvoice(userId,
				new BasicInvoiceGenerationOptions(month(month), useCredit), locale);
		return success(result != null ? localized(result, locale) : null);
	}

	/**
	 * Get the status of the current billing cycle's preview invoice, without
	 * waiting for it to be generated.
	 *
	 * <p>
	 * The preview is started if needed. Request again until the state is
	 * {@code Done} or {@code Failed}.
	 * </p>
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
	 * @return the preview status, with the invoice when done
	 * @since 2.4
	 */
	@ResponseBody
	@RequestMapping(value = "/invoices/preview/status", method = RequestMethod.GET)
	public Result<InvoicePreviewStatus> previewInvoiceStatus(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		InvoicePreviewStatus result = biz.getPreviewInvoiceStatus(userId,
				new BasicInvoiceGenerationOptions(month(month), useCredit), locale);
		final Invoice invoice = (result != null ? result.invoice() : null);
		return success(result != null && invoice != null
				? new InvoicePreviewStatus(result.state(), localized(invoice, locale))
				: result);
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
		MediaType outputType = outputType(accept);
		return resourceResponse(biz.previewInvoice(userId,
				new BasicInvoiceGenerationOptions(month(month), useCredit), outputType, locale),
				outputType);
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

	/**
	 * Find the consolidated invoices billed to a user.
	 *
	 * @param filter
	 *        the search criteria; the user is the primary account user
	 * @param locale
	 *        the locale
	 * @return the search results
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/list", method = RequestMethod.GET)
	public Result<FilterResults<InvoiceMatch, String>> findFilteredConsolidatedInvoices(
			InvoiceFilterCommand filter, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( filter.getUserId() == null ) {
			filter.setUserId(SecurityUtils.getCurrentActorUserId());
		}
		FilterResults<InvoiceMatch, String> results = biz.findFilteredConsolidatedInvoices(filter,
				filter.getSortDescriptors(), filter.getOffset(), filter.getMax());
		return success(localized(results, locale));
	}

	/**
	 * Get a consolidated invoice, with a summary of the account invoices it is
	 * made of.
	 *
	 * @param invoiceId
	 *        the ID of the consolidated invoice to get
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param locale
	 *        the request locale
	 * @return the consolidated invoice
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/{invoiceId}", method = RequestMethod.GET)
	public Result<Invoice> getConsolidatedInvoice(@PathVariable String invoiceId,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		Invoice result = biz.getConsolidatedInvoice(userId, invoiceId, locale);
		return success(result != null ? localized(result, locale) : null);
	}

	/**
	 * Render a consolidated invoice.
	 *
	 * @param invoiceId
	 *        the ID of the consolidated invoice to render
	 * @param accept
	 *        an optional output type, defaults to {@literal text/html}
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param locale
	 *        the request locale
	 * @return the rendered consolidated invoice entity
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/{invoiceId}/render", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderConsolidatedInvoice(@PathVariable String invoiceId,
			@RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = "text/html") String accept,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		MediaType outputType = outputType(accept);
		return resourceResponse(biz.renderConsolidatedInvoice(userId, invoiceId, outputType, locale),
				outputType);
	}

	/**
	 * Render a consolidated invoice as PDF.
	 *
	 * @param invoiceId
	 *        the ID of the consolidated invoice to render
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param locale
	 *        the request locale
	 * @return the rendered consolidated invoice entity
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/{invoiceId}/render/pdf", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderPdfConsolidatedInvoice(@PathVariable String invoiceId,
			@RequestParam(value = "userId", required = false) Long userId, Locale locale) {
		return renderConsolidatedInvoice(invoiceId, "application/pdf", userId, locale);
	}

	/**
	 * Preview the current billing cycle's consolidated invoice, for the invoice
	 * consolidation group of a primary account.
	 *
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the preview consolidated invoice, or {@code null} data if the
	 *         user is not the primary account of a valid group
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/preview", method = RequestMethod.GET)
	public Result<Invoice> previewConsolidatedInvoice(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		Invoice result = biz.getPreviewConsolidatedInvoice(userId,
				new BasicInvoiceGenerationOptions(month(month), useCredit), locale);
		return success(result != null ? localized(result, locale) : null);
	}

	/**
	 * Get the status of the current billing cycle's preview consolidated
	 * invoice, for the invoice consolidation group of a primary account,
	 * without waiting for it to be generated.
	 *
	 * <p>
	 * The preview of the next account of the group is started if needed.
	 * Request again until the state is {@code Done} or {@code Failed}.
	 * </p>
	 *
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the preview status, with each account of the group and the
	 *         consolidated invoice when done, or {@code null} data if the user
	 *         is not the primary account of a valid group
	 * @since 2.4
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/preview/status", method = RequestMethod.GET)
	public Result<ConsolidatedInvoicePreviewStatus> previewConsolidatedInvoiceStatus(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		ConsolidatedInvoicePreviewStatus result = biz.getPreviewConsolidatedInvoiceStatus(userId,
				new BasicInvoiceGenerationOptions(month(month), useCredit), locale);
		final Invoice invoice = (result != null ? result.invoice() : null);
		return success(
				result != null && invoice != null
						? new ConsolidatedInvoicePreviewStatus(result.state(), result.accounts(),
								localized(invoice, locale))
						: result);
	}

	/**
	 * Render a preview of the current billing cycle's consolidated invoice.
	 *
	 * @param accept
	 *        an optional output type, defaults to {@literal text/html}
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the rendered consolidated invoice entity
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/preview/render", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderPreviewConsolidatedInvoice(
			@RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = "text/html") String accept,
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		BillingBiz biz = billingBiz();
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentActorUserId();
		}
		MediaType outputType = outputType(accept);
		return resourceResponse(
				biz.previewConsolidatedInvoice(userId,
						new BasicInvoiceGenerationOptions(month(month), useCredit), outputType, locale),
				outputType);
	}

	/**
	 * Render a preview of the current billing cycle's consolidated invoice as a
	 * PDF.
	 *
	 * @param userId
	 *        the optional user ID of the primary account; if not provided the
	 *        current actor's ID is used
	 * @param month
	 *        the optional month, in {@literal YYYY-MM} format; if not provided
	 *        the current month will be used
	 * @param useCredit
	 *        {@literal true} to allow using credit
	 * @param locale
	 *        the request locale
	 * @return the rendered consolidated invoice entity
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/consolidated-invoices/preview/render/pdf", method = RequestMethod.GET)
	public ResponseEntity<Resource> renderPreviewConsolidatedInvoicePdf(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "useCredit", required = false) boolean useCredit, Locale locale) {
		return renderPreviewConsolidatedInvoice("application/pdf", userId, month, useCredit, locale);
	}

}
