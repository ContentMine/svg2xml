package org.xmlcml.svg2xml.semantic;

import java.util.Set;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.svg2xml.analyzer.AbstractAnalyzer;
import org.xmlcml.svg2xml.analyzer.ChunkId;
import org.xmlcml.svg2xml.analyzer.PDFIndex;

/**
 * @author pm286
 *
 */
public class DOIAnalyzer extends AbstractAnalyzer {
	private static final Logger LOG = Logger.getLogger(DOIAnalyzer.class);
	public static final Pattern PATTERN = Pattern.compile(".*[Dd][Oo][Ii][\\s\\d\\;\\/\\-]+.*", Pattern.DOTALL);
	public final static String TITLE = "DOI";
	
	public DOIAnalyzer(PDFIndex pdfIndex) {
		super(pdfIndex);
	}
	
	public void analyze() {
	}
	
	@Override
	public SVGG oldAnnotateChunk() {
		throw new RuntimeException("annotate NYI");
	}
	
	public Integer indexAndLabelChunk(String content, ChunkId id) {
		Integer serial = super.indexAndLabelChunk(content, id);
		// index...
		return serial;
	}
	
	/** Pattern for the content for this analyzer
	 * 
	 * @return pattern (default null)
	 */
	@Override
	protected Pattern getPattern() {
		return PATTERN;
	}

	/** (constant) title for this analyzer
	 * 
	 * @return title (default null)
	 */
	@Override
	public String getTitle() {
		return TITLE;
	}

}
