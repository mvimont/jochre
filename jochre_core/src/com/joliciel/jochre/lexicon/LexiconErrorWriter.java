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
package com.joliciel.jochre.lexicon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.analyser.LetterGuessObserver;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Creates the following outputs:<br/>
 * a 2x2 matrix of known/unknown vs error/correct<br/>
 * a list of words for each square in the matrix<br/>
 * @author Assaf Urieli
 *
 */
public class LexiconErrorWriter implements LetterGuessObserver {
	private static final Log LOG = LogFactory.getLog(LexiconErrorWriter.class);
	private File outputDir;
	private String baseName;
	MostLikelyWordChooser wordChooser;
	
	Writer knownWordErrorWriter;
	Writer knownWordCorrectWriter;
	Writer unknownWordErrorWriter;
	Writer unknownWordCorrectWriter;
	Writer allErrorWriter;
	Writer allWordWriter;
	
	private static final String ALL_GROUP = "All";
	Map<String,Set<Integer>> documentGroups = new HashMap<String, Set<Integer>>();
	List<String> documentNames = null;
	
	Map<String,ErrorStatistics> errorMap = new LinkedHashMap<String, ErrorStatistics>();
	private JochreDocument currentDoc = null;
	private boolean beamContainsRightWord = false;
	
	private static DecimalFormat df = new DecimalFormat("0.##");
	
	public LexiconErrorWriter(File outputDir, String baseName, MostLikelyWordChooser wordChooser) {
		try {
			this.outputDir = outputDir;
			this.baseName = baseName;
			this.wordChooser = wordChooser;
			
			errorMap.put(ALL_GROUP, new ErrorStatistics());
			
			knownWordErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KE.csv"), false),"UTF8"));
			knownWordCorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KC.csv"), false),"UTF8"));
			unknownWordErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_UE.csv"), false),"UTF8"));
			unknownWordCorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_UC.csv"), false),"UTF8"));
			allWordWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_all.csv"), false),"UTF8"));
			allErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_err.csv"), false),"UTF8"));
				
			String line = CSVFormatter.format("realSeq")
				+ CSVFormatter.format("realWord")
				+ CSVFormatter.format("guessSeq")
				+ CSVFormatter.format("guessWord")
				+ CSVFormatter.format("realFreq")
				+ CSVFormatter.format("guessFreq")
				+ CSVFormatter.format("file")
				+ CSVFormatter.format("page")
				+ CSVFormatter.format("par")
				+ CSVFormatter.format("row")
				+ CSVFormatter.format("group")
				+ CSVFormatter.format("id")
				+ "\n";
			
			knownWordErrorWriter.write(line);
			knownWordCorrectWriter.write(line);
			unknownWordErrorWriter.write(line);
			unknownWordCorrectWriter.write(line);
			
			line = CSVFormatter.format("realSeq")
				+ CSVFormatter.format("realWord")
				+ CSVFormatter.format("guessSeq")
				+ CSVFormatter.format("guessWord")
				+ CSVFormatter.format("known")
				+ CSVFormatter.format("error")
				+ CSVFormatter.format("realFreq")
				+ CSVFormatter.format("guessFreq")
				+ CSVFormatter.format("file")
				+ CSVFormatter.format("page")
				+ CSVFormatter.format("par")
				+ CSVFormatter.format("row")
				+ CSVFormatter.format("group")
				+ CSVFormatter.format("id")
				+ "\n";
			allWordWriter.write(line);
			allErrorWriter.write(line);
			
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		JochreDocument doc = jochreImage.getPage().getDocument();
		if (!doc.equals(currentDoc)) {
			currentDoc = doc;
			ErrorStatistics stats = errorMap.get(doc.getName());
			if (stats==null) {
				stats = new ErrorStatistics();
				errorMap.put(doc.getName(), stats);
			}
		}
	}

	@Override
	public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {

	}

	@Override
	public void onStartSequence(LetterSequence letterSequence) {
		
	}
	

	@Override
	public void onBeamSearchEnd(LetterSequence bestSequence,
			List<LetterSequence> finalSequences,
			List<LetterSequence> holdoverSequences) {
		beamContainsRightWord = false;
		for (LetterSequence letterSequence : finalSequences) {
			if (letterSequence.getRealWord().equals(letterSequence.getGuessedWord())) {
				beamContainsRightWord = true;
				break;
			}
		}
		if (beamContainsRightWord && holdoverSequences!=null && holdoverSequences.size()>0) {
			beamContainsRightWord = false;
			for (LetterSequence letterSequence : holdoverSequences) {
				if (letterSequence.getRealWord().equals(letterSequence.getGuessedWord())) {
					beamContainsRightWord = true;
					break;
				}
			}
		}
		
	}
	
	@Override
	public void onGuessSequence(LetterSequence bestSequence) {
		try  {
			int realFrequency = 0;
			if (wordChooser!=null)
				realFrequency = wordChooser.getFrequency(bestSequence.getRealWord());
			boolean error = !bestSequence.getRealWord().equals(bestSequence.getGuessedWord());
			boolean known = realFrequency>0;
			boolean badSeg = bestSequence.getRealSequence().contains("[") || bestSequence.getRealSequence().contains("|");
			
			for (int i=0;i<3;i++) {
				Writer writer = null;
				if (i==0) {
					writer = allWordWriter;
				} else if (i==1) {
					if (error)
						writer = allErrorWriter;
					else
						continue;
				} else {
					int j=0;
					List<ErrorStatistics> statList = new ArrayList<LexiconErrorWriter.ErrorStatistics>();
					statList.add(errorMap.get(ALL_GROUP));
					statList.add(errorMap.get(currentDoc.getName()));
					for (String docGroupName : documentGroups.keySet()) {
						if (documentGroups.get(docGroupName).contains(currentDoc.getId()))
							statList.add(errorMap.get(docGroupName));
					}
					
					if (beamContainsRightWord) {
						if (error) {
							for (ErrorStatistics stats : statList)
								stats.answerInBeamErrorCount++;
						} else {
							for (ErrorStatistics stats : statList)
								stats.answerInBeamCorrectCount++;
						}
						beamContainsRightWord = false;
					}
					
					Linguistics linguistics = Linguistics.getInstance(JochreSession.getLocale());
					for (ShapeInSequence shapeInSequence : bestSequence.getUnderlyingShapeSequence()) {
						String letterGuess = bestSequence.get(j++).getString();
						String letter = shapeInSequence.getShape().getLetter();
						boolean badSegLetter = letter.contains("|") || letter.length()==0 || (letter.length()>1 && !linguistics.getDualCharacterLetters().contains(letter));
						if (letter.equals(letterGuess)) {
							if (known) {
								for (ErrorStatistics stats : statList)
									stats.knownWordCorrectLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.unknownWordCorrectLetterCount++;
							}
							if (badSegLetter) {
								for (ErrorStatistics stats : statList)
									stats.badSegCorrectLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.goodSegCorrectLetterCount++;
							}
						} else {
							if (known) {
								for (ErrorStatistics stats : statList)
									stats.knownWordErrorLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.unknownWordErrorLetterCount++;
							}
							if (badSegLetter) {
								for (ErrorStatistics stats : statList)
									stats.badSegErrorLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.goodSegErrorLetterCount++;
							}
						}
					}
					if (error&&known) {
						for (ErrorStatistics stats : statList)
							stats.knownWordErrorCount++;
						writer = knownWordErrorWriter;
					} else if (error&&!known) {
						for (ErrorStatistics stats : statList)
							stats.unknownWordErrorCount++;
						writer = unknownWordErrorWriter;
					} else if (!error&&known) {
						for (ErrorStatistics stats : statList)
							stats.knownWordCorrectCount++;
						writer = knownWordCorrectWriter;
					} else if (!error&&!known) {
						for (ErrorStatistics stats : statList)
							stats.unknownWordCorrectCount++;
						writer = unknownWordCorrectWriter;
					}
					
					if (error) {
						if (badSeg) {
							for (ErrorStatistics stats : statList)
								stats.badSegErrorCount++;
						} else {
							for (ErrorStatistics stats : statList)
								stats.goodSegErrorCount++;
						}
					} else {
						if (badSeg) {
							for (ErrorStatistics stats : statList)
								stats.badSegCorrectCount++;
						} else {
							for (ErrorStatistics stats : statList)
								stats.goodSegCorrectCount++;
						}
					}
				}
				
				writer.write(CSVFormatter.format(bestSequence.getRealSequence()));
				writer.write(CSVFormatter.format(bestSequence.getRealWord()));
				writer.write(CSVFormatter.format(bestSequence.getGuessedSequence()));
				writer.write(CSVFormatter.format(bestSequence.getGuessedWord()));
				
				if (i<2) {
					writer.write(CSVFormatter.format(known ? 1 : 0));
					writer.write(CSVFormatter.format(error ? 1 : 0));
				}
				
				writer.write(CSVFormatter.format(realFrequency));
				writer.write(CSVFormatter.format(bestSequence.getFrequency()));
				writer.write(CSVFormatter.format(bestSequence.getFirstGroup().getRow().getParagraph().getImage().getPage().getDocument().getName()));
				writer.write(CSVFormatter.format(bestSequence.getFirstGroup().getRow().getParagraph().getImage().getPage().getIndex()));
				writer.write(CSVFormatter.format(bestSequence.getFirstGroup().getRow().getParagraph().getIndex()));
				writer.write(CSVFormatter.format(bestSequence.getFirstGroup().getRow().getIndex()));
				writer.write(CSVFormatter.format(bestSequence.getFirstGroup().getIndex()));
				writer.write(CSVFormatter.format(bestSequence.getFirstGroup().getId()));
				writer.write("\n");
				writer.flush();
			}
			
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
		try  {
			knownWordErrorWriter.close();
			knownWordCorrectWriter.close();
			unknownWordErrorWriter.close();
			unknownWordCorrectWriter.close();
			allWordWriter.close();
			allErrorWriter.close();
			
			Writer statsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KEMatrix.csv"), false),"UTF8"));
			writeStats(statsWriter, errorMap);

			statsWriter.flush();
			statsWriter.close();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}

	}
	
	public static void writeStats(Writer statsWriter, Map<String,ErrorStatistics> errorMap) {
		try {
			for (String statName : errorMap.keySet()) {
				statsWriter.write(CSVFormatter.format(statName) + CSVFormatter.getCsvSeparator() + CSVFormatter.getCsvSeparator() + CSVFormatter.getCsvSeparator() + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");
			
			for (@SuppressWarnings("unused") String statName : errorMap.keySet()) {
				statsWriter.write(CSVFormatter.getCsvSeparator() + CSVFormatter.format("correct") + CSVFormatter.format("error")+ CSVFormatter.format("total") + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");
			
			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("known") + CSVFormatter.format(stats.knownWordCorrectCount) + CSVFormatter.format(stats.knownWordErrorCount) + CSVFormatter.format(stats.knownWordCorrectCount+stats.knownWordErrorCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("unknown") + CSVFormatter.format(stats.unknownWordCorrectCount) + CSVFormatter.format(stats.unknownWordErrorCount) + CSVFormatter.format(stats.unknownWordCorrectCount+stats.unknownWordErrorCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("goodSeg") + CSVFormatter.format(stats.goodSegCorrectCount) + CSVFormatter.format(stats.goodSegErrorCount) + CSVFormatter.format(stats.goodSegCorrectCount+stats.goodSegErrorCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("badSeg") + CSVFormatter.format(stats.badSegCorrectCount) + CSVFormatter.format(stats.badSegErrorCount) + CSVFormatter.format(stats.badSegCorrectCount+stats.badSegErrorCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("inBeam") + CSVFormatter.format(stats.answerInBeamCorrectCount) + CSVFormatter.format(stats.answerInBeamErrorCount) + CSVFormatter.format(stats.getAnswerInBeamCount()) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("total") + CSVFormatter.format(stats.knownWordCorrectCount + stats.unknownWordCorrectCount) + CSVFormatter.format(stats.knownWordErrorCount + stats.unknownWordErrorCount) + CSVFormatter.format(stats.knownWordCorrectCount+stats.knownWordErrorCount+stats.unknownWordCorrectCount+stats.unknownWordErrorCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("known%") + CSVFormatter.format(stats.getKnownWordCount()==0 ? "0" : df.format((double)stats.knownWordCorrectCount/stats.getKnownWordCount()*100)) + CSVFormatter.format(stats.getKnownWordCount()==0 ? "0" : df.format((double)stats.knownWordErrorCount/stats.getKnownWordCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format(stats.getKnownWordCount()/stats.getTotalCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("unknown%") + CSVFormatter.format(stats.getUnknownWordCount()==0 ? "0" : df.format((double)stats.unknownWordCorrectCount/stats.getUnknownWordCount()*100)) + CSVFormatter.format(stats.getUnknownWordCount()==0 ? "0" : df.format((double)stats.unknownWordErrorCount/stats.getUnknownWordCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format(stats.getUnknownWordCount()/stats.getTotalCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("goodSeg%") + CSVFormatter.format(stats.getGoodSegCount()==0 ? "0" : df.format((double)stats.goodSegCorrectCount/stats.getGoodSegCount()*100)) + CSVFormatter.format(stats.getGoodSegCount()==0 ? "0" : df.format((double)stats.goodSegErrorCount/stats.getGoodSegCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format(stats.getGoodSegCount()/stats.getTotalCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("badSeg%") + CSVFormatter.format(stats.getBadSegCount()==0 ? "0" : df.format((double)stats.badSegCorrectCount/stats.getBadSegCount()*100)) + CSVFormatter.format(stats.getBadSegCount()==0 ? "0" : df.format((double)stats.badSegErrorCount/stats.getBadSegCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format(stats.getBadSegCount()/stats.getTotalCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("inBeam%") + CSVFormatter.format(stats.getAnswerInBeamCount()==0 ? "0" : df.format((double)stats.answerInBeamCorrectCount/stats.getAnswerInBeamCount()*100)) + CSVFormatter.format(stats.getAnswerInBeamCount()==0 ? "0" : df.format((double)stats.answerInBeamErrorCount/stats.getAnswerInBeamCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format(stats.getAnswerInBeamCount()/stats.getTotalCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("total%") + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format((double)(stats.knownWordCorrectCount + stats.unknownWordCorrectCount) / stats.getTotalCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format((double)(stats.knownWordErrorCount + stats.unknownWordErrorCount) / stats.getTotalCount()*100)) + CSVFormatter.format(stats.getTotalCount()==0 ? "0" : df.format(100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("knownLetters") + CSVFormatter.format(stats.knownWordCorrectLetterCount) + CSVFormatter.format(stats.knownWordErrorLetterCount) + CSVFormatter.format(stats.knownWordCorrectLetterCount+stats.knownWordErrorLetterCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("unknownLetters") + CSVFormatter.format(stats.unknownWordCorrectLetterCount) + CSVFormatter.format(stats.unknownWordErrorLetterCount) + CSVFormatter.format(stats.unknownWordCorrectLetterCount+stats.unknownWordErrorLetterCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("goodSegLetters") + CSVFormatter.format(stats.goodSegCorrectLetterCount) + CSVFormatter.format(stats.goodSegErrorLetterCount) + CSVFormatter.format(stats.goodSegCorrectLetterCount+stats.goodSegErrorLetterCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("badSegLetters") + CSVFormatter.format(stats.badSegCorrectLetterCount) + CSVFormatter.format(stats.badSegErrorLetterCount) + CSVFormatter.format(stats.badSegCorrectLetterCount+stats.badSegErrorLetterCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("totalLetters") + CSVFormatter.format(stats.knownWordCorrectLetterCount + stats.unknownWordCorrectLetterCount) + CSVFormatter.format(stats.knownWordErrorLetterCount + stats.unknownWordErrorLetterCount) + CSVFormatter.format(stats.knownWordCorrectLetterCount+stats.knownWordErrorLetterCount+stats.unknownWordCorrectLetterCount+stats.unknownWordErrorLetterCount) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("knownLetter%") + CSVFormatter.format(stats.getKnownWordLetterCount()==0 ? "0" : df.format((double)stats.knownWordCorrectLetterCount/stats.getKnownWordLetterCount()*100)) + CSVFormatter.format(stats.getKnownWordLetterCount()==0 ? "0" : df.format((double)stats.knownWordErrorLetterCount/stats.getKnownWordLetterCount()*100)) + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format(stats.getKnownWordLetterCount()/stats.getTotalLetterCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("unknownLetter%") + CSVFormatter.format(stats.getUnknownWordLetterCount()==0 ? "0" : df.format((double)stats.unknownWordCorrectLetterCount/stats.getUnknownWordLetterCount()*100)) + CSVFormatter.format(stats.getUnknownWordLetterCount()==0 ? "0" : df.format((double)stats.unknownWordErrorLetterCount/stats.getUnknownWordLetterCount()*100)) + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format(stats.getUnknownWordLetterCount()/stats.getTotalLetterCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("goodSegLetter%") + CSVFormatter.format(stats.getGoodSegLetterCount()==0 ? "0" : df.format((double)stats.goodSegCorrectLetterCount/stats.getGoodSegLetterCount()*100)) + CSVFormatter.format(stats.getGoodSegLetterCount()==0 ? "0" : df.format((double)stats.goodSegErrorLetterCount/stats.getGoodSegLetterCount()*100)) + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format(stats.getGoodSegLetterCount()/stats.getTotalLetterCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("badSegLetter%") + CSVFormatter.format(stats.getBadSegLetterCount()==0 ? "0" : df.format((double)stats.badSegCorrectLetterCount/stats.getBadSegLetterCount()*100)) + CSVFormatter.format(stats.getBadSegLetterCount()==0 ? "0" : df.format((double)stats.badSegErrorLetterCount/stats.getBadSegLetterCount()*100)) + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format(stats.getBadSegLetterCount()/stats.getTotalLetterCount()*100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSVFormatter.format("totalLetter%") + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format((double)(stats.knownWordCorrectLetterCount + stats.unknownWordCorrectLetterCount) / stats.getTotalLetterCount()*100)) + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format((double)(stats.knownWordErrorLetterCount + stats.unknownWordErrorLetterCount) / stats.getTotalLetterCount()*100)) + CSVFormatter.format(stats.getTotalLetterCount()==0 ? "0" : df.format(100)) + CSVFormatter.getCsvSeparator());
			}
			statsWriter.write("\n");

			statsWriter.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public Map<String, Set<Integer>> getDocumentGroups() {
		return documentGroups;
	}

	public void setDocumentGroups(Map<String, Set<Integer>> documentGroups) {
		this.documentGroups = documentGroups;
		for (String group : documentGroups.keySet()) {
			this.errorMap.put(group, new ErrorStatistics());
		}
	}



	public List<String> getDocumentNames() {
		return documentNames;
	}

	public void setDocumentNames(List<String> documentNames) {
		this.documentNames = documentNames;
		for (String documentName : documentNames) {
			this.errorMap.put(documentName, new ErrorStatistics());
		}
	}



	private static final class ErrorStatistics {
		public int knownWordErrorCount;
		public int knownWordCorrectCount;
		public int unknownWordErrorCount;
		public int unknownWordCorrectCount;
		public int goodSegCorrectCount;
		public int goodSegErrorCount;
		public int badSegCorrectCount;
		public int badSegErrorCount;

		public int knownWordErrorLetterCount;
		public int knownWordCorrectLetterCount;
		public int unknownWordErrorLetterCount;
		public int unknownWordCorrectLetterCount;
		
		public int goodSegCorrectLetterCount;
		public int goodSegErrorLetterCount;
		public int badSegCorrectLetterCount;
		public int badSegErrorLetterCount;
		
		public int answerInBeamCorrectCount;
		public int answerInBeamErrorCount;
		
		public double getTotalCount() {
			return knownWordCorrectCount+unknownWordCorrectCount+knownWordErrorCount+unknownWordErrorCount;
		}
		
		public double getTotalLetterCount() {
			return knownWordCorrectLetterCount+unknownWordCorrectLetterCount+knownWordErrorLetterCount+unknownWordErrorLetterCount;
		}
		
		public double getKnownWordCount() {
			return knownWordCorrectCount+knownWordErrorCount;
		}
		public double getUnknownWordCount() {
			return unknownWordCorrectCount+unknownWordErrorCount;
		}
		public double getKnownWordLetterCount() {
			return knownWordCorrectLetterCount+knownWordErrorLetterCount;
		}
		public double getUnknownWordLetterCount() {
			return unknownWordCorrectLetterCount+unknownWordErrorLetterCount;
		}
		
		public double getGoodSegCount() {
			return goodSegCorrectCount+goodSegErrorCount;
		}
		public double getBadSegCount() {
			return badSegCorrectCount+badSegErrorCount;
		}

		public double getGoodSegLetterCount() {
			return goodSegCorrectLetterCount+goodSegErrorLetterCount;
		}
		public double getBadSegLetterCount() {
			return badSegCorrectLetterCount+badSegErrorLetterCount;
		}
		
		public double getAnswerInBeamCount() {
			return answerInBeamCorrectCount + answerInBeamErrorCount;
		}
	}
	
	public static void main(String[] args) throws Exception {
		File evalDir = new File(args[0]);
		String prefix = args[1];
		mergeCrossValidation(evalDir, prefix);
	}
	
	static void mergeCrossValidation(File evalDir, String prefix) {
		try  {
			File[] files = evalDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.endsWith(".csv"))
						return true;
					else
						return false;
				}
			});
			List<String> groupNames = new ArrayList<String>();
			Map<String, Writer> writers = new HashMap<String, Writer>();
			Map<String, ErrorStatistics> errorMap = new LinkedHashMap<String, ErrorStatistics>();
			Map<String, Map<String,DescriptiveStatistics>> statMap = new HashMap<String, Map<String,DescriptiveStatistics>>();
			for (File file : files) {
				String filename = file.getName();
				LOG.debug("Processing " + filename);
				int index = Integer.parseInt(filename.substring(prefix.length(),prefix.length()+1));
				String suffix = filename.substring(prefix.length()+2, filename.lastIndexOf('_'));
				String fileType = filename.substring(filename.lastIndexOf('_')+1, filename.lastIndexOf('.'));
				LOG.debug("Processing " + filename);
				LOG.debug("index: " + index);
				LOG.debug("suffix: " + suffix);
				LOG.debug("fileType: " + fileType);
				Writer writer = writers.get(fileType);
				boolean firstFile = false;
				if (writer==null) {
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(evalDir, prefix + "A_" + suffix + "_" + fileType + ".csv"), false),"UTF8"));
					writers.put(fileType, writer);
					firstFile = true;
				}
				if (fileType.equals("KEMatrix")) {
					Scanner scanner = new Scanner(file);
					int i = 0;
					List<String> myGroupNames = new ArrayList<String>();
					Map<String,Boolean> haveCountMap = new HashMap<String, Boolean>();
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						List<String> cells = CSVFormatter.getCSVCells(line);
						if (i==0) {
							for (int j=0; j<cells.size(); j+=5) {
								String groupName = cells.get(j);
								if (!errorMap.containsKey(groupName)) {
									errorMap.put(groupName, new ErrorStatistics());
									statMap.put(groupName, new HashMap<String, DescriptiveStatistics>());
									groupNames.add(groupName);
								}
								myGroupNames.add(groupName);
							}
						} else if (i==1) {
							// do nothing
						} else {
							String rowName = cells.get(0);
							int j=0;
							for (String groupName : myGroupNames) {
								ErrorStatistics errorStats = errorMap.get(groupName);
								Map<String,DescriptiveStatistics> stats = statMap.get(groupName);
								double correctCount = Double.parseDouble(cells.get(j*5+1));
								double errorCount = Double.parseDouble(cells.get(j*5+2));
								double totalCount = Double.parseDouble(cells.get(j*5+3));
								Boolean haveCount = haveCountMap.get(groupName);
								
								if (rowName.equals("known")) {
									errorStats.knownWordCorrectCount += correctCount;
									errorStats.knownWordErrorCount += errorCount;
								} else if (rowName.equals("unknown")) {
									errorStats.unknownWordCorrectCount += correctCount;
									errorStats.unknownWordErrorCount += errorCount;
								} else if (rowName.equals("goodSeg")) {
									errorStats.goodSegCorrectCount += correctCount;
									errorStats.goodSegErrorCount += errorCount;
								} else if (rowName.equals("badSeg")) {
									errorStats.badSegCorrectCount += correctCount;
									errorStats.badSegErrorCount += errorCount;
								} else if (rowName.equals("knownLetters")) {
									errorStats.knownWordCorrectLetterCount += correctCount;
									errorStats.knownWordErrorLetterCount += errorCount;
								} else if (rowName.equals("unknownLetters")) {
									errorStats.unknownWordCorrectLetterCount += correctCount;
									errorStats.unknownWordErrorLetterCount += errorCount;
								} else if (rowName.equals("goodSegLetters")) {
									errorStats.goodSegCorrectLetterCount += correctCount;
									errorStats.goodSegErrorLetterCount += errorCount;
								} else if (rowName.equals("badSegLetters")) {
									errorStats.badSegCorrectLetterCount += correctCount;
									errorStats.badSegErrorLetterCount += errorCount;
								} else if (rowName.equals("inBeam")) {
									errorStats.answerInBeamCorrectCount += correctCount;
									errorStats.answerInBeamErrorCount += errorCount;
								} else if (rowName.equals("total")) {
									haveCountMap.put(groupName, totalCount>0);
								} else if (rowName.endsWith("%")) {
									if (haveCount) {
										String keyPrefix = rowName.substring(0, rowName.length()-1);
										String key = keyPrefix + "|correct";
										DescriptiveStatistics correctStat = stats.get(key);
										if (correctStat==null) {
											correctStat = new DescriptiveStatistics();
											stats.put(key, correctStat);
										}
										correctStat.addValue(correctCount);
										key = keyPrefix + "|error";
										DescriptiveStatistics errorStat = stats.get(key);
										if (errorStat==null) {
											errorStat = new DescriptiveStatistics();
											stats.put(key, errorStat);
										}
										errorStat.addValue(errorCount);
										key = keyPrefix + "|total";
										DescriptiveStatistics totalStat = stats.get(key);
										if (totalStat==null) {
											totalStat = new DescriptiveStatistics();
											stats.put(key, totalStat);
										}
										totalStat.addValue(totalCount);
									}
								}
								
								j++;
							}
						}
						i++;
					}
				} else {
					Scanner scanner = new Scanner(file);
					boolean firstLine = true;
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						if (firstLine) {
							if (firstFile)
								writer.write(line + "\n");
							firstLine = false;
						} else {
							writer.write(line + "\n");
						}
						writer.flush();
					}
				} // file type
			} // next file
			
			Writer statsWriter = writers.get("KEMatrix");
			writeStats(statsWriter, errorMap);
			statsWriter.write("\n");
			String[] statTypes = new String[] { "known", "unknown", "goodSeg", "badSeg", "inBeam", "total", "knownLetter", "unknownLetter", "goodSegLetter", "badSegLetter", "totalLetter" };
			for (String statType : statTypes) {
				for (String groupName : groupNames) {
					Map<String,DescriptiveStatistics> statsMap = statMap.get(groupName);
					DescriptiveStatistics correctStat = statsMap.get(statType + "|correct");
					DescriptiveStatistics errorStat = statsMap.get(statType + "|error");
					DescriptiveStatistics totalStat = statsMap.get(statType + "|total");
					
					statsWriter.write(CSVFormatter.format(statType+ "%Avg") + CSVFormatter.format(correctStat.getMean()) + CSVFormatter.format(errorStat.getMean()) + CSVFormatter.format(totalStat.getMean()) + CSVFormatter.getCsvSeparator());
			
				} // next group
				statsWriter.write("\n");
				for (String groupName : groupNames) {
					Map<String,DescriptiveStatistics> statsMap = statMap.get(groupName);
					DescriptiveStatistics correctStat = statsMap.get(statType + "|correct");
					DescriptiveStatistics errorStat = statsMap.get(statType + "|error");
					DescriptiveStatistics totalStat = statsMap.get(statType + "|total");
					
					statsWriter.write(CSVFormatter.format(statType+ "%Dev") + CSVFormatter.format(correctStat.getStandardDeviation()) + CSVFormatter.format(errorStat.getStandardDeviation()) + CSVFormatter.format(totalStat.getStandardDeviation()) + CSVFormatter.getCsvSeparator());
			
				} // next group
				statsWriter.write("\n");
				statsWriter.flush();
			}
			statsWriter.close();
			
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}


}
