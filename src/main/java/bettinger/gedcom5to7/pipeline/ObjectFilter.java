package bettinger.gedcom5to7.pipeline;

import java.util.Collection;
import java.util.LinkedList;

import bettinger.gedcom5to7.GedStruct;

/**
 * Convert OBJE substructures with no payload into a pointer to an OBJE record
 */
public class ObjectFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		final var newRecords = new LinkedList<GedStruct>();
		update(struct, newRecords);
		return newRecords;
	}

	private void update(final GedStruct struct, final Collection<GedStruct> newRecords) {
		if ("https://gedcom.io/terms/v7/OBJE".equals(struct.uri) && struct.pointsTo == null && struct.payload == null) {
			final var newRecord = new GedStruct(null, "OBJE");
			newRecords.add(newRecord);

			final var iterator = struct.sub.iterator();
			while (iterator.hasNext()) {
				final var subStruct = iterator.next();
				if (subStruct.uri == null) {
					newRecord.addSubstructure(subStruct);
					iterator.remove();
				}
			}

			struct.pointTo(newRecord);
			newRecord.tag2uri();
		}

		for (final var subStruct : struct.sub)
			update(subStruct, newRecords);
	}
}
