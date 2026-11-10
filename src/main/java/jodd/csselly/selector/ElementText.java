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
 * ({@code Element#text()} and {@code Element#ownText()}): whitespace is normalized, except in elements that preserve it,
 * such as {@code pre}, and a space separates block elements.
 * <p>
 * Ported from <a href="https://jsoup.org">jsoup</a>, Copyright (c) 2009-2025 Jonathan Hedley, MIT License.
 */
final class ElementText {

	private ElementText() {
	}

	// jsoup's org.jsoup.parser.TagSet
	private static final Set<String> PRESERVE_WHITESPACE_TAGS = set("pre", "listing", "plaintext", "title", "textarea", "script");
	private static final Set<String> DATA_TAGS = set("iframe", "noembed", "noframes", "script", "style", "xmp");
	private static final Set<String> BLOCK_TAGS = set(
		"html", "head", "body", "frameset", "script", "noscript", "style", "meta", "link", "title", "frame",
		"noframes", "section", "nav", "aside", "hgroup", "header", "footer", "p", "h1", "h2", "h3", "h4", "h5",
		"h6", "dialog", "search",
		"ul", "ol", "pre", "div", "blockquote", "hr", "address", "figure", "figcaption", "form", "fieldset",
		"dl", "dt", "dd", "li", "table", "caption", "thead", "tfoot", "tbody", "colgroup", "col", "tr", "th",
		"td", "details", "menu", "plaintext", "template", "article", "main",
		"center",
		"dir", "applet", "marquee", "listing",
		// MathML and SVG
		"math", "svg", "femerge", "femergenode");
	// text() readability hint for controls, widgets, and embedded objects
	private static final Set<String> TEXT_BOUNDARY_TAGS = set(
		"button", "input", "select", "textarea", "option", "output", "progress", "meter",
		"img", "picture", "audio", "video", "canvas", "object", "embed", "iframe");

	private static Set<String> set(final String... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	/**
	 * Returns the text of the element and of its descendants, whitespace normalized and trimmed.
	 */
	static String text(final Node element) {
		final StringBuilder accum = new StringBuilder();
		appendText(accum, element);
		return accum.toString().trim();
	}

	private static void appendText(final StringBuilder accum, final Node node) {
		if (isText(node)) {
			appendNormalisedText(accum, node);
			return;
		}
		if (node.getNodeType() != Node.NodeType.ELEMENT) {
			return;
		}
		// add a space before leading blocks and readable boundaries when text would otherwise run together
		if (accum.length() > 0 && needsLeadingTextSeparator(node) && !lastCharIsWhitespace(accum)) {
			accum.append(' ');
		}
		final int childCount = node.getChildNodesCount();
		for (int i = 0; i < childCount; i++) {
			appendText(accum, node.getChild(i));
		}
		// make sure there is a space between block or readable-boundary elements and immediately following texts or inline elements
		final Node next = node.getNextSibling();
		if (needsTrailingTextSeparator(node)
			&& next != null && (isText(next) || next.getNodeType() == Node.NodeType.ELEMENT && !isBlock(next))
			&& !lastCharIsWhitespace(accum)) {
			accum.append(' ');
		}
	}

	private static boolean needsLeadingTextSeparator(final Node element) {
		return isBlock(element)
			|| isElement(element, "br")
			|| isTextBoundary(element) && element.getChildNodesCount() > 0 && hasText(element);
	}

	private static boolean needsTrailingTextSeparator(final Node element) {
		return isTextBoundary(element) || isBlock(element) || hasBlockChild(element);
	}

	private static boolean hasBlockChild(final Node element) {
		final int childCount = element.getChildNodesCount();
		for (int i = 0; i < childCount; i++) {
			final Node child = element.getChild(i);
			if (child.getNodeType() == Node.NodeType.ELEMENT && isBlock(child)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns {@code true} if the element contains a non-blank text.
	 */
	private static boolean hasText(final Node node) {
		final int childCount = node.getChildNodesCount();
		for (int i = 0; i < childCount; i++) {
			final Node child = node.getChild(i);
			if (isText(child) ? !isBlank(child.getNodeValue()) : hasText(child)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBlank(final String value) {
		if (value == null) {
			return true;
		}
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c != ' ' && c != '\t' && c != '\n' && c != '\f' && c != '\r') {
				return false;
			}
		}
		return true;
	}

	private static boolean isBlock(final Node element) {
		return BLOCK_TAGS.contains(lowerCase(element.getNodeName()));
	}

	private static boolean isTextBoundary(final Node element) {
		return TEXT_BOUNDARY_TAGS.contains(lowerCase(element.getNodeName()));
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
