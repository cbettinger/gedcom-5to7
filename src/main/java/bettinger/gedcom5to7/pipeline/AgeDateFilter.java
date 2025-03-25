package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedAge;
import bettinger.gedcom5to7.GedDateValue;
import bettinger.gedcom5to7.GedStruct;

public class AgeDateFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		String phrase = null;

		if (struct.tag.equals("AGE") && struct.payload != null) {
			final GedAge age = GedAge.from551(struct.payload);
			struct.payload = age.getPayload();
			phrase = age.getPhrase();
		} else if (struct.tag.equals("DATE") && struct.payload != null) {
			final GedDateValue date = GedDateValue.from551(struct.payload);
			struct.payload = date.getPayload();
			phrase = date.getPhrase();
		}

		if (phrase != null && !phrase.isEmpty())
			new GedStruct(struct, "PHRASE", phrase);

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
