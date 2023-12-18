var currentName;

var loginDiv = document.getElementById("login");
var mainDiv = document.getElementById("main");
var userPane = document.getElementById("userPane");

document.addEventListener("keydown", this.onEnter);

//TODO: add Websocket logic

function onJoinClick() {
  const errorPane = document.getElementById("loginError");
  const usernameInput = document.getElementById("usernameField");

  let value = this.escapeString(usernameInput.value);
  let status = this.validateText(value);

  if ("valid" !== status) {
    errorPane.textContent = status;
    errorPane.style.opacity = "1";

    return;
  }

  //Check if username is taken

  errorPane.style.opacity = "0";
  currentName = value;

  this.setGreeting();
  this.toggleVisibility();
};

function onSendClick() {
  const area = document.getElementById("area");
  const messageInput = document.getElementById("messageField");

  let value = this.escapeString(messageInput.value);
  let status = this.validateText(value);

  if ("valid" !== status) {
    this.setGreeting(status);

    return;
  }

  //Send to server

  messageInput.value = "";
  this.setGreeting();
  area.value += `${currentName}: ${value}\n`;
};

function onEnter(event) {
  if ("Enter" !== event.key) {
    return;
  }

  let active = document.activeElement;

  if ("INPUT" !== active.tagName) {
    return;
  }

  if ("usernameField" === active.id) {
    document.getElementById("joinBtn").click();

    return;
  }

  if ("messageField" === active.id) {
    document.getElementById("sendBtn").click();

    return;
  }
}



function setGreeting(errorMessage) {
  if (errorMessage) {
    userPane.classList.add("error");
    userPane.textContent = errorMessage;

    return;
  }

  userPane.classList.remove("error");
  userPane.textContent = `Welcome, ${currentName}!`
};

function validateText(text) {
  if (text.trim().length === 0) {
    return "Can not be blank!";
  }

  if (text.includes("|")) {
    return "Can not include \"|\"!";
  }

  return "valid";
};

function onChangeNameClick() {
  this.toggleVisibility();
};

function toggleVisibility() {
  let temp = loginDiv.style.display;

  loginDiv.style.display = mainDiv.style.display;
  mainDiv.style.display = temp;
};

function escapeString(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
};