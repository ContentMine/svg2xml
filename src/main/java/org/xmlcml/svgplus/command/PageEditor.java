package org.xmlcml.svgplus.command;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.graphics.svg.SVGClipPath;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.svgplus.core.SVGPlusConstants;
import org.xmlcml.svgplus.core.SemanticDocumentAction;
import org.xmlcml.svgplus.figure.Figure;
import org.xmlcml.svgplus.figure.FigureAnalyzer;
import org.xmlcml.svgplus.paths.PathAnalyzer;
import org.xmlcml.svgplus.table.Table;
import org.xmlcml.svgplus.table.TableAnalyzer;
import org.xmlcml.svgplus.text.TextAnalyzer;
import org.xmlcml.svgplus.tools.PageChunkSplitterAnalyzer;
import org.xmlcml.svgplus.tools.PageClipPathAnalyzer;
import org.xmlcml.svgplus.tools.PageFontSizeAnalyzer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

/** the key class controlling the analysis of a page.
 * 
 * PageInterpreter analyzes a page without other context (although its results can be fed back to
 * DocumentAnalyzer. (It might be called PageAnalyzer but that is visually close to PathAnalyzer)
 * 
 * It has several sub-analyzers (e.g. 
 * ChunkAnalyzer, TextAnalyzer, PathAnalyzer, FigureAnalyzer, TableAnalyzer
 * which are deployed in sequence (and possibly multiple times) on the single page. The results of
 * these are left in the transformed page but may also be abstracted and aggregated 
 * back to DocumentAnalyzer (in this way overall heuristic data can be compiled)
 * These analyzers all have references to PageInterpreter
 * 
 * Any "stitching together" of pages will be done by document analyzer.
 * 
 * @author pm286
 *
 */
public class PageEditor {

	private static final Logger LOG = Logger.getLogger(PageEditor.class);
	
	private static final String SHAPE_RENDERING = "shape-rendering";
	private static final String SPACE = "space";
	private static final String TEXT_RENDERING = "text-rendering";
	
	private static final String AFTER_CHUNK = "afterChunk";
	private static final String AFTER_TEXT = "afterText";
	private static final String AFTER_PATH = "afterPath";

	public static final int DECIMAL_PLACES = 3;

	public static final String ROLE = "role";
	public static final String CHAR = "char";
	public static final String RECT = "rect";
	public static final String POLYLINE = "polyline";
	public static final String PATH = "path";
	public static final String USE = "use";
	public static final String FIRST = "first";
	public static final String NOT_FIRST = "notFirst";
	public static final String LAST = "last";
	public static final String REPORTED_PAGE_NUMBER = "reportedPageNumber";
	private static final String BEFORE_TEXT = "beforeText";
	public static final String NAME_PREFIX = SVGPlusConstants.P;

	private BiMap<String, String> clipPathByIdMap;
	
	private SVGSVG svgPage;             
	private int pageNumber;
	private String pageNumberString;
	private Integer pageNumberInteger;
	private List<SVGText> textChunkList;
	private List<Figure> figureList;
	private List<Table> tableList;

	private PathAnalyzer pathAnalyzer;
	private PageClipPathAnalyzer clipPathAnalyzer;
	private PageFontSizeAnalyzer fontSizeAnalyzer;
	private PageChunkSplitterAnalyzer pageChunkSplitter;
	private TextAnalyzer textAnalyzer;
	private FigureAnalyzer figureAnalyzer;
	private TableAnalyzer tableAnalyzer;
	private ChunkAnalyzer currentChunkAnalyzer;

	private Integer rotationAngle;

	private SemanticDocumentAction semanticDocumentAction;

	private PageEditor() {
	}
	
	public PageEditor(SemanticDocumentAction semanticDocumentAction) {
		this();
		this.semanticDocumentAction = semanticDocumentAction;
	}

	public static void removeUnwantedSVGAttributesAndAddIds(SVGSVG svgPage) {
		Long time0 = System.currentTimeMillis();
		List<SVGElement> elements = SVGUtil.getQuerySVGElements(svgPage, "//svg:*");
		List<Attribute> attributeList = new ArrayList<Attribute>();
		int attno = 0;
		for (SVGElement element : elements) {
			for (int i = 0; i < element.getAttributeCount(); i++) {
				Attribute attribute = element.getAttribute(i);
				String name = attribute.getLocalName();
				if (name.equals(TEXT_RENDERING) ||
				    name.equals(SHAPE_RENDERING) ||
					name.equals(SPACE)) {
					attributeList.add(attribute);
				}
			}
			if (element.getId() == null) {
				element.setId(element.getLocalName()+(attno++));
			}
		}

		for (Attribute attribute : attributeList) {
			attribute.detach();
		}
		LOG.trace("ATTS "+(System.currentTimeMillis()-time0));
	}
	

	// ========================= TRANSFERRED ELSEWHERE ======================
	
	public PathAnalyzer ensurePathAnalyzer() {
		if (pathAnalyzer == null) {
			pathAnalyzer = new PathAnalyzer(semanticDocumentAction);
		}
		return pathAnalyzer;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public PathAnalyzer getPathAnalyzer() {
		return pathAnalyzer;
	}


	public TextAnalyzer getTextAnalyzer() {
		return textAnalyzer;
	}

	/** returns a conventional page number (e.g. at top or bottom of page)
	 * formally unrelated to the number of pages in the document though normally a constant offset
	 * @return the number as a string (might be "22a", etc.)
	 */
	public String getAuthorPageNumberString() {
		return pageNumberString;
	}
	
	/** returns a conventional page number (e.g. at top or bottom of page)
	 * formally unrelated to the number of pages in the document though normally a constant offset
	 * @return the number as an integer (null if unparsable as such)
	 */
	public Integer getAuthorPageNumber() {
		return pageNumberInteger;
	}
	
	/**
	 * get chunks of text in PDF order. Requires heuristics to decide what they are
	 * @return
	 */
	public List<SVGText> getTextChunkList() {
		return textChunkList;
	}
	
	/**
	 * get chunks of text in PDF order. Requires heuristics to decide what they are
	 * @return
	 */
	public List<Figure> getFigureList() {
		ensureFigureList();
		return figureList;
	}
	
	private void ensureFigureList() {
		if (figureList == null) {
			figureList = new ArrayList<Figure>();
		}
	}

	public void analyzeClipPathsAndAddToMap(Multimap<String, PageEditor> clipPathDMap) {
		analyzeClipPaths();
		addClipPathsToMap(clipPathDMap);
	}

	public void analyzeClipPaths() {
		ensureClipPathAnalyzer();
		clipPathAnalyzer.analyze();
	}

	private void ensureClipPathAnalyzer() {
		if (clipPathAnalyzer == null) {
			clipPathAnalyzer = new PageClipPathAnalyzer(semanticDocumentAction);
		}
	}

	public void addClipPathsToMap(Multimap<String, PageEditor> clipPathDMap) {
		List<SVGElement> clipPaths  = this.getPageClipPathAnalyzer().getClipPathList();
		for (SVGElement clipPath : clipPaths) {
			SVGPath path = (SVGPath) ((SVGClipPath) clipPath).getChildElements().get(0);
			String d = path.getDString();
			LOG.trace(""+this.getPageNumber()+" .. "+d);
			clipPathDMap.put(d, this);
		}
	}

	public void analyzeFontSizesAndAddToMap(Multimap<Integer, PageEditor> fontSizeMap) {
		analyzeFontSizes();
		addFontSizesToMap(fontSizeMap);
	}

	private void analyzeFontSizes() {
		ensureFontSizeAnalyzer();
		fontSizeAnalyzer.analyze();
	}

	public PageFontSizeAnalyzer ensureFontSizeAnalyzer() {
		if (fontSizeAnalyzer == null) {
			fontSizeAnalyzer = new PageFontSizeAnalyzer(semanticDocumentAction);
		}
		return fontSizeAnalyzer;
	}

	public PageChunkSplitterAnalyzer ensurePageChunkSplitter() {
		if (pageChunkSplitter == null) {
			pageChunkSplitter = new PageChunkSplitterAnalyzer(semanticDocumentAction);
		}
		return pageChunkSplitter;
	}

	public TextAnalyzer ensureTextAnalyzer() {
		if (textAnalyzer == null) {
			textAnalyzer = new TextAnalyzer(semanticDocumentAction);
		}
		return textAnalyzer;
	}

	public FigureAnalyzer ensureFigureAnalyzer() {
		if (figureAnalyzer == null) {
			figureAnalyzer = new FigureAnalyzer(semanticDocumentAction);
		}
		return figureAnalyzer;
	}

	public TableAnalyzer ensureTableAnalyzer() {
		if (tableAnalyzer == null) {
			tableAnalyzer = new TableAnalyzer(semanticDocumentAction);
		}
		return tableAnalyzer;
	}

	private void applyBrowserScale() {
		List<SVGElement> gList = SVGUtil.getQuerySVGElements(svgPage, ".//svg:g[@id='"+PageChunkSplitterAnalyzer.TOP_CHUNK+"']");
		if (gList.size() != 1) {
			LOG.error("should have one topChunk G");
		} else {
			gList.get(0).setTransform(Transform2.applyScale(0.7));
		}
	}
	

	public void addFontSizesToMap(Multimap<Integer, PageEditor> fontSizeMap) {
		Multimap<Integer, SVGElement> elementsByFontSize = fontSizeAnalyzer.createMapsForElementsByFontSize();
		for (Integer fontSize : elementsByFontSize.keySet()) {
			fontSizeMap.put(fontSize, this);
		}
	}
	
	public SVGSVG getSVGPage() {
		return svgPage;
	}

	public void setSVGPage(SVGSVG svgPage) {
		this.svgPage = svgPage;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public PageClipPathAnalyzer getPageClipPathAnalyzer() {
		return clipPathAnalyzer;
	}

	public PageFontSizeAnalyzer getPageFontSizeAnalyzer() {
		return fontSizeAnalyzer;
	}

	public PageChunkSplitterAnalyzer getPageChunkSplitter() {
		return pageChunkSplitter;
	}

	public List<Table> getTableList() {
		ensureTableList();
		return tableList;
	}

	private void ensureTableList() {
		if (tableList == null) {
			tableList = new ArrayList<Table>();
		}
	}

	/** because the unscaled clipPaths can hide the scaled stuff
	 * use with care as the defs are used for styles - i.e. only at the end
	 * 
	 */
	public void removeClipPathsForDisplay() {
		List<SVGElement> clipPathDefs = SVGUtil.getQuerySVGElements(
				svgPage, "svg:g/svg:defs/svg:clipPath");
		for (SVGElement def : clipPathDefs) {
			def.detach();
		}
	}

	private static void usage() {
		System.err.println("Usage: <svgfilein>");
	}
	
	public boolean islastPage(int pageNumber) {
		boolean isLastPage = false;
		Object lastPageS = /*documentAnalyzer*/semanticDocumentAction.getVariable(DocumentAnalyzer.REPORTED_PAGE_COUNT);
		if (lastPageS != null && lastPageS instanceof String) {
			try {
				Integer lastPage = new Integer((String)lastPageS);
				isLastPage = lastPage == pageNumber + 1; // we count from ZERO, document counts from 1
			} catch (Exception e) {
				throw new RuntimeException("bad page number: "+lastPageS, e);
			}
		}
		return isLastPage;
	}
	/** tru if no pages range or pages='first' and page==0
	 * 
	 * @param chunkStyle TODO
	 * @param pageNumber
	 * @return
	 */
	public boolean isAllowedPage(int pageNumber) {
		boolean allowed = false;
		String range = null;
//		String range = chunkStyle.element.getAttributeValue(ChunkStyle.PAGE_RANGE);
		if (range == null) {
			allowed = true; // no page range, allowed
		} else if (FIRST.equals(range) && pageNumber == 0) {
			allowed = true;
		} else if (NOT_FIRST.equals(range) && pageNumber > 0) {
			allowed = true;
		} else if (LAST.equals(range) && islastPage(pageNumber)) {
			allowed = true;
		} else {
			allowed = false;
		}
		return allowed;
	}

	/**
   <clipPath clipPathUnits="userSpaceOnUse" id="clipPath1">
    <path d="M0 0 L60.9419 0 L60.9419 81.2217 L0 81.2217 L0 0 Z"/>
   </clipPath>
	 */
	public BiMap<String, String> ensureClipPathByIdMap() {
		if (clipPathByIdMap == null) {
			this.clipPathByIdMap = HashBiMap.create();
			List<SVGElement> clipPaths = SVGUtil.getQuerySVGElements(svgPage, "svg:g/svg:defs/svg:clipPath");
			for (SVGElement clipPath : clipPaths) {
				String id = clipPath.getId();
				String d = clipPath.getChildElements().get(0).getAttributeValue("d");
				try {
					clipPathByIdMap.put(id,  d);
				} catch (IllegalArgumentException iae) {
					LOG.trace("clip path failure: "+iae);
				}
			}
		}
		return clipPathByIdMap;
	}

	public String getNamePrefix() {
		return NAME_PREFIX;
	}

	public void setCurrentChunkAnalyzer(ChunkAnalyzer chunkAnalyzer) {
		this.currentChunkAnalyzer = chunkAnalyzer;
	}

	public ChunkAnalyzer getCurrentChunkAnalyzer() {
		return currentChunkAnalyzer;
	}

	public void setRotationAngle(Integer angle) {
		this.rotationAngle = angle;
	}

	public SemanticDocumentAction getSemanticDocumentAction() {
		return semanticDocumentAction;
	}

	public void setSemanticDocumentAction(SemanticDocumentAction semanticDocumentAction) {
		this.semanticDocumentAction = semanticDocumentAction;
	}
}