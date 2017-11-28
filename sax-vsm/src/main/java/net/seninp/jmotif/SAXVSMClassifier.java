package net.seninp.jmotif;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import net.seninp.jmotif.sax.SAXException;
import net.seninp.jmotif.text.Params;
import net.seninp.jmotif.text.TextProcessor;
import net.seninp.jmotif.text.TopKWord;
import net.seninp.jmotif.text.WordBag;
import net.seninp.util.StackTrace;
import net.seninp.util.UCRUtils;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;

/**
 * This implements a classifier.
 * 
 * @author psenin
 * 
 */
public class SAXVSMClassifier {

	private static final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
	private static DecimalFormat fmt = new DecimalFormat("0.00###", otherSymbols);
	private static final Object CR = "\n";
	private static final String COMMA = ", ";

	private static TextProcessor tp = new TextProcessor();

	private static Map<String, List<double[]>> trainData;
	private static Map<String, List<double[]>> testData;
	
	public static int ts_length=0;

	// static block - we instantiate the logger
	//
	private static final Logger consoleLogger;
	private static final Level LOGGING_LEVEL = Level.INFO;

	static {
		consoleLogger = (Logger) LoggerFactory.getLogger(SAXVSMClassifier.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}

	public static void main(String[] args) throws SAXException, IOException {

		try {

			SAXVSMClassifierParams params = new SAXVSMClassifierParams();
			JCommander jct = new JCommander(params, args);

			if (0 == args.length) {
				jct.usage();
				System.exit(-10);
			}

			StringBuffer sb = new StringBuffer(1024);
			sb.append("SAX-VSM Classifier").append(CR);
			sb.append("parameters:").append(CR);

			sb.append("  train data:                  ").append(SAXVSMClassifierParams.TRAIN_FILE).append(CR);
			sb.append("  test data:                   ").append(SAXVSMClassifierParams.TEST_FILE).append(CR);
			sb.append("  SAX sliding window size:     ").append(SAXVSMClassifierParams.SAX_WINDOW_SIZE).append(CR);
			sb.append("  SAX PAA size:                ").append(SAXVSMClassifierParams.SAX_PAA_SIZE).append(CR);
			sb.append("  SAX alphabet size:           ").append(SAXVSMClassifierParams.SAX_ALPHABET_SIZE).append(CR);
			sb.append("  SAX numerosity reduction:    ").append(SAXVSMClassifierParams.SAX_NR_STRATEGY).append(CR);
			sb.append("  SAX normalization threshold: ").append(SAXVSMClassifierParams.SAX_NORM_THRESHOLD).append(CR);

			trainData = UCRUtils.readUCRData(SAXVSMClassifierParams.TRAIN_FILE);
			consoleLogger.info("trainData classes: " + trainData.size() + ", series length: "
					+ trainData.entrySet().iterator().next().getValue().get(0).length);
			for (Entry<String, List<double[]>> e : trainData.entrySet()) {
				consoleLogger.info(" training class: " + e.getKey() + " series: " + e.getValue().size());
			}
			
			ts_length = 210;//trivial change: can be obtained from above values also!

			testData = UCRUtils.readUCRData(SAXVSMClassifierParams.TEST_FILE);
			consoleLogger.info("testData classes: " + testData.size() + ", series length: "
					+ testData.entrySet().iterator().next().getValue().get(0).length);
			for (Entry<String, List<double[]>> e : testData.entrySet()) {
				consoleLogger.info(" test class: " + e.getKey() + " series: " + e.getValue().size());
			}

		} catch (Exception e) {
			System.err.println("There was an error...." + StackTrace.toString(e));
			System.exit(-10);
		}
		Params params = new Params(SAXVSMClassifierParams.SAX_WINDOW_SIZE, SAXVSMClassifierParams.SAX_PAA_SIZE,
				SAXVSMClassifierParams.SAX_ALPHABET_SIZE, SAXVSMClassifierParams.SAX_NORM_THRESHOLD,
				SAXVSMClassifierParams.SAX_NR_STRATEGY);
		classify(params);
	}

	private static void classify(Params params) throws SAXException, IOException {
		// making training bags collection
		List<WordBag> bags = tp.labeledSeries2WordBags(trainData, params);
		// getting TFIDF done
		HashMap<String, HashMap<String, Double>> tfidf = tp.computeTFIDF(bags);

		/*
		 * Steps to print highest tf-idf weights
		 */
		Map<Double, String> printMap = new TreeMap<Double, String>();

		String class_l = null;
		List<TopKWord> topWordsList = new ArrayList<>();
		for (Entry<String, HashMap<String, Double>> e : tfidf.entrySet()) {
			class_l = e.getKey();
			// System.out.println("e.getValue() = "+e.getValue());

			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
				TopKWord topWord = new TopKWord(class_l, e2.getKey(), e2.getValue()); // class,
																						// literal,
																						// weights
				topWordsList.add(topWord);
			}

		}

		System.out.println("Top k =5 words and weights for each class..1=Diabetic, 0=Healthy");
		//System.out.println(" ");
		Collections.sort(topWordsList, TopKWord.TopKWordsComparator);
		BufferedWriter diabCSV = new BufferedWriter(
				new FileWriter("//home/sayantan/Desktop/data/diab/topWordsDiabetic.csv"));
		BufferedWriter healthCSV = new BufferedWriter(
				new FileWriter("//home/sayantan/Desktop/data/diab/topWordsHealthy.csv"));

		String output1 = null;
		String output2 = null;

		HashMap<Integer, String> diab_top5 = new HashMap<Integer, String>();
		HashMap<Integer, String> hlty_top5 = new HashMap<Integer, String>();
		
		//initialise constructor for setting up two global static maps - that holds top K words for all data
		SAXVSMWordsWithTS blank = new SAXVSMWordsWithTS(ts_length);

		int i = 0;
		int j = 0;
		for (TopKWord t : topWordsList) {
			if (t.getClass_label().equals("1") && i < 5) {
				System.out.println(t);
				i++;
				output1 = t.getStr_literal() + "," + t.getTfIDFWeights();
				diab_top5.put(i, t.getStr_literal());
				diabCSV.append(output1);
				diabCSV.newLine();
			}

			if (t.getClass_label().equals("0") && j < 5) {
				System.out.println(t);
				j++;
				output2 = t.getStr_literal() + "," + t.getTfIDFWeights();
				hlty_top5.put(j, t.getStr_literal());
				healthCSV.append(output2);
				healthCSV.newLine();

			}
		}
		diabCSV.close();
		healthCSV.close();

		
		int testSampleSize = 0;
		int positiveTestCounter = 0;
		int temp_result = -1;

		int diab_clsfn_pos = 0;
		int diab_clsfn_neg = 0;

		int healthy_clsfn_pos = 0;
		int healthy_clsfn_neg = 0;
		
		
		//HashMap<String, Integer> words_with_timeStamp = new HashMap<String, Integer>();
		HashMap<String, HashMap<String, Integer>> topTimeSeries = new HashMap<String,HashMap<String, Integer>>();
		
		int count=0;
		// classifying test data here:
		for (String label : tfidf.keySet()) {
			List<double[]> testD = testData.get(label);
			//System.out.println("testD length =" + testD.size() + " for label =" + label);
			for (double[] series : testD) {
				count++;
				//System.out.println("series =" + series);
				//System.out.println("label =" + label + " postive counter =" + positiveTestCounter);
				// positiveTestCounter = positiveTestCounter
				// + tp.classify(label, series, tfidf, params);
				temp_result = tp.classify(label, series, tfidf, params);
				positiveTestCounter = positiveTestCounter + temp_result;
				
				WordBag test = tp.seriesToWordBag("test", series, params);
				HashMap<String, Integer> test_words = new HashMap<String, Integer>();//bag of words
				HashMap<String, Integer> words_with_time_test = new HashMap<String, Integer>();//bag of words with time
				int time_stamp_of_last_word =-1;
				
				test_words = test.getWords();
				words_with_time_test = test.getWords_with_time();
				time_stamp_of_last_word = test.getTimePosition_Of_last_word();
				
				
				SAXVSMWordsWithTS createTS_CSV = new SAXVSMWordsWithTS(series, label, words_with_time_test,diab_top5,hlty_top5);
		//		System.out.println("");
		//		System.out.println("Mapping Test TS to Topk Words");
				// this method is important, this method tests exactly, at which point in time
                //in the TS, a top word occurs in test data and writes it in csv for plotting in R
				
				HashMap<String, Integer> top_words_projected = new HashMap<String, Integer>();
                top_words_projected= createTS_CSV.matchTopKWordsnPrint(series,label,temp_result, words_with_time_test,
						time_stamp_of_last_word,"test",count);
				
				//add to global map 
				topTimeSeries.put("Test"+count, top_words_projected);
				
                //System.out.println("Classfying test data, for Time Series Numbr "+count+" No of words = "+test_words.size());
				if (label.equals("1") && temp_result == 1) { // Label=Diab,Predicted=Correct,// i.e. Diab
					diab_clsfn_pos++;// tp
					//find if the top diab word occurs in the time series
				} else if (label.equals("1") && temp_result == 0) {// Label=Diab, Predicted=Wrong,.i.e.Healthy
					diab_clsfn_neg++;// fn
				} else if (label.equals("0") && temp_result == 1) { // Label=Healthy, Predicted=Correct, i.e. Healthy
					healthy_clsfn_pos++;// tn
				} else if (label.equals("0") && temp_result == 0) {// Label=Healthy, Predicted=Wrong, i.e. Diab
					healthy_clsfn_neg++;// fp
				}
				testSampleSize++;

			}
		}
		
		//Top K Words on Training Data - only for projection and printing
		count=0;
		
		for (String label_train : tfidf.keySet()) {
			List<double[]> trainD = trainData.get(label_train);
			//System.out.println("testD length =" + testD.size() + " for label =" + label);
			for (double[] series : trainD) {
				count++;
								
				WordBag train_d = tp.seriesToWordBag("train", series, params);
				HashMap<String, Integer> train_words = new HashMap<String, Integer>();
				HashMap<String, Integer> words_with_time_train = new HashMap<String, Integer>();
				
				train_words = train_d.getWords();
				words_with_time_train = train_d.getWords_with_time();
				int time_stamp_of_last_word = train_d.getTimePosition_Of_last_word();
				
                //System.out.println("Just Printing, Trng Data,  Time Series Numbr "+count+" No of Words ="+train_words.size());
                SAXVSMWordsWithTS createTS_CSV = new SAXVSMWordsWithTS(series, label_train, words_with_time_train,diab_top5,hlty_top5);
                int classifn_result=1; //since this is trng data, and we know the class label for sure
               // System.out.println("");
               // System.out.println("Mapping Training TS to Topk Words");
                // this method is important, this method tests exactly which point in time
                //in the TS, a top word occurs in trng data and writes it in csv for plotting in R
                HashMap<String, Integer> top_words_projected = new HashMap<String, Integer>();
                top_words_projected = createTS_CSV.matchTopKWordsnPrint(series,label_train,classifn_result, 
						words_with_time_train,time_stamp_of_last_word, "train",count);
				
				topTimeSeries.put("Train"+count, top_words_projected);
					//find if the top diab word occurs in the time series
					//System.out.println("Test Data, BOW, Map Size ="+test_words.size());
		
						
				} 
			}
		
		//Get Top K Word Distribution of all data training and test data
		//This method scans all TS. Here we check with the static map (holding top K words) projected into original TS
		//This creates the whole distribution -for diabetic and healthy - which is NOT exactly what Uli wanted!
		SAXVSMWordsWithTS create_global_BOW = new SAXVSMWordsWithTS();
		create_global_BOW.printGlobalBOWs();
		
		
		/*New Method added on 12th May - 
		new requirement : Find all those time series, where a given top K word occurs exactly at same time!
		//sounds nice, isn't it? :-)
		*/
			  
		//run a loop on test data and create objects using this class for each TS
		// add the TS objects in a global map, that will be iterated against each top k-word
		String topWordOuter=null;
		String topWordInner=null;
		String TS_Num =null;
		String file_output=null;
		Map<String,String> results_TS = new HashMap<>();
		BufferedWriter CSV_All_TS_locatn = new BufferedWriter(new FileWriter("//home/sayantan/Desktop/data/diab/All_Time_Series.csv"));
		CSV_All_TS_locatn.append("TS Number"+","+"Top Diab Word"+","+"Location");//TS_Num+","+topWordInner + ","+String.valueOf(k+1);
		CSV_All_TS_locatn.newLine();
		//new FileWriter("//home/sayantan/Desktop/data/diab/topWordsHealthy.csv"));
		
		for(Map.Entry<Integer, String> m : diab_top5.entrySet()){
			topWordOuter = m.getValue();
			
			//now enter check for each position of the TS, i.e. for each 210 positions
			for(int k=0;k<ts_length;k++){
				
				//for this index, enter each time series and see, if this Top K word occured at position k
				for(Map.Entry<String, HashMap<String, Integer>> all_TS : topTimeSeries.entrySet()){//topTimeSeries = will have test+train, 744
					//sift thru all words
					TS_Num = all_TS.getKey();//Key = "test1" or "train544";
					Map<String,Integer> word_with_time = new HashMap<>();
					word_with_time = all_TS.getValue();//this has the top word projected in original TS, 210 space
					
					for(Map.Entry<String, Integer> single_TS : word_with_time.entrySet()){
						topWordInner = single_TS.getKey();
					if(topWordInner.equals(topWordOuter) && single_TS.getValue()==k+1){//if the words match and they are at same position
						results_TS.put(TS_Num, "Word :"+topWordInner+"~Located At~"+String.valueOf(k+1));
				//		System.out.println(" For "+TS_Num+" Top Word :"+topWordInner+"~Located At~"+String.valueOf(k+1));
						file_output = TS_Num+","+topWordInner + ","+String.valueOf(k+1);
						CSV_All_TS_locatn.append(file_output);
						CSV_All_TS_locatn.newLine();
					}
					}
				}
			}
			
			
		}
		
		CSV_All_TS_locatn.close();
		
		
		
		/*
		 * Following part is only for printing results
		 * */
		
		//System.out.println("Test Sample Size =" + testSampleSize);
		System.out.println("tp: " + diab_clsfn_pos);
		System.out.println("tn: " + healthy_clsfn_pos);
		System.out.println("fp: " + healthy_clsfn_neg);
		System.out.println("fn: " + diab_clsfn_neg);

		int correct = diab_clsfn_pos + healthy_clsfn_pos;
		int wrong = healthy_clsfn_neg + diab_clsfn_neg;
		double accuracy1 = (double) correct * 100 / (correct + wrong);
		System.out.println("Validating Test Samples, accuracy = " + accuracy1);

		char t1 = ' ';

		System.out.println("  ");
		System.out.println("                     " + "<---- Predicted Class Lables ----->");
		System.out.println("                     " + "  Diabetic  " + "  |  " + "  Healthy ");
		System.out.println("                     ___________________________________");

		System.out.println("Actual # 'Diabetic'  |-> " + diab_clsfn_pos + " | " + diab_clsfn_neg);
		System.out.println("Actual # 'Healthy '  |-> " + healthy_clsfn_neg + t1 + " | " + healthy_clsfn_pos);
		System.out.println("  ");

		// accuracy and error
		double accuracy = (double) positiveTestCounter / (double) testSampleSize;
		double error = 1.0d - accuracy;
	//	System.out.println("Hence accuracy = (Positive/TestSampleSize) = " + accuracy);
		// report results
		System.out.println("classification results: " + toLogStr(params, accuracy, error));

	}

	protected static String toLogStr(Params params, double accuracy, double error) {
		StringBuffer sb = new StringBuffer();
		sb.append("strategy ").append(params.getNrStartegy().toString()).append(COMMA);
		sb.append("window ").append(params.getWindowSize()).append(COMMA);
		sb.append("PAA ").append(params.getPaaSize()).append(COMMA);
		sb.append("alphabet ").append(params.getAlphabetSize()).append(COMMA);
		sb.append(" accuracy ").append(fmt.format(accuracy)).append(COMMA);
		sb.append(" error ").append(fmt.format(error));
		return sb.toString();
	}
	
	

}
