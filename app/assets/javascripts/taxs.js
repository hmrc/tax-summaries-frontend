(function(){
  // =====================================================
  // Back link mimics browser back functionality
  // =====================================================
  // store referrer value to cater for IE - https://developer.microsoft.com/en-us/microsoft-edge/platform/issues/10474810/  */
  var docReferrer = document.referrer;
  // prevent resubmit warning
  if (
    window.history &&
    window.history.replaceState &&
    typeof window.history.replaceState === 'function'
  ) {
    window.history.replaceState(null, null, window.location.href);
  }


  const printlink = document.getElementById('printLink');

  if(printlink != null && printlink != 'undefined' ) {

      printlink.addEventListener("click", function (e) {
          e.preventDefault();
          window.print();
      });
  };
})();
