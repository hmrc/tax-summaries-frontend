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

    $('.error-summary').focus();

    document.getElementById('proposition-menu').querySelector('.js-header-toggle.menu').setAttribute("aria-hidden", "true");

});

