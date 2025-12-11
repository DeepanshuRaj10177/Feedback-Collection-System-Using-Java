import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

class Feedback {
    private String userName;
    private String userEmail;
    private Map<String, Integer> ratings;
    private String comments;
    private String formId;
    private String formTitle;

    public Feedback(String userName, String userEmail, Map<String, Integer> ratings, String comments, String formId, String formTitle) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.ratings = ratings;
        this.comments = comments;
        this.formId = formId;
        this.formTitle = formTitle;
    }

    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public Map<String, Integer> getRatings() { return ratings; }
    public String getComments() { return comments; }
    public String getFormId() { return formId; }
    public String getFormTitle() { return formTitle; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------\n");
        sb.append(" Form: ").append(formTitle).append("\n");
        sb.append(" Name: ").append(userName).append("\n");
        sb.append(" Email: ").append(userEmail).append("\n");
        
        if (ratings != null) {
            for (Map.Entry<String, Integer> entry : ratings.entrySet()) {
                sb.append(" Rating (").append(entry.getKey()).append("): ")
                  .append(entry.getValue()).append(" / 5\n");
            }
        }
        
        sb.append(" Comments: ").append(comments).append("\n");
        sb.append("---------------------------------\n\n");
        return sb.toString();
    }
}

class User {
    private String username;
    private String passwordHash;
    private String role;

    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}

class FormDefinition {
    private String id;
    private String title;
    private String description;
    private List<String> ratingCategories;

    public FormDefinition(String title, String description, List<String> ratingCategories) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.ratingCategories = ratingCategories;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getRatingCategories() { return ratingCategories; }
    
    @Override
    public String toString() {
        return title;
    }
}

public class DataService {

    private static DataService instance;
    private final List<User> userList = new CopyOnWriteArrayList<>();
    private final List<Feedback> feedbackList = new CopyOnWriteArrayList<>();
    private final List<FormDefinition> formList = new CopyOnWriteArrayList<>();

    private DataService() {
        addInitialUsers();
        addInitialForms();
    }

    public static synchronized DataService getInstance() {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addInitialUsers() {
        userList.add(new User("admin", hashPassword("123"), "ADMIN"));
        userList.add(new User("deepanshu", hashPassword("123"), "USER"));
        userList.add(new User("dev", hashPassword("123"), "USER"));
        userList.add(new User("deepak", hashPassword("123"), "USER"));
        userList.add(new User("divyansh", hashPassword("123"), "USER"));
        userList.add(new User("daksh", hashPassword("123"), "USER"));

    }
    
    private void addInitialForms() {
        formList.add(new FormDefinition("General Website Feedback", "Tell us what you think...", List.of("Overall Experience")));
        formList.add(new FormDefinition("Product Support Survey", "How was support?", List.of("Speed", "Clarity", "Friendliness")));
    }

    public User authenticateUser(String username, String password) {
        String hash = hashPassword(password);
        if (hash == null) return null;
        for (User user : userList) {
            if (user.getUsername().equals(username) && user.getPasswordHash().equals(hash)) {
                return user;
            }
        }
        return null;
    }

    public List<User> getUsers() {
        return userList;
    }

    public boolean addUser(String username, String password, String role) {
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return false;
            }
        }
        String hash = hashPassword(password);
        if (hash == null) return false;
        userList.add(new User(username, hash, role));
        return true;
    }

    public void deleteUser(String username) {
        userList.removeIf(user -> user.getUsername().equals(username));
    }
    
    public boolean updateUserPassword(String username, String newPassword) {
        String hash = hashPassword(newPassword);
        if (hash == null) return false;
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                user.setPasswordHash(hash);
                return true;
            }
        }
        return false;
    }

    public void addFeedback(Feedback feedback) {
        feedbackList.add(feedback);
    }

    public List<Feedback> getFeedback() {
        return feedbackList;
    }
    
    public boolean hasUserSubmittedForm(User user, FormDefinition form) {
        for (Feedback fb : feedbackList) {
            if (fb.getFormId().equals(form.getId()) && fb.getUserName().equals(user.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public void clearAllFeedback() {
        feedbackList.clear();
    }
    
    public List<FormDefinition> getForms() {
        return formList;
    }
    
    public void addForm(String title, String description, List<String> ratingCategories) {
        formList.add(new FormDefinition(title, description, ratingCategories));
    }
    
    public void deleteForm(FormDefinition form) {
        formList.remove(form);
    }
}