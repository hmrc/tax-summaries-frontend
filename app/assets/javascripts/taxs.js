// No conflict
$(function() {
    // This will be used for the back link
      var docReferrer = document.referrer
      if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
          window.history.replaceState(null, null, window.location.href);
      }
      var backLinkElem = document.getElementById("back-link");
      if (backLinkElem !=  null){
          if (window.history && window.history.back && typeof window.history.back === 'function') {
              var backScript = (docReferrer === "" || docReferrer.indexOf(window.location.host) !== -1) ? "javascript:window.history.back(); return false;" : "javascript:void(0);"
              backLinkElem.setAttribute("onclick",backScript);
              backLinkElem.setAttribute("href","javascript:void(0);");
          }
      }

    var options = {
          outOf: 65, // number to calculate the bar % out of
          applyOnInit: false, // apply the chart immediately
          toggleText: "Toggle between chart and table", // if you want toggle links to be added
          autoOutdent: true, // will automatically place values too big for a bar outside it
          outdentAll: true // will place all bar values just outside the bar rather than sitting in the bar
        },
        table = $("table#tax-spent"),
        chart = $.magnaCharta(table, options),

        link = $(".mc-toggle-link"),
        chartBars = $('.mc-bar-cell span'),
        chartKeys = $('.mc-key-cell'),
        chartBarValues = []; // Stores the figures for each bar

    function getToggleLinkLangText(lang, textType) {
        if (lang === 'Lang(en,GB)') {
            return (textType === 'table') ? 'Text if English and on the table view'
                                          : 'Text if English and on the chart view';
        } else {
            return (textType === 'table') ? 'Text if Welsh and on the table view'
                                          : 'Text if Welsh and on the chart view';
        }
    }

    function modifyToggleLink() {

      if (link.length) {

        link.html(
          (table.hasClass("visually-hidden")) ? chart_toggle_text[0]
                                             : chart_toggle_text[1]
        );

        $("a.mc-toggle-link").blur();

      }
    }

    modifyToggleLink();

    $(document).on("click", "." + link.attr("class"), function(e) {

      e.preventDefault();

      modifyToggleLink();

    });


    $('.error-summary').focus();

    document.getElementById('proposition-menu').querySelector('.js-header-toggle.menu').setAttribute("aria-hidden", "true");

});

