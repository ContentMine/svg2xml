package org.xmlcml.svg2xml.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.svg2xml.analyzer.PageAnalyzer;
import org.xmlcml.svg2xml.text.ScriptLine;
import org.xmlcml.svg2xml.text.ScriptWord;
import org.xmlcml.svg2xml.text.StyleSpan;
import org.xmlcml.svg2xml.text.StyleSpans;
import org.xmlcml.svg2xml.text.TextFixtures;
import org.xmlcml.svg2xml.text.TextLine;
import org.xmlcml.svg2xml.text.TextStructurer;

public class ScriptContainerTest {

	public final static Logger LOG = Logger.getLogger(ScriptContainerTest.class);
	
	@Test
	public void test3WordContainer() {
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(TextFixtures.BMC_312_6_1SA_SVG);
		Assert.assertEquals("1a", 
				"TextStructurer: 1chars: 9 Y: 39.615 fontSize: 7.97 physicalStyle: null >>Page6of14\n",
				textContainer.toString());
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(TextFixtures.BMC_312_6_1SA_SVG);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		Assert.assertEquals("1a", "Page6of14", sc.getRawValue());
	}
	
	
	@Test
	public void test4WordContainerScriptList() {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(TextFixtures.BMC_312_6_1SA_SVG);
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(TextFixtures.BMC_312_6_1SA_SVG);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		List<ScriptLine> scriptList = sc.getScriptLineList();
		Assert.assertEquals("scriptLines", 1, scriptList.size());
		Assert.assertEquals("line0", "Page6of14  %%%%\n", scriptList.get(0).toString());
	}

	@Test
	public void testGet4Words() {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(TextFixtures.BMC_312_6_1SA_SVG);
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(TextFixtures.BMC_312_6_1SA_SVG);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		List<ScriptLine> scriptLineList = sc.getScriptLineList();
		ScriptLine scriptLine = scriptLineList.get(0);
		List<ScriptWord> scriptWords = scriptLine.getWords();
		Assert.assertEquals("line0", 4, scriptWords.size());
		String[] value ={"Page", "6", "of", "14"};
		for (int i = 0; i < scriptWords.size(); i++) {
			Assert.assertEquals(""+i, value[i], scriptWords.get(i).getRawValue());
		}
	}

	@Test
	public void testGetTitle() {
		testScript(TextFixtures.BMC_312_6_0SA_SVG, new String[][] {
				{"Hiwatashi", "et", "al.", "BMC", "Evolutionary", "Biology", "2011,", "11:312"},
				{"http://www.biomedcentral.com/1471-2148/11/312", },
				});
	}

	/** this is not right - shouldn't split after slash */
	@Test
	public void testBadSlash() {
		testScript(TextFixtures.BMC_312_6_0SA1_SVG, new String[][] {
				{"http://www.biomedcentral.com/1471-2148/11/312" }
				});
	}

	@Test
	public void testGetShortPara() {
		testScript(TextFixtures.BMC_312_6_3SA_SVG, new String[][] {
				{"genes", "in", "the", "exons", "and", "introns", "in", "these", "individuals", "was"},
				{"essentially", "the", "same", "as", "the", "pattern", "shown", "in", "Figure", "1."}
		});
	}

	@Test
	public void testGetShortHeading0() {
		testScript(TextFixtures.BMC_312_6_4SA_SVG, new String[][] {
				{"Nucleotide", "diversity", "of", "L", "and", "M", "opsin", "genes", "within"},
				{"species"}
		}
		);
	}

	@Test
	/** {} means skip checking that line
	 * 
	 */
	public void testGetLargePara() {
		testScript(TextFixtures.BMC_312_6_4SB_SVG, new String[][] {
				{"Figure", "2", "summarizes", "the", "nucleotide", "diversity", "of", "the", "L"},
				{"and", "M", "opsin", "exons", "and", "introns", "and", "of", "the", "neutral", "refer-"},
				{"ences", "(see", "Additional", "file", "1,", "Tables", "S4", "and", "S5", "for", "the"},
				{},				{},				{},
				{},				{},				{},				{},				{},
				{},				{},				{},				{},				{},
				{},				{},				{},				{},				{},
				{},				{},				{},				{},				{},
				{},				{},				{},				{},				{},
				{},				{},				{},				{},				{},
		}
		);
	}

	@Test
	public void testGetLargePara3() {
		testScript(TextFixtures.BMC_312_6_4SB3_SVG, new String[][] {
				{"ences", "(see", "Additional", "file", "1,", "Tables", "S4", "and", "S5", "for", "the"},
		}
		);
	}
	
	@Test
	public void testGetSpans0() {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(TextFixtures.BMC_312_6_0SA0_SVG);
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(TextFixtures.BMC_312_6_0SA0_SVG);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		List<StyleSpans> styleSpansList = sc.getStyleSpansList();
		Assert.assertEquals("lists", 1, styleSpansList.size());
		Assert.assertEquals("lists0", 7, styleSpansList.get(0).size());
		Assert.assertEquals("lists0.0", "Hiwatashi ", styleSpansList.get(0).get(0).toString());
	}

	@Test
	public void testGetSpans() {
		String[][] values ={
				{"Hiwatashi ", "<I>et al</I>", ". ", "<I>BMC Evolutionary Biology </I>", "2011, ", "<B>11</B>", ":312"}, 
				{"http://www.biomedcentral.com/1471-2148/11/312"}
		};
		testSpans(values, TextFixtures.BMC_312_6_0SA_SVG);
	}

	@Test
	// the apostrophes are created in a bold font!
	public void testGetParaSpans() {
		String[][] values ={
				{"Blood samples were collected from a total of 157 indivi-"},
				{"duals of the following species: Agile (", "<I>Hylobates agilis</I>", ";"},
				{"N = 37), Kloss", "<B>’</B>", "(", "<I>H. klossii</I>", "; N = 2), White-handed (", "<I>H. lar</I>", ";"},
				{"N = 40), Silvery Javan (", "<I>H. moloch</I>", "; N = 6), Mueller", "<B>’</B>", "s Bor-"},
				{"nean gray (","<I>H. muelleri</I>","; N = 6), Pileated (","<I>H. pileatus</I>","; N ="},
				{"19), Chinese White-cheeked (","<I>Nomascusleucogenys</I>","; N ="},
				{"16) and Siamang (","<I>Symphalangus syndactylus</I>","; N = 31)."},
				{"Sampling was conducted at the Ragunan Zoo and the"},
				{"Pontianak Zoo in Indonesia, and the Chiang M ai Zoo,"},  //error
				{"the Bangkok Zoo and the Khao Kheow Open Zoo in"},
				{"Thailand. We also sampled gibbons reared by local resi-"},
				{"dents in Kalimantan, Indonesia. Genomic DNA was"},
				{"extracted from blood samples using the DNA Microex-"}, 
				{"traction Kit (Stratagene, Santa Clara, CA) or the QIAamp"},
				{"DNA Blood M ini Kit (Qiagen, Duesseldorf, Germany)."},     // error
				{"Research permissions were granted by each country and"},
				{"sampling was conducted according to the Guide for the"},
				{"Care and Use of Laboratory Animals by the National"},
				{"Institute of Health, U.S.A. (1985) and the Guide for the"},
				{"Care and Use of Laboratory Primates by the Primate"},
				{"Research Institute, Kyoto University (1986, 2002). All"},
				{"procedures were approved by the animal ethics commit-"},
				{"tee of the Primate Research Institute, Kyoto University."},
				{"Among the 157 individuals, 152 were subjected to the"},
				{"genotyping of the L/ M opsin genes (Additional file 1,"},
				{"Table S1). The remaining 5 individuals (two ", "<I>H. agilis</I>", ","},
				{"one ", "<I>H. lar</I>", ", and two ", "<I>S. syndactylus</I>", ") were included in the"},
				{"analysis of the neutral reference genes. Among the 152"},
				{"individuals, 94 were subjected to DNA sequencing of the"},
				{"entire 3.6~3.9-kb region encompassing exon 3 to exon 5"},
				{"(Table 1)."},
		};
		File file = TextFixtures.BMC_312_2_4SC_SVG;
		testSpans(values, file);
	}

	@Test
	public void testGetHeaders() {
		String[][] values ={
				{"<B>Nucleotide diversity of L and M opsin genes within</B>"},
				{"<B>species</B>"},
		};
		File file = TextFixtures.BMC_312_6_4SA_SVG;
		testSpans(values, file);
	}
	
	@Test
	// note this has some "bold" quotation marks
	public void testGetReferences() {
		String[][] values ={
	{"1. NathansJ, ThomasD, Hogness DS: ", "Molecular genetics of human color"},
	{"vision: the genes encoding blue, green, and red pigments. ", "<I>Science </I>", "1986,"},
	{"232", ":193-202."},
	{"2. JacobsGH: ", "Primate photopigments and primate color vision. ", "<I>Proc Natl</I>"},
	{"<I>Acad Sci USA </I>", "1996, ", "93", ":577-581."},
	{"3. Yokoyama R, Yokoyama S: ", "Convergent evolution of the red- and green-"},
	{"like visual pigment genes in fish, ", "<I>Astyanax fasciatus</I>", ", and human. ", "<I>Proc</I>"},
	{"<I>Natl Acad Sci USA </I>", "1990, ", "87", ":9315-9318."},
	{"4. Neitz M, Neitz J, Jacobs GH: ", "Spectral tuning of pigments underlying red-"},
	{"green color vision. ", "<I>Science </I>", "1991, ", "252", ":971-974."},
	{"5. Asenjo AB, Rim J, Oprian DD: ", "Molecular determinants of human red/"},
	{"green color discrimination. ", "<I>Neuron </I>", "1994, ", "12", ":1131-1138."},
	{"6. Yokoyama S, Radlwimmer FB: ", "The ", "<B>“</B>", "five-sites", "<B>”</B>", "rule and the evolution of"},
	{"red and green color vision in mammals. ", "<I>Mol Biol Evol </I>", "1998, ", "15", ":560-567."},
	{"7. Yokoyama S, Radlwimmer FB: ", "The molecular genetics of red and green"},
	{"color vision in mammals. ", "<I>Genetics</I>", "1999, ", "153", ":919-932."},
	{"8. Yokoyama S, Radlwimmer FB: ", "The molecular genetics and evolution of"},
	{"red and green color vision in vertebrates. ", "<I>Genetics</I>", "2001, ", "158", ":1697-1710."},
	{"9. Chan T, Lee M, Sakmar TP: ", "Introduction of hydroxyl-bearing amino acids"},
	{"causes bathochromic spectral shifts in rhodopsin. Amino acid"},
		};
		File file = TextFixtures.BMC_312_12_7SB_SVG;
		testSpans(values, file);
	}

	@Test
	public void testGetSuScripts() {
		String[][] values ={
	{"Tomohide Hiwatashi", "1", ", Akichika Mikami", "2,8", ", Takafumi Katsumura", "1", ", Bambang Suryobroto", "3", ","},
	{"Dyah Perwitasari-Farajallah", "3,4", ", Suchinda Malaivijitnond", "5", ", Boripat Siriaroonrat", "6", ", Hiroki Oota", "1,9", ", Shunji Goto", "7,10 ", "and"},
	{"Shoji Kawamura", "1*"},
		};
		File file = TextFixtures.BMC_312_1_4SA_SVG;
		testSpans(values, file);
	}
	
	@Test
	public void testCorrespondence() {
		String[][] values ={
	{"* Correspondence: kawamura@k.u-tokyo.ac.jp",},
	{"1", "Department of Integrated Biosciences, Graduate School of Frontier Sciences,"},
	{"The University of Tokyo, Kashiwa 277-8562, Japan", },
	{"Full list of author information is available at the end of the article"},
		};
		File file = TextFixtures.BMC_312_1_7DA_SVG;
		testSpans(values, file);
	}
	
	@Test
	public void testLicence() {
		String[][] values ={
	{"© 2011 Hiwatashi et al; licensee BioMed Central Ltd. This is an Open Access article distributed under the terms of the Creative",},
	{"Commons Attribution License (http://creativecommons.org/licenses/by/2.0), which permits unrestricted use, distribution, and", },
	{"reproduction in any medium, provided the original work is properly cited.", },
		};
		File file = TextFixtures.BMC_312_1_10SA_SVG;
		testSpans(values, file);
	}
	
	
	@Test
	public void testBoldWithSubscriptAndItalic() {
		String[][] values ={
	{"<B>Effect of late promoter </B>", "<B><I>p</I></B>", "<B><I>R</I></B>", "<B><I>’</I></B>", "<B>activity</B>"},
		};
		File file = TextFixtures.BMC_174_5_3SA_SVG;
		testSpans(values, file);
	}
	
	@Test
	public void testLargePara() {
		String[][] values ={
	{"seemingly convex relationship between ", "<I>t</I>", "L ", "- ", "<I>t</I>", "KCN ", "and"},
	{"<I>t</I>", "KCN ", "[[39], their figure five]."},
	{"The effects of ", "<I>t</I>", "KCN ", "on lysis time SDs and CVs are"},
	{"shown in Figure 4B. Again, we witnessed the expected"}, 
	{"pattern of a significant negative relationship between"},
	{"<I>t</I>", "KCN ", "and the SDs (a quadratic fit, ", "<I>F</I>", "[2,4] ", "= 9.91, ", "<I>p </I>", "="},
	// MISSINGLINE in the source
	{"CVs (a quadratic fit, ", "<I>F</I>", "[2,4] ", "= 16.03, ", "<I>p </I>", "= 0.0282, adjusted", },
	{"<I>R</I>", "2 ", "= 0.834). These results showed that the later in time"}, 
	{"KCN was added, the less variation there was in indivi-"}, 
	{"dual lysis times. In fact, the lowest SD (1.45 min) and"},
	{"lowest CV (2.53%) were observed when KCN was added"},
    {"55 min after induction. This was a significant two-fold"},
    {"reduction in the SD when compared normal lysis condi-"}, 
    {"tions (see Table 1 for strain IN56 with the SD = 3.24"}, 
    {"min; Student", "<B>’</B>", "s ", "<I>t </I>", "= 15.45, ", "<I>p </I>", "< 0.0001, using the standard"},
	{"deviation for the SD in Box 7.1 of [56]). This observa-"}, 
	{"tion indicated that individual triggering for hole forma-"}, 
	{"tion during the normal progression of cell lysis was"}, 
	{"relatively asynchronous when compared to the artificial"}, 
	{"method of acute triggering by KCN addition."},
	{"Similar to the effect of growth rate, a linear regression"}, 
	{"of the SDs (", "<I>F</I>", "[1,5] ", "= 0.60, ", "<I>p </I>", "= 0.4726) or CVs (", "<I>F</I>", "[1,5] ", "="}, 
	{"0.328, ", "<I>p </I>", "= 0.5917) against the MLTs did not yield signif-"}, 
	{"icant result. Another interesting aspect of the relation-"}, 
	{"ship between ", "<I>t</I>", "KCN ", "and the lysis time SDs is that the"}, 
	{"SDs drop precipitously when KCN is added about 35"}, 
	{"min after induction. This observation suggests that,"}, 
	{"approximately 35 min after thermal induction, the"}, 
	{"majority of the lysogenic cells have accumulated enough"}, 
	{"holin proteins in the cell membrane to form holes"},
	{"immediately if triggered."}, 
	};
		File file = TextFixtures.BMC_174_6_3SA_SVG;
		testSpans(values, file);
	}
	
	@Test
	public void testGetHTML63() throws Exception {
		File file = TextFixtures.BMC_174_6_3SA_SVG;
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(file);
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(file);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		HtmlElement divElement = sc.createHtmlElement();
		CMLUtil.debug(divElement, new FileOutputStream("target/bmc174_6_3.html"), 0);
	}


	@Test
	public void testReferencesHtml() throws Exception {
		File file = TextFixtures.BMC_312_12_7SB_SVG;
		String outfile = "target/bmc312_12_7sb.html";
		createHtml(file, outfile);
	}

	@Test
	public void testTitleHtml() throws Exception {
		File file = TextFixtures.BMC_312_6_0SA_SVG;
		String outfile = "target/bmc312_6_0sa.html";
		createHtml(file, outfile);
	}

	@Test
	// fails because one textLine is split at gap
	public void testTitleChemical() throws Exception {
		File file = TextFixtures.MDPI_27_4_1SA_SVG;
		String outfile = "target/mdpi_27_4_1sa.html";
		createHtml(file, outfile);
	}

	@Test
	public void testTitleChemical1() throws Exception {
		File file = TextFixtures.MDPI_27_4_1SA0_SVG;
		String outfile = "target/mdpi_27_4_1sa0.html";
		createHtml(file, outfile);
	}

	@Test
	public void testAJC() throws Exception {
		File file = TextFixtures.AJC_01182_2_5SA_SVG;
		String outfile = "target/ajc_01182_2_5Sa.html";
		createHtml(file, outfile);
	}

	@Test
	public void testBMCList() throws Exception {
		File file = TextFixtures.BMC_312_12_7SB_SVG;
		String outfile = "target/bmc_312_12_7Sb.html";
		createList(file, outfile);
	}

	@Test
	@Ignore // fails RuntimeException // FIXME
	public void testMaterialsList() throws Exception {
		File file = TextFixtures.MDPI_27_18_7SA_SVG;
		String outfile = "target/mdpi_27_18_7Sa.html";
		createList(file, outfile);
	}
	
	@Test
	public void testAJCList() throws Exception {
		File file = TextFixtures.ACS_072516_6_4SB_SVG;
		String outfile = "target/acs072516_6_4Sa.html";
		createList(file, outfile);
	}
	
	@Test
	public void testAJCList65() throws Exception {
		File file = TextFixtures.ACS_072516_6_5SA_SVG;
		String outfile = "target/acs072516_6_5Sa.html";
		createList(file, outfile);
	}
	
	@Test
	public void testPeerJBullet() throws Exception {
		File file = TextFixtures.PEERJ_50_12_6SB_SVG;
		String outfile = "target/peerj50.chunk12.6Sb.html";
		createList(file, outfile);
	}
	
	@Test
	public void testRSCList() throws Exception {
		File file = TextFixtures.RSC_B306241d_6_8SA_SVG;
		String outfile = "target/rscb306241d.chunk6.8Sa.html";
		createList(file, outfile);
	}

	@Test
	// don't understand why this doesn't work. Perhaps on double boundary?
	public void testNPGList() throws Exception {
		File file = TextFixtures.NPG_00788_5_3SA_SVG;
		String outfile = "target/npg00778.chunk5.3Sa.html";
		createList(file, outfile);
	}

	@Test
	// PROBLEM WITH SEPARATE ACCENTS on slightly different line
	// also wobbly x coords for start of indent (up to 0.6 pixel)
	@Ignore // superscripts not sorted out
	public void testNPGList54() throws Exception {
		File file = TextFixtures.NPG_00778_5_4SA_SVG;
		String outfile = "target/npg00778.chunk5.4Sa.html";
		createList(file, outfile);
	}

	/** =======================================================
	npg00778.chunk5.3Sa
	 * ========================================================
	 */
	
	private void createList(File file, String outfile) {
		ScriptContainer sc = createScriptContainer(file);
		ListContainer listContainer = ListContainer.createList(sc);
		if (listContainer != null) listContainer.debug();
	}
	


	private void createHtml(File file, String outfile) throws IOException,
			FileNotFoundException {
		ScriptContainer sc = createScriptContainer(file);
		HtmlElement divElement = sc.createHtmlElement();
		CMLUtil.debug(divElement, new FileOutputStream(outfile), 0);
	}


	private ScriptContainer createScriptContainer(File file) {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(file);
		TextStructurer textStructurer = 
				TextStructurer.createTextStructurerWithSortedLines(file);
		List<TextLine> textLineList = textStructurer.getTextLineList();
		for (TextLine textLine : textLineList) {
			LOG.trace("L> "+String.valueOf(textLine));
		}
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textStructurer, pageAnalyzer);
		return sc;
	}

	// ==========================================================================================


	private void testSpans(String[][] values, File file) {
		List<StyleSpans> styleSpansList = ScriptContainerTest.getStyleSpansList(file);
		Assert.assertEquals("lists", values.length, styleSpansList.size());
		for (int i = 0; i < values.length; i++) {
			StyleSpans styleSpans = styleSpansList.get(i);
			if (values[i].length > 0) {
				if (values[i].length != styleSpans.size()) {
					for (int j = 0; j < styleSpans.size(); j++) {
						System.out.println(styleSpans.get(j).toString());
					}
				}
				Assert.assertEquals("line"+i, values[i].length, styleSpans.size());
				for (int j = 0; j < values[i].length; j++) {
					Assert.assertEquals("line"+i+","+j, values[i][j], styleSpans.get(j).toString());
				}
			}
		}
	}


	public static List<StyleSpans> getStyleSpansList(File file) {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(file);
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(file);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		List<StyleSpans> styleSpansList = sc.getStyleSpansList();
		return styleSpansList;
	}

	
	private void testScript(File svgFile, String[][] words) {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(svgFile);
		TextStructurer textContainer = 
				TextStructurer.createTextStructurerWithSortedLines(svgFile);
		PageAnalyzer pageAnalyzer = new PageAnalyzer(svgPage);
		ScriptContainer sc = ScriptContainer.createScriptContainer(textContainer, pageAnalyzer);
		List<ScriptLine> scriptLineList = sc.getScriptLineList();
		Assert.assertEquals("scriptLines", words.length, scriptLineList.size());
		for(int i = 0; i < words.length; i++) {
			ScriptLine scriptLine = scriptLineList.get(i);
			List<ScriptWord> scriptWords = scriptLine.getWords();
			if (words[i].length > 0) {
				if (words[i].length != scriptWords.size()) {
					for (int j = 0; j < scriptWords.size(); j++) {
						System.out.println(scriptWords.get(j).getRawValue());
					}
				}
				Assert.assertEquals("line"+i, words[i].length, scriptWords.size());
				for (int j = 0; j < scriptWords.size(); j++) {
					Assert.assertEquals(""+j, words[i][j], scriptWords.get(j).getRawValue());
				}
			}
		}
	}
}
