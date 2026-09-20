package skillswap.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import skillswap.model.User;
import skillswap.util.CsvUtil;
import skillswap.util.PasswordUtil;
import skillswap.util.TextUtil;

public class UserStore {
    private final File usersFile;

    public UserStore(File usersFile) {
        this.usersFile = usersFile;
    }

    public void createFileIfNeeded() throws IOException {
        File parent = usersFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (!usersFile.exists()) {
            saveUsers(new ArrayList<>());
        }

        List<User> users = readUsers();
        if (users.isEmpty()) {
            addStarterPeople();
        } else {
            // rewrite so older plain-password rows get hashed once
            saveUsers(users);
        }
    }

    // a few made-up people so the matching page is not empty on first run
    private void addStarterPeople() throws IOException {
        List<User> users = new ArrayList<>();
        users.add(createUser("Vridhi", "vridhi@skillswap.demo", "practice1", "Java", "Public speaking"));
        users.add(createUser("Aman", "aman@skillswap.demo", "practice1", "Public speaking", "Java"));
        users.add(createUser("Sara", "sara@skillswap.demo", "practice1", "Excel", "UI design"));
        users.add(createUser("Kabir", "kabir@skillswap.demo", "practice1", "Java", "Guitar"));
        saveUsers(users);
    }

    public User createUser(String name, String email, String password, String skill, String want) {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash(password, salt);
        return new User(
            TextUtil.clean(name),
            TextUtil.clean(email).toLowerCase(),
            hash,
            salt,
            TextUtil.clean(skill),
            TextUtil.clean(want)
        );
    }

    public synchronized List<User> readUsers() throws IOException {
        List<User> users = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(usersFile))) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                User user = parseUser(line);
                if (user != null) {
                    users.add(user);
                }
            }
        }

        return users;
    }

    private User parseUser(String line) {
        String[] parts = CsvUtil.splitLine(line);

        // older files only had a plain password in the third column
        if (parts.length == 5) {
            String salt = PasswordUtil.newSalt();
            String hash = PasswordUtil.hash(parts[2], salt);
            return new User(parts[0], parts[1].toLowerCase(), hash, salt, parts[3], parts[4]);
        }

        if (parts.length < 6) {
            return null;
        }

        return new User(parts[0], parts[1].toLowerCase(), parts[2], parts[3], parts[4], parts[5]);
    }

    public synchronized void saveUsers(List<User> users) throws IOException {
        try (FileWriter writer = new FileWriter(usersFile)) {
            writer.write("Name,Email,PasswordHash,Salt,Skill,Want\n");
            for (User user : users) {
                writer.write(user.toCsvLine() + "\n");
            }
        }
    }

    public User findByEmail(String email) throws IOException {
        for (User user : readUsers()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }
}
