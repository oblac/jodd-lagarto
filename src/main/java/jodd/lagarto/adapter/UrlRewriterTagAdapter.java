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

package jodd.lagarto.adapter;

import jodd.lagarto.Tag;
import jodd.lagarto.TagAdapter;
import jodd.lagarto.TagVisitor;
import jodd.util.CharUtil;

import java.util.function.Function;

/**
 * URL Rewriter.
 */
public class UrlRewriterTagAdapter extends TagAdapter {

	private final Function<CharSequence, CharSequence> rewriter;

	public UrlRewriterTagAdapter(final TagVisitor target, final Function<CharSequence, CharSequence> rewriter) {
		super(target);
		this.rewriter = rewriter;
	}

	@Override
	public void tag(final Tag tag) {
		if (tag.getType().isStartingTag()) {
			final CharSequence tagName = tag.getName();

			if (tagName.length() == 1 && CharUtil.toLowerAscii(tagName.charAt(0)) == 'a') {
				final CharSequence href = tag.getAttributeValue("href");

				if (href != null) {
					final CharSequence newHref = rewriter.apply(href);

					if (newHref != href) {
						tag.setAttribute("href", newHref);
					}
				}
			}
		}
		super.tag(tag);
	}
}
