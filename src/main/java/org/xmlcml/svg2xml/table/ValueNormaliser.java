
package org.xmlcml.svg2xml.table;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Normalise numerical values from visual to semantic form.
 * 
 * Likely to be superseded by refactoring of upstream character processing.
 * 
 * @author jkbcm
 */
public class ValueNormaliser {
    // Detect dashes used as visual equivalents to minus signs immediately 
    // preceding numerical values:
    //    Unicode minus \u2212
    //    En-dash
    //    Em-dash
    //    Figure dash
    // These should all be converted to 'hyphen minus', which will be accepted 
    // as a minus in ASCII/UTF-8 output such as CSV
    // Dashes denoting a range are not replaced with hyphen minus:
    //    \d--\d
    //    \d--\.
    private static final Logger LOG = Logger.getLogger(TableContentCreator.class);
    static {
        LOG.setLevel(Level.DEBUG);
    }
    
    // Use the Unicode regex for Punctuation dash and also \u2212 Unicode minus
    private static final Pattern MINUS_EQUIVALENTS_PREFIX = Pattern.compile("([\\D])([\\p{IsPd}\u2212])([.\\d]?\\d+)");
    private static final Pattern UNUSUAL_CHAR_TOOLTIP = Pattern.compile("(?<prefixStr>.*[\\s\\p{P}]?)(\\S)$\\s*char: \\S+; name: (?<charName>\\S+); f: (?<fontShortName>\\S+); fn: \\S+; e: \\S+\\R?", Pattern.MULTILINE);
    /////private static final Pattern UNUSUAL_CHAR_TOOLTIP = Pattern.compile(".+\\s*char: \\S+; name: (?<charName>\\\\S+); f: (?<fontShortName>\\\\S+); fn: \\\\S+; e: \\\\S+\\\\R?", Pattern.MULTILINE|Pattern.DOTALL);
    private static final String HTML_UNICODE_UNKNOWN_CHAR_SYMBOL = "\uFFFD";
    
    private static Map<String, String> universalMathPiCodeMapByName = new HashMap<String, String>();
    
    static {
        initialiseCodePointMap();
    }
    
    public static String normaliseNumericalValueString(String cellValueString) { 
        String result = cellValueString;
        Matcher m = MINUS_EQUIVALENTS_PREFIX.matcher(cellValueString);
        StringBuffer sb = new StringBuffer();

        boolean isMatch = false;
        
        while(m.find()) {
            isMatch = true;
            LOG.trace("Matched prefix minus:"+cellValueString);
            m.appendReplacement(sb, "$1-$3");
        }
        
        m.appendTail(sb);
        LOG.trace("Replace non-Unicode char:"+sb.toString());
        
        if (isMatch) {
            LOG.trace("Transformed:"+sb.toString());
        }
        
        result = sb.toString();
        // Handle strings starting with a dash
        result = result.replaceFirst("^\\p{Pd}|\u2212", "-");
        
        return result;
    }
    
    public static String removeUnusualCharacterTooltip(String inputString) {
        if (inputString.isEmpty()) {
            return inputString;
        }
        
        Matcher m =  UNUSUAL_CHAR_TOOLTIP.matcher(inputString);
        
        String result = inputString;
        
        while(m.find()) {
            LOG.debug("Matched unusual character tooltip:["+inputString+"]");
            String charName = m.group("charName");
            String fontShortName = m.group("fontShortName");
            String prefixStr = m.group("prefixStr");
            LOG.debug("char:"+charName+";fontShortName:"+fontShortName+";prefixStr:"+prefixStr);
            if (fontShortName.startsWith("Universal") && charName != null) {
                String lookUp = universalMathPiCodeMapByName.getOrDefault(charName, HTML_UNICODE_UNKNOWN_CHAR_SYMBOL);
                result = m.replaceAll("${prefixStr}"+lookUp);
            } else {
                result = m.replaceAll("");
            }
            
            // The character may be a placeholder needed to preserve the layout
            // so if removing the descriptive text ensure that there is at least one character
            if (result.isEmpty()) {
                result = HTML_UNICODE_UNKNOWN_CHAR_SYMBOL;
            }
        }
        
        return result;
    }
    
    private static void initialiseCodePointMap() {
        if (universalMathPiCodeMapByName == null) {
            universalMathPiCodeMapByName = new HashMap<String, String>();
        }
        
        universalMathPiCodeMapByName.put("H11002", "-");
        universalMathPiCodeMapByName.put("H11003", "\u00d7");
        universalMathPiCodeMapByName.put("H11004", "÷");
        universalMathPiCodeMapByName.put("H11005", "=");
        universalMathPiCodeMapByName.put("H11021", "<");
        universalMathPiCodeMapByName.put("H9004", "\u0394"); // note="GREEK CAPITAL LETTER DELTA"
        universalMathPiCodeMapByName.put("H9021", "\u03A6"); // note="GREEK CAPITAL LETTER PHI"
        universalMathPiCodeMapByName.put("H9251", "\u03B1"); // note="GREEK SMALL LETTER ALPHA"
        universalMathPiCodeMapByName.put("a", "\u03B1"); // note="GREEK SMALL LETTER ALPHA"
        universalMathPiCodeMapByName.put("H9252", "\u03B2"); // note="GREEK SMALL LETTER BETA"
        universalMathPiCodeMapByName.put("b", "\u03B2"); // note="GREEK SMALL LETTER BETA"
        universalMathPiCodeMapByName.put("H9253", "\u03B3"); // note="GREEK SMALL LETTER GAMMA"
        universalMathPiCodeMapByName.put("H9254", "\u03B4"); // note="GREEK SMALL LETTER DELTA"
        universalMathPiCodeMapByName.put("H9255", "\u03B5"); // note="GREEK SMALL LETTER EPSILON"
        universalMathPiCodeMapByName.put("H9256", "\u03B6"); // note="GREEK SMALL LETTER ZETA"
        universalMathPiCodeMapByName.put("H9257", "\u03B7"); // note="GREEK SMALL LETTER ETA"
        universalMathPiCodeMapByName.put("H9258", "\u03B8"); // note="GREEK SMALL LETTER THETA"
        universalMathPiCodeMapByName.put("u", "\u03B8"); // note="GREEK SMALL LETTER THETA"
        universalMathPiCodeMapByName.put("H9259", "\u03B9"); // note="GREEK SMALL LETTER IOTA"
        universalMathPiCodeMapByName.put("H9260", "\u03BA"); // note="GREEK SMALL LETTER KAPPA"
        universalMathPiCodeMapByName.put("H9261", "\u03BB"); // note="GREEK SMALL LETTER LAMBDA"
        universalMathPiCodeMapByName.put("H9262", "\u03BC"); // note="GREEK SMALL LETTER MU"
        universalMathPiCodeMapByName.put("m", "\u03BC"); // note="GREEK SMALL LETTER MU"
        universalMathPiCodeMapByName.put("H9263", "\u03BD"); // note="GREEK SMALL LETTER NU"
        universalMathPiCodeMapByName.put("H9266", "\u03C0"); // note="GREEK SMALL LETTER PI"
        universalMathPiCodeMapByName.put("H9267", "\u03C0"); // note="GREEK SMALL LETTER RHO"
        universalMathPiCodeMapByName.put("H9268", "\u03C3"); // note="GREEK SMALL LETTER SIGMA"
        universalMathPiCodeMapByName.put("H9270", "\u03C4"); // note="GREEK SMALL LETTER TAU"
        universalMathPiCodeMapByName.put("H9274", "\u03C6"); // note="GREEK SMALL LETTER PHI"
        universalMathPiCodeMapByName.put("H9273", "\u03C7"); // note="GREEK SMALL LETTER CHI"
        universalMathPiCodeMapByName.put("x", "\u03C7"); // note="GREEK SMALL LETTER CHI"
        universalMathPiCodeMapByName.put("H9274", "\u03C8"); // note="GREEK SMALL LETTER PSI"
        universalMathPiCodeMapByName.put("H9275", "\u03C9"); // note="GREEK SMALL LETTER OMEGA"
        universalMathPiCodeMapByName.put("H11001", "\u002B"); // note="PLUS"
        universalMathPiCodeMapByName.put("H11002", "\u002D"); // note="MINUS"
        universalMathPiCodeMapByName.put("H11003", "\u00D7"); // note="MULTIPLICATION SIGN" 
        universalMathPiCodeMapByName.put("H11004", "\u00F7"); // note="DIVISION SIGN" 
        universalMathPiCodeMapByName.put("H11005", "\u003D"); // note="EQUALS" 
        universalMathPiCodeMapByName.put("H11006", "\u00B1"); // note="PLUS-MINUS SIGN" 
        universalMathPiCodeMapByName.put("H11009", "\u221E"); // note="INFINITY" 
        universalMathPiCodeMapByName.put("H11011", "\u007E"); // note="TILDE" 
        universalMathPiCodeMapByName.put("H11015", "\u2245"); // note="APPROXIMATELY EQUAL TO" 
        universalMathPiCodeMapByName.put("H11021", "\u003C"); // note="LESS-THAN SIGN" 
        universalMathPiCodeMapByName.put("comma", "\u003C"); // note="LESS-THAN SIGN" 
        universalMathPiCodeMapByName.put("H11022", "\u003E"); // note="GREATER-THAN SIGN" 
        universalMathPiCodeMapByName.put("period", "\u003E"); // note="GREATER-THAN SIGN" 
        universalMathPiCodeMapByName.put("H11032", "\u0027"); // note="APOSTROPHE" 
        universalMathPiCodeMapByName.put("nine", "\u0027"); // note="APOSTROPHE"
        universalMathPiCodeMapByName.put("H11033", "\\u0022"); // note="QUOT"
        universalMathPiCodeMapByName.put("H11034", "\u00B0"); // note="DEGREE SIGN" 
        universalMathPiCodeMapByName.put("H11341", "\u007C");  // "VERTICAL LINE" 
        universalMathPiCodeMapByName.put("H11349", "\u2264"); // note="LESS THAN OR EQUAL TO" 
        universalMathPiCodeMapByName.put("H11350", "\u2265"); // note="GREATER THAN OR EQUAL TO"
        universalMathPiCodeMapByName.put("H11554", "\u00B7"); // note="MIDDLE DOT" 
        universalMathPiCodeMapByName.put("HS11005", "\u2260"); // note="NOT EQUAL TO" 
        universalMathPiCodeMapByName.put("asciitilde", "\u0028"); // note="LEFT PARENTHESIS" 
        universalMathPiCodeMapByName.put("exclam", "\u0029"); // note="RIGHT PARENTHESIS" 
        universalMathPiCodeMapByName.put("H11013", "\u003D"); //unicodeName="EQUALS" 
        universalMathPiCodeMapByName.put("H20900", "\u005B"); //unicodeName="LEFT SQUARE BRACKET" 
        universalMathPiCodeMapByName.put("H20908", "\u003C"); //unicodeName="LESS-THAN SIGN" 
        universalMathPiCodeMapByName.put("H20909", "\u003E"); //unicodeName="GREATER-THAN SIGN" 
        universalMathPiCodeMapByName.put("H20841", "\u007C"); //unicodeName="VERTICAL LINE" 
        universalMathPiCodeMapByName.put("H20848", "\u222B"); //unicodeName="INTEGRAL" 
        universalMathPiCodeMapByName.put("H20849", "\u0028"); //unicodeName="LEFT PARENTHESIS" 
        universalMathPiCodeMapByName.put("H20850", "\u0029"); //unicodeName="RIGHT PARENTHESIS" 
        universalMathPiCodeMapByName.put("H20853", "\u007B"); //note="LEFT CURLY BRACKET" 
        universalMathPiCodeMapByName.put("H20854", "\u007D"); //note="RIGHT CURLY BRACKET" 
        universalMathPiCodeMapByName.put("H20858", "\u03A3"); //note="GREEK CAPITAL LETTER SIGMA" 
        universalMathPiCodeMapByName.put("H20873", "\u0028"); //note="LEFT PARENTHESIS" 
        universalMathPiCodeMapByName.put("H20874", "\u0029"); //note="RIGHT PARENTHESIS" 
        universalMathPiCodeMapByName.put("H20881", "\u221A"); //unicodeName="SQUARE ROOT" 
        universalMathPiCodeMapByName.put("H20888", "\u2211"); //unicodeName="N-ARY SUMMATION" 
        universalMathPiCodeMapByName.put("H11541", "\u2032"); //unicodeName="PRIME" 
        universalMathPiCodeMapByName.put("H11542", "\u2033"); //unicodeName="DOUBLE PRIME" 
        universalMathPiCodeMapByName.put("H11546", "\u2014"); //unicodeName="EM DASH" 
        universalMathPiCodeMapByName.put("H11549", "\u003D"); //unicodeName="EQUALS" 
        universalMathPiCodeMapByName.put("H11601", "\u007E"); // unicodeName="TILDE" 
        universalMathPiCodeMapByName.put("H17188", "\u2207"); // unicodeName="NABLA" 
        universalMathPiCodeMapByName.put("H33522", "\u007E"); // unicodeName="TILDE" 
        universalMathPiCodeMapByName.put("space", "\u0020"); // note="SPACE" 
        universalMathPiCodeMapByName.put("H17003", "\u25C7"); // note="WHITE DIAMOND"  
        universalMathPiCodeMapByName.put("H17004", "\u25C7"); // note="WHITE DIAMOND" 
        universalMathPiCodeMapByName.put("H17005", "\u25B3"); // note="WHITE UPWARDS-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17006", "\u25BD"); // note="WHITE DOWNWARDS-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17007", "\u25B7"); // note="WHITE RIGHT-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17008", "\u25C1"); // note="WHITE LEFT-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17009", "\u25B2"); // note="BLACK UPWARDS-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17010", "\u25BC"); // note="BLACK DOWNWARDS-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17011", "\u25C0"); // note="BLACK LEFT-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17012", "\u25B6"); // note="BLACK RIGHT-POINTING TRIANGLE" 
        universalMathPiCodeMapByName.put("H17033", "\u25CF"); // note="BLACK CIRCLE" 
        universalMathPiCodeMapByName.put("H17034", "\u25CB"); // note="WHITE CIRCLE" 
        universalMathPiCodeMapByName.put("H17039", "\u25A0"); // note="BLACK SQUARE" 
        universalMathPiCodeMapByName.put("H17040", "\u25A1"); // note="WHITE SQUARE" 
        universalMathPiCodeMapByName.put("H11568", "\u25E6"); // note="WHITE BULLET" 
        universalMathPiCodeMapByName.put("H11569", "\u002A"); // note="ASTERISK" 
        universalMathPiCodeMapByName.put("H11623", "\u25A1"); // note="WHITE SQUARE" 
        universalMathPiCodeMapByName.put("H22841", "\u2606"); // note="WHITE STAR" 
    }
}
