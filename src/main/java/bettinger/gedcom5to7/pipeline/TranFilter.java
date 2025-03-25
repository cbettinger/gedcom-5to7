package bettinger.gedcom5to7.pipeline;

import java.util.ArrayList;
import java.util.Collection;

import bettinger.gedcom5to7.GedStruct;

public class TranFilter implements Filter {
	public Collection<GedStruct> update(final GedStruct struct) {
		if (struct.sup != null && "TYPE".equals(struct.tag)) {
			if ("FONE".equals(struct.sup.tag)) {
				struct.sup.tag = "TRAN";
				struct.tag = "LANG";

				switch (struct.payload.toLowerCase()) {
					case "hangul":
						struct.payload = "ko-hang";
						break;
					case "kana":
						struct.payload = "jp-hrkt";
						break;
					case "pinyin":
						struct.payload = "und-Latn-pinyin";
						break;
					default:
						struct.payload = "x-phonetic-" + struct.payload;
				}

				struct.sup.tag2uri();
			} else if ("ROMN".equals(struct.sup.tag)) {
				struct.sup.tag = "TRAN";
				struct.tag = "LANG";

				switch (struct.payload.toLowerCase()) {
					case "romanji":
						struct.payload = "jp-Latn";
						break;
					case "wadegiles":
						struct.payload = "zh-Latn-wadegile";
						break;
					default:
						struct.payload = "und-Latn-x-" + struct.payload;
				}

				struct.sup.tag2uri();
			}
		}

		for (final var subStruct : struct.sub)
			update(subStruct);

		return new ArrayList<>();
	}
}
