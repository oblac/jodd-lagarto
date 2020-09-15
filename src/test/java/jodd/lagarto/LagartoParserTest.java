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

package jodd.lagarto;

import jodd.io.FileUtil;
import jodd.lagarto.visitor.TagWriter;
import jodd.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static jodd.util.StringPool.NEWLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LagartoParserTest {

	protected final String testDataRoot = this.getClass().getResource("data").getFile();
	protected final String testTagRoot = this.getClass().getResource("tags").getFile();

	@Test
	void testDataHtml() throws IOException {
		_testHtmls(testDataRoot);
	}

	@Test
	void testTagsHtml() throws IOException {
		_testHtmls(testTagRoot);
	}

	private void _testHtmls(final String root) throws IOException {
		final List<File> files = new ArrayList<>();

		Files.walkFileTree(FileUtil.file(root).toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file,
			                                 final BasicFileAttributes attrs) throws IOException {
				if (file.toString().endsWith("ml")) {
					files.add(file.toFile());
				}
				return FileVisitResult.CONTINUE;
			}
		});

		long reps = 1;

		boolean processed = false;

		while (reps-- > 0) {
			for (final File file : files) {
				processed = true;
				System.out.println('+' + file.getName());

				String content = FileUtil.readString(file);
				content = StringUtil.removeChars(content, '\r');
				String expectedResult = FileUtil.readString(new File(file.getAbsolutePath() + ".txt"));

				String formatted = null;
				final File formattedFile = new File(file.getAbsolutePath() + "-fmt.htm");
				if (formattedFile.exists()) {
					formatted = FileUtil.readString(formattedFile);
				}
				if (formatted != null) {
					formatted = StringUtil.removeChars(formatted, '\r');
				}

				final boolean isXml = file.getName().endsWith(".xml");

				final String[] results = _parse(content, isXml);
				String result = results[0];        // parsing result
				String result2 = results[1];    // tag writer

				expectedResult = StringUtil.removeChars(expectedResult, '\r');
				result = StringUtil.removeChars(result, '\r').trim();
				result2 = StringUtil.removeChars(result2, '\r').trim();

				assertEquals(expectedResult, result);

				if (formatted != null) {
					assertEquals(formatted, result2);
				} else {
					assertEquals(content, result2);
				}
			}
		}

		assertTrue(processed);
	}

	private String[] _parse(final String content, final boolean isXml) {
		final StringBuilder result = new StringBuilder();
		final StringBuilder out = new StringBuilder();

		final TagVisitor visitor = new TagVisitor() {

			@Override
			public void start() {
			}

			@Override
			public void end() {
			}

			@Override
			public void tag(final Tag tag) {
				result.append("tag:").append(tag.getName());
				result.append(':').append(tag.getDeepLevel());
				switch (tag.getType()) {
					case START:
						result.append('<');
						break;
					case END:
						result.append('>');
						break;
					case SELF_CLOSING:
						result.append("<>");
						break;
				}
				if (tag.getAttributeCount() > 0) {
					tag.writeTo(result);
				}
				result.append(NEWLINE);
			}

			@Override
			public void xml(final CharSequence version, final CharSequence encoding, final CharSequence standalone) {
				result.append("xml:").append(version).append(':').append(encoding).append(':').append(standalone);
				result.append(NEWLINE);
			}

			@Override
			public void script(final Tag tag, final CharSequence bodyM) {
				result.append("scr:").append(tag.getDeepLevel());
				if (tag.getAttributeCount() > 0) {
					tag.writeTo(result);
				}
				String body = bodyM.toString();
				body = StringUtil.removeChars(body, "\r\n\t\b");
				result.append('[').append(body).append(']');
				result.append(NEWLINE);
			}

			@Override
			public void comment(final CharSequence commentM) {
				String comment = commentM.toString();
				comment = StringUtil.removeChars(comment, "\r\n\t\b");
				result.append("com:[").append(comment).append(']').append(NEWLINE);
			}

			@Override
			public void cdata(final CharSequence cdataM) {
				String cdata = cdataM.toString();
				cdata = StringUtil.removeChars(cdata, "\r\n\t\b");
				result.append("cdt:[").append(cdata).append(']').append(NEWLINE);
			}

			@Override
			public void doctype(final Doctype doctype) {
				result.append("doc:[").append(doctype.getName()).append(' ');
				result.append(doctype.getPublicIdentifier()).append(' ').append(doctype.getSystemIdentifier()).append(']').append(NEWLINE);
			}

			@Override
			public void condComment(final CharSequence expression, final boolean isStartingTag, final boolean isHidden, final boolean isHiddenEndTag) {
				result.append(isStartingTag ? "CC" : "cc").append(isHidden ? 'H' : 'S');
				result.append(isHiddenEndTag ? "h" : "");
				result.append(":[").append(expression).append(']');
				result.append(NEWLINE);

			}

			@Override
			public void text(final CharSequence text) {
				String t = text.toString();
				t = StringUtil.removeChars(t, "\r\n\t\b");
				if (t.length() != 0) {
					result.append("txt:[").append(t).append(']').append(NEWLINE);
				}
			}

			@Override
			public void error(final String message) {
				result.append("wrn:[").append(message).append(NEWLINE);
			}
		};


		final LagartoParser lagartoParser = new LagartoParser(content);
		lagartoParser.getConfig().setCalculatePosition(true);
		lagartoParser.getConfig().setEnableConditionalComments(true);

		if (isXml) {
			lagartoParser.getConfig().setParseXmlTags(true);
		}

		final TagWriter tagWriter = new TagWriter(out);

		lagartoParser.parse(new TagVisitors(visitor, tagWriter));

		return new String[]{result.toString(), out.toString()};
	}

}
