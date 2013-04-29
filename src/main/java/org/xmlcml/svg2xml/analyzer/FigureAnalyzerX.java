package org.xmlcml.svg2xml.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import nu.xom.Nodes;

import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.svg2xml.action.SemanticDocumentActionX;
import org.xmlcml.svg2xml.figure.Figure;
import org.xmlcml.svg2xml.figure.FigurePanel;
import org.xmlcml.svg2xml.tools.Caption;
import org.xmlcml.svg2xml.tools.Chunk;

/**
 * @author pm286
 *
 */
public class FigureAnalyzerX extends AbstractPageAnalyzerX {

	static final Logger LOG = Logger.getLogger(FigureAnalyzerX.class);

	private static final String CHUNK_STYLE = "chunkStyle";
	public static final String FRAGMENT = "FRAGMENT";
	public static final String OUTLINE_BOX = "outlineBox";
	private static final Object USE_CAPTIONS = "useCaptions";

	public static final Pattern PATTERN = Pattern.compile("[Ff][Ii][Gg][Uu]?[Rr]?[Ee]?\\s*\\.?\\s*(\\d*).*", Pattern.DOTALL);
	public static final String TITLE = "FIGURE";
	
	private List<Figure> figureList;
	private List<FigurePanel> panelList;
	private String locationStrategy;

	private String[] clusterColours = new String[]{"red", "blue", "green", "cyan", "yellow", "magenta", "grey"};
	private Real2 clusterWhitespaceBoxMargins = new Real2(5.0, 5.0);
	private Double panelSeparation = 3.0;

	public FigureAnalyzerX(SemanticDocumentActionX semanticDocumentActionX) {
		super(semanticDocumentActionX);
	}
	
	public FigureAnalyzerX(PDFIndex pdfIndex) {
		super(pdfIndex);
	}
	
	public List<FigurePanel> createPanelsUsingWhitespace() {
		if (panelList == null) {
			panelList = new ArrayList<FigurePanel>();
			for (Figure figure : figureList) {
				List<FigurePanel> pList = figure.splitByHorizontalWhitespace();
				if (pList == null) {
					throw new RuntimeException("Null panelList");
				}
				panelList.addAll(pList);
			}
		}
		return panelList;
	}

	public void createFragmentsInsidePanelsForAllFigures() {
		for (Figure figure : figureList) {
			figure.createFragmentsInsidePanels();
		}
	}

	public List<Figure> createFigures() {
		if (locationStrategy == null) {
			throw new RuntimeException("Must give strategy");
		}
		if (OUTLINE_BOX.equals(locationStrategy)) {
			createFiguresFromBoxes();
		} else if (USE_CAPTIONS.equals(locationStrategy)) {
			getListOfFiguresAndAssembleFromCaptions();
		} else {
			throw new RuntimeException("Unknown or missing locationStrategy: "+locationStrategy);
		}
		return figureList;
	}

	public void setLocationStrategy(String strategy) {
		this.locationStrategy = strategy;
	}

	private List<Figure> createFiguresFromBoxes() {
		figureList = new ArrayList<Figure>();
		List<SVGElement> boxChunks = null;
//				List<SVGElement> boxChunks = SVGUtil.getQuerySVGElements(
//				svgPage, ".//svg:g[@"+CHUNK_STYLE+"='"+ChunkStyle.OUTLINED_BOX+"']");
		// we have already chunked the text, so look for captions
		for (SVGElement boxChunk : boxChunks) {
			/**
			 *  of form 
			 *  <g xmlns="http://www.w3.org/2000/svg" clipPath="clipPath8" chunkStyle="CAPTION">
			 *    <text style=" stroke  ... 
			 *    <g name="para">
                    <text style=" stroke : none;" x="62.929" y="473.365" font-size="8.0">Figure 1 Multi-gene phylogenetic ...
                  </g>
                </g>
			 */
			// A figure must have a caption (to avoid mention of Figure 1 in main text)
			Chunk caption = getCaptionFromBox(boxChunk);
			if (caption != null) {
				Figure figure = createFigure(boxChunk);
				boxChunk.getParent().replaceChild(boxChunk, figure);
				figure.addCaption(caption);
				figureList.add(figure);
			}
		}
		return figureList;
	}

	/**
	 * this is where figures have no box and we have to assemble from chunks
	 */
	private void getListOfFiguresAndAssembleFromCaptions() {
		// find captions
		List<SVGElement> boxChunks = null;
//		List<SVGElement> boxChunks = SVGUtil.getQuerySVGElements(
//				svgPage, ".//svg:g[@"+CHUNK_STYLE+"='"+ChunkStyle.OUTLINED_BOX+"']");
		for (SVGElement boxChunk : boxChunks) {
			Chunk caption = getCaptionFromBox(boxChunk);
			if (caption != null) {
				throw new RuntimeException("NYI");
			}
		}
	}

	private Figure createFigure(SVGElement chunk) {
		Figure figure = new Figure(this/*, figureStyle*/);
		figure.copyAttributesAndChildrenFromSVGElement(chunk);
		figure.createElementListAndCalculateBoundingBoxes(/*chunk*/);
		return figure;
	}

	/** caption is identified by "Fig. " or "Figure " as dictated by stylesheet
	 * 
	 * @param boxChunk
	 * @return
	 */
	private Chunk getCaptionFromBox(SVGElement boxChunk) {
		Chunk caption = null;
		String startsWith = null;
		List<SVGElement> captionElements = SVGUtil.getQuerySVGElements(
				boxChunk, "./svg:g[svg:g[@name='para']/svg:text[starts-with(., '"+startsWith+"')]]");
		if (captionElements.size() == 1) {
			caption = createCaptionAndReplace((Chunk)captionElements.get(0));
		}
		return caption;
	}

	private Chunk createCaptionAndReplace(Chunk chunk) {
		Chunk caption = new Caption(chunk);
		caption.copyAttributesAndChildrenFromSVGElement(chunk);
		chunk.detach();
		caption.createElementListAndCalculateBoundingBoxes();
		return caption;
	}

	public void setColourClusters(String col) {
		if (col == null || col.trim().length() == 0) {
			clusterColours = null;
		} else {
			clusterColours = col.split(CMLConstants.S_WHITEREGEX);
		}
	}

	public String[] getColourClusters() {
		return clusterColours;
	}
	
	public Real2 getClusterWhitespaceBoxMargins() {
		return clusterWhitespaceBoxMargins;
	}

	public void setClusterMargins(String mm) {
		this.clusterWhitespaceBoxMargins = new Real2(mm);
	}
	
	public void setClusterMargins(Real2 clusterWhitespaceBoxMargins) {
		this.clusterWhitespaceBoxMargins = clusterWhitespaceBoxMargins;
	}
	
	public Double getPanelSeparation() {
		return panelSeparation;
	}

	public void setPanelSeparation(String s) {
		try {
			Double d = new Double(s);
			this.panelSeparation = d;
		} catch (Exception e) {
			throw new RuntimeException("Bad panel separation: "+s);
		}
	}
	
	public void setPanelSeparation(Double panelSeparation) {
		this.panelSeparation = panelSeparation;
	}
	
	@Override
	public SVGG labelChunk() {
		throw new RuntimeException("annotate NYI");
	}
	
	public Integer indexAndLabelChunk(String content, ChunkId id, Set<ChunkId> usedChunkSet) {
		Integer serial = super.indexAndLabelChunk(content, id);
		// index...
		return serial;
	}
	
	/** Pattern for the content for this analyzer
	 * 
	 * @return pattern (default null)
	 */
	@Override
	protected Pattern getPattern() {
		return PATTERN;
	}

	/** (constant) title for this analyzer
	 * 
	 * @return title (default null)
	 */
	@Override
	public String getTitle() {
		return TITLE;
	}

}
