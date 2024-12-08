
function loadOptionsExpDate(code, market){
    console.log('loadOptionsExpDate code:'+ code+' market:'+ market);
    $.ajax({
      url: "/options/strike/list",
      data: {
        code: code,
        market: market,
        time: new Date().getTime()
      },
      success: function( result ) {
        console.log('/options/strike/list:'+ result);
      }
    });
}

function reloadData(){
    $.ajax({
      url: "/underlying/list",
      data: {
        owner: $("#owner").val(),
        time: new Date().getTime()
      },
      success: function( result ) {
        var output = document.getElementById("underlying");
        output.innerHTML = "";

        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            output.innerHTML += '<li class="nav-item"><a href="#'+obj.code+'" class="nav-link" id="'+obj.code+'" onclick="loadOptionsExpDate(\''+obj.code+'\',\''+obj.market+'\')">'+obj.code+'</a></li>';
        }
      }
    });
}




$(function() {

    $('#reload-btn').on('click', function() {
        reloadData();
    });

    $(document).ready(function() {
       $(window).on('hashchange', function() {
           var hash = window.location.hash;
           $(".nav-link").removeClass("active");
           $(hash).addClass("active");
           console.log('The hash has changed to '+ hash);
       });
    });

    reloadData();
});