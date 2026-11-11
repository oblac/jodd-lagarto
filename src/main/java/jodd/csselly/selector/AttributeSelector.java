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

package jodd.csselly.selector;

import jodd.csselly.Selector;
import jodd.csselly.CSSellyException;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Attribute selector.
 */
public class AttributeSelector extends Selector implements NodeFilter {

	// ---------------------------------------------------------------- ctor

	protected final String name;
	protected final String value;
	protected final Match match;
	protected char quoteChar;
	protected boolean caseInsensitive;
	protected boolean caseSensitive;
	// precomputed for case-insensitive matching
	private boolean htmlCaseInsensitiveAttribute;
	private String lowerCaseValue;

	/**
	 * Attributes whose values are matched ASCII case-insensitively on HTML elements in HTML documents, unless the
	 * {@code s} flag is used.
	 * See <a href="https://html.spec.whatwg.org/multipage/semantics-other.html#case-sensitivity-of-selectors">HTML</a>.
	 */
	private static final Set<String> HTML_CASE_INSENSITIVE_ATTRIBUTES = new HashSet<>(Arrays.asList(
		"accept", "accept-charset", "align", "alink", "axis", "bgcolor", "charset", "checked", "clear", "codetype",
		"color", "compact", "declare", "defer", "dir", "direction", "disabled", "enctype", "face", "frame",
		"hreflang", "http-equiv", "lang", "language", "link", "media", "method", "multiple", "nohref", "noresize",
		"noshade", "nowrap", "readonly", "rel", "rev", "rules", "scope", "scrolling", "selected", "shape",
		"target", "text", "type", "valign", "valuetype", "vlink"));

	public AttributeSelector(final String name, final String sign, final String value) {
		super(Type.ATTRIBUTE);
		this.name = name.trim();
		this.value = extractValue(value);
		this.match = Match.valueOf(sign);
		initCaseFolding();
	}

	public AttributeSelector(final String name, final Match match, final String value) {
		super(Type.ATTRIBUTE);
		this.name = name.trim();
		this.match = match;
		this.value = extractValue(value);
		initCaseFolding();
	}

	public AttributeSelector(final String attr) {
		super(Type.ATTRIBUTE);
		int index = attr.indexOf('=');
		if (index == -1) {
			this.name = attr.trim();
			this.match = null;
			this.value = null;
			return;
		}
		final char first = attr.charAt(index - 1);
		this.match = Match.valueOfFirstChar(first);

		final int signLen = this.match.getSign().length();
		index -= (signLen - 1);
		this.name = attr.substring(0, index).trim();
		this.value = extractValueAndFlag(attr.substring(index + signLen));
		if (match == Match.NOT_EQUALS && (caseInsensitive || caseSensitive)) {
			// like jQuery
			throw new CSSellyException("Attribute selector flags are not supported with " + match.getSign());
		}
		initCaseFolding();
	}

	private void initCaseFolding() {
		htmlCaseInsensitiveAttribute = HTML_CASE_INSENSITIVE_ATTRIBUTES.contains(asciiLowerCase(name));
		lowerCaseValue = value == null ? null : asciiLowerCase(value);
	}

	/**
	 * Extracts the value of an attribute selector, with an optional
	 * case-sensitivity flag ({@code i} or {@code s}), eg: {@code 'value' i}.
	 */
	protected String extractValueAndFlag(String value) {
		value = value.trim();
		if (value.isEmpty()) {
			throw new CSSellyException("Missing attribute value");
		}
		final char first = value.charAt(0);
		final String flag;
		if (first == '"' || first == '\'') {
			quoteChar = first;
			final int end = value.lastIndexOf(first);
			if (end == 0) {
				throw new CSSellyException("Unclosed attribute value: " + value);
			}
			flag = value.substring(end + 1).trim();
			value = value.substring(1, end);
		} else {
			quoteChar = 0;
			int ws = value.length() - 1;
			while (ws >= 0 && !Character.isWhitespace(value.charAt(ws))) {
				ws--;
			}
			if (ws == -1) {
				flag = "";
			} else {
				flag = value.substring(ws + 1);
				value = value.substring(0, ws).trim();
			}
		}
		if (flag.equalsIgnoreCase("i")) {
			caseInsensitive = true;
		} else if (flag.equalsIgnoreCase("s")) {
			caseSensitive = true;
		} else if (!flag.isEmpty()) {
			throw new CSSellyException("Invalid attribute selector flag: " + flag);
		}
		return value;
	}

	protected String extractValue(String value) {
		// whitespace is allowed around the value, but not trimmed inside quotes
		value = value.trim();
		quoteChar = value.charAt(0);
		if (quoteChar != '"' && quoteChar != '\'') {
			quoteChar = 0;
		}

		if (quoteChar != 0) {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}

	// ---------------------------------------------------------------- getters

	/**
	 * Returns attribute name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns attribute value or {@code null} if doesn't exist.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns matching type.
	 */
	public Match getMatch() {
		return match;
	}

	/**
	 * Returns the quote char or {@code 0} if quote is not used.
	 */
	public char getQuoteChar() {
		return quoteChar;
	}

	/**
	 * Returns {@code true} if the value is compared case-insensitively ({@code i} flag).
	 */
	public boolean isCaseInsensitive() {
		return caseInsensitive;
	}

	/**
	 * Returns {@code true} if the value is compared case-sensitively ({@code s} flag),
	 * even for the attributes that HTML matches case-insensitively.
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	// ---------------------------------------------------------------- match

	@Override
	public boolean accept(final Node node) {
		if (match == Match.NOT_EQUALS) {
			// like jQuery: also matches elements without the attribute, always case-sensitive
			if (!node.hasAttribute(name)) {
				return true;
			}
			final String nodeValue = node.getAttribute(name);
			return match.compare(nodeValue == null ? "" : nodeValue, value);
		}

		if (!node.hasAttribute(name)) {
			return false;
		}

		if (value == null) {        // just detect if attribute exist
			return true;
		}

		final String nodeValue = node.getAttribute(name);

		if (nodeValue == null) {
			return false;
		}

		if (caseInsensitive || (!caseSensitive && isHtmlCaseInsensitive(node))) {
			return match.compare(asciiLowerCase(nodeValue), lowerCaseValue);
		}
		return match.compare(nodeValue, value);
	}

	private boolean isHtmlCaseInsensitive(final Node node) {
		if (!htmlCaseInsensitiveAttribute) {
			return false;
		}
		final Document document = node.getOwnerDocument();
		return document != null && !document.getConfig().getParserConfig().isCaseSensitive();
	}

	private static String asciiLowerCase(final String value) {
		char[] chars = null;
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c >= 'A' && c <= 'Z') {
				if (chars == null) {
					chars = value.toCharArray();
				}
				chars[i] = (char) (c + ('a' - 'A'));
			}
		}
		return chars == null ? value : new String(chars);
	}
}
