package no.ntnu.idi.tdt4300.apriori;

import org.apache.commons.cli.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This is the main class of the association rule generator.
 * <p>
 * It's a dummy reference program demonstrating the accepted command line arguments, input file format and standard output
 * format also required from your implementation. The generated standard output follows the CSV (comma-separated values) format.
 * <p>
 * It's up to you if you use this program as your base, however, it's very important to strictly follow the given formatting
 * of the inputs and outputs. Your assignment will be partly automatically evaluated, therefore keep the input arguments
 * and output format identical.
 * <p>
 * Alright, I believe it's enough to stress three times the importance of the input and output formatting. Four times...
 *
 * @author tdt4300-undass@idi.ntnu.no
 */
public class Apriori {

    /**
     * Loads the transaction from the ARFF file.
     *
     * @param filepath relative path to ARFF file
     * @return list of transactions as sets
     * @throws java.io.IOException signals that I/O error has occurred
     */
    public static List<List> readTransactionsFromFile(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        List<String> attributeNames = new ArrayList<String>();
        List<SortedSet<String>> itemSets = new ArrayList<SortedSet<String>>();

        List<List> transactionsAndNames = new ArrayList<List>();; // <itemSets, attributeNames>

        String line = reader.readLine();
        while (line != null) {
            if (line.contains("#") || line.length() < 2) {
                line = reader.readLine();
                continue;
            }
            if (line.contains("attribute")) {
                int startIndex = line.indexOf("'");
                if (startIndex > 0) {
                    int endIndex = line.indexOf("'", startIndex + 1);
                    attributeNames.add(line.substring(startIndex + 1, endIndex));
                }
            } else {
                SortedSet<String> is = new TreeSet<String>();
                StringTokenizer tokenizer = new StringTokenizer(line, ",");
                int attributeCounter = 0;
                String itemSet = "";
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    if (token.equalsIgnoreCase("t")) {
                        String attribute = attributeNames.get(attributeCounter);
                        itemSet += attribute + ",";
                        is.add(attribute);
                    }
                    attributeCounter++;
                }
                itemSets.add(is);
            }
            line = reader.readLine();
        }

        reader.close();

        transactionsAndNames.add(itemSets);
        transactionsAndNames.add(attributeNames);
        return transactionsAndNames;
    }

    /**
     * Generates the frequent itemsets given the support threshold. The results are returned in CSV format.
     *
     * @param transactions list of transactions
     * @param support      support threshold
     * @return frequent itemsets in CSV format with columns size and items; columns are semicolon-separated and items are comma-separated
     */

    //Calculcate the support of an itemset
    public static double getSupport(List<SortedSet<String>> transactions, SortedSet<String> itemSet){
        double support=0;
        for(SortedSet<String> transaction : transactions){
            boolean itemsInTransaction=true;

            itemLoop:
            for(String item : itemSet){
                if(!transaction.contains(item)){
                    itemsInTransaction=false;
                    break itemLoop;
                }
            }
            if(itemsInTransaction) support+=1;
        }

        support=support/transactions.size();

        return (support);
    }

    //Check if two itemsets are eligble to be combined into a k+1-set
    public static boolean areCompatible(SortedSet<String> itemSet1, SortedSet<String> itemSet2) {
        boolean compatible = false;

        if(itemSet1.size()==1) compatible=true;

        else{
            //Get the k-1 items from the first set. They are to be compared to the k-1 items from the second set
            SortedSet<String> firstSetK1 = new TreeSet<String>(itemSet1.subSet(itemSet1.first(),itemSet1.last()));
            SortedSet<String> firstSetK2 = new TreeSet<String>(itemSet2.subSet(itemSet2.first(),itemSet2.last()));
            compatible = (firstSetK1.equals(firstSetK2));
        }
        return compatible;
    }

    //Combine two itemsets into a k+1-set
    public static SortedSet<String> combineSets(SortedSet<String> itemSet1, SortedSet<String> itemSet2){
        SortedSet<String> combinedSet = new TreeSet<String>(itemSet1);
        combinedSet.add(itemSet2.last());
        return combinedSet;
    }

    public static String generateFrequentItemsets(List<SortedSet<String>> transactions, SortedSet<String> allAttributes, double minsup) {
        //Create a list to hold all of the frequent itemsets, separated by the length of the sets.
        List<ArrayList<SortedSet<String>>> frequentSets = new ArrayList<ArrayList<SortedSet<String>>>();

        //Create a temporary set to hold the k=1-sets
        ArrayList<SortedSet<String>> frequentOneSets = new ArrayList<SortedSet<String>>();

        //Add all of the k=1-sets (single attributes) to the temporary set^
        for(String attribute : allAttributes){
            SortedSet<String> itemSet = new TreeSet<String>();
            itemSet.add(attribute);
            if(getSupport(transactions, itemSet)>=minsup) frequentOneSets.add(itemSet);
        }

        //Add the frequent k=1-sets to the frequentSets list
        frequentSets.add(frequentOneSets);

        //As long as the current list of frequent k-sets has more than one itemset, try to combine them into k+1-sets.
        while(frequentSets.get(frequentSets.size()-1).size()>1){
            //Change the k value, by getting the frequent sets generated last loop
            ArrayList<SortedSet<String>> checkQueue  = frequentSets.get(frequentSets.size()-1);

            //Create a holder list to to hold the frequent k-sets
            ArrayList<SortedSet<String>> frequentKSets = new ArrayList<SortedSet<String>>();

            //For each itemset in the current queue, try to combine it with the succeeding itemsets,
            //and add all of the new frequent k+1-sets to the frequentKSets list
            int n=1;
            for(SortedSet<String> currItemSet : checkQueue){
                for(int i=n; i<checkQueue.size(); i++){
                    SortedSet<String> compareSet = checkQueue.get(i);
                    if(areCompatible(currItemSet,compareSet)){
                        SortedSet<String> combinedItemSet = combineSets(currItemSet, compareSet);
                        if(getSupport(transactions, combinedItemSet)>=minsup) frequentKSets.add(combinedItemSet);
                    }
                }
                n++;
            }

            //Add the new frequent k+1-itemsets to the frequentSets list
            //If there are more than one frequent item set, continue this whole loop
            frequentSets.add(frequentKSets);
        }


        return myString(frequentSets);
        /*return "size;items\n" +
                "1;beer\n" +
                "1;bread\n" +
                "1;diapers\n" +
                "1;milk\n" +
                "2;beer,diapers\n" +
                "2;bread,diapers\n" +
                "2;bread,milk\n" +
                "2;diapers,milk\n" +
                "3;bread,diapers,milk\n";*/
    }

    private static String myString(List<ArrayList<SortedSet<String>>> frequentSets){
        String outString = "size;items\n";
        for(ArrayList<SortedSet<String>> frequentKSets : frequentSets){
            for(SortedSet<String> freqSet : frequentKSets){
                outString+=freqSet.size() + ";";
                for(String item : freqSet){
                    outString+=item + ",";
                }
                outString=outString.substring(0, outString.length()-1);
                outString+="\n";
            }
        }
    }

    /**
     * Generates the association rules given the support and confidence threshold. The results are returned in CSV
     * format.
     *
     * @param transactions list of transactions
     * @param support      support threshold
     * @param confidence   confidence threshold
     * @return association rules in CSV format with columns antecedent, consequent, confidence and support; columns are semicolon-separated and items are comma-separated
     */
    public static String generateAssociationRules(List<SortedSet<String>> transactions, SortedSet<String> allAttributes, double support, double confidence) {
        // TODO: Generate and print association rules given the method parameters.


        return "antecedent;consequent;confidence;support\n" +
                "diapers;beer;0.6;0.5\n" +
                "beer;diapers;1.0;0.5\n" +
                "diapers;bread;0.8;0.67\n" +
                "bread;diapers;0.8;0.67\n" +
                "milk;bread;0.8;0.67\n" +
                "bread;milk;0.8;0.67\n" +
                "milk;diapers;0.8;0.67\n" +
                "diapers;milk;0.8;0.67\n" +
                "diapers,milk;bread;0.75;0.5\n" +
                "bread,milk;diapers;0.75;0.5\n" +
                "bread,diapers;milk;0.75;0.5\n" +
                "bread;diapers,milk;0.6;0.5\n" +
                "milk;bread,diapers;0.6;0.5\n" +
                "diapers;bread,milk;0.6;0.5\n";
    }

    /**
     * Main method.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // definition of the accepted command line arguments
        Options options = new Options();
        options.addOption(Option.builder("f").argName("file").desc("input file with transactions").hasArg().required(true).build());
        options.addOption(Option.builder("s").argName("support").desc("support threshold").hasArg().required(true).build());
        options.addOption(Option.builder("c").argName("confidence").desc("confidence threshold").hasArg().required(false).build());
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            // extracting filepath and support threshold from the command line arguments
            String filepath = cmd.getOptionValue("f");
            double support = Double.parseDouble(cmd.getOptionValue("s"));

            // reading transaction from the file
            List<SortedSet<String>> transactions = readTransactionsFromFile(filepath).get(0);
            List<String> attributes = readTransactionsFromFile(filepath).get(1);

            //Sort the attribute names
            SortedSet<String> sortedAttributes = new TreeSet<String>(attributes);


            if (cmd.hasOption("c")) {
                // extracting confidence threshold
                double confidence = Double.parseDouble(cmd.getOptionValue("c"));

                // printing generated association rules
                System.out.println(generateAssociationRules(transactions, sortedAttributes, support, confidence));
            } else {
                // printing generated frequent itemsets
                System.out.println(generateFrequentItemsets(transactions, sortedAttributes, support));
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setOptionComparator(null);
            helpFormatter.printHelp("java -jar apriori.jar", options, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}