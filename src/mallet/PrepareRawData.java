package mallet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class PrepareRawData {
	public static String input = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\Classifier Training Set 8-11.csv";
	public static String output = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\LDA\\Classifier Training Set 2015.txt";
	
	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(input));
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(output)));
		String line = br.readLine();	//title line
		
		while((line = br.readLine()) != null) {
			String[] thisline = line.split(",");
			if(thisline.length > 2) {
				for(int i=2; i<thisline.length-1; i++)
					thisline[1] += thisline[i];
			}
			dos.writeBytes(thisline[0] + "\t" + "X" + "\t" + thisline[1] + "\r\n");
		}
		
		br.close();		
		dos.close();
	}
}
