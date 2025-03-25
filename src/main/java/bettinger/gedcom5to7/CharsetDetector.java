package bettinger.gedcom5to7;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CharsetDetector {
	private FileInputStream inputStream;
	private byte[] buffer;
	private int length;
	private int position;

	private CharsetDetector(final FileInputStream inputStream) {
		this.inputStream = inputStream;
		this.buffer = new byte[1024];
		this.position = 0;
		this.length = 0;
	}

	private int readNextByte() throws IOException {
		if (position < length)
			return buffer[position++] & 0xFF;

		length = inputStream.read(buffer);
		position = 0;

		return position < length ? buffer[position++] & 0xFF : -1;
	}

	/**
	 * A kind of half-way multi-string regex. Given a set of templates, read a byte
	 * at a time until one of them is found, returning the index of which was found
	 * or -1 if none were. Treats '\n' as /[\n\r]+/ and ' ' as /[ \t]+/ and
	 * lower-case ASCII as upper-case.
	 */
	private int findFirstOf(final String... needles) throws IOException {
		@SuppressWarnings("unchecked")
		final List<Integer>[] dots = new List[2];
		dots[0] = new ArrayList<>();
		dots[1] = new ArrayList<>();

		List<Integer> tmp;

		var b = readNextByte();
		while (b >= 0) {
			final var c1 = (char) b;

			for (int i = 0; i < needles.length; i += 1) {
				final var n = needles[i];
				var c = n.charAt(0);

				if (c == '\r')
					c = '\n';

				if (c == '\t')
					c = ' ';

				if (c >= 'a' && c <= 'z')
					c ^= 0x20;

				if (c == c1)
					dots[1].add((i << 16) | 1);
			}

			for (final var dot : dots[0]) {
				final var i = dot >> 16;
				final var j = dot & 0xFFFF;
				final var n = needles[i];
				var c = n.charAt(j);

				if (c == '\r')
					c = '\n';

				if (c == '\t')
					c = ' ';

				if (c >= 'a' && c <= 'z')
					c ^= 0x20;

				if (c == c1) {
					if (c == ' ' || c == '\n')
						dots[1].add((i << 16) | (j));

					dots[1].add((i << 16) | (1 + j));
				}
			}

			tmp = dots[0];
			dots[0] = dots[1];
			tmp.clear();
			dots[1] = tmp;

			for (final var dot : dots[0]) {
				final var i = dot >> 16;
				final var j = dot & 0xFFFF;
				final var n = needles[i];
				if (j == n.length())
					return i;
			}

			b = readNextByte();
		}

		return -1;
	}

	public static Charset detect(final String filename) {
		try (final FileInputStream inputStream = new FileInputStream(filename)) {
			final byte[] m = new byte[4];
			inputStream.read(m);

			if ((m[0] & 0xff) == 0xEF && (m[1] & 0xff) == 0xBB && (m[2] & 0xff) == 0xBF)
				return StandardCharsets.UTF_8;

			if ((m[0] & 0xff) == 0xFF && (m[1] & 0xff) == 0xFE)
				return StandardCharsets.UTF_16LE;

			if ((m[0] & 0xff) == 0xFE && (m[1] & 0xff) == 0xFF)
				return StandardCharsets.UTF_16BE;

			if (m[0] == 0)
				return StandardCharsets.UTF_16BE;

			if (m[1] == 0)
				return StandardCharsets.UTF_16LE;

			final var detector = new CharsetDetector(inputStream);

			var got = detector.findFirstOf("\n0", "\n1 CHAR");
			if (got < 1)
				return StandardCharsets.UTF_8;

			got = detector.findFirstOf("\n", "ASCII", "ANSEL");
			if (got < 1)
				return StandardCharsets.UTF_8;

			if (got == 1)
				return StandardCharsets.ISO_8859_1; // a non-standard but popular superset of ASCII

			return new AnselCharset();
		} catch (final IOException e) {
			e.printStackTrace();
			return StandardCharsets.UTF_8;
		}
	}
}
