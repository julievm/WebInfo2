package ie.tcd.dalyc24;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.FixedShingleFilterFactory;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class ParseDocs {

    // Directory where the search index will be saved
    private static String INDEX_DIRECTORY = "../index";

    public static void main(String[] args) throws IOException, ParseException {
        File file = new File("cs7is3-lucene-tutorial-examples/corpus2/cran.all.1400");

        // create analyzer
        Analyzer analyzer = new EnglishAnalyzer();
        BufferedReader br = new BufferedReader(new FileReader(file));

        // Open the directory that contains the search index
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

        // Set up an index writer to add process and save documents to the index
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        // Keep track of line we are reading in
        String nextline = br.readLine();

        // Counters to check how often each field gets passed
        int i = 1;
        int j = 1;
        int k = 1;
        int m = 1;

        // Initialize a new document
        Document doc = new Document();

        // Keep reading in until we reach the last line
        while(nextline != null){
            if(nextline.charAt(0) == '.'){
                // If we encounter '.I' -> id
                if(nextline.charAt(1) == 'I'){
                    doc = new Document();
                    int docId = Integer.parseInt(nextline.substring(3).trim());

                    doc.add(new TextField("docId", Integer.toString(docId), Field.Store.YES));
                    nextline = br.readLine();
                    j++;
                }
                // If we encounter '.T' -> title
                if(nextline.charAt(1) == 'T'){
                    StringBuilder title = new StringBuilder();
                    // Keep reading until we encounter '.' again at the start of line
                    while((nextline = br.readLine()).charAt(0) != '.'){
                        title.append(nextline);
                    }
                    TextField fieldTitle = new TextField("title", title.toString(), Field.Store.YES);
                    doc.add(fieldTitle);
                    k++;
                }
                // If we encounter '.A' -> author
                if(nextline.charAt(1) == 'A'){
                    StringBuilder author = new StringBuilder();
                    while((nextline = br.readLine()).charAt(0) != '.'){
                        author.append(nextline);
                    }
                    doc.add(new TextField("author", author.toString(), Field.Store.YES));
                    m++;
                }
                // If we encounter '.B' -> bib
                if(nextline.charAt(1) == 'B'){
                    StringBuilder bib = new StringBuilder();
                    while((nextline = br.readLine()).charAt(0) != '.'){
                        bib.append(nextline);
                    }
                    doc.add(new TextField("bib", bib.toString(), Field.Store.YES));
                }
                // If we encounter '.W' -> content
                if(nextline.charAt(1) == 'W'){
                    StringBuilder content = new StringBuilder();
                    while((nextline = br.readLine()) != null && !nextline.startsWith(".I ")){
                        content.append(nextline);
                    }
                    doc.add(new TextField("content", content.toString(), Field.Store.YES));
                    iwriter.addDocument(doc);
                    i++;
                }


            }

        }

        // Print out counters
        System.out.println("content: " + i);
        System.out.println("id: " + j);
        System.out.println("title " + k);
        System.out.println("author " + m);

        // Commit everything and close
        iwriter.close();
        directory.close();

        // Call queryIndexing
        queryIndexing();
    }

    public static void queryIndexing() throws IOException, ParseException {
        // Open the folder that contains our search index
        // the location of the search index
        String INDEX_DIRECTORY = "../index";

        // Limit the number of search results we get
        int MAX_RESULTS = 10;
        Directory directory2 = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        File file = new File("cs7is3-lucene-tutorial-examples/corpus2/cran.qry");

        // Create result file
        File results = new File("results.txt");

        // Analyzer
        Analyzer analyzer = new EnglishAnalyzer();

        // create objects to read and search across the index
        DirectoryReader ireader = DirectoryReader.open(directory2);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        isearcher.setSimilarity(new BM25Similarity());

        BufferedWriter writer = new BufferedWriter(new FileWriter(results, true));

        // Query parser
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[] {"content", "title"},
                analyzer);

        BufferedReader br = new BufferedReader(new FileReader(file));

        String nextLine = br.readLine();
        int queryID = 1;
        //read in the whole file containing queries
        while(nextLine != null){
            if(nextLine.charAt(0) == '.'){
                //we ignore the id of the query
                nextLine = br.readLine();
                if(nextLine.charAt(1) == 'W'){
                    //read in the whole query
                    StringBuilder content = new StringBuilder();
                    while((nextLine = br.readLine()) != null && nextLine.charAt(0) != '.'){
                        content.append(nextLine);
                    }
                    // Replace chars that are not allowed
                    String queryString = content.toString().trim().replace("*", "").replace("?", "");
                    Query query = parser.parse(queryString);

                    ScoreDoc[] hits = isearcher.search(query, 50).scoreDocs;

                    // Get right form for trec_eval
                    for (int i = 0; i < hits.length; i++)
                    {
                        Document hitDoc = isearcher.doc(hits[i].doc);
                        if(hitDoc.get("docId") == null){
                            System.out.println(hitDoc.get("title"));
                        }
                        writer.append(queryID + " Q0 " + hitDoc.get("docId") + " " + i + " " + hits[i].score + " STANDARD\n");
                    }
                    //increase queryId
                    queryID++;
                }
            }
        }
        // close everything and quit
        writer.close();
        ireader.close();
        directory2.close();
    }




}
