
package org.xmlcml.svg2xml.table;

import java.util.LinkedList;
import java.util.List;
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
    
    public static String normaliseNumericalValueString(String cellValueString) { 
        String result = cellValueString;
        Matcher m = MINUS_EQUIVALENTS_PREFIX.matcher(cellValueString);
        StringBuffer sb = new StringBuffer();

        boolean isMatch = false;
        
        while(m.find()) {
            isMatch = true;
            LOG.debug("Matched prefix minus:"+cellValueString);
            m.appendReplacement(sb, "$1-$3");
        }
        
        m.appendTail(sb);
        
        if (isMatch) {
            LOG.debug("Transformed:"+sb.toString());
        }
        
        result = sb.toString();
        // Handle strings starting with a dash
        result = result.replaceFirst("^\\p{Pd}|\u2212", "-");
        LOG.debug("replaceFirst:"+result);
        
        return result;
    }
}
