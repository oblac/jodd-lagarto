package jodd.lagarto;

import jodd.jerry.Jerry;
import jodd.lagarto.adapter.StripHtmlTagAdapter;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.LagartoDOMBuilder;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.Text;
import jodd.lagarto.visitor.TagWriter;
import jodd.mutable.MutableInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the code that appears in the documentation.
 * Test methods are named following this pattern:
 * section_filesindex
 */
class DocumentationTest {

	@Test
	void test_1_1() {
		final MutableInteger count = MutableInteger.of(0);
		final LagartoParser lagartoParser = new LagartoParser("<html><h1>Hello</h1></html>");

		final TagVisitor tagVisitor = new EmptyTagVisitor() {
			@Override
			public void tag(final Tag tag) {
				if (tag.nameEquals("h1")) {
					System.out.println(tag.getName());
					count.value++;
				}
			}

			@Override
			public void text(final CharSequence text) {
				System.out.println(text);
				count.value++;
			}
		};

		lagartoParser.parse(tagVisitor);

		assertEquals(3, count.value);
	}

	@Test
	void test_1_3() {
		{
			final LagartoParserConfig cfg = new LagartoParserConfig().setCaseSensitive(true);
			final LagartoParser lagartoParser = new LagartoParser(cfg, "<html>");

			assertTrue(lagartoParser.getConfig().isCaseSensitive());
		}
		{
			final LagartoParser lagartoParser = new LagartoParser("<html>").configure(cfg -> {
				cfg.setCaseSensitive(true);
			});
			assertTrue(lagartoParser.getConfig().isCaseSensitive());
		}
	}

	@Test
	void test_1_4() {
		final TagWriter tagWriter = new TagWriter();
		final StripHtmlTagAdapter adapter = new StripHtmlTagAdapter(tagWriter);
		final LagartoParser lagartoParser = new LagartoParser("<html> <h1>  Hello  </h1> </html>");
		lagartoParser.parse(adapter);

		assertEquals("<html><h1> Hello </h1></html>", tagWriter.getOutput().toString());
	}

	@Test
	void test_2_1() {
		final Document document = new LagartoDOMBuilder().parse("<html><h1>Hello</h1></html>");

		final Node html = document.getChild(0);
		final Node h1 = html.getFirstChild();

		System.out.println(h1.getTextContent());
		assertEquals("Hello", h1.getTextContent());

		final Text text = (Text) h1.getFirstChild();
		System.out.println(text.getTextValue());
		assertEquals("Hello", text.getTextValue());

		System.out.println(text.getCssPath());
		assertEquals("html h1", text.getCssPath());
	}

	@Test
	void test_3_1() {
		final Jerry doc = Jerry.of("<html><div id='jodd'><b>Hello</b> Jerry</div></html>");

		doc.s("div#jodd b").css("color", "red").addClass("ohmy");

		System.out.println(doc.html());
		assertEquals("<html><div id=\"jodd\"><b style=\"color:red;\" class=\"ohmy\">Hello</b> Jerry</div></html>", doc.html());
	}

	void test_3_2() {
		Jerry.of("html")
				.s("select option:selected")
				.each(($this, index) -> {
					System.out.println($this.text());
					return true;
				});

		Jerry.of("html")
				.form("#myform", (form, parameters) -> {
					// process form and parameters
				});
	}
}
