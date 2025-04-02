package bettinger.gedcom5to7;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GedStruct {
	public GedStruct sup;
	public List<GedStruct> sub;
	public int level;
	public String tag;
	public String id;
	public String payload;
	public String uri;
	public Collection<GedStruct> incoming;
	public GedStruct pointsTo;

	private static final Pattern GED_LINE = Pattern.compile("﻿?\\s*([0-9]+)\\s+(?:(@[^@]+@)\\s+)?([A-Za-z0-9_]+)(?:\\s([^\\n\\r]*))?[\\n\\r]*");
	private static final Pattern FUZZY_XREF = Pattern.compile("\\s*@[^#@][^@]*@\\s*");
	private static final Pattern AT_DETECTOR = Pattern.compile("^\\s*(@[^@#][^@]*@)\\s*$|" + "(@@)|" + "@#D([^@]*)@ ?|" + "@#([^D@][^@]*)@ ?");

	public static final GedStruct VOID;

	static {
		VOID = new GedStruct(null, "");
		VOID.id = "@VOID@";
	}

	/**
	 * Parse a line from a GEDCOM file into a basic GedStruct @throws@
	 * IllegalArgumentException if the line cannot be parsed
	 */
	public GedStruct(final String line) {
		final var matcher = GED_LINE.matcher(line);
		if (!matcher.matches())
			throw new IllegalArgumentException("No GEDCOM found in:\n" + line);

		sub = new LinkedList<>();
		level = Integer.parseInt(matcher.group(1));
		if (matcher.group(2) != null) {
			id = matcher.group(2).toUpperCase();
			if ("@VOID@".equals(id))
				throw new IllegalArgumentException("@VOID@ must not be used as a record identifier in:\n" + line);
		}

		tag = matcher.group(3).toUpperCase();
		payload = fixAtSign(matcher.group(4));
	}

	public GedStruct(final GedStruct sup, final String tag) {
		this.sub = new LinkedList<>();

		if (tag.indexOf(':') < 0)
			this.tag = tag;
		else {
			this.uri = tag;
			this.uri2tag();
		}

		if (sup != null) {
			sup.addSubstructure(this);
			this.level = sup.level + 1;
		} else {
			this.sup = null;
			this.level = 0;
		}
	}

	public GedStruct(final GedStruct sup, final String tag, final String payload) {
		this.sub = new LinkedList<>();

		if (tag.indexOf(':') < 0)
			this.tag = tag;
		else {
			this.uri = tag;
			this.uri2tag();
		}

		this.payload = payload;

		if (sup != null) {
			sup.addSubstructure(this);
			this.level = sup.level + 1;
		} else {
			this.sup = null;
			this.level = 0;
		}
	}

	public GedStruct(final GedStruct sup, final String tag, final GedStruct payload) {
		this.sub = new LinkedList<>();

		if (tag.indexOf(':') < 0)
			this.tag = tag;
		else {
			this.uri = tag;
			this.uri2tag();
		}

		this.pointsTo = payload;

		if (payload == null)
			this.pointsTo = VOID;
		else if (payload.incoming != null)
			payload.incoming.add(this);
		else {
			payload.incoming = new LinkedList<>();
			payload.incoming.add(this);
		}

		if (sup != null) {
			sup.addSubstructure(this);
			this.level = sup.level + 1;
		} else {
			this.sup = null;
			this.level = 0;
		}
	}

	private static String fixAtSign(final String payload) {
		if (payload == null || payload.isEmpty())
			return null;

		String result = "";

		final var matcher = AT_DETECTOR.matcher(payload);
		int lastidx = 0;
		while (matcher.find()) {
			if (matcher.group(1) != null)
				return matcher.group(1); // pointer
			if (matcher.group(2) != null) { // @@
				result += payload.substring(lastidx, matcher.start() + 1);
			} else if (matcher.group(3) != null) { // date
				result += payload.substring(lastidx, matcher.start());
				result += matcher.group(3).replaceAll("\\s", "_");
				result += ' ';
			} else if (matcher.group(4) != null) {
				result += payload.substring(lastidx, matcher.start());
				// skip non-date escape, which have been invalid since 5.3
			} else {
				throw new UnsupportedOperationException("Impossible: \"" + matcher.group(0) + "\" matched the AT_DETECTOR");
			}

			lastidx = matcher.end();
		}

		result += payload.substring(lastidx);

		return result;
	}

	public void tag2uri() {
		tag2uri(true);
	}

	public void tag2uri(final boolean replaceSelf) {
		final var definitions = GedcomDefinitions.get();
		if (uri == null || replaceSelf) {
			if (sup == null)
				uri = definitions.getStructure("", tag);
			else if (sup.uri == null || definitions.getTag(sup.uri) == null)
				uri = definitions.getStructure(null, tag);
			else
				uri = definitions.getStructure(sup.uri, tag);
		}

		for (final var subStructure : sub)
			subStructure.tag2uri(replaceSelf);
	}

	public void uri2tag() {
		if (uri != null) {
			final var tag2 = GedcomDefinitions.get().getTag(uri);
			if (tag2 != null)
				tag = tag2;
		}

		for (final var subStruct : sub)
			subStruct.uri2tag();
	}

	/**
	 * Adds a substructure to this structure. If the substructure is a CONT or CONC,
	 * this operation modifies the payload; otherwise it adds bidirectional linking
	 * between the structures. Because substructure order is significant, this
	 * method must be called in the order substructures appear in the file.
	 */
	public boolean addSubstructure(final GedStruct substructure) {
		if ("CONT".equals(substructure.tag)) {
			payload += "\n" + substructure.payload;
			return false;
		} else if ("CONC".equals(substructure.tag)) {
			payload += substructure.payload;
			return false;
		} else {
			sub.add(substructure);
			substructure.sup = this;
			return true;
		}
	}

	/**
	 * Given a map from xref_id strings to their parsed structures, populates the
	 * <code>pointsTo</code> fields of this struct and its substructures. If
	 * <code>cleanup</code> is <code>true</code>, also clears old xref_id payloads
	 * and replaces dead pointers with <code"@VOID@"</code>.
	 */
	@SuppressWarnings({ "unchecked" })
	public void convertPointers(final Map<String, GedStruct> xrefs, final boolean cleanup, final Class<?> cachetype) {
		if (payload != null) {
			final var to = xrefs.get(this.payload);

			if (to != null) {
				if (cachetype != null) {
					try {
						if (to.incoming == null)
							to.incoming = (Collection<GedStruct>) cachetype.getConstructor().newInstance();

						to.incoming.add(this);
					} catch (Exception e) {
						// intentionally left blank
					}
				}

				pointsTo = to;

				if (cleanup)
					payload = null;
			} else if (cleanup && FUZZY_XREF.matcher(payload).matches()) {
				pointsTo = VOID;	// TODO: log this workaround
			}
		}

		for (final var subStruct : sub)
			subStruct.convertPointers(xrefs, cleanup, cachetype);
	}

	public void pointTo(final GedStruct struct) {
		if (pointsTo == struct)
			return;

		if (pointsTo != null && pointsTo.incoming != null)
			pointsTo.incoming.remove(this);

		pointsTo = struct;

		if (pointsTo != null) {
			if (pointsTo.incoming == null)
				pointsTo.incoming = new LinkedList<>();

			pointsTo.incoming.add(this);
		} else {
			pointsTo = VOID;
		}
	}

	/**
	 * Serializes as GEDCOM. Advanced data overrides primitive data: for example,
	 * superstructure supersedes level, pointers supersede payloads, etc.
	 */
	public String toString() {
		final var sb = new StringBuilder();
		serialize(sb);
		return sb.toString();
	}

	void serialize(final StringBuilder sb) {
		if (sup != null)
			level = sup.level + 1;

		sb.append(level);
		sb.append(' ');

		// TODO: add ID addition if no ID but pointed to
		if (level == 0 && id != null) {
			sb.append(id);
			sb.append(' ');
		}

		if (uri == null && !"CONT".equals(tag) && !"TRLR".equals(tag))
			sb.append("_EXT_");

		sb.append(tag);

		if (pointsTo != null) {
			// TODO: add ID addition if point to non-ID struct
			sb.append(' ');
			sb.append(pointsTo.id);
		} else if (payload != null) {
			sb.append(' ');
			sb.append(payload.replaceAll("^@|\\n@|\\r@", "$0@").replaceAll("\r\n?|\n", "\n" + (level + 1) + " CONT "));
		}

		//if (uri != null) sb.append("    <"+uri+">");

		sb.append("\n");

		for (final var subStruct : sub)
			subStruct.serialize(sb);
	}
}
