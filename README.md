# gedcom-5to7
This is an open source Java implementation of a GEDCOM 5.5.1 to GEDCOM 7.0 converter. It is a fork of [java-converter](https://github.com/gedcom7code/java-converter) released to public domain by its author [Luther Tychonievich](https://github.com/tychonievich).

The aim of this fork is to publish a somewhat polished and maven-buildable version. Furthermore I will try to complete the missing functionalities (see below).

## Usage

### Standalone
```sh
mvn package
java -jar target/gedcom-5to7-1.0.2.jar data/gedcom551.ged
java -jar target/gedcom-5to7-1.0.2.jar data/gedcom551.ged > data/gedcom7.ged
```

### As dependency
Add the repository and the dependency to your application's `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns="http://maven.apache.org/POM/4.0.0"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	...
	<repositories>
		<repository>
			<id>jitpack.io</id>
			<url>https://jitpack.io</url>
		</repository>
	</repositories>
	<dependencies>
		...
		<dependency>
			<groupId>com.github.cbettinger</groupId>
			<artifactId>gedcom-5to7</artifactId>
			<version>1.0.2</version>
		</dependency>
	</dependencies>
</project>
```

Parse GEDCOM 5.5.1 file and write GEDCOM 7 file:

```java
import bettinger.gedcom5to7.Converter;
import bettinger.gedcom5to7.Converter.ConvertException;

...

try (final OutputStream output = new FileOutputStream(target)) {
	final Converter converter = Converter.parse(source);
	converter.write(output);
} catch (final ConvertException e1) {
	System.err.println(e1.toString());
} catch (final IOException e2) {
	System.err.println(e2.toString());
}
```

## Current Status
This implements all of the major pieces of a 5.5.1-to-7.0 converter. Some tests were perfomed during development, but not enough to provide confidence of bug-free status.

### To-Do
- [ ] change string-valued `INDI`.`ALIA` into `NAME` with `TYPE` `AKA`
- [ ] move base64-encoded OBJE into GEDZIP file
- [ ] add `SCHMA` for all used known extensions
    - [ ] add URIs (or standard tags) for all extensions from <https://wiki-de.genealogy.net/GEDCOM/_Nutzerdef-Tag> and <http://www.gencom.org.nz/GEDCOM_tags.html>

### Done
- [x] Detect character encodings, as documented in [ELF Serialisation](https://fhiso.org/TR/elf-serialisation).
- [x] Convert to UTF-8
- [x] Normalize line whitespace, including stripping leading spaces
- [x] Remove `CONC`
- [x] Fix `@` usage
- [x] Limit character set of cross-reference identifiers
- [x] Normalize case of tags
- [x] Covert `DATE`
    - [x] replace date_phrase with `PHRASE` structure
    - [x] replace calendar escapes with calendar tags
    - [x] change `BC` and `B.C.` to `BCE` and remove if found in unsupported calendars
    - [x] replace dual years with single years and `PHRASE`s
    - [x] replace just-year dual years in unqualified date with `BET`/`AND`
- [x] Convert `AGE`
    - [x] change age words to canonical forms (stillborn as `0y`, child as `< 8y`, infant as `< 1y`) with `PHRASE`s
    - [x] Normalize spacing in `AGE` payloads
    - [x] add missing `y`
- [x] change `SOUR` with text payload into pointer to `SOUR` with `NOTE`
- [x] change `OBJE` with no payload to pointer to new `OBJE` record
- [x] change `NOTE` record or with pointer payload into `SNOTE`
    - [x] use heuristic to change some pointer-`NOTE` to nested-`NOTE` instead of `SNOTE`
- [x] Convert `LANG` payloads to BCP 47 tags, using [FHISO's mapping](https://github.com/fhiso/legacy-format/blob/master/languages.tsv)
- [x] tag renaming, including
    - `EMAI`, `_EMAIL` → `EMAIL`
    - `FORM`.`TYPE` → `FORM`.`MEDI`
    - (deferred) `_SDATE` → `SDATE` -- `_SDATE` is also used as "accessed at" date for web resources by some applications so this change is not universally correct
    - `_UID` → `UID`
    - `_ASSO` → `ASSO`
    - `_CRE`, `_CREAT` → `CREA`
    - `_DATE` → `DATE`
    - `ASSO`.`RELA` → `ASSO`.`ROLE`
    - other?
- [x] Enumerated values
    - [x] Normalize case
    - [x] Convert user-text to `PHRASE`s
- [x] change `RFN`, `RIN`, and `AFN` to `EXID`
- [x] change `_FSFTID`, `_APID` to `EXID`
- [x] Convert `MEDI`.`FORM` payloads to media types
- [x] Convert `FONE` and `ROMN` to `TRAN` and their `TYPE`s to BCP-47 `LANG`s
- [x] change `FILE` payloads into URLs
    - [x] Windows-style `\` becomes `/`
    - [x] Windows diver letter `C:\WINDOWS` becomes `file:///c:/WINDOWS`
    - [x] POSIX-stye `/User/foo` becomes `file:///User/foo`
- [x] remove `SUBN`, `HEAD`.`FILE`, `HEAD`.`CHAR`
- [x] update the `GEDC`.`VERS` to `7.0`
- [x] Change any illegal tag `XYZ` into `_EXT_XYZ`
    - [ ] or to `_XYZ` and add a SCHMA entry for it
    - [ ] leave unchanged under extensions

## Updating to new GEDCOM definitions
The folder `src/main/resources` contains copies of the TSV defintion files from <https://github.com/FamilySearch/GEDCOM/>, <https://github.com/fhiso/legacy-format/> and <https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry> used during runtime.

These can be updated by running

```sh
javac DownloadDefinitions.java
java DownloadDefinitions
```

from the projects root directory.

`DownloadDefinitions.java` is otherwise unneeded, and should not be included in distributions of the gedcom-5to7 package.
