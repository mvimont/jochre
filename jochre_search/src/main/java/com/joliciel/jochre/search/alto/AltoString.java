///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.alto;

import java.util.List;

import com.joliciel.jochre.search.JochreToken;

public interface AltoString extends JochreToken {
	public String getContent();
	public int getLeft();
	public int getTop();
	public int getWidth();
	public int getHeight();
	public AltoTextLine getTextLine();
	
	/**
	 * Alternative possibilities for the current string.
	 * @return
	 */
	public List<String> getAlternatives();
	
	/**
	 * The confidence assigned by the OCR model to this string,
	 * from 0 to 1.
	 * @return
	 */
	public double getConfidence();
	public void setConfidence(double confidence);
	
	/**
	 * Is this string the first half of a hyphenated word
	 * which crosses an end-of-line.
	 * @return
	 */
	public boolean isHyphenStart();
	public void setHyphenStart(boolean hyphenStart);
	
	/**
	 * Is this string the second half of a hyphenated word
	 * which corsses an end-of-line.
	 * @return
	 */
	public boolean isHyphenEnd();
	public void setHyphenEnd(boolean hyphenEnd);
	
	/**
	 * The full hyphenated word, or null if string is not part of hyphenated word.
	 * @return
	 */
	public String getHyphenatedContent();
	public void setHyphenatedContent(String hyphenatedContent);
	
	/**
	 * The index of this string within the current TextLine.
	 * @return
	 */
	public int getIndex();

	/**
	 * The starting position of this string's character span, where 0
	 * is considered to be the start of the current page.
	 * @return
	 */
	public abstract int getSpanStart();
	public abstract void setSpanStart(int spanStart);
	
	/**
	 * The position just after the last character in this string.
	 * @return
	 */
	public abstract int getSpanEnd();
	public abstract void setSpanEnd(int spanEnd);

	/**
	 * Does this string represent white space?
	 * @return
	 */
	public boolean isWhiteSpace();

}