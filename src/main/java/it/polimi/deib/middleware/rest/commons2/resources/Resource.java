package it.polimi.deib.middleware.rest.commons2.resources;


import java.util.List;

public class Resource { // a tweet

    private String id;
    private String author;
    private String location;
    private List<String> tags;
    private List<String> mentions;

    public void Resource(String author, String location, List<String> tags, List<String> mentions) {
        this.author = author;
        this.location = location;
        this.tags = tags;
        this.mentions = mentions;
    }

    public Resource() {
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

    public String toString() {
        return "id = "+id+" ; author = "+author+" ; location = "+location+" ; tags = "
                + tags + " ; mentions = "+mentions;
    }
}
