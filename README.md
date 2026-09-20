# SkillSwap

A small website I made for people who want to trade skills.

You put down one thing you can teach and one thing you want to learn. The app then shows people who fit: either a two-way swap, or someone on a similar path.

It is a Java + HTML/CSS/JS project. No extra frameworks. Accounts are stored in a CSV file.

## Run it

You need Java 17 or later.

```bash
./run.sh
```

Open [http://localhost:8080](http://localhost:8080).

If `./run.sh` is not executable:

```bash
chmod +x run.sh
./run.sh
```

## Try it with the sample people

On first run the app adds a few demo accounts. Password for all of them is `practice1`.

- `vridhi@skillswap.demo` knows Java, wants public speaking
- `aman@skillswap.demo` knows public speaking, wants Java
- `sara@skillswap.demo` knows Excel, wants UI design
- `kabir@skillswap.demo` knows Java, wants guitar

Vridhi and Aman should show up as a two-way swap.

## What the folders are

```
src/skillswap/     Java code
  Main.java        starts the server
  model/           one User class
  store/           CSV file + login sessions
  http/            each URL has its own handler
  util/            password hashing, CSV, small text helpers
web/               the site
data/users.csv     saved accounts
```

## How matching works

For two people A and B:

- **two-way swap** if A’s skill is what B wants, and the other way around
- **similar path** if they share a skill or they want the same thing

Spaces and capital letters are ignored while matching, so `Java` and `java` still count.

## Notes I would tell an examiner

- Passwords are not stored as plain text. Each one is hashed with SHA-256 and a random salt.
- After login the browser gets an HttpOnly cookie. Matches and profile edits use that session, not an email in the URL.
- The old CSV format (plain password in column 3) still loads, and gets rewritten in the new format the next time data is saved.
- This is still a college project, not a bank. The CSV file is local, sessions live in memory, and there is no HTTPS unless you put it behind a host that adds it.

## API

| Method | Path | Who can call it |
| ------ | ---- | --------------- |
| GET | `/` | anyone, the website |
| POST | `/signup` | create an account and log in |
| POST | `/login` | log in |
| POST | `/logout` | log out |
| GET | `/me` | current user, needs a session |
| GET | `/matches` | your matches, needs a session |
| POST | `/update` | change skill and goal, needs a session |
