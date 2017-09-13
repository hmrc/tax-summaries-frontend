
if (!String.prototype.trim) {
  String.prototype.trim = function () {
    return this.replace(/^\s+|\s+$/g,'');
  };
}

$(function(){
  $('body').on('click', '.more-help-link a', function(e){

    e.preventDefault();

    var $this = $(this),
      $moreHelpWrap = $this.closest('.more-help-wrap'),
      $moreHelpContent = $moreHelpWrap.find('.more-help-content');

    $moreHelpContent.toggle();

    if ($moreHelpContent.is(':visible')){
      $moreHelpWrap.find('.more-help-link i').removeClass('icon-caret-right').addClass('icon-caret-down');
    } else {
      $moreHelpWrap.find('.more-help-link i').addClass('icon-caret-right').removeClass('icon-caret-down');
    }
  });


  $('body').on('click', '.toggle-link', function(e){
    e.preventDefault();
    var target = $(this).attr('data-target');
    $('#'+target).toggle();
  });


  $('body').on('change', 'input[type="checkbox"]', function(e){
    var $this = $(this);
    $this.closest('label').toggleClass('selected', $this.is(':checked'));
  });

  $('body').on('change', 'input,select,textarea', function(){
    var $this = $(this);

    // toggle optional sections
    if ($this.is(':checkbox')){

      var $toggleTarget = $('.optional-section-'+$this.attr('name') + '[data-toggle-value="'+$this.val() + '"]');

      if ($toggleTarget.length){
        $toggleTarget.toggle($this.is(':checked') && $this.val() == $toggleTarget.attr('data-toggle-value'));
      }

    } else if ($this.is(':radio')){

      var $toggleTarget = $('.optional-section-'+$this.attr('name'));

      $toggleTarget.each(function(){
        var $thisTarget = $(this);
        var toggleValue = $thisTarget.data('toggle-value').toString().split(',');
        var toggleFlag = false;
        for (var i = toggleValue.length - 1; i >= 0; i--) {
          if ($this.val() == toggleValue[i]) toggleFlag = true;
        };
        $thisTarget.toggle(toggleFlag);
      });
    }

  });

  // handling forms because middleman can't
  $('form.handled-by-js').on('submit', function(e){

    var $this = $(this),
      $checked = $this.find('input:checked');

    if ($checked.length === 0){
      $this.find('.error-notification').show();
      return false;
    }

    window.location.href = $checked.first().attr('data-url');
    return false;
  });

  // active taxes
  var activeTaxes = localStorage['activeTaxes'];
  activeTaxes = (activeTaxes) ? JSON.parse(activeTaxes) : ['self-assessment', 'vat'];

  $activeTaxesInputs = $('.active-taxes input');

  if (activeTaxes.length > 0){

    $('body').addClass(activeTaxes.join(' '));
    if ($activeTaxesInputs.length > 0){

      for (var i = 0; i < activeTaxes.length; i++){
        $activeTaxesInputs.filter('[value="' + activeTaxes[i] + '"]').attr('checked', true).closest('.selectable').addClass('selected');
      }
    }
  }

  $('.active-taxes input').on('click', function(){

    var activeTaxes = [];

    $('.active-taxes input:checked').each(function(){
      activeTaxes.push($(this).val());
    });

    localStorage['activeTaxes'] = JSON.stringify(activeTaxes);
  });
});

$(document).ready(function() {

  // Example - Highlight grid

  if ($('.js-highlight-grid').length>0) {
    $('.js-highlight-grid').click(function(e) {

      e.preventDefault();
      var html = $('html');

      if ($('.is-inner-block-highlight').length>0) {
        // Don't add more than once
      } else {
        $('.grid .inner-block').wrapInner('<div class="is-inner-block-highlight"></div>');
      }

      if (html.hasClass('example-highlight-grid')) {
          html.removeClass('example-highlight-grid');
      } else {
          html.addClass('example-highlight-grid');
      }
    });
  }

  // Example - Form focus styles

  if ($('.form').length>0) {
    $(".block-label").each(function() {

      // Add focus
      $(".block-label input").focus(function() {        
        $("label[for='" + this.id + "']").addClass("add-focus");
        }).blur(function() {
        $("label").removeClass("add-focus");
      });
      // Add selected class
      $('input:checked').parent().addClass('selected');

    });

    $(".block-label input[data-url]").focus(function() {
      $('a.button').attr('href', $(this).data('url'));
    });    

    // Add/remove selected class
    $('.block-label').find('input[type=radio], input[type=checkbox]').click(function() {

      $('input:not(:checked)').parent().removeClass('selected');
      $('input:checked').parent().addClass('selected');

      // Hide open data-toggle content
      $('.toggle-content').hide();

      // Show data-toggle content
      var target = $(this).parent().attr('data-target');
      $('#'+target).show();
    });

  }
});

handleCounter = function (counted) {
  var counterId = '#' + counted.id + '-counter';
  $(counterId).html((2500 - counted.value.length) +  " characters remaining (limit is 2500 characters)");
}

initCounters = function () {
  $('.counted').each(function (index) {
    this.oninput = function () {
      this.onkeydown = null;
      handleCounter(this);
    };

    this.onkeydown = function () {
      handleCounter(this);
    };
  });
}

$(document).ready(function() {
  initCounters();
  $('')
});

