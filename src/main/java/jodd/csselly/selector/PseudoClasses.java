package jodd.csselly.selector;

import jodd.lagarto.dom.Node;

import java.util.List;

/**
 * Simple collection of {@code PseudoClass} implementations.
 */
public interface PseudoClasses {

	// ---------------------------------------------------------------- STANDARD PSEUDO CLASSES

	/**
	 * Same as {@code :nth-child(1)}. Represents an element that is the first child of some other element.
	 */
	class FIRST_CHILD extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getSiblingElementIndex() == 0;
		}
	}

	/**
	 * Same as {@code :nth-last-child(1)}. Represents an element that is the last child of some other element.
	 */
	class LAST_CHILD extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getSiblingElementIndex() == node.getParentNode().getChildElementsCount() - 1;
		}
	}

	/**
	 * Represents an element that has a parent element and whose parent element has no other element children.
	 * Same as {@code :first-child:last-child} or {@code :nth-child(1):nth-last-child(1)}, but with
	 * a lower specificity.
	 */
	class ONLY_CHILD extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return (node.getSiblingElementIndex() == 0) && (node.getParentNode().getChildElementsCount() == 1);
		}
	}

	/**
	 * Same as {@code :nth-of-type(1)}. Represents an element that is the first sibling of its
	 * type in the list of children of its parent element.
	 */
	class FIRST_OF_TYPE extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getSiblingNameIndex() == 0;
		}
	}

	/**
	 * Same as {@code :nth-last-of-type(1)}. Represents an element that is the last sibling of its
	 * type in the list of children of its parent element.
	 */
	class LAST_OF_TYPE extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getNextSiblingName() == null;
		}
	}

	/**
	 * Represents an element that is the root of the document.
	 * In HTML 4, this is always the HTML element.
	 */
	class ROOT extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getParentNode().getNodeType() == Node.NodeType.DOCUMENT;
		}
	}

	/**
	 * Represents an element that has no children at all.
	 */
	class EMPTY extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getChildNodesCount() == 0;
		}
	}

	/**
	 * Represents an element that has a parent element and whose parent
	 * element has no other element children with the same expanded element
	 * name. Same as {@code :first-of-type:last-of-type} or
	 * {@code :nth-of-type(1):nth-last-of-type(1)}, but with a lower specificity.
	 */
	class ONLY_OF_TYPE extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return (node.getSiblingNameIndex() == 0) && (node.getNextSiblingName() == null);
		}
	}

	// ---------------------------------------------------------------- EXTENDED PSEUDO CLASSES

	/**
	 * Selects the first matched element.
	 */
	class FIRST extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index) {
			if (matchedResults.isEmpty()) {
				return false;
			}
			final Node firstNode = matchedResults.get(0);    // getFirst();
			if (firstNode == null) {
				return false;
			}
			return firstNode == node;
		}
	}

	/**
	 * Selects the last matched element. Note that {@code :last} selects
	 * a single element by filtering the current collection and matching the
	 * last element within it.
	 */
	class LAST extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index) {
			final int size = matchedResults.size();
			if (size == 0) {
				return false;
			}
			final Node lastNode = matchedResults.get(size - 1); // getLast();
			if (lastNode == null) {
				return false;
			}
			return lastNode == node;
		}
	}

	/**
	 * Selects all button elements and elements of type button.
	 */
	class BUTTON extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("button");
		}
	}

	/**
	 * Selects all elements of type checkbox.
	 */
	class CHECKBOX extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("checkbox");
		}
	}

	/**
	 * Selects all elements of type file.
	 */
	class FILE extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("file");
		}
	}

	/**
	 * Selects all elements that are headers, like h1, h2, h3 and so on.
	 */
	class HEADER extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String name = node.getNodeName();
			if (name == null) {
				return false;
			}
			if (name.length() != 2) {
				return false;
			}
			final char c1 = name.charAt(0);
			if (c1 != 'h' && c1 != 'H') {
				return false;
			}
			final int c2 = name.charAt(1) - '0';
			return c2 >= 1 && c2 <= 6;
		}
	}

	/**
	 * Selects all elements of type image.
	 */
	class IMAGE extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("image");
		}
	}

	/**
	 * Selects all input, textarea, select and button elements.
	 */
	class INPUT extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String tagName = node.getNodeName();
			if (tagName == null) {
				return false;
			}
			if (tagName.equals("button")) {
				return true;
			}
			if (tagName.equals("input")) {
				return true;
			}
			if (tagName.equals("select")) {
				return true;
			}
			//noinspection RedundantIfStatement
			if (tagName.equals("textarea")) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Select all elements that are the parent of another element, including text nodes.
	 * This is the inverse of {@code :empty}.
	 */
	class PARENT extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.getChildNodesCount() != 0;
		}
	}

	/**
	 * Selects all elements of type password.
	 */
	class PASSWORD extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("password");
		}
	}

	/**
	 * Selects all elements of type radio.
	 */
	class RADIO extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("radio");
		}
	}

	/**
	 * Selects all elements of type reset.
	 */
	class RESET extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("reset");
		}
	}

	/**
	 * Selects all elements that are selected.
	 */
	class SELECTED extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.hasAttribute("selected");
		}
	}

	/**
	 * Selects all elements that are checked.
	 */
	class CHECKED extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.hasAttribute("checked");
		}
	}

	/**
	 * Selects all elements of type submit.
	 */
	class SUBMIT extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("submit");
		}
	}

	/**
	 * Selects all elements of type text.
	 */
	class TEXT extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			final String type = node.getAttribute("type");
			if (type == null) {
				return false;
			}
			return type.equals("text");
		}
	}

	/**
	 * Selects even elements, zero-indexed.
	 */
	class EVEN extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index) {
			return index % 2 == 0;
		}
	}

	/**
	 * Selects odd elements, zero-indexed.
	 */
	class ODD extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return true;
		}

		@Override
		public boolean matchInRange(final List<Node> matchedResults, final Node node, final int index) {
			return index % 2 != 0;
		}
	}


	// ---------------------------------------------------------------- interface

}
