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

import jodd.io.IOUtil;
import jodd.jerry.Jerry;
import jodd.jerry.JerryParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class StuckTest {

	protected final String testDataRoot = this.getClass().getResource("misc").getFile();

	@Test
	void testStuck() throws IOException {
		final File file = new File(testDataRoot, "stuck.html.gz");
		final InputStream in = new GZIPInputStream(new FileInputStream(file));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtil.copy(in, out);
		in.close();

		final JerryParser jerryParser = new JerryParser();
//		LagartoDOMBuilder lagartoDOMBuilder = (LagartoDOMBuilder) jerryParser.getDOMBuilder();
//		lagartoDOMBuilder.setParsingErrorLogLevelName("ERROR");
		final Jerry doc = jerryParser.parse(out.toString("UTF-8"));

		// parse
		try {
			doc.s("a").each(($this, index) -> {
				assertEquals("Go to Database Directory", $this.html().trim());
				return false;
			});
		} catch (final StackOverflowError stackOverflowError) {
			fail("stack overflow!");
		}
	}
}
