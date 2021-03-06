package org.xmlcml.svg2xml.container;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Node;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlLi;
import org.xmlcml.svg2xml.text.ScriptLine;
import org.xmlcml.svg2xml.text.TextStructurer;
import org.xmlcml.svg2xml.util.SVG2XMLUtil;

/** holds a chunk of scriptLines
 * examples are list elements
 * @author pm286
 *
 */
public class ListItem {

	private final static Logger LOG = Logger.getLogger(ListItem.class);
	
	/** leading number
	 * e.g. 2.  [2]  {2}  (2)  2:  -2-
	 * 
	 */
	private static final String LEADING_NUMBER = 
			"\\s*[\\(\\{\\<\\-]?\\s*(\\d+)\\s*[\\.\\)\\}\\>]?\\s*[\\.:\\-]?.*";

	/** leading bullet
	 * e.g. * or middot or black bullet or white bullet
	 * 
	 */
	private static final String LEADING_BULLET = 
			"\\s*([\\*\\u00b7\\u2022\\u2219\\u25D8\\u25E6]).*";
	// FIXME
	
	
	private List<ScriptLine> scriptLineList;
	private boolean indented;
	
	public ListItem() {
		
	}

	public void add(ScriptLine scriptLine) {
		ensureScriptLineList();
		scriptLineList.add(scriptLine);
	}

	private void ensureScriptLineList() {
		if (scriptLineList == null) {
			scriptLineList = new ArrayList<ScriptLine>();
		}
	}

	public List<ScriptLine> getScriptLineList() {
		ensureScriptLineList();
		return scriptLineList;
	}

	public int size() {
		ensureScriptLineList();
		return scriptLineList.size();
	}
	
	public ScriptLine get(int index) {
		ensureScriptLineList();
		return scriptLineList.get(index);
	}
	
	public Integer getLeadingInteger() {
		Integer leadingInteger = null;
		Pattern listItemNumber = Pattern.compile(LEADING_NUMBER);
		String value = (indented || scriptLineList.size() == 0) ? null : scriptLineList.get(0).getRawValue();
		if (value != null) {
			Matcher matcher = listItemNumber.matcher(value);
			if (matcher.matches()) {
				String integerS = matcher.group(1);
				// some numbers might just be very large 
				if (integerS.length() < 6) {
					try {
						leadingInteger = new Integer(integerS);
					} catch (NumberFormatException nfe) {
						
					}
				}
			}
		}
		return leadingInteger;
	}
	
	public String getBullet() {
		String bullet = null;
		Pattern listItemBullet = Pattern.compile(LEADING_BULLET);
		String value = (indented || scriptLineList.size() == 0) ? null : scriptLineList.get(0).getRawValue();
		if (value != null) {
			Matcher matcher = listItemBullet.matcher(value);
			if (matcher.matches()) {
				bullet = matcher.group(1);
			}
		}
		return bullet;
	}

	public void setIndented(boolean indented) {
		this.indented = indented;
	}
	
	public HtmlElement createHtmlElement() {
		HtmlElement li = new HtmlLi();
		ScriptContainer scriptContainer = new ScriptContainer(scriptLineList);
		HtmlElement e = scriptContainer.createHtmlElement();
    	SVG2XMLUtil.moveChildrenFromTo(e, li);
		return li;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (ScriptLine scriptLine : scriptLineList) {
			sb.append(scriptLine.getTextContentWithSpaces()+"\n");
		}
		return sb.toString();
	}
}