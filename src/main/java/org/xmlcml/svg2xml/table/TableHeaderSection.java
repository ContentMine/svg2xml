package org.xmlcml.svg2xml.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.euclid.IntRange;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGRect;
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
		createSortedColumnManagerListFromUnassignedPhrases(phrases);
	}

	private List<Phrase> createHeaderRowListAndUnassignedPhrases() {
		phrases = null;
		headerRowList = new ArrayList<HeaderRow>();
		Double lastY = null;
		HeaderRow headerRow = null;
		for (HorizontalElement element : getHorizontalElementList()) {
			if (element instanceof PhraseList) {
				if (phrases == null) {
					phrases = new ArrayList<Phrase>();
				}
				PhraseList phraseList = (PhraseList) element;
				phrases.addAll(phraseList.getOrCreateChildPhraseList());
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
				for (int i = phrases.size() - 1; i >= 0; i--) {
					Phrase phrase = phrases.get(i);
					// somewhere above the ruler (ignore stacked rulers at this stage
					if (rulerRange.includes(phrase.getIntRange()) && phrase.getY() < ruler.getY()) {
						phrases.remove(i);
						columnGroup.add(phrase);
						columnGroup.add(ruler);
						headerRow.add(columnGroup);
					}
				}
			}
		}
		return phrases;
	}

	public List<HeaderRow> getOrCreateHeaderRowList() {
		if (headerRowList == null) {
			headerRowList = new ArrayList<HeaderRow>();
		}
		return headerRowList;
	}
	
	public SVGElement createMarkedSections(
			SVGElement svgChunk,
			String[] colors,
			double[] opacity) {
		// write SVG
		SVGG g = createColumnBoxesAndTransformToOrigin(svgChunk, colors, opacity);
		svgChunk.appendChild(g);
		g = createHeaderBoxesAndTransformToOrigin(svgChunk, colors, opacity);
		svgChunk.appendChild(g);
		return svgChunk;
	}

	private SVGG createColumnBoxesAndTransformToOrigin(SVGElement svgChunk, String[] colors, double[] opacity) {
		SVGG g = new SVGG();
		if (boundingBox == null) {
			LOG.warn("no bounding box");
		} else {
			RealRange yRange = boundingBox.getYRange();
			for (int i = 0; i < columnManagerList.size(); i++) {
				ColumnManager columnManager = columnManagerList.get(i);
				RealRange xRange = new RealRange(columnManager.getEnclosingRange());
				ColumnGroup colGroup = nearestCoveringColumnGroup(xRange);
				RealRange yRange1 = colGroup == null ? yRange : 
					new RealRange(colGroup.getBoundingBox().getYRange().getMax(), yRange.getMax());
				String title = columnManager.getStringValue();
				SVGTitle svgTitle = new SVGTitle(title);
				SVGRect plotBox = GraphPlot.plotBox(new Real2Range(xRange, yRange1), colors[1], opacity[1]);
				plotBox.appendChild(svgTitle);
				g.appendChild(plotBox);
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

	private SVGG createHeaderBoxesAndTransformToOrigin(SVGElement svgChunk, String[] colors, double[] opacity) {
		SVGG g = new SVGG();
		for (int i = 0; i < headerRowList.size(); i++) {
			HeaderRow headerRow = headerRowList.get(i);
			for (ColumnGroup columnGroup : headerRow.getOrCreateColumnGroupList()) {
				Real2Range bbox = columnGroup.getBoundingBox();
				g.appendChild(GraphPlot.plotBox(bbox, colors[1], opacity[1]));
			}
		}
		TableContentCreator.shiftToOrigin(svgChunk, g);
		return g;
	}

}