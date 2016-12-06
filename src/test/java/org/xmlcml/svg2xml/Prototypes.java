package org.xmlcml.svg2xml;

import java.io.File;

import org.xmlcml.pdf2svg.PDF2SVGConverter;
import org.xmlcml.svg2xml.pdf.PDFAnalyzerTest;

public class Prototypes {

	public static void main(String[] args) {
//		carnosic();
//		funnel();
		clinical();
	}
	
	private static void carnosic() {
//		new PDF2SVGConverter().run("-logger", "-infofiles", "-logglyphs", "-outdir", "target/phytochem", "src/test/resources/elsevier/carnosic.pdf"
//		new SVG2XMLConverter().run("-logger", "-infofiles", "-logglyphs", "-outdir", "target/phytochem", "src/test/resources/elsevier/carnosic.pdf"
//		PDFAnalyzerTest.analyzePDF("src/test/resources/pdfs/els/1-s2.0-S105579031300//		PDFAnalyzerTest.analyzePDF("demos/clinical/1917-main.pdf");
		PDFAnalyzerTest.analyzePDF("src/test/resources/pdfs/els/carnosic.pdf");
	
	}
	
	private static void funnel() {
//		new PDF2SVGConverter().run("-logger", "-infofiles", "-logglyphs", "-outdir", "target/funnel", "../pdf2svg/demos/sage/Sbarra-454-74.pdf");
		PDFAnalyzerTest.analyzePDF("../pdf2svg/demos/sage/Sbarra-454-74.pdf");
	
	}

	private static void clinical() {
//		new PDF2SVGConverter().run("-logger", "-infofiles", "-logglyphs", "-outdir", "target/funnel", "../pdf2svg/demos/sage/Sbarra-454-74.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/ACR65481.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/ADA_PH1.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/AHA_PH2.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/AMA_Dobson2013_1.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/BLK_JPR52758.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/BLK_SAM55371.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/BMC73226.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/ELS_Petaja2009.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/EVJ62903.pdf");               // no SVG extracted
//		PDFAnalyzerTest.analyzePDF("demos/clinical/LB_HV_Romanowski2011_1.pdf"); // no SVG extracted
//		PDFAnalyzerTest.analyzePDF("demos/clinical/LPW_Reisinger2007.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/LWW61463.pdf");               // no SVG extracted
//		PDFAnalyzerTest.analyzePDF("demos/clinical/OUP_PH3.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/PLOS57170.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/SPR57530.pdf");               // no tables extracted
//		PDFAnalyzerTest.analyzePDF("demos/clinical/SPR68755.pdf");
//		PDFAnalyzerTest.analyzePDF("demos/clinical/WK_Vesikari2015.pdf");        // no SVG extracted
//		PDFAnalyzerTest.analyzePDF("demos/clinical/Wiley44386.pdf");
	}

}
