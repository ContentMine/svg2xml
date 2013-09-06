package org.xmlcml.svg2xml.paths;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;

import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.element.CMLArray;
import org.xmlcml.cml.element.CMLScalar;
import org.xmlcml.euclid.IntArray;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Array;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealArray;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGPolyline;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.svg2xml.figure.AxisAnalyzerX;
import org.xmlcml.svg2xml.page.BoundingBoxManager;
import org.xmlcml.svg2xml.page.PageChunkAnalyzer;
import org.xmlcml.svg2xml.page.TextAnalyzer;
import org.xmlcml.svg2xml.page.TextAnalyzerUtils;
import org.xmlcml.svg2xml.page.BoundingBoxManager.BoxEdge;
import org.xmlcml.svg2xml.paths.ComplexLine.CombType;
import org.xmlcml.svg2xml.paths.ComplexLine.LineOrientation;
import org.xmlcml.svg2xml.words.TypedNumber;

public class Axis {


	private final static Logger LOG = Logger.getLogger(Axis.class);

	public static final String AXIS_PREF = "axis_";
	public static final String AXIS = AXIS_PREF+ "axis";
	private static final String AXISCLASS = AXIS_PREF+ "axis";
	private static final String BACKBONE = AXIS_PREF+ "backbone";
	private static final String LABEL = AXIS_PREF+ "label";
	private static final String MAJOR_TICKS = AXIS_PREF+ "majorTicks";
	private static final String MINOR_TICKS = AXIS_PREF+ "minorTicks";
	private static final String VALUES = AXIS_PREF+ "values";

	private double eps = 0.001;
	private ComplexLine complexLine;
	private Real2 axisWorldCoordStart = null;
	private Real2 axisWorldCoordEnd = null;
	private String axisLabel = null;
	private String axisUnits = null;
	private CombType combType;
	private List<SVGElement> texts;
	private Double boxThickness;
	private Double boxLengthExtension;
	private AxisAnalyzerX axisAnalyzerX;
	private PageChunkAnalyzer textAnalyzerX;
	private String id;

	private double minTickLengthPixels;
	private double maxTickLengthPixels;
	private List<Joint> majorTickJointList;
	private List<Joint> minorTickJointList;
	private Integer majorTickSpacingPixelsToMinorTick;
	private Double majorTickSpacingInPixels = null;
	private Double minorTickSpacingInPixels = null;
	
	private CMLArray majorTickMarkValues;
	private CMLScalar scalar;
	
	private LineOrientation lineOrientation;
	
	private Real2 lowestMajorTickInPixels;
	private Real2 highestMajorTickInPixels;
	private Double lowestMajorTickCoordInPixels;
	private Double highestMajorTickCoordInPixels;
	private Double lowestTickMarkValue;
	private Double highestTickMarkValue;

	private List<SVGText> numericTexts;
	private List<SVGText> nonNumericTexts;

	private Double arraySpacingInValues;
	private RealRange axisRangeInPixels;
	private Double lowestAxisValue;
	private Double highestAxisValue;

	private Double pixelToValueScale;


	public Axis(AxisAnalyzerX axisAnalyzerX) {
		this.axisAnalyzerX = axisAnalyzerX;
		this.boxLengthExtension = axisAnalyzerX.getBoxLengthExtension();
		this.boxThickness = axisAnalyzerX.getBoxThickness();
	}

	public Double getBoxThickness() {
		return boxThickness;
	}

	public void setBoxThickness(Double boxThickness) {
		this.boxThickness = boxThickness;
	}

	public CombType getCombType() {
		return combType;
	}

	public void setCombType(CombType combType) {
		this.combType = combType;
	}

	public Double getMajorTickPixelSpacing() {
		return majorTickSpacingInPixels;
	}

	public void setMajorTickPixelSpacing(Double majorTickPixelSpacing) {
		this.majorTickSpacingInPixels = majorTickPixelSpacing;
	}

	public Double getMinorTickPixelSpacing() {
		return minorTickSpacingInPixels;
	}

	public void setMinorTickPixelSpacing(Double minorTickPixelSpacing) {
		this.minorTickSpacingInPixels = minorTickPixelSpacing;
	}

	public Real2 getAxisWorldCoordStart() {
		if (axisWorldCoordStart == null) {
			axisWorldCoordStart = complexLine.getBackbone().getXY(0);
		}
		return axisWorldCoordStart;
	}

	public Real2 getAxisWorldCoordEnd() {
		if (axisWorldCoordEnd == null) {
			axisWorldCoordEnd = complexLine.getBackbone().getXY(1);
		}
		return axisWorldCoordEnd;
	}

	public String getAxisLabel() {
		return axisLabel;
	}

	public void setAxisLabel(String axisLabel) {
		this.axisLabel = axisLabel;
	}

	public String getAxisUnits() {
		return axisUnits;
	}

	public void setAxisUnits(String axisUnits) {
		this.axisUnits = axisUnits;
	}

	public ComplexLine getComplexLine() {
		return complexLine;
	}
	
	public LineOrientation getOrientation() {
		if (lineOrientation == null) {
			if (complexLine != null) {
				lineOrientation = complexLine.getBackboneOrientation();
			}
		}
		return lineOrientation;
	}

	public List<Joint> getMinorTickJointList() {
		return minorTickJointList;
	}

	public void setComplexLine(ComplexLine complexLine) {
		complexLine.getBackbone().normalizeDirection(eps);
		this.complexLine = complexLine;
	}
	
	public List<Joint> trimJointList(List<Joint> jointList, double minTickLength, double maxTickLength) {
		minorTickJointList = new ArrayList<Joint>();
		for (Joint joint : jointList) {
			double jointLength = joint.getLength();
			if (jointLength <= maxTickLength && jointLength >= minTickLength) {
				minorTickJointList.add(joint);
			}
		}
		return minorTickJointList;
	}


	public String debug(String msg) {
		String s = msg+"\n";
		s += " TrimmedJoints: "+minorTickJointList.size();
		s += " Spacing: "+minorTickSpacingInPixels;
		s += " Orient: "+complexLine.getBackboneOrientation()+"\n";
		s += " start: "+complexLine.getBackbone().getXY(0)+" end "+complexLine.getBackbone().getXY(1)+"\n";
		return s;
	}
	
	/** only works for correctly oriented text
	 * may have to rotate for other text
	 * 
	 * @param container
	 * @param boxThickness
	 * @param boxLengthExtension
	 */
	public void processScaleValuesAndTitles(SVGElement container) {
		texts = SVGUtil.getQuerySVGElements(container, ".//svg:text");
		countTSpanChildren("ALL ", texts);
		Real2Range textBox = getTextBox(complexLine.getBackbone());
		BoxEdge edge = (LineOrientation.HORIZONTAL.equals(getOrientation())) ? BoxEdge.XMIN : BoxEdge.YMIN;
		List<SVGElement> sortedTexts = BoundingBoxManager.getElementsSortedByEdge(texts, edge);
		countTSpanChildren("SORTED ", texts);
		List<SVGText> boundedTexts = getTextsInBox(textBox, sortedTexts); 
		countTSpanChildren("BOUND ", texts);
		ensureTickmarks();
		if (LineOrientation.HORIZONTAL.equals(lineOrientation)) {
			List<SVGText> horizontalTexts = getTexts(boundedTexts, LineOrientation.HORIZONTAL);
			countTSpanChildren("HOR ", horizontalTexts);
			for (SVGText horizontalText : horizontalTexts) {
				horizontalText.debug("HOR TEXT");
			}
			analyzeHorizontalAxis(horizontalTexts);
		} else if (LineOrientation.VERTICAL.equals(lineOrientation)) {
			List<SVGText> verticalTexts = getTexts(boundedTexts, LineOrientation.HORIZONTAL);
			analyzeVerticalAxis(verticalTexts);
			for (SVGText rotatedText : verticalTexts) {
				LOG.trace("ROT "+rotatedText.getValue()+" .. "+
			       rotatedText.getTransform().getAngleOfRotation().getDegrees());
			}
		}
	}

	private void countTSpanChildren(String msg, List<? extends SVGElement> texts) {
		int tspanCount = 0;
		for (SVGElement text : texts) {
			tspanCount += ((SVGText)text).getChildTSpans().size();
		}
		LOG.trace(msg+" TSPANS****************"+tspanCount);
	}
	
	public void createAxisGroup() {
/*
	private double eps = 0.001;
	private Real2 axisWorldCoordStart = null;
	private Real2 axisWorldCoordEnd = null;
	private String axisLabel = null;
	private String axisUnits = null;
	private CombType combType;
	private List<SVGElement> texts;
	private Double boxThickness;
	private Double boxLengthExtension;
	private AxisAnalyzer axisAnalyzer;
	private TextAnalyzer textAnalyzer;
	private String id;

	private double minTickLengthPixels;
	private double maxTickLengthPixels;
	
	private ComplexLine complexLine;
	private List<Joint> majorTickJointList;
	private List<Joint> minorTickJointList;
	
	private Integer majorTickSpacingPixelsToMinorTick;
	private Double majorTickSpacingInPixels = null;
	private Double minorTickSpacingInPixels = null;
	
	private CMLArray majorTickMarkValues;
	private CMLScalar scalar;
	
	private LineOrientation lineOrientation;
	
	private Real2 lowestMajorTickInPixels;
	private Real2 highestMajorTickInPixels;
	private Double lowestMajorTickCoordInPixels;
	private Double highestMajorTickCoordInPixels;
	private Double lowestTickMarkValue;
	private Double highestTickMarkValue;

	private List<SVGText> numericTexts;
	private List<SVGText> nonNumericTexts;

	private Double arraySpacingInValues;
	private RealRange axisRangeInPixels;
	private Double lowestAxisValue;
	private Double highestAxisValue;

	private Double pixelToValueScale;

 */
		SVGLine backbone = complexLine.getBackbone();
		SVGElement parent = (SVGElement) backbone.getParent();
		if (parent == null) {
			throw new RuntimeException("backbone has no parent");
		}
		SVGG svgg = new SVGG();
		svgg.setClassName(AXISCLASS);
		parent.appendChild(svgg);
		
//		groupBackbone(backbone, svgg);
		groupField(svgg, BACKBONE, backbone);
		// do minor first as major ticks are also included in minor
		groupTickJoints(svgg, MINOR_TICKS, minorTickJointList);
		groupTickJoints(svgg, MAJOR_TICKS, majorTickJointList);
		if (nonNumericTexts.size() > 0) {
			groupField(svgg, LABEL, nonNumericTexts.get(0));
		}
		groupFields(svgg, VALUES, numericTexts);
		List<SVGElement> axisMarks = SVGUtil.getQuerySVGElements(svgg, "./svg:*[contains(@class, '"+AXIS_PREF+"')]");
		for (SVGElement axisMark : axisMarks) {
			axisMark.setStroke("yellow");
		}
		List<SVGElement> rects = SVGUtil.getQuerySVGElements(svgg, "//svg:rect");
		for (SVGElement rect : rects) {
			rect.detach();
		}
	}

	private void groupField(SVGG svgg, String fieldName, SVGElement field) {
		if (field != null) {
			field.setClassName(fieldName);
			field.detach();
			svgg.appendChild(field);
		}
	}

	private void groupFields(SVGG svgg, String fieldName, List<? extends SVGElement> fields) {
		if (fields != null) {
			for (SVGElement field : fields) {
				field.setClassName(fieldName);
				field.detach();
				svgg.appendChild(field);
			}
		}
	}

//	private void groupBackbone(SVGLine backbone, SVGG svgg) {
//		backbone.setClassName(BACKBONE);
//		backbone.detach();
//		svgg.appendChild(backbone);
//	}

	private void groupTickJoints(SVGG svgg, String tickType, List<Joint> tickList) {
		SVGG jointG = new SVGG();
		jointG.setClassName(tickType);
		svgg.appendChild(jointG);
		for (Joint joint : tickList) {
			SVGLine line = joint.getLine();
			line.detach();
			jointG.appendChild(line);
		}
	}

	private void transformArrayFromPixelsToScale(List<SVGPolyline> polylines) {
		getOrientation();
		SVGElement parentSVG = (SVGElement)complexLine.getBackbone().getParent();
		if (parentSVG == null) {
			LOG.trace("NULL SVG PARENT");
		} else {
			ensureTickmarks();
			SVGElement parent = (SVGElement) parentSVG.getParent();
			for (SVGPolyline polyline : polylines) {
				Real2Array polylineCoords = polyline.getReal2Array();
				RealArray polylineAxisPixelCoords = (LineOrientation.HORIZONTAL.equals(lineOrientation)) ?
						polylineCoords.getXArray() : polylineCoords.getYArray();
				RealArray polylineValueCoords = polylineAxisPixelCoords.createScaledArrayToRange(
					lowestMajorTickCoordInPixels, highestMajorTickCoordInPixels, lowestTickMarkValue, highestTickMarkValue);
				Double range = polylineValueCoords.getRange().getRange();
				int places = (int) Math.max(0, 6 - (Math.log10(range)-0.5));
				polylineValueCoords.format(places);
			}
		}
	}

	private void ensureTickmarks() {
		if (lowestMajorTickCoordInPixels == null) {
			getOrientation();
			getLowestMajorTickCoordinateInPixels();
			getHighestMajorTickCoordinateInPixels();
			getLowestMajorTickPointInPixels();
			getLowestTickMarkValue();
			getHighestMajorTickPointInPixels();
			getHighestTickMarkValue();
			getHighestAndLowestAxisValues();
		}
	}

	private Real2 getLowestMajorTickPointInPixels() {
		lowestMajorTickInPixels = majorTickJointList.get(0).getPoint();
		return lowestMajorTickInPixels;
	}

	private Real2 getHighestMajorTickPointInPixels() {
		highestMajorTickInPixels = majorTickJointList.get(majorTickJointList.size()-1).getPoint();
		return highestMajorTickInPixels;
	}
	
	private Double getLowestMajorTickCoordinateInPixels() {
		Real2 point = getLowestMajorTickPointInPixels();
		LOG.trace("LowestTick "+point+ "orientation "+lineOrientation);
		lowestMajorTickCoordInPixels = (LineOrientation.HORIZONTAL.equals(lineOrientation)) ? point.getX() : point.getY();
		return lowestMajorTickCoordInPixels;
	}
	
	private Double getHighestMajorTickCoordinateInPixels() {
		Real2 point = getHighestMajorTickPointInPixels();
		LOG.trace("HighestTick "+point+ "orientation "+lineOrientation);
		highestMajorTickCoordInPixels = (LineOrientation.HORIZONTAL.equals(lineOrientation)) ? point.getX() : point.getY();
		return highestMajorTickCoordInPixels;
	}
	
	private void getArraySpacingInValues() {
		if (arraySpacingInValues == null) {
			int size = majorTickMarkValues.getSize();
			if (CMLConstants.XSD_INTEGER.equals(majorTickMarkValues.getDataType())) {
				arraySpacingInValues = ((double) majorTickMarkValues.getInts()[size-1] - (double) majorTickMarkValues.getInts()[0])  / (double )(size - 1);
			} else if (CMLConstants.XSD_DOUBLE.equals(majorTickMarkValues.getDataType())) {
				arraySpacingInValues = ((double) majorTickMarkValues.getDoubles()[size-1] - (double) majorTickMarkValues.getDoubles()[0])  / (double )(size - 1);
			} 
			LOG.trace("SCALE/TICK "+arraySpacingInValues);
		}
	}
	
	private void getHighestAndLowestAxisValues() {
		if (lowestTickMarkValue != null) {
			getArraySpacingInValues();
			getAxisRangeInPixels();
			ensureTickmarks();
			getPixelToValueScale();
			if (lowestTickMarkValue != null && highestTickMarkValue != null) {
				double axisMin = axisRangeInPixels.getMin();
				lowestAxisValue = (axisMin - lowestMajorTickCoordInPixels) / (pixelToValueScale) + lowestTickMarkValue;
				LOG.trace(" axisMin: "+axisMin+" lowestMajorTick "+lowestMajorTickCoordInPixels+" arraySpacingInPixels "+
				      arraySpacingInValues+" lowestTickMarkValue "+lowestTickMarkValue);
				LOG.trace("lowestAxisValue: "+lowestAxisValue);
				double axisMax = axisRangeInPixels.getMax();
				highestAxisValue = (axisMax - highestMajorTickCoordInPixels) / (pixelToValueScale) + highestTickMarkValue;
				LOG.trace(" axisMax: "+axisMax+" highestMajorTick "+highestMajorTickCoordInPixels+" arraySpacingInPixels "+
				      arraySpacingInValues+" highestTickMarkValue "+highestTickMarkValue);
				LOG.trace("highestAxisValue: "+highestAxisValue);
			}
		}
	}

	private double getPixelToValueScale() {
		if (pixelToValueScale == null) {
			ensureTickmarks();
			if (lowestTickMarkValue != null && lowestMajorTickCoordInPixels != null) {
				pixelToValueScale = (highestMajorTickCoordInPixels - lowestMajorTickCoordInPixels) / (highestTickMarkValue - lowestTickMarkValue);
			}
		}
		return pixelToValueScale;
	}

	private void analyzeHorizontalAxis(List<SVGText> ySortedTexts) {
		createNumericAndNonNumericTexts(ySortedTexts);
		processHorizontalScaleValuesAndScaleTitle(ySortedTexts);
		mapTickPositionsToValues();
	}
	
	private void analyzeVerticalAxis(List<SVGText> ySortedTexts) {
		createNumericAndNonNumericTexts(ySortedTexts);
		processVerticalScaleValuesAndScaleTitle(ySortedTexts);
		mapTickPositionsToValues();
	}

	private void mapTickPositionsToValues() {
		if (majorTickMarkValues != null && majorTickJointList != null) {
			if (majorTickMarkValues.getSize() == majorTickJointList.size()) {
				getArraySpacingInValues();
				// this should be elsewhere
				List<SVGPolyline> polylines = SVGPolyline.extractPolylines(SVGUtil.getQuerySVGElements(null, "./svg:g/svg:polyline"));
				transformArrayFromPixelsToScale(polylines);
			} else {
				LOG.trace("ARRAY: "+majorTickMarkValues.getSize()+ " != "+majorTickJointList.size());
			}
		}
	}
	
	/** texts should have already have been grouped into words
	 * 
	 * @param texts
	 */
	private void processHorizontalScaleValuesAndScaleTitle(List<SVGText> texts) {
		createNumericAndNonNumericTexts(texts);
		Integer y = null;
		Double numericYCoord = TextAnalyzerUtils.getCommonYCoordinate(numericTexts, axisAnalyzerX.eps);
		if (numericYCoord != null) {
			majorTickMarkValues = createNumericValues(numericTexts);
		}
		Double nonNumericYCoord = TextAnalyzerUtils.getCommonYCoordinate(nonNumericTexts, axisAnalyzerX.eps);
		if (nonNumericYCoord != null && nonNumericTexts.size() > 0) {
			axisLabel = nonNumericTexts.get(0).getValue();
		}
	}

	/** texts should have already have been grouped into words
	 * assuming horizontal scale values at present
	 * @param texts
	 */
	private void processVerticalScaleValuesAndScaleTitle(List<SVGText> texts) {
		
		createNumericAndNonNumericTexts(texts);
		Integer y = null;
		Double numericRightXCoord = TextAnalyzerUtils.getCommonRightXCoordinate(numericTexts, TextAnalyzer.TEXT_EPS);
		Double numericLeftXCoord = TextAnalyzerUtils.getCommonLeftXCoordinate(numericTexts, TextAnalyzer.TEXT_EPS);
		if (numericRightXCoord != null || numericLeftXCoord != null) {
			majorTickMarkValues = createNumericValues(numericTexts);
		}
		Double nonNumericYCoord = TextAnalyzerUtils.getCommonYCoordinate(nonNumericTexts, axisAnalyzerX.eps);
		if (nonNumericYCoord != null && nonNumericTexts.size() == 1) {
			axisLabel = nonNumericTexts.get(0).getValue();
		}
	}

	private CMLArray createNumericValues(List<SVGText> numericTexts) {
		CMLArray array = null;
		if (numericTexts.size() == 1 ) {
			SVGText text = numericTexts.get(0);
			String dataType = text.getAttributeValue(TypedNumber.DATA_TYPE);
			String numbers = text.getAttributeValue(TypedNumber.NUMBERS);
			LOG.trace("NUMBERS: "+numbers);
			if (CMLConstants.XSD_INTEGER.equals(dataType)) {
				IntArray intArray = new IntArray(numbers);
				array = new CMLArray(intArray.getArray());
			} else if (CMLConstants.XSD_DOUBLE.equals(dataType)) {
				RealArray realArray = new RealArray(numbers);
				array = new CMLArray(realArray.getArray());
			}
		} else {
			String dataType = getCommonDataType(numericTexts);
			if (dataType != null) {
				List<String> values = new ArrayList<String>();
				for (SVGText numericText : numericTexts) {
					values.add(TypedNumber.getNumericValue(numericText));
				}
				if (CMLConstants.XSD_INTEGER.equals(dataType)) {
					IntArray intArray = new IntArray(values.toArray(new String[0]));
					array = new CMLArray(intArray.getArray());
				} else if (CMLConstants.XSD_DOUBLE.equals(dataType)) {
					RealArray realArray = new RealArray(values.toArray(new String[0]));
					array = new CMLArray(realArray.getArray());
				}
			}
		}
		return array;
	}

	private String getCommonDataType(List<SVGText> numericTexts) {
		String dataType = null;
		for (SVGText numericText : numericTexts) {
			String dt = numericText.getAttributeValue(TypedNumber.DATA_TYPE);
			if (dataType == null) {
				dataType = dt;
			} else if (!dataType.equals(dt)) {
				dataType = null;
				break;
			}
		}
		return dataType;
	}

	private void createNumericAndNonNumericTexts(List<SVGText> texts) {
		if (numericTexts == null) {
			numericTexts = new ArrayList<SVGText>();
			nonNumericTexts = new ArrayList<SVGText>();
			for (SVGText text : texts) {
				if (text.query("@"+TypedNumber.NUMBER).size() > 0 ||
					text.query("@"+TypedNumber.NUMBERS).size() > 0  ) {
					numericTexts.add(text);
				} else {
					if (text.getValue().trim().length() != 0) {
						nonNumericTexts.add(text);
					}
				}
			}
			LOG.trace("NUMERIC "+numericTexts.size()+" NON-NUM "+nonNumericTexts.size());
		}
	}

	public PageChunkAnalyzer getTextAnalyzerX() {
		return textAnalyzerX;
	}

	private List<SVGText> getTexts(List<SVGText> textList, LineOrientation orientation) {
		LOG.trace("ORIENT "+orientation+" texts "+textList.size());
		List<SVGText> subTextList = new ArrayList<SVGText>();
		for (SVGText text : textList) {
			Transform2 transform = text.getTransform();
			boolean isRotated = false;
			Double degrees = null;
			if (transform != null) {
				degrees = transform.getAngleOfRotation().getDegrees();
			} else {
				degrees = 0.0;
			}
			isRotated = Math.abs(degrees) > eps;
			LOG.trace("IS ROT "+isRotated);
			if (isRotated == LineOrientation.VERTICAL.equals(orientation)) {
				LOG.trace("ADDED TEXT ");
				subTextList.add(text);
			} else {
				text.debug("NOT ADDED");
			}
		}
		return subTextList;
	}

	private List<SVGText> getTextsInBox(Real2Range textBox, List<SVGElement> sortedTexts) {
		// crude at present
		LOG.trace("TEXTBOX "+textBox);
		List<SVGText> textList = new ArrayList<SVGText>();
		for (int i = 0; i < sortedTexts.size(); i++) {
			SVGText sortedText = (SVGText) sortedTexts.get(i);
			Real2Range bb = sortedText.getBoundingBox();
			LOG.trace("   BOX? "+bb);
			if (textBox.includes(bb)) {
				textList.add(sortedText);
			} else {
				sortedText.getBoundingBox();
//				sortedText.debug(bb+ " NOT INCLUDED in "+textBox);
			}
		}
		return textList;
	}

	private Real2Range getTextBox(SVGLine backbone) {
		Real2Range textBox = null;
		if (LineOrientation.HORIZONTAL.equals(getOrientation())) {
			double x0 = backbone.getXY(0).getX();
			double x1 = backbone.getXY(1).getX();
			double y = backbone.getXY(0).getY();
			textBox = new Real2Range(new Real2(x0 - boxLengthExtension, y), 
					      new Real2(x1 + boxLengthExtension, y + boxThickness));
		} else if (LineOrientation.VERTICAL.equals(getOrientation())) { // only LHS at present
			double y0 = backbone.getXY(0).getY();
			double y1 = backbone.getXY(1).getY();
			double x = backbone.getXY(0).getX();
			textBox = new Real2Range(
					new Real2(x - boxThickness, y0 - boxLengthExtension), 
					new Real2(x, y1 + boxLengthExtension));
		}
		return textBox;
	}
	

	public String getId() {
		return this.id;
	}

	public void setId(String string) {
		this.id = string;
	}

	public List<Joint> getMajorTicks(double tickEpsRatio) {
		RealArray realArray = new RealArray();
		for (Joint joint : minorTickJointList) {
			realArray.addElement(joint.getLength());
		}
		minTickLengthPixels = realArray.getMin();
		maxTickLengthPixels = realArray.getMax();
		double meanTickLength = (minTickLengthPixels + maxTickLengthPixels) / 2.0; 
		// if not significant difference assume all ticks same size
		if (maxTickLengthPixels / minTickLengthPixels < tickEpsRatio) {
			return minorTickJointList;
		}
		majorTickJointList = new ArrayList<Joint>();
		for (Joint joint : minorTickJointList) {
			if (joint.getLength() > meanTickLength) {
				majorTickJointList.add(joint);
			}
		}
		return majorTickJointList;
	}

	public void analyzeMajorMinorTicks(ComplexLine complexLine) {
		addAxisAttribute(complexLine.getBackbone(), getId());
		for (Joint joint : getMinorTickJointList()) {
			addAxisAttribute(joint.getLine(), getId());
		}
		minorTickSpacingInPixels = ComplexLine.calculateInterJointSpacing(minorTickJointList, axisAnalyzerX.jointEps);
		majorTickJointList = getMajorTicks(AxisAnalyzerX._MAJOR_MINOR_TICK_RATIO);
		majorTickSpacingInPixels = ComplexLine.calculateInterJointSpacing(majorTickJointList, axisAnalyzerX.jointEps);
		majorTickSpacingPixelsToMinorTick = null;
		if (majorTickSpacingInPixels != null && minorTickSpacingInPixels != null) {
			double ratio = majorTickSpacingInPixels/minorTickSpacingInPixels;
			majorTickSpacingPixelsToMinorTick = (int) Math.rint(ratio);
			if (Math.abs(ratio - majorTickSpacingPixelsToMinorTick) > 0.1) {
				throw new RuntimeException("Cannot get integer tick mark ratio: "+ratio + "/" +majorTickSpacingPixelsToMinorTick);
			}
			LOG.trace("MAJOR/MINOR "+(majorTickSpacingPixelsToMinorTick)+" majorTicks: "+majorTickJointList.size()+" ");
			LOG.trace(debug("NEW COMB"));
		}
	}

	public RealArray createScaledArrayToRange(RealArray polylinePixelCoords) {
		ensureTickmarks();
		RealArray realArray = null;
		if (lowestTickMarkValue != null && lowestMajorTickCoordInPixels != null) {
			realArray =  polylinePixelCoords.createScaledArrayToRange(
					lowestMajorTickCoordInPixels, highestMajorTickCoordInPixels, lowestTickMarkValue, highestTickMarkValue);
		}
		return realArray;
	}
	
	void addAxisAttribute(SVGElement element, String id) {
		element.addAttribute(new Attribute(AXIS, id));
	}

	public String toString() {
		String s = "\n";
		ensureTickmarks();
		if (majorTickMarkValues != null && majorTickSpacingInPixels != null && majorTickJointList != null) {
			s += tickDetail("major", majorTickSpacingInPixels, majorTickJointList)+"\n";
			int nValues = majorTickMarkValues.getSize();
			s += " "+nValues+" major values "+getLowestTickMarkValue()+" ... "+(nValues-1)+" gaps ... "+
			" "+getHighestTickMarkValue()+"\n";
		}
		if (minorTickSpacingInPixels != null && minorTickSpacingInPixels != null && minorTickJointList != null) {
			s += tickDetail("minor", minorTickSpacingInPixels, minorTickJointList)+"\n";
		}
		getHighestAndLowestAxisValues();
		if (lowestAxisValue != null) {
			s += "axis " + lowestAxisValue+" ... " + highestAxisValue + "\n";
		} else {
			s += "NO AXIS VALUES"+ "\n";
		}
		s += "label: "+axisLabel+"\n";
		
		return s;
	}

	private Double getLowestTickMarkValue() {
		if (lowestTickMarkValue == null && majorTickMarkValues != null) {
			lowestTickMarkValue = majorTickMarkValues.getElementAt(0).getNumberAsDouble();
		}
		return lowestTickMarkValue;
	}

	private Double getHighestTickMarkValue() {
		if (highestTickMarkValue == null && majorTickMarkValues != null) {
			highestTickMarkValue = majorTickMarkValues.getElementAt(majorTickMarkValues.getSize()-1).getNumberAsDouble();
		}
		return highestTickMarkValue;
	}

	private String tickDetail(String title, double spacing, List<Joint> jointList) {
		int nTicks = jointList.size();
		return " "+nTicks+" "+title+" ticks (pixels): "+jointList.get(0).getPoint().format(3)+" ... "+(nTicks-1)+" gaps "+
				Real.normalize(spacing, 3)+"(pixels) ... "+jointList.get(nTicks-1).getPoint().format(3);
	}

	public RealRange getAxisRangeInPixels() {
		Real2Range r2r = complexLine.getBackbone().getReal2Range();
		axisRangeInPixels = (LineOrientation.HORIZONTAL.equals(lineOrientation)) ? r2r.getXRange() : r2r.getYRange();
		return axisRangeInPixels;
	}
}
