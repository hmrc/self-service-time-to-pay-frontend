$(function() {

    $("a.trackedOutboundLink").on("mouseup", function () {
        ga("send", "event", "click-outgoing-link", $(this).attr("href"));
    });

    $("a.trackedSignInSidebar").on("mouseup", function () {
        ga("send", "event", "click-sidebar-signin", window.location.pathname);
    });

    $(".trackedPrintLink").on("mouseup", function () {
        ga("send", "event", "click-print", window.location.pathname);
    });

    $(".trackedDetails").on('toggle', function () {
        var d = $(this),
            state = (d.prop("open") === true) ? "open" : "close";
        ga("send", "event", state, d.attr("id"));
    })

    $("#calculatorInitialPaymentForm").on("submit", function () {
        ga("send", "event", "calculator-payment-today-update", $(this).find("input[name=amount]").val());
    });

    $("#arrangementPaymentDayForm").on("submit", function () {
        ga("send", "event", "arrangement-day-of-month-change", $(this).find("input[name=dayOfMonth]").val());
    });


    $("#calculatorDurationForm").on("submit", function () {
        ga("send", "event", "calculator-duration-update", $(this).find("input[name=months]").val());
    });

    $("#calculatorDurationDecrease").on("mouseup", function () {
        ga("send", "event", "calculator-duration-decrease-from", $("input[name=months]").val());
    });

    $("#calculatorDurationIncrease").on("mouseup", function () {
        ga("send", "event", "calculator-duration-increase-from", $("input[name=months]").val());
    });
});