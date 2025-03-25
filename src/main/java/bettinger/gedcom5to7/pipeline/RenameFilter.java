package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

public class RenameFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if ("EMAI".equals(struct.tag) || "_EMAIL".equals(struct.tag)) {
			struct.uri = "https://gedcom.io/terms/v7/EMAIL";
			struct.tag2uri(false);
		} else if ("TYPE".equals(struct.tag) && struct.sup != null && "https://gedcom.io/terms/v7/FORM".equals(struct.sup.uri)) {
			struct.uri = "https://gedcom.io/terms/v7/MEDI";
			struct.tag2uri(false);
		} else if ("_UID".equals(struct.tag)) {
			struct.uri = "https://gedcom.io/terms/v7/UID";
			struct.tag2uri(false);
		} else if ("_ASSO".equals(struct.tag)) {
			if (struct.pointsTo == null) {
				new GedStruct(struct, "https://gedcom.io/terms/v7/PHRASE", struct.payload);
				struct.pointsTo = GedStruct.VOID;
			}
			struct.uri = "https://gedcom.io/terms/v7/ASSO";
			struct.tag2uri(false);
		} else if ("_CRE".equals(struct.tag) || "_CREAT".equals(struct.tag)) {
			struct.uri = "https://gedcom.io/terms/v7/CREA";
			struct.tag2uri(false);
		} else if ("_DATE".equals(struct.tag)) {
			struct.tag = "DATE"; // needed for AgeDateFilter
			struct.uri = "https://gedcom.io/terms/v7/DATE";
			struct.tag2uri(false);
		} else if ("RELA".equals(struct.tag) && struct.sup != null && "https://gedcom.io/terms/v7/ASSO".equals(struct.sup.uri)) {
			struct.uri = "https://gedcom.io/terms/v7/ROLE";
			struct.tag2uri(false);
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
