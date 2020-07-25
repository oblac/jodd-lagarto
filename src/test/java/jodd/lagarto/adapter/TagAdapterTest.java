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

import jodd.io.FileUtil;
import jodd.lagarto.LagartoParser;
import jodd.lagarto.Tag;
import jodd.lagarto.TagAdapter;
import jodd.lagarto.TagWriter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagAdapterTest {

	protected final String testAdapterRoot = this.getClass().getResource("data").getFile();

	@Test
	void testCleanHtml() throws IOException {
		File ff = new File(testAdapterRoot, "clean.html");

		final LagartoParser lagartoParser = new LagartoParser(FileUtil.readString(ff));

		final StringBuilder out = new StringBuilder();
		final TagWriter tagWriter = new TagWriter(out);
		final StripHtmlTagAdapter stripHtmlTagAdapter = new StripHtmlTagAdapter(tagWriter);
		lagartoParser.parse(stripHtmlTagAdapter);

		ff = new File(testAdapterRoot, "clean-out.html");

		assertEquals(FileUtil.readString(ff), out.toString());
	}

	@Test
	void testTwoAdapters() throws IOException {
		File ff = new File(testAdapterRoot, "two.html");

		final LagartoParser lagartoParser = new LagartoParser(FileUtil.readString(ff));
		final StringBuilder out = new StringBuilder();
		final TagWriter tagWriter = new TagWriter(out);

		final TagAdapter tagAdapter1 = new TagAdapter(tagWriter) {
			@Override
			public void tag(final Tag tag) {
				if (tag.getType().isStartingTag()) {
					final String tagname = tag.getName().toString();
					if (tagname.equals("title")) {
						final String id = tag.getAttributeValue("id").toString();
						tag.setAttribute("id", String.valueOf(Integer.parseInt(id) + 1));
					}
				}
				super.tag(tag);
			}
		};


		final TagAdapter tagAdapter2 = new TagAdapter(tagAdapter1) {
			@Override
			public void tag(final Tag tag) {
				if (tag.getType().isStartingTag()) {
					final String tagname = tag.getName().toString();
					if (tagname.equals("title")) {
						tag.addAttribute("id", "172");
					}
				}
				super.tag(tag);
			}
		};


		lagartoParser.parse(tagAdapter2);
		ff = new File(testAdapterRoot, "two-out.html");
		assertEquals(FileUtil.readString(ff), out.toString());
	}

}
