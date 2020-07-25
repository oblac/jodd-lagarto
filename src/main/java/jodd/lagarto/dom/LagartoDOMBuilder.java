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

import jodd.lagarto.LagartoParser;
import jodd.lagarto.LagartoParserConfig;

import java.util.function.Consumer;

/**
 * Lagarto DOM builder creates DOM tree from HTML, XHTML or XML content.
 */
public class LagartoDOMBuilder implements DOMBuilder {

	public LagartoDOMBuilder() {
		this(new LagartoDomBuilderConfig());
	}

	public LagartoDOMBuilder(final LagartoDomBuilderConfig config) {
		this.config = config;
		enableHtmlMode();
	}

	// ---------------------------------------------------------------- config

	protected final LagartoDomBuilderConfig config;

	public LagartoDomBuilderConfig getConfig() {
		return config;
	}

	public LagartoParserConfig getParserConfig() {
		return config.parserConfig;
	}

	public LagartoDOMBuilder configure(final Consumer<LagartoDomBuilderConfig> configConsumer) {
		configConsumer.accept(this.config);
		return this;
	}

	// ---------------------------------------------------------------- quick settings

	/**
	 * Enables debug mode. Performances are lost.
	 */
	public LagartoDOMBuilder enableDebug() {
		config.collectErrors = true;
		config.parserConfig.setCalculatePosition(true);
		return this;
	}

	/**
	 * Disables debug mode.
	 */
	public LagartoDOMBuilder disableDebug() {
		config.collectErrors = false;
		config.parserConfig.setCalculatePosition(false);
		return this;
	}

	/**
	 * Enables {@link #enableHtmlMode() html mode} with additional
	 * and somewhat experimental rules.
	 */
	public LagartoDOMBuilder enableHtmlPlusMode() {
		enableHtmlMode();
		config.useFosterRules = true;
		config.unclosedTagAsOrphanCheck = true;
		return this;
	}


	/**
	 * Enables HTML5 parsing mode.
	 */
	public LagartoDOMBuilder enableHtmlMode() {
		config.ignoreWhitespacesBetweenTags = false;               // collect all whitespaces
		config.parserConfig.setCaseSensitive(false);               // HTML is case insensitive
		config.parserConfig.setEnableRawTextModes(true);           // script and style tags are parsed as CDATA
		config.enabledVoidTags = true;                             // list of void tags
		config.selfCloseVoidTags = false;                          // don't self close void tags
		config.impliedEndTags = true;                              // some tags end is implied
		config.parserConfig.setEnableConditionalComments(false);   // don't enable IE conditional comments
		config.parserConfig.setParseXmlTags(false);                // enable XML mode in parsing
		return this;
	}

	/**
	 * Enables XHTML mode.
	 */
	public LagartoDOMBuilder enableXhtmlMode() {
		config.ignoreWhitespacesBetweenTags = false;               // collect all whitespaces
		config.parserConfig.setCaseSensitive(true);                // XHTML is case sensitive
		config.parserConfig.setEnableRawTextModes(false);          // all tags are parsed in the same way
		config.enabledVoidTags = true;                             // list of void tags
		config.selfCloseVoidTags = true;                           // self close void tags
		config.impliedEndTags = false;                             // no implied tag ends
		config.parserConfig.setEnableConditionalComments(false);   // don't enable IE conditional comments
		config.parserConfig.setParseXmlTags(false);                // enable XML mode in parsing
		return this;
	}

	/**
	 * Enables XML parsing mode.
	 */
	public LagartoDOMBuilder enableXmlMode() {
		config.ignoreWhitespacesBetweenTags = true;                // ignore whitespaces that are non content
		config.parserConfig.setCaseSensitive(true);                // XML is case sensitive
		config.parserConfig.setEnableRawTextModes(false);          // all tags are parsed in the same way
		config.enabledVoidTags = false;                            // there are no void tags
		config.selfCloseVoidTags = false;                          // don't self close empty tags (can be changed!)
		config.impliedEndTags = false;                             // no implied tag ends
		config.parserConfig.setEnableConditionalComments(false);   // disable IE conditional comments
		config.parserConfig.setParseXmlTags(true);                 // enable XML mode in parsing
		return this;
	}

	// ---------------------------------------------------------------- parse

	/**
	 * Creates DOM tree from provided content.
	 */
	@Override
	public Document parse(final char[] content) {
		final LagartoParser lagartoParser = new LagartoParser(config.parserConfig, content);
		return parseWithLagarto(lagartoParser);
	}

	/**
	 * Creates DOM tree from the provided content.
	 */
	@Override
	public Document parse(final String content) {
		final LagartoParser lagartoParser = new LagartoParser(config.parserConfig, content);
		return parseWithLagarto(lagartoParser);
	}

	/**
	 * Parses the content using provided lagarto parser.
	 */
	protected Document parseWithLagarto(final LagartoParser lagartoParser) {
		final LagartoDOMBuilderTagVisitor domBuilderTagVisitor =
				new LagartoDOMBuilderTagVisitor(this);

		lagartoParser.parse(domBuilderTagVisitor);

		return domBuilderTagVisitor.getDocument();
	}

}
