/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package find_dead_python_code;

/**
 *
 * @author DThompson
 */
// TODO: string callers of methods can look like this-> id=('|\\")<METHOD_NAME>('|\\") 
// above applies to "location_census_popup" method
// TODO: Find "# TODO: DELETE" comments
// TODO: Test for multiple commented lines (single) in a row
// TODO: Remember that kid files can reference python
// TODO: Remember enpoint references
// TODO: ask tim about get methods which turn into SQL refs
// TODO: bug Tim/Cale about other potential uses of methods
// TODO: Remember to check for imports of methods
// TODO: Remember to check within the parent file whether the method is being passed as an object to something
// TODO: Parse all kid files to check for endpoint useage (for now, jsut exclude exposed methods)
// TODO: Also... gotta check to see whether the kid files themselves are being used. O_o Damn.
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Find_Dead_Python_Code {

    static String myDirectoryPath = "/Users/DThompson/Workspaces/OnShift";
    List<String> ignoreList = Arrays.asList(new String[]{"/Users/DThompson/Workspaces/OnShift/bazman/tests",
        "/Users/DThompson/Workspaces/OnShift/static/reactjs_components/test",
        "/Users/DThompson/Workspaces/OnShift/static/dist",
        "/Users/DThompson/Workspaces/OnShift/static/reactjs_components/js/components.js",
        "/Users/DThompson/Workspaces/OnShift/bazman/scripts2/elastic_search/Vpy36",
        "/Users/DThompson/Workspaces/OnShift/bazman/optimizer/r.js",
        "/Users/DThompson/Workspaces/OnShift/bazman/bazman/util/feature_toggle/feature_toggle_finder/tests",
        "/Users/DThompson/Workspaces/OnShift/bazman/bazman/reporting/test",
        "/Users/DThompson/Workspaces/OnShift/bazman/bazman/model/callout/tests"});
    List<String> scanTypes = Arrays.asList(new String[]{"py", "kid", "js", "jsx", "hbs"});
    Map<String, CodeFile> allFiles = new HashMap<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Find_Dead_Python_Code findCode = new Find_Dead_Python_Code();
        
        //CodeElements.runTest();

        System.out.println("Loading files");
        findCode.loopFiles(myDirectoryPath);
        System.out.println("\nGathering functions defined");
        findCode.collectFunctionDefinitions();
        System.out.println("\nGathering functions called");
        findCode.collectCalledFunctions();
        findCode.printCollectedData();
    }

    private void loopFiles(String directory) {
        File dir = new File(directory);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {
            for (File child : directoryListing) {
                boolean isDirectoy = child.isDirectory();
                String fullPath = child.getAbsolutePath();

                if (this.shouldIgnore(fullPath) || (!isDirectoy && !this.isScanType(fullPath))) {
                    continue;
                }

                if (isDirectoy) {
                    this.loopFiles(fullPath);
                } else {
                    try {
                        allFiles.put(fullPath, this.makeCodeFile(fullPath));
                        System.out.print(".");
                    } catch (IOException e) {
                        System.out.println("ERROR: " + fullPath + " - " + e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    private boolean isScanType(String fullPath) {
        return scanTypes.contains(getFileExtension(fullPath));
    }

    private String getFileExtension(String fullPath) {
        String[] splitFilePath = fullPath.split("\\.");
        return splitFilePath.length > 1 ? splitFilePath[splitFilePath.length - 1] : "";
    }

    private boolean shouldIgnore(String fullPath) {
        return ignoreList.contains(fullPath);
    }

    private CodeFile makeCodeFile(String fullPath) throws FileNotFoundException, IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fullPath))) {
            String code = "";
            String line;
            String extension = getFileExtension(fullPath);

            // TODO: clean out html comments, clean kid files of html & js comments
            switch (extension) {
                case "kid":
                    while ((line = br.readLine()) != null) {
                        code += line + "\n";
                    }
                    break;
                case "jsx":
                case "js": // js files can contain python
                    while ((line = br.readLine()) != null) {
                        code += getSingleLineNoComments(line, "//");
                    }
                    code = removeJSBlockQuotes(code);
                    code = removePythonBlockQuotes(code);
                    break;
                case "py":
                    while ((line = br.readLine()) != null) {
                        // TODO: Consider including properties at a later date
                        if (line.trim().equals("@property")) {
                            br.readLine();
                        }
                        
                        code += getSingleLineNoComments(line, "#");
                    }

                    code = removePythonBlockQuotes(code);
                    break;
                case "hbs":
                    while ((line = br.readLine()) != null) {
                        code += line + "\n";
                    }
                    break;
            }

            return new CodeFile(fullPath, code, extension);
        }
    }
    
    private String removePythonBlockQuotes(String code) {
        // TODO: Look into whether there's a way to actually do this... Python uses multiline strings (which I need)
        // as block quotes 
        /*code = code.replaceAll("(?s)\"\"\"(.*?)\"\"\"", "");
        code = code.replaceAll("(?s)'''(.*?)'''", "");
        // eliminate completely blank lines
        code = code.replaceAll("\\s*\n\\s*\n", "\n");*/
                    
        return code;
    }
    
    private String removeJSBlockQuotes(String code) {
        code = code.replaceAll("(?s)\\/\\*.*?\\*\\/", "");
        // eliminate completely blank lines
        code = code.replaceAll("\\s*\n\\s*\n", "\n");
        return code;
    }

    private String getSingleLineNoComments(String line, String delimitter) {
        String ret = "";
        
        if (line.contains(delimitter)) {
            String[] decommentedLine = line.split(delimitter);
            if (decommentedLine.length > 0 && !decommentedLine[0].trim().isEmpty()) {
                ret = decommentedLine[0] + "\n";
            }
        } else if (!line.trim().isEmpty()) {
            ret = line + "\n";
        }
        
        return ret;
    }

    private void collectFunctionDefinitions() {
        allFiles.values().forEach((curFile) -> {
            curFile.parseDefinedFunctions();
        });
    }

    private void collectCalledFunctions() {
        allFiles.values().forEach((curFile) -> {
            curFile.parseUsedFunctions();
        });
    }

    // TODO: probably delete this if it doesn't turn into something useful
    private void printCollectedData() {
        Map<String, Integer> functionsCalled = new HashMap<>();

        // organizes called functions
        allFiles.values().forEach((codeFile) -> {
            codeFile.getFunctionsCalled().entrySet().forEach((entry) -> {
                String myFunction = entry.getKey();
                Integer count = entry.getValue();

                if (functionsCalled.containsKey(myFunction)) {
                    functionsCalled.replace(myFunction, functionsCalled.get(myFunction) + count);
                } else {
                    functionsCalled.put(myFunction, count);
                }
            });
        });

        int functionsToDelete = 0;
        int linesToDelete = 0;

        // tests defined functions
        for (CodeFile codeFile : allFiles.values()) {
            boolean hasDead = false;
            Integer numDead = 0;
            for (Function myFunction : codeFile.getMyFunctions()) {
                if (!functionsCalled.containsKey(myFunction.getName())) {
                    
                    numDead++;
                    functionsToDelete++;
                    Integer functionLines = codeFile.countFunctionLines(myFunction.getName());
                    linesToDelete += functionLines;
                    
                    // TODO: only here for convenience... delete later.
                    if (functionLines < 40) {
                        continue;
                    }
                    
                    if (!hasDead) {
                        hasDead = true;
                        System.out.println("Dead functions in: " + codeFile.getFullPathName());
                    }
                    System.out.println(numDead.toString() +":   " + myFunction.getName() 
                            + "    Lines: " + functionLines);
                }
            }
        }

        System.out.println("\nSuspect Functions: " + functionsToDelete + "\nLines freed: " + linesToDelete);
    }
}
