package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;
import bettinger.gedcom5to7.GedcomDefinitions;

public class LanguageFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if ((struct.tag != null) && struct.tag.equals("LANG")) {
			var lang = GedcomDefinitions.getDefinitions().langTag(struct.payload);

			if (lang == null)
				new GedStruct(struct, "https://gedcom.io/terms/v7/PHRASE", struct.payload);

			struct.payload = (lang == null) ? "und" : lang;
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
