$(function() {

    $("a.trackedOutboundLink").on("mouseup", function () {
        ga("send", "event", "click-outgoing-link", $(this).attr("href"));
    });

    $("a.trackedSignInSidebar").on("mouseup", function () {
        ga("send", "event", "click-sidebar-signin", window.location.pathname);
    });

    $("a.trackedAssistanceSidebar").on("mouseup", function () {
        ga("send", "event", "click-direct-debit-assistance", window.location.pathname);
    });
    $(".openOnPrint").on('mouseup', function () {
        if ($('html').hasClass('no-details')) then ($("body")).find('details').addClass("open")
        else ($("body")).find('details').attr("open", "")
    })
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
    //This is used to prevent a user from entering letters
    $(".numbersonly input").on("keypress", function () {
        var charCode = event.keyCode;
        if (charCode > 31 && (charCode < 48 || charCode > 57))
            return false;
    });
    //This is used to prevent a user from entering letters or more then two digits after a decimal
    $(".moneyonly input").on("keypress", function () {
        var character = String.fromCharCode(event.keyCode)
        var newValue = this.value + character;
        if (isNaN(newValue) || hasDecimalPlace(newValue, 3)) {
            event.preventDefault();
            return false;
        }
    });

    function hasDecimalPlace(value, x) {
        var pointIndex = value.indexOf('.');
        return  pointIndex >= 0 && pointIndex < value.length - x;
    };
});
