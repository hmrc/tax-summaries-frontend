// No conflict
(function($) {

  // Document ready
  $(function() {

    var options = {
          outOf: 65, // number to calculate the bar % out of
          applyOnInit: true, // apply the chart immediately
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

        var gaLinkText = table.hasClass("visually-hidden") ? chart_toggle_text_ga[0] : chart_toggle_text_ga[1];

        link.attr('data-journey-click', "link - click:Your taxes and public spending:" + gaLinkText);


        $("a.mc-toggle-link").blur();

      }
    }

    modifyToggleLink();

    $(document).on("click", "." + link.attr("class"), function(e) {

      e.preventDefault();

      modifyToggleLink();

    });

    // This fixes the issue with the header menu for mobile
    var els = $('a.js-header-toggle.menu');
    var menu_items = document.getElementById('proposition-links');

    function toggleHeaderMenu() {

      if ( $(menu_items).hasClass("js-hidden") || (!$(menu_items).hasClass("js-visible") && !$(menu_items).hasClass("js-hidden")) ) {

        $(menu_items).removeClass("js-hidden")
                     .addClass("js-visible");

        els.addClass('toggled');

      } else if ( $(menu_items).hasClass("js-visible") ) {

        $(menu_items).removeClass("js-visible")
                     .addClass("js-hidden");

        els.removeClass('toggled');

      }
    }

    els.bind('click', function(e) {

      e.preventDefault();

      if (typeof menu_items !== 'undefined')
        toggleHeaderMenu();

    });

    $('#errors').focus();

  });
})(window.jQuery)
