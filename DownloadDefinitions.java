import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadDefinitions {

	private static void download(final String source, final String target) {
		try {
			final URL sourceURL = new URI(source).toURL();
			try (final BufferedInputStream inputStream = new BufferedInputStream(sourceURL.openStream())) {
				try (final FileOutputStream outputStream = new FileOutputStream(target)) {
					byte[] buffer = new byte[1024];
					int count = 0;
					while ((count = inputStream.read(buffer, 0, buffer.length)) != -1) {
						outputStream.write(buffer, 0, count);
					}
				}
			}
		} catch (Exception e) {
			Logger.getLogger(DownloadDefinitions.class.getName()).log(Level.SEVERE, String.format("Unable to download from '%s' to '%s'", source, target), e);
		}
	}

	private static void downloadIANALanguageSubtagRegistery(final String target) {
		final String source = "https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry";

		try {
			final URL sourceURL = new URI(source).toURL();
			try (final Scanner inputStream = new Scanner(sourceURL.openStream());) {
				try (final FileOutputStream outputStream = new FileOutputStream(target)) {
					boolean isLanguage = false;
					String tag = null;

					while (inputStream.hasNextLine()) {
						final String line = inputStream.nextLine();
						if (line.startsWith("Type: ")) {
							isLanguage = line.equals("Type: language");
						} else if (line.startsWith("Subtag: ")) {
							tag = line.substring(8);
						} else if (isLanguage && line.startsWith("Description: ")) {
							String key = line.substring(13);
							// key = key.replaceAll(" \\(.*", ""); // TODO: change "Modern Greek (1453-)" to "Modern Greek" but also conflates arr "Karo (Brazil)" and kxh "Karo (Ethiopia)" so disabled for now
							key = key.replaceAll(" language.*", ""); // TODO: change "Bihari languages" to "Bihari"
							outputStream.write((key + "\t" + tag + "\n").getBytes(StandardCharsets.UTF_8)); // TODO: why call getBytes()?
						}
					}
				}
			}

		} catch (Exception e) {
			Logger.getLogger(DownloadDefinitions.class.getName()).log(Level.SEVERE, String.format("Unable to download from '%s' to '%s'", source, target), e);
		}
	}

	public static void main(String[] args) {
		download("https://github.com/FamilySearch/GEDCOM/raw/main/extracted-files/enumerations.tsv", "src/main/java/bettinger/gedcom5to7/definitions/enumerations.tsv");
		download("https://github.com/FamilySearch/GEDCOM/raw/main/extracted-files/enumerationsets.tsv", "src/main/java/bettinger/gedcom5to7/definitions/enumerationsets.tsv");
		download("https://github.com/FamilySearch/GEDCOM/raw/main/extracted-files/payloads.tsv", "src/main/java/bettinger/gedcom5to7/definitions/payloads.tsv");
		download("https://github.com/FamilySearch/GEDCOM/raw/main/extracted-files/substructures.tsv", "src/main/java/bettinger/gedcom5to7/definitions/substructures.tsv");
		download("https://github.com/FamilySearch/GEDCOM/raw/main/extracted-files/cardinalities.tsv", "src/main/java/bettinger/gedcom5to7/definitions/cardinalities.tsv");
		download("https://github.com/fhiso/legacy-format/raw/master/languages.tsv", "src/main/java/bettinger/gedcom5to7/definitions/languages.tsv");

		downloadIANALanguageSubtagRegistery("src/main/java/bettinger/gedcom5to7/definitions/all-languages.tsv");
	}
}
