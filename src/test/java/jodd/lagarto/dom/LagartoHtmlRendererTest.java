// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package jodd.lagarto.dom;

import jodd.io.FileUtil;
import jodd.lagarto.dom.render.LagartoHtmlRenderer;
import jodd.lagarto.dom.render.LagartoHtmlRendererNodeVisitor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static jodd.lagarto.dom.render.LagartoHtmlRendererNodeVisitor.Case.DEFAULT;
import static jodd.lagarto.dom.render.LagartoHtmlRendererNodeVisitor.Case.LOWERCASE;
import static jodd.lagarto.dom.render.LagartoHtmlRendererNodeVisitor.Case.RAW;
import static jodd.lagarto.dom.render.LagartoHtmlRendererNodeVisitor.Case.UPPERCASE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LagartoHtmlRendererTest {

	protected final String testDataRoot = this.getClass().getResource("data").getFile();
	
	@Test
	void testSimple() {
		final String html = "<html><boDY><div id=\"z\" fooBar=\"aAa\">some Text</div></boDY></html>";
		final LagartoDOMBuilder domBuilder = new LagartoDOMBuilder();

		// case insensitive -> lowercase
		Document document = domBuilder.parse(html);
		String htmlOut = document.getHtml();
		assertEquals("<html><body><div id=\"z\" foobar=\"aAa\">some Text</div></body></html>", htmlOut);

		// case sensitive -> raw
		domBuilder.getParserConfig().setCaseSensitive(true);
		document = domBuilder.parse(html);
		htmlOut = document.getHtml();
		assertEquals(html, htmlOut);
	}

	@Test
	void testCases() {
		final String html = "<html><boDY><div id=\"z\" fooBar=\"aAa\">some Text</div></boDY></html>";
		final LagartoDOMBuilder domBuilder = new LagartoDOMBuilder();

		// case insensitive -> lowercase
		final Document document = domBuilder.parse(html);

		// raw, default

		document.getConfig().setHtmlRenderer(
				new LagartoHtmlRenderer() {
					@Override
					protected NodeVisitor createRenderer(final Appendable appendable) {
						final LagartoHtmlRendererNodeVisitor renderer =
								(LagartoHtmlRendererNodeVisitor) super.createRenderer(appendable);

						renderer.setTagCase(RAW);
						renderer.setAttributeCase(DEFAULT);

						return renderer;
					}
				}
		);

		assertEquals("<html><boDY><div id=\"z\" foobar=\"aAa\">some Text</div></boDY></html>", document.getHtml());

		// raw, raw
		document.getConfig().setHtmlRenderer(
				new LagartoHtmlRenderer() {
					@Override
					protected NodeVisitor createRenderer(final Appendable appendable) {
						final LagartoHtmlRendererNodeVisitor renderer =
								(LagartoHtmlRendererNodeVisitor) super.createRenderer(appendable);

						renderer.setTagCase(RAW);
						renderer.setAttributeCase(RAW);

						return renderer;
					}
				}
		);

		assertEquals(html, document.getHtml());

		// default, raw

		document.getConfig().setHtmlRenderer(
				new LagartoHtmlRenderer() {
					@Override
					protected NodeVisitor createRenderer(final Appendable appendable) {
						final LagartoHtmlRendererNodeVisitor renderer =
								(LagartoHtmlRendererNodeVisitor) super.createRenderer(appendable);

						renderer.setTagCase(DEFAULT);
						renderer.setAttributeCase(RAW);

						return renderer;
					}
				}
		);

		assertEquals("<html><body><div id=\"z\" fooBar=\"aAa\">some Text</div></body></html>", document.getHtml());

		// default, default
		document.getConfig().setHtmlRenderer(
				new LagartoHtmlRenderer() {
					@Override
					protected NodeVisitor createRenderer(final Appendable appendable) {
						final LagartoHtmlRendererNodeVisitor renderer =
								(LagartoHtmlRendererNodeVisitor) super.createRenderer(appendable);

						renderer.setTagCase(DEFAULT);
						renderer.setAttributeCase(DEFAULT);

						return renderer;
					}
				}
		);

		assertEquals("<html><body><div id=\"z\" foobar=\"aAa\">some Text</div></body></html>", document.getHtml());

		// lowercase, uppercase
		document.getConfig().setHtmlRenderer(
				new LagartoHtmlRenderer() {
					@Override
					protected NodeVisitor createRenderer(final Appendable appendable) {
						final LagartoHtmlRendererNodeVisitor renderer =
								(LagartoHtmlRendererNodeVisitor) super.createRenderer(appendable);

						renderer.setTagCase(LOWERCASE);
						renderer.setAttributeCase(UPPERCASE);

						return renderer;
					}
				}
		);

		assertEquals("<html><body><div ID=\"z\" FOOBAR=\"aAa\">some Text</div></body></html>", document.getHtml());

		// uppercase, lowercase
		// lowercase, uppercase
		document.getConfig().setHtmlRenderer(
				new LagartoHtmlRenderer() {
					@Override
					protected NodeVisitor createRenderer(final Appendable appendable) {
						final LagartoHtmlRendererNodeVisitor renderer =
								(LagartoHtmlRendererNodeVisitor) super.createRenderer(appendable);

						renderer.setTagCase(UPPERCASE);
						renderer.setAttributeCase(LOWERCASE);

						return renderer;
					}
				}
		);

		assertEquals("<HTML><BODY><DIV id=\"z\" foobar=\"aAa\">some Text</DIV></BODY></HTML>", document.getHtml());
	}

	// ---------------------------------------------------------------- vsethi test

	/**
	 * Custom renderer, example of dynamic rules:
	 *
	 * + HTML tags are lowercase
	 * + NON-HTML tags remains raw
	 * + HTML attr names are lowercase
	 * + NON-HTML attr names are raw
	 * + XML block is detected by xml-attrib attribute
	 * + XML block is all RAW
	 */
	public static class CustomRenderer extends LagartoHtmlRenderer {
		@Override
		protected NodeVisitor createRenderer(final Appendable appendable) {

			return new LagartoHtmlRendererNodeVisitor(appendable) {

				@Override
				public void document(final Document document) {
					configHtml();
					super.document(document);
				}

				protected void configHtml() {
					setTagCase(LOWERCASE);
					setAttributeCase(LOWERCASE);
				}
				protected void configXML() {
					setTagCase(RAW);
					setAttributeCase(RAW);
				}

				@Override
				protected String resolveAttributeName(final Node node, final Attribute attribute) {
					final String attributeName = attribute.getRawName();
					if (attributeName.contains("_") || attributeName.contains("-")) {
						return attributeName;
					}
					return super.resolveAttributeName(node, attribute);
				}

				@Override
				protected void elementBody(final Element element) throws IOException {
					final boolean hasXML = element.hasAttribute("xml-attrib");

					// detects XML content
					if (hasXML) {
						configXML();
					}

					super.elementBody(element);

					if (hasXML) {
						configHtml();
					}

				}

			};
		}
	}

	@Test
	void testVKSethi() throws IOException {
		final String html = FileUtil.readString(new File(testDataRoot, "vksethi.html"));
		final String htmlExpected = FileUtil.readString(new File(testDataRoot, "vksethi-out.html"));

		final LagartoDOMBuilder domBuilder = new LagartoDOMBuilder();
		for (int i = 0; i < 2; i++) {
			// this does not change anything with html output
			domBuilder.getParserConfig().setCaseSensitive(i == 1);
			domBuilder.getConfig().setHtmlRenderer(new CustomRenderer());

			final Document document = domBuilder.parse(html);

			final String htmlOut = document.getHtml();
			assertEquals(htmlExpected, htmlOut);
		}
	}

}
