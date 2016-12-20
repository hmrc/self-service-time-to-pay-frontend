$("a.trackedOutboundLink").on("mouseup", function() {
  ga("send", "event", "click-outgoing-link", $(this).attr("href"));
});

$("a.trackedSignInSidebar").on("mouseup", function() {
    ga("send", "event", "click-sidebar-signin", $("h1, h2").first().text() );
});

$(".trackedPrintLink").on("mouseup", function() {
    ga("send", "event", "click-print", $("h1, h2").first().text());
});

$(".trackedBackLink").on("mouseup", function() {
    ga("send", "event", "clicked-back", $("h1, h2").first().text());
});


$("#calculatorInitialPaymentForm").on("submit", function() {
    ga("send", "event", "calculator-payment-today-update", $(this).find("input[name=amount]").val());
});


$("#calculatorDurationForm").on("submit", function() {
    ga("send", "event", "calculator-duration-update", $(this).find("input[name=months]").val());
});

$("#calculatorDurationDecrease").on("mouseup", function() {
    ga("send", "event", "calculator-duration-decrease-from", $("input[name=months]").val());
});

$("#calculatorDurationIncrease").on("mouseup", function() {
    ga("send", "event", "calculator-duration-increase-from", $("input[name=months]").val());
});