package org.xmlcml.svg2xml.analyzer;

import java.util.ArrayList;

import java.util.List;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGRect;
import org.xmlcml.svgplus.action.SemanticDocumentActionX;
import org.xmlcml.svgplus.paths.Axis;
import org.xmlcml.svgplus.paths.ComplexLine;
import org.xmlcml.svgplus.paths.ComplexLine.CombType;
import org.xmlcml.svgplus.paths.ComplexLine.LineOrientation;
import org.xmlcml.svgplus.tools.PlotBox;

public class AxisAnalyzerX extends AbstractPageAnalyzerX {

	static final Logger LOG = Logger.getLogger(AxisAnalyzerX.class);

	public static final String AXES_BOX = "axesBox";
	public static final double _MAJOR_MINOR_TICK_RATIO = 1.1;

	private List<ComplexLine> horizontalComplexLines;
	private List<SVGLine> horizontalLines;
	private List<ComplexLine> verticalComplexLines;
	private List<SVGLine> verticalLines;

	private double maxTickLength = 50.d;
	private double minTickLength = 1.0d;
	public double jointEps = 0.5;
	private int minJointCount = 2;
	private int maxJointCount = 999;
	private double boxThickness = 100.;
	private double boxLengthExtension = 50.;

	private List<Axis> horizontalAxisList;
	private List<Axis> verticalAxisList;
	private Axis verticalAxis;
	private Axis horizontalAxis;
	
	private SVGElement container;
	public double eps;

	private PlotBox plotBox;

	public AxisAnalyzerX(SVGElement container, SemanticDocumentActionX semanticDocumentActionX) {
		super(semanticDocumentActionX);
		this.container = container;
	}
	
	public void createVerticalHorizontalAxisList(List<SVGLine> svgLines, double eps) {
		this.eps = eps;
		if (verticalAxisList == null) {
			if (svgLines.get(0).getParent() == null) {
				throw new RuntimeException("NO PARENT ");
			}
			this.verticalLines = ComplexLine.createSubset(svgLines, LineOrientation.VERTICAL, eps);
			this.horizontalLines = ComplexLine.createSubset(svgLines, LineOrientation.HORIZONTAL, eps);
			this.verticalComplexLines = ComplexLine.createComplexLines(this.verticalLines, this.horizontalLines, eps);
			this.horizontalComplexLines = ComplexLine.createComplexLines(this.horizontalLines, this.verticalLines, eps);
			horizontalAxisList = createAxisList(horizontalComplexLines, LineOrientation.HORIZONTAL);
			if (horizontalAxisList.size() == 1) {
				this.horizontalAxis = horizontalAxisList.get(0);
			}
			// not fully implemented
			verticalAxisList = createAxisList(verticalComplexLines, LineOrientation.VERTICAL);
			if (verticalAxisList.size() == 1) {
				this.verticalAxis = verticalAxisList.get(0);
			}
			plotBox = createPlotBox();
			LOG.trace("FINISHED AXES");
		}
	}
	
	private PlotBox createPlotBox() {
		if (horizontalAxis != null && verticalAxis != null) {
			plotBox = new PlotBox(horizontalAxis, verticalAxis);
			LOG.debug("PLOT BOX "+plotBox);
			drawBox();
		}
		return plotBox;
	}
	
	private void drawBox() {
		SVGRect bbox =plotBox.createRect();
		bbox.setClassName(AXES_BOX);
		bbox.setOpacity(0.3);
		bbox.setStroke("cyan");
		bbox.setStrokeWidth(5.0);
		container.appendChild(bbox);
	}

	public Axis getVerticalAxis() {
		return verticalAxis;
	}

	public Axis getHorizontalAxis() {
		return horizontalAxis;
	}


	/** create axis for given orientation
	 * 
	 * @param complexLines
	 * @param orientation
	 * @return
	 */
	private List<Axis> createAxisList(List<ComplexLine> complexLines, LineOrientation orientation) {
		 List<Axis> axisList = new ArrayList<Axis>();
		 if (complexLines != null) {
			for (ComplexLine complexLine : complexLines) {
				Axis axis = createAxis(complexLine, orientation);
				if (axis != null) {
					axisList.add(axis);
//					container.debug("AXIS CONT");
					axis.processScaleValuesAndTitles(container);
					axis.createAxisGroup();
					LOG.debug("************  AXIS "+axis);
				}
			}
		}
		 return axisList;
	}

	private Axis createAxis(ComplexLine complexLine, LineOrientation orientation) {
		Axis axis = new Axis(this);
		if (!orientation.equals(axis.getOrientation())) {
//			throw new RuntimeException("Inconsistent axis orientation");
		}
		axis.setId("a_"+complexLine.getBackbone().getId());
		axis.setComplexLine(complexLine);
		complexLine.setMinMaxJointLength(minTickLength, maxTickLength);
		complexLine.setMinJointCount(2);
		complexLine.setRequirePerpendicularJoints(true);
		CombType combType = complexLine.getCombType();
		if (combType != null) {
			axis.trimJointList(complexLine.getJointList(), minTickLength, maxTickLength);
			axis.setCombType(ComplexLine.getCombType(axis.getMinorTickJointList(), minJointCount, maxJointCount));
		}
		if (axis.getCombType() != null) {
			axis.analyzeMajorMinorTicks(complexLine);
			LOG.trace(" ++++++++ AXIS "+axis.toString());
		} else {
			axis = null;
		}
		return axis;
	}

	public double getMaxTickLength() {
		return maxTickLength;
	}

	public void setMaxTickLength(double maxTickLength) {
		this.maxTickLength = maxTickLength;
	}

	public double getBoxLengthExtension() {
		return boxLengthExtension;
	}

	public void setBoxLengthExtension(double boxLengthExtension) {
		this.boxLengthExtension = boxLengthExtension;
	}

	public double getBoxThickness() {
		return boxThickness;
	}

	public void setBoxThickness(double boxThickness) {
		this.boxThickness = boxThickness;
	}

	public PlotBox getPlotBox() {
		return plotBox;
	}

}