package tweeter.resources;

public class User {

    private String id;
    private String fullname;
    private String email;
    private String age;

    public User(String id, String fullnanme) {
        this.id = id;
        this.fullname = fullnanme;
    }

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "{\"fullname\":\"" + fullname + "\"," +
                "\"id\":\"" + id + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"age\":\"" + age + "\"}";
    }
}
