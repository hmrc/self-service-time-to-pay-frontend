$(document).ready(function() {

    //Accessibility
    var errorSummary =  $('#error-summary-display'),
    $input = $('input:text')
    //Error summary focus
    if (errorSummary){ errorSummary.focus() }
    $input.each( function(){
        if($(this).closest('label').hasClass('form-field--error')){
            $(this).attr('aria-invalid', true)
        }else{
            $(this).attr('aria-invalid', false)
        }
    });





    var cookieData = window.GOVUKFrontend.getCookie("UrBannerHide");
    if (cookieData == null) {
        $("#ur-panel").css('display', 'flex');
    }
    $(".banner-panel__close").on("click", function (e) {
        e.preventDefault();
        window.GOVUK.setCookie("UrBannerHide", "suppress_for_all_services", 99999999999);
        $("#ur-panel").hide();
    });
});
