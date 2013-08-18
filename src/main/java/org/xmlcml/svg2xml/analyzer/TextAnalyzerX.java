package org.xmlcml.svg2xml.analyzer;


import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Attribute;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.euclid.EuclidConstants;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Array;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealArray;
import org.xmlcml.euclid.Univariate;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGTSpan;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.svg2xml.container.AbstractContainer;
import org.xmlcml.svg2xml.container.ScriptContainer;
import org.xmlcml.svg2xml.text.SimpleFont;
import org.xmlcml.svg2xml.text.SvgPlusCoordinate;
import org.xmlcml.svg2xml.text.TextLine;
import org.xmlcml.svg2xml.text.TextStructurer;
import org.xmlcml.svg2xml.text.TextStructurer.Splitter;
import org.xmlcml.svg2xml.textextra.Paragraph;
import org.xmlcml.svg2xml.textextra.Word;
import org.xmlcml.svg2xml.textextra.WordSequence;
import org.xmlcml.svg2xml.tools.Chunk;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/** attempts to assemble characters into meaningful text
 * 
 * @author pm286
 *
 */
public class TextAnalyzerX extends AbstractAnalyzer {

	private static final String ID_PREFIX = "textChunk";
	private final static Logger LOG = Logger.getLogger(TextAnalyzerX.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	public static final String TEXT1 = "text1";
	public static final String CHUNK = "chunk";
	private static final Double INDENT = 6.0; // appears to be about 1.5 char widths for BMC
	public static final String TEXT = "TEXT";
	private static final String WORD_LIST = "wordList";
	private static final String PREVIOUS = "previous";
	private static final double YPARA_SEPARATION_FACTOR = 1.15;
	
	public static final double DEFAULT_TEXTWIDTH_FACTOR = 0.9;
	public static final int NDEC_FONTSIZE = 3;
	public static final double INDENT_MIN = 1.0; //pixels
	public static Double TEXT_EPS = 1.0;


	private TextLine rawCharacterList;
	private Map<Integer, TextLine> characterByXCoordMap;
	@Deprecated
	private Map<Integer, TextLine> characterByYCoordMap;
	private Map<String, RealArray> widthByCharacter;
	private Multimap<Integer, TextLine> subLineByYCoord;
	private List<TextLine> horizontalCharacterList;
	private List<WordSequence> wordSequenceList;

	private Map<String, Double> medianWidthOfCharacterNormalizedByFontMap;
	private RealArray spaceSizes;

	public List<Real2Range> emptyYTextBoxes;
	public List<Real2Range> emptyXTextBoxes;

	private Chunk chunk;
	private SVGElement svgParent;
	private List<Paragraph> paragraphList;

	private List<Chunk> textChunkList;
	// refactor
	private SimpleFont simpleFont;
	
	private boolean subSup;
	private boolean removeNumericTSpans;
    private List<SVGText> textCharacters;
	
	/** refactored container */
	private TextStructurer textStructurer;
	private HtmlElement createdHtmlElement;
	
	public TextAnalyzerX() {
		super();
	}
	
	public TextAnalyzerX(List<SVGText> characterList) {
		this.setTextCharacters(characterList);
	}

	public TextAnalyzerX(SVGElement svgElement) {
		this(SVGText.extractTexts(svgElement));
	}

	public String getTag() {
		return TEXT1;
	}
	
	public Map<Integer, TextLine> getCharacterByXCoordMap() {
		return characterByXCoordMap;
	}

	public Map<String, RealArray> getWidthByText() {
		return widthByCharacter;
	}

	public Multimap<Integer, TextLine> getLineByYCoord() {
		return subLineByYCoord;
	}

	public void analyzeTexts(List<SVGText> textCharacters) {
		if (textCharacters == null) {
			throw new RuntimeException("null characters: ");
		} else {
			this.textCharacters = textCharacters;
			ensureTextContainerWithSortedLines().sortLineByXandMakeTextLineByYCoordMap(textCharacters);
		}
	}


	public List<TextLine> getLinesInIncreasingY() {
		return ensureTextContainerWithSortedLines().getLinesInIncreasingY();
	}

	private void analyzeSpaces() {
		this.getWidthByCharacterNormalizedByFontSize();
		this.getMedianWidthOfCharacterNormalizedByFont();
		this.getSpaceSizes();
	}

//	private TextChunkRUN analyzeRawText(Chunk chunk) {
//		this.chunk = chunk;
//		this.svgParent = (chunk != null) ? chunk : pageEditorX.getSVGPage();
//		
//		this.getRawTextCharacterList();
//		this.createRawTextCharacterPositionMaps();
//		this.createHorizontalCharacterListAndCreateWords();
//		analyzeSpaces();
//		TextChunkRUN textChunk = this.createTextChunkWithParagraphs();
//		return textChunk;
//	}
	
	private void getRawCharacterList(List<SVGElement> textElements) {
		this.rawCharacterList = new TextLine(this);
		for (int i = 0; i < textElements.size(); i++) {
			SVGText text = (SVGText) textElements.get(i);
			text.setBoundingBoxCached(true);
			rawCharacterList.add(text);
		}
	}
	
	/** puts all characters (usually SVGText of length 1) into
	 * a single container
	 * @return
	 */
	public TextLine getRawTextCharacterList() {
		if (rawCharacterList == null) {
			List<SVGElement> textElements = SVGUtil.getQuerySVGElements(svgParent, ".//svg:text");
			getRawCharacterList(textElements);
			LOG.trace("read "+rawCharacterList.size()+" raw characters "+rawCharacterList.toString());
		}
		return rawCharacterList;
	}
	
	/** creates integer indexed maps of all character positions
	 * indexes to nearest pixel
	 */
	private void createRawTextCharacterPositionMaps() {
		if (characterByXCoordMap == null) {
			getRawTextCharacterList();
			characterByXCoordMap = new HashMap<Integer, TextLine>();
			characterByYCoordMap = new HashMap<Integer, TextLine>();
			for (SVGText rawCharacter : rawCharacterList) {
				Real2 xy = SVGUtil.getTransformedXY(rawCharacter);
				Integer xx = (int) Math.round(xy.getX());
				Integer yy = (int) Math.round(xy.getY());
				addCharacterToMap(characterByXCoordMap, xx, rawCharacter);
				addCharacterToMap(characterByYCoordMap, yy, rawCharacter);
			}
			LOG.trace("createRawTextCharacterPositionMaps: X int coords "+characterByXCoordMap.size()+
					"  / Y int coords: " +characterByYCoordMap.size());
		}
	}

	/** sorts lines into coordinate order
	 * 
	 */
	private void sortCharacterLineMaps() {
		if (subLineByYCoord == null) {
			createRawTextCharacterPositionMaps();
			Set<Integer> keys = characterByYCoordMap.keySet();
			Integer[] yList = keys.toArray(new Integer[keys.size()]);
			Arrays.sort(yList);
			subLineByYCoord = ArrayListMultimap.create();
			for (Integer y : yList) {
				TextLine line = characterByYCoordMap.get(y);
				line.sortLineByX();
				List<TextLine> splitLines = line.getSubLines();
				for (TextLine subLine : splitLines) {
					addSubLine(y, subLine);
				}
			}
			LOG.trace("created subLines: "+subLineByYCoord.size());
		}
	}
	
	private void addSubLine(Integer y, TextLine subLine) {
		subLineByYCoord.put(y, subLine);
		subLine.setY(y);
	}
	
	/** create a list of the sorted sublines in the document
	 * 
	 * @return
	 */
	@Deprecated // use createWords separately
	private List<TextLine> createHorizontalCharacterListAndCreateWords() {
		Long time0 = System.currentTimeMillis();
		if (horizontalCharacterList == null) {
			makeHorizontalCharacterList();
			createWordsInSublines();
		}
		LOG.trace("createYSortedLineList "+(System.currentTimeMillis()-time0));
		return horizontalCharacterList;
	}

	private void makeHorizontalCharacterList() {
		if (horizontalCharacterList == null) {
			sortCharacterLineMaps();
			Set<Integer> keys = subLineByYCoord.keySet();
			Integer[] yList = keys.toArray(new Integer[keys.size()]);
			Arrays.sort(yList);
			this.horizontalCharacterList = new ArrayList<TextLine>();
			for (Integer y : yList) {
				Collection<TextLine> subLineList = subLineByYCoord.get(y);
				LOG.trace("SUBLINELIST "+subLineList.size());
				for (TextLine subLine : subLineList) {
					horizontalCharacterList.add(subLine);
				}
			}
			LOG.trace("created lines: "+horizontalCharacterList.size());
			if (Level.TRACE.equals(LOG.getLevel())) {
				for (TextLine cl : horizontalCharacterList) {
					LOG.trace("sorted line "+cl.toString());
				}
			}
		}
	}
	
	private void createWordsInSublines() {
		for (TextLine subline : horizontalCharacterList) {
			LOG.trace("SUBLINE "+subline);
//			LOG.trace("Guess "+subline.guessAndApplySpacingInLine());
			WordSequence wordSequence = subline.createWords();
			LOG.trace("WordSeq "+wordSequence.getWords().size()+" .. "+wordSequence.getStringValue());
		}
	}
	
	/** uses all lines to estimate the width of a character
	 * 
	 * @param lineListlist
	 * @return all observed widths of the character
	 */
	private Map<String, RealArray> getWidthByCharacterNormalizedByFontSize() {
		if (widthByCharacter == null) {
			createHorizontalCharacterListAndCreateWords();
			widthByCharacter = new HashMap<String, RealArray>();
			for (TextLine characterList : horizontalCharacterList) {
				Double fontSize = characterList.getFontSize();
				Map<String, RealArray> widthByCharacter0 = characterList.getInterCharacterWidthsByCharacterNormalizedByFont();
				for (String charr : widthByCharacter0.keySet()) {
					RealArray widthList0 = widthByCharacter0.get(charr);
					RealArray widthList = widthByCharacter.get(charr);
					if (widthList == null) {
						widthList = new RealArray();
						widthByCharacter.put(charr, widthList);
					}
					widthList.addArray(widthList0);
				}
			}
		}
		return widthByCharacter;
	}

	/** estimates the likely width of a character
	 * maybe this should be the mode?
	 * @param widthByCharacter
	 * @return
	 */
	private Map<String, Double> getMedianWidthOfCharacterNormalizedByFont() {
		if (medianWidthOfCharacterNormalizedByFontMap == null) {
			getWidthByCharacterNormalizedByFontSize();
			medianWidthOfCharacterNormalizedByFontMap = new HashMap<String, Double>();
			for (String charr : widthByCharacter.keySet()) {
				RealArray widthList = widthByCharacter.get(charr);
				Univariate univariate = new Univariate(widthList);
				Double median = univariate.getMedian();
				medianWidthOfCharacterNormalizedByFontMap.put(charr, median);
			}
		}
		return medianWidthOfCharacterNormalizedByFontMap;
	}

	/** surveys all the inter-character spaces and attempts to create space metrics
	 * uses all lines on page
	 * @return
	 */
	private RealArray getSpaceSizes() {
		spaceSizes = new RealArray();
		getMedianWidthOfCharacterNormalizedByFont();
		for (TextLine sortedLine : horizontalCharacterList) {
			RealArray interCharacterSpaces = sortedLine.getCharacterWidthArray();
			for (int i = 0; i < sortedLine.size()-1; i++) {
				String charr = sortedLine.get(i).getValue();
				Double medianWidth = medianWidthOfCharacterNormalizedByFontMap.get(charr);
				double space = SVGUtil.decimalPlaces(interCharacterSpaces.get(i) - medianWidth, 3);
				if (space > 0.1) {
					spaceSizes.addElement(space);
				}
			}
		}
		return spaceSizes;
	}
	
	public RealArray getMeanFontSizeArray() {
		return ensureTextContainerWithSortedLines().getMeanFontSizeArray();
	}

	public void debug() {
		debug("xmap", characterByXCoordMap);
		debug("ymap", characterByYCoordMap);
	}

	private void addCharacterToMap(Map<Integer, TextLine> map, Integer x, SVGText rawCharacter) {
		TextLine textList = map.get(x);
		if (textList == null) {
			textList = new TextLine(this);
			map.put(x, textList);
		}
		textList.add(rawCharacter);
	}

//	/** creates a list of the listOfWordsInLine
//	 * 
//	 * @param simpleFont
//	 * @return
//	 */
//	private TextChunkRUN createTextChunkWithParagraphs() {
//		createWordsAndAddToWordSequenceListRUN();
//		textChunk = new TextChunkRUN(chunk, wordSequenceList);
//		paragraphList = textChunk.createParagraphs();
//		addParagraphsAsSVG();
//		return textChunk;
//	}
	
	private void addParagraphsAsSVG() {
		for (Paragraph paragraph : paragraphList) {
			paragraph.detach();
			chunk.appendChild(paragraph);
		}
	}

	public List<WordSequence> createWordsAndAddToWordSequenceListRUN() {
		ensureSimpleFont();
		if (wordSequenceList == null) {
			RealArray ySeparationArray = new RealArray();
			Double lastY = null;
			if (svgParent != null) {
				Real2Range bbox = svgParent.getBoundingBox();
				if (bbox != null) {
					Double xLeft = bbox.getXRange().getMin();
					Double deltaY = null;
					LOG.trace("XL "+xLeft);
					createHorizontalCharacterListAndCreateWords();
					Double interlineSeparation = getInterlineSeparation();
					wordSequenceList = new ArrayList<WordSequence>();
					for (TextLine sortedLine : horizontalCharacterList) {
						boolean added = false;
						WordSequence wordSequence = sortedLine.createWords();
						Real2 xy = wordSequence.getXY();
						double y = xy.getY();
						if (lastY != null) {
							deltaY = y - lastY;
							if (interlineSeparation != null && deltaY > YPARA_SEPARATION_FACTOR * interlineSeparation) {
								addParagraphMarker();
								added = true;
							}
						}
						lastY = y;
						if (!added) {
							Double indent = xy.getX() - xLeft;
							if (indent > INDENT) {
								addParagraphMarker();
							}
						}
						wordSequenceList.add(wordSequence);
					}
				}
			}
		}
		return wordSequenceList;
	}

	private Double getInterlineSeparation() {
		// TODO Auto-generated method stub
		return null;
	}

	private void addParagraphMarker() {
		WordSequence paragraphMarkerSequence = WordSequence.createParagraphMarker();
		wordSequenceList.add(paragraphMarkerSequence);
	}

//	public void analyzeTextChunksCreateWordsLinesParasAndSubSupRUN(List<SVGElement> chunkElements) {
//		TextChunkRUN lastMainTextChunk = null;
//		TextChunkRUN lastScriptTextChunk = null;
//		TextChunkRUN textChunk = null;
//		List<Chunk> chunks = TextAnalyzerUtils.castToChunks(chunkElements);
//		ensureTextChunkList();
//		int id = 0;
//		for (Chunk chunk : chunks) {
//			if (chunk.isTextChunk()) {
//				chunk.setId(ID_PREFIX+id);
//				if (SVGUtil.getQuerySVGElements(chunk, "svg:g").size() == 0) {
//					TextAnalyzerX textAnalyzer = new TextAnalyzerX();
//					textChunk = textAnalyzer.analyzeRawText(chunk);
//					// processing a probable sub/superscript
//					if (textChunk.isSuscript()) {
//						lastScriptTextChunk = analyzeSubSup(lastMainTextChunk, lastScriptTextChunk, textChunk);
//					} else {
//						analyzeBodyText(lastScriptTextChunk, textChunk);
//						lastMainTextChunk = textChunk;
//					}
//					ensureParagraphList();
//					List<Paragraph> paraList = textAnalyzer.getParagraphList();
//					paragraphList.addAll(paraList);
//				}
//				Chunk parentChunk = (Chunk)chunk.getParent();
////				parentChunk.setChunkStyleValue(ChunkStyle.TEXT_CHUNK);
////				removeIfSubSup(chunk);
//				TextAnalyzerUtils.cleanAndWordWrapText(chunk);
//				parentChunk.addAttribute(new Attribute(CHUNK, TEXT));
//				textChunkList.add(parentChunk);
//			}
//			id++;
//		}
//	}
	
	public void analyzeSingleWordsOrLinesRUN(List<SVGElement> elements) {
		horizontalCharacterList = new ArrayList<TextLine>();
		subLineByYCoord = ArrayListMultimap.create();
		for (SVGElement element : elements) {
			if (hasNoSVGGChildren()) {
				TextAnalyzerX textAnalyzer = new TextAnalyzerX();
				textAnalyzer.analyzeRawText(element);
				horizontalCharacterList.addAll(textAnalyzer.horizontalCharacterList);
				for (TextLine subline : horizontalCharacterList) {
					addSubLine(subline.getIntegerY(), subline);
				}
			}
		}
	}
	
	private boolean hasNoSVGGChildren() {
		return SVGUtil.getQuerySVGElements(chunk, "svg:g").size() == 0;
	}
	
	private void analyzeRawText(SVGElement element) {
		this.svgParent = element;
		this.getRawTextCharacterList();
		this.createRawTextCharacterPositionMaps();
		this.createHorizontalCharacterListAndCreateWords();
		analyzeSpaces();
		createWordsAndAddToWordSequenceListRUN();
		copyWordSequencesToParent();
		detachRawCharacters();
	}

	private void copyWordSequencesToParent() {
		if (rawCharacterList.size() > 0) {
			LOG.trace("WordSequences "+wordSequenceList.size());
			SVGElement parent = (SVGElement) rawCharacterList.get(0).getParent();
			if (parent == null) {
				LOG.warn("No parent for rawCharacterList: "+rawCharacterList);
			} else {
				LOG.trace("P "+parent.getId());
				int wsSerial = 0;
				for (WordSequence ws : wordSequenceList) {
					SVGText svgText = ws.createSVGText();
					LOG.trace("Text "+svgText.getId());
					List<Word> words = ws.getWords();
					LOG.trace("words "+words.size());
					if (words.size() > 0) {
						SVGG wordListG = new SVGG();
						wordListG.setClassName(WORD_LIST);
						wordListG.setId("tw_"+words.get(0).getId());
						if (!Word.S_PARA.equals(svgText.getValue())) {
							parent.appendChild(wordListG);
							for (Word word : words) {
								word.addToParentAndReplaceCharacters(wordListG);
							}
						} else {
							for (Word word : words) {
								word.detachCharacters();
							}
						}
						LOG.trace("TXT "+svgText.getValue()+" "+svgText.toXML());
					}
				}
			}
		}
	}

	private void detachRawCharacters() {
		for (SVGText charx : rawCharacterList) {
			charx.detach();
		}
	}
	
	private void ensureTextChunkList() {
		if (textChunkList == null) {
			textChunkList = new ArrayList<Chunk>();
		}
	}

	private void ensureParagraphList() {
		if (paragraphList == null) {
			paragraphList = new ArrayList<Paragraph>();
		}
	}

	private List<Paragraph> getParagraphList() {
		return paragraphList;
	}

//	private TextChunkRUN analyzeBodyText(TextChunkRUN lastScriptTextChunk, TextChunkRUN textChunk) {
//		if (lastScriptTextChunk != null) {
//			if (textChunk.hasSuperscriptsIn(lastScriptTextChunk)) {
//				textChunk.addSuperscriptsFrom(lastScriptTextChunk);
//				textChunk.detachChunk();
//				lastScriptTextChunk = null;
//			}
//		}
//		return lastScriptTextChunk;
//	}
//	
//	private TextChunkRUN analyzeSubSup(
//			TextChunkRUN lastMainTextChunk, TextChunkRUN lastScriptTextChunk, TextChunkRUN textChunk) {
//		if (lastMainTextChunk != null) {
//			if (lastMainTextChunk.hasSubscriptsIn(textChunk)) {
//				lastMainTextChunk.addSubscriptsFrom(textChunk);
//				textChunk.detachChunk();
//				lastScriptTextChunk = null;
//			} else {
//				// save for superscript
//				lastScriptTextChunk = textChunk;
//			}
//		}
//		return lastScriptTextChunk;
//	}
//
//	public void mergeChunksRUN() {
//		List<SVGElement> textChunkList1 = SVGUtil.getQuerySVGElements(pageEditorX.getSVGPage(), ".//svg:g[@"+TEXT+"='"+CHUNK+"']");
//		for (SVGElement textChunk : textChunkList1) {
//			List<SVGElement> paraList = SVGUtil.getQuerySVGElements(textChunk, "./*/svg:g[@"+Paragraph.NAME+"='"+Paragraph.PARA+"']");
//			for (SVGElement paraElement : paraList) {
//				Paragraph paragraph = Paragraph.createElement(paraElement);
//				paragraph.createAndAddHtml();
//			}
//		}
//	}

	public SimpleFont ensureSimpleFont() {
		if (this.simpleFont == null) {
			simpleFont = SimpleFont.SIMPLE_FONT;
			if (this.simpleFont == null) {
				//throw new RuntimeException("Cannot make simpleFont");
				LOG.warn("Cannot make simpleFont");
			}
		}
		return simpleFont;
	}

	public List<WordSequence> getWordSequenceList() {
		return wordSequenceList;
	}

//	/** merge subsups and remove old subsup elements
//	 * assumes lines are in order Y slowest, X fastest
//	 * not yet recursive
//	 * 
//	 * @param texts
//	 * @return
//	 */
//	private void mergeSubSup() {
//		if (subSup) {
//			List<SVGText> texts = SVGText.extractTexts(SVGUtil.getQuerySVGElements(svgg, ".//svg:g[@class='word']/svg:text"));
//			LOG.trace("SUBSUP....."+texts.size());
//			mergeSubSup(texts);
//		}
//	}

	public void mergeSubSup(List<SVGText> texts) {
		SubSupAnalyzerX subSupAnalyzerX = new SubSupAnalyzerX(this);
		subSupAnalyzerX.mergeTexts(texts);
	}

	public void splitAtSpaces() {
		// select texts which contains spaces
		List<SVGText> texts = SVGText.extractTexts(SVGUtil.getQuerySVGElements(
				svgg, ".//svg:text[contains(text(),' ')] | .//svg:tspan[contains(.,' ')]"));
		for (SVGText text : texts) {
			splitAtSpaces(text);
		}
	}

	public void splitAtSpaces(SVGText textOrSpan) {
		String s = textOrSpan.getText();
		String id = textOrSpan.getId();
		String coords = textOrSpan.getAttributeValue(Word.COORDS);
		List<SVGTSpan> spans = textOrSpan.getChildTSpans();
		if (spans.size() > 0) {
			processSpans(textOrSpan, spans);
		} else {
			Real2Array real2Array = (coords == null) ? null : Real2Array.createFromCoords(coords);
			if (real2Array == null || s == null || real2Array.size() !=s.length()) {
				LOG.trace("Cannot match array: "+coords);
				real2Array = null;
			} else {
				processLeafSpanOrText(textOrSpan, s, id, real2Array);
			}
		}
	}

	private void processLeafSpanOrText(SVGText textOrSpan, String s, String id, Real2Array real2Array) {
		Integer index = null;
		SVGText parent = null;
		if (textOrSpan instanceof SVGTSpan) {
			parent = (SVGText) textOrSpan.getParent();
			index = parent.indexOf(textOrSpan);
			textOrSpan.detach();
		} else {
			parent = textOrSpan;
			parent.setText(null);
		}
		SVGTSpan lastSpan = null;
		int last = 0;
		int l = s.length();
		for (int i = 0; i < l; i++) {
			if (s.charAt(i) == EuclidConstants.C_SPACE || i == l-1) {
				i = (i == l-1) ? l : i;
				String ss = s.substring(last, i);
				if (ss.trim().length() > 0) {
					SVGTSpan tSpan = new SVGTSpan(real2Array.get(last), ss);
					CMLUtil.copyAttributes(textOrSpan, tSpan);
					tSpan.setId(id+"_"+last);
					Real2Array subArray = real2Array.createSubArray(last, i-1);
					tSpan.addAttribute(new Attribute(Word.COORDS, subArray.toString()));
					tSpan.setXY(subArray.get(0));
					if (index == null) {
						parent.appendChild(tSpan);
					} else {
						parent.insertChild(tSpan, index);
						index++;
					}
					if (lastSpan != null) {
						tSpan.addAttribute(new Attribute(PREVIOUS, lastSpan.getId()));
					}
					lastSpan = tSpan;
				}
				last = i+1;
			}
		}
	}
	
	private void processSpans(SVGText textOrSpan, List<SVGTSpan> spans) {
		textOrSpan.setText(null);
		for (SVGTSpan span : spans) {
			splitAtSpaces(span);
		}
	}

	public void setSubSup(boolean subSup) {
		this.subSup = subSup;
	}

	public void setRemoveNumericTSpans(boolean removeNumericTSpans) {
		this.removeNumericTSpans = removeNumericTSpans;
	}

	public List<TextLine> getTextLines() {
		return ensureTextContainerWithSortedLines().getTextLineList();
	}

	/** creates one "para" per line
	 * usually needs tidying with createHtmlDivWithParas
	 * @return
	 */
	public HtmlElement createHtmlRawDiv() {
		ensureTextContainerWithSortedLines();
//		List<TextLine> textLineList = textContainer.getLinesWithLargestFont();
		List<TextLine> textLineList = textStructurer.getLinesWithCommonestFont();
		HtmlDiv div = new HtmlDiv();
		for (TextLine textLine : textLineList) {
			HtmlElement p = textLine.createHtmlLine();
			div.appendChild(p);
		}
		return div;
	}

	/** creates one "para" per line
	 * usually needs tidying with createHtmlDivWithParas
	 * @return
	 */
	public static HtmlElement createHtmlRawDiv(List<TextLine> linesToBeAnalyzed) {
		HtmlDiv div = new HtmlDiv();
		for (TextLine textLine : linesToBeAnalyzed) {
			HtmlElement p = textLine.createHtmlLine();
			div.appendChild(p);
		}
		return div;
	}

	public HtmlElement createHtmlDivWithParas() {
		ensureTextContainerWithSortedLines();
		HtmlElement div = textStructurer.createHtmlDivWithParas();
		return div;
	}

	
	private TextStructurer ensureTextContainerWithSortedLines() {
		if (this.textStructurer == null) {
			this.textStructurer = TextStructurer.createTextStructurerWithSortedLines(textCharacters, this);
		} else {
			this.textStructurer.sortLineByXandMakeTextLineByYCoordMap(textCharacters);
		}
		return this.textStructurer;
	}
	
	@Override
	public SVGG annotateChunk(List<? extends SVGElement> svgElements) {
		return annotateElements(svgElements, 0.2, 0.7, 5.0, "pink");
	}

	
	
	

	// ===========utils============================
	
	private void debug(String string, Map<Integer, TextLine> textByCoordMap) {
		if (textByCoordMap == null) {
			LOG.debug("No textCoordMap "+textStructurer.getTextLineByYCoordMap());
		} else {
			Set<Integer> keys = textByCoordMap.keySet();
			Integer[] ii = keys.toArray(new Integer[keys.size()]);
			Arrays.sort(ii);
			for (int iz : ii) {
				TextLine textList = textByCoordMap.get(iz);
				for (SVGText text : textList) {
					LOG.trace(">> "+text.getXY()+" "+text.getText()+ " ");
				}
			}
//			System.out.println();
		}
	}


	public List<SVGText> getTextCharacters() {
		return textCharacters;
	}
	
	public void setTextCharacters(List<SVGText> textCharacters) {
		this.textCharacters = textCharacters;
	}

	// =========== Delegates ============
	public Double getMainInterTextLineSeparation(int decimalPlaces) {
		return ensureTextContainerWithSortedLines().getMainInterTextLineSeparation(decimalPlaces);
	}

	public RealArray getInterTextLineSeparationArray() {
		return ensureTextContainerWithSortedLines().getInterTextLineSeparationArray();
	}

	public TextStructurer getTextContainer() {
		return ensureTextContainerWithSortedLines();
	}

	public List<TextLine> getLinesWithLargestFont() {
		return ensureTextContainerWithSortedLines().getLinesWithCommonestFont();
	}

	public Real2Range getTextLinesLargestFontBoundingBox() {
		return ensureTextContainerWithSortedLines().getLargestFontBoundingBox();
	}

	public Integer getSerialNumber(TextLine textLine) {
		return ensureTextContainerWithSortedLines().getSerialNumber(textLine);
	}

	public List<String> getTextLineContentList() {
		return ensureTextContainerWithSortedLines().getTextLineContentList();
	}

	public void insertSpaces() {
		ensureTextContainerWithSortedLines().insertSpaces();
	}

	public RealArray getTextLineCoordinateArray() {
		return ensureTextContainerWithSortedLines().getTextLineCoordinateArray();
	}

	public Multimap<SvgPlusCoordinate, TextLine> getTextLineListByFontSize() {
		return ensureTextContainerWithSortedLines().getTextLineListByFontSize();
	}

	public void insertSpaces(double d) {
		ensureTextContainerWithSortedLines().insertSpaces(d);
	}

	public void getTextLineByYCoordMap() {
		ensureTextContainerWithSortedLines().getTextLineByYCoordMap();
	}

	public RealArray getModalExcessWidthArray() {
		return ensureTextContainerWithSortedLines().getModalExcessWidthArray();
	}

	public Multiset<Double> createSeparationSet(int decimalPlaces) {
		return ensureTextContainerWithSortedLines().createSeparationSet(decimalPlaces);
	}

	public List<Double> getActualWidthsOfSpaceCharactersList() {
		return ensureTextContainerWithSortedLines().getActualWidthsOfSpaceCharactersList();
	}

	public void setTextStructurer(TextStructurer textStructurer) {
		this.textStructurer = textStructurer;
	}

	protected HtmlElement createHtml() {
		LOG.trace("createHTMLParasAndDivs");
		List<TextLine> textLines = this.getLinesInIncreasingY();
		LOG.trace("lines "+textLines.size());
		for (TextLine textLine : textLines){
			LOG.trace(">> "+textLine);
		}
		createdHtmlElement = this.createHtmlDivWithParas();
		if (createdHtmlElement != null) {
			AbstractAnalyzer.tidyStyles(createdHtmlElement);
		}
		return createdHtmlElement;
	}
	
	//FIXME to use Splitters customized for different dataTypes  and parameters
	/** splits svgg into textContainers using a list of splitters
	 * 
	 * @param gOrig
	 * @param chunkId
	 * @param splitters
	 * @return
	 */
	public List<TextStructurer> createSplitTextContainers(SVGG gOrig, ChunkId chunkId, Splitter ...splitters) {
		TextStructurer textContainer = new TextStructurer(this);
		textContainer.getScriptedLineList();
		List<TextStructurer> splitTLCList = new ArrayList<TextStructurer>();
		splitTLCList.add(textContainer);
		for (Splitter splitter : splitters) {
			List<TextStructurer> newSplitTLCList = new ArrayList<TextStructurer>();
			for (TextStructurer tlc : splitTLCList) {
				List<TextStructurer> splitList = textContainer.split(splitter);
				LOG.debug("SPLIT: "+splitList);
				newSplitTLCList.addAll(splitList);
			}
			splitTLCList = newSplitTLCList;
		}
		LOG.debug("SPLIT "+splitTLCList.size());
		return splitTLCList;
	}

	/** counter is container counter
	 * 
	 * @param analyzerX
	 * @param suffix
	 * @param pageAnalyzer
	 * @return
	 */
	@Override
	public List<AbstractContainer> createContainers(PageAnalyzer pageAnalyzer) {
		TextStructurer textContainer1 = this.getTextContainer();
		textContainer1.getScriptedLineList();
		List<TextStructurer> splitList = textContainer1.splitOnFontBoldChange(-1);
		List<TextStructurer> textContainerList = splitList;
		LOG.trace(" split LIST "+textContainerList.size());
		if (textContainerList.size() > 1) {
			splitBoldHeaderOnFontSize(textContainerList);
		}
		ensureAbstractContainerList();
		for (TextStructurer textContainer : textContainerList) {
			ScriptContainer scriptContainer = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
//			scriptContainer.setChunkId(textContainer.getChunkId());
			scriptContainer.setChunkId(this.getChunkId());
			abstractContainerList.add(scriptContainer);
		}
		return abstractContainerList;
	}

	private void splitBoldHeaderOnFontSize(List<TextStructurer> textContainerList) {
		TextStructurer textContainer0 = textContainerList.get(0);
		if (textContainer0.getScriptedLineList().size() > 1) {
			textContainer0.getScriptedLineList();
			List<TextStructurer> splitList = textContainer0.splitOnFontSizeChange(999);
			List<TextStructurer> fontSplitList =
				splitList;
			if (fontSplitList.size() > 1) {
				int index = textContainerList.indexOf(textContainer0);
				textContainerList.remove(index);
				for (TextStructurer splitTC : fontSplitList) {
					textContainerList.add(index++, splitTC);
				}
				LOG.trace("SPLIT FONT");
			}
		}
	}


	
}
