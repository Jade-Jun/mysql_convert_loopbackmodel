import java.io.*;
import java.util.ArrayList;

/**
 * my sql convert to loopback model
 * Created By Jun Yong Su 2018.01.15
 */

public class ParsingSql {
    public static void main(String args[]) {
        System.out.println("start");

        // 현재 경로로 파일 탐색 
        ParsingSql obj = new ParsingSql();
        if (args.length == 0) {
            ArrayList<File> fileList = getFileList();
            if (0 < fileList.size()) {
                for (File file : fileList) {
                    obj.fileAccess(file.getName());
                }
            }
        } 
        // 지정한 파일로 진행
        else {
            System.out.println("file search start...");
            for (int i=0; i<args.length; i++) {
                if (args[i].endsWith(".sql")) {
                    System.out.format("%s export start%n", i, args[i]);
                    obj.fileAccess(args[i]);
                }
            }
        }

        System.out.println("end");
    }

    void fileAccess(String filePath) {
        class FileManager {
            String readFile(String filePath) {
                String modeltext = null;
                try {
                    File file = new File(filePath);
                    int pos = file.getName().lastIndexOf('.');
                    String modelName = file.getName().substring(0, pos);
                    modelName = modelName.replaceAll("toptoon_", "");
                    modeltext = getHeaderText(modelName);
                    modeltext += addLeftEmptyString("\"" + "properties" + "\": {" , 2);
                    BufferedReader reader = null;
                    System.out.println("file read start");
                    reader = new BufferedReader(new FileReader(file));
                    String line = reader.readLine();
                    boolean isTableSchema = false;
                    boolean isFirstColWrite = false;
                    while(line != null) {
                        // model 구분
                        if (line.startsWith("CREATE TABLE")) {
                            line = reader.readLine();
                            isTableSchema = true;
                        }

                        // model 정보 추출
                        if (isTableSchema) {
                            line = line.trim();
                            if (line.startsWith("PRIMARY KEY") || line.startsWith("UNIQUE")) {
                                System.out.println("file read end");
                                break;
                            }
                            if (isFirstColWrite) modeltext += ",\n";
                            String text = parsingText(line, isFirstColWrite);
                            modeltext += text;
                            isFirstColWrite = true;
                        }
                        line = reader.readLine();
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                }
                modeltext += "\n"; 
                modeltext += getFooterText();
                System.out.println(modeltext);
                return modeltext;
            }

            void writeFile(String fileName, String modeltext) {
                try {
                    System.out.println("file write start");
                    File dir = new File("./export");
                    if (!dir.exists()) {
                        System.out.println("export directory create");
                        dir.mkdirs();
                    }

                    File file = new File(filePath);
                    int pos = file.getName().lastIndexOf('.');
                    String filePath = dir.getName() + "/" + file.getName().substring(0, pos) + ".json";
                    filePath = filePath.replaceAll("toptoon_", "");
                    filePath = filePath.replaceAll("_", "-");
                    BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
                    out.write(modeltext);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } 
                System.out.println("file write end");
            }
        }
        FileManager fileManager = new FileManager();
        fileManager.writeFile(filePath, fileManager.readFile(filePath));
    }

    // // 현재경로의 sql 파일 리스트 획득 
    private static ArrayList<File> getFileList() {
        File nowDir = new File("./");
        ArrayList<File> sqlFileList = new ArrayList<File>();

        System.out.println("file path search...");
        for (File file : nowDir.listFiles()) {
            if (file.getName().endsWith(".sql")) {
                sqlFileList.add(file);
            }
        }
        System.out.println("total search : " + sqlFileList.size());
        return sqlFileList;
    }

    public static String getType(String value) {
           
            // date type도 number로 처리
            if (isDateValue(value)) {
                return "number";
            } else if (isIntValue(value)) {
                return "number";
            }  else {
                return  "string";
            }
    }

     private static boolean isIntValue(String value) {

            if (value.contains("int")) return true;
            else if (value.contains("float")) return true;
            else if (value.contains("serial")) return true;
            else if (value.contains("time")) return true;

            return false;
    }

    private static boolean isDateValue(String value) {

            if (value.contains("date")) return true;
            if (value.contains("datetime")) return true;

            return false;
    }

    private static String getHeaderText(String modelName) {
        String text = "{\n";
        text +=  addLeftEmptyString("\"" + "name" + "\": " + "\"" + modelName + "\",", 2);
        text +=  addLeftEmptyString("\"" + "base" + "\": " + "\"" + "PersistedModel" + "\",", 2);
        text +=  addLeftEmptyString("\"" + "idInjection" + "\": " + "false,", 2);
        text +=  addLeftEmptyString("\"" + "options" + "\": " + "{", 2);
        text +=  addLeftEmptyString("\"" + "validateUpsert" + "\": " + "true", 4);
        text +=  addLeftEmptyString("},", 2);
        return text;
    }

    private static String getFooterText() {
        String text = addLeftEmptyString("},", 2);
        text +=  addLeftEmptyString("\"" + "validations" + "\": " + "[],", 2);
        text += addLeftEmptyString("\"" + "relations" + "\": " + "{}," , 2);
        text += addLeftEmptyString("\"" + "acls" + "\": " + "[]," , 2);
        text += addLeftEmptyString("\"" + "methods" + "\": " + "{}" , 2);
        text += "}";
        return text;
    }

    private static String  parsingText(String line, boolean isFirstColWrite) {
        String[] array = line.split(" ");
        
        String text =  array[0].replaceAll("`", ""); 
        String type = getType(line);
        text = addLeftEmptyString("\"" + text + "\"" + ": {", 4);
        if (!isFirstColWrite) {
            text += addLeftEmptyString("\"required\": " + "\"" + true + "\"" + ",", 6);
            text += addLeftEmptyString("\"id\": " + "\"" + true + "\"" + ",", 6);
        }
        
        if (type.contains("number")) {
            text += addLeftEmptyString("\"type\": " + "\"" + type + "\"" + ",", 6);
            text += addLeftEmptyString("\"default:\": 0", 6); 
        } else {
            text += addLeftEmptyString("\"type\": " + "\"" + type + "\"", 6);
        }

        text += "     }";
        return text;
    }

    private static String addLeftEmptyString(String text, int len) {
        String emptyString = "";
        for (int i=0; i<=len; i++) {
            emptyString += " ";
        }
        text = emptyString + text + "\n";
        return text;
    }
}