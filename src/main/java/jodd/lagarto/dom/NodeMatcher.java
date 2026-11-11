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
import jodd.csselly.CssSelector;

import java.util.BitSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class NodeMatcher {

	protected final Node rootNode;

	/**
	 * Nodes known not to match the selectors up to a given index, for the selector being matched.
	 * Avoids exponential backtracking with several descendant or general sibling combinators.
	 */
	private Map<Node, BitSet> failures;

	public NodeMatcher(final Node rootNode) {
		this.rootNode = rootNode;
	}

	public boolean match(final String query) {
		Collection<List<CssSelector>> selectorsCollection = CSSelly.parse(query);
		return match(selectorsCollection);
	}

	/**
	 * Returns {@code true} if the node matches any of the selectors of the group.
	 * Each selector is matched from right to left, honoring the combinators.
	 */
	public boolean match(final Collection<List<CssSelector>> selectorsCollection) {
		try {
			for (List<CssSelector> selectors : selectorsCollection) {
				if (failures != null) {
					failures.clear();
				}
				if (match(rootNode, selectors, selectors.size() - 1)) {
					return true;
				}
			}
			return false;
		} finally {
			failures = null;
		}
	}

	/**
	 * Matches the node against the selector at given index and,
	 * recursively, its ancestors or previous siblings against the previous selectors.
	 */
	protected boolean match(final Node node, final List<CssSelector> selectors, final int index) {
		if (failures != null) {
			final BitSet nodeFailures = failures.get(node);
			if (nodeFailures != null && nodeFailures.get(index)) {
				return false;
			}
		}
		final CssSelector cssSelector = selectors.get(index);
		if (!cssSelector.accept(node)) {
			return false;
		}
		if (index == 0) {
			return true;
		}
		if (matchPrevious(node, selectors, index - 1)) {
			return true;
		}
		// the node matches the selector, but not its previous selectors: no need to try again
		if (failures == null) {
			failures = new IdentityHashMap<>();
		}
		failures.computeIfAbsent(node, n -> new BitSet(selectors.size())).set(index);
		return false;
	}

	/**
	 * Matches the ancestors or previous siblings of the node against the selector at given index,
	 * depending on its combinator.
	 */
	private boolean matchPrevious(final Node node, final List<CssSelector> selectors, final int prevIndex) {
		switch (selectors.get(prevIndex).getCombinator()) {
			case CHILD: {
				final Node parent = node.getParentNode();
				return parent != null && match(parent, selectors, prevIndex);
			}
			case DESCENDANT: {
				Node ancestor = node.getParentNode();
				while (ancestor != null) {
					if (match(ancestor, selectors, prevIndex)) {
						return true;
					}
					ancestor = ancestor.getParentNode();
				}
				return false;
			}
			case ADJACENT_SIBLING: {
				final Node sibling = node.getPreviousSiblingElement();
				return sibling != null && match(sibling, selectors, prevIndex);
			}
			case GENERAL_SIBLING: {
				Node sibling = node.getPreviousSiblingElement();
				while (sibling != null) {
					if (match(sibling, selectors, prevIndex)) {
						return true;
					}
					sibling = sibling.getPreviousSiblingElement();
				}
				return false;
			}
			default:
				return false;
		}
	}


}
