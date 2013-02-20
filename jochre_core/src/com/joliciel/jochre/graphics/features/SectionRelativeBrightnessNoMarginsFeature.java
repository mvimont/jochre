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
package com.joliciel.jochre.graphics.features;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;

/**
 * Gives a mark from 0 to 1
 * a given section within a shape, in view of its
 * pixel brightness total / pixel count
 * with respect to the same measure on all other sectors.
 * The section with the highest brightness will always be given 1.
 * @author Assaf Urieli
 *
 */
public class SectionRelativeBrightnessNoMarginsFeature extends AbstractShapeFeature<Double> implements DoubleFeature<ShapeWrapper> {
    @SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(SectionRelativeBrightnessNoMarginsFeature.class);
	private IntegerFeature<ShapeWrapper> xFeature;
	private IntegerFeature<ShapeWrapper> yFeature;
	private IntegerFeature<ShapeWrapper> verticalSectionsFeature;
	private IntegerFeature<ShapeWrapper> horizontalSectionsFeature;
	
	public SectionRelativeBrightnessNoMarginsFeature(IntegerFeature<ShapeWrapper> xFeature, IntegerFeature<ShapeWrapper> yFeature,
			IntegerFeature<ShapeWrapper> verticalSectionsFeature, IntegerFeature<ShapeWrapper> horizontalSectionsFeature) {
		super();
		this.xFeature = xFeature;
		this.yFeature = yFeature;
		this.verticalSectionsFeature = verticalSectionsFeature;
		this.horizontalSectionsFeature = horizontalSectionsFeature;
		
		this.setName(super.getName()
				+ "(" + this.xFeature.getName()
				+ "," + this.yFeature.getName()
				+ "," + this.verticalSectionsFeature.getName()
				+ "," + this.horizontalSectionsFeature.getName()
				+ ")");
	}
	
	@Override
	public FeatureResult<Double> checkInternal(ShapeWrapper shapeWrapper) {
		FeatureResult<Double> result = null;

		FeatureResult<Integer> xResult = xFeature.check(shapeWrapper);
		FeatureResult<Integer> yResult = yFeature.check(shapeWrapper);
		FeatureResult<Integer> verticalSectionsResult = verticalSectionsFeature.check(shapeWrapper);
		FeatureResult<Integer> horizontalSectionsResult = horizontalSectionsFeature.check(shapeWrapper);
		
		if (xResult!=null && yResult!=null && verticalSectionsResult!=null && horizontalSectionsResult!=null) {
			int x = xResult.getOutcome();
			int y = yResult.getOutcome();
			int verticalSections = verticalSectionsResult.getOutcome();
			int horizontalSections = horizontalSectionsResult.getOutcome();	
			
			Shape shape = shapeWrapper.getShape();
			double[][] graduatedBrightnessGrid = shape.getBrightnessBySection(verticalSections, horizontalSections, SectionBrightnessMeasurementMethod.RELATIVE_TO_MAX_SECTION);
			double graduatedBrightness = graduatedBrightnessGrid[x][y];
			
			result = this.generateResult(graduatedBrightness);
		}
		return result;
	}
}