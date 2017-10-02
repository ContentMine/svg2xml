package org.xmlcml.svg2xml.container;


import java.util.List;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.html.HtmlDiv;
import org.xmlcml.graphics.html.HtmlElement;
import org.xmlcml.graphics.html.HtmlTable;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGImage;
import org.xmlcml.graphics.svg.SVGShape;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.svg2xml.page.FigureAnalyzer;
import org.xmlcml.svg2xml.page.ImageAnalyzer;
import org.xmlcml.svg2xml.page.MixedAnalyzer;
import org.xmlcml.svg2xml.page.PageAnalyzer;
import org.xmlcml.svg2xml.page.ShapeAnalyzer;
import org.xmlcml.svg2xml.page.TableAnalyzer;
import org.xmlcml.svg2xml.page.TextAnalyzerOLD;
import org.xmlcml.svg2xml.text.ScriptLineOLD;
import org.xmlcml.svg2xml.text.TextLineOLD;
import org.xmlcml.svg2xml.text.TextStructurerOLD;

public class MixedContainer extends AbstractContainerOLD {

	public final static Logger LOG = Logger.getLogger(MixedContainer.class);
	private boolean box;
	private ShapeContainer shapeContainer;
	private ImageContainer imageContainer;
	private TextAnalyzerOLD textAnalyzerX;
	private TextStructurerOLD textStructurer;
	private ScriptContainerOLD scriptContainer;
	private HtmlTable tableElement;
	private HtmlDiv figureElement;

	public MixedContainer(PageAnalyzer pageAnalyzer) {
		super(pageAnalyzer);
	}

	public static MixedContainer createMixedContainer(PageAnalyzer pageAnalyzer, MixedAnalyzer mixedAnalyzer) {
		MixedContainer mixedContainer = new MixedContainer(pageAnalyzer);
		addSVGElements(mixedContainer, mixedAnalyzer);
		return mixedContainer;
	}

	private static void addSVGElements(MixedContainer mixedContainer, MixedAnalyzer mixedAnalyzer) {
		List<SVGShape> shapeList = mixedAnalyzer.getShapeList();
		if (shapeList != null && shapeList.size() > 0){
			mixedContainer.addShapeList(shapeList);
		}
		List<SVGImage> imageList = mixedAnalyzer.getImageList();
		if (imageList != null && imageList.size() > 0){
			mixedContainer.addImageList(imageList);
		}
		List<SVGText> textList = mixedAnalyzer.getTextList();
		if (textList != null && textList.size() > 0){
			mixedContainer.addTextList(textList);
		}
	}

	public void addImageList(List<SVGImage> imageList) {
		if (imageList != null && imageList.size() > 0) {
			imageContainer = new ImageContainer(pageAnalyzer);
			imageContainer.addImageList(imageList);
			imageContainer.setChunkId(this.getChunkId());
			this.add(imageContainer);
		}
	}

	public void addShapeList(List<SVGShape> shapeList) {
		if (shapeList != null && shapeList.size() > 0) {
			shapeContainer = new ShapeContainer(pageAnalyzer);
			shapeContainer.setShapeList(shapeList);
			shapeContainer.setChunkId(this.getChunkId());
			this.add(shapeContainer);
		}
	}

	public void addTextList(List<SVGText> characterList) {
		if (characterList != null && characterList.size() > 0) {
			textAnalyzerX = new TextAnalyzerOLD(pageAnalyzer);
			textAnalyzerX.setTextList(characterList);
			List<TextLineOLD> textLineList = getTextAnalyzer().getTextLines();
			textStructurer = new TextStructurerOLD((TextAnalyzerOLD) null);
			textStructurer.setTextLines(textLineList);
			List<ScriptLineOLD> scriptList = textStructurer.getScriptedLineListForCommonestFont();
			scriptContainer = new ScriptContainerOLD(pageAnalyzer);
			scriptContainer.add(scriptList);
			add(scriptContainer);
		}
	}

	@Override
	public HtmlElement createHtmlElement() {
		return super.createFigureHtmlElement();
		//HtmlDiv divElement = new HtmlDiv();
		//HtmlP p = new HtmlP();
		//p.appendChild("DIV NYI");
		//divElement.appendChild(p);
		//return divElement;
	}
	
	@Override
	public SVGG createSVGGChunk() {
		SVGG g = new SVGG();
		for (AbstractContainerOLD container : containerList) {
			SVGG childG = container.createSVGGChunk();
			g.appendChild(childG);
		}
		return g;
	}

	@Override
	public String toString() {
		LOG.trace("");
		StringBuilder sb = new StringBuilder("<MIXED>\n"+super.toString());
		sb.append("</MIXED>\n");
		String s = sb.toString();
		return s;
	}

	public void setBox(boolean b) {
		this.box = b;
	}
	
	public boolean hasBox() {return box;}

	@Override
	public String getRawValue() {
		StringBuilder sb = new StringBuilder();
		for (AbstractContainerOLD container : containerList) {
			sb.append(container.getRawValue()+"\n");
		}
		return sb.toString();
	}

	public TextAnalyzerOLD getTextAnalyzer() {
		return textAnalyzerX;
	}

	public void setTextAnalyzerX(TextAnalyzerOLD textAnalyzerX) {
		this.textAnalyzerX = textAnalyzerX;
	}

	public AbstractContainerOLD getPathContainer() {
		return shapeContainer;
	}
	
	public List<SVGText> getTextCharacters() {
		return textAnalyzerX == null ? null : textAnalyzerX.getTextCharacters();
	}
	
	public List<SVGShape> getShapeList() {
		return shapeContainer == null ? null : shapeContainer.getShapeList();
	}

	public List<SVGImage> getImageList() {
		return imageContainer == null ? null : imageContainer.getImageList();
	}

	public ShapeAnalyzer createShapeAnalyzer() {
		ShapeAnalyzer shapeAnalyzer = new ShapeAnalyzer(pageAnalyzer);
		List<SVGShape> shapeList = getShapeList(); 
		shapeAnalyzer.addShapeList(shapeList);
		return shapeAnalyzer;
	}

	public ImageAnalyzer createImageAnalyzer() {
		ImageAnalyzer imageAnalyzer = new ImageAnalyzer(pageAnalyzer);
		List<SVGImage> imageList = getImageList(); 
		imageAnalyzer.addImageList(imageList);return imageAnalyzer;
	}

	public HtmlTable createTableHtmlElement() {
		if (true && false) {
			LOG.warn("FORCE SKIP TABLE");
			HtmlTable table = new HtmlTable();
			table.appendChild("NULL TABLE");
			return table;
		}
		if (tableElement == null) {
			TableAnalyzer tableAnalyzer = new TableAnalyzer(getTextAnalyzer(), createShapeAnalyzer());
			tableElement = tableAnalyzer.createTable();
			LOG.trace(tableElement.toXML());
		}
		return tableElement;
	}

	public HtmlDiv createFigureElement() {
		if (figureElement == null) {
			FigureAnalyzer figureAnalyzer = new FigureAnalyzer(getTextAnalyzer(), createShapeAnalyzer(), createImageAnalyzer(), this.svgChunk);
			figureElement = figureAnalyzer.createHtmlFigure();
		}
		return figureElement;
	}

}
