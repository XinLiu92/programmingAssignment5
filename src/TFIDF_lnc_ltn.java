import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.*;

class ResultComparator implements Comparator<DocumentResults> {
    public int compare(DocumentResults d2, DocumentResults d1) {
        if (d1.getScore() < d2.getScore())
            return -1;
        if (d1.getScore() == d2.getScore())
            return 0;
        return 1;
    }
}

public class TFIDF_lnc_ltn {

    private IndexSearcher searcher;
    private QueryParser parser;
    private ArrayList<Data.Page> pageList;
    private int numDocs = 10;
    private HashMap<String, ArrayList<DocumentResults>> queryResults;

    TFIDF_lnc_ltn(ArrayList<Data.Page> pl, int n) throws ParseException, IOException {

        numDocs = n; // Get the (max) number of documents to return
        pageList = pl; // Each page title will be used as a query

        // Parse the parabody field using StandardAnalyzer
        parser = new QueryParser("parabody", new StandardAnalyzer());

        // Create an index searcher
        String INDEX_DIRECTORY = "index";
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));

        // Set our own similarity class which computes tf[t,d]
        SimilarityBase lnc_ltn = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {
                return (float) (1 + Math.log10(freq));
            }

            @Override
            public String toString() {
                return null;
            }
        };
        searcher.setSimilarity(lnc_ltn);
    }


    public HashMap<String, ArrayList<RankInfo>> getResultMap() throws IOException, ParseException {
        queryResults = new HashMap<>(); // Maps query to map of Documents with
        // TF-IDF score

        for (Data.Page page : pageList) {
            HashMap<Document, Float> scores = new HashMap<>();
            HashMap<Document, DocumentResults> docMap = new HashMap<>();
            PriorityQueue<DocumentResults> docQueue = new PriorityQueue<>(new ResultComparator());
            ArrayList<DocumentResults> docResults = new ArrayList<>();
            HashMap<TermQuery, Float> queryweights = new HashMap<>();
            ArrayList<TermQuery> terms = new ArrayList<>();
            Query q = parser.parse(page.getPageName());
            String qid = page.getPageId();

            for (String term : page.getPageName().split(" ")) {
                TermQuery tq = new TermQuery(new Term("parabody", term));
                terms.add(tq);
                queryweights.put(tq, queryweights.getOrDefault(tq, 0.0f) + 1.0f);
            }
            for (TermQuery query : terms) {
                IndexReader reader = searcher.getIndexReader();
                float DF = (reader.docFreq(query.getTerm()) == 0) ? 1 : reader.docFreq(query.getTerm());

                float qTF = (float) (1 + Math.log10(queryweights.get(query))); // Logarithmic

                float qIDF = (float) (Math.log10(reader.numDocs() / DF)); // Logarithmic
                float qWeight = qTF * qIDF; // Final calculation

                // Store query weight for later calculations
                queryweights.put(query, qWeight);

                // Get the top 100 documents that match our query
                TopDocs tpd = searcher.search(query, numDocs);
                for (int i = 0; i < tpd.scoreDocs.length; i++) {
                    Document doc = searcher.doc(tpd.scoreDocs[i].doc);
                    double score = tpd.scoreDocs[i].score * queryweights.get(query);
                    DocumentResults dResults = docMap.get(doc);
                    if (dResults == null) {
                        dResults = new DocumentResults(doc);
                    }
                    float prevScore = dResults.getScore();
                    dResults.score((float) (prevScore + score));
                    dResults.queryId(qid);
                    dResults.paragraphId(doc.getField("paraid").stringValue());
                    dResults.teamName("Group7");
                    dResults.methodName("tf.idf_lnc_ltn");
                    dResults.setContent(doc.getField("parabody").stringValue());
                    docMap.put(doc, dResults);

                    // Store score for later use
                    scores.put(doc, (float) (prevScore + score));
                }
            }

            // Get cosine Length
            float cosineLength = 0.0f;
            for (Map.Entry<Document, Float> entry : scores.entrySet()) {
                Document doc = entry.getKey();
                Float score = entry.getValue();

                cosineLength = (float) (cosineLength + Math.pow(score, 2));
            }
            cosineLength = (float) (Math.sqrt(cosineLength));


            for (Map.Entry<Document, Float> entry : scores.entrySet()) {
                Document doc = entry.getKey();
                Float score = entry.getValue();

                scores.put(doc, score / scores.size());
                DocumentResults dResults = docMap.get(doc);
                dResults.score(dResults.getScore() / cosineLength);

                docQueue.add(dResults);
            }

            int rankCount = 1;
            DocumentResults current;
            while ((current = docQueue.poll()) != null) {
                current.rank(rankCount);
                docResults.add(current);
                rankCount++;
            }


            queryResults.put(qid, docResults);
        }

        HashMap<String, ArrayList<RankInfo>> result_map = new HashMap<>();
        for (Map.Entry<String, ArrayList<DocumentResults>> results : queryResults.entrySet()) {
            ArrayList<DocumentResults> list = results.getValue();
            String queryStr = results.getKey();
            ArrayList<RankInfo> ranklist = new ArrayList<RankInfo>();
            for (int i = 0; i < list.size() && i < 10; i++) {
                DocumentResults dr = list.get(i);

                RankInfo rank = new RankInfo();
                rank.setParaId(dr.getParagraphId());
                rank.setQueryStr(queryStr);
                rank.setRank(dr.getRank());
                rank.setScore(dr.getScore());
                rank.setTeam_method_name(dr.getTeamName() + "-" + dr.getMethodName());
                rank.setParaContent(dr.getContent());

                ranklist.add(rank);
            }

            result_map.put(queryStr, ranklist);
        }


        return result_map;

    }

}
