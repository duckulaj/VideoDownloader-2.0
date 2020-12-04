$(document).ready(function()
{


	function openInformationModal () {
	    	
	        var resourceId = $(this).attr("searchInfo");
	        $.ajax({
	            url: "https://api.themoviedb.org/3/search/movie/" + resourceId + "?language=en-US",
	            data: {
	                api_key: "56744a89f97f5119fe2a2306794d29a3"
	            },
	            dataType: 'json',
	            success: function (result, status, xhr) {
	                $("#modalTitleH4").html(result["itle"]);
	
	                var image = result["profile_path"] == null ? "Image/no-image.png" : "https://image.tmdb.org/t/p/w500/" + result["profile_path"];
	                var biography = result["biography"] == null ? "No information available" : result["biography"];
	
	                var resultHtml = "<p class=\"text-center\"><img src=\"" + image + "\"/></p><p>" + biography + "</p>";
	                resultHtml += "<p>Birdthday: " + result["birthday"] + "</p><p>Place of Birth: " + result["place_of_birth"] + "";
	
	                $("#modalBodyDiv").html(resultHtml)
	
	                $("#myModal").modal("show");
	            },
	            error: function (xhr, status, error) {
	                $("#message").html("Result: " + status + " " + error + " " + xhr.status + " " + xhr.statusText)
	            }
	        });
	    });

});    
