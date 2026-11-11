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

package jodd.csselly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSS selector parser. Works with one query, i.e. does not support groups
 * of selectors (selectors separated by a comma). To parse selectors group,
 * manually split the group query into single queries and parse each.
 * See: http://www.w3.org/TR/css3-selectors/#w3cselgrammar
 */
public class CSSelly {

	protected final CSSellyLexer lexer;

	public CSSelly(final String input) {
		this.lexer = new CSSellyLexer(input.toCharArray());
	}

	public CSSelly(final char[] input) {
		this.lexer = new CSSellyLexer(input);
	}

	// ---------------------------------------------------------------- parse

	/**
	 * Parses selector string.
	 * Returns {@code null} if no selector can be parsed.
	 */
	public List<CssSelector> parse() {
		try {
			lexer.yylex();
			if (lexer.selectors.isEmpty()) {
				return null;
			}

			// fixes last combinator
			final CssSelector last = lexer.selectors.get(lexer.selectors.size() - 1);
			if (last.getCombinator() == Combinator.DESCENDANT) {
				last.setCombinator(null);
			}

			// set previous css selector
			CssSelector prevCssSelector = null;
			for (final CssSelector cssSelector : lexer.selectors) {
				if (prevCssSelector != null) {
					if (cssSelector.isScope()) {
						throw new CSSellyException(":scope is only supported at the beginning of a selector");
					}
					cssSelector.setPrevCssSelector(prevCssSelector);
				}
				prevCssSelector = cssSelector;
			}

			return lexer.selectors;
		} catch (final IOException ioex) {
			throw new CSSellyException(ioex);
		}
	}

	/**
	 * Parses string of selectors (separated with <b>,</b>). Returns
	 * list of {@link CssSelector} lists in the same order.
	 */
	public static List<List<CssSelector>> parse(final String query) {
		final List<String> singleQueries = splitSelectorsGroup(query);
		final List<List<CssSelector>> selectors = new ArrayList<>(singleQueries.size());

		for (final String singleQuery: singleQueries) {
			selectors.add(new CSSelly(singleQuery).parse());
		}

		return selectors;
	}

	/**
	 * Splits a group of selectors on the commas that are not inside
	 * strings, attribute selectors or pseudo function expressions.
	 */
	static List<String> splitSelectorsGroup(final String query) {
		final List<String> singleQueries = new ArrayList<>();
		int depth = 0;
		int start = 0;
		final int length = query.length();
		for (int i = 0; i < length; i++) {
			final char c = query.charAt(i);
			if (c == '\\') {
				i++;
			} else if (c == '"' || c == '\'') {
				// like the lexer, an unclosed quote is a plain character, eg: :contains(don't)
				final int end = stringEnd(query, i);
				if (end != -1) {
					i = end;
				}
			} else if (c == '(' || c == '[') {
				depth++;
			} else if ((c == ')' || c == ']') && depth > 0) {
				depth--;
			} else if (c == ',' && depth == 0) {
				singleQueries.add(query.substring(start, i));
				start = i + 1;
			}
		}
		singleQueries.add(query.substring(start));
		return singleQueries;
	}

	/**
	 * Returns the index of the quote that closes the string starting at given index,
	 * or {@code -1} if the string isn't closed before the end of the line.
	 */
	private static int stringEnd(final String query, final int startIndex) {
		final char quote = query.charAt(startIndex);
		final int length = query.length();
		for (int i = startIndex + 1; i < length; i++) {
			final char c = query.charAt(i);
			if (c == quote) {
				return i;
			}
			if (c == '\\') {
				i++;
			} else if (c == '\n' || c == '\r' || c == '\f') {
				return -1;
			}
		}
		return -1;
	}

	// ---------------------------------------------------------------- escapes

	/**
	 * Resolves the CSS escapes of an identifier or a string, eg: {@code \:}, {@code \31 } or {@code \0000e9}.
	 * See <a href="https://www.w3.org/TR/css-syntax-3/#consume-escaped-code-point">CSS Syntax</a>.
	 */
	public static String unescape(final String value) {
		if (value.indexOf('\\') == -1) {
			return value;
		}
		final StringBuilder out = new StringBuilder(value.length());
		final int length = value.length();
		int i = 0;
		while (i < length) {
			final char c = value.charAt(i++);
			if (c != '\\') {
				out.append(c);
			} else if (i < length) {
				int hexEnd = i;
				while (hexEnd < length && hexEnd - i < 6 && Character.digit(value.charAt(hexEnd), 16) != -1) {
					hexEnd++;
				}
				if (hexEnd > i) {
					final int codePoint = Integer.parseInt(value.substring(i, hexEnd), 16);
					final boolean valid = codePoint != 0 && codePoint <= Character.MAX_CODE_POINT
						&& (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE);
					out.appendCodePoint(valid ? codePoint : 0xFFFD);
					// a single whitespace terminates a hex escape
					i = skipNewline(value, hexEnd);
					if (i == hexEnd && i < length && isWhitespace(value.charAt(i))) {
						i++;
					}
				} else {
					final int next = skipNewline(value, i);
					if (next == i) {
						out.append(value.charAt(i++));
					} else {
						// an escaped newline continues a string on the next line
						i = next;
					}
				}
			}
		}
		return out.toString();
	}

	private static boolean isWhitespace(final char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
	}

	/**
	 * Returns the index after the newline at given index, or the index itself if there is no newline.
	 */
	private static int skipNewline(final String value, final int index) {
		if (index >= value.length()) {
			return index;
		}
		final char c = value.charAt(index);
		if (c == '\r' && index + 1 < value.length() && value.charAt(index + 1) == '\n') {
			return index + 2;
		}
		return c == '\n' || c == '\r' || c == '\f' ? index + 1 : index;
	}

	// ---------------------------------------------------------------- toString

	/**
	 * Returns string representation of given list of selectors.
	 */
	public static String toString(final List<CssSelector> selectors) {
		final StringBuilder out = new StringBuilder();
		for (final CssSelector s : selectors) {
			out.append(s.toString());
		}
		return out.toString();
	}

}
