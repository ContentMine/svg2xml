package org.xmlcml.svgplus.core;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.svgplus.Fixtures;
import org.xmlcml.svgplus.TestUtils;

public class SemanticDocumentActionTest {

	private final static Logger LOG = Logger.getLogger(SemanticDocumentActionTest.class);
	@Test
	public void testSetVariable() {
		SemanticDocumentAction semanticDocumentAction = new SemanticDocumentAction();
		semanticDocumentAction.setVariable("s.fooName", "barValue");
		Object value = semanticDocumentAction.getVariable("s.fooName");
		Assert.assertNotNull("value should not be null", value);
		Assert.assertTrue("class", value instanceof String);
		Assert.assertEquals("class", "barValue", value);
	}
	
	@Test
	public void testSetVariableWithIncorrectName() {
		SemanticDocumentAction semanticDocumentAction = new SemanticDocumentAction();
		semanticDocumentAction.setVariable("fooName", "barValue");
		Object value = semanticDocumentAction.getVariable("fooName");
		Assert.assertNull("value should be null", value);
	}
	
	@Test
	public void testResetVariable() {
		SemanticDocumentAction semanticDocumentAction = new SemanticDocumentAction();
		semanticDocumentAction.setVariable("d.fooName", "barValue1");
		semanticDocumentAction.setVariable("d.fooName", "barValue2");
		Object value = semanticDocumentAction.getVariable("d.fooName");
		Assert.assertNotNull("value should not be null", value);
		Assert.assertEquals("class", "barValue2", value);
	}
	
	@Test
	/** read and set variables
	 */
	//@Ignore // FIXME semanticDocumentAction null
	public void testVariableMap() {
		SVGPlusConverter converter = new SVGPlusConverter();
		converter.run(
				" -c "+Fixtures.NOOP_FILE+
				" -i "+Fixtures.AJC_PAGE6_PDF
				);
		List<String> variableNames = converter.getSemanticDocumentAction().getVariableNames();
		Assert.assertNotNull("value should not be null", variableNames);
	}

	@Test
	/** read and set variables
	 * read a PDF because current logic requires there to be one
	 */
	//@Ignore // FIXME semanticDocumentAction null
	public void testInjectVariable() {
		SVGPlusConverter converter = new SVGPlusConverter();
		converter.run(
				" -c "+ Fixtures.NOOP_FILE +
				" -i "+ Fixtures.AJC_PAGE6_PDF +
			    " -d.dummy dummyValue"
				);
		SemanticDocumentAction semanticDocumentAction = converter.getSemanticDocumentAction();
		List<String> variableNames = semanticDocumentAction.getVariableNames();
		Assert.assertNotNull(variableNames);
		for (String name : variableNames) {
			LOG.debug("name: "+name);
		}
		Assert.assertEquals("var names", 3, variableNames.size());
		Assert.assertTrue("sem doc", TestUtils.fileEquals(Fixtures.NOOP_FILE, 
				(String) semanticDocumentAction.getVariable(SemanticDocumentAction.S_SEMDOC)));
		Object fileObj = semanticDocumentAction.getVariable(SemanticDocumentAction.S_INFILE);
		Assert.assertNotNull("file ", fileObj);
		Assert.assertTrue("file ", fileObj instanceof File);
		Assert.assertEquals("input file", new File(Fixtures.AJC_PAGE6_PDF).getPath(), 
				((File) fileObj).getPath());
		Assert.assertNull("output file",  
				semanticDocumentAction.getVariable(SemanticDocumentAction.S_OUTFILE));
		Assert.assertEquals("dummy", "dummyValue", 
				semanticDocumentAction.getVariable("d.dummy"));
		Assert.assertNull("null",  
				semanticDocumentAction.getVariable(null));
	}
}
