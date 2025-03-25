package bettinger.gedcom5to7.pipeline;

import java.util.Collection;
import java.util.LinkedList;

import bettinger.gedcom5to7.GedStruct;

/**
 * Two changes to SOUR <Text>: 1. Create a new SOUR record with a NOTE <Text>.
 * 2. Put any TEXT substructures inside a new DATA substructure.
 */
public class SourceFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		final var newRecords = new LinkedList<GedStruct>();
		update(struct, newRecords);
		return newRecords;
	}

	private void update(final GedStruct struct, final Collection<GedStruct> newRecords) {
		if ("https://gedcom.io/terms/v7/SOUR".equals(struct.uri) && struct.payload != null) {
			final var sour = new GedStruct(null, "https://gedcom.io/terms/v7/record-SOUR");
			new GedStruct(sour, "https://gedcom.io/terms/v7/NOTE", struct.payload);
			newRecords.add(sour);

			struct.payload = null;
			struct.pointTo(sour);

			final var data = new GedStruct(null, "https://gedcom.io/terms/v7/SOUR-DATA");

			for (final var subStruct : struct.sub)
				if ("TEXT".equals(subStruct.tag)) {
					subStruct.uri = "https://gedcom.io/terms/v7/TEXT";
					data.addSubstructure(subStruct);
				}

			if (!data.sub.isEmpty()) {
				struct.sub.removeIf(s2 -> "TEXT".equals(s2.tag));
				struct.addSubstructure(data);
			}
		}

		for (final var subStruct : struct.sub)
			update(subStruct, newRecords);
	}
}
