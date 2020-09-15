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

package jodd.jerry;

import jodd.lagarto.dom.DOMBuilder;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeSelector;
import jodd.lagarto.dom.NodeUtil;
import jodd.lagarto.dom.Text;
import jodd.util.ArraysUtil;
import jodd.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Jerry is JQuery in Java.
 */
@SuppressWarnings("MethodNamesDifferingOnlyByCase")
public class Jerry implements Iterable<Jerry> {
	private static final String EMPTY = "";

	@SuppressWarnings("CloneableClassWithoutClone")
	private static class NodeList extends ArrayList<Node> {

		private NodeList(final int initialCapacity) {
			super(initialCapacity);
		}

		private NodeList() {
		}

		@Override
		public boolean add(final Node o) {
			for (final Node node : this) {
				if (node == o) {
					return false;
				}
			}
			return super.add(o);
		}
	}

	// ---------------------------------------------------------------- create

	/**
	 * Parses input sequence and creates new {@code Jerry}.
	 */
	public static Jerry of(final char[] content) {
		return create().parse(content);
	}

	/**
	 * Parses input content and creates new {@code Jerry}.
	 */
	public static Jerry of(final CharSequence content) {
		return create().parse(content);
	}

	// ---------------------------------------------------------------- 2-steps init

	/**
	 * Just creates new {@link JerryParser Jerry runner} to separate
	 * parser creation and creation of new Jerry instances.
	 */
	public static JerryParser create() {
		return new JerryParser();
	}

	/**
	 * Creates new {@link JerryParser Jerry runner} with
	 * provided implementation of {@link jodd.lagarto.dom.DOMBuilder}.
	 */
	public static JerryParser create(final DOMBuilder domBuilder) {
		return new JerryParser(domBuilder);
	}

	// ---------------------------------------------------------------- ctor

	protected final Jerry parent;
	protected final Node[] nodes;
	protected final DOMBuilder builder;

	/**
	 * Creates root Jerry.
	 */
	protected Jerry(final DOMBuilder builder, final Node... nodes) {
		this.parent = null;
		this.nodes = nodes;
		this.builder = builder;
	}

	/**
	 * Creates child Jerry.
	 */
	protected Jerry(final Jerry parent, final Node... nodes) {
		this.parent = parent;
		this.nodes = nodes;
		this.builder = parent.builder;
	}

	/**
	 * Creates child Jerry.
	 */
	protected Jerry(final Jerry parent, final Node[] nodes1, final Node[] nodes2) {
		this.parent = parent;
		this.nodes = NodeUtil.join(nodes1, nodes2);
		this.builder = parent.builder;
	}

	/**
	 * Creates child Jerry.
	 */
	protected Jerry(final Jerry parent, final List<Node> nodeList) {
		this(parent, nodeList.toArray(new Node[0]));
	}

	// ---------------------------------------------------------------- this

	/**
	 * Returns number of nodes in this Jerry.
	 */
	public int length() {
		return nodes.length;
	}

	/**
	 * Returns number of nodes in this Jerry.
	 */
	public int size() {
		return nodes.length;
	}

	/**
	 * Returns node at given index. Returns {@code null}
	 * if index is out of bounds.
	 */
	public Node get(final int index) {
		if ((index < 0) || (index >= nodes.length)) {
			return null;
		}
		return nodes[index];
	}

	/**
	 * Retrieve all DOM elements matched by this set.
	 * Warning: returned array is not a clone!
	 */
	public Node[] get() {
		return nodes;
	}

	/**
	 * Searches for a given {@code Node} from among the matched elements.
	 */
	public int index(final Node element) {
		if (nodes.length == 0) {
			return -1;
		}

		int index = 0;
		for (final Node node : nodes) {
			if (node == element) {
				return index;
			}
			index++;
		}
		return -1;
	}

	// ---------------------------------------------------------------- Traversing

	/**
	 * Gets the immediate children of each element in the set of matched elements.
	 */
	public Jerry children() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				final Node[] children = node.getChildElements();

				Collections.addAll(result, children);
			}
		}
		return new Jerry(this, result);
	}

	/**
	* Get the children of each element in the set of matched elements, 
	* including text and comment nodes.
	*/
	public Jerry contents() {
		final List<Node> result = new NodeList(nodes.length);
		if (nodes.length > 0) {
			for (final Node node : nodes) {
				final Node[] contents = node.getChildNodes();
				Collections.addAll(result, contents);
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Gets the parent of each element in the current set of matched elements.
	 */
	public Jerry parent() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				result.add(node.getParentNode());
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Gets the siblings of each element in the set of matched elements.
	 */
	public Jerry siblings() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				final Node[] allElements = node.getParentNode().getChildElements();
				for (final Node sibling : allElements) {
					if (sibling != node) {
						result.add(sibling);
					}
				}
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Gets the immediately following sibling of each element in the
	 * set of matched elements.
	 */
	public Jerry next() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				result.add(node.getNextSiblingElement());
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Get all following siblings of each element in the set of matched 
	 * elements, optionally filtered by a selector.
	 */
	public Jerry nextAll() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				Node currentSiblingElement = node.getNextSiblingElement();
				while (currentSiblingElement != null) {
					result.add(currentSiblingElement);
					currentSiblingElement = currentSiblingElement.getNextSiblingElement();
				}
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Gets the immediately preceding sibling of each element in the
	 * set of matched elements.
	 */
	public Jerry prev() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				result.add(node.getPreviousSiblingElement());
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Get all preceding siblings of each element in the set of matched 
	 * elements, optionally filtered by a selector.
	 */
	public Jerry prevAll() {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				Node currentSiblingElement = node.getPreviousSiblingElement();
				while (currentSiblingElement != null) {
					result.add(currentSiblingElement);
					currentSiblingElement = currentSiblingElement.getPreviousSiblingElement();
				}
			}
		}
		return new Jerry(this, result);
	}

	/**
	 *  Gets the descendants of each element in the current set of matched elements,
	 *  filtered by a selector.
	 */
	public Jerry find(final String cssSelector) {
		final List<Node> result = new NodeList();

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				final NodeSelector nodeSelector = createNodeSelector(node);
				final List<Node> filteredNodes = nodeSelector.select(cssSelector);
				result.addAll(filteredNodes);
			}
		}

		return new Jerry(this, result);
	}

	/**
	 * Selects nodes.
	 *
	 * @see #find(String)
	 */
	public Jerry s(final String cssSelector) {
		return find(cssSelector);
	}

	/**
	 * Creates node selector.
	 */
	protected NodeSelector createNodeSelector(final Node node) {
		return new NodeSelector(node);
	}

	/**
	 * Iterates over a jQuery object, executing a function for
	 * each matched element.
	 * @see #eachNode(JerryNodeFunction)
	 */
	public Jerry each(final JerryFunction function) {
		for (int i = 0; i < nodes.length; i++) {
			final Node node = nodes[i];
			final Jerry $this = new Jerry(this, node);
			final Boolean result = function.onNode($this, i);
			if (result == Boolean.FALSE) {
				break;
			}
		}
		return this;
	}

	/**
	 * Iterates over a jQuery object, executing a function for
	 * each matched element.
	 * @see #each(JerryFunction)
	 */
	public Jerry eachNode(final JerryNodeFunction function) {
		for (int i = 0; i < nodes.length; i++) {
			final Node node = nodes[i];
			if (!function.onNode(node, i)) {
				break;
			}
		}
		return this;
	}

	// ---------------------------------------------------------------- Miscellaneous Traversing

	/**
	 * Adds elements to the set of matched elements.
	 */
	public Jerry add(final String selector) {
		return new Jerry(this, nodes, root().find(selector).nodes);
	}

	/**
	 * Ends the most recent filtering operation in the current chain
	 * and returns the set of matched elements to its previous state.
	 */
	public Jerry end() {
		return parent;
	}

	/**
	 * Removes elements from the set of matched elements.
	 */
	public Jerry not(final String cssSelector) {
		final Node[]  notNodes = root().find(cssSelector).nodes;
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				if (!ArraysUtil.contains(notNodes, node)) {
					result.add(node);
				}
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Returns root Jerry.
	 */
	public Jerry root() {
		Jerry jerry = this.parent;
		if (jerry == null) {
			return this;
		}
		while (jerry.parent != null) {
			jerry = jerry.parent;
		}
		return jerry;
	}


	// ---------------------------------------------------------------- Filtering

	/**
	 * Reduces the set of matched elements to the first in the set.
	 */
	public Jerry first() {
		final List<Node> result = new NodeList(nodes.length);
		if (nodes.length > 0) {
			result.add(nodes[0]);
		}
		return new Jerry(this, result);
	}

	/**
	 * Reduces the set of matched elements to the last in the set.
	 */
	public Jerry last() {
		final List<Node> result = new NodeList(nodes.length);
		if (nodes.length > 0) {
			result.add(nodes[nodes.length - 1]);
		}
		return new Jerry(this, result);
	}

	/**
	 * Reduces the set of matched elements to the one at the specified index.
	 */
	public Jerry eq(final int value) {
		final List<Node> result = new NodeList(1);
		final int matchingIndex = value >= 0 ? value : nodes.length + value;

		if (nodes.length > 0) {
			int index = 0;
			for (final Node node : nodes) {
				if (index == matchingIndex) {
					result.add(node);
					break;
				}
				index++;
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Reduces the set of matched elements to the one at an index greater
	 * than specified index.
	 */
	public Jerry gt(final int value) {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			int index = 0;

			for (final Node node : nodes) {
				if (index > value) {
					result.add(node);
				}
				index++;
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Reduces the set of matched elements to the one at an index less
	 * than specified index.
	 */
	public Jerry lt(final int value) {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			int index = 0;
			for (final Node node : nodes) {
				if (index < value) {
					result.add(node);
				}
				index++;
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Checks the current matched set of elements against a selector and
	 * return {@code true} if at least one of these elements matches
	 * the given arguments.
	 */
	public boolean is(final String cssSelectors) {
		if (nodes.length == 0) {
			return false;
		}

		for (final Node node : nodes) {
			final Node parentNode = node.getParentNode();
			if (parentNode == null) {
				continue;
			}

			final NodeSelector nodeSelector = createNodeSelector(parentNode);
			final List<Node> selectedNodes = nodeSelector.select(cssSelectors);

			for (final Node selected : selectedNodes) {
				if (node == selected) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Reduces the set of matched elements to those that match the selector.
	 */
	public Jerry filter(final String cssSelectors) {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				final Node parentNode = node.getParentNode();
				if (parentNode == null) {
					continue;
				}

				final NodeSelector nodeSelector = createNodeSelector(parentNode);
				final List<Node> selectedNodes = nodeSelector.select(cssSelectors);

				for (final Node selected : selectedNodes) {
					if (node == selected) {
						result.add(node);
					}
				}
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Reduces the set of matched elements to those that pass the
	 * {@link JerryFunction function's} test.
	 */
	public Jerry filter(final JerryFunction jerryFunction) {
		final List<Node> result = new NodeList(nodes.length);

		for (int i = 0; i < nodes.length; i++) {
			final Node node = nodes[i];
			final Node parentNode = node.getParentNode();
			if (parentNode == null) {
				continue;
			}

			final Jerry $this = new Jerry(this, node);

			final boolean accept = jerryFunction.onNode($this, i);

			if (accept) {
				result.add(node);
			}
		}
		return new Jerry(this, result);
	}

	/**
	 * Reduce the set of matched elements to those that have a descendant that
	 * matches the selector or DOM element.
	 */
	public Jerry has(final String cssSelectors) {
		final List<Node> result = new NodeList(nodes.length);

		if (nodes.length > 0) {
			for (final Node node : nodes) {
				final NodeSelector nodeSelector = createNodeSelector(node);
				final List<Node> selectedNodes = nodeSelector.select(cssSelectors);

				if (!selectedNodes.isEmpty()) {
					result.add(node);
				}
			}
		}

		return new Jerry(this, result);
	}

	// ---------------------------------------------------------------- Attributes

	/**
	 * Gets the value of an attribute for the first element in the set of matched elements.
	 * Returns {@code null} if set is empty.
	 */
	public String attr(final String name) {
		if (nodes.length == 0) {
			return null;
		}
		if (name == null) {
			return null;
		}
		return nodes[0].getAttribute(name);
	}

	/**
	 * Sets one or more attributes for the set of matched elements.
	 */
	public Jerry attr(final String name, final String value) {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			node.setAttribute(name, value);
		}
		return this;
	}

	/**
	 * Removes an attribute from each element in the set of matched elements.
	 */
	public Jerry removeAttr(final String name) {
		if (name == null) {
			return this;
		}
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			node.removeAttribute(name);
		}
		return this;
	}

	/**
	 * Gets the value of a style property for the first element
	 * in the set of matched elements. Returns {@code null}
	 * if set is empty.
	 */
	public String css(String propertyName) {
		if (nodes.length == 0) {
			return null;
		}

		propertyName = StringUtil.fromCamelCase(propertyName, '-');

		final String styleAttrValue = nodes[0].getAttribute("style");
		if (styleAttrValue == null) {
			return null;
		}

		final Map<String, String> styles = createPropertiesMap(styleAttrValue, ';', ':');
		return styles.get(propertyName);
	}

	/**
	 * Sets one or more CSS properties for the set of matched elements.
	 * By passing an empty value, that property will be removed.
	 * Note that this is different from jQuery, where this means
	 * that property will be reset to previous value if existed.
	 */
	public Jerry css(String propertyName, final String value) {
		propertyName = StringUtil.fromCamelCase(propertyName, '-');

		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			String styleAttrValue = node.getAttribute("style");
			final Map<String, String> styles = createPropertiesMap(styleAttrValue, ';', ':');
			if (value.length() == 0) {
				styles.remove(propertyName);
			} else {
				styles.put(propertyName, value);
			}

			styleAttrValue = generateAttributeValue(styles, ';', ':');
			node.setAttribute("style", styleAttrValue);
		}
		return this;
	}

	/**
	 * Sets one or more CSS properties for the set of matched elements.
	 */
	public Jerry css(final String... css) {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			String styleAttrValue = node.getAttribute("style");
			final Map<String, String> styles = createPropertiesMap(styleAttrValue, ';', ':');

			for (int i = 0; i < css.length; i += 2) {
				String propertyName = css[i];
				propertyName = StringUtil.fromCamelCase(propertyName, '-');
				final String value = css[i + 1];
				if (value.length() == 0) {
					styles.remove(propertyName);
				} else {
					styles.put(propertyName, value);
				}
			}
			styleAttrValue = generateAttributeValue(styles, ';', ':');
			node.setAttribute("style", styleAttrValue);
		}
		return this;
	}

	/**
	 * Adds the specified class(es) to each of the set of matched elements.
	 */
	public Jerry addClass(final String... classNames) {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final String attrClass = node.getAttribute("class");
			final Set<String> classes = createPropertiesSet(attrClass, ' ');
			boolean wasChange = false;
			for (final String className : classNames) {
				if (classes.add(className)) {
					wasChange = true;
				}
			}
			if (wasChange) {
				final String attrValue = generateAttributeValue(classes, ' ');
				node.setAttribute("class", attrValue);
			}
		}
		return this;
	}

	/**
	 * Determines whether any of the matched elements are assigned the given class.
	 */
	public boolean hasClass(final String... classNames) {
		if (nodes.length == 0) {
			return false;
		}
		for (final Node node : nodes) {
			final String attrClass = node.getAttribute("class");
			final Set<String> classes = createPropertiesSet(attrClass, ' ');
			for (final String className : classNames) {
				if (classes.contains(className)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Removes a single class, multiple classes, or all classes
	 * from each element in the set of matched elements.
	 */
	public Jerry removeClass(final String... classNames) {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final String attrClass = node.getAttribute("class");
			final Set<String> classes = createPropertiesSet(attrClass, ' ');
			boolean wasChange = false;
			for (final String className : classNames) {
				if (classes.remove(className)) {
					wasChange = true;
				}
			}
			if (wasChange) {
				final String attrValue = generateAttributeValue(classes, ' ');
				node.setAttribute("class", attrValue);
			}
		}
		return this;
	}

	/**
	 * Adds or remove one or more classes from each element in the set of
	 * matched elements, depending on either the class's presence or
	 * the value of the switch argument.
	 */
	public Jerry toggleClass(final String... classNames) {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final String attrClass = node.getAttribute("class");
			final Set<String> classes = createPropertiesSet(attrClass, ' ');
			for (final String className : classNames) {
				if (classes.contains(className)) {
					classes.remove(className);
				} else {
					classes.add(className);
				}
			}
			final String attrValue = generateAttributeValue(classes, ' ');
			node.setAttribute("class", attrValue);
		}
		return this;
	}

	// ---------------------------------------------------------------- content

	/**
	 * Gets the combined text contents of each element in the set of
	 * matched elements, including their descendants.
	 * Text is HTML decoded for text nodes.
	 */
	public String text() {
		if (nodes.length == 0) {
			return EMPTY;
		}

		final StringBuilder sb = new StringBuilder();
		for (final Node node : nodes) {
			sb.append(node.getTextContent());
		}
		return sb.toString();
	}

	/**
	 * Sets the content of each element in the set of matched elements to the specified text.
	 */
	public Jerry text(String text) {
		if (nodes.length == 0) {
			return this;
		}
		if (text == null) {
			text = EMPTY;
		}
		for (final Node node : nodes) {
			node.removeAllChilds();
			final Text textNode = new Text(node.getOwnerDocument(), text);
			node.addChild(textNode);
		}
		return this;
	}

	/**
	 * Gets the HTML contents of the first element in the set of matched elements.
	 * Content is raw, not HTML decoded.
	 * @see #htmlAll(boolean)
	 */
	public String html() {
		if (nodes.length == 0) {
			return null;
		}
		return nodes[0].getInnerHtml();
	}

	/**
	 * Gets the combined HTML contents of each element in the set of
	 * matched elements, including their descendants.
	 * @see #html()
	 * @param setIncluded if {@code true} than sets node are included in the output
	 */
	public String htmlAll(final boolean setIncluded) {
		if (nodes.length == 0) {
			return EMPTY;
		}
		final StringBuilder sb = new StringBuilder();
		for (final Node node : nodes) {
			sb.append(setIncluded ? node.getHtml() : node.getInnerHtml());
		}
		return sb.toString();
	}

	/**
	 * Sets the HTML contents of each element in the set of matched elements.
	 */
	public Jerry html(String html) {
		if (html == null) {
			html = EMPTY;
		}

		final Document doc = builder.parse(html);

		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			node.removeAllChilds();

			// clone to preserve for next iteration
			// as nodes will be detached from parent
			final Document workingDoc = doc.clone();

			node.addChild(workingDoc.getChildNodes());
		}
		return this;
	}
	
	// ---------------------------------------------------------------- DOM

	/**
	 * Inserts content, specified by the parameter, to the end of each
	 * element in the set of matched elements.
	 */
	public Jerry append(String html) {
		if (html == null) {
			html = EMPTY;
		}
		final Document doc = builder.parse(html);

		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Document workingDoc = doc.clone();
			node.addChild(workingDoc.getChildNodes());
		}
		return this;
	}

	/**
	 * Insert content, specified by the parameter, to the beginning of each 
	 * element in the set of matched elements.
	 */
	public Jerry prepend(String html) {
		if (html == null) {
			html = EMPTY;
		}
		final Document doc = builder.parse(html);

		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Document workingDoc = doc.clone();
			node.insertChild(workingDoc.getChildNodes(), 0);
		}
		return this;
	}

	/**
	 * Inserts content, specified by the parameter, before each
	 * element in the set of matched elements.
	 */
	public Jerry before(String html) {
		if (html == null) {
			html = EMPTY;
		}
		final Document doc = builder.parse(html);
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Document workingDoc = doc.clone();
			node.insertBefore(workingDoc.getChildNodes(), node);
		}
		return this;
	}

	/**
	 * Inserts content, specified by the parameter, after each
	 * element in the set of matched elements.
	 */
	public Jerry after(String html) {
		if (html == null) {
			html = EMPTY;
		}
		final Document doc = builder.parse(html);
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Document workingDoc = doc.clone();
			node.insertAfter(workingDoc.getChildNodes(), node);
		}
		return this;
	}

	/**
	 * Replace each element in the set of matched elements with the provided 
	 * new content and return the set of elements that was removed.
	 */
	public Jerry replaceWith(String html) {
 		if (html == null) {
		    html = EMPTY;
	    }
		final Document doc = builder.parse(html);

		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Node parent = node.getParentNode();
			// if a node already is the root element, don't unwrap
			if (parent == null) {
				continue;
			}

			// replace, if possible
			final Document workingDoc = doc.clone();
			final int index = node.getSiblingIndex();
			parent.insertChild(workingDoc.getFirstChild(), index);
			node.detachFromParent();
		}

		return this;
	}

	/**
	 * Removes the set of matched elements from the DOM.
	 */
	public Jerry remove() {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			node.detachFromParent();
		}
		return this;
	}

	/**
	 * Removes the set of matched elements from the DOM.
	 * Identical to {@link #remove()}.
	 */
	public Jerry detach() {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			node.detachFromParent();
		}
		return this;
	}

	/**
	 * Removes all child nodes of the set of matched elements from the DOM.
	 */
	public Jerry empty() {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			node.removeAllChilds();
		}
		return this;
	}

	// ---------------------------------------------------------------- wrap

	/**
	 * Wraps an HTML structure around each element in the set of matched elements.
	 * Returns the original set of elements for chaining purposes.
	 */
	public Jerry wrap(String html) {
		if (html == null) {
			html = EMPTY;
		}
		final Document doc = builder.parse(html);

		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Document workingDoc = doc.clone();
			Node inmostNode = workingDoc;
			while (inmostNode.hasChildNodes()) {
				inmostNode = inmostNode.getFirstChild();
			}

			// replace
			final Node parent = node.getParentNode();
			final int index = node.getSiblingIndex();
			inmostNode.addChild(node);
			parent.insertChild(workingDoc.getFirstChild(), index);
		}

		return this;
	}

	/**
	 * Remove the parents of the set of matched elements from the DOM, leaving 
	 * the matched elements (and siblings, if any) in their place. 
	 */
	public Jerry unwrap() {
		if (nodes.length == 0) {
			return this;
		}
		for (final Node node : nodes) {
			final Node parent = node.getParentNode();
			// if a node already is the root element, don't unwrap
			if (parent == null) {
				continue;
			}

			// replace, if possible
			final Node grandparent = parent.getParentNode();
			if (grandparent == null) {
				continue;
			}

			final Node[] siblings = parent.getChildNodes();
			final int index = parent.getSiblingIndex();
			grandparent.insertChild(siblings, index);
			parent.detachFromParent();
		}

		return this;
	}

	// ---------------------------------------------------------------- iterator

	/**
	 * Returns iterator over nodes contained in the Jerry object.
	 * Each node is wrapped. Similar to {@link #each(JerryFunction)}.
	 */
	@Override
	public Iterator<Jerry> iterator() {
		final Jerry jerry = this;

		return new Iterator<Jerry>() {
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < jerry.nodes.length;
			}

			@Override
			public Jerry next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				final Jerry nextJerry = new Jerry(jerry, jerry.get(index));
				index++;
				return nextJerry;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	// ---------------------------------------------------------------- form

	/**
	 * Processes all forms, collects all form parameters and calls back the
	 * {@link JerryFormHandler}.
	 */
	public Jerry form(final String formCssSelector, final JerryFormHandler jerryFormHandler) {
		final Jerry form = find(formCssSelector);

		// process each form
		for (final Node node : form.nodes) {
			final Jerry singleForm = new Jerry(this, node);

			final Map<String, String[]> parameters = new HashMap<>();

			// process all input elements

			singleForm.s("input").each(($inputTag, index) -> {

				String type = $inputTag.attr("type");

				// An input element with no type attribute specified represents
				// the same thing as an input element with its type attribute set to "text".

				if (type == null) {
					type = "text";
				}

				final boolean isCheckbox = type.equals("checkbox");
				final boolean isRadio = type.equals("radio");

				if (isRadio || isCheckbox) {
					if (!($inputTag.nodes[0].hasAttribute("checked"))) {
						return true;
					}
				}

				final String name = $inputTag.attr("name");
				if (name == null) {
					return true;
				}

				String tagValue = $inputTag.attr("value");

				if (tagValue == null) {
					if (isCheckbox) {
						tagValue = "on";
					}
				}

				// add tag value
				String[] value = parameters.get(name);

				if (value == null) {
					value = new String[] {tagValue};
				} else {
					value = ArraysUtil.append(value, tagValue);
				}

				parameters.put(name, value);
				return true;
			});

			// process all select elements

			singleForm.s("select").each(($selectTag, index) -> {
				final String name = $selectTag.attr("name");

				$selectTag.s("option").each(($optionTag, index1) -> {
					if ($optionTag.nodes[0].hasAttribute("selected")) {
						final String tagValue = $optionTag.attr("value");

						// add tag value
						String[] value = parameters.get(name);

						if (value == null) {
							value = new String[] {tagValue};
						} else {
							value = ArraysUtil.append(value, tagValue);
						}

						parameters.put(name, value);
					}
					return true;
				});

				return true;
			});

			// process all text areas

			singleForm.s("textarea").each(($textarea, index) -> {
				final String name = $textarea.attr("name");
				final String value = $textarea.text();

				parameters.put(name, new String[] {value});
				return true;
			});

			// done

			jerryFormHandler.onForm(singleForm, parameters);
		}

		return this;
	}

	// ---------------------------------------------------------------- internal

	protected Set<String> createPropertiesSet(final String attrValue, final char propertiesDelimiter) {
		if (attrValue == null) {
			return new LinkedHashSet<>();
		}
		final String[] properties = StringUtil.splitc(attrValue, propertiesDelimiter);
		final LinkedHashSet<String> set = new LinkedHashSet<>(properties.length);

		Collections.addAll(set, properties);
		return set;
	}
	
	protected String generateAttributeValue(final Set<String> set, final char propertiesDelimiter) {
		final StringBuilder sb = new StringBuilder(set.size() * 16);
		boolean first = true;
		for (final String entry : set) {
			if (!first) {
				sb.append(propertiesDelimiter);
			} else {
				first = false;
			}
			sb.append(entry);
		}
		return sb.toString();
	}
	
	protected Map<String, String> createPropertiesMap(final String attrValue, final char propertiesDelimiter, final char valueDelimiter) {
		if (attrValue == null) {
			return new LinkedHashMap<>();
		}
		final String[] properties = StringUtil.splitc(attrValue, propertiesDelimiter);
		final LinkedHashMap<String, String> map = new LinkedHashMap<>(properties.length);
		for (final String property : properties) {
			final int valueDelimiterIndex = property.indexOf(valueDelimiter);
			if (valueDelimiterIndex != -1) {
				final String propertyName = property.substring(0, valueDelimiterIndex).trim();
				final String propertyValue = property.substring(valueDelimiterIndex + 1).trim();
				map.put(propertyName, propertyValue);
			}
		}
		return map;
	}
	
	protected String generateAttributeValue(final Map<String, String> map, final char propertiesDelimiter, final char valueDelimiter) {
		final StringBuilder sb = new StringBuilder(map.size() * 32);
		for (final Map.Entry<String, String> entry : map.entrySet()) {
			sb.append(entry.getKey());
			sb.append(valueDelimiter);
			sb.append(entry.getValue());
			sb.append(propertiesDelimiter);
		}
		return sb.toString();
	}

}
