import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccar_v2.read_data.DeserializeData.*;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

public class Main {
    static final private String INDEX_DIRECTORY = "index";
    static private String Cbor_FILE;
    static private String Cbor_OUTLINE;
    static private String Cbor_ARTICLE;
    static final private String OUTPUT_DIR = "output";

    private void indexAllParagraphs() throws CborException, IOException {
        Directory indexdir = FSDirectory.open((new File(INDEX_DIRECTORY)).toPath());
        IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
        conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iw = new IndexWriter(indexdir, conf);
        for (Data.Paragraph p : DeserializeData.iterableParagraphs(new FileInputStream(new File(Cbor_FILE)))) {
            this.indexPara(iw, p);
        }
        iw.close();
    }

    private void indexPara(IndexWriter iw, Data.Paragraph para) throws IOException {
        Document paradoc = new Document();
        paradoc.add(new StringField("paraid", para.getParaId(), Field.Store.YES));
        paradoc.add(new TextField("parabody", para.getTextOnly(), Field.Store.YES));
        FieldType indexType = new FieldType();
        indexType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        indexType.setStored(true);
        indexType.setStoreTermVectors(true);

        paradoc.add(new Field("content", para.getTextOnly(), indexType));

        iw.addDocument(paradoc);
    }


    private ArrayList<Data.Page> getPageListFromPath(String path) {
        ArrayList<Data.Page> pageList = new ArrayList<Data.Page>();
        try {
            FileInputStream fis = new FileInputStream(new File(path));
            for (Data.Page page : DeserializeData.iterableAnnotations(fis)) {
                pageList.add(page);


            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pageList;
    }


    public static HashMap<String, HashMap<String, String>> read_dataFile(String file_name) {
        HashMap<String, HashMap<String, String>> query = new HashMap<String, HashMap<String, String>>();

        File f = new File(file_name);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            ArrayList<String> al = new ArrayList<>();
            String text = null;
            while ((text = br.readLine()) != null) {
                String queryId = text.split(" ")[0];
                String paraID = text.split(" ")[2];
                String rank = text.split(" ")[3];

                if (al.contains(queryId))
                    query.get(queryId).put(paraID, rank);
                else {
                    HashMap<String, String> docs = new HashMap<String, String>();
                    docs.put(paraID, rank);
                    query.put(queryId, docs);
                    al.add(queryId);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (br != null)
                br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return query;
    }



    public void writeRunfile(String filename, ArrayList<String> runfileStrings) {
        String fullpath = OUTPUT_DIR + "/" + filename;
        try (FileWriter runfile = new FileWriter(new File(fullpath))) {
            for (String line : runfileStrings) {
                runfile.write(line + "\n");
            }

            runfile.close();
        } catch (IOException e) {
            System.out.println("Could not open " + fullpath);
        }
    }

    public void writeDataFile(String filename, ArrayList<String> datafileString) {
        String fullpath = OUTPUT_DIR + "/" + filename;
        try (FileWriter runfile = new FileWriter(new File(fullpath))) {
            for (String line : datafileString) {
                runfile.write(line + "\n");
            }


        } catch (IOException e) {
            System.out.println("Could not open " + fullpath);
        }
    }

    public static void main(String[] args) {
        Main q = new Main();
        Cbor_FILE = args[0];
        Cbor_OUTLINE = args[1];
        Cbor_ARTICLE = args[2];
        try {
            q.indexAllParagraphs();

            ArrayList<Data.Page> pagelist = q.getPageListFromPath(Cbor_OUTLINE);

            String fileName = "feature_data.txt";
            String contentFile = "paraContent.txt";
            String print_query = "Brush%20rabbit";

            HashMap<String, HashMap<String, String>> relevance_data = read_dataFile(Cbor_ARTICLE);

            int max_doc_per_query = 10;

            ArrayList<String> writeStringList = new ArrayList<String>();

            HashMap<String, ArrayList<RankInfo>> result_bnn_bnn = new HashMap<>();

            HashMap<String, ArrayList<RankInfo>> result_lnc_ltn = new HashMap<>();

            HashMap<String, ArrayList<RankInfo>> result_UL = new HashMap<>();

            HashMap<String, ArrayList<RankInfo>> result_UJM = new HashMap<>();

            HashMap<String, ArrayList<RankInfo>> result_UDS = new HashMap<>();

            /*
             * bnn_bnn:1, lnc_ltn:2, UL:3, UJM:4, UDS:5
             */

            // bnn.bnn
            TFIDF_bnn_bnn tfidf_bnn_bnn = new TFIDF_bnn_bnn(pagelist, max_doc_per_query);
            result_bnn_bnn = tfidf_bnn_bnn.getResult();

            // lnc.ltn
            TFIDF_lnc_ltn tfidf_lnc_ltn = new TFIDF_lnc_ltn(pagelist, max_doc_per_query);
            result_lnc_ltn = tfidf_lnc_ltn.getResultMap();

            // UL
            System.out.println("Run LanguageMode_UL...");
            UnigramLanguageModel UL_ranking = new UnigramLanguageModel(pagelist, max_doc_per_query);
            result_UL = UL_ranking.getResults();


            // UJM
            System.out.println("Run LanguageMode_UJM...");
            LanguageModel_UJM UJM_ranking = new LanguageModel_UJM(pagelist, max_doc_per_query);
            result_UJM = UJM_ranking.getResults();

            // UDS
            System.out.println("Run LanguageMode_UDS...");
            LanguageModel_UDS UDS_ranking = new LanguageModel_UDS(pagelist);
            result_UDS = UDS_ranking.getResult();

            for (Data.Page page : pagelist) {
                String query = page.getPageId();
                ArrayList<RankInfo> bnn_list = result_bnn_bnn.get(query);
                ArrayList<RankInfo> lnc_list = result_lnc_ltn.get(query);
                ArrayList<RankInfo> ul_list = result_UL.get(query);
                ArrayList<RankInfo> ujm_list = result_UJM.get(query);
                ArrayList<RankInfo> uds_list = result_UDS.get(query);

                HashMap<String, String> relevantDocs = relevance_data.get(query);

                ArrayList<String> total_unique_docs = getAllUniqueDocumentId(bnn_list, lnc_list, ul_list, ujm_list,
                        uds_list);

                System.out.println("Total :" + total_unique_docs.size() + " docs for Query: " + query);
                for (String id : total_unique_docs) {
                    RankInfo r1 = getRankInfoById(id, bnn_list);
                    RankInfo r2 = getRankInfoById(id, lnc_list);
                    RankInfo r3 = getRankInfoById(id, ul_list);
                    RankInfo r4 = getRankInfoById(id, ujm_list);
                    RankInfo r5 = getRankInfoById(id, uds_list);

                    float f1 = (float) ((r1 == null) ? 0.0 : (float) 1 / r1.getRank());
                    float f2 = (float) ((r2 == null) ? 0.0 : (float) 1 / r2.getRank());
                    float f3 = (float) ((r3 == null) ? 0.0 : (float) 1 / r3.getRank());
                    float f4 = (float) ((r4 == null) ? 0.0 : (float) 1 / r4.getRank());
                    float f5 = (float) ((r5 == null) ? 0.0 : (float) 1 / r5.getRank());

                    int relevant = 0;
                    if (relevantDocs.get(id) != null) {
                        if (Integer.parseInt(relevantDocs.get(id)) > 0) {
                            relevant = 1;
                        }
                    }

                    String line = relevant + " qid:" + query + " 1:" + f1 + " 2:" + f2 + " 3:" + f3 + " 4:" + f4 + " 5:"
                            + f5 + " # DocId:" + id;
                    writeStringList.add(line);
                }

                if (query.equalsIgnoreCase(print_query)) {
                    System.out.println("Print paragraph contents for Query: " + print_query);
                    ArrayList<String> strLines = new ArrayList<>();
                    if (bnn_list != null && bnn_list.size() > 0) {
                        strLines.add("Reuslt for bnn.bnn >>>>>>>>>>");
                        strLines.addAll(getContentResult(bnn_list));
                        strLines.add(" ");
                    }
                    if (lnc_list != null && lnc_list.size() > 0) {
                        strLines.add("Reuslt for lnc.ltc >>>>>>>>>>");
                        strLines.addAll(getContentResult(lnc_list));
                        strLines.add(" ");
                    }
                    if (ul_list != null && ul_list.size() > 0) {
                        strLines.add("Reuslt for UL >>>>>>>>>>");
                        strLines.addAll(getContentResult(ul_list));
                        strLines.add(" ");
                    }
                    if (ujm_list != null && ujm_list.size() > 0) {
                        strLines.add("Reuslt for UJM >>>>>>>>>>");
                        strLines.addAll(getContentResult(ujm_list));
                        strLines.add(" ");
                    }
                    if (uds_list != null && uds_list.size() > 0) {
                        strLines.add("Reuslt for UDS >>>>>>>>>>");
                        strLines.addAll(getContentResult(uds_list));
                        strLines.add(" ");
                    }

                    q.writeDataFile(contentFile, strLines);
                }

                q.writeDataFile(fileName, writeStringList);
            }

        } catch (CborException | IOException | ParseException e) {
            e.printStackTrace();
        }

    }

    public static ArrayList<String> getAllUniqueDocumentId(ArrayList<RankInfo> bnn, ArrayList<RankInfo> lnc,
                                                           ArrayList<RankInfo> ul, ArrayList<RankInfo> ujm, ArrayList<RankInfo> uds) {

        ArrayList<String> total_documents = new ArrayList<String>();
        ArrayList<RankInfo> total_rankInfo = new ArrayList<RankInfo>();

        total_rankInfo.addAll(bnn);
        total_rankInfo.addAll(lnc);
        total_rankInfo.addAll(ul);
        total_rankInfo.addAll(ujm);
        total_rankInfo.addAll(uds);

        for (RankInfo rank : total_rankInfo) {
            total_documents.add(rank.getParaId());
        }

        Set<String> hs = new HashSet<>();

        hs.addAll(total_documents);
        total_documents.clear();
        total_documents.addAll(hs);

        return total_documents;
    }

    private static RankInfo getRankInfoById(String id, ArrayList<RankInfo> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (RankInfo rank : list) {
            if (rank.getParaId().equalsIgnoreCase(id)) {
                return rank;
            }
        }
        return null;
    }

    private static ArrayList<String> getContentResult(ArrayList<RankInfo> list) {
        ArrayList<String> result = new ArrayList<>();
        if (list != null && list.size() > 0) {
            for (RankInfo rank : list) {
                String line = rank.getRank() + " : " + rank.getParaContent();
                result.add(line);
            }
        }

        return result;
    }
}
