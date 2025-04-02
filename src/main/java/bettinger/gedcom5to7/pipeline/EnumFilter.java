package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;
import bettinger.gedcom5to7.GedcomDefinitions;

public class EnumFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		final var payload = GedcomDefinitions.getPayload(struct.uri);

		if ("https://gedcom.io/terms/v7/type-Enum".equals(payload)) {
			final var bit = struct.payload.trim().toUpperCase().replaceAll("[- ]+", "_");
			final var uri = GedcomDefinitions.getEnum(struct.uri, bit);

			if (uri == null) {
				new GedStruct(struct, "https://gedcom.io/terms/v7/PHRASE", struct.payload);
				struct.payload = GedcomDefinitions.getEnum(struct.uri, "OTHER") == null ? "_OTHER" : "OTHER";
			} else {
				struct.payload = bit;
			}
		} else if ("https://gedcom.io/terms/v7/type-List#Enum".equals(payload)) {
			final var bits = struct.payload.split(",");

			var other = false;
			final var others = struct.payload;
			struct.payload = "";

			for (var bit : bits) {
				bit = bit.trim().toUpperCase();

				if (GedcomDefinitions.getEnum(struct.uri, bit) != null) {
					if (!struct.payload.isEmpty())
						struct.payload += ", ";

					struct.payload += bit;
				} else {
					other = true;
				}
			}

			if (other) {
				if (!struct.payload.isEmpty())
					struct.payload += ", ";

				struct.payload = GedcomDefinitions.getEnum(struct.uri, "OTHER") == null ? "_OTHER" : "OTHER";
				new GedStruct(struct, "https://gedcom.io/terms/v7/PHRASE", others);
			}
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
