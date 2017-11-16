package org.xmlcml.svg2xml.table;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGShape;
import org.xmlcml.graphics.svg.SVGTitle;
import org.xmlcml.svg2xml.util.GraphPlot;

/** manages the table footer
 * 
 * @author pm286
 *
 */
public class TableFooterSection extends TableSection {
	static final String FOOTER_TITLE = "footer.title";
	static final Logger LOG = Logger.getLogger(TableFooterSection.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	public TableFooterSection() {
		super(TableSectionType.FOOTER);
	}
	
	public TableFooterSection(TableSection tableSection) {
		super(tableSection);
	}
	
	public SVGElement createMarkedContent(
			SVGElement svgChunk,
			String[] colors,
			double[] opacity) {
			SVGG g = createColumnBoxesAndShiftToOrigin(svgChunk, colors, opacity);
			svgChunk.appendChild(g);
			return svgChunk;
	}
	
	private SVGG createColumnBoxesAndShiftToOrigin(SVGElement svgChunk, String[] colors, double[] opacity) {
		SVGG g = new SVGG();
		g.setClassName(FOOTER_TITLE);
		if (boundingBox == null) {
			LOG.trace("no bounding box");
		} else {
			String title = FOOTER_TITLE+": "+this.getFontInfo()+" //" +this.getStringValue();
			SVGTitle svgTitle = new SVGTitle("footer: "+title);
			SVGShape plotBox = GraphPlot.plotBox(boundingBox, colors[1], opacity[1]);
			plotBox.appendChild(svgTitle);
			g.appendChild(plotBox);
			TableContentCreator.shiftToOrigin(svgChunk, g);
		}
		return g;
	}

	


}
