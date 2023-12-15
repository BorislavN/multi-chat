
function onJoinClick(event) {
    const error = document.getElementById("loginError");

    if (error) {
      error.style.opacity=(error.style.opacity==0)?1:0;
    }

    console.log("clicked!");
};