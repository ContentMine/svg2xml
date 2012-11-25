package org.xmlcml.svgplus.command;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import nu.xom.Nodes;

import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.svgplus.tools.PageSelector;
import org.xmlcml.svgplus.util.GraphUtil;


public abstract class PageAction extends AbstractAction {
	
	final static Logger LOG = Logger.getLogger(PageAction.class);

	public static final String DOCUMENT = "document";
	public static final String PAGE = "page";
	public static final String EXIT = "exit";


	// private so it can't be modified - have to copy it - is this a good idea?
	private PageSelector pageSelector;
	private int pageCount;

	public PageAction(AbstractActionElement actionElement) {
		super(actionElement);
	}
	
	public Integer getBoxCount() {
		return getInteger(PageActionElement.BOX_COUNT);
	}
	
	public Integer getDecimalPlaces() {
		return getInteger(PageActionElement.FORMAT_DECIMAL_PLACES);
	}
	
	public Integer getDepth() {
		return getInteger(PageActionElement.DEPTH);
	}
	
	public String getDeleteXPaths() {
		return getAndExpand(PageActionElement.DELETE_XPATHS);
	}
	
	public String getFail() {
		return getAndExpand(PageActionElement.FAIL);
	}
	
	public String getFill() {
		return getAndExpand(PageActionElement.FILL);
	}
	
	public Double getMarginX() {
		return getDouble(PageActionElement.MARGIN_X);
	}
	
	public Double getMarginY() {
		return getDouble(PageActionElement.MARGIN_Y);
	}
	
	public Double getOpacity() {
		return getDouble(PageActionElement.OPACITY);
	}
	
	public String getPageRange() {
		return getAndExpand(PageActionElement.PAGE_RANGE);
	}
	
	public String getStroke() {
		return getAndExpand(PageActionElement.STROKE);
	}
	
	public Double getStrokeWidth() {
		return getDouble(PageActionElement.STROKE_WIDTH);
	}
	
	public List<String> getVariables() {
		String s = getActionElement().getAttributeValue(PageActionElement.VARIABLES);
		String[] ss = (s == null) ? null : s.split(CMLConstants.S_WHITEREGEX);
		return (ss == null) ? null : Arrays.asList(ss);
	}
	
	protected void deleteNodes(String xpath) {
		if (xpath != null) {
			Nodes nodes = GraphUtil.query(getSVGPage(), xpath);
			for (int i = 0; i < nodes.size(); i++) {
				nodes.get(i).detach();
			}
		}
	}

	public PageSelector getPageSelector() {
		if (pageSelector == null) {
			String pageRange = getPageRange();
			pageCount = (Integer) semanticDocumentAction.getVariable(PageIteratorAction.PAGE_COUNT);
			pageSelector = (pageRange == null) ? null : new PageSelector(pageCount);
		}
		return pageSelector;
	}

	protected void fail(String string) {
		String fail = getFail();
		if (EXIT.equalsIgnoreCase(fail)) {
			throw new RuntimeException(string+" ... "+getActionElement().toXML());
		} else {
			LOG.error("******** FAIL: "+string+" *************");
		}
	}

	protected void warn(String string) {
		LOG.error("******** WARN: "+string+" *************");
	}

	protected void info(String string) {
		LOG.error("******** INFO: "+string+" *************");
	}

	protected void log(String string) {
		info(string);
	}

	protected void debugFile(String filename) {
		SVGSVG svg = new SVGSVG(getSVGPage());
		List<SVGElement> defs = SVGUtil.getQuerySVGElements(svg, ".//svg:defs");
		for (SVGElement def : defs) def.detach();
		CMLUtil.outputQuietly(svg, new File(filename), 1);
	}

	public PageEditor getPageEditor() {
		return semanticDocumentAction.getPageEditor();
	}
}
