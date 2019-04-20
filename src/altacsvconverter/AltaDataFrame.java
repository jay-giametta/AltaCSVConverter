package altacsvconverter;

import java.time.Year;
import java.time.LocalDate;
import java.util.Arrays;

public class AltaDataFrame {
    
    //altaMatrix Headers
    private final int TIME = 0;
    private final int IMGAP = 3;
    private final int CMD1 = 4;
    private final int CMD2 = 5;
    private final int STS1 = 6;
    private final int STS2 = 7;
    private final int DATA = 8;
    private final int CDP = 40;
    
    //Used for date string decomposition
    private final int YEAR = 1;
    private final int DAY = 3;
    private final int HR = 4;
    private final int MIN = 5;
    private final int SEC = 6;
    private final int MS = 7;
    
    private int length = 0;             //stores the length of the alta csv
    
    private String[][] altaMatrix;      //holds all data passed from the alta csv
    private String[][] messageFlow;     //message directing either transmit or receive
    private String[][] messageValidity; //results of the RT's message validation test    
    private String[][] RTAddress;       //the RT receiving the message
    private String[][] RTSubAddress;    //the sub-address used by the RT
   
    private String[] dataWordCount;     //the number of data words present
    private String[] dataWords;         //hex representation of data words passed
    private String[] messageGap;        //intermessage gap in microseconds
    private String[] messageErrors;     //any errors are concatenated in this string
    private String[] messageType;       //describes the message (ex. RTRT, BCRT, etc)
    private String[] modeCodeDesc;      //mode code description
    private String[] modeCodeWord;      //data word used with mode code
    private String[] messageBus;        //bus 'A' or 'B' info
    private String[] timeStamp;         //datetime stamp for message

    private int[] modeCode;             //int representation of a mode code
    
    public AltaDataFrame(String[][] inputMatrix){
         
        length = inputMatrix.length;
        
        altaMatrix = inputMatrix;
        messageFlow = new String[2][length];
        messageValidity = new String[2][length];
        RTAddress = new String[2][length];
        RTSubAddress = new String[2][length];

        dataWordCount = new String[length];         
        dataWords = new String[length];
        messageBus = new String[length];
        messageErrors = new String[length];
        messageGap = new String[length];
        messageType = new String[length];  
        modeCodeDesc = new String[length]; 
        modeCodeWord = new String[length]; 
        timeStamp = new String[length];
        
        modeCode = new int[length]; 
           
        Arrays.fill(messageErrors,"");
        Arrays.fill(messageType,"");
        Arrays.fill(modeCodeDesc, "UNUSED");
        Arrays.fill(modeCodeWord, "UNUSED");
        Arrays.fill(modeCode,-1);
        
        calculateDates();
        parseBinaryandHex();
        parseCDPWord();
        parseCommandAndStatusWords();
    }
    //parses 1553 command word (Ref MIL-STD-1553, p. 6)
    private void parseCommandAndStatusWords(){
        for (int row=0;row < length;row++){
            
            //read the RT address from the command word
            RTAddress[0][row]=Integer.toString(Integer.
                    parseInt(altaMatrix[row][CMD1].substring(0,5),2));
            
            //read the flow direction from the command word
            if (altaMatrix[row][CMD1].charAt(5)=='1')messageFlow[0][row]="TRANSMIT";
            else messageFlow[0][row]="RECEIVE";
            
            //read the SA from the command word
            RTSubAddress[0][row]=Integer.toString(Integer.
                    parseInt(altaMatrix[row][CMD1].substring(6,11),2));
            
            //check RT message validation result / STS only valid if not BRDCST
            if (altaMatrix[row][STS1].charAt(5)=='1' && altaMatrix[row][CDP].
                    charAt(0)!='1')messageValidity[0][row]="FAILED RT TEST";
            else messageValidity[0][row]="VALID";
            
            messageGap[row]=Double.toString(Double.parseDouble(altaMatrix[row][IMGAP])/10);
            
            //Only show the values for CMD2 if RT-RT communications otherwise they're padded
            if (altaMatrix[row][CDP].charAt(2)=='1'){
                RTAddress[1][row]=Integer.toString(Integer.
                        parseInt(altaMatrix[row][CMD2].substring(0,5),2));
                RTSubAddress[1][row]=Integer.toString(Integer.
                        parseInt(altaMatrix[row][CMD2].substring(6,11),2));
                
                if (altaMatrix[row][CMD2].charAt(5)=='1')messageFlow[1][row]="TRANSMIT";
                else messageFlow[1][row]="RECEIVE";
                
                if (altaMatrix[row][STS2].charAt(5)=='1' && altaMatrix[row][CDP].
                        charAt(0)!='1')messageValidity[1][row]="FAILED RT TEST";
                else messageValidity[1][row]="VALID";
            }
            
            else {
                messageFlow[1][row]="UNUSED";
                RTAddress[1][row]="UNUSED";
                RTSubAddress[1][row]="UNUSED";
                messageValidity[1][row]="VALID";
            }
            
            //Handle optional mode codes (Ref MIL-STD-1553, p.10)
            if (altaMatrix[row][CDP].charAt(1)=='1'){
                modeCode[row]=Integer.parseInt(altaMatrix[row][CMD1].substring(11),2); 
                
                switch(modeCode[row]){
                    case 0:  modeCodeDesc[row] = "DYNAMIC BUS CTRL"; break;
                    case 1:  modeCodeDesc[row] = "SYNC"; break;
                    case 2:  modeCodeDesc[row] = "XMIT STATUS WORD"; break;
                    case 3:  modeCodeDesc[row] = "INIT SELF TEST"; break;
                    case 4:  modeCodeDesc[row] = "XMIT SHUT"; break;
                    case 5:  modeCodeDesc[row] = "OVERRIDE XMIT SHUT"; break;
                    case 6:  modeCodeDesc[row] = "INHIBIT TERM FLAG BIT"; break;
                    case 7:  modeCodeDesc[row] = "OVERRIDE INHIBIT TERM FLAG BIT"; break;
                    case 8:  modeCodeDesc[row] = "RESET REMOTE TERM"; break;
                    case 16: modeCodeDesc[row] = "XMIT VECTOR WORD"; break;
                    case 17: modeCodeDesc[row] = "SYNC"; break;
                    case 18: modeCodeDesc[row] = "XMIT LAST CMD"; break;
                    case 19: modeCodeDesc[row] = "XMIT BIT WORD"; break;
                    case 20: modeCodeDesc[row] = "SELECTED XMIT SHUT"; break;
                    case 21: modeCodeDesc[row] = "OVERRIDE SELECTED XMIT SHUT"; break;
                }
                
                if (altaMatrix[row][CMD1].charAt(11)=='1')modeCodeWord[row] = dataWords[row].substring(0,4);
            }
        }
    }
    //parses CDP Status Word (Ref AltaView Users Manual, p. 211)
    private void parseCDPWord() {
        for (int row=0;row < length;row++){
                        
            //check message type
            if (altaMatrix[row][CDP].charAt(1)=='1')messageType[row]="MODE_CODE";
            if (altaMatrix[row][CDP].charAt(2)=='1')messageType[row]=messageType[row].concat("RT-RT");
            if (altaMatrix[row][CDP].charAt(3)=='1')messageType[row]=messageType[row].concat("RT-BC");
            if (altaMatrix[row][CDP].charAt(4)=='1')messageType[row]=messageType[row].concat("BC-RT");
            
            //add broadcast info if present
            if (altaMatrix[row][CDP].charAt(0)=='1')messageType[row]=messageType[row].concat(" BROADCAST");
            
            //check for errors
            if (altaMatrix[row][CDP].charAt(16)=='1')messageErrors[row]="NO ERROR";
            if (altaMatrix[row][CDP].charAt(5)=='1')messageErrors[row]="SPURIOUS";
            if (altaMatrix[row][CDP].charAt(15)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("COMPARE");
            }
            if (altaMatrix[row][CDP].charAt(17)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("SYNC");
            }
            if (altaMatrix[row][CDP].charAt(18)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("BIT_ERROR");
            }
            if (altaMatrix[row][CDP].charAt(19)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("PARITY");
            }
            if (altaMatrix[row][CDP].charAt(20)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("WORD_COUNT");
            }
            if (altaMatrix[row][CDP].charAt(21)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("NO_RESPONSE");
            }
            if (altaMatrix[row][CDP].charAt(22)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("WRONG_RT");
            }
            if (altaMatrix[row][CDP].charAt(23)=='1'){
                if (messageErrors[row].length() > 1)messageErrors[row]=messageErrors[row].concat(" ");
                messageErrors[row]=messageErrors[row].concat("TWO_BUS");
            }
            
            //check bus assignment
            if (altaMatrix[row][CDP].charAt(25)=='1')messageBus[row]="A";
            else messageBus[row]="B";
            
            //calculate number of message words
            dataWordCount[row]=Integer.toString(Integer.parseInt(altaMatrix[row][CDP].substring(26),2));
            if (Integer.parseInt(dataWordCount[row]) > 0 && Integer.parseInt(dataWordCount[row]) < 32) {dataWords[row]=dataWords[row].
                    substring(0,Integer.parseInt(dataWordCount[row])*5-1);}
            else if (Integer.parseInt(dataWordCount[row]) == 0){
                dataWords[row]="";
            }
        }
    }
    //converts integer values provided by alta to more meaningful data formats
    private void parseBinaryandHex(){
        //convert CMD and STS words from int values to 16-bit binary strings
        for (int row=0;row < length;row++){
            for (int col=CMD1;col < DATA;col++){
                altaMatrix[row][col] = String.format("%16s", Integer.toBinaryString(
                        Integer.parseInt(altaMatrix[row][col]))).replace(" ", "0");
            }
        }
        
        //convert DATA words to HEX and add separators between each 16-bits
        for (int row=0;row < length;row++){
            for (int col=DATA;col <= DATA+31;col++){
                if (col==DATA) {
                    dataWords[row] = String.format("%4s", Integer.toHexString(
                        Integer.parseInt(altaMatrix[row][col]))).replace(" ", "0").toUpperCase();
                }
                
                else {
                    dataWords[row] = dataWords[row].
                            concat(" ").concat(String.format("%4s", Integer.toHexString(
                        Integer.parseInt(altaMatrix[row][col]))).replace(" ", "0").toUpperCase());
                }
            }
        }
         
        //convert CDP words from int values to 32-bit binary strings
        for (int row=0;row < length;row++){
            altaMatrix[row][CDP] = String.format("%32s", Long.toBinaryString(
                    Long.parseLong(altaMatrix[row][CDP]))).replace(" ", "0");
        }
    }
    //converts from [YYYY](Calendar Day)HH:MM:SS.sss.000.000 to ISO 8601 format
    private void calculateDates(){
       
        for(int row=0;row<length;row++){
            
            String[] dateSplit = altaMatrix[row][TIME].split("\\[|\\]|\\(|\\)|:|\\.");
            LocalDate date = Year.of(Integer.parseInt(dateSplit[YEAR])).
                    atDay(Integer.parseInt(dateSplit[DAY]));
            timeStamp[row]= date.atTime(Integer.parseInt(dateSplit[HR]),
                    Integer.parseInt(dateSplit[MIN]), 
                    Integer.parseInt(dateSplit[SEC]), 
                    Integer.parseInt(dateSplit[MS])*1000000).toString();
        }  
    }
    //output calculated matrices
    public String[][] getOutputMatrix(){
        String[][] outputMatrix = {
            timeStamp,
            messageType,
            messageBus,
            RTAddress[0],
            RTAddress[1],
            RTSubAddress[0],
            RTSubAddress[1],
            messageFlow[0],
            messageFlow[1],
            messageGap,
            messageValidity[0],
            messageValidity[1],
            messageErrors,
            modeCodeDesc,
            modeCodeWord,
            dataWordCount,
            dataWords
        };
        return outputMatrix;
    }

}
