import org.apache.lucene.document.Document;

public class DocumentResults {
    private Document doc;
    private String queryId;
    private String q0 = "Q0";
    private String paragraphId;
    private int rank;
    private float score;
    private String teamName;
    private String methodName;
    private String content;


    DocumentResults(Document d) {
        doc = d;
        queryId = "";
        paragraphId = "";
        rank = -1;
        score = 0.0f;
        teamName = "";
        methodName = "";
    }



    public void queryId(String qid) {
        queryId = qid;
    }



    public void paragraphId(String pid) {
        paragraphId = pid;
    }

    public String getParagraphId() {
        return paragraphId;
    }

    public void rank(int r) {
        rank = r;
    }

    public int getRank() {
        return rank;
    }

    public void score(float s) {
        score = s;
    }

    public float getScore() {
        return score;
    }

    public void teamName(String tn) {
        teamName = tn;
    }

    public String getTeamName() {
        return teamName;
    }

    public void methodName(String mn) {
        methodName = mn;
    }

    public String getMethodName() {
        return methodName;
    }



    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}