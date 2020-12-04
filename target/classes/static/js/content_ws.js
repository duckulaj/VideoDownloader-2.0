var stompClient = null;

$(document).ready(function()
{
    $('#btnDownload').click(function () {
    	
    	  $.get("/");
    });
    
    $('#btnInterrupt').click(function () {
    	
      var downloadName = $('#name').val();
      $.get("/interrupt/"+downloadName);
    });
    
    $('#btnPause').click(function () {
    	
        var downloadName = $('#name').val();
        $.get("/pause/"+downloadName);
      });
    
    

    connect();
    //setTimeout(updateProgress, 10);
});

function connect(){
    var socket = new SockJS('/mystatus');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame){
    	stompClient.debug = function(str) {};
        stompClient.subscribe('/app/initial', function (messageOutput){
            // console.log("INITIAL: "+messageOutput);
            var progressList = $.parseJSON(messageOutput.body);
            $.each(progressList,function(index, element){
                update(element);
            });
        });

        stompClient.subscribe('/topic/status', function(messageOutput) {
            // console.log("New Message: "+messageOutput);
            var messageObject = $.parseJSON(messageOutput.body);
            update(messageObject);
        });


    });
}


function update(newMessage){

    var rows = $('#tableBody').find('#'+newMessage.jobName);
    if(rows.length === 0)
    {
        $('#tableBody').append('<tr id="'+newMessage.jobName+'">' +
            /*'<td>'+newMessage.jobName+'</td>'+*/
            '<td>'+newMessage.originalFileName+'</td>' +
            '<td>'+
            '<div class="progress"> <div class="progress-bar" role="progressbar" aria-valuenow="" aria-valuemin="0" aria-valuemax="100" style="min-width: 2em;"> 0%</div></div>' +
            '</td>'+
            '<td class="state"></td>' +
            '<td><form name="interruptForm" action="/interrupt">' +
            '<input type="hidden" value="'+newMessage.jobName+'" name="name" id="name" />' +
            '<button id="btnInterrupt" value="'+newMessage.jobName+'" type="submit" class="btn btn-outline-success">' +
            '<span class="glyphicon glyphicon-stop" aria-hidden="true"></span>' +
            ' Stop Job' +
            '</button>' +
            '</form></td>' +
            /*'<td><form name="pauseForm" action="/pause">' +
            '<input type="hidden" value="'+newMessage.jobName+'" name="name" id="name" />' +
            '<button id="btnPause" value="'+newMessage.jobName+'" type="submit" class="btn btn-outline-success">' +
            '<span class="glyphicon glyphicon-pause" aria-hidden="true"></span>' +
            ' Pause Job' +
            '</button>' +
            '</form></td>' +*/
            '</tr>');
    }
    
    //set stuffs
    var parentDiv = $('#'+newMessage.jobName);
    parentDiv.find('.progress-bar').html(newMessage.progress +"%");
    parentDiv.find('.progress-bar').css('width',newMessage.progress+'%').attr("aria-valuenow",newMessage.progress);
    parentDiv.find('.state').text(newMessage.state);
    if(newMessage.state == "DONE")
    {
        parentDiv.removeClass("active info success").addClass("success");
    }
    else if(newMessage.state == "RUNNING")
    {
        parentDiv.removeClass("active info success").addClass("active");
    }
    else if(newMessage.state == "NEW")
    {
        parentDiv.removeClass("active info success").addClass("info");
    }
    //end set stuffs
}