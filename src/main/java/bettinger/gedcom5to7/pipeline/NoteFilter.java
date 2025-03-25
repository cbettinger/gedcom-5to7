package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

/**
 * Convert NOTE records with 0 or 2+ incoming pointers to SNOTE records. Convert
 * NOTE substructures pointing to SNOTE records to SNOTE substructures. Convert
 * NOTE substructures pointing to NOTE records to inlined copy of the NOTE
 * record.
 */
public class NoteFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if ("NOTE".equals(struct.tag)) {
			if (struct.incoming == null || struct.incoming.size() != 1) {
				struct.tag = "SNOTE";
				struct.tag2uri();

				if (struct.incoming != null)
					for (final var ref : struct.incoming) {
						ref.tag = "SNOTE";
						ref.tag2uri();
					}
			} else {
				final var ref = struct.incoming.iterator().next();
				ref.payload = struct.payload;
				ref.pointsTo = null;

				for (final var subStruct : struct.sub)
					ref.addSubstructure(subStruct);

				ref.uri = null;
				ref.tag2uri();

				struct.sup = struct; // because s.sup != null causes it to be removed in Converter5to7
				struct.sub.clear();
			}
		}

		return new ArrayList<>();
	}
}
