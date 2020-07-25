package jodd.lagarto;

import jodd.util.BinarySearchBase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HtmlDecoder {
	private static final Map<String, char[]> ENTITY_MAP;
	private static final char[][] ENTITY_NAMES;

	static {
		final Properties entityReferences = new Properties();

		final String propertiesName = HtmlDecoder.class.getSimpleName() + ".properties";

		try (final InputStream is = HtmlDecoder.class.getResourceAsStream(propertiesName)) {
			entityReferences.load(is);
		} catch (final Exception ex) {
			throw new IllegalStateException("Can't load properties", ex);
		}

		ENTITY_MAP = new HashMap<>(entityReferences.size());

		final Enumeration<String> keys = (Enumeration<String>) entityReferences.propertyNames();
		while (keys.hasMoreElements()) {
			final String name = keys.nextElement();
			final String values = entityReferences.getProperty(name);
			final String[] array = values.split(",");

			final char[] chars;

			final String hex = array[0];
			final char value = (char) Integer.parseInt(hex, 16);

			if (array.length == 2) {
				final String hex2 = array[1];
				final char value2 = (char) Integer.parseInt(hex2, 16);

				chars = new char[]{value, value2};
			} else {
				chars = new char[]{value};
			}

			ENTITY_MAP.put(name, chars);
		}

		// create sorted list of entry names

		ENTITY_NAMES = new char[ENTITY_MAP.size()][];

		int i = 0;
		for (final String name : ENTITY_MAP.keySet()) {
			ENTITY_NAMES[i++] = name.toCharArray();
		}

		Arrays.sort(ENTITY_NAMES, Comparator.comparing(String::new));
	}

	/**
	 * Decodes HTML text. Assumes that all character references are properly closed with semi-colon.
	 */
	public static String decode(final String html) {

		int ndx = html.indexOf('&');
		if (ndx == -1) {
			return html;
		}

		final StringBuilder result = new StringBuilder(html.length());

		int lastIndex = 0;
		final int len = html.length();
		mainloop:
		while (ndx != -1) {
			result.append(html, lastIndex, ndx);

			lastIndex = ndx;
			while (html.charAt(lastIndex) != ';') {
				lastIndex++;
				if (lastIndex == len) {
					lastIndex = ndx;
					break mainloop;
				}
			}

			if (html.charAt(ndx + 1) == '#') {
				// decimal/hex
				final char c = html.charAt(ndx + 2);
				final int radix;
				if ((c == 'x') || (c == 'X')) {
					radix = 16;
					ndx += 3;
				} else {
					radix = 10;
					ndx += 2;
				}

				final String number = html.substring(ndx, lastIndex);
				final int i = Integer.parseInt(number, radix);
				result.append((char) i);
				lastIndex++;
			} else {
				// token
				final String encodeToken = html.substring(ndx + 1, lastIndex);

				final char[] replacement = ENTITY_MAP.get(encodeToken);
				if (replacement == null) {
					result.append('&');
					lastIndex = ndx + 1;
				} else {
					result.append(replacement);
					lastIndex++;
				}
			}
			ndx = html.indexOf('&', lastIndex);
		}
		result.append(html.substring(lastIndex));
		return result.toString();
	}

	private static final class Ptr {
		public int offset;
		public char c;
	}

	/**
	 * Detects the longest character reference name on given position in char array.
	 * Returns {@code null} if name not found.
	 */
	public static String detectName(final CharSequence input, int ndx) {
		final Ptr ptr = new Ptr();

		int firstIndex = 0;
		int lastIndex = ENTITY_NAMES.length - 1;
		final int len = input.length();
		char[] lastName = null;

		final BinarySearchBase binarySearch = new BinarySearchBase() {
			@Override
			protected int compare(final int index) {
				final char[] name = ENTITY_NAMES[index];

				if (ptr.offset >= name.length) {
					return -1;
				}

				return name[ptr.offset] - ptr.c;
			}
		};

		while (true) {
			ptr.c = input.charAt(ndx);

			if (!isAlphaOrDigit(ptr.c)) {
				return lastName != null ? new String(lastName) : null;
			}

			firstIndex = binarySearch.findFirst(firstIndex, lastIndex);
			if (firstIndex < 0) {
				return lastName != null ? new String(lastName) : null;
			}

			final char[] element = ENTITY_NAMES[firstIndex];

			if (element.length == ptr.offset + 1) {
				// total match, remember position, continue for finding the longer name
				lastName = ENTITY_NAMES[firstIndex];
			}

			lastIndex = binarySearch.findLast(firstIndex, lastIndex);

			if (firstIndex == lastIndex) {
				// only one element found, check the rest
				for (int i = ptr.offset; i < element.length; i++) {
					if (ndx == input.length() || element[i] != input.charAt(ndx)) {
						return lastName != null ? new String(lastName) : null;
					}
					ndx++;
				}
				return new String(element);
			}

			ptr.offset++;

			ndx++;
			if (ndx == len) {
				return lastName != null ? new String(lastName) : null;
			}
		}
	}

	private static boolean isAlphaOrDigit(final char c) {
		return (c >= '0' && c <= '9') || ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
	}


}
