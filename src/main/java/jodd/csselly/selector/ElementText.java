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

import jodd.lagarto.dom.Node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Element texts for the pseudo functions born in jsoup ({@code :containsOwn()}...), computed like jsoup 1.24
 * ({@code Element#ownText()}): whitespace is normalized, except in elements that preserve it, such as {@code pre}.
 * <p>
 * Ported from <a href="https://jsoup.org">jsoup</a>, Copyright (c) 2009-2025 Jonathan Hedley, MIT License.
 */
final class ElementText {

	private ElementText() {
	}

	// jsoup's org.jsoup.parser.TagSet
	private static final Set<String> PRESERVE_WHITESPACE_TAGS = set("pre", "listing", "plaintext", "title", "textarea", "script");
	private static final Set<String> DATA_TAGS = set("iframe", "noembed", "noframes", "script", "style", "xmp");

	private static Set<String> set(final String... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	/**
	 * Returns the text of the element itself, without the text of its children elements, whitespace normalized and trimmed.
	 */
	static String ownText(final Node element) {
		final StringBuilder accum = new StringBuilder();
		final int childCount = element.getChildNodesCount();
		for (int i = 0; i < childCount; i++) {
			final Node child = element.getChild(i);
			if (isText(child)) {
				appendNormalisedText(accum, child);
			} else if (isElement(child, "br") && !lastCharIsWhitespace(accum)) {
				accum.append(' ');
			}
		}
		return accum.toString().trim();
	}

	/**
	 * Lower-cases like jsoup.
	 */
	static String lowerCase(final String value) {
		return value.toLowerCase(Locale.ROOT);
	}

	/**
	 * Normalizes whitespace, like jsoup: whitespace runs are replaced with a single space.
	 */
	static String normaliseWhitespace(final String value) {
		final StringBuilder accum = new StringBuilder(value.length());
		appendNormalisedWhitespace(accum, value, false);
		return accum.toString();
	}

	/**
	 * Removes the backslashes of escaped characters, like jsoup's {@code TokenQueue#unescape}.
	 */
	static String unescape(final String value) {
		if (value.indexOf('\\') == -1) {
			return value;
		}
		final StringBuilder out = new StringBuilder(value.length());
		char last = 0;
		for (char c : value.toCharArray()) {
			if (c == '\\') {
				if (last == '\\') {
					out.append(c);
					c = 0;
				}
			} else {
				out.append(c);
			}
			last = c;
		}
		return out.toString();
	}

	// ---------------------------------------------------------------- internal

	/**
	 * Text and CDATA nodes, but not the content of data elements such as {@code script} or {@code style}.
	 */
	static boolean isText(final Node node) {
		final Node.NodeType type = node.getNodeType();
		if (type != Node.NodeType.TEXT && type != Node.NodeType.CDATA) {
			return false;
		}
		final Node parent = node.getParentNode();
		return parent == null || parent.getNodeType() != Node.NodeType.ELEMENT || !DATA_TAGS.contains(lowerCase(parent.getNodeName()));
	}

	static boolean isElement(final Node node, final String name) {
		return node.getNodeType() == Node.NodeType.ELEMENT && name.equalsIgnoreCase(node.getNodeName());
	}

	static void appendNormalisedText(final StringBuilder accum, final Node textNode) {
		final String text = textNode.getNodeValue();
		if (text == null) {
			return;
		}
		if (textNode.getNodeType() == Node.NodeType.CDATA || preserveWhitespace(textNode.getParentNode())) {
			accum.append(text);
		} else {
			appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum));
		}
	}

	/**
	 * Looks at the element and five levels up, like jsoup.
	 */
	private static boolean preserveWhitespace(Node node) {
		int i = 0;
		while (i < 6 && node != null && node.getNodeType() == Node.NodeType.ELEMENT) {
			if (PRESERVE_WHITESPACE_TAGS.contains(lowerCase(node.getNodeName()))) {
				return true;
			}
			node = node.getParentNode();
			i++;
		}
		return false;
	}

	static boolean lastCharIsWhitespace(final StringBuilder accum) {
		return accum.length() != 0 && accum.charAt(accum.length() - 1) == ' ';
	}

	private static void appendNormalisedWhitespace(final StringBuilder accum, final String string, final boolean stripLeading) {
		boolean lastWasWhite = false;
		boolean reachedNonWhite = false;
		final int length = string.length();
		int c;
		for (int i = 0; i < length; i += Character.charCount(c)) {
			c = string.codePointAt(i);
			if (isActuallyWhitespace(c)) {
				if ((stripLeading && !reachedNonWhite) || lastWasWhite) {
					continue;
				}
				accum.append(' ');
				lastWasWhite = true;
			} else if (!isInvisibleChar(c)) {
				accum.appendCodePoint(c);
				lastWasWhite = false;
				reachedNonWhite = true;
			}
		}
	}

	private static boolean isActuallyWhitespace(final int c) {
		// 160 is &nbsp;
		return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == 160;
	}

	private static boolean isInvisibleChar(final int c) {
		// zero width space, soft hyphen
		return c == 8203 || c == 173;
	}
}
