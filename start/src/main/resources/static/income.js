// JS

function reloadData(){
    $.ajax({
      url: "/owner/summary",
      data: {
        time: new Date().getTime()
      },
      success: function( response ) {
        var result = response.data;

        var getTpl = summary.innerHTML;
        var view = document.getElementById('result');
        laytpl(getTpl).render(result, function(html){
          view.innerHTML = html;
        });
        render();
      }
    });
}

reloadData();
