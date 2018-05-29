/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package find_dead_python_code;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author DThompson
 */
public class CodeElements {

    private static final String[] controllers = {"superuser\\/v2org",
            "superuser\\/chain",
            "employee\\/template",
            "printable",
            "bazman",
            "myaccount",
            "message",
            "category",
            "v2",
            "employee",
            "subscription",
            "reports",
            "videos",
            "v2org",
            "shift",
            "shift_type_type",
            "shiftreports",
            "shifttemplates",
            "superuser",
            "excel",
            "v2shift",
            "shiftrequirement",
            "upload",
            "api",
            "notes",
            "pto",
            "help",
            "chain",
            "executive",
            "punch",
            "twilio",
            "census",
            "support",
            "landing_page",
            "scheduler_visibility",
            "superintuser",
            "overtime",
            "emp_share",
            "reporting",
            "mobile",
            "integrationtasks",
            "authorship",
            "rest",
            "auth",
            "kiosk",
            "calendar",
            "sso",
            "integration",
            "insight",
            "pbj",
            "marketing_engage",
            "overtime_log",
            "chain_management",
            "budget"};
    
    public final static String findExposedPythonRegex = getExposedPythonRegex();
    public final static String findExposedPythonHBSRegexSimple = "(href=[\\\"|'|`])([^\\?|\\\"|'|`]*)";
    public final static String findExposedPythonHBSRegexComplex = getfindExposedPythonHBSRegexComplex();

    private static final List<String> reservedWords = Arrays.asList("False", "class", "finally", "is", "return",
            "None", "continue", "for", "lambda", "try",
            "True", "def", "from", "nonlocal", "while",
            "and", "del", "global", "not", "with",
            "as", "elif", "if", "or", "yield",
            "assert", "else", "import", "pass",
            "break", "except", "in", "raise");
    
    public static boolean isReservedWord(String tokenString) {
        return reservedWords.contains(tokenString);
    }

    public static String getExposedPythonRegex() {
        String list = "";
        
        for (String controller : CodeElements.controllers) {
            if (!list.isEmpty()) {
                list += "|";
            }
            list += controller;
        }
        
        return "('|\\\"|`)\\/(" + list + ")\\/([^\\/|\\\"|'|\\?]*)";
    }
    
    private static String getfindExposedPythonHBSRegexComplex() {
        String list = "";
        
        for (String controller : controllers) {
            if (!list.isEmpty()) {
                list += "|";
            }
            list += controller;
        }
        
        return "([href]|[action]=[\\\"|'|`]\\/?)(" + list + ")\\/([^\\?|\\\"|'|`|\\/|\\$]*)";
    }
    
    public static void runTest() {
        Pattern p = Pattern.compile(CodeElements.findExposedPythonHBSRegexComplex);
        Matcher m = p.matcher("<form name=\"create_pto_form\" action=\"/executive/alert_rule_save${'?alert_rule_id=' + str(alert_rule.id) if alert_rule else ''}\" method=\"post\">");
        
        while (m.find()) {
            for (Integer i = 0; i <= m.groupCount(); i++) {
                System.out.print(i.toString() + ":" + m.group(i) + " - ");
            }
            System.out.print("\n");
        }
    }
}
