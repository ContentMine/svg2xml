package org.xmlcml.svg2xml.table;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nu.xom.Attribute;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.euclid.IntRange;
import org.xmlcml.euclid.IntRangeArray;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGRect;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGShape;
import org.xmlcml.graphics.svg.SVGTitle;
import org.xmlcml.html.HtmlBody;
import org.xmlcml.html.HtmlCaption;
import org.xmlcml.html.HtmlHtml;
import org.xmlcml.html.HtmlTable;
import org.xmlcml.html.HtmlTd;
import org.xmlcml.html.HtmlTh;
import org.xmlcml.html.HtmlTr;
import org.xmlcml.svg2xml.page.PageLayoutAnalyzer;
import org.xmlcml.svg2xml.table.TableSection.TableSectionType;
import org.xmlcml.svg2xml.text.HorizontalElement;
import org.xmlcml.svg2xml.text.HorizontalRuler;
import org.xmlcml.svg2xml.text.PhraseList;
import org.xmlcml.svg2xml.text.PhraseListList;
import org.xmlcml.svg2xml.text.TextStructurer;
import org.xmlcml.svg2xml.util.GraphPlot;
import org.xmlcml.xml.XMLUtil;

public class TableContentCreator extends PageLayoutAnalyzer {

	private static final Logger LOG = Logger.getLogger(TableContentCreator.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	public final static Pattern TABLE_N = Pattern.compile("(T[Aa][Bb][Ll][Ee]\\s+\\d+\\.?\\s+(?:\\(cont(inued)?\\.?\\))?\\s*)");
	private static final String TABLE_FOOTER = "table.footer";
	private static final String TABLE_BODY = "table.body";
	private static final String TABLE_HEADER = "table.header";
	private static final String TABLE_TITLE = "table.title";
	public static final String DOT_ANNOT_SVG = ".annot.svg";
	private static final String DOT_PNG = ".png";
	private static final String CELL_FULL = "cell";
	private static final String CELL_EMPTY = "empty";
        private static final String DATA_CELL_MIN_X = "data-cellminx";
        private static final String DATA_CELL_MIN_Y = "data-cellminy";
        private static final String DATA_ROW_MIN_X = "data-rowminx";
        private static final String DATA_ROW_MIN_Y = "data-rowminy";
        
        private static final String DATA_ATTR_ROLE = "data-role";
        private static final String ROLE_OBSERVATION_LABEL = "obslabel";
        private static final String ROLE_OBSERVATION = "obs";
        private static final String DATA_ATTR_TBL_SEG = "data-tblseg";
        private static final String TBL_SEG_SUBTABLE_TITLE = "subtabletitle";
        
                 
	private List<HorizontalRuler> rulerList;
	private List<TableSection> tableSectionList;
	private IntRangeArray rangesArray;
	private TableTitle tableTitle;
	private boolean addIndents;
	private TableTitleSection tableTitleSection;
	private TableHeaderSection tableHeaderSection;
	private TableBodySection tableBodySection;
	private TableFooterSection tableFooterSection;
	private SVGElement annotatedSvgChunk;
	private double rowDelta = 2.5; //large to manage suscripts
        private final double xEpsilon = 0.1;
	
	public TableContentCreator() {
	}

	/** scans whole file for all tableTitles.
	 * 
	 * @param svgChunkFiles
	 * @return list of titles;
	 */
	public List<TableTitle> findTableTitles(List<File> svgChunkFiles) {
		List<TableTitle> tableTitleList = new ArrayList<TableTitle>();
		for (File svgChunkFile : svgChunkFiles) {
			findTableTitle(tableTitleList, svgChunkFile);
		}
		return tableTitleList;
	}

	private void findTableTitle(List<TableTitle> tableTitleList, File svgChunkFile) {
		TextStructurer textStructurer = TextStructurer.createTextStructurerWithSortedLines(svgChunkFile);
		PhraseListList phraseListList = textStructurer.getPhraseListList();
		phraseListList.format(3);
		String value = phraseListList.getStringValue();
		List<String> titleList = findTitlesWithPattern(value);
		for (int i = 0; i < titleList.size(); i++) {
			TableTitle tableTitle = new TableTitle(titleList.get(i), svgChunkFile.getName());
			tableTitleList.add(tableTitle);
		}
	}

	private List<String> findTitlesWithPattern(String value) {
		Matcher matcher = TABLE_N.matcher(value);
		List<String> titleList = new ArrayList<String>();
		int start = 0;
		while (matcher.find(start)) {
			start = matcher.end();
			String title = matcher.group(0);
			if (titleList.contains(title)) {
				LOG.warn("Duplicate title: "+title);
				title += "*";
			}
			titleList.add(title);
		}
		return titleList;
	}

	public int search(String title) {
		int titleIndex = -1;
		for (int i = 0; i < horizontalList.size(); i++) {
			HorizontalElement horizontalElement = horizontalList.get(i);
			if (horizontalElement instanceof PhraseList) {
				String value = ((PhraseList)horizontalElement).getStringValue().trim();
				if (value.startsWith(title)) {
					titleIndex = i;
					LOG.trace("title["+value+"]");
					break;
				}
			}
		}
		LOG.trace(title+"/"+titleIndex+"/");
		return titleIndex;
	}

	public HorizontalRuler getNextRuler(int irow) {
		HorizontalRuler nextRuler = null;
		getFullRulers(irow);
		return nextRuler;
	}

	/** assume following ruler is representative of this table and find all subsequent full rulers.
	 * 
	 * @param startRow
	 * @return
	 */
	public List<HorizontalRuler> getFullRulers(int startRow) {
		HorizontalRuler firstRuler = null;
		IntRange firstRange = null;
		List<HorizontalRuler> followingRulerList = new ArrayList<HorizontalRuler>();
		IntRange previousRange = null;
		for (int i = startRow; i < horizontalList.size(); i++) {
			HorizontalElement horizontalElement = horizontalList.get(i);
			if (horizontalElement instanceof HorizontalRuler) {
				HorizontalRuler thisRuler = (HorizontalRuler) horizontalElement;
				IntRange thisRange = new IntRange(thisRuler.getBoundingBox().getXRange());
				if (firstRuler == null) {
					firstRuler = thisRuler;
					firstRange = thisRange;
				} else if (!thisRange.isEqualTo(firstRange)) {
					LOG.trace("skipped range: "+thisRange+" vs "+firstRange);
					continue;
				}
				followingRulerList.add(thisRuler);
			}
		}
		return followingRulerList;
	}

	public void createSectionsAndRangesArray() {
		List<HorizontalElement> horizontalList = getHorizontalList();
		int iRow = tableTitle == null ? 0 : search(tableTitle.getTitle());
//		FIXME
//		mergeOverlappingRulersWithSameYInHorizontalElements(iRow);
		if (iRow == -1) {
			LOG.error("Cannot find title: "+tableTitle);
		} else {
			List<HorizontalRuler> fullRulerList = getFullRulers(iRow);
			tableSectionList = new ArrayList<TableSection>();
			IntRange tableSpan = fullRulerList.size() == 0 ? null : fullRulerList.get(0).getIntRange().getRangeExtendedBy(20, 20);
			if (tableSpan != null) {
				this.createSections(horizontalList, iRow, fullRulerList, tableSpan);
				this.createPhraseRangesArray();
				analyzeRangesAndSections();

			}
		}
	}
	
	private IntRangeArray createPhraseRangesArray() {
		rangesArray = new IntRangeArray();
		int length = 0;
		for (TableSection tableSectionX : tableSectionList) {
			int phraseListCount = tableSectionX.getPhraseListCount();
			IntRange intRange = new IntRange(length, length + phraseListCount);
			length += phraseListCount;
			rangesArray.add(intRange);
		}
		return rangesArray;
	}

	private void analyzeRangesAndSections() {
		
		String firstSectionString = tableSectionList.get(0).getStringValue().trim();
		firstSectionString = firstSectionString.substring(0,  Math.min(firstSectionString.length(), 50)).trim();
		String lastSectionString = tableSectionList.get(tableSectionList.size() - 1).getStringValue().trim();
		lastSectionString = lastSectionString.substring(0,  Math.min(lastSectionString.length(), 50)).trim();
		if (firstSectionString.startsWith("Table")) {
			LOG.trace("title 0 "+firstSectionString);
		} else if (lastSectionString.startsWith("Table")) {
			LOG.trace("title last "+lastSectionString);
		} else {
			LOG.debug("***** NO TITLE SECTION ****");//\n"+firstSectionString+"\n"+lastSectionString);
		}
		if (rangesArray.size() == 4) {
			// the commonest
			if (rangesArray.get(0).getMax() > 2) {
				LOG.trace("large title: "+firstSectionString+"\n"+rangesArray);
			} else if (rangesArray.get(1).getRange() > 4) {
				LOG.trace("large header: "+rangesArray);
			} else if (rangesArray.get(2).getRange() < 4) {
				LOG.trace("small body: "+rangesArray);
			}
		} else {
			LOG.debug("Ranges: "+rangesArray.size()+"; "+rangesArray);
		}

	}

	private void createSections(List<HorizontalElement> horizontalList, int iRow, List<HorizontalRuler> fullRulerList,
			IntRange tableSpan) {
		TableSection tableSection = null;
		LOG.trace("start at row: "+iRow+"; "+horizontalList.get(0));
		for (int j = iRow; j < horizontalList.size(); j++) {
			HorizontalElement element = horizontalList.get(j);
			HorizontalRuler ruler = (element instanceof HorizontalRuler) ? 
					(HorizontalRuler) element : null;
			if (tableSection == null || fullRulerList.contains(ruler)) {
				tableSection = new TableSection(TableSectionType.OTHER);
				tableSectionList.add(tableSection);
			}
			if (element instanceof PhraseList) {
				PhraseList newPhraseList = (PhraseList) element;
				if (newPhraseList.size() > 0) {
					tableSection.add(newPhraseList);
				}
				
			} else if (element instanceof HorizontalRuler) {
				// dont add Ruler if first element (e.g sectioning ruler)
				if (tableSection.getHorizontalElementList().size() > 0) {
					tableSection.add(element);
				}
			}
		}
	}

	public IntRangeArray getRangesArray() {
		return rangesArray;
	}

	public List<TableSection> getTableSectionList() {
		return tableSectionList;
	}

	public HtmlHtml createHTMLFromSVG(File inputFile) {
		createContent(inputFile);
		createSectionsAndRangesArray();
		HtmlHtml html = tableStructurer.createHtmlWithTable(tableSectionList, tableTitle);
		try {
			XMLUtil.debug(html, new File("target/table/debug/sections.html"), 1);
		} catch (IOException e) {
		}
		return html;
	}

	public void setTableTitle(TableTitle tableTitle) {
		this.tableTitle = tableTitle;
	}

	public void setAddIndents(boolean add) {
		this.addIndents = add;
	}

	/** FIXME.
	 * works on getTextStructurer().getSVGChunk(). needs refactoring to textStructurer
	 * 
	 * returns original file with overlaid boxes
	 * 
	 * @param colors
	 * @param opacity
	 * @return
	 */
	public SVGElement createMarkedSections(/*SVGElement markedChunk,*/
			String[] colors,
			double[] opacity) {
		// write SVG
		SVGElement markedChunk = getTextStructurer().getSVGChunk();
		SVGG g = new SVGG();
		g.setClassName("sections");
		markedChunk.appendChild(g);
		TableStructurer tableStructurer = getTableStructurer();
		SVGRect plotBox;
		plotBox = GraphPlot.plotBox(tableStructurer.getTitleBBox(), colors[0], opacity[0]);
		plotBox.setClassName(TABLE_TITLE);
		plotBox.appendChild(new SVGTitle(TABLE_TITLE));
		g.appendChild(plotBox);
		plotBox = GraphPlot.plotBox(tableStructurer.getHeaderBBox(), colors[1], opacity[1]);
		plotBox.setClassName(TABLE_HEADER);
		plotBox.appendChild(new SVGTitle(TABLE_HEADER));
		g.appendChild(plotBox);
		plotBox = GraphPlot.plotBox(tableStructurer.getBodyBBox(), colors[2], opacity[2]);
		plotBox.setClassName(TABLE_BODY);
		plotBox.appendChild(new SVGTitle(TABLE_BODY));
		g.appendChild(plotBox);
		plotBox = GraphPlot.plotBox(tableStructurer.getFooterBBox(), colors[3], opacity[3]);
		plotBox.setClassName(TABLE_FOOTER);
		plotBox.appendChild(new SVGTitle(TABLE_FOOTER));
		g.appendChild(plotBox);
		TableContentCreator.shiftToOrigin(markedChunk, g);
		return markedChunk;
	}

	public static void shiftToOrigin(SVGElement markedChunk, SVGG g) {
		SVGG gg = null;
		SVGElement svgElement =  (SVGElement) markedChunk.getChildElements().get(0);
		if (svgElement instanceof SVGG) {
			SVGG firstG = (SVGG) markedChunk.getChildElements().get(0);
			Transform2 t2 = firstG.getTransform();
			g.setTransform(t2);
		}
	}

	public TableTitleSection getOrCreateTableTitleSection() {
		if (tableTitleSection == null) {
			List<TableSection> tableSectionList = getTableStructurer().getTableSectionList();
			if (tableSectionList.size() >= 1) {
				tableTitleSection = new TableTitleSection(tableSectionList.get(0));
			}
		}
		return tableTitleSection;
	}

	public TableHeaderSection getOrCreateTableHeaderSection() {
		if (tableHeaderSection == null) {
			List<TableSection> tableSectionList = getTableStructurer().getTableSectionList();
			if (tableSectionList.size() >= 2) {
				tableHeaderSection = new TableHeaderSection(tableSectionList.get(1));
			}
		}
		return tableHeaderSection;
	}

	public TableBodySection getOrCreateTableBodySection() {
		if (tableBodySection == null) {
			List<TableSection> tableSectionList = getTableStructurer().getTableSectionList();
			if (tableSectionList.size() >= 3) {
				tableBodySection = new TableBodySection(tableSectionList.get(2));
			}
		}
		return tableBodySection;
	}

	public TableFooterSection getOrCreateTableFooterSection() {
		if (tableFooterSection == null) {
			List<TableSection> tableSectionList = getTableStructurer().getTableSectionList();
			if (tableSectionList.size() >= 4) {
				tableFooterSection = new TableFooterSection(tableSectionList.get(3));
			}
		}
		return tableFooterSection;
	}


	public SVGElement getSVGChunk() {
		return textStructurer.getSVGChunk();
	}

	public SVGElement annotateAreas(File inputFile) {
		createHTMLFromSVG(inputFile);
		return annotateAreasInSVGChunk();
	}

	public SVGElement annotateAreasInSVGChunk() {
		SVGElement svgChunk = createMarkedSections(
				new String[] {"yellow", "red", "cyan", "blue"},
				new double[] {0.2, 0.2, 0.2, 0.2}
			);
		TableTitleSection tableTitle = getOrCreateTableTitleSection();
		if (tableTitle == null) {
			LOG.warn("no table title");
		} else {
			svgChunk = tableTitle.createMarkedContent(
					(SVGElement) svgChunk.copy(),
					new String[] {"yellow", "yellow"}, 
					new double[] {0.2, 0.2}
					);
		}
		TableHeaderSection tableHeader = getOrCreateTableHeaderSection();
		if (tableHeader == null) {
			LOG.warn("no table header");
		} else {
			tableHeader.createHeaderRowsAndColumnGroups();
			svgChunk = tableHeader.createMarkedSections(
					(SVGElement) svgChunk.copy(),
					new String[] {"blue", "green"}, 
					new double[] {0.2, 0.2}
					);
		}
		TableBodySection tableBody = getOrCreateTableBodySection();
		if (tableBody == null) {
			LOG.trace("no table body");
		} else {
			tableBody.createHeaderRowsAndColumnGroups();
			svgChunk = tableBody.createMarkedSections(
					(SVGElement) svgChunk.copy(),
					new String[] {"yellow", "red"}, 
					new double[] {0.2, 0.2}
					);
		}
		TableFooterSection tableFooter = getOrCreateTableFooterSection();
		if (tableFooter != null) {
			svgChunk = tableFooter.createMarkedContent(
					(SVGElement) svgChunk.copy(),
					new String[] {"blue", "blue"}, 
					new double[] {0.2, 0.2}
					);
		}
		return svgChunk;
	}

	public void markupAndOutputTable(File inputFile, File outDir) {
		String outRoot = inputFile.getName();
		outRoot = outRoot.substring(0, outRoot.length() - DOT_PNG.length());
		LOG.trace("reading SVG "+inputFile);
		annotatedSvgChunk = annotateAreas(inputFile);
		File outputFile = new File(outDir, outRoot+DOT_ANNOT_SVG);
		LOG.trace("writing annotated SVG "+outputFile);
		SVGSVG.wrapAndWriteAsSVG(annotatedSvgChunk, outputFile);
	}

	/** create HTML from annot.svg
	 * 
	 * @param annotSvgFile
	 * @param outDir
	 * @throws IOException 
	 */
	public void createHTML(File annotSvgFile, File outDir) throws IOException {
		LOG.debug("reading SVG from "+annotSvgFile);
		HtmlHtml html = createHtmlFromSVG();
		File outfile = new File(outDir, annotSvgFile.getName()+".html");
		LOG.debug("writing HTML to : "+outfile);
		XMLUtil.debug(html, outfile, 1);
		
		
	}

	public HtmlHtml createHtmlFromSVG() {
		HtmlHtml html = new HtmlHtml();
		HtmlBody body = new HtmlBody();
		html.appendChild(body);
		HtmlTable table = new HtmlTable();
		table.setClassAttribute("table");
		body.appendChild(table);
		
		addCaption(annotatedSvgChunk, table);
		int bodyCols = getGElements(annotatedSvgChunk).size();
		addHeader(annotatedSvgChunk, table, bodyCols);
		addBody(annotatedSvgChunk, table);
                
		return html;
	}

	private void addHeader(SVGElement svgElement, HtmlTable table, int bodyCols) {
		int cols = 0;
		HtmlTr tr = new HtmlTr();
                tr.addAttribute(new Attribute("data-tblrole", "columnheaderrow"));
		table.appendChild(tr);
		SVGElement g = svgElement == null ? null : (SVGElement) XMLUtil.getSingleElement(svgElement, 
				".//*[local-name()='g' and @class='"+TableHeaderSection.HEADER_COLUMN_BOXES+"']");
		if (g != null) {
			cols = addHeaderBoxes(tr, g, bodyCols);
		}
	}

	private int addHeaderBoxes(HtmlTr tr, SVGElement g, int bodyCols) {
		List<SVGRect> rects = SVGRect.extractSelfAndDescendantRects(g);
		int headerCols = rects.size();
		int bodyDelta = bodyCols - headerCols;
		LOG.trace("Header boxes: "+headerCols+"; delta: "+bodyDelta);
		for (int i = 0; i < bodyDelta; i++) {
			HtmlTh th = new HtmlTh();
			tr.appendChild(th);
		}
		for (int i = 0; i < headerCols; i++) {
			SVGRect rect = rects.get(i);   // messy but has to be rewritten
			Real2 xy = rect.getXY();
			String title = rect.getValue();   // messy but has to be rewritten
			title = title.replace(" //", "");
			HtmlTh th = new HtmlTh();
			th.setClassAttribute(CELL_FULL);
			th.appendChild(title.substring(title.indexOf("/")+1));
			tr.appendChild(th);
		}
		return headerCols;
	}

	private void addBody(SVGElement svgElement, HtmlTable table) {
		List<SVGG> gs = getGElements(svgElement);
		if (gs.size() == 0) {
			LOG.warn("No annotated body");
			return;
		}
		List<List<SVGRect>> columnList = new ArrayList<List<SVGRect>>();
		for (int i = 0; i < gs.size(); i++) {
			List<SVGRect> rects = SVGRect.extractSelfAndDescendantRects(gs.get(i));
			columnList.add(rects);
		}
		LOG.trace("Body columns: "+columnList.size());

		if (columnList.size() == 0) {
			return;
		}

		List<RealRange> allRanges = createRowRanges(columnList);
		for (int jcol = 0; jcol < columnList.size(); jcol++) {
			List<SVGRect> column = columnList.get(jcol);
			padColumn(column, allRanges);
		}
		
		for (int irow = 0; irow < allRanges.size(); irow++) {
			createRowsAndAddToTable(table, columnList, irow);
		}
                
                // Re-structure into subtables
                HtmlTable transformedTable = createSubtablesFromIndents(table, columnList.size());
                
                // If transformed table exists and is basically well formed
                // then replace the raw set of body rows (containing tds)
                // Better to use thead tbody tfoot structure?
                if (transformedTable != null && transformedTable.getRows() != null) {
                    // Replace the rows after the header rows
                    // Better to use a tbody and replace that?
                    ArrayList<HtmlTr> currentRows = (ArrayList<HtmlTr>)table.getRows();
                    
                    for (HtmlTr tr : currentRows) {
                        Attribute attr = tr.getAttribute("data-tblrole");
                        
                        if (attr == null || !("columnheaderrow".equals(attr.getValue()))) {
                            table.removeChild(tr);
                        }
                    }
                     
                    for (HtmlTr stTr : transformedTable.getRows()) {
                        table.appendChild((HtmlTr)(HtmlTr.create(stTr)));
                    }
                }
	}

	private List<SVGG> getGElements(SVGElement svgElement) {
		SVGElement g = svgElement == null ? null : (SVGElement) XMLUtil.getSingleElement(svgElement, 
				".//*[local-name()='g' and @class='"+TableBodySection.BODY_CELL_BOXES+"']");
		List<SVGG> gs = (g == null) ? new ArrayList<SVGG>() : SVGG.extractSelfAndDescendantGs(g);
		return gs;
	}

	private void createRowsAndAddToTable(HtmlTable table, List<List<SVGRect>> columnList, int irow) {
		HtmlTr tr = new HtmlTr();
		table.appendChild(tr);
                double tblMinX = 0.0;
		for (int jcol = 0; jcol < columnList.size(); jcol++) {
			List<SVGRect> rectjList = columnList.get(jcol);
			if (irow >= rectjList.size()) {
				LOG.trace("row index out of range "+irow);;
			} else {
				SVGShape rectij = rectjList.get(irow);
				HtmlTd td = new HtmlTd();
				tr.appendChild(td);
				String value = rectij == null ? "/" : rectij.getValue();
				String value1 = value.substring(value.indexOf("/")+1);
				td.appendChild(value1);
				td.setClassAttribute((value1.trim().length() == 0) ? CELL_EMPTY : CELL_FULL);
                                addLayoutDataAttributes(tr, td, rectij, rectjList, irow);
			}
		}
	}

	private List<RealRange> createRowRanges(List<List<SVGRect>> columnList) {
		// populate allRanges with column0
		List<RealRange> allRanges = new ArrayList<RealRange>();
		List<SVGRect> column0 = columnList.get(0);
		if (column0.size() == 0) {
			return allRanges; // no rows
		}
		for (int irow = 0; irow < column0.size(); irow++) {
			SVGRect rowi = column0.get(irow);
			RealRange rowRange = rowi.getBoundingBox().getYRange().format(3);
			allRanges.add(rowRange);
		}
		
		// iterate over other columns, filling in holes if necessary
		for (int jcol = 0; jcol < columnList.size(); jcol++) {
			List<SVGRect> columnj = columnList.get(jcol);
			int allPtr = allRanges.size() - 1;
			int colPtr = columnj.size() - 1;
			if (colPtr > allPtr) {
				LOG.error("Column ("+jcol+"; "+(colPtr+1)+") larger than allRanges ("+(allPtr+1)+") \n"+columnj+"; \n"+allRanges);
			}
			while (colPtr >= 0) {
				SVGRect rowi = columnj.get(colPtr);
				RealRange colRange = rowi.getBoundingBox().getYRange();
				RealRange allRange = allRanges.get(allPtr);
				if (colRange.intersectsWith(allRange)) {
					RealRange newRange = colRange.plus(allRange);
					allRanges.set(allPtr, newRange);
					LOG.trace("equal: "+allPtr+"; "+colPtr);
					allPtr--;
					colPtr--;
				} else if (colRange.getMax() < allRange.getMin()) {
					LOG.trace("less: "+allPtr+"; "+colPtr);
					allPtr--;
				} else if (colRange.getMin() > allRange.getMax()) {
					LOG.trace("more: "+allPtr+"; "+colPtr);
					allRanges.add(allPtr + 1, colRange);
					colPtr--;
				} else {
					throw new RuntimeException("cannot add to allRanges "+allRange+"; "+colRange);
				}
				if (allPtr < 0 && colPtr >= 0) {
					LOG.error("Cannot match col=>all "+colPtr+" => "+allPtr+"; "+columnj.size()+" => "+allRanges.size()+" => "+columnj+" => "+allRanges);
					break;
				}
			}
		}
		return allRanges;
	}

	private void padColumn(List<SVGRect> column, List<RealRange> allRanges) {
		int allPtr = allRanges.size() - 1;
		int colPtr = column.size() - 1;
		while (allPtr >= 0) {
			if (colPtr < 0) {
				// empty space at start of column
				column.add(0, (SVGRect) null);
				allPtr--;
			} else {
				RealRange allRange = allRanges.get(allPtr);
				RealRange colRange = column.get(colPtr).getBoundingBox().getYRange();
				if (colRange.intersectsWith(allRange)) {
					// ranges match
					colPtr--;
					allPtr--;
				} else if (colRange.getMin() > allRange.getMax()) {
					throw new RuntimeException("IMPOSSIBLE "+allRange+"; "+colRange);
				} else if (colRange.getMax() < allRange.getMin()) {
					// empty cell in column
					column.add(colPtr + 1, (SVGRect) null);
					allPtr--;
				} else {
					throw new RuntimeException("cannot map to allRanges "+allRange+"; "+colRange);
				}
			}
		}
	}
        
        /**
         * Record page layout data as HTML data attributes
         */
        private void addLayoutDataAttributes(HtmlTr tr, HtmlTd td, SVGShape rectij, List<SVGRect> rectjList, int irow) {
            DecimalFormat decFormat = new DecimalFormat("0.000");
     
            // Record position data in each cell
            if (rectij != null) {
                double cellMinX = rectij.getBoundingBox().getXMin();
                double cellMinY = rectjList.get(irow).getBoundingBox().getYMin();
                td.setAttribute(DATA_CELL_MIN_X, decFormat.format(cellMinX));
                td.setAttribute(DATA_CELL_MIN_Y, decFormat.format(cellMinY));

                // As soon as we get a non-empty cell set the row minimum x, y once  
                if (tr.getAttributeValue(DATA_ROW_MIN_Y) == null) {
                    tr.setAttribute(DATA_ROW_MIN_X, decFormat.format(cellMinX));
                    tr.setAttribute(DATA_ROW_MIN_Y, decFormat.format(cellMinY));
                }                
            }
        }
        
        private boolean rowIsRightClear(HtmlTr tr) {
            boolean isRightClear = false;
            
            List<HtmlTd> tds = tr.getTdChildren();
            
            if (!tds.isEmpty()) {
                // First cell has content
                isRightClear = (tds.get(0).getClassAttribute().equals("cell"));
                
                if (isRightClear) {
                    // All other cells are empty
                    int jcol = 1;
                    
                    while (isRightClear && jcol < tds.size()) {
                        isRightClear = (isRightClear && tds.get(jcol).getClassAttribute().equals("empty"));
                        jcol++;
                    }
                }
            }
            
            return isRightClear;
        }
        
        /**
         * Use layout information in HTML to find subtables using relative x positions
         * across rows.
         * @param table The table as a flat list of rows with cells marked up with raw x,y layout values 
         * @return A transformed copy of the table with subtables as nested tables
         */
        private HtmlTable createSubtablesFromIndents(HtmlTable table, int columnCount) {
            HtmlTable restructTable = null;
            
            if (table != null) {
                double curMinX = 0.0;
                double prevMinX = 0.0;
                
                double curRowMinX;
                HtmlTr prevRow = null;           
                           
                List<HtmlTr> rows = table.getRows();
                
                HtmlTable currentSubtable = null;
                restructTable = new HtmlTable();
                
                LOG.debug("---");
                                    
                for (int irow = 0; irow < rows.size(); irow++) {
                    HtmlTr tr = rows.get(irow);
                    
                    // Set prevRow every time.
                    // Defer processing until next line when indent relationship is known
                    // Logic from indents determines where prevRow is put into restruct table
                    
                    // Skip the header row
                    ArrayList<HtmlTh> ths = (ArrayList<HtmlTh>)tr.getThChildren();
                    
                    if (ths != null && ths.size() > 0) {
                        // A header row cannot be part of a subtable
                        tr.addAttribute(new Attribute("data-tblrole","columnheaderrow"));
                        continue;
                    }
                    
                    // Get the min X of the current row
                    Attribute attr = tr.getAttribute("data-rowminx");
                    curRowMinX = Double.parseDouble(attr == null ? "0.0" : attr.getValue());
                    
                    // Find left edge of current table or subtable
                    if (curRowMinX > 0.0) {
                        curMinX = curRowMinX;
                    }
                    
                    // Make a copy of the current row reference and 
                    // store it until its role is determined at the next row
                    if (irow == 1) {
                        prevRow = tr;
                    }
                    
                    LOG.debug("R:"+irow+"\t"+extractRowLabel(tr));
                    
                    if (curMinX != 0.0) {
                        if (isGreaterThan(curMinX, prevMinX, xEpsilon)) {
                            // INDENT or FIRST
                            if (prevMinX > 0.0) {
                                // This is an indent -- a new subtable has started
                                LOG.debug("ST:Indent: ("+prevMinX+"->"+curMinX+")");
                                LOG.debug("R:"+irow+"\t"+"[IN]\t\t\t"+"ST?:"+(currentSubtable != null ? "Y" : "N"));
                                
                                currentSubtable = new HtmlTable();

                                // The previous line was subtable heading line
                                if (irow > 0) {      
                                    LOG.debug("Add ST header:"+irow+"\t"+extractRowLabel(prevRow));
                                    this.addSubtableRow(currentSubtable, prevRow, true);
                                }
                            } 
                        } else if (isEqualTo(curMinX, prevMinX, xEpsilon)) {
                            // ALIGNED
                            LOG.debug("Obs row: (" + prevMinX + "==" + curMinX + ")");
                            LOG.debug("R:"+irow+"\t"+"[ALIGN]\t\t\t"+"ST?:"+(currentSubtable != null ? "Y" : "N"));
                            if (curMinX != 0.0) {        
                                if (!rowIsRightClear(tr)) {
                                    if (tr.getTd(0) != null) {
                                        tr.getTd(0).setAttribute("data-role", "obslabel");
                                    }
                                }
                                
                                if (currentSubtable != null) {
                                    addSubtableRow(currentSubtable, prevRow, false);
                                } else {
                                    addTopLevelRow(restructTable, prevRow);
                                }
                            } 
                        } else { 
                            // curMinX < prevMinX, within tolerance
                            // OUTDENT
                            LOG.debug("R:"+irow+"\t"+"[OUT]\t\t\t"+"ST?:"+(currentSubtable != null ? "Y" : "N"));
                            // This is the start of a new section
                            // The previous line is the end of the subtable
                            this.addSubtableRow(currentSubtable, prevRow, false);
                            List<HtmlTr> stRows = currentSubtable.getRows();
                            LOG.debug("ST:Complete: ("+prevMinX+"->"+curMinX+"):total rows:"+(stRows != null ? stRows.size() : 0));
                            
                            // Add the completed subtable to the restructured table
                            HtmlTr subtableWrapperRow = wrapSubtableAsRow(currentSubtable, columnCount);
                            restructTable.appendChild(subtableWrapperRow);
  
                            currentSubtable = null; 
                        }
                        
                        // Make a copy of the current row reference and 
                        // store it until its role is determined at the next row
                        prevRow = tr;
                    
                        prevMinX = curRowMinX;
                    }
                }
                
                // At the end of main table processing 
                // and allocate final row and
                // close any final incomplete subtable
                if (currentSubtable != null) {
                    addSubtableRow(currentSubtable, prevRow, false);
                    
                    LOG.debug("ST:Complete At Table end: (" + prevMinX + "->" + curMinX + "):total rows:" + currentSubtable);
                    // Wrap up subtable rows as a row in the transformed table
                    HtmlTr subtableWrapperRow = wrapSubtableAsRow(currentSubtable, columnCount);
                    restructTable.appendChild(subtableWrapperRow);
                } else {
                    addTopLevelRow(restructTable, prevRow);
                }
            }
            
            return restructTable;
        }

        /**
         * Helper method.  
         * Add top-level row to table.
         * Top-level rows are observation labels and observation data.
         */
        private void addTopLevelRow(HtmlTable mainTable, HtmlTr topLevelRow) {
            if (topLevelRow == null) {
                return;
            }
            
            topLevelRow.setAttribute(ROLE_OBSERVATION, ROLE_OBSERVATION);
            
            if (!rowIsRightClear(topLevelRow)) {
                if (topLevelRow.getTd(0) != null) {
                    topLevelRow.getTd(0).setAttribute(DATA_ATTR_ROLE, ROLE_OBSERVATION_LABEL);
                }
            }

            // Add a deep-copy of the row to mainTable
            HtmlTr trDeepCopy = (HtmlTr) (HtmlTr.create(topLevelRow));
            mainTable.addRow(trDeepCopy);
        }

        /**
         * Helper method.  
         * Add top-level row to table.
         * Top-level rows are observation labels and observation data.
         */
        private void addSubtableRow(HtmlTable subTable, HtmlTr subtableRow, boolean isSubtableHeading) {
            if (subTable == null) {
                return;
            }
            
            String rowLabel = extractRowLabel(subtableRow);
            LOG.debug("addSubtableRow: Add ST "+(isSubtableHeading ? "header" : "row" )+":"+rowLabel);
            
            if (isSubtableHeading) {
                subtableRow.setAttribute(DATA_ATTR_TBL_SEG, TBL_SEG_SUBTABLE_TITLE);   
            } else {
                subtableRow.setAttribute(DATA_ATTR_ROLE, ROLE_OBSERVATION);
            }
            
            if (!rowIsRightClear(subtableRow)) {
                if (subtableRow.getTd(0) != null) {
                    subtableRow.getTd(0).setAttribute(DATA_ATTR_ROLE, ROLE_OBSERVATION_LABEL);
                }
            }

            // Add a deep-copy of the row to subTable
            HtmlTr trDeepCopy = (HtmlTr) (HtmlTr.create(subtableRow));
            subTable.addRow(trDeepCopy);
        }
        
        /**
         * Helper method.
         * @return Label 
         */
        String extractRowLabel(HtmlTr row) {
            String rowLabel = "";
            
            if (row != null) {
                if (row.getTd(0) != null) {
                    rowLabel = row.getTd(0).getValue();
                }
            } 
            
            return rowLabel;
        }
              
        /**
         * The current assumption is that the main table comprises a list of tr only
         * Wrap the subtable <table> element in a td and tr for compatibility with this.
         * FIXME Subtables rows should be contained in a tbody instead 
         * to avoid need for extra tr, td and td colspan=columnCount, with adjustments 
         * to downstream processing of tables as tr lists
         * @param subtable
         * @param columnCount
         * @return 
         */
        private HtmlTr wrapSubtableAsRow(HtmlTable subtable, int columnCount) {
            HtmlTr subtableWrapperRow = new HtmlTr();
            // Need to add a colspan for main table columns
            // so nested table is displayed across the width of the enclosing table
            HtmlTd subtableWrapperCell = new HtmlTd();
            subtableWrapperCell.addAttribute(new Attribute("colspan", Integer.toString(columnCount)));
            subtable.setClassAttribute("subtable");
            subtableWrapperCell.appendChild(subtable);
            
            subtableWrapperRow.appendChild(subtableWrapperCell);
            
            return subtableWrapperRow;
        }
        
        /**
         * Compare coordinates with explicit tolerance
         * 
         */
        private boolean isEqualTo(double d1, double d2, double tolerance) {
            return (Math.abs(d1 - d2) <= tolerance);
        }
        
        /**
         * Compare coordinates with explicit tolerance
         * 
         */
        private boolean isGreaterThan(double d1, double d2, double tolerance) {
            return ((d1 - d2) > tolerance);
        }

	// FIXME empty caption
	private void addCaption(SVGElement svgElement, HtmlTable table) {
		HtmlCaption caption = new HtmlCaption();
		String captionS = svgElement == null ? null : XMLUtil.getSingleValue(svgElement, ".//*[local-name()='g' and @class='"+TableTitleSection.TITLE_TITLE+"']");
		if (captionS !=null) {
			int idx = captionS.indexOf("//");
			captionS = idx == -1 ? captionS : captionS.substring(idx + 2);
	//		caption.appendChild(captionS.substring(captionS.indexOf("//")+2));
			table.appendChild(caption);
		}
	}

	public SVGElement getAnnotatedSvgChunk() {
		return annotatedSvgChunk;
	}

}
