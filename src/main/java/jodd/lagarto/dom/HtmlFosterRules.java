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

import jodd.util.StringUtil;

import java.util.ArrayList;

/**
 * Simplified HTML foster rules for tables.
 */
public class HtmlFosterRules {

	public static final String[] FOSTER_TABLE_ELEMENTS = new String[] {
		"table", "tbody", "tfoot", "thead", "tr"
	};

	public static final String[] TABLE_ELEMENTS = new String[] {
		"table", "tbody", "tfoot", "thead", "th" ,"tr", "td", "caption", "colgroup", "col"
	};

	/**
	 * Returns {@code true} if provided element is one of the table-related elements.
	 */
	protected boolean isOneOfTableElements(final Element element) {
		final String elementName = element.getNodeName().toLowerCase();

		return StringUtil.equalsOne(elementName, TABLE_ELEMENTS) != -1;
	}

	/**
	 * Returns {@code true} if given node is a table element.
	 */
	protected boolean isTableElement(final Node node) {
		if (node.getNodeType() != Node.NodeType.ELEMENT) {
			return false;
		}
		final String elementName = node.getNodeName().toLowerCase();

		return elementName.equals("table");
	}

	/**
	 * Returns {@code true} if parent node is one of the table elements.
	 */
	protected boolean isParentNodeOneOfFosterTableElements(final Node parentNode) {
		if (parentNode == null) {
			return false;
		}
		if (parentNode.getNodeName() == null) {
			return false;
		}
		final String nodeName = parentNode.getNodeName().toLowerCase();

		return StringUtil.equalsOne(nodeName, FOSTER_TABLE_ELEMENTS) != -1;
	}

	/**
	 * Finds the last table in stack of open elements.
	 */
	protected Element findLastTable(final Node node) {
		Node tableNode = node;

		while (tableNode != null) {
			if (tableNode.getNodeType() == Node.NodeType.ELEMENT) {
				final String tableNodeName = tableNode.getNodeName().toLowerCase();

				if (tableNodeName.equals("table")) {
					break;
				}
			}
			tableNode = tableNode.getParentNode();
		}

		return (Element) tableNode;
	}

	// ---------------------------------------------------------------- core

	protected ArrayList<Element> lastTables = new ArrayList<>();
	protected ArrayList<Element> fosterElements = new ArrayList<>();
	protected ArrayList<Text> fosterTexts = new ArrayList<>();

	/**
	 * Fixes foster elements.
	 */
	public void fixFosterElements(final Document document) {
		findFosterNodes(document);
		fixElements();
		fixText();
	}

	/**
	 * Finds foster elements. Returns {@code true} if there was no change in
	 * DOM tree of the parent element. Otherwise, returns {@code false}
	 * meaning that parent will scan its childs again.
	 */
	protected boolean findFosterNodes(final Node node) {
		boolean isTable = false;

		if (!lastTables.isEmpty()) {
			// if inside table
			if (node.getNodeType() == Node.NodeType.TEXT) {
				final String value = node.getNodeValue();
				if (!StringUtil.isBlank(value)) {
					if (isParentNodeOneOfFosterTableElements(node.getParentNode())) {
						fosterTexts.add((Text) node);
					}
				}
			}
		}

		if (node.getNodeType() == Node.NodeType.ELEMENT) {
			final Element element = (Element) node;

			isTable = isTableElement(node);

			if (isTable) {
				// if node is a table, add it to the stack-of-last-tables
				lastTables.add(element);
			} else {
				// otherwise...

				// ...if inside the table
				if (!lastTables.isEmpty()) {
					// check this and parent
					final Node parentNode = node.getParentNode();
					if (
							isParentNodeOneOfFosterTableElements(parentNode) &&
							!isOneOfTableElements(element)
							) {

						final String elementNodeName = element.getNodeName().toLowerCase();
						if (elementNodeName.equals("form")) {
							if (element.getChildNodesCount() > 0) {
								// if form element, take all its child nodes
								// and add after the from element
								final Node[] formChildNodes = element.getChildNodes();
								parentNode.insertAfter(formChildNodes, element);
								return false;
							} else {
								// empty form element, leave it where it is
								return true;
							}
						}

						if (elementNodeName.equals("input")) {
							final String inputType = element.getAttribute("type");

							if (inputType.equals("hidden")) {
								// input hidden elements remains as they are
								return true;
							}
						}

						// foster element found, remember it to process it later
						fosterElements.add(element);
					}

				} else {
					// ...if not inside the table, just keep going
				}
			}
		}

		allchilds:
		while (true) {
			final int childs = node.getChildNodesCount();
			for (int i = 0; i < childs; i++) {
				final Node childNode = node.getChild(i);

				final boolean done = findFosterNodes(childNode);
				if (!done) {
					continue allchilds;
				}
			}
			break;
		}

		if (isTable) {
			// remove last element
			final int size = lastTables.size();
			if (size > 0) {
				lastTables.remove(size - 1);	// no array copy occurs when the last element is removed
			}
		}
		return true;
	}

	/**
	 * Performs the fix for elements.
	 */
	protected void fixElements() {
		for (final Element fosterElement : fosterElements) {
			// find parent table
			final Element lastTable = findLastTable(fosterElement);
			final Node fosterElementParent = fosterElement.getParentNode();

			// filter our foster element
			final Node[] fosterChilds = fosterElement.getChildNodes();
			for (final Node fosterChild : fosterChilds) {
				if (fosterChild.getNodeType() == Node.NodeType.ELEMENT) {
					if (isOneOfTableElements((Element) fosterChild)) {
						// move all child table elements outside
						// the foster element
						fosterChild.detachFromParent();
						fosterElementParent.insertBefore(fosterChild, fosterElement);
					}
				}
			}

			// finally, move foster element above the table
			fosterElement.detachFromParent();
			lastTable.getParentNode().insertBefore(fosterElement, lastTable);
		}
	}

	protected void fixText() {
		for (final Text fosterText : fosterTexts) {
			// find parent table
			final Element lastTable = findLastTable(fosterText);

			// move foster element above the table
			fosterText.detachFromParent();

			final Node tablesPreviousNode = lastTable.getPreviousSibling();
			if (tablesPreviousNode.getNodeType() == Node.NodeType.TEXT) {
				// append to previous text node
				final Text textNode = (Text) tablesPreviousNode;

				final String text = textNode.getNodeValue();

				textNode.setNodeValue(text + fosterText.getNodeValue());
			} else {
				// insert text node before the table
				lastTable.getParentNode().insertBefore(fosterText, lastTable);
			}
		}
	}

}
