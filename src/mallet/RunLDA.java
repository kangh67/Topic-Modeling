package mallet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

public class RunLDA {
	public static int numTopics = 100;
	public static int iterations = 2000;
	public static int topicDisplay = 10;
	
	public static String originalCSV = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\Classifier Training Set 8-11.csv";
	public static String input = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\LDA\\Classifier Training Set 2015.txt";
	public static String stopwords = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\LDA\\stoplist.txt";
	public static String topicFile = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\LDA\\topic display_" + numTopics + ".txt";
	public static String csvFile = "C:\\Users\\hkang1.UTHSAHS\\Google Drive\\MAUDE\\filter_classifier\\LDA\\topic distribution_" + numTopics + ".csv";
	
	public static int instanceNum = 0;
	
	public static void main(String[] args) throws Exception {
		ArrayList<String[]> ID_label = getInstanceNumber();
		ArrayList<double[]> topicDistribution = LDA();
		writeCSV(ID_label, topicDistribution);
	}
	
	public static ArrayList<String[]> getInstanceNumber() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(originalCSV));
		String line = br.readLine();
		ArrayList<String[]> ID_label = new ArrayList<String[]>();
		
		while((line = br.readLine()) != null) {
			String[] thisline = line.split(",");
			String[] s = new String[2];
			s[0] = thisline[0];
			s[1] = thisline[thisline.length - 1];
			ID_label.add(s);
		}		
		br.close();		
		
		br = new BufferedReader(new FileReader(input));		
		
		while(br.readLine() != null) {
			instanceNum ++;			
		}
		br.close();
		
		return ID_label;
	}
	
	public static ArrayList<double[]> LDA() throws Exception {
        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(new File(stopwords), "UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence() );

        InstanceList instances = new InstanceList (new SerialPipes(pipeList));

        Reader fileReader = new InputStreamReader(new FileInputStream(new File(input)), "UTF-8");
        instances.addThruPipe(new CsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, label, name fields

        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only, 
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(iterations);
        model.estimate();

        // Show the words and topics in the first instance

        // The data alphabet maps word IDs to strings        
        Alphabet dataAlphabet = instances.getDataAlphabet();
        
        
        /****
        
        FeatureSequence tokens = (FeatureSequence) model.getData().get(id).instance.getData();
        LabelSequence topics = model.getData().get(id).topicSequence;
        
        Formatter out = new Formatter(new StringBuilder(), Locale.US);
        for (int position = 0; position < tokens.getLength(); position++) {
            out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
        }
        System.out.println(out);
        
        ****/
        
        
        
        // Estimate the topic distribution of the first instance, 
        //  given the current Gibbs state.
        ArrayList<double[]> topicDistribution= new ArrayList<double[]>();
        for(int i=0; i<instanceNum; i++) {
        	double[] thisDistribution = model.getTopicProbabilities(i);
        	topicDistribution.add(thisDistribution);
        }

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        
        // Show top N words in topics with proportions for the first document
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(topicFile)));
        for (int topic = 0; topic < numTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
            
            
            Formatter out = new Formatter(new StringBuilder(), Locale.US);
            out.format("%d\t", topic);            
            
            int rank = 0;
            while (iterator.hasNext() && rank < topicDisplay) {
                IDSorter idCountPair = iterator.next();
                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
                rank++;
            }
            System.out.println(out);
            dos.writeBytes(out + "\r\n");
        }
        
        dos.close();
        
        return topicDistribution;
        
        /****
        
        // Create a new instance with high probability of topic 0
        StringBuilder topicZeroText = new StringBuilder();
        Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

        int rank = 0;
        while (iterator.hasNext() && rank < 5) {
            IDSorter idCountPair = iterator.next();
            topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
            rank++;
        }

        // Create a new instance named "test instance" with empty target and source fields.
        InstanceList testing = new InstanceList(instances.getPipe());
        testing.addThruPipe(new Instance(topicZeroText.toString(), null, "test instance", null));

        TopicInferencer inferencer = model.getInferencer();
        double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
        System.out.println("0\t" + testProbabilities[0]);
        
        ****/
    }
	
	public static void writeCSV(ArrayList<String[]> ID_Label, ArrayList<double[]> distribution) throws IOException{
		if(ID_Label.size() != distribution.size())
			System.out.println("Warning: ID_label and distribution have different size!");
		
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(csvFile)));
		
		//title
		dos.writeBytes("ID" + "," + "HIT_OR_NOT");
		for(int i=0; i<numTopics; i++)
			dos.writeBytes("," + i);
		dos.writeBytes("\r\n");
		
		for(int i=0; i<ID_Label.size(); i++) {
			dos.writeBytes(ID_Label.get(i)[0] + "," + ID_Label.get(i)[1]);
			for(int j=0; j<distribution.get(i).length; j++)
				dos.writeBytes("," + distribution.get(i)[j]);
			dos.writeBytes("\r\n");
		}
		
		dos.close();
	}
}
