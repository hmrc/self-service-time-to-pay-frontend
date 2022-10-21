
window.GOVUKFrontend.initAll();
window.HMRCFrontend.initAll();


function uncheck(id){
        document.getElementById(id).checked = false

        if(id=="other"){document.getElementById("customDate").style.display = "none"} else {document.getElementById("customDate").style.display = "block"}
    }