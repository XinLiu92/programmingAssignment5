import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import edu.unh.cs.treccar_v2.Data;


class RunFileString {
    public String queryId;
    public String paraId;
    public int rank;
    public float score;
    public String teamName;
    public String methodName;


    RunFileString(String qid, String pid, int r, float s) {
        queryId = qid;
        paraId = pid;
        rank = r;
        score = s;
        teamName = "Group7";
        methodName = "DS-Dirichlet";
    }


    public String toString() {
        return (queryId + " Q0 " + paraId + " " + rank + " " + score + " " + teamName + "-" + methodName);
    }
}

// class for LM UDS
class LanguageModel_UDS {
    static final private String INDEX_DIRECTORY = "index";
    static final private String OUTPUT_DIRECTORY = "output";
    static final private String outputName = "results_uds.run";
    private final int numDocs = 10;
    private HashMap<String, ArrayList<RankInfo>> result_map;


    private static IndexReader getInedexReader(String path) throws IOException {
        return DirectoryReader.open(FSDirectory.open((new File(path).toPath())));
    }


    public static SimilarityBase getSimilarity() throws IOException {
        SimilarityBase sim = new SimilarityBase() {
            @Override
            protected float score(BasicStats stats, float freq, float arg2) {
                float totalTF = stats.getTotalTermFreq();
                return (freq + 1000) / (totalTF + 1000);
            }

            @Override
            public String toString() {
                return null;
            }
        };
        return sim;
    }


    LanguageModel_UDS(ArrayList<Data.Page> pagelist) {

        try {
            ArrayList<RunFileString> resultLines = new ArrayList<>();
            result_map = new HashMap<>();
            for (Data.Page page : pagelist) {
                String queryStr = page.getPageId();
                ArrayList<RankInfo> rankList = getRanked(queryStr);

                result_map.put(queryStr, rankList);
            }
            // writeArrayToFile(resultLines);

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, ArrayList<RankInfo>> getResult() {
        return result_map;
    }

    /*
     * gets result array of size 100 for a given query.
     */
    private ArrayList<RankInfo> getRanked(String query) throws IOException {
        IndexSearcher indexSearcher = new IndexSearcher(getInedexReader(INDEX_DIRECTORY));
        indexSearcher.setSimilarity(getSimilarity());

        QueryParser parser = new QueryParser("parabody", new StandardAnalyzer());

        ArrayList<RankInfo> rankList = new ArrayList<RankInfo>();
        try {
            Query q = parser.parse(query);
            TopDocs topDocs = indexSearcher.search(q, numDocs);
            ScoreDoc[] hits = topDocs.scoreDocs;
            for (int i = 0; i < hits.length; i++) {
                Document doc = indexSearcher.doc(hits[i].doc);
                String docId = doc.get("paraid");
                float score = hits[i].score;

                RankInfo rank = new RankInfo();
                rank.setQueryStr(query);
                rank.setParaId(docId);
                rank.setRank(i + 1);
                rank.setScore(score);
                rank.setTeam_method_name("Group7-DS-Dirichlet");
                rank.setParaContent(doc.get("parabody"));
                rankList.add(rank);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return rankList;
    }

    /*
     * makes output directory if it doesn't exist. Same for output file. writes
     * the contents of an ArrayList<String> to the output file.
     */
    private void writeArrayToFile(ArrayList<RunFileString> list) throws IOException {
        File dir = new File(OUTPUT_DIRECTORY);
        if (!dir.exists()) {
            if (dir.mkdir()) {
                System.out.println("output directory made...");
            }
        }
        File file = new File(OUTPUT_DIRECTORY + "/" + outputName);
        if (file.createNewFile()) {
            System.out.println(outputName + " file made...");
        }
        BufferedWriter buff = new BufferedWriter(new FileWriter(file));
        for (RunFileString line : list) {
            buff.write(line.toString() + "\n");
        }
        buff.close();
    }
}