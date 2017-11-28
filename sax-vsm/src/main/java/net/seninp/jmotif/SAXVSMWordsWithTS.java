package net.seninp.jmotif;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This implements a class to find top K projected into original TS.
 * 
 * @author sayantan & gajanana
 * 
 */

public class SAXVSMWordsWithTS {
	
	public double [] ts;
	public String class_label;
	public HashMap<String, Integer > words_with_time;  
	public HashMap<Integer,String > diab_topK;  
	public HashMap<Integer,String > healthy_topK;  
	public static HashMap<Integer,Integer > BOW_DISTRBN_DIAB ;
	public static HashMap<Integer,Integer > BOW_DISTRBN_HEALTHY ;
	
	
	public String file_location="//home/sayantan/Desktop/data/diab/TS/";
	
	public SAXVSMWordsWithTS(int ts_length) {
		
		BOW_DISTRBN_DIAB = new HashMap<Integer,Integer>();
		BOW_DISTRBN_HEALTHY = new HashMap<Integer,Integer>();
		for(int i=0;i<ts_length;i++){
			BOW_DISTRBN_DIAB.put(i+1, 0);
			BOW_DISTRBN_HEALTHY.put(i+1, 0);
		}
		
	}
	
	public SAXVSMWordsWithTS() {
	super();
	}
	
	public SAXVSMWordsWithTS(double[] ts, String class_label, HashMap<String, Integer> words_with_time,
			HashMap<Integer,String > diab_topK,HashMap<Integer,String > healthy_topK) {
		super();
		this.ts = ts;
		this.class_label = class_label;
		this.words_with_time = words_with_time;
		
		this.diab_topK = diab_topK;
		this.healthy_topK = healthy_topK;
	}



	public HashMap<String,Integer> matchTopKWordsnPrint(double[] series, String label, int classfn_result, HashMap<String, Integer> words_with_tme, 
			int time_stamp_of_last_word, String train_test_label,int count) throws IOException {
		// TODO Auto-generated method stub
		
		int ts_length = 0;
		int total_word_counts_with_time=0;
		HashMap<String, Integer> word_postion_in_original_TS = new HashMap<>();
		
		ts_length = series.length;
		total_word_counts_with_time = time_stamp_of_last_word;
		//file_location="//home/sayantan/Desktop/data/diab/TS/";
		BufferedWriter CSV_filename_locatn = new BufferedWriter(new FileWriter(file_location+train_test_label+"/"+count+".csv"));
		
		
		//testing data, i.e. printing while classfying
		String word=null;
		String file_output=null;
			
			
			int word_temporal_position=-1;
			double word_postion_in_TS=0;
			
			for(Map.Entry<String, Integer> f: words_with_tme.entrySet()){ // enter the BOW for this specific TS
				word = f.getKey(); // a particular SAX-VSM word, in temporal order
				word_temporal_position = f.getValue(); // equivalent temporal position of the word in word series
				
				if(label.equals("1"))//check if diab, then check with topK diab words
				{
				//now check if this word appears in top-K Diab word, for that, enter into loop of top K diab words
				for(Map.Entry<Integer, String> d: diab_topK.entrySet()){
					if(word.equals(d.getValue())){
						//Bingo!, calculate the equivalent position of this candidate in the original TS
						word_postion_in_TS=0;
						word_postion_in_TS = (float) ((double)word_temporal_position/(double)total_word_counts_with_time)*(double)ts_length;
						word_postion_in_TS = Math.round(word_postion_in_TS);
						file_output = word + "," + (word_postion_in_TS);
						String temp=null;
						temp = String.valueOf(word_postion_in_TS);
						word_postion_in_original_TS.put(d.getValue(), (int) word_postion_in_TS);//added for final requirement to get ALL TS
						//System.out.println("Match with Top K Diab Word found in TS Numbr="+count+" The Word is "+d.getValue());
						CSV_filename_locatn.append(file_output);
						CSV_filename_locatn.newLine();
						
						//update global map for diabetes - to manage count of word at each temporal position
						for(Map.Entry<Integer, Integer> diab : BOW_DISTRBN_DIAB.entrySet()){
							if(diab.getKey()==word_postion_in_TS){
								int val = diab.getValue();
								BOW_DISTRBN_DIAB.put(diab.getKey(), val+1);
							}
						}
					}
				}
			}else if(label.equals("0")){
				//now check if this word appears in top-K Healthy word, for that, enter into loop of top K diab words
				for(Map.Entry<Integer, String> h: healthy_topK.entrySet()){
					if(word.equals(h.getValue())){
						//Bingo!, calculate the equivalent position of this candidate in the original TS
						word_postion_in_TS=0;
						word_postion_in_TS = (float) ((double)word_temporal_position/(double)total_word_counts_with_time)*(double)ts_length;
						word_postion_in_TS = Math.round(word_postion_in_TS);
						file_output = word + "," + (word_postion_in_TS);
						//System.out.println("Match with Top K Healthy Word found in TS Numbr="+count+" The Word is "+h.getValue());
						CSV_filename_locatn.append(file_output);
						CSV_filename_locatn.newLine();
						
						
						String temp=null;
						temp = String.valueOf(word_postion_in_TS);
						word_postion_in_original_TS.put(h.getValue(), (int) word_postion_in_TS);//added for final requirement to get ALL TS
						
						//update global map for diabetes - to manage count of word at each temporal position
						for(Map.Entry<Integer, Integer> healthy : BOW_DISTRBN_HEALTHY.entrySet()){
							if(healthy.getKey()==word_postion_in_TS){
								int val = healthy.getValue();
								BOW_DISTRBN_HEALTHY.put(healthy.getKey(), val+1);//increase count
							}
						}
					}
				}
			}
				
				
				
			}
			
		
		CSV_filename_locatn.close();
		return word_postion_in_original_TS;//needed for printing ALL TS- as per last requirement
	}
	
	public void printGlobalBOWs() throws IOException{
		//print the Global maps
		//file_location="//home/sayantan/Desktop/data/diab/TS/";
		
		BufferedWriter diab_locatn = new BufferedWriter(new FileWriter(file_location+"BOW/"+"DIAB.csv"));
		BufferedWriter healthy_locatn = new BufferedWriter(new FileWriter(file_location+"BOW/"+"HEALTHY.csv"));
		
		for(Map.Entry<Integer, Integer> dia : BOW_DISTRBN_DIAB.entrySet()){
			diab_locatn.append(String.valueOf(dia.getKey())+","+String.valueOf(dia.getValue()));
			diab_locatn.newLine();
			
		}
		
		for(Map.Entry<Integer, Integer> hl : BOW_DISTRBN_HEALTHY.entrySet()){
			healthy_locatn.append(String.valueOf(hl.getKey())+","+String.valueOf(hl.getValue()));
			healthy_locatn.newLine();
			
		}
		
		diab_locatn.close();
		healthy_locatn.close();
	}
	
	
	
	
	
	

}
