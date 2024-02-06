var currentName;
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
  let message =event.data;
  const namePrefix="#user";

  if(message.startsWith(namePrefix)){
    let temp=message.substring(namePrefix.length);

    if(temp.length>0){
      currentName=temp;
      this.showMainPage();
    }else{
      this.showLoginError("Invalid username!")
    }
    
    return;
  }

  let isAtBottom = (textArea.scrollTop+textArea.clientHeight) === textArea.scrollHeight;

  textArea.value += `${message}\n`;

  if (isAtBottom) {
    textArea.scrollTop = textArea.scrollHeight;
  };
});

socket.addEventListener("error", (event) => {
  let message = event.target.readyState === 3 ? "Connection closed!" : "Exception occurred!";

  console.log(message);

  this.disableAllButtons();

  this.showLoginError("Exception occurred!");
  this.setGreeting("Exception occurred!")
});

socket.addEventListener("close", (event) => {
  this.disableAllButtons();

  //Echo back close frame
  socket.close(event.code);

  this.showLoginError("Connection closed!");
  this.setGreeting("Connection closed!")
});

document.addEventListener("keydown", this.onEnter);

function onJoinClick() {
  let value = this.escapeString(usernameInput.value);
  let status = this.validateText(value);

  if ("valid" !== status) {
    this.showLoginError(status);

    return;
  }

  socket.send(`#user${value}`);
};

function showMainPage(){
  loginError.style.opacity = "0";

  this.setGreeting();
  this.toggleVisibility();
}

function onSendClick() {
  let value = this.escapeString(messageInput.value);
  let status = this.validateText(value);

  if ("valid" !== status) {
    this.setGreeting(status);

    return;
  }

  socket.send(`${currentName}: ${value}`);

  messageInput.value = "";
  this.setGreeting();
};

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

function setGreeting(errorMessage) {
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

function validateText(text) {
  if (text.trim().length === 0) {
    return "Can not be blank!";
  }

  return "valid";
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

function escapeString(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
};