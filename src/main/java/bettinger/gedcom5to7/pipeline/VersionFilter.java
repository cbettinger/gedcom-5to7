package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

/**
 * Updates required HEAD fields. In 5.5.1 that was GEDC.VERS, GEDC.FORM and
 * CHAR. In 7.0 it's just GEDC.VERS, which is recommended to come first.
 */
public class VersionFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if (struct.tag.equals("HEAD")) {
			struct.sub.removeIf(s -> s.tag.equals("GEDC") || s.tag.equals("CHAR") || s.tag.equals("SUBN") || s.tag.equals("FILE"));

			final var gedc = new GedStruct(null, "https://gedcom.io/terms/v7/GEDC", (String) null);
			new GedStruct(gedc, "https://gedcom.io/terms/v7/GEDC-VERS", "7.0");

			struct.sub.addFirst(gedc);
			gedc.sup = struct;
		}

		if (struct.tag.equals("SUBN")) {
			struct.sup = struct;
		} // TODO: delete SUBN records

		return new ArrayList<>();
	}
}
