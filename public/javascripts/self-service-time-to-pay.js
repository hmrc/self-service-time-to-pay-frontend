$(".trackedOutboundLink").click(function() {
    ga('send', 'event', 'outbound', 'click', $(this).attr("href"));
});


$(".back-link").click(function() {
    ga('send', 'event', 'back', 'click', $("h1").text());
});
