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
import jodd.jerry.Jerry;
import jodd.jerry.JerryParser;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.Element;
import jodd.lagarto.dom.LagartoDOMBuilder;
import jodd.lagarto.visitor.TagWriter;
import jodd.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ParsingProblemsTest {

	protected final String testDataRoot = this.getClass().getResource("misc").getFile();

	@Test
	void testInvalidTag() {
		final String html = "<html>text1<=>text2</html>";

		final LagartoParser lagartoParser = new LagartoParser(html);

		final StringBuilder sb = new StringBuilder();

		try {
			lagartoParser.parse(new EmptyTagVisitor() {
				@Override
				public void tag(final Tag tag) {
					sb.append(tag.getName()).append(' ');
				}

				@Override
				public void text(final CharSequence text) {
					sb.append(text).append(' ');
				}

				@Override
				public void error(final String message) {
					System.out.println(message);
				}
			});
		} catch (final LagartoException lex) {
			lex.printStackTrace();
			fail("error");
		}

		assertEquals("html text1 <=>text2 html ", sb.toString());
	}

	@Test
	void testNonQuotedAttributeValue() {
		String html = "<a href=123>xxx</a>";

		LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getParserConfig().setCalculatePosition(true);
		Document document = lagartoDOMBuilder.parse(html);

		assertEquals("<a href=\"123\">xxx</a>", document.getHtml());
		assertTrue(document.check());

		html = "<a href=../org/w3c/dom/'http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-list'>xxx</a>";

		lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getParserConfig().setCalculatePosition(true);
		document = lagartoDOMBuilder.parse(html);
		assertTrue(document.check());

		assertEquals("<a href=\"../org/w3c/dom/'http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-list'\">xxx</a>", document.getHtml());
	}

	@Test
	void testIssue23_0() throws IOException {
		final File file = new File(testDataRoot, "index-4-v0.html");

		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getParserConfig().setCalculatePosition(true);
		lagartoDOMBuilder.getConfig().setCollectErrors(true);
		final Document doc = lagartoDOMBuilder.parse(FileUtil.readString(file));
		assertTrue(doc.check());

		assertEquals(1, doc.getErrors().size());
	}

	@Test
	void testIssue23_1() throws IOException {
		final File file = new File(testDataRoot, "index-4-v1.html");

		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getParserConfig().setCalculatePosition(true);
		lagartoDOMBuilder.getConfig().setCollectErrors(true);
		final Document doc = lagartoDOMBuilder.parse(FileUtil.readString(file));
		assertTrue(doc.check());

		assertEquals(1, doc.getErrors().size());
	}

	@Test
	void testIssue23() throws IOException {
		File file = new File(testDataRoot, "index-4.html");

		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getParserConfig().setCalculatePosition(true);
		lagartoDOMBuilder.getConfig().setCollectErrors(true);
		final Document document = lagartoDOMBuilder.parse(FileUtil.readString(file));
		assertTrue(document.check());

		// (1564 open DTs + 1564 open DDs) 1 open P
		assertEquals(19, document.getErrors().size());

		Jerry doc = Jerry.of(FileUtil.readString(file));
		assertEquals(16, doc.s("td.NavBarCell1").size());
		assertEquals(2, doc.s("table td.NavBarCell1Rev").size());

		assertEquals(1, doc.s("dl").size());
		assertEquals(1564, doc.s("dd").size());
		assertEquals(1564, doc.s("dt").size());
		assertEquals(3144, doc.s("dt a").size());

		// http://docs.oracle.com/javase/6/docs/api/index-files/index-4.html
		file = new File(testDataRoot, "index-4-eng.html");
		doc = Jerry.of(FileUtil.readString(file));

		assertEquals(16, doc.s("td.NavBarCell1").size());
		assertEquals(2, doc.s("table td.NavBarCell1Rev").size());

		final StringBuilder sb = new StringBuilder();
		doc.s("td.NavBarCell1").each(($this, index) -> {
			sb.append("---\n");
			sb.append($this.text().trim());
			sb.append('\n');
			return true;
		});
		String s = sb.toString();
		s = StringUtil.remove(s, ' ');
		s = StringUtil.remove(s, '\r');
		s = StringUtil.remove(s, '\u00A0');
		s = StringUtil.remove(s, "&nbsp;");
		assertEquals(
				"---\n" +
						"Overview\n" +
						"Package\n" +
						"Class\n" +
						"Use\n" +
						"Tree\n" +
						"Deprecated\n" +
						"Index\n" +
						"Help\n" +
						"---\n" +
						"Overview\n" +
						"---\n" +
						"Package\n" +
						"---\n" +
						"Class\n" +
						"---\n" +
						"Use\n" +
						"---\n" +
						"Tree\n" +
						"---\n" +
						"Deprecated\n" +
						"---\n" +
						"Help\n" +
						"---\n" +
						"Overview\n" +
						"Package\n" +
						"Class\n" +
						"Use\n" +
						"Tree\n" +
						"Deprecated\n" +
						"Index\n" +
						"Help\n" +
						"---\n" +
						"Overview\n" +
						"---\n" +
						"Package\n" +
						"---\n" +
						"Class\n" +
						"---\n" +
						"Use\n" +
						"---\n" +
						"Tree\n" +
						"---\n" +
						"Deprecated\n" +
						"---\n" +
						"Help\n",
				s);
	}

	@Test
	void testNamespaces() throws IOException {
		final File file = new File(testDataRoot, "namespace.xml");

		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableXmlMode();
		lagartoDOMBuilder.getParserConfig().setCalculatePosition(true);

		final Document doc = lagartoDOMBuilder.parse(FileUtil.readString(file));
		assertTrue(doc.check());

		final Element cfgTestElement = (Element) doc.getChild(1);

		assertEquals("cfg:test", cfgTestElement.getNodeName());

		final Element cfgNode = (Element) cfgTestElement.getChild(0);

		assertEquals("cfg:node", cfgNode.getNodeName());


		final JerryParser jerryParser = new JerryParser();

		((LagartoDOMBuilder) jerryParser.getDOMBuilder()).enableXmlMode();

		final Jerry jerry = jerryParser.parse(FileUtil.readString(file));

		final StringBuilder result = new StringBuilder();

		jerry.s("cfg\\:test").each(($this, index) -> {
			result.append($this.s("cfg\\:node").text());
			return true;
		});

		assertEquals("This is a text", result.toString());
	}

	@Test
	void testPreserveCC() throws IOException {
		final File file = new File(testDataRoot, "preserve-cc.html");

		final String expectedResult = FileUtil.readString(file);

		final JerryParser jerryParser = new JerryParser();
		((LagartoDOMBuilder) jerryParser.getDOMBuilder()).enableHtmlMode();
		((LagartoDOMBuilder) jerryParser.getDOMBuilder()).getParserConfig().setEnableConditionalComments(false);

		final Jerry jerry = jerryParser.parse(expectedResult);
		final String result = jerry.html();

		assertEquals(expectedResult, result);
	}

	@Test
	void testKelkoo() throws Exception {
		final File file = new File(testDataRoot, "kelkoo.html");
		final Jerry jerry;
		try {
			jerry = Jerry.create().parse(FileUtil.readString(file));
		} catch (final Exception ex) {
			fail(ex.toString());
			throw ex;
		}

		final Element script = (Element) jerry.s("script").get(0);

		assertEquals("script", script.getNodeName());
		assertEquals(6, script.getAttributesCount());

		assertEquals("src", script.getAttribute(0).getName());
		assertEquals("data-config", script.getAttribute(1).getName());
		assertEquals("ext\\u00e9rieur|barbecue,", script.getAttribute(2).getName());
		assertEquals("planchaaccessoires\":\"http:\\", script.getAttribute(3).getName());
		assertEquals("www.kelkoo.fr\"}'", script.getAttribute(4).getName());
		assertEquals("data-adsense-append", script.getAttribute(5).getName());
	}

	@Test
	void testEntity() {
		assertEquals(
				"<head><title>Peanut Butter &amp; Jelly</title>" +
						"it's yummy &amp; delicious</head>",
				Jerry.create().parse(
						"<head><title>Peanut Butter & Jelly</title>" +
								"it's yummy & delicious").html());
	}

	@Test
	void testCoCo() {
		final StringBuilder stringBuilder = new StringBuilder();

		new LagartoParser("Jean-Pierre Vitrac, CO&CO").parse(new EmptyTagVisitor() {
			@Override
			public void text(final CharSequence text) {
				stringBuilder.append(text);
			}
		});
		assertEquals("Jean-Pierre Vitrac, CO&CO", stringBuilder.toString());
	}


	@Test
	void testCnnConditionals() {
		final String html =
				"<html><head>\n" +
						"<!--[if lte IE 9]><meta http-equiv=\"refresh\" content=\"1;url=/2.218.0/static/unsupp.html\" /><![endif]-->\n" +
						"<!--[if gt IE 9><!--><script>alert(\"Hello!\");</script><!--<![endif]-->\n" +
						"</head>\n" +
						"</html>";

		final StringBuilder sb = new StringBuilder();
		final AtomicInteger errorCount = new AtomicInteger(0);

		new LagartoParser(html).configure(cfg -> {
			cfg.setEnableConditionalComments(true);
		}).parse(new EmptyTagVisitor() {
			@Override
			public void condComment(final CharSequence expression, final boolean isStartingTag, final boolean isHidden, final boolean isHiddenEndTag) {
				sb.append("C:").append(expression).append('-').append(isStartingTag).append('\n');
			}

			@Override
			public void comment(final CharSequence comment) {
				sb.append("R:").append(comment).append('\n');
			}

			@Override
			public void error(final String message) {
				errorCount.incrementAndGet();
			}
		});

		assertEquals(0, errorCount.intValue());
		assertEquals(
				"C:if lte IE 9-true\n" +
						"C:endif-false\n" +
						"R:[if gt IE 9><!\n" +
						"R:<![endif]\n",
				sb.toString());
	}

	@Test
	void testShortComment() {
		final StringBuilder sb = new StringBuilder();
		final AtomicInteger errorCount = new AtomicInteger(0);

		new LagartoParser("<!---->").parse(new EmptyTagVisitor() {
			@Override
			public void comment(final CharSequence comment) {
				sb.append(comment);
			}

			@Override
			public void error(final String message) {
				errorCount.incrementAndGet();
			}
		});
		assertEquals(0, errorCount.intValue());
		assertEquals("", sb.toString());

		// err
		sb.setLength(0);
		new LagartoParser("<!--->").parse(new EmptyTagVisitor() {
			@Override
			public void comment(final CharSequence comment) {
				sb.append(comment);
			}

			@Override
			public void error(final String message) {
				errorCount.incrementAndGet();
			}
		});
		assertEquals(2, errorCount.intValue());
		assertEquals("-", sb.toString());
	}

	@Test
	void testShortComment2() {
		final StringBuilder sb = new StringBuilder();
		final AtomicInteger errorCount = new AtomicInteger(0);

		new LagartoParser("<html>\n" +
				"<body>\n" +
				"<!--->\n" +
				"-->\n" +
				"</body>\n" +
				"</html>").parse(new EmptyTagVisitor() {
			@Override
			public void comment(final CharSequence comment) {
				sb.append(comment);
			}

			@Override
			public void error(final String message) {
				errorCount.incrementAndGet();
			}
		});
		assertEquals(1, errorCount.intValue());
		assertEquals("-", sb.toString());
	}

	@Test
	void testComment() {
		final StringBuilder sb = new StringBuilder();
		final AtomicInteger errorCount = new AtomicInteger(0);

		new LagartoParser("<!-- ------- IDENTIFICATION DEBUT ------- //-->").parse(new EmptyTagVisitor() {
			@Override
			public void comment(final CharSequence comment) {
				sb.append(comment);
			}

			@Override
			public void error(final String message) {
				errorCount.incrementAndGet();
			}
		});

		assertEquals(0, errorCount.intValue());
		assertEquals(" ------- IDENTIFICATION DEBUT ------- //", sb.toString());
	}

	@Test
	void testWithSelfClosingTitle() {
		final StringBuilder sb = new StringBuilder();

		final EmptyTagVisitor visitor = new EmptyTagVisitor() {
			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};

		// note the odd self-closing title: <title />
		LagartoParser parser = new LagartoParser("<html><head><title /></head><body>hello world!</body></html>");
		parser.parse(visitor);
		assertEquals("" +
				"tag: html START\n" +
				"tag: head START\n" +
				"tag: title SELF_CLOSING\n" +
				"text: </head><body>hello world!</body></html>\n", sb.toString());

		// note the correct title: <title></title>
		sb.setLength(0);
		parser = new LagartoParser("<html><head><title></title></head><body>hello world!</body></html>");
		parser.parse(visitor);
		assertEquals("" +
				"tag: html START\n" +
				"tag: head START\n" +
				"tag: title START\n" +
				"tag: title END\n" +
				"tag: head END\n" +
				"tag: body START\n" +
				"text: hello world!\n" +
				"tag: body END\n" +
				"tag: html END\n", sb.toString());
	}

	@Test
	void testAInTextArea() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};
		final LagartoParser parser = new LagartoParser("<html><body><textarea><a>Foo</a></textarea></body></html>");
		parser.parse(visitor);
		assertEquals("tag: html START\n" +
				"tag: body START\n" +
				"tag: textarea START\n" +
				"text: <a>Foo</a>\n" +
				"tag: textarea END\n" +
				"tag: body END\n" +
				"tag: html END\n", sb.toString());
	}

	@Test
	void testPInTitle() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};
		final LagartoParser parser = new LagartoParser("<html><head><title>one<p>two</p>three</title></html>");
		parser.parse(visitor);
		assertEquals("tag: html START\n" +
				"tag: head START\n" +
				"tag: title START\n" +
				"text: one<p>two</p>three\n" +
				"tag: title END\n" +
				"tag: html END\n", sb.toString());
	}

	@Test
	void testMissingAmpersand() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}
		};
		final LagartoParser parser = new LagartoParser("<html><body>&#... &#x...</body></html>");
		parser.parse(visitor);

		assertEquals("tag: html START\n" +
				"tag: body START\n" +
				"text: &#... &#x...\n" +
				"tag: body END\n" +
				"tag: html END\n", sb.toString());
	}

	@Test
	void testEmoji() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}
		};
		final LagartoParser parser = new LagartoParser("<html><body>Search &#x1F50E;</html>");
		parser.parse(visitor);

		assertEquals("tag: html START\n" +
				"tag: body START\n" +
				"text: Search \uD83D\uDD0E\n" +
				"tag: html END\n", sb.toString());
	}

	@Test
	void testStrangeIncorrectTitle() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};
		// note the incorrect syntax: </title <meta
		final String html = "<html><head><title>Hello World</title <meta content=\"text/html; charset=UTF-8\" http-equiv=\"content-type\" /><meta content=\"Description\" lang=\"en-US\" name=\"description\" /></head><body></body>hello world</html>";
		final LagartoParser parser = new LagartoParser(html);
		parser.parse(visitor);
		assertEquals("tag: html START\n" +
				"tag: head START\n" +
				"tag: title START\n" +
				"text: Hello World\n" +
				"tag: title END\n" +
				"tag: meta SELF_CLOSING\n" +
				"tag: head END\n" +
				"tag: body START\n" +
				"tag: body END\n" +
				"text: hello world\n" +
				"tag: html END\n", sb.toString());
	}


	@Test
	void testAttributesInEndTag() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + tag.getAttributeCount() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};
		final String html = "<html>123</html lang='en'>";
		final LagartoParser parser = new LagartoParser(html);
		parser.parse(visitor);
		assertEquals("tag: html START0\n" +
				"text: 123\n" +
				"tag: html END0\n", sb.toString());
	}


	@Test
	void testClosingAndSelfClosing() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};
		final String html = "<div>123</div/>";
		final LagartoParser parser = new LagartoParser(html);
		parser.parse(visitor);
		assertEquals("tag: div START\n" +
				"text: 123\n" +
				"tag: div END\n", sb.toString());
	}

	@Test
	void testSelfClosingScriptTag() {
		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {

			@Override
			public void tag(final Tag tag) {
				sb.append("tag: " + tag.getName() + " " + tag.getType() + "\n");
			}

			@Override
			public void script(final Tag tag, final CharSequence body) {
				sb.append("script: " + tag.getType() + "\n");
				sb.append("stext: " + body + "\n");
			}

			@Override
			public void text(final CharSequence text) {
				sb.append("text: " + text + "\n");
			}

		};
		final String html = "<script src=\"a.js\"/>\n" +
				"<link href=\"b.html\">\n" +
				"<script src=\"c.js\"></script>";
		LagartoParser parser = new LagartoParser(html);
		parser.parse(visitor);
		assertEquals("script: SELF_CLOSING\n" +
				"stext: \n" +
				"<link href=\"b.html\">\n" +
				"<script src=\"c.js\">\n", sb.toString());

		parser = new LagartoParser(html);
		final TagWriter tv = new TagWriter();
		parser.parse(tv);
		assertEquals("<script src=\"a.js\">\n" +
				"<link href=\"b.html\">\n" +
				"<script src=\"c.js\"></script>", tv.getOutput().toString());
	}

}
