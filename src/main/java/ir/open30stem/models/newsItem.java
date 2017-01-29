package ir.open30stem.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */
@Document(collection = "news_item")
public class newsItem {

    @Id
    public String id;

    public String press;
    public String link;
    public String title;
    public String text;
    public String date;
    public String newsId;
    public String category;
    public String description;
    public String[] stemmed;
    public String[] tokenized;
    public boolean finished;
    public boolean isIndexed;
    public boolean isModified;

    public String[] similarDocs;
    public String[] title_tokenized;
    public String[] three_grams;

    public News toNews(){
        return new News(id,press,link, title, text, date, newsId, category, description, similarDocs);
    }
}
