package bettinger.gedcom5to7;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class GedcomDefinitions {

	private static GedcomDefinitions engine;

	public static GedcomDefinitions get() {
		if (engine == null)
			engine = new GedcomDefinitions();

		return engine;
	}

	private Map<String, String> tagOf;
	private Map<String, Set<String>> required;
	private Set<String> singular;

	private Map<String, String> cardinalities;

	private Map<String, String> payloads;

	private Map<String, String> enums;
	private Set<String> enumSet;

	private Map<String, String> substructures;
	private Set<String> structSet;

	private Map<String, String> languages;
	private Map<String, String> moreLanguages;

	private GedcomDefinitions() {
		tagOf = new HashMap<>();
		required = new HashMap<>();
		singular = new HashSet<>();

		cardinalities = readTSV(new Scanner(getClass().getResourceAsStream("/cardinalities.tsv")));
		cardinalities.forEach((k, v) -> {
			if (v.charAt(1) == '1') {
				final var k2 = k.split("\t");
				required.putIfAbsent(k2[0], new HashSet<>());
				required.get(k2[0]).add(k2[1]);
			}

			if (v.charAt(3) == '1')
				singular.add(k);
		});

		payloads = readTSV(new Scanner(getClass().getResourceAsStream("/payloads.tsv")));

		enums = toOldEnumFormat(readTSV(new Scanner(getClass().getResourceAsStream("/enumerations.tsv"))), readTSV2(new Scanner(getClass().getResourceAsStream("/enumerationsets.tsv"))));
		enumSet = new HashSet<>(enums.values());
		addTags(enums);

		substructures = readTSV(new Scanner(getClass().getResourceAsStream("/substructures.tsv")));
		substructures.put("\tHEAD", "HEAD pseudostructure"); // HARD-CODE based on substructures.tsv implementation
		structSet = new HashSet<>(substructures.values());
		addTags(substructures);

		languages = readTSV(new Scanner(getClass().getResourceAsStream("/languages.tsv")));
		for (final var entry : languages.entrySet()) { // remove trailing '*' from ELF's tsv
			final var value = entry.getValue();
			if (value.endsWith("*")) {
				languages.put(entry.getKey(), value.substring(0, value.length() - 1));
			}
		}
		moreLanguages = readTSV(new Scanner(getClass().getResourceAsStream("/all-languages.tsv")));
	}

	private void addTags(final Map<String, String> source) {
		for (final var entry : source.entrySet()) {
			final var tag = entry.getKey().split("\t")[1];
			final var value = entry.getValue();
			final var oldTag = tagOf.get(value);

			if (oldTag != null && !tag.equals(oldTag))
				throw new RuntimeException("ERROR: uri " + value + " has multiple tags\n\t- " + oldTag + "\n\t- " + tag);
			else if (oldTag == null)
				tagOf.put(value, tag);
		}
	}

	/**
	 * Looks up the URI of an enumeration based on the GEDCOM 7 spec
	 *
	 * @param uri the URI of the containing structure. use <code>null</code> for an
	 *            extension.
	 * @param tag the enumeration value
	 * @return the URI of the enumeration value, or <code>null</code> if unknown
	 */
	public String getEnum(final String uri, final String tag) {
		if (uri == null) {
			final var val = "https://gedcom.io/terms/v7/" + tag;
			if (enumSet.contains(val))
				return val;

			return null;
		} else {
			final var key = uri + '\t' + tag;
			return enums.get(key);
		}
	}

	/**
	 * Looks up the URI of an structure type based on the GEDCOM 7 spec
	 *
	 * @param uri the URI of the containing structure type use <code>""</code> for a
	 *            record and <code>null</code> for an extension.
	 * @param tag the tag of the structure
	 * @return the URI of the structure type, or <code>null</code> if unknown
	 */
	public String getStructure(final String uri, final String tag) {
		if (uri == null) {
			final var val = "https://gedcom.io/terms/v7/" + tag;
			if (structSet.contains(val))
				return val;

			return null;
		} else {
			final var key = uri + '\t' + tag;
			return substructures.get(key);
		}
	}

	/**
	 * Looks up the tag of a structure URI based on the GEDCOM 7 spec
	 *
	 * @param uri the URI of the structure type
	 * @return the tag of the structure type, or <code>null</code> if unknown
	 */
	public String getTag(final String uri) {
		return uri == null ? null : tagOf.get(uri);
	}

	/**
	 * Looks up the payload type of a structure based on the GEDCOM 7 spec
	 *
	 * @param uri the URI of the containing structure type
	 * @return the type code (URI or <code>"Y|<NULL>"</code> or <code>""</code> or
	 *         <code>"@XREF:</code>tag<code>"</code>) of the payload type, or
	 *         <code>null</code> if unknown
	 */
	public String getPayload(final String uri) {
		return uri == null ? null : payloads.get(uri);
	}

	/**
	 * Looks up the language tag type of a language based ELF's mapping
	 *
	 * @param lang the 5.5.1 language name
	 * @return the BCP-47 language tag, or <code>null</code> if unknown
	 */
	public String getLanguage(final String languageName) {
		if (languageName == null)
			return null;

		var ans = languages.get(languageName);
		if (ans == null)
			ans = moreLanguages.get(languageName);

		return ans;
	}

	private static Map<String, String> readTSV(final Scanner scanner) {
		final Map<String, String> result = new HashMap<>();

		while (scanner.hasNextLine()) {
			final var line = scanner.nextLine();

			final var lastIndex = line.lastIndexOf('\t');
			if (lastIndex < 0)
				continue;

			result.put(line.substring(0, lastIndex), line.substring(lastIndex + 1));
		}

		return result;
	}

	private static Map<String, Set<String>> readTSV2(final Scanner scanner) {
		final Map<String, Set<String>> result = new HashMap<>();

		while (scanner.hasNextLine()) {
			final var line = scanner.nextLine();

			final var lastIndex = line.lastIndexOf('\t');
			if (lastIndex < 0)
				continue;

			final var key = line.substring(0, lastIndex);
			result.computeIfAbsent(key, _ -> new HashSet<>());
			result.get(key).add(line.substring(lastIndex + 1));
		}

		return result;
	}

	/**
	 * enumerations.tsv was split into two files after most of this code was
	 * written. This placeholder function replicates the old format from the new.
	 */
	private static Map<String, String> toOldEnumFormat(Map<String, String> enums, Map<String, Set<String>> enumSets) {
		final var result = new HashMap<String, String>();

		for (final var entry : enums.entrySet()) {
			for (final var tag : enumSets.get(enums.get(entry.getKey()))) {
				var tagIndex = tag.lastIndexOf('-');
				if (tagIndex < 0)
					tagIndex = tag.lastIndexOf('/');

				result.put(entry.getKey() + '\t' + tag.substring(tagIndex + 1), tag);
			}
		}

		return result;
	}
}
