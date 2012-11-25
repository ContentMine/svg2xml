package org.xmlcml.svgplus.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xmlcml.svgplus.command.AbstractPageAnalyzer;
import org.xmlcml.svgplus.command.PageEditor;
import org.xmlcml.svgplus.core.SemanticDocumentAction;

/**
 * @author pm286
 *
 */
public class TableAnalyzer extends AbstractPageAnalyzer {
	private static final Logger LOG = Logger.getLogger(TableAnalyzer.class);
	
	private List<Table> tableList;

	public TableAnalyzer(SemanticDocumentAction semanticDocumentAction) {
		super(semanticDocumentAction);
	}
	
	public void analyze() {
	}

	public List<Table> findTables() {
		tableList = new ArrayList<Table>();
		// NYI
//		caption = (Caption) Chunk.createFromAndReplace((SVGG)captions.get(0), new Caption(pgeAnalyzer, "bar"));
//
//		List<SVGElement> tables = SVGUtil.getQuerySVGElements(
//				svgPage, "//svg:g[svg:g/svg:g[@name='para']/svg:text[starts-with(., 'Table ')]]");
//		for (SVGElement elem : tables) {
//			createCaptionAndReplace(elem, "svg:g/svg:g[@name='para' and svg:text[starts-with(., 'Table ')]]", Table.TABLE);
//			Table table = Table.createFromAndReplace((SVGG) elem);
//			table.removeOriginalText();
//			tableList.add(table);
//		}
		return tableList;
	}
	
	
}
