package jodd.lagarto;

import jodd.lagarto.visitor.TagWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagWriterTest {

	@Test
	void testScript_selfClosed() {
		final String scriptText = "<script src=\"abc.js\"/>";
		final LagartoParser parser = new LagartoParser(scriptText);

		final StringBuilder stringBuilder = new StringBuilder();
		final TagVisitor vistor = new TagWriter(stringBuilder);
		parser.parse(vistor);

		assertEquals("<script src=\"abc.js\"></script>", stringBuilder.toString());
	}

	@Test
	void testScript_empty() {
		final String scriptText = "<script src=\"abc.js\"></script>";
		final LagartoParser parser = new LagartoParser(scriptText);

		final StringBuilder stringBuilder = new StringBuilder();
		final TagVisitor vistor = new TagWriter(stringBuilder);
		parser.parse(vistor);

		assertEquals("<script src=\"abc.js\"></script>", stringBuilder.toString());
	}


	@Test
	void testScript_full() {
		final String scriptText = "<script src=\"abc.js\">var s;</script>";
		final LagartoParser parser = new LagartoParser(scriptText);

		final StringBuilder stringBuilder = new StringBuilder();
		final TagVisitor vistor = new TagWriter(stringBuilder);
		parser.parse(vistor);

		assertEquals("<script src=\"abc.js\">var s;</script>", stringBuilder.toString());
	}

	@Test
	void testStyle() {
		final String html = "<style>\n" +
				"    /* -------------------------------------\n" +
				"        BODY & CONTAINER <do not escape>;\n" +
				"    ------------------------------------- */\n" +
				"    .btn > tbody > tr > td {\n" +
				"        padding-bottom: 15px; \n" +
				"    }\n" +
				"</style>";

		final LagartoParser parser = new LagartoParser(html);

		final StringBuilder stringBuilder = new StringBuilder();
		final TagVisitor vistor = new TagWriter(stringBuilder);
		parser.parse(vistor);

		assertEquals(html, stringBuilder.toString());

	}

}
