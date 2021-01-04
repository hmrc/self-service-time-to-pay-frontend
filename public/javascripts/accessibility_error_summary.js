/*
 * This script solves a problem
 * the Select tax to pay page
 *  error summary section
 * The href has a value which does not
 * match the id of the first radio button
 */

window.addEventListener("load", function(event) {
    var linkHref= document.getElementById("regime-error-summary");
    if(linkHref) {
        linkHref.setAttribute("href", "option-vat");
    }
});
