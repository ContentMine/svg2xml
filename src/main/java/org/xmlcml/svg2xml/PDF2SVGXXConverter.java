package org.xmlcml.svg2xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGSVG;

/** dummy class to enable new PDF2SVG to be slotted in.
 * 
 * @author pm286
 *
 */
public class PDF2SVGXXConverter {
	private static final Logger LOG = Logger.getLogger(PDF2SVGXXConverter.class);
	
	static {
		LOG.setLevel(Level.DEBUG);
	}

	public PDF2SVGXXConverter() {
//		LOG.warn("PDFConverter shorted out");
	}

	public void run(String ...string) {
		LOG.warn("PDFConverter.run() shorted out");
//		PDF2SVGConverter converter = new PDF2SVGConverter();
	}

	public List<SVGSVG> getPageList() {
		LOG.warn("PDF2SVGConverter skipped");
		// TODO Auto-generated method stub
		return null;
	}

	public void createSVGFromPDF(File pdfOrigDir, File projectDir) {
		projectDir.mkdirs();
		File[] pdfDirs = pdfOrigDir.listFiles();
		for (File pdfDir : pdfDirs) {
			File projectPdfDir = new File(projectDir, pdfDir.getName());
			projectPdfDir.mkdirs();
			File pdfFile = new File(pdfDir, "fulltext.pdf");
			File svgDir = new File(projectPdfDir, "svg/");
			svgDir.mkdirs();
			
			run("-logger", "-infofiles", "-logglyphs", "-outdir", svgDir.toString(), pdfFile.toString());
			
			File pngDir = new File(projectPdfDir, "png/");
			List<File> pngs = new ArrayList<File>(FileUtils.listFiles(svgDir, new String[] {"png"}, false));
			for (File png : pngs) {
				if (FileUtils.sizeOf(png) == 0) {
					png.delete();
				} else {
					try {
						FileUtils.moveToDirectory(png, pngDir, true);
					} catch (Exception e) {
						LOG.error("cannot move file: "+e);
					}
				}
			}
		}
	}
}
