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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public interface PseudoFunctions {
	/**
	 * The expression of {@code :nth-child(an+b of S)} and {@code :nth-last-child(an+b of S)}:
	 * only the siblings that match the selector list S are counted.
	 */
	class NthChildExpression extends PseudoFunctionExpression {

		private static final Pattern OF = Pattern.compile("\\s+of\\s+", Pattern.CASE_INSENSITIVE);

		protected final List<List<CssSelector>> selectors;

		public NthChildExpression(final String expression, final List<List<CssSelector>> selectors) {
			super(expression);
			this.selectors = selectors;
		}

		/**
		 * Parses {@code an+b}, optionally followed by {@code of S}.
		 */
		static PseudoFunctionExpression parse(final String expression) {
			final Matcher matcher = OF.matcher(expression);
			if (!matcher.find()) {
				return new PseudoFunctionExpression(expression);
			}
			final String selectorList = expression.substring(matcher.end());
			if (selectorList.trim().isEmpty()) {
				throw new CSSellyException("Empty selector after 'of'");
			}
			final List<List<CssSelector>> selectors = CSSelly.parse(selectorList);
			for (final List<CssSelector> selector : selectors) {
				if (selector.get(0).isScope()) {
					throw new CSSellyException("Relative selectors are not supported after 'of'");
				}
			}
			return new NthChildExpression(expression.substring(0, matcher.start()), selectors);
		}

		/**
		 * Returns the selector list S.
		 */
		public List<List<CssSelector>> getSelectors() {
			return selectors;
		}

		/**
		 * Matches if the node matches S and if its index among the siblings that match S matches {@code an+b}.
		 */
		boolean matchAmongSiblings(final Node node, final boolean fromEnd) {
			if (!new NodeMatcher(node).match(selectors)) {
				return false;
			}
			final Node parent = node.getParentNode();
			final int childCount = parent.getChildNodesCount();
			int index = 1;
			for (int i = 0; i < childCount; i++) {
				final Node sibling = parent.getChild(fromEnd ? childCount - 1 - i : i);
				if (sibling == node) {
					return match(index);
				}
				if (sibling.getNodeType() == Node.NodeType.ELEMENT && new NodeMatcher(sibling).match(selectors)) {
					index++;
				}
			}
			return false;
		}
	}

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
			return NthChildExpression.parse(expression);
		}

		@Override
		public boolean match(final Node node, final PseudoFunctionExpression expression) {
			if (expression instanceof NthChildExpression) {
				return ((NthChildExpression) expression).matchAmongSiblings(node, false);
			}
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
			return NthChildExpression.parse(expression);
		}

		@Override
		public boolean match(final Node node, final PseudoFunctionExpression expression) {
			if (expression instanceof NthChildExpression) {
				return ((NthChildExpression) expression).matchAmongSiblings(node, true);
			}
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
	 * Selects all elements that contain the specified text, in them or in their descendants, like jsoup:
	 * case is ignored, whitespace is normalized, block elements are separated with a space,
	 * and the content of elements such as {@code script} or {@code style} is not considered.
	 */
	class CONTAINS extends PseudoFunction<String> {

		@Override
		public String parseExpression(String expression) {
			if (StringUtil.startsWithChar(expression, '\'') || StringUtil.startsWithChar(expression, '"')) {
				expression = expression.substring(1, expression.length() - 1);
			}
			return ElementText.lowerCase(ElementText.normaliseWhitespace(CSSelly.unescape(expression)));
		}

		@Override
		public boolean match(final Node node, final String expression) {
			return ElementText.lowerCase(ElementText.text(node)).contains(expression);
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
			if (expression.trim().isEmpty()) {
				throw new CSSellyException("Empty selector in :has()");
			}
			final List<List<CssSelector>> selectors = CSSelly.parse(expression);
			for (final List<CssSelector> selector : selectors) {
				// relative selectors are anchored at the tested element, but an explicit :scope still represents
				// the scoping root of the whole selection, which is not available here
				if (selector.get(0).isScope() && !selector.get(0).isRelative()) {
					throw new CSSellyException(":scope is not supported in :has()");
				}
			}
			return selectors;
		}

		@Override
		public boolean match(final Node node, final List<List<CssSelector>> selectors) {
			// stops at the first matching node
			return new NodeSelector(node).hasMatch(selectors);
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
			if (expression.trim().isEmpty()) {
				if (isForgiving()) {
					// matches nothing, like in browsers
					return Collections.emptyList();
				}
				throw new CSSellyException("Empty selector in :" + getPseudoFunctionName() + "()");
			}
			final List<List<CssSelector>> selectors = CSSelly.parse(expression);
			for (final List<CssSelector> selector : selectors) {
				if (selector.get(0).isScope()) {
					throw new CSSellyException("Relative selectors are not supported in :" + getPseudoFunctionName() + "()");
				}
			}
			return selectors;
		}

		/**
		 * Returns {@code true} if an empty selector list is accepted, as with {@code :is()} and {@code :where()}.
		 */
		protected boolean isForgiving() {
			return false;
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
		protected boolean isForgiving() {
			return true;
		}

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
