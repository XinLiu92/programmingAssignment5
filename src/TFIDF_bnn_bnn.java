import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.search.Query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;


class DocResult {
    public int docId;
    public int score;

    // constructor
    public DocResult(int id, int s) {
        docId = id;
        score = s;
    }
}


class DocComparator implements Comparator<DocResult> {

    @Override
    public int compare(DocResult d1, DocResult d2) {
        if (d1.score < d2.score)
            return 1;
        if (d1.score > d2.score)
            return -1;
        return 0;
    }
}


public class TFIDF_bnn_bnn {

    private IndexSearcher indexSearcher = null;
    private QueryParser queryParser = null;
    private ArrayList<Data.Page> queryPages;
    private int numDocs = 100;
    HashMap<Query, ArrayList<DocumentResults>> queryResults;


    static final private String INDEX_DIRECTORY = "index";
    static final private String OUTPUT_DIR = "output";


    TFIDF_bnn_bnn(ArrayList<Data.Page> pageList, int maxDox) throws IOException, ParseException {
        queryPages = pageList;
        numDocs = maxDox;

        queryParser = new QueryParser("parabody", new StandardAnalyzer());

        indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));

        SimilarityBase bnn = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float decLen) {
                return freq > 0 ? 1 : 0;
            }

            @Override
            public String toString() {
                return null;
            }
        };
        indexSearcher.setSimilarity(bnn);
    }

    public HashMap<String, ArrayList<RankInfo>> getResult() throws ParseException, IOException {
        queryResults = new HashMap<>();

        HashMap<String, HashMap<Integer, Integer>> results = new HashMap<>();

        // run through cbor.outlines for queries
        for (Data.Page page : queryPages) {
            ArrayList<TermQuery> queryTerms = new ArrayList<>();

            Query qry = queryParser.parse(page.getPageName());
            String qid = page.getPageId();

            for (String term : page.getPageName().split(" ")) {
                TermQuery cur = new TermQuery(new Term("parabody", term));
                queryTerms.add(cur);
            }

            HashMap<Integer, Integer> docScores = new HashMap<>();
            for (TermQuery term : queryTerms) {
                TopDocs topDocs = indexSearcher.search(term, numDocs);
                for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                    Document doc = indexSearcher.doc(topDocs.scoreDocs[i].doc);

                    if (!docScores.containsKey(topDocs.scoreDocs[i].doc)) {
                        docScores.put(topDocs.scoreDocs[i].doc, 1);
                    } else {
                        int prev = docScores.get(topDocs.scoreDocs[i].doc);
                        docScores.put(topDocs.scoreDocs[i].doc, ++prev);
                    }

                }

            }
            results.put(qid, docScores);
        }

        return getResultString(results);

    }



    private HashMap<String, ArrayList<RankInfo>> getResultString(HashMap<String, HashMap<Integer, Integer>> map)
            throws IOException {

        ArrayList<String> resultList = new ArrayList<String>();
        HashMap<String, ArrayList<RankInfo>> resultMap = new HashMap<>();

        Set<String> keys = map.keySet();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String curQuery = iter.next();
            HashMap<Integer, Integer> doc = map.get(curQuery);
            String q = curQuery.toString();
            Set<Integer> tmp = doc.keySet();
            Iterator<Integer> docIds = tmp.iterator();

            ArrayList<RankInfo> rankList = new ArrayList<RankInfo>();

            PriorityQueue<DocResult> queue = new PriorityQueue<>(new DocComparator());
            while (docIds.hasNext()) {
                int curDocId = docIds.next();
                int score = doc.get(curDocId);
                DocResult tmsRes = new DocResult(curDocId, score);

                queue.add(tmsRes);
            }
            int count = 0;
            DocResult cur;
            while ((cur = queue.poll()) != null && count++ < 10) {
                RankInfo rank = new RankInfo();
                rank.setDocId(cur.docId);
                rank.setParaId(indexSearcher.doc(cur.docId).getField("paraid").stringValue());
                rank.setRank(count);
                rank.setScore(cur.score);
                rank.setQueryStr(curQuery);
                rank.setParaContent(indexSearcher.doc(cur.docId).getField("parabody").stringValue());
                rank.setTeam_method_name("Group7-tfidf_bnn_bnn");

                rankList.add(rank);
            }

            resultMap.put(curQuery, rankList);
        }
        return resultMap;
    }

}

