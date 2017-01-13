package org.xmlcml.svg2xml.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.IntArray;
import org.xmlcml.euclid.IntRange;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.xml.XMLUtil;

import nu.xom.Element;

public class PhraseListList extends SVGG implements Iterable<PhraseList> {
	public static final Logger LOG = Logger.getLogger(PhraseListList.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	public final static String TAG = "phraseListList";
	private static final int EPS = 5;

	private List<PhraseList> childPhraseListList;

	public PhraseListList() {
		super();
		this.setClassName(TAG);
	}
	
	public Iterator<PhraseList> iterator() {
		getOrCreateChildPhraseList();
		return childPhraseListList.iterator();
	}

	public List<PhraseList> getOrCreateChildPhraseList() {
		if (childPhraseListList == null) {
			List<Element> phraseChildren = XMLUtil.getQueryElements(this, "*[local-name()='"+SVGG.TAG+"' and @class='"+PhraseList.TAG+"']");
			childPhraseListList = new ArrayList<PhraseList>();
			for (Element child : phraseChildren) {
				PhraseList phraseList = (PhraseList)child;
				childPhraseListList.add(phraseList);
			}
		}
		return childPhraseListList;
	}

	public String getStringValue() {
		getOrCreateChildPhraseList();
		StringBuilder sb = new StringBuilder();
		for (PhraseList phraseList : childPhraseListList) {
			sb.append(""+phraseList.getStringValue()+"//");
		}
		this.setStringValueAttribute(sb.toString());
		return sb.toString();
	}

	public void add(PhraseList phraseList) {
		this.appendChild(phraseList);
		childPhraseListList = null;
		getOrCreateChildPhraseList();
	}

	public PhraseList get(int i) {
		getOrCreateChildPhraseList();
		return childPhraseListList.get(i);
	}
	
	public List<IntArray> getLeftMarginsList() {
		getOrCreateChildPhraseList();
		List<IntArray> leftMarginsList = new ArrayList<IntArray>();
		for (PhraseList phraseList : childPhraseListList) {
			IntArray leftMargins = phraseList.getLeftMargins();
			leftMarginsList.add(leftMargins);
		}
		return leftMarginsList;
	}
	
	/** assumes the largest index in phraseList is main body of table.
	 * 
	 * @return
	 */
	public int getMaxColumns() {
		getOrCreateChildPhraseList();
		int maxColumns = 0;
		for (PhraseList phraseList : childPhraseListList) {
			maxColumns = Math.max(maxColumns, phraseList.size());
		}
		return maxColumns;
	}

	public List<IntRange> getBestColumnRanges() {
		getOrCreateChildPhraseList();
		int maxColumns = getMaxColumns();
		List<IntRange> columnRanges = new ArrayList<IntRange>();
		for (int i = 0; i < maxColumns; i++) {
			columnRanges.add(i, null);
		}
		for (PhraseList phraseList : childPhraseListList) {
			if (phraseList.size() == maxColumns) {
				for (int i = 0; i < phraseList.size(); i++) {
					Phrase phrase = phraseList.get(i);
					IntRange range = phrase.getIntRange();
					IntRange oldRange = columnRanges.get(i);
					range = (oldRange == null) ? range : range.plus(oldRange);
					columnRanges.set(i, range);
				}
			}
		}
		return columnRanges;
	}
	
	public List<IntRange> getBestWhitespaceList() {
		getOrCreateChildPhraseList();
		int maxColumns = getMaxColumns();
		List<IntRange> bestColumnRanges = getBestColumnRanges();
		List<IntRange> bestWhitespaces = new ArrayList<IntRange>();
		if (maxColumns > 0) {
			bestWhitespaces.add(new IntRange(bestColumnRanges.get(0).getMin() - EPS, bestColumnRanges.get(0).getMax() - EPS));
			for (int i = 1; i < maxColumns; i++) {
				IntRange whitespace = new IntRange(bestColumnRanges.get(i - 1).getMax(), bestColumnRanges.get(i).getMax());
				bestWhitespaces.add(whitespace);
			}
		}
		return bestWhitespaces;
	}
	
	/** find rightmostWhitespace range which includes start of phrase.
	 * 
	 */
	public int getRightmostEnclosingWhitespace(List<IntRange> bestWhitespaces, Phrase phrase) {
		for (int i = bestWhitespaces.size() - 1; i >= 0; i--) {
			IntRange range = bestWhitespaces.get(i);
			int phraseX = (int)(double) phrase.getStartX();
			if (range.contains(phraseX)) {
				return i;
			}
		}
		return -1;
	}

	public int size() {
		getOrCreateChildPhraseList();
		return childPhraseListList.size();
	}

	public Real2Range getBoundingBox() {
		getOrCreateChildPhraseList();
		Real2Range bbox = null;
		if (childPhraseListList.size() > 0) {
			bbox = childPhraseListList.get(0).getBoundingBox();
			for (int i = 1; i < childPhraseListList.size(); i++) {
				bbox = bbox.plus(childPhraseListList.get(i).getBoundingBox());
			}
		}
		return bbox;
	}

	public void rotateAll(Real2 centreOfRotation, Angle angle) {
		getOrCreateChildPhraseList();
		for (PhraseList phraseList : childPhraseListList) {
			phraseList.rotateAll(centreOfRotation, angle);
			LOG.trace("PL: "+phraseList.toXML());
		}
		updatePhraseListList();
	}
	
	public void updatePhraseListList() {
		for (int i = 0; i < childPhraseListList.size(); i++) {
			this.replaceChild(this.getChildElements().get(i), childPhraseListList.get(i));
		}
	}

	public Real2 getXY() {
		return this.getBoundingBox().getCorners()[0];
	}



}
