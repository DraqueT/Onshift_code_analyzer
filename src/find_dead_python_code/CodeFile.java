/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package find_dead_python_code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author DThompson
 */
public class CodeFile {
    private final String fileExtension;
    private final String fullPathName;
    private final String fileText;
    private final Map<String, Function> myFunctions = new HashMap<>();
    private final Map<String, Integer> functionsCalled = new HashMap<>();

    public CodeFile(String _fullPathName, String _fileText, String extension) {
        fileText = _fileText;
        fullPathName = _fullPathName;
        fileExtension = extension;
    }

    public String getFileText() {
        return fileText;
    }

    public String getFullPathName() {
        return fullPathName;
    }
    
    public Map<String, Integer> getFunctionsCalled() {
        return functionsCalled;
    }
    
    public List<Function> getMyFunctions() {
        return new ArrayList(myFunctions.values());
    }

    /**
     * Resets values of code file (post code text change)
     */
    public void resetCalculations() {
        myFunctions.clear();
        functionsCalled.clear();
    }
    
    /**
     * *
     * Parses and stores all functions defined in file (does not associate with class)
     */
    public void parseDefinedFunctions() {
        switch (fileExtension) {
            case "py":
                parsePythonDefinedFunctions();
                break;
            case "js":
            case "jsx":
                // TODO: This? Maybe? This seems nightmarishly difficult with JS
                break;
            case "kid":
                // TODO: Ugh. No.
                break;
        }
    }
    
    private void parsePythonDefinedFunctions() {
        System.out.print(".");
        for (String line : fileText.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("def ")) {
                String functionName = getFunctionName(line);
                Function newFunction = new Function(functionName);
                myFunctions.put(functionName, newFunction);
            }
        }
    }

    /**
     * *
     * Parses and stores all functions called within file (does not attempt to track function of owner due to Python
     * ducktyping)
     */
    public void parseUsedFunctions() {
        System.out.print(".");
        switch (fileExtension) {
            case "py":
                parsePythonUsedFunctions();
                // python files can contain JS strings
                parseJSUsedFunctions();
                parseHBSUsedFunctions();
                break;
            case "js":
            case "jsx":
                parseJSUsedFunctions();
                break;
            case "kid":
                // kid files can contain any goddamned thing because they are both terrible and also the worst.
                parseKidUsedFunctions();
                parseJSUsedFunctions();
                parsePythonUsedFunctions();
                parseHBSUsedFunctions();
                break;
            case "hbs":
                parseHBSUsedFunctions();
        }
    }
    
    private void parseHBSUsedFunctions() {
        Pattern p = Pattern.compile(CodeElements.findExposedPythonHBSRegexSimple);
        Matcher m = p.matcher(fileText);
        
        while (m.find()) {
            if (m.groupCount() == 2) {
                String foundToken = m.group(2);
                if (functionsCalled.containsKey(foundToken)) {
                    functionsCalled.replace(foundToken, functionsCalled.get(foundToken) + 1);
                } else {
                    functionsCalled.put(foundToken, 1);
                }
            }
        }
        
        p = Pattern.compile(CodeElements.findExposedPythonHBSRegexComplex);
        m = p.matcher(fileText);
        
        while (m.find()) {
            if (m.groupCount() == 3) {
                String foundToken = m.group(3);
                if (functionsCalled.containsKey(foundToken)) {
                    functionsCalled.replace(foundToken, functionsCalled.get(foundToken) + 1);
                } else {
                    functionsCalled.put(foundToken, 1);
                }
            }
        }
        
        p = Pattern.compile("id[\\\\n|\" \"|\\\\t|\\\\r]*=[\\\\n|\" \"|\\\\t|\\\\r]*'" +  + "'"); //[\\n|" "|\\t|\\r]*
        m = p.matcher(fileText);
    }
    
    private void parseKidUsedFunctions() {
        // TODO: is there more to be done here?
    }
    
    private void parseJSUsedFunctions() {
        String regex = CodeElements.getExposedPythonRegex();// findExposedPythonRegex;
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(fileText);
        
        while (m.find()) {
            if (m.groupCount() == 3) {
                String foundToken = m.group(3);
                if (functionsCalled.containsKey(foundToken)) {
                    functionsCalled.replace(foundToken, functionsCalled.get(foundToken) + 1);
                } else {
                    functionsCalled.put(foundToken, 1);
                }
            }
        }
    }
    
    private void parsePythonUsedFunctions() {
        // eliminate lines that are definitions
        String defFilteredText = fileText.replaceAll("def .*", "");
                
        // finds legal 
        Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)[\\\\n|\" \"|\\\\t|\\\\r]*[\\(|,|\\)]");
        Matcher matcher = pattern.matcher(defFilteredText);
        while (matcher.find()) {
            String foundToken = matcher.group(1);
            if (!CodeElements.isReservedWord(foundToken)) {
                if (functionsCalled.containsKey(foundToken)) {
                    functionsCalled.replace(foundToken, functionsCalled.get(foundToken) + 1);
                } else {
                    functionsCalled.put(foundToken, 1);
                }
            }
        }
    }
    
    public int countFunctionLines(String funName) {
        int funLines = 0;
        
        if (myFunctions.containsKey(funName)) {
            String[] breakUpCode = fileText.split("def(\\s*)" + funName + "\\(");
            
            String[] postFuncLines = {};
            
            if (breakUpCode.length > 1) {
                postFuncLines = breakUpCode[1].split("\n");
            }
            
            int startDepth = postFuncLines.length > 1 ? getCodeDepth(postFuncLines[1]) : 0;
            for (int i = 1; i < postFuncLines.length; i++) {
                String line = postFuncLines[i];
                if (!line.trim().isEmpty() && getCodeDepth(line) < startDepth) {
                    break;
                }
                funLines++;
            }
        }
        
        return funLines;
    }
    
    public void printFunctionLengths() {
        myFunctions.keySet().forEach((funName) -> {
            System.out.println(funName + ":" + countFunctionLines(funName));
        });
    }
    
    /**
     * Returns depth of given line of code (4 spaces or 1 tab = 1 level of depth)
     * @param codeLine line of code to analyze
     * @return depth of code (starts at 0)
     */
    private int getCodeDepth(String codeLine) {
        String curLine = codeLine;
        int ret = 0;
        
        for (;;) {
            if (curLine.startsWith("    ")) {
                ret++;
                curLine = curLine.substring(4);
            } else if (curLine.startsWith("\t")) {
                curLine = curLine.substring(1);
            } else {
                break;
            }
        }
        
        return ret;
    }

    /**
     * Returns name of function in given line of python code
     *
     * @param line
     * @return
     */
    private String getFunctionName(String line) {
        int defIndex = line.indexOf("def");
        int parensIndex = line.indexOf("(");
        String ret = "";
        if (defIndex > -1 && defIndex < parensIndex) {
            ret = line.substring(defIndex + 3, parensIndex).trim();
        }
        return ret;
    }
}
