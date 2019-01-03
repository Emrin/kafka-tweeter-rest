package tweeter.resources;


import java.util.List;

public class Tweet {

    private String id;
    private String author;
    private String location; // query
    private List<String> tags; // query
    private List<String> mentions; // query

    public Tweet(String author, String location, List<String> tags, List<String> mentions) {
        this.author = author;
        this.location = location;
        this.tags = tags;
        this.mentions = mentions;
    }

    public Tweet() {
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getMentions() {
        return mentions;
    }

    public void setMentions(List<String> mentions) {
        this.mentions = mentions;
    }

    @Override
    public String toString() {
        return "id = "+id+" ; author = "+author+" ; location = "+location+" ; tags = "
                + tags + " ; mentions = "+mentions;
    }

    public Boolean filterLoc(String filter) {
        return location.toLowerCase().contains(filter.toLowerCase());
    }

    public Boolean filterTag(String filter) {
        return String.join(",", tags).toLowerCase().contains(filter.toLowerCase());
    }

    public Boolean filterMention(String filter) {
        return String.join(",", mentions).toLowerCase().contains(filter.toLowerCase());
    }

}
