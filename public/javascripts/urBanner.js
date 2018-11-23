$(document).ready(function() {
    var cookieData = GOVUK.getCookie("UrBannerHide");
    if (cookieData == null) {
        $("#ur-panel").show();
    }
    console.log("Hello world");
    $(".banner-panel__close").on("click", function (e) {
        e.preventDefault();
        GOVUK.setCookie("UrBannerHide", "suppress_for_all_services", 99999999999);
        $("#ur-panel").hide();
    });
});