let currentUser = null;

const authPanel = document.getElementById("authPanel");
const dashboard = document.getElementById("dashboard");
const notice = document.getElementById("notice");
const panelTitle = document.getElementById("panelTitle");
const panelSubtitle = document.getElementById("panelSubtitle");
const loginForm = document.getElementById("loginForm");
const signupForm = document.getElementById("signupForm");
const loginTab = document.getElementById("loginTab");
const signupTab = document.getElementById("signupTab");
const welcomeText = document.getElementById("welcomeText");
const welcomeSummary = document.getElementById("welcomeSummary");
const content = document.getElementById("content");
const profileButton = document.getElementById("profileButton");
const matchesButton = document.getElementById("matchesButton");
const updateButton = document.getElementById("updateButton");
const logoutButton = document.getElementById("logoutButton");

const loginEmail = document.getElementById("loginEmail");
const loginPassword = document.getElementById("loginPassword");
const signupName = document.getElementById("signupName");
const signupEmail = document.getElementById("signupEmail");
const signupPassword = document.getElementById("signupPassword");
const signupSkill = document.getElementById("signupSkill");
const signupWant = document.getElementById("signupWant");

loginTab.addEventListener("click", () => setAuthMode("login"));
signupTab.addEventListener("click", () => setAuthMode("signup"));
loginForm.addEventListener("submit", (event) => {
  event.preventDefault();
  loginUser();
});
signupForm.addEventListener("submit", (event) => {
  event.preventDefault();
  signupUser();
});
profileButton.addEventListener("click", showProfile);
matchesButton.addEventListener("click", loadMatches);
updateButton.addEventListener("click", showUpdateForm);
logoutButton.addEventListener("click", logout);

setAuthMode("login");

function setAuthMode(mode) {
  const loginMode = mode === "login";

  loginForm.classList.toggle("hidden", !loginMode);
  signupForm.classList.toggle("hidden", loginMode);
  loginTab.classList.toggle("active", loginMode);
  signupTab.classList.toggle("active", !loginMode);
  panelTitle.textContent = loginMode ? "Welcome back" : "Create your profile";
  panelSubtitle.textContent = loginMode
    ? "Log in to check your profile and see who you match with."
    : "Tell people what you can help with and what you want to learn.";
  clearNotice();
}

function encode(data) {
  return Object.keys(data)
    .map((key) => `${encodeURIComponent(key)}=${encodeFormValue(data[key])}`)
    .join("&");
}

function encodeFormValue(value) {
  return encodeURIComponent(value).replaceAll("%40", "@");
}

async function loginUser() {
  try {
    const response = await fetch("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: encode({
        email: loginEmail.value.trim(),
        password: loginPassword.value
      })
    });

    const data = await response.json();
    if (!response.ok) {
      showNotice(data.message || "We could not log you in right now.", "error");
      return;
    }

    currentUser = data.user;
    authPanel.classList.add("hidden");
    dashboard.classList.remove("hidden");
    showNotice("", "success");
    syncDashboard();
    showProfile();
  } catch (error) {
    showNotice("The server is not responding. Start the Java app and try again.", "error");
  }
}

async function signupUser() {
  try {
    const response = await fetch("/signup", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: encode({
        name: signupName.value.trim(),
        email: signupEmail.value.trim(),
        password: signupPassword.value,
        skill: signupSkill.value.trim(),
        want: signupWant.value.trim()
      })
    });

    const data = await response.json();
    if (!response.ok) {
      showNotice(data.message || "We could not create your account.", "error");
      return;
    }

    signupForm.reset();
    showNotice("Your account is ready. You can log in now.", "success");
    setAuthMode("login");
    loginEmail.value = data.user.email;
  } catch (error) {
    showNotice("The server is not responding. Start the Java app and try again.", "error");
  }
}

function syncDashboard() {
  welcomeText.textContent = `Hi ${currentUser.name}`;
  welcomeSummary.textContent = `Right now you can help with ${currentUser.skill}, and you want to learn ${currentUser.want}.`;
  setActiveAction(profileButton);
}

function showProfile() {
  if (!currentUser) {
    return;
  }

  setActiveAction(profileButton);
  content.innerHTML = `
    <article class="profile-card">
      <h3>Your profile</h3>
      <p>This is how other people will understand what you bring and what you are looking for.</p>
      <div class="profile-grid">
        <div class="profile-stat">
          <strong>Name</strong>
          <span>${escapeHtml(currentUser.name)}</span>
        </div>
        <div class="profile-stat">
          <strong>Email</strong>
          <span>${escapeHtml(currentUser.email)}</span>
        </div>
        <div class="profile-stat">
          <strong>You can help with</strong>
          <span>${escapeHtml(currentUser.skill)}</span>
        </div>
        <div class="profile-stat">
          <strong>You want to learn</strong>
          <span>${escapeHtml(currentUser.want)}</span>
        </div>
      </div>
    </article>
  `;
}

async function loadMatches() {
  if (!currentUser) {
    return;
  }

  setActiveAction(matchesButton);

  try {
    const response = await fetch(`/matches?email=${currentUser.email}`);
    const data = await response.json();

    if (!response.ok) {
      content.innerHTML = renderEmptyState(data.message || "We could not load your matches yet.");
      return;
    }

    if (!data.matches.length) {
      content.innerHTML = renderEmptyState("No matches yet. Try updating your skills or goals to discover more people.");
      return;
    }

    content.innerHTML = `
      <div class="match-list">
        ${data.matches.map((match) => `
          <article class="match-card">
            <span class="match-tag">${escapeHtml(match.reason)}</span>
            <h3>${escapeHtml(match.name)}</h3>
            <p><strong>Can help with:</strong> ${escapeHtml(match.skill)}</p>
            <p><strong>Wants to learn:</strong> ${escapeHtml(match.want)}</p>
            <p><strong>Contact:</strong> ${escapeHtml(match.email)}</p>
          </article>
        `).join("")}
      </div>
    `;
  } catch (error) {
    content.innerHTML = renderEmptyState("The server is not responding. Start the Java app and try again.");
  }
}

function showUpdateForm() {
  if (!currentUser) {
    return;
  }

  setActiveAction(updateButton);
  content.innerHTML = `
    <form class="update-card" id="updateForm">
      <h3>Update your details</h3>
      <label>
        <span>What can you help with?</span>
        <input id="updateSkill" type="text" value="${escapeAttribute(currentUser.skill)}" required>
      </label>
      <label>
        <span>What do you want to learn?</span>
        <input id="updateWant" type="text" value="${escapeAttribute(currentUser.want)}" required>
      </label>
      <button class="primary-button" type="submit">Save changes</button>
    </form>
  `;

  document.getElementById("updateForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const updateSkill = document.getElementById("updateSkill");
    const updateWant = document.getElementById("updateWant");

    try {
      const response = await fetch("/update", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: encode({
          email: currentUser.email,
          skill: updateSkill.value.trim(),
          want: updateWant.value.trim()
        })
      });

      const data = await response.json();
      if (!response.ok) {
        showNotice(data.message || "We could not update your profile.", "error");
        return;
      }

      currentUser = data.user;
      showNotice("Your profile has been updated.", "success");
      syncDashboard();
      showProfile();
    } catch (error) {
      showNotice("The server is not responding. Start the Java app and try again.", "error");
    }
  });
}

function logout() {
  currentUser = null;
  dashboard.classList.add("hidden");
  authPanel.classList.remove("hidden");
  loginForm.reset();
  signupForm.reset();
  clearNotice();
  setAuthMode("login");
}

function setActiveAction(activeButton) {
  [profileButton, matchesButton, updateButton].forEach((button) => {
    button.classList.toggle("active-action", button === activeButton);
  });
}

function showNotice(message, type) {
  if (!message) {
    clearNotice();
    return;
  }

  notice.textContent = message;
  notice.className = `notice ${type}`;
}

function clearNotice() {
  notice.textContent = "";
  notice.className = "notice hidden";
}

function renderEmptyState(message) {
  return `
    <article class="empty-state">
      <h3>Nothing to show just yet</h3>
      <p>${escapeHtml(message)}</p>
    </article>
  `;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}

function escapeAttribute(value) {
  return escapeHtml(value).replaceAll("`", "&#96;");
}
