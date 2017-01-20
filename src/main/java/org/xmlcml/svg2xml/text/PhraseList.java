package org.xmlcml.svg2xml.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

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

public class PhraseList extends LineChunk implements Iterable<Phrase> {
	
	private static final Logger LOG = Logger.getLogger(PhraseList.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	public final static String TAG = "phraseList";
	public static final PhraseList NULL = new PhraseList();
	static {
		NULL.add(new Phrase(Phrase.NULL));
	};
	
	// this is not exposed
	private List<Phrase> childPhraseList; 

	public PhraseList() {
		super();
		this.setClassName(TAG);
	}
	
	public PhraseList(PhraseList phraseList) {
		super(phraseList);
	}

	public Iterator<Phrase> iterator() {
		getOrCreateChildPhraseList();
		return childPhraseList.iterator();
	}
	
	public void add(Phrase phrase) {
		this.appendChild(phrase);
	}

	protected List<? extends LineChunk> getChildChunks() {
		getOrCreateChildPhraseList();
		return childPhraseList;
	}


	public IntArray getLeftMargins() {
		getOrCreateChildPhraseList();
		IntArray leftMargins = new IntArray();
		for (Phrase phrase : childPhraseList) {
			Double firstX = phrase.getFirstX();
			if (firstX != null) {
				leftMargins.addElement((int)(double) firstX);
			}
		}
		return leftMargins;
	}
	
	public Phrase get(int i) {
		getOrCreateChildPhraseList();
		return i < 0 || i >= size() ? null : childPhraseList.get(i);
	}

	public List<Phrase> getOrCreateChildPhraseList() {
		if (childPhraseList == null) {
			List<Element> phraseChildren = XMLUtil.getQueryElements(this, "*[local-name()='"+SVGG.TAG+"' and @class='"+Phrase.TAG+"']");
			childPhraseList = new ArrayList<Phrase>();
			for (Element child : phraseChildren) {
				// FIXME 
				childPhraseList.add(new Phrase((SVGG)child));
//				childPhraseList.add((Phrase)child);
			}
		}
		return childPhraseList;
	}

	public int size() {
		getOrCreateChildPhraseList();
		return childPhraseList.size();
	}

	public Real2Range getBoundingBox() {
		getOrCreateChildPhraseList();
		Real2Range bboxTotal = null;
		for (int i = 0; i < childPhraseList.size(); i++) {
			Phrase phrase = childPhraseList.get(i);
			Real2Range bbox = phrase.getBoundingBox();
			if (i == 0) {
				bboxTotal = bbox;
			} else {
				bboxTotal = bboxTotal.plus(bbox);
			}
		}
		return bboxTotal;
				
	}
	
	public Real2 getXY() {
		Real2Range bbox = this.getBoundingBox();
		return bbox == null ? null : bbox.getCorners()[0];
	}


	
	public Double getFontSize() {
		getOrCreateChildPhraseList();
		Double f = null;
		if (childPhraseList.size() > 0) {
			f = childPhraseList.get(0).getFontSize();
			for (int i = 1; i < childPhraseList.size(); i++) {
				Double ff = childPhraseList.get(i).getFontSize();
				if (ff != null) {
					f = Math.max(f,  ff);
				}
			}
		}
		return f;
	}

	public Element copyElement() {
//		return (Element) this.copy();
		getOrCreateChildPhraseList();
		Element element = (Element) this.copy();
		for (LineChunk phrase : childPhraseList) {
			element.appendChild(phrase.copyElement());
		}
		return element;
	}

	public String getStringValue() {
		getOrCreateChildPhraseList();
		StringBuilder sb = new StringBuilder();
		for (Phrase phrase : childPhraseList) {
			sb.append(phrase.getStringValue());
			sb.append(" ");
		}
		this.setStringValueAttribute(sb.toString());
		return sb.toString();
	}

	public void rotateAll(Real2 centreOfRotation, Angle angle) {
		getOrCreateChildPhraseList();
		for (Phrase phrase : childPhraseList) {
			phrase.rotateAll(centreOfRotation, angle);
			LOG.trace("P: "+phrase.toXML());
		}
		updateChildPhraseList();
	}

	public void updateChildPhraseList() {
		for (int i = 0; i < childPhraseList.size(); i++) {
			this.replaceChild(this.getChildElements().get(i), childPhraseList.get(i));
		}
	}

	@Override
	public String toString() {
		return /*this.getClass().getSimpleName()+": "+*/ /*this.getXY()+": "+*/ this.getStringValue();
	}

	public PhraseList extractIncludedLists(IntRange tableSpan) {
		PhraseList includedPhraseList = new PhraseList();
		for (Phrase phrase : this) {
			if (tableSpan.includes(phrase.getIntRange())) {
				includedPhraseList.add(new Phrase(phrase));
			} else {
				LOG.trace("excluded phrase by tableSpan: "+phrase);
			}
		}
		return includedPhraseList;
	}

	public void mergeByXCoord(PhraseList otherPhraseList) {
		Queue<Phrase> otherQueue = otherPhraseList.getPhraseQueue();
		Queue<Phrase> thisQueue = this.getPhraseQueue();
		Phrase thisPhrase = thisQueue.isEmpty() ? null : thisQueue.remove();
		Phrase otherPhrase = otherQueue.isEmpty() ? null : otherQueue.remove();
		List<Phrase> newPhraseList = new ArrayList<Phrase>();
		while (thisPhrase != null || otherPhrase != null) {
			if (thisPhrase == null && !thisQueue.isEmpty()) {
				thisPhrase = thisQueue.remove();
			}
			if (otherPhrase == null && !otherQueue.isEmpty()) {
				otherPhrase = otherQueue.remove();
			}
			if (thisPhrase == null && otherPhrase != null) {
				newPhraseList.add(otherPhrase);
				otherPhrase = null;
			} else if (otherPhrase == null && thisPhrase != null) {
				newPhraseList.add(thisPhrase);
				thisPhrase = null;
			} else if (thisPhrase.getX() < otherPhrase.getX()) {
				newPhraseList.add(thisPhrase);
				thisPhrase = null;
			} else {
				newPhraseList.add(otherPhrase);
				otherPhrase = null;
			}
		}
		this.childPhraseList = newPhraseList;
	}

	public Queue<Phrase> getPhraseQueue() {
		Queue<Phrase> phraseQueue = new LinkedList<Phrase>();
		for (Phrase phrase : this) {
			phraseQueue.add(phrase);
		}
		return phraseQueue;
	}

	private void addSuperscript(Phrase superPhrase) {
		for (int index = 0; index <= this.size(); index++) {
			LineChunk phrase1 = (index == 0) ? null : this.get(index - 1);
			LineChunk phrase2 = index == this.size() ? null : this.get(index);
			if (canHaveSuperscript(phrase1, superPhrase, phrase2)) {
				this.insertSuperscript(index, superPhrase);
				break;
			}
		}
	}

	public void insertSuperscript(int index, Phrase superPhrase) {
		this.childPhraseList.add(index, superPhrase);
		superPhrase.setSuperscript(true);
	}
	
	public boolean canHaveSuperscript(LineChunk phrase1, Phrase superPhrase, LineChunk phrase2) {
		boolean overlap = false;
		Real2Range bbox1 = phrase1 == null ? null : phrase1.getOrCreateBoundingBox().format(1).getReal2RangeExtendedInX(0.0, 1.0);
		Real2Range bbox2 = phrase2 == null ? null : phrase2.getOrCreateBoundingBox().format(1).getReal2RangeExtendedInX(0.0, 1.0);
		Real2Range superBBox = superPhrase.getOrCreateBoundingBox().format(1).getReal2RangeExtendedInX(1.0, 0.0);
		Real2Range over1 = bbox1 == null ? null : bbox1.intersectionWith(superBBox);
		Real2Range over2 = bbox2 == null ? null : bbox2.intersectionWith(superBBox);
		if ((phrase1 == null || over1 != null) ||
			(phrase2 == null || over2 != null)) {			
//		Double x1 = bbox1.getXMin();
//		Double x2 = bbox2.getXMin();
//		Double superX = superBBox.getXMin();
//		if ()
//		Real2Range over = firstBBox == null ? new Real2Range() : firstBBox.intersectionWith(superBBox);
//		LOG.debug(">ov>"+firstBBox+" / "+superBBox+" / "+over);
//		if ((thisX == null || (over != null && thisX < superX)) {
			LOG.debug("OVER "+this.getStringValue()+" / "+superPhrase.getStringValue());
			overlap = true;
		}
		return overlap;
	}

	public void setSuperscript(boolean b) {
		for (Phrase phrase : this) {
			phrase.setSuperscript(b);
		}
	}

	public void setSubscript(boolean b) {
		for (Phrase phrase : this) {
			phrase.setSubscript(b);
		}
	}



}