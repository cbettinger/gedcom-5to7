package bettinger.gedcom5to7;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import bettinger.gedcom5to7.pipeline.AgeDateFilter;
import bettinger.gedcom5to7.pipeline.EnumFilter;
import bettinger.gedcom5to7.pipeline.ExidFilter;
import bettinger.gedcom5to7.pipeline.FileFilter;
import bettinger.gedcom5to7.pipeline.Filter;
import bettinger.gedcom5to7.pipeline.LanguageFilter;
import bettinger.gedcom5to7.pipeline.MediaTypeFilter;
import bettinger.gedcom5to7.pipeline.NoteFilter;
import bettinger.gedcom5to7.pipeline.ObjectFilter;
import bettinger.gedcom5to7.pipeline.RenameFilter;
import bettinger.gedcom5to7.pipeline.SourceFilter;
import bettinger.gedcom5to7.pipeline.TranFilter;
import bettinger.gedcom5to7.pipeline.VersionFilter;

public class Converter {
	private final int idBase;
	private final int idToSkip;
	private int lastId;

	private List<GedStruct> records;

	private List<String> errorLog;

	/**
	 * Parses file using error-tolerant algorithm and performs full 5to7 conversion.
	 * Record IDs are assigned as sequential base-<code>idBase</code> integers.
	 */
	private Converter(String filename, int idBase) {
		if (idBase < 2 || idBase > 36)
			throw new IllegalArgumentException("idBse must be between 2 and 36");

		this.idBase = idBase;
		this.idToSkip = idBase > 'V' - 'A' + 10 ? Integer.parseInt("VOID", idBase) : -1;
		this.lastId = -1;

		this.records = new LinkedList<>();

		this.errorLog = new LinkedList<>();

		fuzzyParse(filename);

		GedStruct trlr = records.removeLast();
		if (!"TRLR".equals(trlr.tag)) {
			records.add(trlr);
			trlr = new GedStruct(null, "TRLR");
		}

		for (final GedStruct s : records)
			s.tag2uri();

		final Filter[] filters = { new RenameFilter(), new AgeDateFilter(), new VersionFilter(), new NoteFilter(), new SourceFilter(), new ObjectFilter(), new LanguageFilter(), new TranFilter(), new EnumFilter(), new ExidFilter(), new FileFilter(), new MediaTypeFilter() };
		for (final Filter filter : filters) {
			final var createdRecords = new LinkedList<GedStruct>();

			final Iterator<GedStruct> iterator = records.iterator();
			while (iterator.hasNext()) {
				final GedStruct struct = iterator.next();

				Collection<GedStruct> updatedRecods = filter.update(struct);

				if (updatedRecods != null)
					createdRecords.addAll(updatedRecods);

				if (struct.sup != null)
					iterator.remove();
			}
			records.addAll(createdRecords);
		}

		for (GedStruct s : records)
			s.uri2tag();

		reID();

		records.add(trlr);
	}

	/**
	 * Parses a file, logging but permitting errors and converts cross-references to
	 * pointers.
	 */
	private void fuzzyParse(final String filename) {
		final var stack = new Stack<GedStruct>();
		final var xrefs = new TreeMap<String, GedStruct>();

		try (final var stream = Files.lines(Paths.get(filename), CharsetDetector.detect(filename))) {
			stream.forEach(line -> {
				try {
					final var got = new GedStruct(line);

					while (got.level < stack.size())
						stack.pop();

					if (stack.empty())
						records.add(got);
					else
						stack.peek().addSubstructure(got);

					stack.push(got);

					if (got.id != null)
						xrefs.put(got.id, got);
				} catch (IllegalArgumentException e) {
					errorLog.add(e.toString());
				}
			});

			stack.clear();

			for (final GedStruct r : records)
				r.convertPointers(xrefs, true, LinkedList.class);

		} catch (Exception e) {
			errorLog.add(e.toString());
		}
	}

	/**
	 * Finds which anchors are actually used, renames those and scraps unused
	 * anchors. Unnecessary by itself, but useful before NOTE/SNOTE heuristic and
	 * after adding new records.
	 */
	private void reID() {
		for (final GedStruct r : records)
			if (r.incoming != null && !r.incoming.isEmpty()) {
				r.id = nextID();
			} else {
				r.id = null;
			}
	}

	/**
	 * Allocates and returns the next available record ID.
	 */
	private String nextID() {
		if (lastId == idToSkip)
			lastId += 1;

		return "@" + Integer.toString(lastId++, idBase).toUpperCase() + "@";
	}

	/**
	 * Outputs a parsed dataset as a GEDCOM file.
	 */
	public void write(final OutputStream outputStream) throws IOException {
		outputStream.write("\uFEFF".getBytes(StandardCharsets.UTF_8));

		for (GedStruct r : records)
			outputStream.write(r.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static class ConvertException extends Exception {
		public ConvertException(final List<String> errorLog) {
			super(String.join("\n** ", errorLog));
		}
	}

	public static Converter parse(String filename) throws ConvertException {
		return Converter.parse(filename, 10);
	}

	public static Converter parse(String filename, int idBase) throws ConvertException {
		final var converter = new Converter(filename, idBase);

		if (!converter.errorLog.isEmpty()) {
			throw new ConvertException(converter.errorLog);
		}

		return converter;
	}

	public static void main(String[] args) {
		if (args.length == 1) {
			final var absolutePath = new File(args[0]).getAbsolutePath();
			try {
				final var converter = Converter.parse(absolutePath);
				converter.write(System.out);
			} catch (Exception e) {
				System.exit(-1);
			}
		}
	}
}
