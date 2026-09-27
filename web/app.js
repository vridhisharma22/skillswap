const guestNav = document.getElementById("guestNav");
const userNav = document.getElementById("userNav");
const navName = document.getElementById("navName");
const landing = document.getElementById("landing");
const howSection = document.getElementById("how");
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
const signupConfirm = document.getElementById("signupConfirm");
const signupSkill = document.getElementById("signupSkill");
const signupWant = document.getElementById("signupWant");

let currentUser = null;

loginTab.addEventListener("click", () => setAuthMode("login"));
signupTab.addEventListener("click", () => setAuthMode("signup"));
document.getElementById("showLoginButton").addEventListener("click", () => {
  setAuthMode("login");
  authPanel.scrollIntoView({ behavior: "smooth", block: "start" });
});
document.getElementById("showSignupButton").addEventListener("click", showSignup);
document.getElementById("heroJoinButton").addEventListener("click", showSignup);

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

checkSession();

function showSignup() {
  setAuthMode("signup");
  authPanel.scrollIntoView({ behavior: "smooth", block: "start" });
}

function setAuthMode(mode) {
  const loginMode = mode === "login";
  loginForm.classList.toggle("hidden", !loginMode);
  signupForm.classList.toggle("hidden", loginMode);
  loginTab.classList.toggle("active", loginMode);
  signupTab.classList.toggle("active", !loginMode);
  panelTitle.textContent = loginMode ? "Welcome back" : "Create your profile";
  panelSubtitle.textContent = loginMode
    ? "Log in to see your profile and matches."
    : "Tell people what you can help with and what you want to learn.";
}

function encode(data) {
  return Object.keys(data)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
    .join("&");
}

async function send(url, options) {
  const response = await fetch(url, {
    credentials: "same-origin",
    ...options
  });
  const data = await response.json();
  return { response, data };
}

async function checkSession() {
  try {
    const { response, data } = await send("/me");
    if (response.ok) {
      currentUser = data.user;
      enterDashboard();
    }
  } catch (error) {
    // server might not be up yet; the login form is still there
  }
}

async function loginUser() {
  try {
    const { response, data } = await send("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: encode({
        email: loginEmail.value.trim(),
        password: loginPassword.value
      })
    });

    if (!response.ok) {
      showNotice(data.message || "We could not log you in.", "error");
      return;
    }

    currentUser = data.user;
    enterDashboard();
    showNotice("Welcome back.", "success");
  } catch (error) {
    showNotice("The server is not responding. Start the Java app and try again.", "error");
  }
}

async function signupUser() {
  if (signupPassword.value !== signupConfirm.value) {
    showNotice("Those two passwords are not the same.", "error");
    return;
  }

  if (signupSkill.value.trim().toLowerCase() === signupWant.value.trim().toLowerCase()) {
    showNotice("Pick different things to teach and learn.", "error");
    return;
  }

  try {
    const { response, data } = await send("/signup", {
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

    if (!response.ok) {
      showNotice(data.message || "We could not create your account.", "error");
      return;
    }

    currentUser = data.user;
    enterDashboard();
    showNotice("You're in. Have a look at your matches.", "success");
  } catch (error) {
    showNotice("The server is not responding. Start the Java app and try again.", "error");
  }
}

function enterDashboard() {
  landing.classList.add("hidden");
  howSection.classList.add("hidden");
  dashboard.classList.remove("hidden");
  guestNav.classList.add("hidden");
  userNav.classList.remove("hidden");
  navName.textContent = currentUser.name;
  syncDashboard();
  showProfile();
}

function showLanding() {
  landing.classList.remove("hidden");
  howSection.classList.remove("hidden");
  dashboard.classList.add("hidden");
  guestNav.classList.remove("hidden");
  userNav.classList.add("hidden");
  loginForm.reset();
  signupForm.reset();
  setAuthMode("login");
}

function syncDashboard() {
  welcomeText.textContent = `Hi ${currentUser.name}`;
  welcomeSummary.textContent = `You can help with ${currentUser.skill}. You want to learn ${currentUser.want}.`;
}

function showProfile() {
  if (!currentUser) {
    return;
  }

  setActiveAction(profileButton);
  content.innerHTML = `
    <article class="profile-card">
      <h3>Your profile</h3>
      <p>This is what other people see when they match with you.</p>
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
    const { response, data } = await send("/matches");

    if (!response.ok) {
      content.innerHTML = renderEmptyState(data.message || "We could not load your matches yet.");
      return;
    }

    if (!data.matches.length) {
      content.innerHTML = renderEmptyState("No matches yet. Try changing your skill or your goal.");
      return;
    }

    content.innerHTML = `
      <div class="match-list">
        ${data.matches.map((match) => `
          <article class="match-card">
            <span class="match-tag ${match.perfect ? "perfect" : ""}">${escapeHtml(match.reason)}</span>
            <h3>${escapeHtml(match.name)}</h3>
            <p><strong>Can help with:</strong> ${escapeHtml(match.skill)}</p>
            <p><strong>Wants to learn:</strong> ${escapeHtml(match.want)}</p>
            <a class="mail-link" href="mailto:${escapeAttribute(match.email)}">Email ${escapeHtml(match.email)}</a>
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
      <p>Change these whenever your interests move around. Matching uses the latest version.</p>
      <label>
        <span>What can you help with?</span>
        <input id="updateSkill" type="text" maxlength="40" value="${escapeAttribute(currentUser.skill)}" required>
      </label>
      <label>
        <span>What do you want to learn?</span>
        <input id="updateWant" type="text" maxlength="40" value="${escapeAttribute(currentUser.want)}" required>
      </label>
      <button class="primary-button" type="submit">Save changes</button>
    </form>
  `;

  document.getElementById("updateForm").addEventListener("submit", saveProfile);
}

async function saveProfile(event) {
  event.preventDefault();

  const skill = document.getElementById("updateSkill").value.trim();
  const want = document.getElementById("updateWant").value.trim();

  if (skill.toLowerCase() === want.toLowerCase()) {
    showNotice("Pick different things to teach and learn.", "error");
    return;
  }

  try {
    const { response, data } = await send("/update", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: encode({ skill, want })
    });

    if (!response.ok) {
      showNotice(data.message || "We could not update your profile.", "error");
      return;
    }

    currentUser = data.user;
    navName.textContent = currentUser.name;
    showNotice("Saved.", "success");
    syncDashboard();
    showProfile();
  } catch (error) {
    showNotice("The server is not responding. Start the Java app and try again.", "error");
  }
}

async function logout() {
  try {
    await send("/logout", { method: "POST" });
  } catch (error) {
    // even if the request fails, send them back to the landing page
  }

  currentUser = null;
  showLanding();
}

function setActiveAction(activeButton) {
  [profileButton, matchesButton, updateButton].forEach((button) => {
    button.classList.toggle("active-action", button === activeButton);
  });
}

function showNotice(message, type) {
  notice.textContent = message;
  notice.className = `notice ${type}`;
  window.clearTimeout(showNotice.hideTimer);
  showNotice.hideTimer = window.setTimeout(() => {
    notice.className = "notice hidden";
  }, 4000);
}

function renderEmptyState(message) {
  return `
    <article class="empty-state">
      <h3>Nothing here yet</h3>
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
