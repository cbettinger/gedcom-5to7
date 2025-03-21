package bettinger.gedcom5to7;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses GEDCOM 5.5.1 age payloads and converts them into GEDCOM 7.0 age
 * payloads.
 */
public class GedAge {

	private static final Pattern SYNTAX = Pattern.compile("(?:\\s*(?:(CHILD)|(INFANT)|(STILLBORN))\\s*)|(?:\\s*([<>]))?((?:\\s*\\d+\\s*[ymwd]?)+)\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern SYNTAX_PART = Pattern.compile("(\\d+)\\s*([ymwd]?)", Pattern.CASE_INSENSITIVE);

	private char modifier;
	private int year;
	private int month;
	private int week;
	private int day;
	private String phrase;

	private GedAge() {
		this('\0', -1, -1, -1, -1, null);
	}

	private GedAge(final char modifier, final int year, final int month, final int week, final int day, final String phrase) {
		this.modifier = modifier;
		this.year = year;
		this.month = month;
		this.week = week;
		this.day = day;
		this.phrase = phrase;
	}

	public static GedAge from551(final String payload) {
		Matcher matcher = SYNTAX.matcher(payload);

		if (!matcher.matches())
			return new GedAge('\0', -1, -1, -1, -1, payload);

		if (matcher.group(1) != null)
			return new GedAge('<', 8, -1, -1, -1, payload);

		if (matcher.group(2) != null)
			return new GedAge('<', 1, -1, -1, -1, payload);

		if (matcher.group(3) != null)
			return new GedAge('\0', 0, -1, -1, -1, payload);

		final GedAge result = new GedAge();

		if (matcher.group(4) != null)
			result.modifier = matcher.group(4).charAt(0);

		matcher = SYNTAX_PART.matcher(matcher.group(5));

		while (matcher.find()) {
			final String mode = matcher.group(2).toUpperCase();
			final int value = Integer.parseInt(matcher.group(1));

			if (mode.equals("M"))
				result.month = value;
			else if (mode.equals("W"))
				result.week = value;
			else if (mode.equals("D"))
				result.day = value;
			else
				result.year = value;
		}

		return result;
	}

	public String getPayload() {
		return ((modifier != '\0' ? modifier + " " : "") + (year >= 0 ? year + "y " : "") + (month >= 0 ? month + "m " : "") + (week >= 0 ? week + "w " : "") + (day >= 0 ? day + "d " : "")).trim();
	}

	public String getPhrase() {
		return phrase;
	}
}
