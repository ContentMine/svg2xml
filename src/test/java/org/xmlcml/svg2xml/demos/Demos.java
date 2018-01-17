package org.xmlcml.svg2xml.demos;

import org.xmlcml.svg2xml.pdf.PDFAnalyzerTestOLD;

public class Demos {

	public static void main(String[] args) {
//			ebola1();
//			astro1();
//			plot1();
			PDFAnalyzerTestOLD.analyzePDF("demos/gandhi/sample.pdf"); 

	}

	private static void ebola1() {
		PDFAnalyzerTestOLD.analyzePDF(("demos/ebola/roadmapsitrep_12Nov2014_eng.pdf")); 
//		PDFAnalyzerTest.analyzePDF(("demos/ebola/roadmapsitrep_14Nov2014_eng.pdf")); 
	}
	
	private static void astro1() {
		PDFAnalyzerTestOLD.analyzePDF(("demos/astro/0004-637X_778_1_1.pdf")); 
	}
	
	private static void plot1() {
		PDFAnalyzerTestOLD.analyzePDF(("demos/plot/22649_Sada_2012-1.pdf")); 
//		analyzePDFFile
	}
	
}
