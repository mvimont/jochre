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
package com.joliciel.jochre.letterGuesser;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.talismane.machineLearning.CorpusEvent;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.PerformanceMonitor;

class JochreLetterEventStream implements CorpusEventStream {
    private static final Log LOG = LogFactory.getLog(JochreLetterEventStream.class);
	private GraphicsService graphicsService;
	private LetterGuesserServiceInternal letterGuesserServiceInternal;
	private BoundaryService boundaryService;
	private MachineLearningService machineLearningService;

	private BoundaryDetector boundaryDetector;
	
	private Set<LetterFeature<?>> features = null;
	private ImageStatus[] imageStatusesToInclude = new ImageStatus[] { ImageStatus.TRAINING_VALIDATED };
	private int shapeIndex = 0;
	
	private int imageCount = 0;
	
	private ShapeInSequence shapeInSequence = null;

	private LetterSequence history = null;
	
	private JochreCorpusGroupReader groupReader;
	ShapeSequence shapeSequence = null;
	LetterValidator letterValidator = null;
	
	private long totalTimeDatabaseRead = 0;
	private int invalidLetterCount = 0;

	/**
	 * Constructor.
	 * @param features the features to analyse when training
	 * @param graphicsService
	 * @param letterGuesserServiceInternal
	 * @param recalculateFeatures if true, features will be recalculated from scratch (slower, but doesn't require previous analysis & database storage space. If false, features will be loaded from the data store.
	 */
	public JochreLetterEventStream(Set<LetterFeature<?>> features, LetterValidator letterValidator) {
		this.features = features;
		this.letterValidator = letterValidator;
	}
	
	@Override
	public CorpusEvent next() {
		PerformanceMonitor.startTask("JochreLetterEventStream.next");
		try {
			CorpusEvent event = null;
			if (this.hasNext()) {
				Shape shape = shapeInSequence.getShape();
				LOG.debug("next event, shape: " + shape);
				LetterGuesserContext context = this.letterGuesserServiceInternal.getContext(shapeInSequence, history);
				
				List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
				PerformanceMonitor.startTask("analyse features");
				try {
					for (LetterFeature<?> feature : features) {
						PerformanceMonitor.startTask(feature.getName());
						try {
							FeatureResult<?> featureResult = feature.check(context);
							if (featureResult!=null) {
								featureResults.add(featureResult);
								if (LOG.isTraceEnabled()) {
									LOG.trace(featureResult.toString());
								}
							}
						} finally {
							PerformanceMonitor.endTask(feature.getName());
						}
					}
				} finally {
					PerformanceMonitor.endTask("analyse features");
				}
				
				String outcome = shape.getLetter();
				
				event = this.machineLearningService.getCorpusEvent(featureResults, outcome);
				
				Letter letter = new Letter(outcome);
				history.add(letter);
				// set shape to null so that hasNext can retrieve the next one.
				this.shapeInSequence = null;
			}
			return event;
		} finally {
			PerformanceMonitor.endTask("JochreLetterEventStream.next");
		}
	}

	@Override
	public boolean hasNext() {
		PerformanceMonitor.startTask("JochreLetterEventStream.hasNext");
		try {
			long startTimeDatabaseRead = (new Date()).getTime();
			this.initialiseStream();
			
			while (shapeInSequence==null && shapeSequence!=null) {
				while (shapeInSequence==null && shapeIndex < shapeSequence.size()) {
					shapeInSequence = shapeSequence.get(shapeIndex);
					shapeIndex++;
					
					Shape shape = shapeInSequence.getShape();
					String letter = shape.getLetter();
					if (!letterValidator.validate(letter)) {
						// if there's an invalid letter, skip the rest of this group
						// note we allow empty letters (which is how we indicate ink smudges in the text)
						LOG.debug("Invalid letter for shape " + shapeInSequence.getOriginalShapes().get(0).getId() + ": " + letter);
						invalidLetterCount++;
						shapeInSequence = null;
						break;
					}
				} 
				
				if (shapeInSequence==null) {
					this.getNextGroup();
				}
			}
		
			totalTimeDatabaseRead += (new Date()).getTime() - startTimeDatabaseRead;
			if (shapeInSequence==null) {
				LOG.debug("invalidLetterCount: " + invalidLetterCount);
			}
			return shapeInSequence!=null;
		} finally {
			PerformanceMonitor.endTask("JochreLetterEventStream.hasNext");			
		}
	}
	
	void getNextGroup() {
		shapeSequence = null;
		shapeIndex = 0;
		if (groupReader.hasNext()) {
			GroupOfShapes group = groupReader.next();
			if (boundaryDetector!=null) {
				// in this case the boundary detector is supposed to give us the correct splits and merges
				shapeSequence = boundaryDetector.findBoundaries(group).get(0);
			} else {
				// simply add this group's shapes
				shapeSequence = boundaryService.getEmptyShapeSequence();
				for (Shape shape : group.getShapes())
					shapeSequence.addShape(shape);
			}
			
			history = this.letterGuesserServiceInternal.getEmptyLetterSequence(shapeSequence);

		}
	}
	
	void initialiseStream() {
		if (groupReader==null) {
			groupReader = this.graphicsService.getJochreCorpusGroupReader();
			groupReader.setImageStatusesToInclude(imageStatusesToInclude);
			groupReader.setImageCount(imageCount);
			this.getNextGroup();
		}

	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public LetterGuesserServiceInternal getLetterGuesserServiceInternal() {
		return letterGuesserServiceInternal;
	}

	public void setLetterGuesserServiceInternal(
			LetterGuesserServiceInternal letterGuesserServiceInternal) {
		this.letterGuesserServiceInternal = letterGuesserServiceInternal;
	}

	public int getImageCount() {
		return imageCount;
	}

	public void setImageCount(int imageCount) {
		this.imageCount = imageCount;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

	public BoundaryDetector getBoundaryDetector() {
		return boundaryDetector;
	}

	public void setBoundaryDetector(BoundaryDetector boundaryDetector) {
		this.boundaryDetector = boundaryDetector;
	}

	public ImageStatus[] getImageStatusesToInclude() {
		return imageStatusesToInclude;
	}

	public void setImageStatusesToInclude(ImageStatus[] imageStatusesToInclude) {
		this.imageStatusesToInclude = imageStatusesToInclude;
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new LinkedHashMap<String, Object>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("imageCount", imageCount);		
		attributes.put("imageStatusesToInclude", imageStatusesToInclude);		
		
		return attributes;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}