package org.xmlcml.svg2xml.text;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;

/** component of a document.
 * Normally a graphics object or text object
 * @author pm286
 *
 */
public class GComponent {
	
	private static final Logger LOG = Logger.getLogger(GComponent.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private LineChunk lineChunk;
	private SVGElement svgElement;

	public GComponent(LineChunk lineChunk) {
		this.lineChunk = lineChunk;
	}

	public GComponent(SVGElement svgElement) {
		this.svgElement = svgElement;
	}

}
