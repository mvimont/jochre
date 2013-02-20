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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.PerformanceMonitor;

final class LetterGuesserImpl implements LetterGuesser {
	private static final Log LOG = LogFactory.getLog(LetterGuesserImpl.class);
	
	private static final double MIN_PROB_TO_STORE = 0.001;
	
	DecisionMaker<Letter> decisionMaker = null;
	Set<LetterFeature<?>> features = null;
	
	LetterGuesserServiceInternal letterGuesserServiceInternal;
	
	public LetterGuesserImpl(Set<LetterFeature<?>> features, DecisionMaker<Letter> decisionMaker) {
		this.decisionMaker = decisionMaker;
		this.features = features;
	}
	
	public String guessLetter(ShapeInSequence shapeInSequence) {
		return this.guessLetter(shapeInSequence, null);
	}


	@Override
	public String guessLetter(ShapeInSequence shapeInSequence, LetterSequence history) {
		PerformanceMonitor.startTask("LetterGuesserImpl.guessLetter");
		try {
			Shape shape = shapeInSequence.getShape();
			if (LOG.isTraceEnabled())
				LOG.trace("guessLetter, shape: " + shape);
			
			
			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			
			PerformanceMonitor.startTask("analyse features");
			try {
				for (LetterFeature<?> feature : features) {
					PerformanceMonitor.startTask(feature.getName());
					try {
						LetterGuesserContext context = this.letterGuesserServiceInternal.getContext(shapeInSequence, history);
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
			
			List<Decision<Letter>> letterGuesses = null;
			PerformanceMonitor.startTask("decision maker");
			try {
				letterGuesses = decisionMaker.decide(featureResults);
			} finally {
				PerformanceMonitor.endTask("decision maker");
			}
			
			Letter bestOutcome = null;
			PerformanceMonitor.startTask("store outcomes");
			try {
				shape.getLetterGuesses().clear();
		
				for (Decision<Letter> letterGuess : letterGuesses) {
					if (letterGuess.getProbability()>=MIN_PROB_TO_STORE) {
						shape.getLetterGuesses().add(letterGuess);
					}
				}
				
				bestOutcome = shape.getLetterGuesses().iterator().next().getOutcome();
			} finally {
				PerformanceMonitor.endTask("store outcomes");
			}
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Shape: " + shape);
				LOG.trace("Letter: " + shape.getLetter());
				LOG.trace("Best outcome: " + bestOutcome);
			}

			return bestOutcome.getString();
		} finally {
			PerformanceMonitor.endTask("LetterGuesserImpl.guessLetter");
		}
	}

	public LetterGuesserServiceInternal getLetterGuesserServiceInternal() {
		return letterGuesserServiceInternal;
	}

	public void setLetterGuesserServiceInternal(
			LetterGuesserServiceInternal letterGuesserServiceInternal) {
		this.letterGuesserServiceInternal = letterGuesserServiceInternal;
	}
	
}