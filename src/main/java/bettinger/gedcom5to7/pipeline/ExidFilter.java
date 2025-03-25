package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

public class ExidFilter implements Filter {
	private static final String TAG = "https://gedcom.io/terms/v7/TYPE";
	private static final String URI = "https://gedcom.io/terms/v7/EXID";

	public Collection<GedStruct> update(final GedStruct struct) {
		var changed = true;

		switch (struct.tag) {
			case "AFN":
				struct.uri = URI;
				new GedStruct(struct, TAG, "https://www.familysearch.org/wiki/en/Ancestral_File");
				break;
			case "RFN":
				struct.uri = URI;
				int colon = struct.payload.indexOf(':');
				if (colon < 0) {
					new GedStruct(struct, TAG, "https://gedcom.io/terms/v7/RFN");
				} else {
					new GedStruct(struct, TAG, "https://gedcom.io/terms/v7/RFN#" + struct.payload.substring(0, colon));
					struct.payload = struct.payload.substring(colon + 1);
				}
				break;
			case "RIN":
				struct.uri = "https://gedcom.io/terms/v7/REFN";
				new GedStruct(struct, TAG, "RIN");
				break;
			case "_FSFTID", "_FID", "FSFTID":
				struct.uri = URI;
				new GedStruct(struct, TAG, "https://www.familysearch.org/tree/person/");
				break;
			case "_APID":
				struct.uri = URI;
				new GedStruct(struct, TAG, "https://www.ancestry.com/family-tree/");
				break;
			// case "HISTID": // TODO: unclear what TYPE to give it
			default:
				changed = false;
		}

		if (changed)
			struct.tag2uri(false);

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
