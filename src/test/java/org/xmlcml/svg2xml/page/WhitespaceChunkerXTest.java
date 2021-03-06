package org.xmlcml.svg2xml.page;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.svg2xml.Fixtures;
import org.xmlcml.svg2xml.paths.Chunk;

//TODO modify the tests to check for chunk counts once decent chunking algorithm has been written
public class WhitespaceChunkerXTest {

	private final static String WHITESPACE = "target/whitespace";
	
	@Test
	public void testPageSplitter0() throws Exception {
		//SemanticDocumentActionX semanticDocumentAction = SemanticDocumentActionX.createSemanticDocumentActionWithSVGPageFile(Fixtures.PAGE0_SVG);
		//SVGSVG svgPage = semanticDocumentAction.getSVGPage();
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(Fixtures.PAGE0_SVG);
		WhitespaceChunkerAnalyzerX pageChunkSplitterAnalyzer = new WhitespaceChunkerAnalyzerX();
		List<Chunk> finalChunkList = pageChunkSplitterAnalyzer.splitByWhitespace(svgPage);
		Assert.assertNotNull(finalChunkList);
		//Assert.assertEquals("chunks", 15, finalChunkList.size());
		Fixtures.drawChunkBoxes(finalChunkList);
		new File(WHITESPACE).mkdirs();
		SVGUtil.debug(svgPage, new FileOutputStream(WHITESPACE+"/pageSplitter0.svg"), 1);
	}

	@Test
	public void testHarter3() throws Exception {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(Fixtures.HARTER3_SVG);
		WhitespaceChunkerAnalyzerX pageChunkSplitterAnalyzer = new WhitespaceChunkerAnalyzerX();
		List<Chunk> finalChunkList = pageChunkSplitterAnalyzer.splitByWhitespace(svgPage);
		Assert.assertNotNull(finalChunkList);
		//Assert.assertEquals("chunks", 29, finalChunkList.size());
		Fixtures.drawChunkBoxes(finalChunkList);
		new File(WHITESPACE).mkdirs();
		SVGUtil.debug(svgPage, new FileOutputStream(WHITESPACE+"/testHarter3.svg"), 1);
	}

	@Test
	public void testHarter3small() throws Exception {
		
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(Fixtures.HARTER3SMALL_SVG);
		WhitespaceChunkerAnalyzerX pageChunkSplitterAnalyzer = new WhitespaceChunkerAnalyzerX();
		List<Chunk> finalChunkList = pageChunkSplitterAnalyzer.splitByWhitespace(svgPage);
		Assert.assertNotNull(finalChunkList);
		//Assert.assertEquals("chunks", 29, finalChunkList.size());
		Fixtures.drawChunkBoxes(finalChunkList);
		new File(WHITESPACE).mkdirs();
		SVGUtil.debug(svgPage, new FileOutputStream(WHITESPACE+"/testHarter3small.svg"), 1);
	}

	@Test
	public void testPolicies() {
		splitFileAndOutputNoTest(Fixtures.POLICIES_SVG, 7, new File(WHITESPACE+"/policies.svg"));
	}
	
	@Test
	public void testPage6() {
		splitFileAndOutputNoTest(Fixtures.AJC6_SVG, 14, new File(WHITESPACE+"/ajc_page6_split.svg"));
	}
	

	private static void splitFileAndOutputNoTest(File svgFile, int chunkCount, File outputFile) {
		try {
			SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(svgFile);
			List<Chunk> finalChunkList = WhitespaceChunkerAnalyzerX.chunkCreateWhitespaceChunkList(svgPage);
			Assert.assertNotNull(finalChunkList);
			//Assert.assertEquals("chunks", chunkCount, finalChunkList.size());
			Fixtures.drawChunkBoxes(finalChunkList);
			outputFile.getParentFile().mkdirs();
			SVGUtil.debug(svgPage, new FileOutputStream(outputFile), 1);
		} catch (Exception e){
			throw new RuntimeException("Cannot test", e);
		}
	}

}
