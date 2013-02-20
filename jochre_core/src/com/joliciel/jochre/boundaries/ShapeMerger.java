///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.boundaries;

import com.joliciel.jochre.graphics.Shape;

/**
 * Decides whether or not to merge two shapes into a single shape.
 * @author Assaf Urieli
 *
 */
public interface ShapeMerger {
	/**
	 * Given two sequential shape, returns the probability of a merge.
	 * @param shape
	 * @return
	 */
	public double checkMerge(Shape shape1, Shape shape2);
	
	/**
	 * Merge two sequential shapes into a single shape.
	 * @param shape1
	 * @param shape2
	 * @return
	 */
	public Shape merge(Shape shape1, Shape shape2);

}