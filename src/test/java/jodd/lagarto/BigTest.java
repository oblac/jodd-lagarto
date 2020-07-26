package jodd.lagarto;

import jodd.io.StreamUtil;
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
						StreamUtil.readChars(zipFile.getInputStream(entry), StandardCharsets.UTF_8.name()));

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
