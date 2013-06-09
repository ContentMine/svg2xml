package org.xmlcml.svg2xml.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.IntArray;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.euclid.RealRangeArray;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.svg2xml.analyzer.TextAnalyzerX;

/** holds one or more TextLines in a chunk
 * bounding boxes of textLines overlap
 * @author pm286
 *
 */
public class ScriptLine implements Iterable<TextLine> {


	private final static Logger LOG = Logger.getLogger(ScriptLine.class);
	
	private static final double X_CHARACTER_TOL = 0.45;  // heuristic
	private static final double FONT_SIZE_EPS = 0.02;  // heuristic
	private static final double SUSCRIPT_EPS = 0.5;  // heuristic
	public static final String SUB = "sub";
	public static final String SUP = "sup";
	public static final String SUSCRIPT = "suscript";

	private static final Double SPACEFACTOR = 0.12;
	
	protected List<TextLine> textLineList = null;
	private TextStructurer textContainer;
	private int largestLine;
	
	public ScriptLine(TextStructurer textContainer) {
		textLineList = new ArrayList<TextLine>();
		this.textContainer = textContainer;
	}
	
	public Iterator<TextLine> iterator() {
		return textLineList.iterator();
	}
	
	public int size() {
		return textLineList.size();
	}
	
	public void add(TextLine textLine) {
		textLineList.add(textLine);
	}
	
	public TextLine get(int i) {
		return textLineList.get(i);
	}

	/** generate ScriptLines by splitting into groups based around the commonest font size
	 * 
	 * @param textContainer
	 * @return
	 */
	public List<ScriptLine> splitIntoUniqueChunks(TextStructurer textContainer) {
		IntArray commonestFontSizeArray = createSerialNumbersOfCommonestFontSizeLines();
		List<ScriptLine> splitArray = new ArrayList<ScriptLine>();
		if (commonestFontSizeArray.size() < 2) {
			splitArray.add(this);
		} else {
			Integer lastCommonestFontSizeSerial = null;
			Integer groupStart = 0;
			Double lastY = null;
			for (int serial = 0; serial < textLineList.size(); serial++) {
				TextLine textLine = this.get(serial);
				Double currentY = textLine.getYCoord();
				if (textContainer.isCommonestFontSize(textLine)) {
					if (lastCommonestFontSizeSerial != null) {
						int delta = serial - lastCommonestFontSizeSerial;
						// two adjacent commonestFont lines
						if (delta == 1) {
							packageAsGroup(groupStart, lastCommonestFontSizeSerial, splitArray);
							groupStart = serial;
						} else if (delta == 2) {
							TextLine midLine = this.textLineList.get(serial - 1);
							Double midY = midLine.getYCoord();
							if (midY == null || lastY == null || currentY == null) {
								LOG.error("null "+midY+" / "+currentY + " / "+lastY);
							} else if (midY - lastY > currentY - midY) {
								packageAsGroup(groupStart, serial - 1, splitArray);
								groupStart = serial;
							} else {
								packageAsGroup(groupStart, lastCommonestFontSizeSerial, splitArray);
								groupStart = serial - 1;
							}
						} else if (delta == 3) {
							// assume subscript and then superscript
							packageAsGroup(groupStart, serial - 2, splitArray);
							groupStart = serial-1;
						} else {
							reportErrorOrMaths(splitArray);
						}
					} else {
						if (serial >= 2) {
							reportErrorOrMaths(splitArray);
						}
						// continue processing
					}
					lastCommonestFontSizeSerial = serial;
					lastY = textLineList.get(serial).getYCoord();
					// last line of group?
					if (serial == textLineList.size()-1) {
						packageAsGroup(groupStart, serial, splitArray);
					}
				}
			}
		}
		return splitArray;
	}

	private ScriptLine packageAsGroup(int groupStart, int groupEnd, List<ScriptLine> splitArray) {
		ScriptLine group = new ScriptLine(textContainer);
		Double maxFontSize = 0.0;
		largestLine = -1;
		int lineNumber = 0;
		for (int i = groupStart; i <= groupEnd; i++) {
			TextLine textLine = textLineList.get(i);
			Double fontSize = textLine.getFontSize();
			if (fontSize != null && fontSize > maxFontSize) {
				largestLine = lineNumber;
				maxFontSize = fontSize;
			}
			lineNumber++;
			group.add(textLine);
		}
		splitArray.add(group);
		return group;
	}

	private IntArray createSerialNumbersOfCommonestFontSizeLines() {
		int iline = 0;
		IntArray commonestFontSizeArray = new IntArray();
		for (TextLine textLine : textLineList) {
			if (textContainer.isCommonestFontSize(textLine)) {
				commonestFontSizeArray.addElement(iline);
				if (commonestFontSizeArray.size() > 1) {
					LOG.trace("COMMONEST FONT SIZE "+commonestFontSizeArray.size());
				}
			}
			iline++;
		}
		return commonestFontSizeArray;
	}
	
	public String summaryString() {
		StringBuilder sb = new StringBuilder("");
		List<ScriptWord> wordList = this.getWords();
		int i = 0;
		for (ScriptWord word : wordList) {
			if (i++ > 0 ) sb.append(" ");
			sb.append(word.summaryString());
		}
//		for (TextLine textLine : textLineList) {
//			sb.append(SVG2XMLUtil.trimText(30, textLine.getSpacedLineString())+"");
//		}
		sb.append("\n");
		return sb.toString();
	}
		
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		for (TextLine textLine : textLineList) {
			sb.append(textLine.getSpacedLineString()+"\n");
		}
		sb.append("----\n");
		return sb.toString();
	}
	
	
	public List<TextLine> createSuscriptTextLineList() {
		List<TextLine> outputTextLineList = null;
		TextLine superscript = null;
		TextLine middleLine = null;
		TextLine subscript = null;
		if (this.textLineList.size() == 1) {
			middleLine = textLineList.get(0);
		} else if (this.textLineList.size() == 2) {
			TextLine text0 = textLineList.get(0);
			TextLine text1 = textLineList.get(1);
			if (!textContainer.isCommonestFontSize(text0) && !textContainer.isCommonestFontSize(text1)) {
				Double fontSize0 = text0.getFontSize();
				Double fontSize1 = text1.getFontSize();
				if (fontSize1 == null) {
					superscript = null;
					middleLine = text0;
					subscript = text1;
				} else if(fontSize0 == null) {
					superscript = text0;
					middleLine = text1;
					subscript = null;
			    } else if(fontSize0 > fontSize1) {
					superscript = null;
					middleLine = text0;
					subscript = text1;
				} else {
					superscript = text0;
					middleLine = text1;
					subscript = null;
				}
			} else if (textContainer.isCommonestFontSize(text0) && !textContainer.isCommonestFontSize(text1)) {
					superscript = null;
					middleLine = text0;
					subscript = text1;
			} else if (!textContainer.isCommonestFontSize(text0) && textContainer.isCommonestFontSize(text1)) {
				superscript = textLineList.get(0);
				middleLine = textLineList.get(1);
				subscript = null;
			} else {
				for (TextLine tLine : textLineList) {
					LOG.trace(">>>> "+tLine);
				}
				LOG.error("Only one commonestFontSize allowed for 2 line textLineGroup");
			}
		} else if (this.textLineList.size() == 3) {
			if (!textContainer.isCommonestFontSize(textLineList.get(0)) &&
				!textContainer.isCommonestFontSize(textLineList.get(2))) {
				superscript = textLineList.get(0);
				middleLine = textLineList.get(1);
				subscript = textLineList.get(2);
			} else {
				reportErrorOrMathsSuscript();
				middleLine = new TextLine();
				subscript = null;
				superscript = null;
			}
		} else {
			reportErrorOrMathsSuscript();
			middleLine = new TextLine();
			subscript = null;
			superscript = null;
		}
		outputTextLineList = createSuscriptTextLineList(superscript, middleLine, subscript);
		return outputTextLineList;
	}

	/** NYI */
	private ScriptLine reportErrorOrMathsSuscript() {
		LOG.debug("Suscript problem: Maths or table? "+textLineList.size());
		ScriptLine group = new ScriptLine(textContainer);
//	    splitArray.add(group);

		for (TextLine textLine : textLineList) {
			LOG.trace("text "+textLine);
		}
		return group;
	}
	
	private ScriptLine reportErrorOrMaths(List<ScriptLine> splitArray) {
		LOG.debug("Maths or table? "+textLineList.size());
//		TextLineGroup group = new TextLineGroup();
		ScriptLine group = null;
	    splitArray.add(group);
	    splitArray.add(null);

		for (TextLine textLine : textLineList) {
			LOG.trace("text "+textLine);
		}
		return group;
	}
	
	/** preparation for HTML
	 * 
	 * @return
	 */
	public static List<TextLine> createSuscriptTextLineList(TextLine superscript, TextLine middleLine, TextLine subscript) {
		List<TextLine> textLineList = new ArrayList<TextLine>();
		if (subscript == null && middleLine == null && superscript == null) {
			textLineList.add(null);
			return textLineList;
		}
		List<SVGText> middleChars = middleLine == null ? null : middleLine.getCharacterList();
		Integer thisIndex = 0;
		List<SVGText> superChars = (superscript == null) ? new ArrayList<SVGText>() : superscript.getCharacterList();
		Integer superIndex = 0;
		List<SVGText> subChars = (subscript == null) ? new ArrayList<SVGText>() : subscript.getCharacterList();
		Integer subIndex = 0;
		TextLine textLine = null;
		while (true) {
			SVGText nextSup = TextLine.peekNext(superChars, superIndex);
			SVGText nextThis = TextLine.peekNext(middleChars, thisIndex);
			SVGText nextSub = TextLine.peekNext(subChars, subIndex);
			SVGText nextText = TextLine.textWithLowestX(nextSup, nextThis, nextSub);
			if (nextText == null) {
				break;
			}
			Suscript suscript = Suscript.NONE;
			if (nextText.equals(nextSup)) {
				superIndex++;
				suscript = Suscript.SUP;
			} else if (nextText.equals(nextThis)) {
				thisIndex++;
				suscript = Suscript.NONE;
			} else if (nextText.equals(nextSub)) {
				subIndex++;
				suscript = Suscript.SUB;
			}
			if (textLine == null || !(suscript.equals(textLine.getSuscript()))) {
				TextAnalyzerX textAnalyzerX = null;
				textLine = new TextLine(textAnalyzerX);
				textLine.setSuscript(suscript);
				textLineList.add(textLine);
			}
			textLine.add(nextText);
		}
		for (TextLine tLine : textLineList) {
			tLine.insertSpaces();
		}
		return textLineList;
	}

	public HtmlElement createHtml() {
		List<TextLine> lineList = this.createSuscriptTextLineList();
		HtmlElement element = TextLine.createHtmlElement(lineList);
		return element;
	}
	
	public TextLine getLargestLine() {
		return (textLineList.get(largestLine));
	}

	public boolean isBold() {
		return (textLineList.get(largestLine)).isBold();
	}

	public String getFontFamily() {
		return (textLineList.get(largestLine)).getFontFamily();
	}

	public Double getFontSize() {
		return (textLineList.get(largestLine)).getFontSize();
	}

	public Double getMeanFontSize() {
		return (textLineList.get(largestLine)).getMeanFontSize();
	}

	public List<TextLine> getTextLineList() {
		return textLineList;
	}
	
	public String getRawValue() {
		return (textLineList.get(largestLine)).getRawValue();
	}

	public List<SVGText> getTextList() {
		List<SVGText> textList = new ArrayList<SVGText>();
		for (TextLine textLine : textLineList) {
			textList.addAll(textLine.getSVGTextCharacters());
		}
		return textList;
	}
	
	public String toShortString() {
		String s = getLargestLine().toString();
		return s.substring(0, Math.min(20, s.length()));
	}

	public List<ScriptWord> getWords() {
		List<ScriptWord> wordList = new ArrayList<ScriptWord>();
		RealRangeArray rangeArray = this.getWordRangeArray();
		LOG.trace("WA "+rangeArray);
		List<SVGText> characters = this.getSVGTextCharacters();
		int rangeCounter = 0;
		int nlines = textLineList.size();
		IntArray lineCounterArray = new IntArray(nlines);
		// assume characters are sorted
		ScriptWord word = null;
		while (true) {
			SVGText character = null;
			RealRange currentRange = (rangeCounter >= rangeArray.size()) ? null : rangeArray.get(rangeCounter);
			Double lowestX = 9999999.;
			Integer lowestLine = null;
			SVGText lowestCharacter = null;
			for (int iline = 0; iline < textLineList.size(); iline++) {
				TextLine textLine = textLineList.get(iline);
				int lineCounter = lineCounterArray.elementAt(iline);
				character = (lineCounter >= textLine.size()) ? null : textLine.get(lineCounter);
				if (character != null) {
					double x = character.getX();
					if (x < lowestX) {
						lowestX = x;
						lowestLine = iline;
						lowestCharacter = character;
					}
				}
			}
			LOG.trace((lowestCharacter == null) ? "null" : "["+lowestCharacter.getValue()+"_"+lowestCharacter.getX()+"/"+lowestX+"/"+lowestLine);
			if (currentRange == null || lowestX <= currentRange.getMax()) {
				if (word == null) {
					word = new ScriptWord(nlines);
					wordList.add(word);
				}
				if (character == null) {
					break;
				}
				word.add(character, lowestLine);
				lineCounterArray.incrementElementAt(lowestLine);
			} else {
				word = null;
				rangeCounter++;
				currentRange = (rangeCounter >= rangeArray.size()) ? null : rangeArray.get(rangeCounter);
			}
			if (lowestLine == null && character == null) break;
		}
		return wordList;
	}

	public RealRangeArray getWordRangeArray() {
		Double fontSize = this.getMeanFontSize();
		if (fontSize == null) {
			fontSize = 8.0; // just in case
		}
		RealRangeArray wordRangeArray = new RealRangeArray();
		List<SVGText> characters = this.getSVGTextCharacters();
		for (SVGText character : characters) {
			wordRangeArray.add(character.getBoundingBox().getXRange());
		}
		wordRangeArray.extendRangesBy(X_CHARACTER_TOL);
		wordRangeArray.sortAndRemoveOverlapping();
		return wordRangeArray;
	}

	public List<SVGText> getSVGTextCharacters() {
		List<SVGText> characters = new ArrayList<SVGText>();
		for (TextLine textLine : textLineList) {
			characters.addAll(textLine.getSVGTextCharacters());
		}
		return characters;
	}

	public List<SVGText> getSortedCharacters() {
		List<SVGText> sortedCharacters = new ArrayList<SVGText>();
		int nline = textLineList.size();
		// make stacks of each textLine
		List<Stack<SVGText>> characterStackList = new ArrayList<Stack<SVGText>>(); 
		for (int i = 0; i < nline; i++) {
			Stack<SVGText> characterStack = new Stack<SVGText>();
			characterStackList.add(characterStack);
			TextLine textLine = textLineList.get(i);
			for (int j = textLine.size()-1; j >= 0; j--) {
				SVGText character = textLine.get(j);
				characterStack.push(character);
			}
		}
		// find lowest character X
		while (true) {
			Double x = null;
			int lowestLine = -1;
			Double xmin = 99999.;
			for (int i = 0; i < nline; i++) {
				Stack<SVGText> characterStack = characterStackList.get(i);
				if (!characterStack.isEmpty()) {
					x = characterStack.peek().getX();
					if (x < xmin) {
						lowestLine = i;
						xmin = x;
					}
				}
			}
			if (x == null) {
				break;
			}
			Stack<SVGText> lowestXStack = characterStackList.get(lowestLine);
			SVGText text = lowestXStack.pop();
			sortedCharacters.add(text);
		}
		return sortedCharacters;
	}

	/** creates spans whenever any of the following change
	 * bold
	 * italic
	 * fontSize
	 * yCoord
	 * fontName
	 * stroke
	 * fill
	 * @return
	 */
	public List<StyleSpan> getStyleSpanList() {
		List<StyleSpan> styleSpanList = new ArrayList<StyleSpan>();
//		List<SVGText> characters = getSVGTextCharacters();
		List<SVGText> characters = getSortedCharacters();
		StyleSpan currentSpan = null;
		boolean inBold = false;
		boolean inItalic = false;
		String currentFontName = null;
		Double currentFontSize = null;
		String currentFill = null;
		String currentStroke = null;
		Double currentY = null;
		Double lastX = null;
		SVGText character = null;
		String value = null;
		for (int i = 0; i < characters.size(); i++) {
			character = characters.get(i);
			boolean bold = character.isBold();
			boolean italic = character.isItalic();
			String fontName = character.getSVGXFontName();
			String fill = character.getFill();
			String stroke = character.getStroke();
			Double fontSize = character.getFontSize();
			Double x = character.getX();
			Double y = character.getY();
			value = character.getText();
			if (lastX != null) {
				double deltaX = x - lastX;
				if (deltaX > SPACEFACTOR *fontSize) {
					SVGText space = new SVGText();
					space.setText(" ");
					space.setXY(new Real2(lastX, y));
					currentSpan.addCharacter(space);
				}
			}
			// have any attributes changed?
			if (i == 0  || bold != inBold || italic != inItalic || 
					!areStringsEqual(currentFontName, fontName) ||
					!areStringsEqual(currentFill, fill) ||
					!areStringsEqual(currentStroke, stroke) ||
					!areDoublesEqual(currentFontSize, fontSize, FONT_SIZE_EPS) ||
					!areDoublesEqual(currentY, y, SUSCRIPT_EPS)
					) { 
				currentSpan = new StyleSpan(bold, italic);
				styleSpanList.add(currentSpan);
				// sub/superscript
				if (currentFontSize != null && fontSize != null && fontSize < currentFontSize) {
					if (currentY - y > 1.0  ) {
						SVGUtil.setSVGXAttribute(character, SUSCRIPT, SUP);
					} else if (y - currentY > 1.0  ) {
						SVGUtil.setSVGXAttribute(character, SUSCRIPT, SUB);
					}
				}
				inBold = bold;
				inItalic = italic;
				currentFontName = fontName;
				currentFontSize = fontSize;
				currentFill = fill;
				currentStroke = stroke;
				currentY = y;
			}
			currentSpan.addCharacter(character);
			lastX = character.getBoundingBox().getXRange().getMax();
		}
		return styleSpanList;
	}

	private boolean areStringsEqual(String s0, String s1) {
		return (s0 == null && s1 == null) || 
			(s0 != null && s0.equals(s1)) ||
			(s1 != null && s1.equals(s0));
			   
	}
	
	private boolean areDoublesEqual(Double d0, Double d1, Double eps) {
		return d0 != null && d1 != null && Real.isEqual(d0, d1, eps);
			   
	}
	
}