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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		if (selectorsCollection.size() > 1 && canSelectInOneWalk(selectorsCollection)) {
			// one walk over the nodes, that are matched against each selector: results are in document order
			final List<Node> results = new ArrayList<>();
			final List<ChainMatcher> matchers = new ArrayList<>(selectorsCollection.size());
			for (List<CssSelector> selectors : selectorsCollection) {
				matchers.add(new ChainMatcher(selectors, rootNode, null));
			}
			walkDescendants(rootNode, node -> {
				for (ChainMatcher matcher : matchers) {
					if (matcher.matches(node)) {
						results.add(node);
						break;
					}
				}
				return true;
			});
			return results;
		}
		List<Node> results = new UniqueNodeList();
		for (List<CssSelector> selectors : selectorsCollection) {
			processSelectors(results, selectors);
		}
		if (selectorsCollection.size() > 1) {
			sortInDocumentOrder(results);
		}
		return new ArrayList<>(results);
	}

	/**
	 * Returns {@code true} if at least one node is selected by the pre-parsed CSS selectors.
	 * Faster than {@link #select(Collection)}, as the selection stops at the first matching node.
	 */
	public boolean hasMatch(final Collection<List<CssSelector>> selectorsCollection) {
		for (List<CssSelector> selectors : selectorsCollection) {
			if (isRangeDependent(selectors)) {
				if (!select(rootNode, selectors).isEmpty()) {
					return true;
				}
			} else if (!selectFromRight(rootNode, selectors, true).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static boolean canSelectInOneWalk(final Collection<List<CssSelector>> selectorsCollection) {
		for (List<CssSelector> selectors : selectorsCollection) {
			if (selectors.get(0).isScope() || isRangeDependent(selectors)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns {@code true} if one of the selectors depends on the position of the nodes within the current results,
	 * eg: {@code :first}, and requires the selection to be performed from left to right.
	 */
	private static boolean isRangeDependent(final List<CssSelector> selectors) {
		for (CssSelector selector : selectors) {
			if (selector.isRangeDependent()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * List of nodes that are checked for duplicates before being added,
	 * {@link #contains(Object)} is fast as it doesn't scan the list.
	 */
	private static final class UniqueNodeList extends ArrayList<Node> {
		private final Set<Node> nodes = Collections.newSetFromMap(new IdentityHashMap<>());

		@Override
		public boolean add(final Node node) {
			nodes.add(node);
			return super.add(node);
		}

		@Override
		public boolean contains(final Object o) {
			return nodes.contains(o);
		}
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
		if (isRangeDependent(selectors)) {
			return selectFromLeft(rootNode, selectors);
		}
		return selectFromRight(rootNode, selectors, false);
	}

	/**
	 * Selects the nodes like browsers do: the candidate nodes are walked once, in document order,
	 * and each one is matched against the selectors, from right to left.
	 */
	private List<Node> selectFromRight(final Node rootNode, final List<CssSelector> selectors, final boolean firstOnly) {
		final List<Node> results = new ArrayList<>();

		// a leading scope selector, explicit (:scope) or implicit (relative selector, eg "> p"), is the scoping root:
		// the root node itself, or the root element when selecting from a document, like :root
		final CssSelector first = selectors.get(0);
		if (first.isScope()) {
			final Node scopeNode = rootNode.getNodeType() == Node.NodeType.DOCUMENT ? rootNode.getFirstChildElement() : rootNode;
			if (scopeNode == null || (first.hasConditions() && !first.accept(scopeNode))) {
				return results;
			}
			if (selectors.size() == 1) {
				// only the descendants of the root node are selected
				if (scopeNode != rootNode) {
					results.add(scopeNode);
				}
				return results;
			}
			final ChainMatcher matcher = new ChainMatcher(selectors, rootNode, scopeNode);
			final NodeVisitor visitor = node -> {
				if (matcher.matches(node)) {
					results.add(node);
					return !firstOnly;
				}
				return true;
			};
			final Combinator combinator = first.getCombinator();
			if (combinator == Combinator.ADJACENT_SIBLING || combinator == Combinator.GENERAL_SIBLING) {
				// the following siblings of the scoping root, and their descendants
				for (Node sibling = scopeNode.getNextSiblingElement(); sibling != null; sibling = sibling.getNextSiblingElement()) {
					if (!visitor.visit(sibling) || !walkDescendants(sibling, visitor)) {
						break;
					}
				}
			} else {
				walkDescendants(scopeNode, visitor);
			}
			return results;
		}

		final ChainMatcher matcher = new ChainMatcher(selectors, rootNode, null);
		walkDescendants(rootNode, node -> {
			if (matcher.matches(node)) {
				results.add(node);
				return !firstOnly;
			}
			return true;
		});
		return results;
	}

	@FunctionalInterface
	private interface NodeVisitor {
		/**
		 * Visits an element, returns {@code false} to stop the walk.
		 */
		boolean visit(Node element);
	}

	/**
	 * Visits the descendant elements of a node in document order, without recursion.
	 * Returns {@code false} if the walk was stopped by the visitor.
	 */
	private static boolean walkDescendants(final Node root, final NodeVisitor visitor) {
		final ArrayDeque<Node> stack = new ArrayDeque<>();
		for (int i = root.getChildNodesCount() - 1; i >= 0; i--) {
			stack.push(root.getChild(i));
		}
		while (!stack.isEmpty()) {
			final Node node = stack.pop();
			if (node.getNodeType() == Node.NodeType.ELEMENT && !visitor.visit(node)) {
				return false;
			}
			for (int i = node.getChildNodesCount() - 1; i >= 0; i--) {
				stack.push(node.getChild(i));
			}
		}
		return true;
	}

	/**
	 * Matches nodes against a selector, from right to left, honoring the combinators.
	 * The results of the intermediate matches are remembered, so the ancestors and siblings
	 * shared by several nodes are matched only once, and each sibling list is walked once for a given selector.
	 */
	private static final class ChainMatcher {
		// bits per selector index in the memo
		private static final int MATCH_KNOWN = 0;
		private static final int MATCH = 1;
		private static final int PREVIOUS_KNOWN = 2;
		private static final int PREVIOUS = 3;
		private static final int BITS = 4;

		private final List<CssSelector> selectors;
		// the matched nodes and their ancestors must be descendants of this node
		private final Node root;
		// the node that a leading scope selector matches, or null
		private final Node anchor;
		private final Map<Node, BitSet> memo = new IdentityHashMap<>();
		// the nodes walked through by matchAnyPreviousSibling, per selector index: nested calls are for lower indexes
		private final List<List<Node>> walkedNodes;

		ChainMatcher(final List<CssSelector> selectors, final Node root, final Node anchor) {
			this.selectors = selectors;
			this.root = root;
			this.anchor = anchor;
			this.walkedNodes = new ArrayList<>(selectors.size());
			for (int i = 0; i < selectors.size(); i++) {
				walkedNodes.add(new ArrayList<>());
			}
		}

		boolean matches(final Node node) {
			final int last = selectors.size() - 1;
			// the node is visited once: no need to remember the result
			return selectors.get(last).accept(node) && (last == 0 || matchPrevious(node, last - 1));
		}

		private BitSet bits(final Node node) {
			BitSet bits = memo.get(node);
			if (bits == null) {
				bits = new BitSet(BITS * selectors.size());
				memo.put(node, bits);
			}
			return bits;
		}

		/**
		 * Matches the node against the selector at given index, and its ancestors or previous siblings
		 * against the previous selectors.
		 */
		private boolean match(final Node node, final int index) {
			if (index == 0 && anchor != null) {
				return node == anchor;
			}
			if (node == root || node.getNodeType() != Node.NodeType.ELEMENT) {
				return false;
			}
			final BitSet bits = bits(node);
			if (bits.get(BITS * index + MATCH_KNOWN)) {
				return bits.get(BITS * index + MATCH);
			}
			final boolean result = selectors.get(index).accept(node) && (index == 0 || matchPrevious(node, index - 1));
			bits.set(BITS * index + MATCH_KNOWN);
			bits.set(BITS * index + MATCH, result);
			return result;
		}

		/**
		 * Matches the ancestors or previous siblings of the node against the selector at given index,
		 * depending on its combinator.
		 */
		private boolean matchPrevious(final Node node, final int prevIndex) {
			switch (selectors.get(prevIndex).getCombinator()) {
				case CHILD: {
					final Node parent = node.getParentNode();
					return parent != null && match(parent, prevIndex);
				}
				case ADJACENT_SIBLING: {
					final Node sibling = node.getPreviousSiblingElement();
					return sibling != null && match(sibling, prevIndex);
				}
				case DESCENDANT: {
					// ancestor chains are short: walked for each node, the ancestors' matches being remembered
					for (Node ancestor = node.getParentNode(); ancestor != null; ancestor = ancestor.getParentNode()) {
						if (match(ancestor, prevIndex)) {
							return true;
						}
						if (ancestor == root || ancestor == anchor) {
							// no need to look further up
							return false;
						}
					}
					return false;
				}
				case GENERAL_SIBLING:
					// sibling lists can be long: walked once
					return matchAnyPreviousSibling(node, prevIndex);
				default:
					return false;
			}
		}

		/**
		 * Returns {@code true} if one of the previous siblings of the node matches the selector at given index.
		 * The result is remembered for the node and for the siblings walked through, as they share the same
		 * previous siblings: each sibling list is walked once.
		 */
		private boolean matchAnyPreviousSibling(final Node node, final int index) {
			final List<Node> walked = walkedNodes.get(index);
			Node current = node;
			boolean result;
			while (true) {
				final BitSet bits = bits(current);
				if (bits.get(BITS * index + PREVIOUS_KNOWN)) {
					result = bits.get(BITS * index + PREVIOUS);
					break;
				}
				walked.add(current);
				final Node previous = current.getPreviousSiblingElement();
				if (previous == null) {
					result = false;
					break;
				}
				if (match(previous, index)) {
					result = true;
					break;
				}
				if (previous == anchor) {
					// no need to look further
					result = false;
					break;
				}
				current = previous;
			}
			// the siblings walked through have the same previous siblings, beyond the ones that didn't match
			for (final Node walkedNode : walked) {
				final BitSet bits = memo.get(walkedNode);
				bits.set(BITS * index + PREVIOUS_KNOWN);
				bits.set(BITS * index + PREVIOUS, result);
			}
			walked.clear();
			return result;
		}
	}

	/**
	 * Selects the nodes from left to right: the nodes matching the first selector are selected,
	 * then the nodes matching the second selector from them, and so on.
	 * Required by the selectors that depend on the position of the nodes within current results, eg: {@code :first}.
	 */
	private List<Node> selectFromLeft(final Node rootNode, final List<CssSelector> selectors) {

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
			List<Node> selectedNodes = new UniqueNodeList();
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