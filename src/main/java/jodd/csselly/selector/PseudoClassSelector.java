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

import jodd.csselly.CSSellyException;
import jodd.csselly.Selector;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeFilter;
import jodd.lagarto.dom.NodeListFilter;
import jodd.util.ClassUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pseudo class selector.
 * A pseudo-class always consists of a "colon" (:) followed by
 * the name of the pseudo-class and optionally by a value between parentheses.
 * <p>
 * Selectors introduces the concept of structural pseudo-classes to permit
 * selection based on extra information that lies in the document tree
 * but cannot be represented by other simple selectors or combinators.
 * <p>
 * Standalone text and other non-element nodes are not counted when calculating
 * the position of an element in the list of children of its parent.
 * When calculating the position of an element in the list of children
 * of its parent, the index numbering starts at 1.
 */
public class PseudoClassSelector extends Selector implements NodeFilter, NodeListFilter {

	protected static final Map<String, PseudoClass> PSEUDO_CLASS_MAP;

	static {
		PSEUDO_CLASS_MAP = new HashMap<>(26);

		registerPseudoClass(PseudoClasses.EMPTY.class);
		registerPseudoClass(PseudoClasses.FIRST_CHILD.class);
		registerPseudoClass(PseudoClasses.FIRST_OF_TYPE.class);
		registerPseudoClass(PseudoClasses.LAST_CHILD.class);
		registerPseudoClass(PseudoClasses.LAST_OF_TYPE.class);
		registerPseudoClass(PseudoClasses.ONLY_CHILD.class);
		registerPseudoClass(PseudoClasses.ONLY_OF_TYPE.class);
		registerPseudoClass(PseudoClasses.ROOT.class);

		registerPseudoClass(PseudoClasses.FIRST.class);
		registerPseudoClass(PseudoClasses.LAST.class);
		registerPseudoClass(PseudoClasses.BUTTON.class);
		registerPseudoClass(PseudoClasses.CHECKBOX.class);
		registerPseudoClass(PseudoClasses.FILE.class);
		registerPseudoClass(PseudoClasses.IMAGE.class);
		registerPseudoClass(PseudoClasses.INPUT.class);
		registerPseudoClass(PseudoClasses.HEADER.class);
		registerPseudoClass(PseudoClasses.PARENT.class);
		registerPseudoClass(PseudoClasses.PASSWORD.class);
		registerPseudoClass(PseudoClasses.RADIO.class);
		registerPseudoClass(PseudoClasses.RESET.class);
		registerPseudoClass(PseudoClasses.SELECTED.class);
		registerPseudoClass(PseudoClasses.CHECKED.class);
		registerPseudoClass(PseudoClasses.SUBMIT.class);
		registerPseudoClass(PseudoClasses.TEXT.class);
		registerPseudoClass(PseudoClasses.EVEN.class);
		registerPseudoClass(PseudoClasses.ODD.class);
	}

	/**
	 * Registers pseudo class.
	 */
	public static void registerPseudoClass(final Class<? extends PseudoClass> pseudoClassType) {
		final PseudoClass pseudoClass;
		try {
			pseudoClass = ClassUtil.newInstance(pseudoClassType);
		} catch (final Exception ex) {
			throw new CSSellyException(ex);
		}
		PSEUDO_CLASS_MAP.put(pseudoClass.getPseudoClassName(), pseudoClass);
	}

	/**
	 * Lookups pseudo class for given pseudo class name.
	 */
	public static PseudoClass lookupPseudoClass(final String pseudoClassName) {
		final PseudoClass pseudoClass = PSEUDO_CLASS_MAP.get(pseudoClassName);
		if (pseudoClass == null) {
			throw new CSSellyException("Unsupported pseudo class: " + pseudoClassName);
		}
		return pseudoClass;
	}

	// ---------------------------------------------------------------- selector

	protected final PseudoClass pseudoClass;

	public PseudoClassSelector(final String pseudoClassName) {
		super(Type.PSEUDO_CLASS);
		this.pseudoClass = lookupPseudoClass(pseudoClassName.trim());
	}

	/**
	 * Returns {@link PseudoClass pseudo class} value.
	 */
	public PseudoClass getPseudoClass() {
		return pseudoClass;
	}

	/**
	 * Matches node to this selector.
	 */
	@Override
	public boolean accept(final Node node) {
		return pseudoClass.match(node);
	}

	/**
	 * Accepts node within selected results. Invoked after results are matched.
	 */
	@Override
	public boolean accept(final List<Node> currentResults, final Node node, final int index) {
		return pseudoClass.matchInRange(currentResults, node, index);
	}

}
