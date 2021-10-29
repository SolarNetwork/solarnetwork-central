/* ==================================================================
 * HtmlToPdfTemplateRenderer.java - 8/08/2020 6:36:16 AM
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

package net.solarnetwork.central.user.billing.snf.pdf;

import static java.util.Collections.singletonList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.w3c.dom.Document;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import net.solarnetwork.domain.BasicIdentity;
import net.solarnetwork.service.TemplateRenderer;

/**
 * {@link TemplateRenderer} that takes the UTF-8 encoded HTML output of another
 * renderer and transforms that to PDF.
 * 
 * @author matt
 * @version 2.0
 */
public class HtmlToPdfTemplateRenderer extends BasicIdentity<String> implements TemplateRenderer {

	/** The UTF-8 character set. */
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/** The PDF MIME type. */
	public static final MimeType PDF_MIME_TYPE = MimeType.valueOf("application/pdf");

	/** List of just the PDF MIME type. */
	public static final List<MimeType> PDF = singletonList(PDF_MIME_TYPE);

	/**
	 * Regular expression for finding HTML
	 * <code>&lt;img src="data:image/svg+xml..."&gt;</code> style elements so
	 * they can be replaced by their SVG directly and rendered into the PDF
	 * output.
	 * 
	 * <p>
	 * Has two matching groups: first one matches optional attributes before the
	 * <code>src</code> attribute. The second matches the SVG content.
	 * </p>
	 */
	public static final Pattern SVG_DATA_IMG_PAT = Pattern
			.compile("<img([^>]*) src=\"data:image/svg\\+xml,%3Csvg(.*)svg%3E\">(?:</img>)?");

	static {
		XRLog.setLoggerImpl(new Slf4jLogger());
	}

	private final TemplateRenderer htmlRenderer;
	private final String baseUri;
	private final W3CDom w3cDom;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code id} value will be set to the class name. The {@code baseUri}
	 * value will be set to the system temp directory.
	 * </p>
	 * 
	 * @param htmlRenderer
	 *        the HTML renderer
	 */
	public HtmlToPdfTemplateRenderer(TemplateRenderer htmlRenderer) {
		this("net.solarnetwork.central.user.billing.snf.pdf.HtmlToPdfTemplateRenderer",
				"file://" + System.getProperty("java.io.tmpdir"), htmlRenderer);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code baseUri} value will be set to the system temp directory.
	 * </p>
	 * 
	 * @param id
	 *        the identifier
	 * @param htmlRenderer
	 *        the HTML renderer
	 */
	public HtmlToPdfTemplateRenderer(String id, TemplateRenderer htmlRenderer) {
		this(id, "file://" + System.getProperty("java.io.tmpdir"), htmlRenderer);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the identifier
	 * @param baseUri
	 *        the base URI to use when parsing HTML
	 * @param htmlRenderer
	 *        the HTML renderer
	 */
	public HtmlToPdfTemplateRenderer(String id, String baseUri, TemplateRenderer htmlRenderer) {
		super(id);
		if ( baseUri == null ) {
			throw new IllegalArgumentException("The baseUri argument must be provided.");
		}
		this.baseUri = baseUri;
		if ( htmlRenderer == null ) {
			throw new IllegalArgumentException("The htmlRenderer argument must be provided.");
		}
		this.htmlRenderer = htmlRenderer;
		this.w3cDom = new W3CDom();
	}

	@Override
	public boolean supportsMimeType(MimeType mimeType) {
		return PDF_MIME_TYPE.isCompatibleWith(mimeType);
	}

	@Override
	public List<MimeType> supportedMimeTypes() {
		return PDF;
	}

	@Override
	public void render(Locale locale, MimeType mimeType, Map<String, ?> parameters, OutputStream out)
			throws IOException {
		// render HTML to memory
		try (ByteArrayOutputStream byos = new ByteArrayOutputStream(8192)) {
			htmlRenderer.render(locale, MimeTypeUtils.TEXT_HTML, parameters, byos);
			String html = new String(byos.toByteArray(), UTF8);
			html = replaceImgDataSvg(html);
			renderPdf(html, out);
			out.flush();
			out.close();
		}
	}

	private void renderPdf(String html, OutputStream out) throws IOException {
		org.jsoup.nodes.Document html5Doc = Jsoup.parse(html, baseUri);
		Document doc = w3cDom.fromJsoup(html5Doc);
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useFastMode();
		builder.withW3cDocument(doc, baseUri);
		builder.toStream(out);
		builder.useSVGDrawer(new BatikSVGDrawer());
		builder.run();
	}

	private String replaceImgDataSvg(String html) throws IOException {
		if ( html == null ) {
			return null;
		}
		StringBuilder buf = new StringBuilder(html.length());
		Matcher m = SVG_DATA_IMG_PAT.matcher(html);
		int pos = 0;
		while ( m.find(pos) ) {
			buf.append(html.substring(pos, m.start()));
			buf.append("<svg");
			buf.append(m.group(1));
			buf.append(URLDecoder.decode(m.group(2), UTF8.name()));
			buf.append("svg>");
			pos = m.end();
		}
		if ( pos < html.length() ) {
			buf.append(html.subSequence(pos, html.length()));
		}
		return buf.toString();
	}

}
