window.addEventListener("load", function(event) {
    var errorSummary = document.getElementById("error-summary-display");
    if(errorSummary) errorSummary.focus();
});

window.addEventListener("load", function(event) {
    var bodyClass = document.querySelector('body');
    var el= document.getElementById("print-page");
    if(el) {
        el.classList.remove("remove-print")
        el.removeAttribute("rel");
    }
});


