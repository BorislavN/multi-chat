var currentName;
var lastMessage;

const COMMAND_DELIMITER = "@";
const ACCEPTED_FLAG = "accepted";
const EXCEPTION_FLAG = "exception";

const loginDiv = document.getElementById("login");
const loginError = document.getElementById("loginError");
const usernameInput = document.getElementById("usernameField");
const joinBtn = document.getElementById("joinBtn");

const mainDiv = document.getElementById("main");
const userPane = document.getElementById("userPane");
const backBtn = document.getElementById("backBtn");
const textArea = document.getElementById("area");
const messageInput = document.getElementById("messageField");
const sendBtn = document.getElementById("sendBtn");

const socket = new WebSocket("ws://localhost:80");

socket.addEventListener("open", (event) => {
  console.log(`Websocket connected!`);

  joinBtn.disabled = false;
});

socket.addEventListener("message", (event) => {
  let message = event.data;

  let isAccepted = message.startsWith(ACCEPTED_FLAG.concat(COMMAND_DELIMITER));
  let isError = message.startsWith(EXCEPTION_FLAG.concat(COMMAND_DELIMITER));

  if (isAccepted || isError) {
    let parts = message.split(COMMAND_DELIMITER);

    if (isError) {
      this.showLoginError(parts[1]);
    }

    if (isAccepted) {
      currentName = parts[1];
      this.showMainPage();
    }

    return;
  }

  this.appendToTextArea(message);
});

socket.addEventListener("error", (event) => {
  let message = event.target.readyState === 3 ? "Connection closed!" : "Exception occurred!";

  console.log(message);

  this.disableAllButtons();

  this.showLoginError(message);
  this.setAnnouncement(message);
});

socket.addEventListener("close", (event) => {
  this.disableAllButtons();

  console.log(event.reason);

  this.showLoginError("Connection closed!");
  this.setAnnouncement("Connection closed!")
});

document.addEventListener("keydown", this.onEnter);

function onJoinClick() {
  let value = usernameInput.value;
  let status = this.validateText(value);

  if ("valid" !== status) {
    this.showLoginError(status);

    return;
  }

  if (currentName === value) {
    this.showMainPage();

    return;
  }

  socket.send(`${COMMAND_DELIMITER}${value}`);
};

function onSendClick() {
  //TODO: check close
  socket.close(1000, "yoo")
  return;

  let value = messageInput.value;
  let status = this.validateText(value);

  if ("valid" !== status) {
    this.setAnnouncement(status);

    return;
  }

  if (lastMessage === value) {
    this.appendToTextArea("Chat spamming!");
    messageInput.value = "";

    return;
  }

  socket.send(`${currentName}: ${value}`);

  messageInput.value = "";
  lastMessage = value;

  this.setAnnouncement();
};

function appendToTextArea(message) {
  let isAtBottom = (textArea.scrollTop + textArea.clientHeight) === textArea.scrollHeight;

  textArea.value += `${message}\n`;

  if (isAtBottom) {
    textArea.scrollTop = textArea.scrollHeight;
  };
}

function onChangeNameClick() {
  this.toggleVisibility();
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
    joinBtn.click();
  }

  if ("messageField" === active.id) {
    sendBtn.click();
  }
};

function showMainPage() {
  loginError.style.opacity = "0";

  this.setAnnouncement();
  this.toggleVisibility();
}

function setAnnouncement(errorMessage) {
  if (errorMessage) {
    userPane.classList.add("error");
    userPane.textContent = errorMessage;

    return;
  }

  userPane.classList.remove("error");
  userPane.textContent = `Welcome, ${currentName}!`;
};

function showLoginError(message) {
  loginError.textContent = message;
  loginError.style.opacity = "1";
};

function toggleVisibility() {
  let temp = loginDiv.style.display;

  loginDiv.style.display = mainDiv.style.display;
  mainDiv.style.display = temp;
};

function disableAllButtons() {
  joinBtn.disabled = true;
  backBtn.disabled = true;
  sendBtn.disabled = true;
};

function validateText(text) {
  if (text.trim().length === 0) {
    return "Can not be blank!";
  }

  return "valid";
};