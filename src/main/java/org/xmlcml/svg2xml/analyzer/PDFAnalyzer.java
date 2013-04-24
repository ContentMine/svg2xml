package org.xmlcml.svg2xml.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Nodes;
import nu.xom.Text;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGTitle;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlMenuSystem;
import org.xmlcml.html.HtmlUl;
import org.xmlcml.pdf2svg.PDF2SVGConverter;
import org.xmlcml.svg2xml.action.SVGPlusConstantsX;
import org.xmlcml.svg2xml.action.SemanticDocumentActionX;
import org.xmlcml.svg2xml.tools.Chunk;
import org.xmlcml.svg2xml.util.NameComparator;

import com.google.common.collect.Multimap;


public class PDFAnalyzer implements Annotatable {


	private static final String Z_CHUNK = "z_";

	private final static Logger LOG = Logger.getLogger(PDFAnalyzer.class);

	private static final String SVG = SVGPlusConstantsX.DOT_SVG;
	private static final String PDF = SVGPlusConstantsX.DOT_PDF;

	public static final String PAGE = "page";


	private File inputTopDir;
	private File inFile;
	private String inputName;
	private String fileRoot;
	private File svgTopDir = new File("target/svg");
	private File svgDocumentDir;
	private File svgPageFile;
	private File outputTopDir = new File("target/output");
	private File outputDocumentDir;
	private int pageNumber;
	private boolean skipFile;

	private DocumentListAnalyzer documentListAnalyzer;
	private PDFIndex pdfIndex;

	private List<SVGSVG> svgOutList;
	private List<SVGG> gOutList;

	public PDFAnalyzer() {
	}

	public PDFAnalyzer(DocumentListAnalyzer documentListAnalyzer) {
		this.documentListAnalyzer = documentListAnalyzer;
	}

	public void setInputTopDir(File inDir) {
		this.inputTopDir = inDir;
	}
	
	public void setSVGTopDir(File svgDir) {
		this.svgTopDir = svgDir;
	}
	
	public void setOutputTopDir(File outDir) {
		this.outputTopDir = outDir;
	}
	
	public void setFileRoot(String fileRoot) {
		this.fileRoot = fileRoot;
	}
	
	public void setSkipFile(boolean skipFile) {
		this.skipFile = skipFile;
	}
	
	public void analyzePDFFile(File inFile) {
		this.inFile = inFile;
		inputName = inFile.getName();
		fileRoot = inputName.substring(0, inputName.length() - PDF.length());
		svgDocumentDir = new File(svgTopDir, fileRoot);
		outputDocumentDir = new File(outputTopDir, fileRoot);
		analyzePDF();
		File htmlDir = (new File(outputTopDir, fileRoot));
		copyOriginalPDF(inFile, htmlDir);
		
		extractEntities(htmlDir);
		createHtmlMenuSystem(htmlDir);
	}

	private void extractEntities(File htmlDir) {
		List<File> htmlFiles = analyzeHtml(htmlDir);
		SpeciesAnalyzer speciesAnalyzer = new SpeciesAnalyzer(pdfIndex);
		HtmlUl speciesList = speciesAnalyzer.extractEntities(htmlFiles);
		try {
			CMLUtil.debug(speciesList, new FileOutputStream(new File(outputDocumentDir, speciesAnalyzer.getFileName())), 1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void copyOriginalPDF(File inFile, File htmlDir) {
		try {
			IOUtils.copy(new FileInputStream(inFile), new FileOutputStream(new File(htmlDir, "00_"+inputName)));
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}

	private List<File> analyzeHtml(File htmlDir) {
		List<File> htmlFileList = new ArrayList<File>();
		File[] files = htmlDir.listFiles();
		for (File file : files) {
			if (file.toString().endsWith(SVGPlusConstantsX.DOT_HTML)) {
				htmlFileList.add(file);
			}
		}
		return htmlFileList;
	}

	public  void analyzePDF() {
		createSVGfromPDF();
		File[] files = svgDocumentDir.listFiles();
		LOG.debug("listing Files in: "+svgDocumentDir);
		if (files == null) {
			throw new RuntimeException("No files in "+svgDocumentDir);
		}

		ensurePDFIndex();
		pdfIndex.ensureElementMultimaps();
		for (int page = 0; page < files.length; page++) {
			System.out.print(page+"~");
			createAndAnalyzeSVGChunks(page);
		}
		System.out.println();
		LOG.debug("IDS: "+pdfIndex.getUsedIdSet());
		pdfIndex.createIndexes();
		pdfIndex.AnalyzeDuplicates();
		writeSvgPages(files);
		pdfIndex.getHtmlElementsSortedByChunkId();
		pdfIndex.RemoveDuplicates();
		pdfIndex.createLinkedElementList();
		pdfIndex.mergeCaptions();
		pdfIndex.mergeHtml();
		pdfIndex.outputHtmlElements();
		// TODO Auto-generated method stub
		

	}

	private void writeSvgPages(File[] files) {
		for (int page = 0; page < files.length; page++) {
			SVGSVG svgPage = svgOutList.get(page);
			annotatePage(svgPage);
			writeSVGPage(page, svgPage);
		}
	}

	private void annotatePage(SVGSVG svgPage) {
		List<SVGG> gList = SVGG.extractGs(SVGUtil.getQuerySVGElements(svgPage, ".//svg:g[@id]"));
		for (SVGG g : gList) {
			ChunkId chunkId = new ChunkId(g.getId());
			boolean indexed = pdfIndex.getUsedIdSet().contains(chunkId);
			LOG.trace("ID written "+chunkId+" "+indexed);
			if (indexed) {
				Real2Range bbox = g.getBoundingBox();
				Real2[] corners = bbox.getCorners();
				SVGLine line = new SVGLine(corners[0], corners[1]);
				line.setOpacity(0.3);
				line.setWidth(5.0);
				line.setFill("green");
				g.appendChild(line);
			}
		}
	}

	public void createSVGfromPDF() {
		LOG.trace("createSVG");
		PDF2SVGConverter converter = new PDF2SVGConverter();
		if (!inFile.exists()) {
			throw new RuntimeException("no input file: "+inFile);
		}
//		File svgDocumentDir = new File(svgTopDir, fileRoot);
		boolean exists = svgDocumentDir.exists();
		File[] files = (svgDocumentDir == null) ? null : svgDocumentDir.listFiles();
		if (!exists || files == null || files.length == 0) {
			svgDocumentDir.mkdirs();
			LOG.debug("running "+inFile.toString()+" to "+svgDocumentDir.toString());
			converter.run("-outdir", svgDocumentDir.toString(), inFile.toString() );
		} else {
			LOG.debug("Skipping SVG");
		}
	}

	private void createAndAnalyzeSVGChunks(int pageNumber) {
		ensurePDFIndex();
		this.pageNumber = pageNumber;
		String pageRoot = createPageRoot(pageNumber);
		String pageSvg = fileRoot+"-"+pageRoot+SVG;
		svgPageFile = new File(svgDocumentDir, pageSvg);
		if (svgPageFile.exists() && skipFile) {
			LOG.debug("Skipping: "+svgPageFile);
			return;
		}
		SVGSVG svg = (SVGSVG) SVGElement.readAndCreateSVG(svgPageFile);
//		stripNewlines(svg);
		processNonUnicodeCharacters(svg);
		SemanticDocumentActionX semanticDocumentAction = 
				SemanticDocumentActionX.createSemanticDocumentActionWithSVGPage(svg);
		List<Chunk> chunkList = 
				WhitespaceChunkerAnalyzerX.chunkCreateWhitespaceChunkList(semanticDocumentAction);
		WhitespaceChunkerAnalyzerX.drawBoxes(chunkList, "red", "yellow", 0.5);
		List<SVGElement> gList = SVGG.generateElementList(svg, "svg:g/svg:g/svg:g[@edge='YMIN']");
		
		SVGSVG svgOut = createSVGOut(pageNumber);
		svgOutList.add(svgOut);
		for (int ichunk = 0; ichunk < gList.size(); ichunk++) {
			SVGG gOrig = (SVGG) gList.get(ichunk);
			SVGG gOut = copyChunkAnalyzeMakeId(pageNumber, gOrig, ichunk);
			ensureGOutList();
			svgOut.appendChild(gOut);
			pdfIndex.addToindexes(gOut);
		}
	}

	private void ensureGOutList() {
		if (gOutList == null) {
			gOutList = new ArrayList<SVGG>();
		}
	}

	private String createPageRoot(int pageNumber) {
		String pageRoot = PAGE+(pageNumber+1);
		return pageRoot;
	}

	private SVGSVG createSVGOut(int pageNumber) {
		ensureSVGOutList();
		SVGSVG svgOut = new SVGSVG();
		svgOut.setWidth(600.0);
		svgOut.setHeight(800.0);
		String pageId = "p."+pageNumber;
		svgOut.setId(pageId);
		return svgOut;
	}

	private void ensureSVGOutList() {
		if (svgOutList == null) {
			svgOutList = new ArrayList<SVGSVG>();
		}
	}

	/**
	 * <title stroke="black" stroke-width="1.0">char: 981; name: null; f: Symbol; fn: PHHOAK+Symbol; e: Dictionary</title>
	 * @param svg
	 */
	private void processNonUnicodeCharacters(SVGSVG svg) {
		List<SVGElement> textTitles = SVGUtil.getQuerySVGElements(svg, ".//svg:title");
		for (SVGElement t : textTitles) {
			SVGTitle title = (SVGTitle) t;
			String s = title.getValue();
			String[] chunks =s.split(";");
			Integer ss = null;
			for (String chunk : chunks) {
				String[] sss = chunk.split(":");
				if (sss[0].equals("char") && !sss[1].equals("null")) {
					ss = new Integer(sss[1].trim());
					break;
				}
				if (sss[0].equals("name") && !sss[1].equals("null")) {
//					ss = sss[1];
					ss = 127;
					break;
				}
			}
			SVGElement text = ((SVGElement)title.getParent());
			int cc =text.getChildCount();
			for (int i = 0; i < cc; i++) {
				text.getChild(0).detach();
			}
			char c =  (char)(int)ss;
			LOG.trace("> "+c);
			text.appendChild(""+c);
//			text.debug("XX");
		}
	}

	private void stripNewlines(SVGSVG svg) {
		Nodes texts = svg.query("//text()");
		for (int i = 0; i < texts.size(); i++) {
			Text text = (Text) texts.get(i);
			String value = text.getValue();
			if (value.contains("\n")) {
				value = value.replaceAll("\n", "");
				text.setValue(value);
			}
		}
	}

	private void ensurePDFIndex() {
		if (pdfIndex == null) {
			pdfIndex = new PDFIndex(this);
		}
	}

	private void writeSVGPage(int pageNumber, SVGSVG svgOut) {
		try {
			String pageRoot = createPageRoot(pageNumber);
			outputDocumentDir.mkdirs();
			String id = svgOut.getId();
			LOG.trace("ID "+id);
			if (pdfIndex.getUsedIdSet().contains(id)) {
				LOG.debug("ANNOTATED: "+id);
			}
			CMLUtil.debug(
				svgOut, new FileOutputStream(new File(outputDocumentDir, pageRoot+SVG)), 1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private SVGG copyChunkAnalyzeMakeId(int pageNumber, SVGG gOrig, int ichunk) {
		ChunkId chunkId = new ChunkId(pageNumber+1, ichunk);
		SVGG gOut = analyzeChunkInSVGPage(gOrig, chunkId);
		CMLUtil.copyAttributes(gOrig, gOut);
		return gOut;
	}

	public static List<List<String>> findDuplicates(String title, Multimap<? extends Object, String> map) {
		List<List<String>> duplicateList = new ArrayList<List<String>>();
		for (Map.Entry<? extends Object, Collection<String>> mapEntry : map.asMap().entrySet()) {
			Object key = mapEntry.getKey();
			Collection<String> ids = mapEntry.getValue();
			List<String> idList = (Arrays.asList(ids.toArray(new String[0])));
			Collections.sort(idList);
			if (idList.size() > 1) {
				LOG.debug("DUPLICATES: "+title+" >"+key+"< "+idList);
				duplicateList.add(idList);
			}
		}
		return duplicateList;
	}
		

	public SVGG labelChunk() {
		// might iterate through pages
		throw new RuntimeException("NYI");
	}
	
	public SVGG analyzeChunkInSVGPage(SVGElement chunkSvg, ChunkId chunkId) {
		SVGG gOut = null;
		AbstractPageAnalyzerX analyzerX = AbstractPageAnalyzerX.getAnalyzer(chunkSvg);
		TextAnalyzerX textAnalyzer = null;
		String message = null;
		HtmlElement htmlElement = new HtmlDiv();
		gOut = analyzerX.labelChunk();
		gOut.setId(chunkId.toString());
		htmlElement = analyzerX.createHTML();
		if (htmlElement != null) {
			pdfIndex.addHtmlElement(htmlElement, chunkId);
			pdfIndex.indexHtmlBySvgId(htmlElement, chunkId);
		} else {
			LOG.warn("no html from: "+analyzerX);
			if (analyzerX instanceof TextAnalyzerX) {
				((TextAnalyzerX)analyzerX).debug();
			}
		}
		return gOut;
	}

	private void createHtmlMenuSystem(File dir) {
		HtmlMenuSystem menuSystem = new HtmlMenuSystem();
		menuSystem.setOutdir(dir.toString());
		File[] filesh = dir.listFiles();
		Arrays.sort(filesh, new NameComparator());
		for (File filex : filesh) {
			menuSystem.addHRef(filex.toString());
		}
		try {
			menuSystem.outputMenuAndBottomAndIndexFrame();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void outputElementAsHTML(HtmlElement htmlElement) {
		ChunkId chunkId = new ChunkId(htmlElement.getId());
		try {
			outputDocumentDir.mkdirs();
			String chunkType = htmlElement.getAttributeValue(PDFIndex.CHUNK_TYPE);
			if (chunkType == null) {
				chunkType = Z_CHUNK;
			}
			String chunkFileRoot = chunkType+chunkId.getPageNumber()+"-"+chunkId.getChunkNumber();
			File outfile = new File(outputDocumentDir, chunkFileRoot+SVGPlusConstantsX.DOT_HTML);
			LOG.trace("writing "+outfile);
			OutputStream os = new FileOutputStream(outfile);
			CMLUtil.debug(htmlElement, os, 1);
			os.close();
		} catch (Exception e) {
			throw new RuntimeException("cannot write HTML: ",e);
		}
	}

	public PDFIndex getIndex() {
		ensurePDFIndex();
		return pdfIndex;
	}

	/**
	mvn exec:java -Dexec.mainClass="org.xmlcml.svg2xml.analyzer.PDFAnalyzer" 
	    -Dexec.args="src/test/resources/pdfs/bmc"
		 * @param args
		 */
		public static void main(String[] args) {
			if (args.length == 0) {
				System.out.println("PDFAnalyzer <directory>");
				System.out.println("mvn exec:java -Dexec.mainClass=\"org.xmlcml.svg2xml.analyzer.PDFAnalyzer\" " +
						" -Dexec.args=\"src/test/resources/pdfs/bmc/1471-2180-11-174.pdf\"");
				System.out.println("OR java org.xmlcml.svg2xml.analyzer.PDFAnalyzer src/test/resources/pdfs/bmc/1471-2180-11-174.pdf");
				System.exit(0);
			} else {
				PDFAnalyzer analyzer = new PDFAnalyzer();
				analyzer.analyzePDFFile(new File(args[0]));
			}
		}

}
