package net.seninp.jmotif.text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TestClass {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		double d1=-0.8867;
		int j=0;
		
		j = (int) d1;
		
		System.out.println(" j ="+j);
		
	BufferedWriter bufWr = new BufferedWriter(new FileWriter("//home/sayantan/Desktop/topWords.csv"));
		for (int i = 0; i < 10; i++) {
			String output = "Result is "+ i+", For Rownum "+i;
			bufWr.append(output);
			bufWr.newLine();
	    }
		bufWr.close();

	}

}
