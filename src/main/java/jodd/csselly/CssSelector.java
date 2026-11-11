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

import jodd.csselly.selector.AttributeSelector;
import jodd.csselly.selector.PseudoClassSelector;
import jodd.csselly.selector.PseudoFunctionSelector;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeFilter;

import java.util.ArrayList;
import java.util.List;

import static jodd.csselly.selector.Match.EQUALS;
import static jodd.csselly.selector.Match.INCLUDES;

/**
 * CSS selector.
 */
public class CssSelector implements NodeFilter {

	private static final String ID = "id";
	private static final String CLASS = "class";
	private static final String STAR = "*";
	private static final String SCOPE = "scope";

	protected CssSelector prevCssSelector;
	protected CssSelector nextCssSelector;

	protected final String element;

	protected Combinator combinator = Combinator.DESCENDANT;
	protected List<Selector> selectors;
	protected boolean scope;
	protected boolean relative;

	public CssSelector() {
		this(null);
	}

	/**
	 * Creates a selector that represents the anchor of a relative selector, i.e. a selector that starts with a combinator,
	 * eg: {@code > p}. The anchor is the node the selection is performed on, eg the element tested by {@code :has()}.
	 */
	public static CssSelector scope() {
		final CssSelector cssSelector = new CssSelector();
		cssSelector.scope = true;
		cssSelector.relative = true;
		return cssSelector;
	}

	public CssSelector(String element) {
		if (element == null) {
			element = STAR;
		}
		this.element = unescape(element);
		this.selectors = new ArrayList<>();
	}

	/**
	 * Returns previous {@link CssSelector}.
	 */
	public CssSelector getPrevCssSelector() {
		return prevCssSelector;
	}

	/**
	 * Returns next {@link CssSelector}.
	 */
	public CssSelector getNextCssSelector() {
		return nextCssSelector;
	}

	// ---------------------------------------------------------------- init

	void setPrevCssSelector(final CssSelector prevCssSelector) {
		this.prevCssSelector = prevCssSelector;
		prevCssSelector.nextCssSelector = this;
	}

	// ---------------------------------------------------------------- selector

	/**
	 * Returns selector element name. Returns {@code *} for
	 * universal selectors.
	 */
	public String getElement() {
		return element;
	}

	/**
	 * Returns selector for given index.
	 */
	public Selector getSelector(final int index) {
		return selectors.get(index);
	}

	/**
	 * Returns number of selectors.
	 */
	public int selectorsCount() {
		return selectors.size();
	}

	/**
	 * Returns {@code true} if this selector represents the scoping root,
	 * either explicitly with {@code :scope} or implicitly when the selector starts with a combinator.
	 */
	public boolean isScope() {
		return scope;
	}

	/**
	 * Returns {@code true} if this selector is the implied anchor of a relative selector, eg: {@code > p},
	 * {@code false} if it's an explicit {@code :scope}.
	 */
	public boolean isRelative() {
		return relative;
	}

	/**
	 * Returns {@code true} if this selector has conditions besides representing the scoping root.
	 */
	public boolean hasConditions() {
		return !element.equals(STAR) || !selectors.isEmpty();
	}

	// ---------------------------------------------------------------- combinator

	/**
	 * Sets combinator.
	 */
	public void setCombinator(final Combinator combinator) {
		this.combinator = combinator;
	}

	/**
	 * Returns combinator, may be {@code null}.
	 */
	public Combinator getCombinator() {
		return combinator;
	}

	// ---------------------------------------------------------------- attributes

	public void addIdSelector(String id) {
		id = unescape(id);
		selectors.add(new AttributeSelector(ID, EQUALS, id));
	}

	public void addClassSelector(String clazz) {
		clazz = unescape(clazz);
		selectors.add(new AttributeSelector(CLASS, INCLUDES, clazz));
	}

	public void addAttributeSelector(final String attribute) {
		// the attribute name and value are unescaped once separated, as quotes may be escaped in the value
		selectors.add(new AttributeSelector(attribute));
	}

	// ---------------------------------------------------------------- pseudo class

	public void addPseudoClassSelector(final String pseudoClass) {
		if (SCOPE.equalsIgnoreCase(pseudoClass.trim())) {
			scope = true;
			return;
		}
		selectors.add(new PseudoClassSelector(pseudoClass));
	}

	public void addPseudoFunctionSelector(final String pseudoFunction, final String expression) {
		selectors.add(new PseudoFunctionSelector(pseudoFunction, expression));
	}

	// ---------------------------------------------------------------- string

	/**
	 * Generates CSS selector for the output.
	 */
	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append(element);
		if (scope) {
			out.append(':').append(SCOPE);
		}

		for (final Selector selector : selectors) {
			switch (selector.getType()) {
				case ATTRIBUTE:
					final AttributeSelector attrSelector = (AttributeSelector) selector;
					final String attrName = attrSelector.getName();
					if (attrName.equals(ID)) {
						out.append('#').append(attrSelector.getValue());
					} else if (attrName.equals(CLASS)) {
						out.append('.').append(attrSelector.getValue());
					} else {
						out.append('[').append(attrSelector.getName());
						final String value = attrSelector.getValue();
						if (value != null) {
							out.append(attrSelector.getMatch().getSign());
							final char quote = attrSelector.getQuoteChar();
							if (quote != 0) {
								out.append(quote);
							}
							out.append(value);
							if (quote != 0) {
								out.append(quote);
							}
							if (attrSelector.isCaseInsensitive()) {
								out.append(" i");
							} else if (attrSelector.isCaseSensitive()) {
								out.append(" s");
							}
						}
						out.append(']');
					}
					break;
				case PSEUDO_CLASS:
					final PseudoClassSelector psc = (PseudoClassSelector) selector;
					out.append(':').append(psc.getPseudoClass().getPseudoClassName());
					break;
				case PSEUDO_FUNCTION:
					final PseudoFunctionSelector pfns = (PseudoFunctionSelector) selector;
					out.append(':').append(pfns.getPseudoFunction().getPseudoFunctionName()).append('(');
					out.append(pfns.getExpression()).append(')');
					break;
			}
		}

		if (nextCssSelector != null) {
			if (combinator != Combinator.DESCENDANT) {
				out.append(' ');
			}
			out.append(combinator.getSign());
			if (combinator != Combinator.DESCENDANT) {
				out.append(' ');
			}
		}
		return out.toString();
	}


	// ---------------------------------------------------------------- match

	/**
	 * Accepts single node.
	 */
	@Override
	public boolean accept(final Node node) {
		// match element name with node name
		if (!matchElement(node)) {
			return false;
		}

		// match attributes
		final int totalSelectors = selectorsCount();
		for (int i = 0; i < totalSelectors; i++) {
			final Selector selector = getSelector(i);

			// just attr name existence
			switch (selector.getType()) {
				case ATTRIBUTE:
					if (!((AttributeSelector) selector).accept(node)) {
						return false;
					}
					break;
				case PSEUDO_CLASS:
					if (!((PseudoClassSelector) selector).accept(node)) {
						return false;
					}
					break;
				case PSEUDO_FUNCTION:
					if (!((PseudoFunctionSelector) selector).accept(node)) {
						return false;
					}
					break;
			}
		}
		return true;
	}

	/**
	 * Matches element to css selector. All non-element types are ignored.
	 */
	protected boolean matchElement(final Node node) {
		if (node.getNodeType() != Node.NodeType.ELEMENT) {
			return false;
		}
		final String element = getElement();
		if (element.equals(STAR)) {
			return true;
		}
		final String nodeName = node.getNodeName();
		// in case-insensitive documents (HTML), node names are lower-cased, so must be the type selector
		final Document document = node.getOwnerDocument();
		if (document != null && !document.getConfig().getParserConfig().isCaseSensitive()) {
			return element.equalsIgnoreCase(nodeName);
		}
		return element.equals(nodeName);
	}


	// ---------------------------------------------------------------- post process

	/**
	 * Accepts node within current results.
	 */
	public boolean accept(final List<Node> currentResults, final Node node, final int index) {
		// match attributes
		final int totalSelectors = selectorsCount();
		for (int i = 0; i < totalSelectors; i++) {
			final Selector selector = getSelector(i);

			// just attr name existence
			switch (selector.getType()) {
				case PSEUDO_FUNCTION:
					if (!((PseudoFunctionSelector) selector).accept(currentResults, node, index)) {
						return false;
					}
					break;
				case PSEUDO_CLASS:
					if (!((PseudoClassSelector) selector).accept(currentResults, node, index)) {
						return false;
					}
					break;
				default:
			}
		}
		return true;
	}


	// ---------------------------------------------------------------- util

	/**
	 * Resolves the CSS escapes of an identifier.
	 * @see CSSelly#unescape(String)
	 */
	protected String unescape(final String value) {
		return CSSelly.unescape(value);
	}

}
