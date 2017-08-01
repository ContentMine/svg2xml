package org.xmlcml.svg2xml.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.euclid.IntRange;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.graphics.svg.GraphicsElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGShape;
import org.xmlcml.graphics.svg.SVGTitle;
import org.xmlcml.svg2xml.text.HorizontalElement;
import org.xmlcml.svg2xml.text.HorizontalRuler;
import org.xmlcml.svg2xml.text.Phrase;
import org.xmlcml.svg2xml.text.PhraseList;
import org.xmlcml.svg2xml.util.GraphPlot;

/** manages the table header, including trying to sort out the column spanning
 * 
 * @author pm286
 *
 */
public class TableHeaderSection extends TableSection {
	private static final String HEADER_BOXES = "header.boxes";
	static final String HEADER_COLUMN_BOXES = "header.columnBoxes";
	static final Logger LOG = Logger.getLogger(TableHeaderSection.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private List<HeaderRow> headerRowList;
	public TableHeaderSection() {
		super(TableSectionType.HEADER);
	}
	
	public TableHeaderSection(TableSection tableSection) {
		super(tableSection);
	}

	public void createHeaderRowsAndColumnGroups() {
		// assume this is sorted by Y; form raw colgroups and reorganize later
		createHeaderRowListAndUnassignedPhrases();
		createSortedColumnManagerListFromUnassignedPhrases(allPhrasesInSection);
	}

	private List<Phrase> createHeaderRowListAndUnassignedPhrases() {
		allPhrasesInSection = null;
		headerRowList = new ArrayList<HeaderRow>();
		Double lastY = null;
		HeaderRow headerRow = null;
		for (HorizontalElement element : getHorizontalElementList()) {
			if (element instanceof PhraseList) {
				if (allPhrasesInSection == null) {
					allPhrasesInSection = new ArrayList<Phrase>();
				}
				PhraseList phraseList = (PhraseList) element;
				allPhrasesInSection.addAll(phraseList.getOrCreateChildPhraseList());
			} else if (element instanceof HorizontalRuler) {
				HorizontalRuler ruler = (HorizontalRuler) element;
				Double y = ruler.getY();
				if (lastY == null || (y - lastY) > HorizontalRuler.Y_TOLERANCE) {
					headerRow = new HeaderRow();
					headerRowList.add(headerRow);
					lastY = y;
				}
				ColumnGroup columnGroup = new ColumnGroup();
				IntRange rulerRange = ruler.getIntRange();
				for (int i = allPhrasesInSection.size() - 1; i >= 0; i--) {
					Phrase phrase = allPhrasesInSection.get(i);
					// somewhere above the ruler (ignore stacked rulers at this stage
					if (rulerRange.includes(phrase.getIntRange()) && phrase.getY() < ruler.getY()) {
						allPhrasesInSection.remove(i);
						columnGroup.add(phrase);
						columnGroup.add(ruler);
						headerRow.add(columnGroup);
					}
				}
			}
		}
		return allPhrasesInSection;
	}

	public List<HeaderRow> getOrCreateHeaderRowList() {
		if (headerRowList == null) {
			headerRowList = new ArrayList<HeaderRow>();
		}
		return headerRowList;
	}
	
	public GraphicsElement createMarkedSections(
			GraphicsElement svgChunk,
			String[] colors,
			double[] opacity) {
		// write SVG
		SVGG g = createColumnBoxesAndTransformToOrigin(svgChunk, colors, opacity);
		svgChunk.appendChild(g);
		g = createHeaderBoxesAndTransformToOrigin(svgChunk, colors, opacity);
		svgChunk.appendChild(g);
		return svgChunk;
	}

	private SVGG createColumnBoxesAndTransformToOrigin(GraphicsElement svgChunk, String[] colors, double[] opacity) {
		SVGG g = new SVGG();
		g.setClassName(HEADER_COLUMN_BOXES);
		if (boundingBox == null) {
			LOG.trace("no bounding box");
		} else {
			RealRange yRange = boundingBox.getYRange();
			for (int i = 0; i < columnManagerList.size(); i++) {
				ColumnManager columnManager = columnManagerList.get(i);
				IntRange range = columnManager.getEnclosingRange();
				if (range != null) {
					RealRange xRange = new RealRange(range);
					ColumnGroup colGroup = nearestCoveringColumnGroup(xRange);
					RealRange yRange1 = colGroup == null ? yRange : 
						new RealRange(colGroup.getBoundingBox().getYRange().getMax(), yRange.getMax());
					String title = "HEADERCOLUMN: "+i+"/"+columnManager.getStringValue();
					SVGTitle svgTitle = new SVGTitle(title);
					SVGShape plotBox = GraphPlot.plotBox(new Real2Range(xRange, yRange1), colors[1], opacity[1]);
					plotBox.appendChild(svgTitle);
					g.appendChild(plotBox);
				}
			}
			TableContentCreator.shiftToOrigin(svgChunk, g);
		}
		return g;
	}

	private ColumnGroup nearestCoveringColumnGroup(RealRange xRange) {
		ColumnGroup columnGroup = null;
		double ymax = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < headerRowList.size(); i++) {
			HeaderRow headerRow = headerRowList.get(i);
			for (ColumnGroup colGroup : headerRow.getOrCreateColumnGroupList()) {
				Real2Range bbox = colGroup.getBoundingBox();
				RealRange colGroupXRange = bbox.getXRange();
				if (colGroupXRange.intersectsWith(xRange)) {
					if (bbox.getYMax() > ymax) {
						ymax = bbox.getYMax();
						columnGroup = colGroup;
					}
				}
			}
		}
		return columnGroup;
	}

	private SVGG createHeaderBoxesAndTransformToOrigin(GraphicsElement svgChunk, String[] colors, double[] opacity) {
		SVGG g = new SVGG();
		g.setClassName(HEADER_BOXES);
		for (int i = 0; i < headerRowList.size(); i++) {
			HeaderRow headerRow = headerRowList.get(i);
			for (ColumnGroup columnGroup : headerRow.getOrCreateColumnGroupList()) {
				Real2Range bbox = columnGroup.getBoundingBox();
				SVGShape plotBox = GraphPlot.plotBox(bbox, colors[1], opacity[1]);
				String title = "HEADERBOX: "+i;
				SVGTitle svgTitle = new SVGTitle(title);
				plotBox.appendChild(svgTitle);
				g.appendChild(plotBox);
			}
		}
		TableContentCreator.shiftToOrigin(svgChunk, g);
		return g;
	}

}
