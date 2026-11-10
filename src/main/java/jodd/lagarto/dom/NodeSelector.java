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

package jodd.lagarto.dom;

import jodd.csselly.CSSelly;
import jodd.csselly.Combinator;
import jodd.csselly.CssSelector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Node selector selects DOM nodes using {@link CSSelly CSS3 selectors}.
 * Group of queries are supported.
 */
public class NodeSelector {

	protected final Node rootNode;

	public NodeSelector(final Node rootNode) {
		this.rootNode = rootNode;
	}

	// ---------------------------------------------------------------- selector

	/**
	 * Selects nodes using CSS3 selector query.
	 */
	public List<Node> select(final String query) {
		Collection<List<CssSelector>> selectorsCollection = CSSelly.parse(query);
		return select(selectorsCollection);
	}

	/**
	 * Selected nodes using pre-parsed CSS selectors.
	 * When there's more than one selector, results are returned in document order.
	 */
	public List<Node> select(final Collection<List<CssSelector>> selectorsCollection) {
		List<Node> results = new ArrayList<>();
		for (List<CssSelector> selectors : selectorsCollection) {
			processSelectors(results, selectors);
		}
		if (selectorsCollection.size() > 1) {
			sortInDocumentOrder(results);
		}
		return results;
	}

	/**
	 * Sorts nodes in document order.
	 */
	protected void sortInDocumentOrder(final List<Node> nodes) {
		if (nodes.size() < 2) {
			return;
		}
		final Map<Node, int[]> paths = new IdentityHashMap<>(nodes.size());
		for (Node node : nodes) {
			paths.put(node, documentPath(node));
		}
		nodes.sort((n1, n2) -> {
			final int[] p1 = paths.get(n1);
			final int[] p2 = paths.get(n2);
			final int length = Math.min(p1.length, p2.length);
			for (int i = 0; i < length; i++) {
				if (p1[i] != p2[i]) {
					return Integer.compare(p1[i], p2[i]);
				}
			}
			// ancestors come first
			return Integer.compare(p1.length, p2.length);
		});
	}

	private static int[] documentPath(Node node) {
		int depth = 0;
		for (Node n = node; n.getParentNode() != null; n = n.getParentNode()) {
			depth++;
		}
		final int[] path = new int[depth];
		for (Node n = node; n.getParentNode() != null; n = n.getParentNode()) {
			path[--depth] = n.getSiblingIndex();
		}
		return path;
	}

	/**
	 * Process selectors and keep adding results.
	 */
	protected void processSelectors(final List<Node> results, final List<CssSelector> selectors) {
		List<Node> selectedNodes = select(rootNode, selectors);

		for (Node selectedNode : selectedNodes) {
			if (!results.contains(selectedNode)) {
				results.add(selectedNode);
			}
		}
	}

	/**
	 * Selects nodes using CSS3 selector query and returns the very first one.
	 */
	public Node selectFirst(final String query) {
		List<Node> selectedNodes = select(query);
		if (selectedNodes.isEmpty()) {
			return null;
		}
		return selectedNodes.get(0);
	}

	/**
	 * Selects nodes using {@link NodeFilter node filter}.
	 */
	public List<Node> select(final NodeFilter nodeFilter) {
		List<Node> nodes = new ArrayList<>();
		walk(rootNode, nodeFilter, nodes);
		return nodes;
	}

	/**
	 * Selects nodes using {@link NodeFilter node filter} and return the very first one.
	 */
	public Node selectFirst(final NodeFilter nodeFilter) {
		List<Node> selectedNodes = select(nodeFilter);
		if (selectedNodes.isEmpty()) {
			return null;
		}
		return selectedNodes.get(0);
	}

	// ---------------------------------------------------------------- internal

	protected void walk(final Node rootNode, final NodeFilter nodeFilter, final List<Node> result) {
		int childCount = rootNode.getChildNodesCount();
		for (int i = 0; i < childCount; i++) {
			Node node = rootNode.getChild(i);
			if (nodeFilter.accept(node)) {
				result.add(node);
			}
			walk(node, nodeFilter, result);
		}
	}

	protected List<Node> select(final Node rootNode, final List<CssSelector> selectors) {

		// start with the root node
		List<Node> nodes = new ArrayList<>();

		// a leading scope selector, explicit (:scope) or implicit (relative selector, eg "> p"), is the scoping root:
		// the root node itself, or the root element when selecting from a document, like :root
		int start = 0;
		final CssSelector first = selectors.get(0);
		if (first.isScope()) {
			final Node scopeNode = rootNode.getNodeType() == Node.NodeType.DOCUMENT ? rootNode.getFirstChildElement() : rootNode;
			if (scopeNode == null || (first.hasConditions() && !first.accept(scopeNode))) {
				return new ArrayList<>();
			}
			if (selectors.size() == 1) {
				// only the descendants of the root node are selected
				if (scopeNode != rootNode) {
					nodes.add(scopeNode);
				}
				return nodes;
			}
			nodes.add(scopeNode);
			start = 1;
		} else {
			nodes.add(rootNode);
		}

		// iterate all selectors
		for (int i = start; i < selectors.size(); i++) {
			final CssSelector cssSelector = selectors.get(i);

			// create new set of results for current css selector
			List<Node> selectedNodes = new ArrayList<>();
			for (Node node : nodes) {
				walk(node, cssSelector, selectedNodes);
			}

			// post-processing: filter out the results
			List<Node> resultNodes = new ArrayList<>();
			int index = 0;
			for (Node node : selectedNodes) {
				boolean match = filter(selectedNodes, node, cssSelector, index);
				if (match) {
					resultNodes.add(node);
				}
				index++;
			}

			// continue with results
			nodes = resultNodes;
		}

		return nodes;
	}

	/**
	 * Walks over the child notes, maintaining the tree order and not using recursion.
	 */
	protected void walkDescendantsIteratively(final LinkedList<Node> nodes, final CssSelector cssSelector, final List<Node> result) {
		while (!nodes.isEmpty()) {
			Node node = nodes.removeFirst();
			selectAndAdd(node, cssSelector, result);

			// append children in walking order to be processed right after this node
			int childCount = node.getChildNodesCount();
			for (int i = childCount - 1; i >= 0; i--) {
				nodes.addFirst(node.getChild(i));
			}
		}
	}

	/**
	 * Finds nodes in the tree that matches single selector.
	 */
	protected void walk(final Node rootNode, final CssSelector cssSelector, final List<Node> result) {

		// previous combinator determines the behavior
		CssSelector previousCssSelector = cssSelector.getPrevCssSelector();

		Combinator combinator = previousCssSelector != null ?
				previousCssSelector.getCombinator() :
				Combinator.DESCENDANT;

		switch (combinator) {
			case DESCENDANT:
				LinkedList<Node> nodes = new LinkedList<>();
				int childCount = rootNode.getChildNodesCount();
				for (int i = 0; i < childCount; i++) {
					nodes.add(rootNode.getChild(i));
					// recursive
//					selectAndAdd(node, cssSelector, result);
//					walk(node, cssSelector, result);
				}
				walkDescendantsIteratively(nodes, cssSelector, result);
				break;
			case CHILD:
				childCount = rootNode.getChildNodesCount();
				for (int i = 0; i < childCount; i++) {
					Node node = rootNode.getChild(i);
					selectAndAdd(node, cssSelector, result);
				}
				break;
			case ADJACENT_SIBLING:
				Node node = rootNode.getNextSiblingElement();
				if (node != null) {
					selectAndAdd(node, cssSelector, result);
				}
				break;
			case GENERAL_SIBLING:
				node = rootNode;
				while (true) {
					node = node.getNextSiblingElement();
					if (node == null) {
						break;
					}
					selectAndAdd(node, cssSelector, result);
				}
				break;
		}	}

	/**
	 * Selects single node for single selector and appends it to the results.
	 */
	protected void selectAndAdd(final Node node, final CssSelector cssSelector, final List<Node> result) {
		// ignore all nodes that are not elements
		if (node.getNodeType() != Node.NodeType.ELEMENT) {
			return;
		}
		boolean matched = cssSelector.accept(node);
		if (matched) {
			// check for duplicates
			if (result.contains(node)) {
				return;
			}
			// no duplicate found, add it to the results
			result.add(node);
		}
	}

	/**
	 * Filter nodes.
	 */
	protected boolean filter(final List<Node> currentResults, final Node node, final CssSelector cssSelector, final int index) {
		return cssSelector.accept(currentResults, node, index);
	}

}