///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.search.lexicon;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Normalises text for storage in index and or lexica.
 * 
 * @author Assaf Urieli
 *
 */
public interface TextNormaliser {
	static Map<Locale, TextNormaliser> textNormaliserMap = new HashMap<>();

	public static TextNormaliser getTextNormaliser(Locale locale) {
		TextNormaliser textNormaliser = null;
		if (locale.getLanguage().equals("yi") || locale.getLanguage().equals("ji")) {
			textNormaliser = textNormaliserMap.get(locale);
			if (textNormaliser == null) {
				textNormaliser = new YiddishTextNormaliser();
				textNormaliserMap.put(locale, textNormaliser);
			}
		}
		return textNormaliser;
	}

	public String normalise(String text);
}
