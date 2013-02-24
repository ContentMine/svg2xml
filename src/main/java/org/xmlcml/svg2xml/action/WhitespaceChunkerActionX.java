package org.xmlcml.svg2xml.action;

import java.util.ArrayList;

import java.util.List;

import nu.xom.Node;

import org.apache.log4j.Logger;
import org.xmlcml.svgplus.analyzer.WhitespaceChunkerAnalyzerX;
import org.xmlcml.svgplus.tools.Chunk;
/**
	<pageAction action="createWhitespaceChunks" depth="3"/>
 * @author pm286
 *
 */

public class WhitespaceChunkerActionX extends PageActionX {

	private final static Logger LOG = Logger.getLogger(WhitespaceChunkerActionX.class);
	
	public WhitespaceChunkerActionX(AbstractActionX actionElement) {
		super(actionElement);
	}
	
	
	public final static String TAG ="whitespaceChunker";
	private static final List<String> ATTNAMES = new ArrayList<String>();
	
	static {
		ATTNAMES.add(AbstractActionX.ACTION);
		ATTNAMES.add(PageActionX.DEPTH);
	}

	/** constructor
	 */
	public WhitespaceChunkerActionX() {
		super(TAG);
	}
	
	
    /**
     * copy node .
     *
     * @return Node
     */
    public Node copy() {
        return new WhitespaceChunkerActionX(this);
    }

	/**
	 * @return tag
	 */
	public String getTag() {
		return TAG;
	}

	protected List<String> getAttributeNames() {
		return ATTNAMES;
	}

	protected List<String> getRequiredAttributeNames() {
		return null;
	}
	
	@Override
	public void run() {
		WhitespaceChunkerAnalyzerX whiteSpaceChunkerAnalyzer = getPageEditor().ensureWhiteSpaceChunker();
		Integer depth = getDepth();
		if (depth != null) {
			LOG.trace("DEPTH cannot yet be set");
		}
		List<Chunk> finalChunkList = whiteSpaceChunkerAnalyzer.splitByWhitespace(getSVGPage());
		whiteSpaceChunkerAnalyzer.labelLeafNodes(finalChunkList);
	}

}