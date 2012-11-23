package org.xmlcml.svgplus.paths;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.svgplus.paths.ComplexLine.Direction;
import org.xmlcml.svgplus.paths.ComplexLine.LineOrientation;
import org.xmlcml.svgplus.tools.BoundingBoxManager;
import org.xmlcml.svgplus.tools.ElementMerger;
import org.xmlcml.svgplus.tools.ElementNeighbourhood;
import org.xmlcml.svgplus.tools.ElementNeighbourhoodManager;

/** a convenience class to help join lines
 * 
 * @author pm286
 *
 */
public class LineMerger extends ElementMerger {

	public enum MergeMethod {
		OVERLAP,
		TOUCHING_LINES, 
	}

	private final static Logger LOG = Logger.getLogger(LineMerger.class);
	private Direction direction; 
	private LineOrientation orientation;
	private MergeMethod method = MergeMethod.OVERLAP;
	
	public LineMerger(SVGLine line0, double eps) {
		super(line0, eps);
		direction = ComplexLine.getLineDirection(line0, eps);
		orientation = ComplexLine.getLineOrientation(line0, eps);
	}
	
	public static LineMerger createLineMerger(SVGLine line0, double eps) {
		LineMerger lineJoin = null;
		if (!line0.isZero(eps)) {
			lineJoin = new LineMerger(line0, eps);
		}
		return lineJoin;
	}
	
	public void setMethod(MergeMethod method) {
		this.method = method;
	}
	public SVGElement createNewElement(SVGElement line1x) {
		if (line1x == null) {
			throw new RuntimeException("null line1");
		}
		SVGElement result = null;
		if (line1x instanceof SVGLine) {
			result = mergeLineLine(line1x, method);
		}
		return result;
	}

	private SVGLine mergeLineLine(SVGElement line1x, MergeMethod method) {
		SVGLine line0 = (SVGLine) elem0;
		SVGLine line1 = (SVGLine) line1x;
		
		SVGLine newLine = null;
		LineOrientation orientation1 = ComplexLine.getLineOrientation(line1, eps);
		Direction direction1 = ComplexLine.getLineDirection(line1, eps);
		if (orientation == null || orientation1 == null) {
			// pathological value
		} else if (orientation.equals(orientation1)) {
			if (MergeMethod.TOUCHING_LINES.equals(method)) {
				newLine = createTouchingLine(line0, line1, direction1);
			} else if (MergeMethod.OVERLAP.equals(method)) {
				newLine = createOverlappedLine(line0, line1);
			}
			LOG.trace("lines "+line0.getId()+": "+line0.getXY(0)+"/"+line0.getXY(1)+"; "+line1.getXY(0)+"/"+line1.getXY(1)+" "+line1.getId());
			if (newLine != null) {
				newLine.setId(line0.getId()+"x");
			}
		} else {
			LOG.debug("Cannot make lines from: "+line0.getId()+" / "+line1.getId());
		}
		return newLine;
	}

	private SVGLine createTouchingLine(SVGLine line0, SVGLine line1, Direction direction1) {
		SVGLine newLine = null;
		Real2 point00 = line0.getXY(0);
		Real2 point01 = line0.getXY(1);
		Real2 point10 = line1.getXY(0);
		Real2 point11 = line1.getXY(1);
		if (direction.equals(direction1)) {
			if (point01.isEqualTo(point10, eps)) {
				newLine = new SVGLine(line0);
				newLine.setXY(point11, 1);
			} else if (point00.isEqualTo(point11, eps)) {
				newLine = new SVGLine(line0);
				newLine.setXY(point10, 0);
			}
		} else {
			// antiparallel
			if (point01.isEqualTo(point11, eps)) {
				newLine = new SVGLine(line0);
				newLine.setXY(point10, 1);
			} else if (point00.isEqualTo(point10, eps)) {
				newLine = new SVGLine(line0);
				newLine.setXY(point11, 0);
			}
		}
		return newLine;
	}
	
	private SVGLine createOverlappedLine(SVGLine line0, SVGLine line1) {
		SVGLine newLine = null;
		Real2Range bbox0 = BoundingBoxManager.createExtendedBox(line0, eps);
		Real2Range bbox1 = BoundingBoxManager.createExtendedBox(line1, eps);
		Real2Range inter = bbox1.intersectionWith(bbox0);
		if (inter != null) {
			Real2Range bbox00 = line0.getBoundingBox();
			Real2Range bbox10 = line1.getBoundingBox();
			Real2Range bbox01 = bbox00.plus(bbox10);
			newLine = new SVGLine(bbox01.getCorners()[0], bbox01.getCorners()[1]);
		}
		return newLine;
	}
	
	public static List<SVGLine> mergeLines(List<SVGLine> linesxx, double eps) {
		ElementNeighbourhoodManager enm = new ElementNeighbourhoodManager(linesxx);
		List<SVGElement> elems;
		while (true) {
			enm.createTouchingNeighbours(eps);
			elems = enm.getElementList();
			SVGElement newElem = null;
			SVGElement oldElem = null;
			SVGElement oldElem1 = null;
			for (int i = 0; i < elems.size(); i++) {
				oldElem1 = elems.get(i);
				LineMerger lineMerger = LineMerger.createLineMerger((SVGLine)oldElem1, eps);
				ElementNeighbourhood neighbourhood = enm.getNeighbourhood(oldElem1);
				if (neighbourhood == null) {
					continue;
				}
				List<SVGElement> neighbours = neighbourhood.getNeighbourList();
				for (SVGElement neighbour : neighbours) {
					if (neighbour instanceof SVGLine) {
						oldElem = (SVGLine) neighbour;
						newElem = lineMerger.createNewElement(oldElem);
						if (newElem != null) {
							LOG.trace(((SVGLine)oldElem1).getEuclidLine()+" + "+((SVGLine)oldElem).getEuclidLine()+" => "+((SVGLine)newElem).getEuclidLine());
							break;
						}
					}
				}
				if (newElem != null) {
					enm.replaceElementsByElement(newElem, Arrays.asList(new SVGElement[] {oldElem, oldElem1}));
					break;
				}
			} // end of loop through elements
			if (newElem == null) {
				break;
			}
		} // end of infinite loop
		List<SVGLine> lines = SVGLine.extractLines(enm.getElementList());
		return lines;
	}
}
