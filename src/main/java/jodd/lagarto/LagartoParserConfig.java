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

package jodd.lagarto;

/**
 * Configuration for {@link jodd.lagarto.LagartoParser}.
 */
public class LagartoParserConfig {

	protected boolean parseXmlTags = false;
	protected boolean enableConditionalComments = true; // todo make false
	protected boolean caseSensitive = false;
	protected boolean calculatePosition = false;
	protected boolean enableRawTextModes = true;
	protected int textBufferSize = 1024;

	/**
	 * @see #setEnableConditionalComments(boolean)
	 */
	public boolean isEnableConditionalComments() {
		return enableConditionalComments;
	}

	/**
	 * Enables detection of IE conditional comments. If not enabled,
	 * downlevel-hidden cond. comments will be treated as regular comment,
	 * while revealed cond. comments will be treated as an error.
	 */
	public LagartoParserConfig setEnableConditionalComments(final boolean enableConditionalComments) {
		this.enableConditionalComments = enableConditionalComments;
		return this;
	}

	/**
	 * Returns {@code true} if parsing of XML tags is enabled.
	 */
	public boolean isParseXmlTags() {
		return parseXmlTags;
	}

	/**
	 * Enables parsing of XML tags.
	 */
	public LagartoParserConfig setParseXmlTags(final boolean parseXmlTags) {
		this.parseXmlTags = parseXmlTags;
		return this;
	}

	/**
	 * Returns {@code true} if case-sensitive flag is enabled.
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Sets the case-sensitive flag for various matching.
	 */
	public LagartoParserConfig setCaseSensitive(final boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		return this;
	}

	/**
	 * @see #setCalculatePosition(boolean)
	 */
	public boolean isCalculatePosition() {
		return calculatePosition;
	}

	/**
	 * Resolves current position on parsing errors
	 * and for DOM elements. Note: this makes processing SLOW!
	 * JFlex may be used to track current line and row, but that brings
	 * overhead, and can't be easily disabled. By enabling this property,
	 * position will be calculated manually only on errors.
	 */
	public LagartoParserConfig setCalculatePosition(final boolean calculatePosition) {
		this.calculatePosition = calculatePosition;
		return this;
	}

	/**
	 * @see #setEnableRawTextModes(boolean)
	 */
	public boolean isEnableRawTextModes() {
		return enableRawTextModes;
	}

	/**
	 * Enables RAW (CDATA) and RCDATA text mode while parsing.
	 */
	public LagartoParserConfig setEnableRawTextModes(final boolean enableRawTextModes) {
		this.enableRawTextModes = enableRawTextModes;
		return this;
	}

	/**
	 * @see #setTextBufferSize(int)
	 */
	public int getTextBufferSize() {
		return textBufferSize;
	}

	/**
	 * Specifies initial text buffer size, used when emitting strings.
	 */
	public void setTextBufferSize(final int textBufferSize) {
		this.textBufferSize = textBufferSize;
	}
}
