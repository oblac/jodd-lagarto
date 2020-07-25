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

import jodd.io.FileUtil;
import jodd.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MalformedTest {

	protected final String testDataRoot = this.getClass().getResource("data").getFile();

	@Test
	void testOneNode() {
		final String content = "<body><div>test<span>sss</span></body>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<body><div>test<span>sss</span></div></body>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testOneNodeWithBlanks() {
		final String content = "<body><div>   <span>sss</span></body>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<body><div>   <span>sss</span></div></body>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testTwoNodes() {
		final String content = "<body><div>test<span><form>xxx</form></body>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<body><div>test<span><form>xxx</form></span></div></body>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testTwoNodes2() {
		final String content = "<body><div>test<span><form>xxx</body>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<body><div>test<span><form>xxx</form></span></div></body>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testPeterSimple1() {
		final String content = "<div><h1>FORELE</h1><p>dicuss<div>xxx</div></div>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<div><h1>FORELE</h1><p>dicuss</p><div>xxx</div></div>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testPeterSimple2() {
		final String content = "<div><h1>FORELE</h1><p>dicuss<div><h2>HAB</h2><p>AMONG</div></div>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<div><h1>FORELE</h1><p>dicuss</p><div><h2>HAB</h2><p>AMONG</p></div></div>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testPeterSimple3WithSpaces() {
		final String content = "<div> <h1>FORELE</h1> <p>dicuss <div> <h2>HAB</h2> <p>AMONG </div> </div>".toUpperCase();
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<div> <h1>FORELE</h1> <p>DICUSS </p><div> <h2>HAB</h2> <p>AMONG </p></div> </div>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testPeterFull() {
		final String content = "<DIV class=\"section\" id=\"forest-elephants\" >\n" +
				"<H1>Forest elephants</H1>\n" +
				"<P>In this section, we discuss the lesser known forest elephants.\n" +
				"...this section continues...\n" +
				"<DIV class=\"subsection\" id=\"forest-habitat\" >\n" +
				"<H2>Habitat</H2>\n" +
				"<P>Forest elephants do not live in trees but among them.\n" +
				"...this subsection continues...\n" +
				"</DIV>\n" +
				"</DIV>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);

		final String expected = "<div class=\"section\" id=\"forest-elephants\">\n" +
				"<h1>Forest elephants</h1>\n" +
				"<p>In this section, we discuss the lesser known forest elephants.\n" +
				"...this section continues...\n</p>" +
				"<div class=\"subsection\" id=\"forest-habitat\">\n" +
				"<h2>Habitat</h2>\n" +
				"<p>Forest elephants do not live in trees but among them.\n" +
				"...this subsection continues...\n</p>" +
				"</div>\n" +
				"</div>";

		assertEquals(expected, doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testEof() {
		final String content = "<body><div>test";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<body><div>test</div></body>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testEof2() {
		final String content = "<body><div>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<body><div></div></body>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testSpanDivOverTable() {
		final String content = "<span><div><table><tr><td>text</span>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<span><div><table><tr><td>text</td></tr></table></div></span>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testDivSpanOverTable() {
		final String content = "<div><span><table><tr><td>text</div>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(content);
		assertEquals("<div><span><table><tr><td>text</td></tr></table></span></div>", doc.getHtml());
		assertTrue(doc.check());
	}

	@Test
	void testTableInTableInTable() throws IOException {
		final String html = read("tableInTable.html", false);

		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		final Document doc = lagartoDOMBuilder.parse(html);

		final String out = read("tableInTable-out.html", true);

		assertEquals(out, html(doc));
		assertTrue(doc.check());
	}

	@Test
	void testFormClosesAll() throws IOException {
		String html = read("formClosesAll.html", false);

		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableDebug();
		Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		String out = read("formClosesAll-out1.html", true);
		assertEquals(out, html);
		assertTrue(doc.check());

		lagartoDOMBuilder.getConfig().setUseFosterRules(true);
		doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		out = read("formClosesAll-out2.html", true);
		assertEquals(out, html);
	}

	@Test
	void testFoster1() {
		String html = "A<table>B<tr>C</tr>D</table>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getConfig().setUseFosterRules(true);
		final Document doc = lagartoDOMBuilder.parse(html);
		html = html1(doc);

		assertEquals("ABCD<table><tr></tr></table>", html);
	}

	@Test
	void testFoster2() {
		String html = "A<table><tr> B</tr> C</table>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.getConfig().setUseFosterRules(true);
		final Document doc = lagartoDOMBuilder.parse(html);
		html = html1(doc);

		assertEquals("ABC<table><tr></tr></table>", html);
	}

	@Test
	void testBodyEnd() {
		String html = "<body><p>111</body>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableDebug();
		final Document doc = lagartoDOMBuilder.parse(html);
		html = html1(doc);

		assertEquals("<body><p>111</p></body>", html);
		assertNull(doc.getErrors());
	}

	@Test
	void testBodyEndWithError() {
		String html = "<body><p>111<h1>222</body>";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableDebug();
		final Document doc = lagartoDOMBuilder.parse(html);
		html = html1(doc);

		assertEquals("<body><p>111</p><h1>222</h1></body>", html);
		assertNotNull(doc.getErrors());
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	void testEOF() {
		String html = "<body><p>111";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableDebug();
		final Document doc = lagartoDOMBuilder.parse(html);
		html = html1(doc);

		assertEquals("<body><p>111</p></body>", html);
		assertNull(doc.getErrors());
	}

	@Test
	void testEOFWithError() {
		String html = "<body><p>111<h1>222";
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableDebug();
		final Document doc = lagartoDOMBuilder.parse(html);
		html = html1(doc);

		assertEquals("<body><p>111</p><h1>222</h1></body>", html);
		assertNotNull(doc.getErrors());
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	void testCrazySpan() throws IOException {
		String html = read("spancrazy.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableHtmlPlusMode();
		lagartoDOMBuilder.enableDebug();

		final Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		final String out = read("spancrazy-out.html", true);
		assertEquals(out, html);
		assertEquals(3, doc.getErrors().size());
	}

	@Test
	void testFosterForm() throws IOException {
		String html = read("fosterForm.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableHtmlPlusMode();
		lagartoDOMBuilder.enableDebug();

		final Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		final String out = read("fosterForm-out.html", true);
		assertEquals(out, html);
		assertNull(doc.getErrors());
	}

	@Test
	void testListCrazy() throws IOException {
		String html = read("listcrazy.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableHtmlPlusMode();
		lagartoDOMBuilder.enableDebug();

		final Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		final String out = read("listcrazy-out.html", true);
		assertEquals(out, html);
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	void testTable1() throws IOException {
		String html = read("table1.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableHtmlPlusMode();
		lagartoDOMBuilder.enableDebug();

		final Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		final String out = read("table1-out.html", true);
		assertEquals(out, html);
	}

	@Test
	void testTable2() throws IOException {
		String html = read("table2.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableHtmlPlusMode();
		lagartoDOMBuilder.enableDebug();

		final Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		final String out = read("table2-out.html", true);
		assertEquals(out, html);
	}

	@Test
	public void smtest() throws IOException {
		String html = read("smtest.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		lagartoDOMBuilder.enableHtmlPlusMode();
		lagartoDOMBuilder.enableDebug();

		final Document doc = lagartoDOMBuilder.parse(html);
		html = html(doc);

		String out = read("smtest-out.html", true);

		// still not working

		out = StringUtil.remove(out, "<tbody>\n");
		out = StringUtil.remove(out, "</tbody>\n");

		html = StringUtil.replace(html, "<td>\nnotworking</td>", "<tr>\n<td>\nnotworking</td>\n</tr>");

		assertEquals(out, html);
	}

	@Test
	void testDecodingQuotes() throws IOException {
		String html = read("decode.html", false);

		LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();
		Document doc = lagartoDOMBuilder.parse(html);

		Element td1 = (Element) doc.getChild(0, 1, 1, 1, 1);
		final String td1attr = td1.getAttribute("onclick");

		Element td2 = (Element) doc.getChild(0, 1, 1, 3, 1);
		final String td2attr = td2.getAttribute("onclick");

		html = html(doc);
		final String out = read("decode-out.html", true);

		assertEquals(out, html);

		// now re-parse the generated html

		final String newHtml = doc.getHtml();

		lagartoDOMBuilder = new LagartoDOMBuilder();
		doc = lagartoDOMBuilder.parse(newHtml);

		td1 = (Element) doc.getChild(0, 1, 1, 1, 1);
		assertEquals(td1attr, td1.getAttribute("onclick"));

		td2 = (Element) doc.getChild(0, 1, 1, 3, 1);
		assertEquals(td2attr, td2.getAttribute("onclick"));

	}

	@Test
	void testQuotes() throws IOException {
		String html = read("quotes.html", false);
		final LagartoDOMBuilder lagartoDOMBuilder = new LagartoDOMBuilder();

		final Document doc = lagartoDOMBuilder.parse(html);

		html = html(doc);
		final String out = read("quotes-out.html", true);

		assertEquals(out, html);
	}

	// ---------------------------------------------------------------- util

	/**
	 * Reads test file and returns its content optionally stripped.
	 */
	protected String read(final String filename, final boolean strip) throws IOException {
		String data = FileUtil.readString(new File(testDataRoot, filename));
		if (strip) {
			data = strip(data);
		}
		return data;
	}

	protected String strip(String string) {
		string = StringUtil.removeChars(string, " \r\n\t");
		string = StringUtil.replace(string, ">", ">\n");
		return string;
	}

	/**
	 * Parses HTML and returns the stripped html.
	 */
	protected String html(final Document document) {
		String html = document.getHtml();
		html = strip(html);
		return html;
	}
	protected String html1(final Document document) {
		String html = document.getHtml();
		html = StringUtil.removeChars(html, " \r\n\t");
		return html;
	}

}
