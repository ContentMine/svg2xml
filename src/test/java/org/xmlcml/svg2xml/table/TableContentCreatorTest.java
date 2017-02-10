package org.xmlcml.svg2xml.table;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.svg2xml.Fixtures;
import org.xmlcml.svg2xml.text.PhraseListList;
import org.xmlcml.svg2xml.text.SuscriptEditor;
import org.xmlcml.xml.XMLUtil;


//@Ignore
public class TableContentCreatorTest {
	
	static final Logger LOG = Logger.getLogger(TableContentCreatorTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	private static final double IMG_XSCALE = 700.0;
	@Test
	public void testSimple1() {
		File inputFile = new File(Fixtures.TABLE_DIR, "grid/simple1.svg");
		File outDir = new File("target/table/grid/");
		// refactor this stack
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
//		Assert.assertEquals("cols",  5, tableContentCreator.getColumnCount());
//		Assert.assertEquals("rows",  7, tableContentCreator.getRowCount());
	}
	
	@Test
	public void testTable1() {
		File inputFile = new File(Fixtures.TABLE_DIR, "grid/table1.svg");
		File outDir = new File("target/table/grid/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
	}

	@Test
	public void testSimple2() {
		File inputFile = new File(Fixtures.TABLE_DIR, "grid/simple2.svg");
		File outDir = new File("target/table/grid/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
	}

	@Test
	public void testSimple3() {
		File inputFile = new File(Fixtures.TABLE_DIR, "grid/simple3.svg");
		File outDir = new File("target/table/grid/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
//		int rows = tableContentCreator.getRowCount();
//		Assert.assertEquals("rows",  8, rows);
//		int cols = tableContentCreator.getColumnCount();
//		Assert.assertEquals("cols",  5, cols);
	}

	@Test
	public void testSimple4() {
		File inputFile = new File(Fixtures.TABLE_DIR, "grid/simple4.svg");
		File outDir = new File("target/table/grid/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
	}
	
	
	// box	10.1016_S0140-6736(16)31461-1/
	// not yet solved
	@Test
	public void testBox1() {
		File inputFile = new File(Fixtures.TABLE_DIR, "box/table1.svg");
		File outDir = new File("target/table/box/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
	}
	
	@Test
	// 	10.1016_j.pain.2014.09.020
	public void testGlueTables() {
		File inputFile1 = new File(Fixtures.TABLE_DIR, "glue/table3.svg");
		File inputFile1cont = new File(Fixtures.TABLE_DIR, "glue/table3cont.svg");
		File inputFile1annot = new File(Fixtures.TABLE_DIR, "glue/table3.svg");
		File inputFile1annotcont = new File(Fixtures.TABLE_DIR, "glue/table3cont.svg");
		File outDir = new File("target/table/glue/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile1, outDir);
	}

	@Test
	/** simple rectangular Table.
	 * No subtables or split columns
	 * @throws IOException
	 */
	public void testSimple2Html() throws IOException {
//		cm-ucl/corpus-oa-pmr/10.1016_j.pain.2014.09.033/pdftable/table1.annot.svg
		File inputFile1 = new File(Fixtures.TABLE_DIR, "html/simple/10.1016_j.pain.2014.09.033.annot.svg");
//		10.1016_j.jadohealth.2016.10.001/pdftable/table3.annot.svg
//		File inputFile1 = new File(Fixtures.TABLE_DIR, "html/table3.annot.svg");
		File outDir = new File("target/table/html/simple");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.createHTML(inputFile1, outDir);
	}
	
	@Test
	@Ignore
	// spurious SVGText with whitespace content
	public void testWhitespaceProblem() {
		File inputFile = new File(Fixtures.TABLE_DIR, "whitespace/table1.svg");
		File outDir = new File("target/table/whitespace/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile, outDir);
	}

	@Test
	/** indents.
	 * No subscripts or split columns
	 * @throws IOException
	 */
	public void testIndents() throws IOException {
//		cm-ucl/corpus-oa-pmr/10.1186_1471-2431-13-190/pdftable/table1.annot.svg
		File inputFile1 = new File(Fixtures.TABLE_DIR, "indent/10.1186_1471-2431-13-190.annot.svg");
		File outDir = new File("target/table/indent/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.createHTML(inputFile1, outDir);
	}
	
	@Test
	/** split column/s
	 * No subscripts or indents
	 * @throws IOException
	 */
	public void testSplitColumn() throws IOException {
//		cm-ucl/corpus-oa-pmr/1	10.1179_1743132815Y.0000000050/pdftable/table5.annot.svg
		File inputFile1 = new File(Fixtures.TABLE_DIR, "splitcol/10.1179_1743132815Y.0000000050.annot.svg");
		File outDir = new File("target/table/splitcol/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.createHTML(inputFile1, outDir);
	}
	
	@Test
	/** subscript
	 * No split columns or indents
	 * complex suscripts in Footer - not yet resolved
	 * @throws IOException
	 */
	public void testSuscriptSVG() throws IOException {
//		cm-ucl/corpus-oa-pmr/10.1371_journal.pbio.1000481/pdftable/table1.annot.svg
		File inputFile1 = new File(Fixtures.TABLE_DIR, "suscript/10.1371_journal.pbio.1000481.svg");
		File outDir = new File("target/table/suscript/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile1, outDir);
		PhraseListList phraseListList = new PhraseListList(tableContentCreator.getTableFooter().getOrCreatePhraseLists());
		SuscriptEditor suscriptEditor = new SuscriptEditor(phraseListList);
		suscriptEditor.mergeAll();
		LOG.trace("PLL"+phraseListList);
	}
	
	@Test
	/** subscript
	 * No split columns or indents
	 * isolated superscripts
	 * @throws IOException
	 */
	public void testSuscriptSVG1() throws IOException {
		File inputFile1 = new File(Fixtures.TABLE_DIR, "suscript/10.1007_s00213-015-4198-1.svg");
		File outDir = new File("target/table/suscript/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.markupAndOutputTable(inputFile1, outDir);
		PhraseListList phraseListList = new PhraseListList(tableContentCreator.getTableFooter().getOrCreatePhraseLists());
		LOG.trace(phraseListList.toString());
		Assert.assertEquals(5, phraseListList.size());
		SuscriptEditor suscriptEditor = new SuscriptEditor(phraseListList);
		suscriptEditor.mergeAll();
		LOG.trace("PLL"+phraseListList);
		XMLUtil.debug(phraseListList.toHtml(), new File(outDir, FilenameUtils.getBaseName(inputFile1.toString())+".html"), 1);
	}
	
	
	@Test
	/** subscript
	 * No split columns or indents
	 * @throws IOException
	 */
	public void testSuscriptHTML() throws IOException {
//		cm-ucl/corpus-oa-pmr/10.1371_journal.pbio.1000481/pdftable/table1.annot.svg
		File inputFile1 = new File(Fixtures.TABLE_DIR, "suscript/10.1371_journal.pbio.1000481.annot.svg");
		File outDir = new File("target/table/suscript/");
		TableContentCreator tableContentCreator = new TableContentCreator(); 
		tableContentCreator.createHTML(inputFile1, outDir);
	}
	
	// ===================================

}

