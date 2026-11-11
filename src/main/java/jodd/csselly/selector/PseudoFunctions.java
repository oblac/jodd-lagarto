package jodd.csselly.selector;

import jodd.csselly.CSSelly;
import jodd.csselly.CSSellyException;
import jodd.csselly.CssSelector;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeMatcher;
import jodd.lagarto.dom.NodeSelector;
import jodd.util.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public interface PseudoFunctions {
	/**
	 * The {@code :nth-child(an+b)} pseudo-class notation represents an element that has an+b-1
	 * siblings before it in the document tree, for any positive integer or zero value of n,
	 * and has a parent element. For values of a and b greater than zero, this effectively divides
	 * the element's children into groups of a elements (the last group taking the remainder),
	 * and selecting the bth element of each group. For example, this allows the selectors
	 * to address every other row in a table, and could be used to alternate the color of
	 * paragraph text in a cycle of four. The a and b values must be integers (positive, negative, or zero).
	 * The index of the first child of an element is 1.
	 */
	class NTH_CHILD extends PseudoFunction<PseudoFunctionExpression> {

		@Override
		public PseudoFunctionExpression parseExpression(final String expression) {
			return new PseudoFunctionExpression(expression);
		}

		@Override
		public boolean match(final Node node, final PseudoFunctionExpression expression) {
			final int value = node.getSiblingElementIndex() + 1;

			return expression.match(value);
		}
	}

	/**
	 * The {@code :nth-last-child(an+b)} pseudo-class notation represents an element that has
	 * an+b-1 siblings after it in the document tree, for any positive integer or zero value
	 * of n, and has a parent element.
	 */
	class NTH_LAST_CHILD extends PseudoFunction<PseudoFunctionExpression> {

		@Override
		public PseudoFunctionExpression parseExpression(final String expression) {
			return new PseudoFunctionExpression(expression);
		}

		@Override
		public boolean match(final Node node, final PseudoFunctionExpression expression) {
			final int value = node.getParentNode().getChildElementsCount() - node.getSiblingElementIndex();

			return expression.match(value);
		}

	}

	/**
	 * The {@code :nth-of-type(an+b)} pseudo-class notation represents an element that
	 * has an+b-1 siblings with the same expanded element name before it in the document tree,
	 * for any zero or positive integer value of n, and has a parent element.
	 */
	class NTH_OF_TYPE extends PseudoFunction<PseudoFunctionExpression> {

		@Override
		public PseudoFunctionExpression parseExpression(final String expression) {
			return new PseudoFunctionExpression(expression);
		}

		@Override
		public boolean match(final Node node, final PseudoFunctionExpression expression) {
			final int value = node.getSiblingNameIndex() + 1;

			return expression.match(value);
		}

	}

	/**
	 * The {@code :nth-last-of-type(an+b)} pseudo-class notation represents an element
	 * that has an+b-1 siblings with the same expanded element name after it in the document tree,
	 * for any zero or positive integer value of n, and has a parent element.
	 */
	class NTH_LAST_OF_TYPE extends PseudoFunction<PseudoFunctionExpression> {

		@Override
		public PseudoFunctionExpression parseExpression(final String expression) {
			return new PseudoFunctionExpression(expression);
		}

		@Override
		public boolean match(final Node node, final PseudoFunctionExpression expression) {
			final Node child = node.getParentNode().getLastChildElement(node.getNodeName());
			final int value = child.getSiblingNameIndex() + 1 - node.getSiblingNameIndex();

			return expression.match(value);
		}
	}

	// ---------------------------------------------------------------- extension

	/**
	 * Select the element at index n within the matched set.
	 */
	class EQ extends PseudoFunction<Integer> {

		@Override
		public Integer parseExpression(final String expression) {
			return Integer.valueOf(expression.trim());
		}

		@Override
		public boolean match(final Node node, final Integer expression) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index, final Integer expression) {
			final int value = expression.intValue();
			if (value >= 0) {
				return index == value;
			} else {
				return index == matchedResults.size() + value;
			}
		}
	}

	/**
	 * Select all elements at an index greater than index within the matched set.
	 */
	class GT extends PseudoFunction<Integer> {

		@Override
		public Integer parseExpression(final String expression) {
			return Integer.valueOf(expression.trim());
		}

		@Override
		public boolean match(final Node node, final Integer expression) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index, final Integer expression) {
			final int value = expression.intValue();
			return index > value;
		}
	}

	/**
	 * Select all elements at an index less than index within the matched set.
	 */
	class LT extends PseudoFunction<Integer> {

		@Override
		public Integer parseExpression(final String expression) {
			return Integer.valueOf(expression.trim());
		}

		@Override
		public boolean match(final Node node, final Integer expression) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index, final Integer expression) {
			final int value = expression.intValue();
			return index < value;
		}
	}

	/**
	 * Selects all elements that contain the specified text.
	 */
	class CONTAINS extends PseudoFunction<String> {

		@Override
		public String parseExpression(String expression) {
			if (StringUtil.startsWithChar(expression, '\'') || StringUtil.startsWithChar(expression, '"')) {
				expression = expression.substring(1, expression.length() - 1);
			}
			return unescape(expression);
		}

		@Override
		public boolean match(final Node node, final String expression) {
			final String text = node.getTextContent();
			return text.contains(expression);
		}

		/**
		 * Resolves CSS escapes, eg: {@code \(} or {@code \28 }, like jQuery does.
		 */
		static String unescape(final String value) {
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
						i = hexEnd;
						// a single whitespace terminates a hex escape
						if (i < length && Character.isWhitespace(value.charAt(i))) {
							i++;
						}
					} else {
						out.append(value.charAt(i++));
					}
				}
			}
			return out.toString();
		}
	}

	/**
	 * jsoup extension: selects elements that directly contain the specified text, ignoring case
	 * and with normalized whitespace. The text of children elements is not considered.
	 */
	class CONTAINS_OWN extends PseudoFunction<String> {

		@Override
		public String parseExpression(final String expression) {
			// like jsoup, the expression is taken as is, quotes included
			final String text = ElementText.unescape(expression);
			if (text.isEmpty()) {
				throw new CSSellyException(":containsOwn(text) query must not be empty");
			}
			return ElementText.lowerCase(ElementText.normaliseWhitespace(text));
		}

		@Override
		public boolean match(final Node node, final String expression) {
			return ElementText.lowerCase(ElementText.ownText(node)).contains(expression);
		}

		@Override
		public String getPseudoFunctionName() {
			return "containsOwn";
		}
	}

	/**
	 * jsoup extension: selects elements whose text, including the text of their descendants and with
	 * normalized whitespace, matches the specified regular expression.
	 */
	class MATCHES extends PseudoFunction<Pattern> {

		@Override
		public Pattern parseExpression(final String expression) {
			return compileRegex(getPseudoFunctionName(), expression);
		}

		@Override
		public boolean match(final Node node, final Pattern pattern) {
			return pattern.matcher(ElementText.text(node)).find();
		}
	}

	/**
	 * jsoup extension: selects elements whose own text, without the text of their children elements and with
	 * normalized whitespace, matches the specified regular expression.
	 */
	class MATCHES_OWN extends PseudoFunction<Pattern> {

		@Override
		public Pattern parseExpression(final String expression) {
			return compileRegex(getPseudoFunctionName(), expression);
		}

		@Override
		public boolean match(final Node node, final Pattern pattern) {
			return pattern.matcher(ElementText.ownText(node)).find();
		}

		@Override
		public String getPseudoFunctionName() {
			return "matchesOwn";
		}
	}

	/**
	 * Compiles the regular expression of a pseudo function. Like jsoup, the expression is taken as is.
	 */
	static Pattern compileRegex(final String pseudoFunctionName, final String regex) {
		if (regex.isEmpty()) {
			throw new CSSellyException(":" + pseudoFunctionName + "(regex) query must not be empty");
		}
		try {
			return Pattern.compile(regex);
		} catch (final PatternSyntaxException ex) {
			throw new CSSellyException(ex);
		}
	}

	// ---------------------------------------------------------------- advanced

	/**
	 * Selects elements which contain at least one element that matches the specified selector.
	 */
	class HAS extends PseudoFunction<List<List<CssSelector>>> {

		@Override
		public List<List<CssSelector>> parseExpression(String expression) {
			if (StringUtil.startsWithChar(expression, '\'') || StringUtil.startsWithChar(expression, '"')) {
				expression = expression.substring(1, expression.length() - 1);
			}
			final List<List<CssSelector>> selectors = CSSelly.parse(expression);
			for (final List<CssSelector> selector : selectors) {
				// relative selectors are anchored at the tested element, but an explicit :scope still represents
				// the scoping root of the whole selection, which is not available here
				if (selector != null && selector.get(0).isScope() && !selector.get(0).isRelative()) {
					throw new CSSellyException(":scope is not supported in :has()");
				}
			}
			return selectors;
		}

		@Override
		public boolean match(final Node node, final List<List<CssSelector>> selectors) {
			final NodeSelector nodeSelector = new NodeSelector(node);
			// selectors one by one: stop at the first one that matches, and don't sort the results of the group
			for (final List<CssSelector> selector : selectors) {
				if (!nodeSelector.select(Collections.singletonList(selector)).isEmpty()) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Base class for pseudo functions that match an element against a list of (non-relative) selectors.
	 */
	abstract class SelectorListPseudoFunction extends PseudoFunction<List<List<CssSelector>>> {

		@Override
		public List<List<CssSelector>> parseExpression(String expression) {
			if (StringUtil.startsWithChar(expression, '\'') || StringUtil.startsWithChar(expression, '"')) {
				expression = expression.substring(1, expression.length() - 1);
			}
			final List<List<CssSelector>> selectors = CSSelly.parse(expression);
			for (final List<CssSelector> selector : selectors) {
				if (selector == null) {
					throw new CSSellyException("Empty selector in :" + getPseudoFunctionName() + "()");
				}
				if (selector.get(0).isScope()) {
					throw new CSSellyException("Relative selectors are not supported in :" + getPseudoFunctionName() + "()");
				}
			}
			return selectors;
		}

		protected boolean matchAny(final Node node, final List<List<CssSelector>> selectors) {
			return new NodeMatcher(node).match(selectors);
		}
	}

	/**
	 * Selects all elements that do not match any of the given selectors.
	 */
	class NOT extends SelectorListPseudoFunction {

		@Override
		public boolean match(final Node node, final List<List<CssSelector>> selectors) {
			return !matchAny(node, selectors);
		}
	}

	/**
	 * Selects all elements that match any of the given selectors.
	 */
	class IS extends SelectorListPseudoFunction {

		@Override
		public boolean match(final Node node, final List<List<CssSelector>> selectors) {
			return matchAny(node, selectors);
		}
	}

	/**
	 * Same as {@link IS}, specificity doesn't apply here.
	 */
	class WHERE extends IS {
	}

}
