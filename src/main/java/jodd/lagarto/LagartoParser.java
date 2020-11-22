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

import jodd.net.HtmlDecoder;
import jodd.util.ArraysUtil;
import jodd.util.CharUtil;

import java.util.function.Consumer;

import static jodd.util.CharUtil.equalsOne;
import static jodd.util.CharUtil.isAlpha;
import static jodd.util.CharUtil.isDigit;

/**
 * HTML/XML content parser/tokenizer using {@link TagVisitor} for callbacks.
 * Works by the HTML5 specs for tokenization, as described
 * on <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/tokenization.html">WhatWG</a>.
 * Differences from the specs:
 *
 * <ul>
 * <li>text is emitted as a block of text, and not character by character.</li>
 * <li>tags name case (and letter case of other entities) is not changed, but case-sensitive
 * information exist for matching.
 * <li>the whole tokenization process is implemented here, without going into the tree building.
 * This applies for switching to the RAWTEXT state.
 * </li>
 * <li>script tag is emitted separately</li>
 * <li>conditional comments added</li>
 * <li>xml states and callbacks added</li>
 * </ul>
 */
@SuppressWarnings("DuplicatedCode")
public class LagartoParser {

	protected TagVisitor visitor;
	protected ParsedTag tag;
	protected ParsedDoctype doctype;
	protected final CharsInput in;
	protected final LagartoParserConfig config;

	/**
	 * Creates parser on char array.
	 */
	public LagartoParser(final LagartoParserConfig parserConfig, final char[] input) {
		this.config = parserConfig;
		in = new CharArrayInput(input);
		initialize();
	}

	/**
	 * Creates parser on char array.
	 */
	public LagartoParser(final char[] input) {
		this(new LagartoParserConfig(), input);
	}

	/**
	 * Creates parser on a char sequence.
	 */
	public LagartoParser(final LagartoParserConfig parserConfig, final CharSequence input) {
		this.config = parserConfig;
		in = new CharSequenceInput(input);
		initialize();
	}

	/**
	 * Creates parser on a char sequence.
	 */
	public LagartoParser(final CharSequence input) {
		this(new LagartoParserConfig(), input);
	}

	/**
	 * Initializes parser.
	 */
	protected void initialize() {
		this.tag = new ParsedTag();
		this.doctype = new ParsedDoctype();
		this.text = new char[config.getTextBufferSize()];
		this.textLen = 0;
	}

	// ---------------------------------------------------------------- configuration

	/**
	 * Returns {@link jodd.lagarto.LagartoParserConfig configuration} of the parser.
	 */
	public LagartoParserConfig getConfig() {
		return config;
	}

	/**
	 * Configures the parser.
	 */
	public LagartoParser configure(final Consumer<LagartoParserConfig> configConsumer) {
		configConsumer.accept(this.config);
		return this;
	}


	// ---------------------------------------------------------------- parse

	protected boolean parsing;

	/**
	 * Parses content and emits event to provided {@link TagVisitor}.
	 */
	public void parse(final TagVisitor visitor) {
		tag.init(config.caseSensitive);

		this.visitor = visitor;

		visitor.start();

		parsing = true;

		while (parsing) {
			state.parse();
		}

		emitText();

		visitor.end();
	}

	// ---------------------------------------------------------------- start & end

	/**
	 * Data state.
	 */
	protected State DATA_STATE =  new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					emitText();
					parsing = false;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '<') {
					emitText();
					state = TAG_OPEN;
					return;
				}

				if (c == '&') {
					consumeCharacterReference();
					continue;
				}

				textEmitChar(c);
			}
		}
	};

	protected void consumeCharacterReference(final char allowedChar) {
		in.ndx++;

		if (in.isEOF()) {
			return;
		}

		final char c = in.charAtNdx();

		if (c == allowedChar) {
			in.ndx--;
			return;
		}

		_consumeAttrCharacterReference();
	}

	protected void consumeCharacterReference() {
		in.ndx++;

		if (in.isEOF()) {
			return;
		}

		_consumeCharacterReference();
	}

	private void _consumeCharacterReference() {
		final int unconsumeNdx = in.ndx - 1;

		char c = in.charAtNdx();

		if (equalsOne(c, CONTINUE_CHARS)) {
			in.ndx = unconsumeNdx;
			textEmitChar('&');
			return;
		}

		if (c == '#') {
			_consumeNumber(unconsumeNdx);
		} else {
			final String name = in.decodeHtmlName();

			if (name == null) {
				// this error is not quite as by the spec. The spec says that
				// only a sequence of alphanumeric chars ending with semicolon
				// gives na error
				errorCharReference();
				textEmitChar('&');
				in.ndx = unconsumeNdx;
				return;
			}

			// missing legacy attribute thing

			in.ndx += name.length();

			textEmitChars(HtmlDecoder.lookup(name));

			c = in.charAtNdx();

			if (c != ';') {
				errorCharReference();
				in.ndx--;
			}
		}
	}

	private void _consumeAttrCharacterReference() {
		final int unconsumeNdx = in.ndx - 1;

		char c = in.charAtNdx();

		if (equalsOne(c, CONTINUE_CHARS)) {
			in.ndx = unconsumeNdx;
			textEmitChar('&');
			return;
		}

		if (c == '#') {
			_consumeNumber(unconsumeNdx);
		} else {
			final String name = in.decodeHtmlName();

			if (name == null) {
				// this error is not quite as by the spec. The spec says that
				// only a sequence of alphanumeric chars ending with semicolon
				// gives na error
				errorCharReference();
				textEmitChar('&');
				in.ndx = unconsumeNdx;
				return;
			}

			// missing legacy attribute thing

			in.ndx += name.length();
			c = in.charAtNdx();

			if (c == ';') {
				textEmitChars(HtmlDecoder.lookup(name));
			} else {
				textEmitChar('&');
				in.ndx = unconsumeNdx;
			}
		}
	}

	private void _consumeNumber(final int unconsumeNdx) {
		in.ndx++;

		if (in.isEOF()) {
			in.ndx = unconsumeNdx;
			return;
		}

		char c = in.charAtNdx();

		int value = 0;
		int digitCount = 0;

		if (c == 'X' || c == 'x') {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					in.ndx = unconsumeNdx;
					return;
				}

				c = in.charAtNdx();

				if (isDigit(c)) {
					value *= 16;
					value += c - '0';
					digitCount++;
				} else if ((c >= 'a') && (c <= 'f')) {
					value *= 16;
					value += c - 'a' + 10;
					digitCount++;
				} else if ((c >= 'A') && (c <= 'F')) {
					value *= 16;
					value += c - 'A' + 10;
					digitCount++;
				} else {
					break;
				}
			}
		} else {
			while (isDigit(c)) {
				value *= 10;
				value += c - '0';

				in.ndx++;

				if (in.isEOF()) {
					in.ndx = unconsumeNdx;
					return;
				}

				c = in.charAtNdx();
				digitCount++;
			}
		}

		if (digitCount == 0) {
			// no character matches the range
			errorCharReference();
			in.ndx = unconsumeNdx;
			return;
		}

		if (c != ';') {
			errorCharReference();
			in.ndx--;    // go back, as pointer is on the next char
		}

		boolean isErr = true;
		switch (value) {
			case 0: c = REPLACEMENT_CHAR; break;
			case 0x80: c = '\u20AC'; break;
			case 0x81: c = '\u0081'; break;
			case 0x82: c = '\u201A'; break;
			case 0x83: c = '\u0192'; break;
			case 0x84: c = '\u201E'; break;
			case 0x85: c = '\u2026'; break;
			case 0x86: c = '\u2020'; break;
			case 0x87: c = '\u2021'; break;
			case 0x88: c = '\u02C6'; break;
			case 0x89: c = '\u2030'; break;
			case 0x8A: c = '\u0160'; break;
			case 0x8B: c = '\u2039'; break;
			case 0x8C: c = '\u0152'; break;
			case 0x8D: c = '\u008D'; break;
			case 0x8E: c = '\u017D'; break;
			case 0x8F: c = '\u008F'; break;
			case 0x90: c = '\u0090'; break;
			case 0x91: c = '\u2018'; break;
			case 0x92: c = '\u2019'; break;
			case 0x93: c = '\u201C'; break;
			case 0x94: c = '\u201D'; break;
			case 0x95: c = '\u2022'; break;
			case 0x96: c = '\u2013'; break;
			case 0x97: c = '\u2014'; break;
			case 0x98: c = '\u02DC'; break;
			case 0x99: c = '\u2122'; break;
			case 0x9A: c = '\u0161'; break;
			case 0x9B: c = '\u203A'; break;
			case 0x9C: c = '\u0153'; break;
			case 0x9D: c = '\u009D'; break;
			case 0x9E: c = '\u017E'; break;
			case 0x9F: c = '\u0178'; break;
			default:
				isErr = false;
		}

		if (isErr) {
			errorCharReference();
			textEmitChar(c);
			return;
		}

		// Otherwise, if the number is in the range 0xD800 to 0xDFFF or is greater than 0x10FFFF, then this is a parse error.
		if (((value >= 0xD800) && (value <= 0xDFFF)) || (value > 0x10FFFF)) {
			errorCharReference();
			textEmitChar(REPLACEMENT_CHAR);
			return;
		}

		c = (char) value;

		textEmitChar(c);

		if (
			((c >= 0x0001) && (c <= 0x0008)) ||
			((c >= 0x000D) && (c <= 0x001F)) ||
			((c >= 0x007F) && (c <= 0x009F)) ||
			((c >= 0xFDD0) && (c <= 0xFDEF))
		) {
			errorCharReference();
			return;
		}

		if (equalsOne(c, INVALID_CHARS)) {
			errorCharReference();
		}
	}

	protected State TAG_OPEN = new State() {
		@Override
		public void parse() {
			tag.start(in.ndx);

			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				textEmitChar('<');
				return;
			}

			final char c = in.charAtNdx();

			if (c == '!') {
				state = MARKUP_DECLARATION_OPEN;
				return;
			}
			if (c == '/') {
				state = END_TAG_OPEN;
				return;
			}
			if (isAlpha(c)) {
				state = TAG_NAME;
				return;
			}
			if (config.parseXmlTags) {
				if (in.match(XML)) {
					in.ndx += XML.length - 1;
					if (xmlDeclaration == null) {
						xmlDeclaration = new XmlDeclaration();
					}
					state = xmlDeclaration.XML_BETWEEN;
					return;
				}
			}
			if (c == '?') {
				errorInvalidToken();
				state = BOGUS_COMMENT;
				return;
			}

			errorInvalidToken();
			state = DATA_STATE;
			textEmitChar('<');

			in.ndx--;
		}
	};

	protected State END_TAG_OPEN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				return;
			}

			final char c = in.charAtNdx();

			if (isAlpha(c)) {
				tag.setType(TagType.END);
				state = TAG_NAME;
				return;
			}

			errorInvalidToken();
			state = BOGUS_COMMENT;
		}
	};

	protected State TAG_NAME = new State() {
		@Override
		public void parse() {
			final int nameNdx = in.ndx;

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					state = BEFORE_ATTRIBUTE_NAME;
					tag.setName(in.subSequence(nameNdx, in.ndx));
					break;
				}

				if (c == '/') {
					state = SELF_CLOSING_START_TAG;
					tag.setName(in.subSequence(nameNdx, in.ndx));
					break;
				}

				if (c == '>') {
					state = DATA_STATE;
					tag.setName(in.subSequence(nameNdx, in.ndx));
					emitTag();
					break;
				}
			}
		}
	};

	protected State BEFORE_ATTRIBUTE_NAME = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '/') {
					state = SELF_CLOSING_START_TAG;
					return;
				}

				if (c == '>') {
					state = DATA_STATE;
					emitTag();
					return;
				}

				if (equalsOne(c, ATTR_INVALID_1)) {
					errorInvalidToken();
				}

				state = ATTRIBUTE_NAME;
				return;
			}
		}
	};

	protected State ATTRIBUTE_NAME = new State() {
		@Override
		public void parse() {
			attrStartNdx = in.ndx;

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					attrEndNdx = in.ndx;
					state = AFTER_ATTRIBUTE_NAME;
					return;
				}

				if (c == '/') {
					attrEndNdx = in.ndx;
					_addAttribute();
					state = SELF_CLOSING_START_TAG;
					return;
				}

				if (c == '=') {
					attrEndNdx = in.ndx;
					state = BEFORE_ATTRIBUTE_VALUE;
					return;
				}

				if (c == '>') {
					state = DATA_STATE;
					attrEndNdx = in.ndx;
					_addAttribute();
					emitTag();
					return;
				}

				if (equalsOne(c, ATTR_INVALID_2)) {
					errorInvalidToken();
				}
			}
		}
	};

	protected State AFTER_ATTRIBUTE_NAME = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '/') {
					state = SELF_CLOSING_START_TAG;
					return;
				}
				if (c == '=') {
					state = BEFORE_ATTRIBUTE_VALUE;
					return;
				}
				if (c == '>') {
					state = DATA_STATE;
					emitTag();
					return;
				}
				if (equalsOne(c, ATTR_INVALID_2)) {
					errorInvalidToken();
				}

				_addAttribute();
				state = ATTRIBUTE_NAME;
				return;
			}
		}
	};

	protected State BEFORE_ATTRIBUTE_VALUE = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '\"') {
					state = ATTR_VALUE_DOUBLE_QUOTED;
					return;
				}
				if (c == '\'') {
					state = ATTR_VALUE_SINGLE_QUOTED;
					return;
				}
				if (c == '&') {
					state = ATTR_VALUE_UNQUOTED;
					in.ndx--;
					return;
				}
				if (c == '>') {
					_addAttribute();
					errorInvalidToken();
					state = DATA_STATE;
					emitTag();
					return;
				}
				if (equalsOne(c, ATTR_INVALID_3)) {
					errorInvalidToken();
				}

				state = ATTR_VALUE_UNQUOTED;
				return;
			}
		}
	};

	protected State ATTR_VALUE_UNQUOTED = new State() {
		@Override
		public void parse() {
			textStart();
			textEmitChar(in.charAtNdx());

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					_addAttributeWithValue();
					state = BEFORE_ATTRIBUTE_NAME;
					return;
				}

				if (c == '&') {
					consumeCharacterReference('>');
					continue;
				}

				if (c == '>') {
					_addAttributeWithValue();
					state = DATA_STATE;
					emitTag();
					return;
				}

				if (equalsOne(c, ATTR_INVALID_4)) {
					errorInvalidToken();
				}

				textEmitChar(c);
			}
		}
	};

	protected State ATTR_VALUE_SINGLE_QUOTED = new State() {
		@Override
		public void parse() {
			textStart();

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '\'') {
					_addAttributeWithValue();
					state = AFTER_ATTRIBUTE_VALUE_QUOTED;
					return;
				}
				if (c == '&') {
					consumeCharacterReference('\'');
					continue;
				}

				textEmitChar(c);
			}
		}
	};

	protected State ATTR_VALUE_DOUBLE_QUOTED = new State() {
		@Override
		public void parse() {
			textStart();
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '"') {
					_addAttributeWithValue();
					state = AFTER_ATTRIBUTE_VALUE_QUOTED;
					return;
				}

				if (c == '&') {
					consumeCharacterReference('\"');
					continue;
				}

				textEmitChar(c);
			}
		}
	};

	protected State AFTER_ATTRIBUTE_VALUE_QUOTED = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				return;
			}

			final char c = in.charAtNdx();

			if (equalsOne(c, TAG_WHITESPACES)) {
				state = BEFORE_ATTRIBUTE_NAME;
				return;
			}

			if (c == '/') {
				state = SELF_CLOSING_START_TAG;
				return;
			}

			if (c == '>') {
				state = DATA_STATE;
				emitTag();
				return;
			}

			errorInvalidToken();
			state = BEFORE_ATTRIBUTE_NAME;
			in.ndx--;
		}
	};

	protected State SELF_CLOSING_START_TAG = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				return;
			}

			final char c = in.charAtNdx();

			if (c == '>') {
				tag.setType(TagType.SELF_CLOSING);
				state = DATA_STATE;
				emitTag();
				return;
			}

			errorInvalidToken();

			state = BEFORE_ATTRIBUTE_NAME;
			in.ndx--;
		}
	};

	// ---------------------------------------------------------------- special

	protected State BOGUS_COMMENT = new State() {
		@Override
		public void parse() {
			int commentEndNdx = in.find('>');

			if (commentEndNdx == -1) {
				commentEndNdx = in.total;
			}

			emitComment(in.ndx, commentEndNdx);

			state = DATA_STATE;
			in.ndx = commentEndNdx;
		}
	};

	protected State MARKUP_DECLARATION_OPEN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = BOGUS_COMMENT;
				return;
			}

			if (in.match(COMMENT_DASH)) {
				state = COMMENT_START;
				in.ndx++;
				return;
			}

			if (in.matchUpperCase(T_DOCTYPE)) {
				state = DOCTYPE;
				in.ndx += T_DOCTYPE.length - 1;
				return;
			}

			if (config.enableConditionalComments) {
				// CC: downlevel-revealed starting
				if (in.match(CC_IF)) {
					int ccEndNdx = in.find(CC_END, in.ndx + CC_IF.length);

					if (ccEndNdx == -1) {
						ccEndNdx = in.total;
					}

					final CharSequence expression = in.subSequence(in.ndx + 1, ccEndNdx);

					conditionalCommentStarted = true;
					visitor.condComment(expression, true, false, false);

					in.ndx = ccEndNdx + 1;
					state = DATA_STATE;
					return;
				}

				// CC: downlevel-* ending tag
				if (in.match(CC_ENDIF) && conditionalCommentStarted) {
					in.ndx += CC_ENDIF.length;

					int ccEndNdx = in.find('>');

					if (ccEndNdx == -1) {
						ccEndNdx = in.total;
					}

					if (in.match(COMMENT_DASH, ccEndNdx - 2)) {
						// downlevel-hidden ending tag
						visitor.condComment(_ENDIF, false, true, false);
					} else {
						visitor.condComment(_ENDIF, false, false, false);
					}
					conditionalCommentStarted = false;

					in.ndx = ccEndNdx;
					state = DATA_STATE;
					return;
				}
			}

			if (config.parseXmlTags) {
				if (in.match(CDATA)) {
					in.ndx += CDATA.length - 1;

					if (xmlDeclaration == null) {
						xmlDeclaration = new XmlDeclaration();
					}

					state = xmlDeclaration.CDATA;
					return;
				}
			}

			errorInvalidToken();
			state = BOGUS_COMMENT;
		}
	};

	// ---------------------------------------------------------------- RAWTEXT

	protected int rawTextStart;
	protected int rawTextEnd;
	protected char[] rawTagName;

	protected State RAWTEXT = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '<') {
					rawTextEnd = in.ndx;
					state = RAWTEXT_LESS_THAN_SIGN;
					return;
				}
			}
		}
	};

	protected State RAWTEXT_LESS_THAN_SIGN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				state = RAWTEXT;
				return;
			}

			final char c = in.charAtNdx();

			if (c == '/') {
				state = RAWTEXT_END_TAG_OPEN;
				return;
			}

			state = RAWTEXT;
		}
	};

	protected State RAWTEXT_END_TAG_OPEN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				state = RAWTEXT;
				return;
			}

			final char c = in.charAtNdx();

			if (isAlpha(c)) {
				state = RAWTEXT_END_TAG_NAME;
				return;
			}

			state = RAWTEXT;
		}
	};

	protected State RAWTEXT_END_TAG_NAME = new State() {
		@Override
		public void parse() {
			final int rawtextEndTagNameStartNdx = in.ndx;

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					state = RAWTEXT;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					if (isAppropriateTagName(rawTagName, rawtextEndTagNameStartNdx, in.ndx)) {
						textEmitChars(rawTextStart, rawTextEnd);
						emitText();

						state = BEFORE_ATTRIBUTE_NAME;
						tag.start(rawTextEnd);
						tag.setName(in.subSequence(rawtextEndTagNameStartNdx, in.ndx));
						tag.setType(TagType.END);
					} else {
						state = RAWTEXT;
					}
					return;
				}

				if (c == '/') {
					if (isAppropriateTagName(rawTagName, rawtextEndTagNameStartNdx, in.ndx)) {
						textEmitChars(rawTextStart, rawTextEnd);
						emitText();

						state = SELF_CLOSING_START_TAG;
						tag.start(rawTextEnd);
						tag.setName(in.subSequence(rawtextEndTagNameStartNdx, in.ndx));
						tag.setType(TagType.SELF_CLOSING);
					} else {
						state = RAWTEXT;
					}
					return;
				}

				if (c == '>') {
					if (isAppropriateTagName(rawTagName, rawtextEndTagNameStartNdx, in.ndx)) {
						textEmitChars(rawTextStart, rawTextEnd);
						emitText();

						state = DATA_STATE;
						tag.start(rawTextEnd);
						tag.setName(in.subSequence(rawtextEndTagNameStartNdx, in.ndx));
						tag.setType(TagType.END);
						tag.end(in.ndx);
						emitTag();
					} else {
						state = RAWTEXT;
					}
					return;
				}
				if (isAlpha(c)) {
					continue;
				}

				state = RAWTEXT;
				return;
			}
		}
	};

	// ---------------------------------------------------------------- RCDATA

	protected int rcdataTagStart = -1;
	protected char[] rcdataTagName;

	protected State RCDATA = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '<') {
					rcdataTagStart = in.ndx;
					state = RCDATA_LESS_THAN_SIGN;
					return;
				}

				if (c == '&') {
					consumeCharacterReference();
					continue;
				}

				textEmitChar(c);
			}
		}
	};

	protected State RCDATA_LESS_THAN_SIGN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				state = RCDATA;
				return;
			}

			final char c = in.charAtNdx();

			if (c == '/') {
				state = RCDATA_END_TAG_OPEN;
				return;
			}

			state = RCDATA;
			textEmitChar('<');
			textEmitChar(c);
		}
	};

	protected State RCDATA_END_TAG_OPEN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				state = RCDATA;
				return;
			}

			final char c = in.charAtNdx();

			if (isAlpha(c)) {
				state = RCDATA_END_TAG_NAME;
				return;
			}

			state = RCDATA;
			textEmitChar('<');
			textEmitChar('/');
			textEmitChar(c);
		}
	};

	protected State RCDATA_END_TAG_NAME = new State() {
		@Override
		public void parse() {
			final int rcdataEndTagNameStartNdx = in.ndx;

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					state = RCDATA;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					if (isAppropriateTagName(rcdataTagName, rcdataEndTagNameStartNdx, in.ndx)) {
						emitText();

						state = BEFORE_ATTRIBUTE_NAME;
						tag.start(rcdataTagStart);
						tag.setName(in.subSequence(rcdataEndTagNameStartNdx, in.ndx));
						tag.setType(TagType.END);
					} else {
						state = RCDATA;
					}
					return;
				}

				if (c == '/') {
					if (isAppropriateTagName(rcdataTagName, rcdataEndTagNameStartNdx, in.ndx)) {
						emitText();

						state = SELF_CLOSING_START_TAG;
						tag.start(rcdataTagStart);
						tag.setName(in.subSequence(rcdataEndTagNameStartNdx, in.ndx));
						tag.setType(TagType.SELF_CLOSING);
					} else {
						state = RCDATA;
					}
					return;
				}

				if (c == '>') {
					if (isAppropriateTagName(rcdataTagName, rcdataEndTagNameStartNdx, in.ndx)) {
						emitText();

						state = DATA_STATE;
						tag.start(rcdataTagStart);
						tag.setName(in.subSequence(rcdataEndTagNameStartNdx, in.ndx));
						tag.setType(TagType.END);
						tag.end(in.ndx);
						emitTag();
					} else {
						state = RCDATA;
					}
					return;
				}

				if (isAlpha(c)) {
					continue;
				}

				state = RCDATA;
				return;
			}
		}
	};

	// ---------------------------------------------------------------- comments

	protected int commentStart;

	protected State COMMENT_START = new State() {
		@Override
		public void parse() {
			in.ndx++;
			commentStart = in.ndx;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				emitComment(commentStart, in.total);
				return;
			}

			final char c = in.charAtNdx();

			if (c == '-') {
				state = COMMENT_START_DASH;
				return;
			}

			if (c == '>') {
				errorInvalidToken();
				state = DATA_STATE;
				emitComment(commentStart, in.ndx);
				return;
			}

			state = COMMENT;
		}
	};

	protected State COMMENT_START_DASH = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				emitComment(commentStart, in.total);
				return;
			}

			final char c = in.charAtNdx();

			if (c == '-') {
				state = COMMENT_END;
				return;
			}
			if (c == '>') {
				errorInvalidToken();
				state = DATA_STATE;
				emitComment(commentStart, in.ndx);
			}

			state = COMMENT;
		}
	};

	protected State COMMENT = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					emitComment(commentStart, in.total);
					return;
				}

				final char c = in.charAtNdx();

				if (c == '-') {
					state = COMMENT_END_DASH;
					return;
				}
			}
		}
	};

	protected State COMMENT_END_DASH = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				emitComment(commentStart, in.total);
				return;
			}

			final char c = in.charAtNdx();

			if (c == '-') {
				state = COMMENT_END;
				return;
			}

			state = COMMENT;
		}
	};

	protected State COMMENT_END = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				emitComment(commentStart, in.total);
				return;
			}

			final char c = in.charAtNdx();

			if (c == '>') {
				state = DATA_STATE;
				emitComment(commentStart, in.ndx - 2);
				return;
			}

			if (c == '!') {
				state = COMMENT_END_BANG;
				return;
			}

			if (c == '-') {
				// append a U+002D HYPHEN-MINUS character (-) to the comment token’s data
			} else {
				state = COMMENT;
			}
		}
	};

	protected State COMMENT_END_BANG = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				emitComment(commentStart, in.total);
				return;
			}

			final char c = in.charAtNdx();

			if (c == '-') {
				state = COMMENT_END_DASH;
				return;
			}
			if (c == '>') {
				state = DATA_STATE;
				emitComment(commentStart, in.ndx - 3);
				return;
			}
			state = COMMENT;
		}
	};

	// ---------------------------------------------------------------- DOCTYPE

	protected State DOCTYPE = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				doctype.quirksMode = true;
				emitDoctype();
				return;
			}

			final char c = in.charAtNdx();

			if (equalsOne(c, TAG_WHITESPACES)) {
				state = BEFORE_DOCTYPE_NAME;
				return;
			}

			errorInvalidToken();
			state = BEFORE_DOCTYPE_NAME;
			in.ndx--;
		}
	};

	protected State BEFORE_DOCTYPE_NAME = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '>') {
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				state = DOCTYPE_NAME;
				return;
			}
		}
	};

	protected State DOCTYPE_NAME = new State() {
		@Override
		public void parse() {
			final int nameStartNdx = in.ndx;

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					doctype.name = in.subSequence(nameStartNdx, in.ndx);
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					state = AFTER_DOCUMENT_NAME;
					doctype.name = in.subSequence(nameStartNdx, in.ndx);
					return;
				}

				if (c == '>') {
					state = DATA_STATE;
					doctype.name = in.subSequence(nameStartNdx, in.ndx);
					emitDoctype();
					return;
				}
			}
		}
	};

	protected State AFTER_DOCUMENT_NAME = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '>') {
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				if (in.matchUpperCase(A_PUBLIC)) {
					in.ndx += A_PUBLIC.length - 1;
					state = AFTER_DOCTYPE_PUBLIC_KEYWORD;
					return;
				}
				if (in.matchUpperCase(A_SYSTEM)) {
					in.ndx += A_SYSTEM.length - 1;
					state = AFTER_DOCTYPE_SYSTEM_KEYWORD;
					return;
				}

				errorInvalidToken();
				state = BOGUS_DOCTYPE;
				doctype.quirksMode = true;
				return;
			}
		}
	};

	protected int doctypeIdNameStart;

	protected State AFTER_DOCTYPE_PUBLIC_KEYWORD = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				doctype.quirksMode = true;
				emitDoctype();
				return;
			}

			final char c = in.charAtNdx();

			if (equalsOne(c, TAG_WHITESPACES)) {
				state = BEFORE_DOCTYPE_PUBLIC_IDENTIFIER;
				return;
			}

			if (c == '\"') {
				errorInvalidToken();
				doctypeIdNameStart = in.ndx + 1;
				state = DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED;
				return;
			}

			if (c == '\'') {
				errorInvalidToken();
				doctypeIdNameStart = in.ndx + 1;
				state = DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED;
				return;
			}

			if (c == '>') {
				errorInvalidToken();
				state = DATA_STATE;
				doctype.quirksMode = true;
				emitDoctype();
				return;
			}

			errorInvalidToken();
			state = BOGUS_DOCTYPE;
			doctype.quirksMode = true;
		}
	};

	protected State BEFORE_DOCTYPE_PUBLIC_IDENTIFIER = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '\"') {
					doctypeIdNameStart = in.ndx + 1;
					state = DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED;
					return;
				}

				if (c == '\'') {
					doctypeIdNameStart = in.ndx + 1;
					state = DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED;
					return;
				}

				if (c == '>') {
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				errorInvalidToken();
				doctype.quirksMode = true;
				state = BOGUS_DOCTYPE;
				return;
			}
		}
	};

	protected State DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					doctype.publicIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
				}

				final char c = in.charAtNdx();

				if (c == '\"') {
					doctype.publicIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					state = AFTER_DOCTYPE_PUBLIC_IDENTIFIER;
					return;
				}

				if (c == '>') {
					doctype.publicIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}
			}
		}
	};

	protected State DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					doctype.publicIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
				}

				final char c = in.charAtNdx();

				if (c == '\'') {
					doctype.publicIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					state = AFTER_DOCTYPE_PUBLIC_IDENTIFIER;
					return;
				}

				if (c == '>') {
					doctype.publicIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}
			}
		}
	};

	protected State AFTER_DOCTYPE_PUBLIC_IDENTIFIER = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				doctype.quirksMode = true;
				emitDoctype();
				return;
			}

			final char c = in.charAtNdx();

			if (equalsOne(c, TAG_WHITESPACES)) {
				state = BETWEEN_DOCTYPE_PUBLIC_AND_SYSTEM_IDENTIFIERS;
				return;
			}

			if (c == '>') {
				state = DATA_STATE;
				emitDoctype();
				return;
			}

			if (c == '\"') {
				errorInvalidToken();
				doctypeIdNameStart = in.ndx + 1;
				state = DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
				return;
			}

			if (c == '\'') {
				errorInvalidToken();
				doctypeIdNameStart = in.ndx + 1;
				state = DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
				return;
			}

			errorInvalidToken();
			doctype.quirksMode = true;
			state = BOGUS_DOCTYPE;
		}
	};

	protected State BETWEEN_DOCTYPE_PUBLIC_AND_SYSTEM_IDENTIFIERS = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '>') {
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				if (c == '\"') {
					doctypeIdNameStart = in.ndx + 1;
					state = DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
					return;
				}

				if (c == '\'') {
					doctypeIdNameStart = in.ndx + 1;
					state = DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
					return;
				}

				errorInvalidToken();
				doctype.quirksMode = true;
				state = BOGUS_DOCTYPE;
				return;
			}
		}
	};


	protected State BOGUS_DOCTYPE = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (c == '>') {
					state = DATA_STATE;
					emitDoctype();
					return;
				}
			}
		}
	};

	protected State AFTER_DOCTYPE_SYSTEM_KEYWORD = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				errorEOF();
				state = DATA_STATE;
				doctype.quirksMode = true;
				emitDoctype();
				return;
			}

			final char c = in.charAtNdx();

			if (equalsOne(c, TAG_WHITESPACES)) {
				state = BEFORE_DOCTYPE_SYSTEM_IDENTIFIER;
				return;
			}

			if (c == '\"') {
				errorInvalidToken();
				doctypeIdNameStart = in.ndx + 1;
				state = DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
				return;
			}

			if (c == '\'') {
				errorInvalidToken();
				doctypeIdNameStart = in.ndx + 1;
				state = DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
				return;
			}

			if (c == '>') {
				errorInvalidToken();
				state = DATA_STATE;
				doctype.quirksMode = true;
				emitDoctype();
				return;
			}

			errorInvalidToken();
			state = BOGUS_DOCTYPE;
			doctype.quirksMode = true;
		}
	};

	protected State BEFORE_DOCTYPE_SYSTEM_IDENTIFIER = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '\"') {
					doctypeIdNameStart = in.ndx + 1;
					state = DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
					return;
				}

				if (c == '\'') {
					doctypeIdNameStart = in.ndx + 1;
					state = DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
					return;
				}

				if (c == '>') {
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				errorInvalidToken();
				doctype.quirksMode = true;
				state = BOGUS_DOCTYPE;
				return;
			}
		}
	};

	protected State DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					doctype.systemIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
				}

				final char c = in.charAtNdx();

				if (c == '\"') {
					doctype.systemIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					state = AFTER_DOCTYPE_SYSTEM_IDENTIFIER;
					return;
				}

				if (c == '>') {
					doctype.systemIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}
			}
		}
	};

	protected State DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					doctype.systemIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
				}

				final char c = in.charAtNdx();

				if (c == '\'') {
					doctype.systemIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					state = AFTER_DOCTYPE_SYSTEM_IDENTIFIER;
					return;
				}

				if (c == '>') {
					doctype.systemIdentifier = in.subSequence(doctypeIdNameStart, in.ndx);
					errorInvalidToken();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}
			}
		}
	};

	protected State AFTER_DOCTYPE_SYSTEM_IDENTIFIER = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					doctype.quirksMode = true;
					emitDoctype();
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					continue;
				}

				if (c == '>') {
					state = DATA_STATE;
					emitDoctype();
					return;
				}

				errorInvalidToken();
				state = BOGUS_DOCTYPE;
				// does NOT set the quirks mode!
			}
		}
	};


	// ---------------------------------------------------------------- SCRIPT

	protected int scriptStartNdx = -1;
	protected int scriptEndNdx = -1;
	protected int scriptEndTagName = -1;

	protected State SCRIPT_DATA = new State() {
		@Override
		public void parse() {

			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					emitScript(scriptStartNdx, in.total);
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '<') {
					scriptEndNdx = in.ndx;
					state = SCRIPT_DATA_LESS_THAN_SIGN;
					return;
				}
			}
		}
	};

	protected State SCRIPT_DATA_LESS_THAN_SIGN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				state = SCRIPT_DATA;
				in.ndx--;
				return;
			}

			final char c = in.charAtNdx();

			if (c == '/') {
				state = SCRIPT_DATA_END_TAG_OPEN;
				return;
			}
			if (c == '!') {
				if (scriptEscape == null) {
					// create script escape states only if really needed
					scriptEscape = new ScriptEscape();
				}
				state = scriptEscape.SCRIPT_DATA_ESCAPE_START;
				return;
			}
			state = SCRIPT_DATA;
		}
	};

	protected State SCRIPT_DATA_END_TAG_OPEN = new State() {
		@Override
		public void parse() {
			in.ndx++;

			if (in.isEOF()) {
				state = SCRIPT_DATA;
				in.ndx--;
				return;
			}

			final char c = in.charAtNdx();

			if (isAlpha(c)) {
				state = SCRIPT_DATA_END_TAG_NAME;
				scriptEndTagName = in.ndx;
				return;
			}

			state = SCRIPT_DATA;
		}
	};

	protected State SCRIPT_DATA_END_TAG_NAME = new State() {
		@Override
		public void parse() {
			while (true) {
				in.ndx++;

				if (in.isEOF()) {
					state = SCRIPT_DATA;
					return;
				}

				final char c = in.charAtNdx();

				if (equalsOne(c, TAG_WHITESPACES)) {
					if (isAppropriateTagName(T_SCRIPT, scriptEndTagName, in.ndx)) {
						state = BEFORE_ATTRIBUTE_NAME;
					} else {
						state = SCRIPT_DATA;
					}
					return;
				}
				if (c == '/') {
					if (isAppropriateTagName(T_SCRIPT, scriptEndTagName, in.ndx)) {
						state = SELF_CLOSING_START_TAG;
					} else {
						state = SCRIPT_DATA;
					}
					return;
				}
				if (c == '>') {
					if (isAppropriateTagName(T_SCRIPT, scriptEndTagName, in.ndx)) {
						state = DATA_STATE;
						emitScript(scriptStartNdx, scriptEndNdx);
					} else {
						state = SCRIPT_DATA;
					}
					return;
				}
				if (isAlpha(c)) {
					continue;
				}
				state = SCRIPT_DATA;
				return;
			}
		}
	};

	// ---------------------------------------------------------------- SCRIPT ESCAPE

	protected ScriptEscape scriptEscape = null;

	/**
	 * Since escaping states inside the SCRIPT tag are rare, we want to use them
	 * lazy, only when really needed. Therefore, they are all grouped inside separate
	 * class that will be instantiated only if needed.
	 */
	protected class ScriptEscape {

		protected int doubleEscapedNdx = -1;
		protected int doubleEscapedEndTag = -1;

		protected State SCRIPT_DATA_ESCAPE_START = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					state = SCRIPT_DATA;
					in.ndx--;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '-') {
					state = SCRIPT_DATA_ESCAPE_START_DASH;
					return;
				}

				state = SCRIPT_DATA;
			}
		};

		protected State SCRIPT_DATA_ESCAPE_START_DASH = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					state = SCRIPT_DATA;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '-') {
					state = SCRIPT_DATA_ESCAPED_DASH_DASH;
					return;
				}

				state = SCRIPT_DATA;
			}
		};

		protected State SCRIPT_DATA_ESCAPED_DASH_DASH = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '-') {
					return;
				}

				if (c == '<') {
					state = SCRIPT_DATA_ESCAPED_LESS_THAN_SIGN;
					return;
				}

				if (c == '>') {
					state = SCRIPT_DATA;
					return;
				}

				state = SCRIPT_DATA_ESCAPED;
			}
		};

		protected State SCRIPT_DATA_ESCAPED_LESS_THAN_SIGN = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					state = SCRIPT_DATA_ESCAPED;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '/') {
					doubleEscapedNdx = -1;
					state = SCRIPT_DATA_ESCAPED_END_TAG_OPEN;
					return;
				}

				if (isAlpha(c)) {
					doubleEscapedNdx = in.ndx;
					state = SCRIPT_DATA_DOUBLE_ESCAPE_START;
					return;
				}

				state = SCRIPT_DATA_ESCAPED;
			}
		};

		protected State SCRIPT_DATA_ESCAPED = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						emitScript(scriptStartNdx, in.total);
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (c == '-') {
						state = SCRIPT_DATA_ESCAPED_DASH;
						break;
					}

					if (c == '<') {
						state = SCRIPT_DATA_ESCAPED_LESS_THAN_SIGN;
						return;
					}
				}
			}
		};


		protected State SCRIPT_DATA_ESCAPED_DASH = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '-') {
					state = SCRIPT_DATA_ESCAPED_DASH_DASH;
					return;
				}

				if (c == '<') {
					state = SCRIPT_DATA_ESCAPED_DASH_DASH;
					return;
				}

				state = SCRIPT_DATA_ESCAPED;
			}
		};

		protected State SCRIPT_DATA_ESCAPED_END_TAG_OPEN = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					state = SCRIPT_DATA_ESCAPED;
					return;
				}

				final char c = in.charAtNdx();

				if (isAlpha(c)) {
					// todo Create a new end tag token?
					state = SCRIPT_DATA_ESCAPED_END_TAG_NAME;
				}

				state = SCRIPT_DATA_ESCAPED;
			}
		};

		protected State SCRIPT_DATA_ESCAPED_END_TAG_NAME = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						state = SCRIPT_DATA_ESCAPED;
						return;
					}

					final char c = in.charAtNdx();

					if (equalsOne(c, TAG_WHITESPACES)) {
						if (isAppropriateTagName(T_SCRIPT, scriptEndTagName, in.ndx)) {
							state = BEFORE_ATTRIBUTE_NAME;
						} else {
							state = SCRIPT_DATA_ESCAPED;
						}
						return;
					}
					if (c == '/') {
						if (isAppropriateTagName(T_SCRIPT, scriptEndTagName, in.ndx)) {
							state = SELF_CLOSING_START_TAG;
						} else {
							state = SCRIPT_DATA_ESCAPED;
						}
						return;
					}
					if (c == '>') {
						if (isAppropriateTagName(T_SCRIPT, scriptEndTagName, in.ndx)) {
							state = DATA_STATE;
							emitTag();
						} else {
							state = SCRIPT_DATA_ESCAPED;
						}
						return;
					}
					if (isAlpha(c)) {
						continue;
					}
					state = SCRIPT_DATA_ESCAPED;
					return;
				}
			}
		};

		// ---------------------------------------------------------------- SCRIPT DOUBLE ESCAPE

		protected State SCRIPT_DATA_DOUBLE_ESCAPE_START = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						state = SCRIPT_DATA_ESCAPED;
						return;
					}

					final char c = in.charAtNdx();

					if (equalsOne(c, TAG_WHITESPACES_OR_END)) {
						if (isAppropriateTagName(T_SCRIPT, doubleEscapedNdx, in.ndx)) {
							state = SCRIPT_DATA_DOUBLE_ESCAPED;
						} else {
							state = SCRIPT_DATA_ESCAPED;
						}
						return;
					}

					if (isAlpha(c)) {
						continue;
					}
					state = SCRIPT_DATA_ESCAPED;
					return;
				}
			}
		};

		protected State SCRIPT_DATA_DOUBLE_ESCAPED = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (c == '-') {
						state = SCRIPT_DATA_DOUBLE_ESCAPED_DASH;
						return;
					}

					if (c == '<') {
						state = SCRIPT_DATA_DOUBLE_ESCAPED_LESS_THAN_SIGN;
						return;
					}
				}
			}
		};

		protected State SCRIPT_DATA_DOUBLE_ESCAPED_DASH = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '-') {
					state = SCRIPT_DATA_DOUBLE_ESCAPED_DASH_DASH;
					return;
				}
				if (c == '<') {
					state = SCRIPT_DATA_DOUBLE_ESCAPED_LESS_THAN_SIGN;
					return;
				}
				state = SCRIPT_DATA_DOUBLE_ESCAPED;
			}
		};

		protected State SCRIPT_DATA_DOUBLE_ESCAPED_DASH_DASH = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (c == '-') {
						continue;
					}

					if (c == '<') {
						state = SCRIPT_DATA_DOUBLE_ESCAPED_LESS_THAN_SIGN;
						return;
					}
					if (c == '>') {
						state = SCRIPT_DATA;
						return;
					}
					state = SCRIPT_DATA_DOUBLE_ESCAPED;
					return;
				}
			}
		};

		protected State SCRIPT_DATA_DOUBLE_ESCAPED_LESS_THAN_SIGN = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					state = SCRIPT_DATA_DOUBLE_ESCAPED;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '/') {
					state = SCRIPT_DATA_DOUBLE_ESCAPE_END;
					return;
				}

				state = SCRIPT_DATA_DOUBLE_ESCAPED;
			}
		};

		protected State SCRIPT_DATA_DOUBLE_ESCAPE_END = new State() {
			@Override
			public void parse() {
				doubleEscapedEndTag = in.ndx + 1;

				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						state = SCRIPT_DATA_DOUBLE_ESCAPED;
						return;
					}

					final char c = in.charAtNdx();

					if (equalsOne(c, TAG_WHITESPACES_OR_END)) {
						if (isAppropriateTagName(T_SCRIPT, doubleEscapedEndTag, in.ndx)) {
							state = SCRIPT_DATA_ESCAPED;
						} else {
							state = SCRIPT_DATA_DOUBLE_ESCAPED;
						}
						return;
					}
					if (isAlpha(c)) {
						continue;
					}

					state = SCRIPT_DATA_DOUBLE_ESCAPED;
					return;
				}
			}
		};
	}

	// ---------------------------------------------------------------- xml

	protected XmlDeclaration xmlDeclaration = null;

	protected class XmlDeclaration {

		protected int xmlAttrCount = 0;
		protected int xmlAttrStartNdx = -1;
		protected CharSequence version;
		protected CharSequence encoding;
		protected CharSequence standalone;
		protected char attrQuote;

		protected void reset() {
			xmlAttrCount = 0;
			xmlAttrStartNdx = -1;
			version = encoding = standalone = null;
		}

		protected State XML_BETWEEN = new State() {
			@Override
			public void parse() {

				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (equalsOne(c, TAG_WHITESPACES)) {
						continue;
					}

					if (c == '?') {
						state = XML_CLOSE;
						return;
					}

					switch (xmlAttrCount) {
						case 0:
							if (in.match(XML_VERSION)) {
								in.ndx += XML_VERSION.length - 1;
								state = AFTER_XML_ATTRIBUTE_NAME;
								return;
							}
							break;
						case 1:
							if (in.match(XML_ENCODING)) {
								in.ndx += XML_ENCODING.length - 1;
								state = AFTER_XML_ATTRIBUTE_NAME;
								return;
							}
							break;
						case 2:
							if (in.match(XML_STANDALONE)) {
								in.ndx += XML_STANDALONE.length - 1;
								state = AFTER_XML_ATTRIBUTE_NAME;
								return;
							}
							break;
					}

					errorInvalidToken();
					state = DATA_STATE;
				}
			}
		};

		protected State AFTER_XML_ATTRIBUTE_NAME = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (equalsOne(c, TAG_WHITESPACES)) {
						continue;
					}

					if (c == '=') {
						state = BEFORE_XML_ATTRIBUTE_VALUE;
						return;
					}

					errorInvalidToken();
					state = DATA_STATE;
					return;
				}
			}
		};

		protected State BEFORE_XML_ATTRIBUTE_VALUE = new State() {
			@Override
			public void parse() {
				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (equalsOne(c, TAG_WHITESPACES)) {
						continue;
					}

					if (c == '\"' || c == '\'') {
						state = XML_ATTRIBUTE_VALUE;
						attrQuote = c;
						return;
					}

					errorInvalidToken();
					state = DATA_STATE;
					return;
				}
			}
		};

		protected State XML_ATTRIBUTE_VALUE = new State() {
			@Override
			public void parse() {
				xmlAttrStartNdx = in.ndx + 1;

				while (true) {
					in.ndx++;

					if (in.isEOF()) {
						errorEOF();
						state = DATA_STATE;
						return;
					}

					final char c = in.charAtNdx();

					if (c == attrQuote) {
						final CharSequence value = in.subSequence(xmlAttrStartNdx, in.ndx);

						switch (xmlAttrCount) {
							case 0:
								version = value;
								break;
							case 1:
								encoding = value;
								break;
							case 2:
								standalone = value;
								break;
						}

						xmlAttrCount++;

						state = XML_BETWEEN;
						return;
					}
				}
			}
		};


		protected State XML_CLOSE = new State() {
			@Override
			public void parse() {
				in.ndx++;

				if (in.isEOF()) {
					errorEOF();
					state = DATA_STATE;
					return;
				}

				final char c = in.charAtNdx();

				if (c == '>') {
					emitXml();
					state = DATA_STATE;
					return;
				}

				errorInvalidToken();
				state = DATA_STATE;
			}
		};

		// ---------------------------------------------------------------- CDATA

		protected State CDATA = new State() {
			@Override
			public void parse() {
				in.ndx++;

				int cdataEndNdx = in.find(CDATA_END, in.ndx);

				if (cdataEndNdx == -1) {
					cdataEndNdx = in.total;
				}

				final CharSequence cdata = in.subSequence(in.ndx, cdataEndNdx);

				emitCData(cdata);

				in.ndx = cdataEndNdx + 2;

				state = DATA_STATE;
			}
		};

	}

	// ---------------------------------------------------------------- text

	protected char[] text;
	protected int textLen;

	private void ensureCapacity() {
		if (textLen == text.length) {
			text = ArraysUtil.resize(text, textLen << 1);
		}
	}

	private void ensureCapacity(final int growth) {
		final int desiredLen = textLen + growth;
		if (desiredLen > text.length) {
			text = ArraysUtil.resize(text, Math.max(textLen << 1, desiredLen));
		}
	}

	/**
	 * Emits characters into the local text buffer.
	 */
	protected void textEmitChar(final char c) {
		ensureCapacity();
		text[textLen++] = c;
	}

	/**
	 * Resets text buffer.
	 */
	protected void textStart() {
		textLen = 0;
	}

	protected void textEmitChars(int from, final int to) {
		ensureCapacity(to - from);
		// todo make it better, not char by char?
		while (from < to) {
			text[textLen++] = in.charAt(from++);
		}
	}

	protected void textEmitChars(final char[] buffer) {
		ensureCapacity(buffer.length);
		for (final char aBuffer : buffer) {
			text[textLen++] = aBuffer;
		}
	}

	protected CharSequence textWrap() {
		if (textLen == 0) {
			return CharArrayInput.EMPTY_CHAR_SEQUENCE;
		}
		return new String(text, 0, textLen);    // todo use charSequence pointer instead!
	}

	// ---------------------------------------------------------------- attr

	protected int attrStartNdx = -1;
	protected int attrEndNdx = -1;

	private void _addAttribute() {
		_addAttribute(in.subSequence(attrStartNdx, attrEndNdx), null);
	}

	private void _addAttributeWithValue() {
		_addAttribute(in.subSequence(attrStartNdx, attrEndNdx), textWrap().toString());
	}

	private void _addAttribute(final CharSequence attrName, final CharSequence attrValue) {
		if (tag.getType() == TagType.END) {
			_error("Ignored end tag attribute");
		} else {
			if (tag.hasAttribute(attrName)) {
				_error("Ignored duplicated attribute: " + attrName);
			} else {
				tag.addAttribute(attrName, attrValue);
			}
		}

		attrStartNdx = -1;
		attrEndNdx = -1;
		textLen = 0;
	}

	protected void emitTag() {
		tag.end(in.ndx + 1);

		if (config.calculatePosition) {
			tag.setPosition(in.positionOf(tag.getTagPosition()));
		}

		if (tag.getType().isStartingTag()) {

			if (matchTagName(T_SCRIPT)) {
				scriptStartNdx = in.ndx + 1;
				state = SCRIPT_DATA;
				return;
			}

			// detect RAWTEXT tags

			if (config.enableRawTextModes) {
				for (final char[] rawtextTagName : RAWTEXT_TAGS) {
					if (matchTagName(rawtextTagName)) {
						tag.setRawTag(true);
						state = RAWTEXT;
						rawTextStart = in.ndx + 1;
						rawTagName = rawtextTagName;
						break;
					}
				}

				// detect RCDATA tag

				for (final char[] rcdataTextTagName : RCDATA_TAGS) {
					if (matchTagName(rcdataTextTagName)) {
						state = RCDATA;
						rcdataTagStart = in.ndx + 1;
						rcdataTagName = rcdataTextTagName;
						break;
					}
				}
			}

			tag.increaseDeepLevel();
		}

		visitor.tag(tag);

		if (tag.getType().isEndingTag()) {
			tag.decreaseDeepLevel();
		}
	}

	private boolean conditionalCommentStarted = false;

	/**
	 * Emits a comment. Also checks for conditional comments!
	 */
	protected void emitComment(final int from, final int to) {
		if (from == -1) {
			// special case when `from` is `-1` in invalid comment
			visitor.comment(CharArrayInput.EMPTY_CHAR_SEQUENCE);
			return;
		}
		if (config.enableConditionalComments) {
			// CC: downlevel-hidden starting
			if (in.match(CC_IF, from)) {
				final int endBracketNdx = in.find(']', from + 3, to);

				if (endBracketNdx == -1) {
					// wrong syntax for CC, then it's just a comment
					// meh, the code repeats, see the end of the method.
					final CharSequence comment = in.subSequence(from, to);
					visitor.comment(comment);
					commentStart = -1;
					return;
				}

				final CharSequence expression = in.subSequence(from + 1, endBracketNdx);

				in.ndx = endBracketNdx + 1;

				final char c = in.charAtNdx();

				if (c != '>') {
					errorInvalidToken();
				}

				conditionalCommentStarted = true;
				visitor.condComment(expression, true, true, false);

				state = DATA_STATE;
				return;
			}

			if (to > CC_ENDIF2.length && in.match(CC_ENDIF2, to - CC_ENDIF2.length) && conditionalCommentStarted) {
				// CC: downlevel-hidden ending
				visitor.condComment(_ENDIF, false, true, true);
				conditionalCommentStarted = false;

				state = DATA_STATE;
				return;
			}
		}

		// just a comment
		final CharSequence comment = in.subSequence(from, to);
		visitor.comment(comment);
		commentStart = -1;
	}

	/**
	 * Emits text if there is some content.
	 */
	protected void emitText() {
		if (textLen != 0) {
			visitor.text(textWrap());
		}
		textLen = 0;
	}

	protected void emitScript(final int from, final int to) {
		tag.increaseDeepLevel();

		tag.setRawTag(true);
		visitor.script(tag, in.subSequence(from, to));

		tag.decreaseDeepLevel();
		scriptStartNdx = -1;
		scriptEndNdx = -1;
	}

	protected void emitDoctype() {
		visitor.doctype(doctype);

		doctype.reset();
	}

	protected void emitXml() {
		visitor.xml(xmlDeclaration.version, xmlDeclaration.encoding, xmlDeclaration.standalone);

		xmlDeclaration.reset();
	}

	protected void emitCData(final CharSequence charSequence) {
		visitor.cdata(charSequence);
	}

	// ---------------------------------------------------------------- error

	protected void errorEOF() {
		_error("Parse error: EOF");
	}

	protected void errorInvalidToken() {
		_error("Parse error: invalid token");
	}

	protected void errorCharReference() {
		_error("Parse error: invalid character reference");
	}

	/**
	 * Prepares error message and reports it to the visitor.
	 */
	protected void _error(String message) {
		if (config.calculatePosition) {
			final CharsInput.Position currentPosition = in.positionOf(in.ndx);
			message = message
					.concat(" ")
					.concat(currentPosition.toString());
		} else {
			message = message
					.concat(" [@")
					.concat(Integer.toString(in.ndx))
					.concat("]");
		}

		visitor.error(message);
	}

	// ---------------------------------------------------------------- util

	private boolean isAppropriateTagName(final char[] lowerCaseNameToMatch, final int from, final int to) {
		final int len = to - from;

		if (len != lowerCaseNameToMatch.length) {
			return false;
		}

		for (int i = from, k = 0; i < to; i++, k++) {
			char c = in.charAt(i);

			c = CharUtil.toLowerAscii(c);

			if (c != lowerCaseNameToMatch[k]) {
				return false;
			}
		}
		return true;
	}

	private boolean matchTagName(final char[] tagNameLowercase) {
		final CharSequence charSequence = tag.getName();

		final int length = tagNameLowercase.length;
		if (charSequence.length() != length) {
			return false;
		}

		for (int i = 0; i < length; i++) {
			char c = charSequence.charAt(i);

			c = CharUtil.toLowerAscii(c);

			if (c != tagNameLowercase[i]) {
				return false;
			}
		}

		return true;
	}

	// ---------------------------------------------------------------- state

	protected State state = DATA_STATE;

	// ---------------------------------------------------------------- names

	private static final char[] TAG_WHITESPACES = new char[]{'\t', '\n', '\r', ' '};
	private static final char[] TAG_WHITESPACES_OR_END = new char[]{'\t', '\n', '\r', ' ', '/', '>'};
	private static final char[] CONTINUE_CHARS = new char[]{'\t', '\n', '\r', ' ', '<', '&'};

	private static final char[] ATTR_INVALID_1 = new char[]{'\"', '\'', '<', '='};
	private static final char[] ATTR_INVALID_2 = new char[]{'\"', '\'', '<'};
	private static final char[] ATTR_INVALID_3 = new char[]{'<', '=', '`'};
	private static final char[] ATTR_INVALID_4 = new char[]{'"', '\'', '<', '=', '`'};

	private static final char[] COMMENT_DASH = new char[]{'-', '-'};

	private static final char[] T_DOCTYPE = new char[]{'D', 'O', 'C', 'T', 'Y', 'P', 'E'};
	private static final char[] T_SCRIPT = new char[]{'s', 'c', 'r', 'i', 'p', 't'};
	private static final char[] T_XMP = new char[]{'x', 'm', 'p'};
	private static final char[] T_STYLE = new char[]{'s', 't', 'y', 'l', 'e'};
	private static final char[] T_IFRAME = new char[]{'i', 'f', 'r', 'a', 'm', 'e'};
	private static final char[] T_NOFRAMES = new char[]{'n', 'o', 'f', 'r', 'a', 'm', 'e', 's'};
	private static final char[] T_NOEMBED = new char[]{'n', 'o', 'e', 'm', 'b', 'e', 'd'};
	private static final char[] T_NOSCRIPT = new char[]{'n', 'o', 's', 'c', 'r', 'i', 'p', 't'};
	private static final char[] T_TEXTAREA = new char[]{'t', 'e', 'x', 't', 'a', 'r', 'e', 'a'};
	private static final char[] T_TITLE = new char[]{'t', 'i', 't', 'l', 'e'};

	private static final char[] A_PUBLIC = new char[]{'P', 'U', 'B', 'L', 'I', 'C'};
	private static final char[] A_SYSTEM = new char[]{'S', 'Y', 'S', 'T', 'E', 'M'};

	private static final char[] CDATA = new char[]{'[', 'C', 'D', 'A', 'T', 'A', '['};
	private static final char[] CDATA_END = new char[]{']', ']', '>'};

	private static final char[] XML = new char[]{'?', 'x', 'm', 'l'};
	private static final char[] XML_VERSION = new char[]{'v', 'e', 'r', 's', 'i', 'o', 'n'};
	private static final char[] XML_ENCODING = new char[]{'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g'};
	private static final char[] XML_STANDALONE = new char[]{'s', 't', 'a', 'n', 'd', 'a', 'l', 'o', 'n', 'e'};

	private static final char[] CC_IF = new char[]{'[', 'i', 'f', ' '};
	private static final char[] CC_ENDIF = new char[]{'[', 'e', 'n', 'd', 'i', 'f', ']'};
	private static final char[] CC_ENDIF2 = new char[]{'<', '!', '[', 'e', 'n', 'd', 'i', 'f', ']'};
	private static final char[] CC_END = new char[]{']', '>'};

	// CDATA
	private static final char[][] RAWTEXT_TAGS = new char[][]{
			T_XMP, T_STYLE, T_IFRAME, T_NOEMBED, T_NOFRAMES, T_NOSCRIPT, T_SCRIPT
	};

	private static final char[][] RCDATA_TAGS = new char[][]{
			T_TEXTAREA, T_TITLE
	};

	private static final char REPLACEMENT_CHAR = '\uFFFD';

	private static final char[] INVALID_CHARS = new char[]{'\u000B', '\uFFFE', '\uFFFF'};
	//, '\u1FFFE', '\u1FFFF', '\u2FFFE', '\u2FFFF', '\u3FFFE', '\u3FFFF', '\u4FFFE,
	//	'\u4FFFF', '\u5FFFE', '\u5FFFF', '\u6FFFE', '\u6FFFF', '\u7FFFE', '\u7FFFF', '\u8FFFE', '\u8FFFF', '\u9FFFE,
	//	'\u9FFFF', '\uAFFFE', '\uAFFFF', '\uBFFFE', '\uBFFFF', '\uCFFFE', '\uCFFFF', '\uDFFFE', '\uDFFFF', '\uEFFFE,
	//	'\uEFFFF', '\uFFFFE', '\uFFFFF', '\u10FFFE', '\u10FFFF',

	private static final CharSequence _ENDIF = "endif";

}
