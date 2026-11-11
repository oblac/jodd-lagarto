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

package jodd.csselly.selector;

import jodd.lagarto.dom.Node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * State of form controls and links, as defined by the
 * <a href="https://html.spec.whatwg.org/multipage/semantics-other.html#pseudo-classes">HTML pseudo-classes</a>.
 * As there is no user interaction, the state only depends on the attributes.
 */
final class FormControls {

	private FormControls() {
	}

	private static final Set<String> INPUT_TYPES = set(
		"hidden", "text", "search", "tel", "url", "email", "password", "date", "month", "week", "time",
		"datetime-local", "number", "range", "color", "checkbox", "radio", "file", "submit", "image", "reset", "button");
	// input types the required attribute doesn't apply to
	private static final Set<String> REQUIRED_NOT_APPLICABLE_INPUT_TYPES = set(
		"hidden", "range", "color", "submit", "image", "reset", "button");
	// input types the readonly attribute applies to
	private static final Set<String> READONLY_APPLICABLE_INPUT_TYPES = set(
		"text", "search", "url", "tel", "email", "password", "date", "month", "week", "time", "datetime-local", "number");

	private static Set<String> set(final String... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	static String name(final Node node) {
		final String name = node.getNodeName();
		return name == null ? "" : name.toLowerCase(Locale.ROOT);
	}

	private static boolean isElement(final Node node) {
		return node != null && node.getNodeType() == Node.NodeType.ELEMENT;
	}

	/**
	 * Returns the state of the type attribute of an input: unknown types are text inputs.
	 */
	static String inputType(final Node input) {
		final String type = input.getAttribute("type");
		if (type == null) {
			return "text";
		}
		final String lowerCaseType = type.toLowerCase(Locale.ROOT);
		return INPUT_TYPES.contains(lowerCaseType) ? lowerCaseType : "text";
	}

	/**
	 * Returns {@code true} for the elements that match either {@code :enabled} or {@code :disabled}.
	 */
	static boolean canBeDisabled(final Node node) {
		switch (name(node)) {
			case "button":
			case "input":
			case "select":
			case "textarea":
			case "optgroup":
			case "option":
			case "fieldset":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Returns {@code true} if the element is actually disabled.
	 */
	static boolean isDisabled(final Node node) {
		switch (name(node)) {
			case "button":
			case "input":
			case "select":
			case "textarea":
			case "fieldset":
				return node.hasAttribute("disabled") || isInDisabledFieldset(node);
			case "optgroup":
				return node.hasAttribute("disabled");
			case "option":
				if (node.hasAttribute("disabled")) {
					return true;
				}
				final Node parent = node.getParentNode();
				return isElement(parent) && name(parent).equals("optgroup") && parent.hasAttribute("disabled");
			default:
				return false;
		}
	}

	/**
	 * Returns {@code true} if the element is a descendant of a disabled fieldset,
	 * but not of the first legend of this fieldset.
	 */
	private static boolean isInDisabledFieldset(final Node node) {
		Node child = node;
		Node parent = node.getParentNode();
		while (isElement(parent)) {
			if (name(parent).equals("fieldset") && parent.hasAttribute("disabled") && child != firstLegend(parent)) {
				return true;
			}
			child = parent;
			parent = parent.getParentNode();
		}
		return false;
	}

	private static Node firstLegend(final Node fieldset) {
		final int childCount = fieldset.getChildNodesCount();
		for (int i = 0; i < childCount; i++) {
			final Node child = fieldset.getChild(i);
			if (isElement(child) && name(child).equals("legend")) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Returns {@code true} for the elements the required attribute applies to.
	 */
	static boolean canBeRequired(final Node node) {
		switch (name(node)) {
			case "select":
			case "textarea":
				return true;
			case "input":
				return !REQUIRED_NOT_APPLICABLE_INPUT_TYPES.contains(inputType(node));
			default:
				return false;
		}
	}

	/**
	 * Returns {@code true} for the form controls that match either {@code :required} or {@code :optional} in browsers.
	 */
	static boolean isSubmittable(final Node node) {
		switch (name(node)) {
			case "button":
			case "input":
			case "select":
			case "textarea":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Returns {@code true} if the element matches {@code :read-write}: a text field that is neither read-only
	 * nor disabled, or an editable element.
	 */
	static boolean isReadWrite(final Node node) {
		switch (name(node)) {
			case "input":
				return READONLY_APPLICABLE_INPUT_TYPES.contains(inputType(node))
					&& !node.hasAttribute("readonly") && !isDisabled(node);
			case "textarea":
				return !node.hasAttribute("readonly") && !isDisabled(node);
			default:
				return isEditable(node);
		}
	}

	/**
	 * Returns {@code true} if the element is editable with {@code contenteditable}, which is inherited.
	 */
	private static boolean isEditable(Node node) {
		while (isElement(node)) {
			if (node.hasAttribute("contenteditable")) {
				// an attribute without value has a null value
				final String value = node.getAttribute("contenteditable");
				switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
					case "":
					case "true":
					case "plaintext-only":
						return true;
					case "false":
						return false;
					default:
						// invalid value: inherited
				}
			}
			node = node.getParentNode();
		}
		return false;
	}

	/**
	 * Returns {@code true} if the element is a hyperlink.
	 */
	static boolean isLink(final Node node) {
		final String name = name(node);
		return (name.equals("a") || name.equals("area")) && node.hasAttribute("href");
	}
}
