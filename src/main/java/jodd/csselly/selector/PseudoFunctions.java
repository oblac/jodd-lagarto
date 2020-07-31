package jodd.csselly.selector;

import jodd.csselly.CSSelly;
import jodd.csselly.CssSelector;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeMatcher;
import jodd.lagarto.dom.NodeSelector;
import jodd.util.StringUtil;

import java.util.List;

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
			return expression;
		}

		@Override
		public boolean match(final Node node, final String expression) {
			final String text = node.getTextContent();
			return text.contains(expression);
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
			return CSSelly.parse(expression);
		}

		@Override
		public boolean match(final Node node, final List<List<CssSelector>> selectors) {
			final List<Node> matchedNodes = new NodeSelector(node).select(selectors);

			return !matchedNodes.isEmpty();
		}
	}

	/**
	 * Selects all elements that do not match the given selector.
	 */
	class NOT extends PseudoFunction<List<List<CssSelector>>> {

		@Override
		public List<List<CssSelector>> parseExpression(String expression) {
			if (StringUtil.startsWithChar(expression, '\'') || StringUtil.startsWithChar(expression, '"')) {
				expression = expression.substring(1, expression.length() - 1);
			}
			return CSSelly.parse(expression);
		}

		@Override
		public boolean match(final Node node, final List<List<CssSelector>> selectors) {
			return !new NodeMatcher(node).match(selectors);
		}
	}

}
