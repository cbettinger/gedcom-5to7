package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

public class MediaTypeFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if ("https://gedcom.io/terms/v7/FORM".equals(struct.uri)) {
			switch (struct.payload.toLowerCase()) {
				case "gif":
					struct.payload = "image/gif";
					break;
				case "jpg", "jpeg":
					struct.payload = "image/jpeg";
					break;
				case "tif", "tiff":
					struct.payload = "image/tiff";
					break;
				case "bmp":
					struct.payload = "image/bmp";
					break;
				case "ole":
					struct.payload = "application/x-oleobject";
					break;
				case "pcx":
					struct.payload = "application/vnd.zbrush.pcx";
					break;
				case "wav":
					struct.payload = "audio/vnd.wave";
					break;
				default:
					struct.payload = "application/x-" + struct.payload;
					break;
			}
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
