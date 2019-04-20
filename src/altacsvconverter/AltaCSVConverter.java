/* Author: Jay Giametta
 * Date: 21 Mar 19
 * Purpose: Converts AltaView Bus Analyzer .csv files to json
 */

package altacsvconverter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class AltaCSVConverter {
    
    public static void main(String[] args) {
        
        String folderName;
        boolean json;
        boolean csv;
        
        //output file headers
        String[] headers = {
            "timestamp" ,
            "messageType" ,
            "messageBus" ,
            "RTAddress_0" ,
            "RTAddress_1" ,
            "RTSubAddress_0" ,
            "RTSubAddress_1" ,
            "messageFlow_0" ,
            "messageFlow_1" ,
            "messageGap" ,
            "messageValidity_0" ,
            "messageValidity_1" ,
            "messageErrors" ,
            "modeCodeDesc" ,
            "modeCodeWord" ,
            "dataWordCount" ,
            "dataWords"
        };
                
        if (args.length == 0){
            folderName = ".";
            json = true;
            csv = false;
        }
        
        else folderName = args[0];
                
        File[] files = new File(folderName).listFiles(new FilenameFilter(){ 
            @Override public boolean accept(File dir, String name) 
            { return name.endsWith(".csv"); } });
        if (files==null) System.out.println("Folder '" + args[0] + "' does not exist.");
        else{
            if (files.length==0) System.out.println(
                    "No csv files found. This program runs in the current directory "
                            + "unless a path is specified as an argument.");

            for (File file : files){

                String fileName = file.getName();
                String[][] altaMatrix = AltaCSVToMatrix(folderName + "/" + fileName);

                AltaDataFrame dataFrame = new AltaDataFrame(altaMatrix); 
                String[][] DFMatrix = dataFrame.getOutputMatrix();

                //DFMatrixToCSV(DFMatrix,headers,altaMatrix.length,folderName);
                DFMatrixToJSON(DFMatrix,headers,altaMatrix.length,folderName);
            }
        }
        
    }
    //writes the dataframe to .json
    private static void DFMatrixToJSON(String[][] DFMatrix, String[] headers, int numLines, String folderName){
                    
        FileWriter json;
        
        try{
            //create output directory if it doesn't exist
            Path outputPath = Paths.get(folderName + "/alta1553");
            if (Files.notExists(outputPath)){
                File outputDir = new File(folderName + "/alta1553");
                outputDir.mkdir();
            }
            //output files using predefined header with timestamp
            String filePath = folderName + "/alta1553/alta1553_" + DFMatrix[0][0].substring(0,19).replace(":","") + ".json";
            json = new FileWriter(new File(filePath));
            
            System.out.println("Writing " + filePath + "...");
                       
            //output matrix contents
            for(int row=0;row<numLines;row++){
                json.write("{");
                for(int col=0;col<DFMatrix.length;col++){
                    json.write("\"" + headers[col] + "\":\"");
                    json.write(DFMatrix[col][row] + "\"");
                    if(col!=DFMatrix.length-1) json.write(",");
                }            
                json.write("}" + System.lineSeparator());
            }

            json.close();
            
        } catch (Exception message){
            message.printStackTrace();
        }
    }
    //writes the dataframe to .csv
    private static void DFMatrixToCSV(String[][] DFMatrix, String[] headers, int numLines, String folderName){
        FileWriter csv;
        
        try{
            //create output directory if it doesn't exist
            Path outputPath = Paths.get(folderName + "/alta1553");
            if (Files.notExists(outputPath)){
                File outputDir = new File(folderName + "/alta1553");
                outputDir.mkdir();
            }
            //output files using predefined header with timestamp
            String filePath = folderName + "/alta1553/alta1553_" + DFMatrix[0][0].substring(0,19).replace(":","") + ".csv";
            csv = new FileWriter(new File(filePath));
            
            System.out.println("Writing " + filePath + "...");
            
            //create header row
            for(int col=0;col < headers.length;col++){
                csv.write(headers[col]);
                if(col!=headers.length -1) csv.write(",");
            }
            
            csv.write(System.lineSeparator());
            //output matrix contents
            for(int row=0;row<numLines;row++){            
                for(int col=0;col<DFMatrix.length;col++){
                    csv.write(DFMatrix[col][row]);
                    if(col!=DFMatrix.length-1) csv.write(",");
                }          
                csv.write(System.lineSeparator());
            }
            csv.close();
            
        } catch (Exception message){
            message.printStackTrace();
        }
    }
    //Reads an Alta .csv and imports it as a 2D matrix of strings
    private static String[][] AltaCSVToMatrix(String altaFileName) {
        
        File altaFile = new File(altaFileName);
        String[][] altaMatrix = new String[1][1];
        
        try{
            //Get the number of lines in the file to initialize the correct 
            //number of rows
            Path altaPath = Paths.get(altaFileName);
            long altaFileLines = Files.lines(altaPath).count();
            altaMatrix = new String[(int)altaFileLines-1][];
            
            //Open an input stream for the .csv
            Scanner altaStream = new Scanner(altaFile);
            
            //Counter for matrix population
            int matrixRow = 0;
            
            //Pop the first line of the stream
            altaStream.nextLine();
            
            //Read each line as a new row in the matrix
            while (altaStream.hasNextLine()){     
                altaMatrix[matrixRow] = altaStream.nextLine().split(", |,");
                matrixRow++;
            }
            altaStream.close();
        //I don't do error handling :/
        } catch (Exception message){
            message.printStackTrace(); 
        }
        //Return 2D 
        return altaMatrix;
    }
    
}
