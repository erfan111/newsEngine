package ir.open30stem.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
/**
 * Created by Erfan Sharafzadeh on 12/11/16.
 */
@Document(collection = "Spider")
public class Spider {
    @Id
    public String id;
    public String name;
    public String farsiName;
    public String[] allowed_domains;
    public String[] urls;
    public String rss_item_path;
    public String description_path;
    public String link_path;
    public String date_path;
    public String date_model;
    public String base_html_path;
    public String text_path;
    public Integer category_index;
    public Integer id_index;

}
