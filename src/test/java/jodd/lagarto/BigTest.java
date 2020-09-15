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
import jodd.util.NaturalOrderComparator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigTest {

	@Test
	@Disabled
	void shouldParseAllHtmlPages() throws IOException {

		final List<Path> zipFiles;
		try {
			zipFiles = Files.list(Paths.get("../jodd-lagarto-data/"))
					.sorted(new NaturalOrderComparator<>())
					.collect(Collectors.toList());
		} catch (final NoSuchFileException ignore) {
			return;
		}

		int counter = 0;
		final List<String> failed = new LinkedList<>();

		for (final Path path : zipFiles) {
			System.out.println(path.getFileName() + " " + failed.size());
			final ZipFile zipFile = new ZipFile(path.toFile());
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				final String html = new String(
						IOUtil.readChars(zipFile.getInputStream(entry), StandardCharsets.UTF_8));

				try {
					new LagartoParser(html).parse(new EmptyTagVisitor());
				} catch (final Exception e) {
					failed.add(entry.getName());
				}

				counter++;
			}
			zipFile.close();
		}

		System.out.println("Total: " + counter);
		assertEquals(0, failed.size());
		failed.forEach(System.err::println);
	}
}
