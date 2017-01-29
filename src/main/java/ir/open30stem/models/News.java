package ir.open30stem.models;


/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */
public class News {
    public String id;
    public String press;
    public String link;
    public String title;
    public String text;
    public String date;
    public String newsId;
    public String category;
    public String description;
    public String[] similar_docs;

    News(String id, String press, String link, String title, String text, String date, String newsId, String category,
         String description, String[] similar_docs) {
        this.id = id;
        this.press = press;
        this.link = link;
        this.title = title;
        this.text = text;
        this.date = date;
        this.newsId = newsId;
        this.category = category;
        this.description = description;
        this.similar_docs = similar_docs;
    }
}
