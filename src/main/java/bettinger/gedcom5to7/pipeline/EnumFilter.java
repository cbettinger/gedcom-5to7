package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;
import bettinger.gedcom5to7.GedcomDefinitions;

public class EnumFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		final var definitions = GedcomDefinitions.getDefinitions();
		final var payloadURI = definitions.payloadURI(struct.uri);

		if ("https://gedcom.io/terms/v7/type-Enum".equals(payloadURI)) {
			final var bit = struct.payload.trim().toUpperCase().replaceAll("[- ]+", "_");
			final var uri = definitions.enumURI(struct.uri, bit);

			if (uri == null) {
				new GedStruct(struct, "https://gedcom.io/terms/v7/PHRASE", struct.payload);
				struct.payload = definitions.enumURI(struct.uri, "OTHER") == null ? "_OTHER" : "OTHER";
			} else {
				struct.payload = bit;
			}
		} else if ("https://gedcom.io/terms/v7/type-List#Enum".equals(payloadURI)) {
			final var bits = struct.payload.split(",");

			var other = false;
			final var others = struct.payload;
			struct.payload = "";

			for (var bit : bits) {
				bit = bit.trim().toUpperCase();

				if (definitions.enumURI(struct.uri, bit) != null) {
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

				struct.payload = definitions.enumURI(struct.uri, "OTHER") == null ? "_OTHER" : "OTHER";
				new GedStruct(struct, "https://gedcom.io/terms/v7/PHRASE", others);
			}
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
