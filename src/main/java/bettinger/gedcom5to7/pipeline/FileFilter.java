package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

public class FileFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if ("https://gedcom.io/terms/v7/FILE".equals(struct.uri)) {
			var url = struct.payload;

			if (url.contains("://")) {
				// URL schema://host.com/path/to/file.ext
			} else if (url.startsWith("\\\\")) {
				// Microsoft's network location notation \\server\path\to\file.ext
				url = "file:" + escapePath(url.replace('\\', '/'));
			} else if (url.matches("[A-Za-z]:\\\\.*")) {
				// Microsoft's absolute c:\path\to\file.ext
				url = "file:///" + url.charAt(0) + ":/" + escapePath(url.substring(3).replace('\\', '/'));
			} else if (url.startsWith("/")) {
				// POSIX's absolute /path/to/file.ext
				url = "file://" + escapePath(url);
			} else {
				// Microsoft's relative path\to\file.ext
				// POSIX's relative path/to/file.ext
				url = escapePath(url.replace('\\', '/'));
			}

			struct.payload = url;
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}

	private static String escapePath(final String url) {
		return url.replace("%", "%25").replace(":", "%3A").replace("?", "%3F").replace("#", "%23").replace("[", "%5B").replace("]", "%5D").replace("@", "%40");
	}
}
