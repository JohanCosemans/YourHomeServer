
	/** Left panel **/
	
	$(document).ready(function(){

		fillAccordeonImages();

		$("#sidebar-left-wrapper .sidebar-title").click(function() {
			$("#accordion").animate({width:'toggle'},500);
			$("#sidebar-left-wrapper .sidebar-title .title").animate({width:'toggle'},500, function() {
				sizeWrapperWindow();
			});
			$("#sidebar-left-wrapper").toggleClass("sidebar-left-wrapper-width sidebar-left-wrapper-width-auto");
		});

	});
	
	function fillAccordeonImages() {
		var h3TitleObject=null;
		var divObject  = null;
		var url = rootUrl+' /api' + '/Views';
		
		$.getJSON(url, function(data) {

			// Set global variable
			window.images = data;

			$.each(data, function(key, imageSection) {	

					h3TitleObject =  jQuery('<h3/>',{
														text: imageSection.title
													}
											);
					divObject  =  jQuery('<div/>');
					var imgULObject  =  jQuery('<ul/>').appendTo(divObject);
					var imgLIObject =  jQuery('<li/>', { "style":"max-height:650px; overflow-y: auto;"}).appendTo(imgULObject);
					var table = jQuery('<table/>', {"style" : "width:100%;"}).appendTo(imgLIObject);
					var translateWidthCounter = 0;
					
					if(imageSection.title == "User") {
						// Add upload function
						var currentTR =	jQuery('<tr/>').appendTo(table);
						var td = jQuery('<td />',{"class":"", "colspan": "2"}).appendTo(currentTR);
						var file = jQuery('<input />',{"id":"user_image_upload_file", "type": "file", "size": "10"}).appendTo(td);
						file.customFile(file.get(0), {
							url: '/api' + '/Images/StageImage',
							error:	function(ev){ 
										$("#upload_background_button").text("Upload");
										$("#upload_background_button").attr("disabled",false);
									},
							success: function(data){ 
										var result = jQuery.parseJSON(data);
										if(translateWidthCounter%2 != 1) {
											currentTR =	jQuery('<tr/>').appendTo(table);
										}
										translateWidthCounter++;
										prepareAccordeonView(result).appendTo(currentTR);
										findByAttribute(window.images,"title","User").objects.push(result);
											  
										$("#upload_background_button").text("Upload");
										$("#upload_background_button").attr("disabled",false);
									}
						});
					}
					$.each(this.objects, function(key, image) {
						
						if(translateWidthCounter%2 != 1) {
							currentTR =	jQuery('<tr/>').appendTo(table);
						}
						translateWidthCounter++;
						prepareAccordeonView(image).appendTo(currentTR);
					});
					
					$("#accordion").append(h3TitleObject);
					$("#accordion").append(divObject);
				
			});
			
			$( "#accordion" ).togglePanels();
			
			
		});
		
	}
	function refreshPaginator() {
		
	}
	function prepareAccordeonView(viewDetails) {
		var td = jQuery('<td />',{"class":"accordeonTable"});
		var a = jQuery('<a />',{"href":"javascript://"}).appendTo(td);
		var img = jQuery('<img/>',{
				"src": viewDetails.icon, 
				"class":"draggable accordeonImage",
				"rel" : viewDetails.id,
				"title" : ""
			  }).appendTo(a);
		activateDraggable(img, viewDetails);
		activateTooltip(a, viewDetails);
		return td;
	}
	function getTypeIcon(valueType) {
		var type = window.typeMap[valueType];
		if(type != undefined) {
			return $("<i/>", { "class" : type.icon, 
										"title": type.description,
										"style" : "padding: 4px;" });
		}
		return null;
	}
	function getAllowsImageList(viewDetails) {
		var show = false;
		var acceptSpan = $("<span/>", { "text" : "Accepts: "});
		$.each(viewDetails.allowed, function(index, valueType) {
			var allowedImage = getTypeIcon(valueType);
			if(allowedImage != undefined) {
				allowedImage.addClass("ic-2x");
				acceptSpan.append(allowedImage);	
				show = true;
			}
		});
		return show?acceptSpan:$("<span/>", { "text" : "This view does not accept controls"});;
	}
	function activateTooltip(baseObject, viewDetails){
		var position  = { my: 'center top', at: 'center bottom+10' };
		baseObject.tooltip( 
		{	position: position,
			show:	false,
			content: function() {
						var element = $( this );
						var returnDiv = $("<div />");
						if(viewDetails.title != undefined) {
							returnDiv.append($("<h2/>", { "text" : viewDetails.title }));
						}
						returnDiv.append(getAllowsImageList(viewDetails))
						return returnDiv;
					},
			open: function(event, ui) {
				if (typeof(event.originalEvent) === 'undefined')
				{
					return false;
				}
				var $id = $(ui.tooltip).attr('id');
				// close any lingering tooltips
				$('div.ui-tooltip').not('#' + $id).remove();
			},
			close: function(event, ui) {
				ui.tooltip.hover(function()
				{
					$(this).stop(true).fadeTo(400, 1); 
				},
				function()
				{
					$(this).fadeOut('400', function()
					{
						$(this).remove();
					});
				});
			}
		});
	}
	function activateDraggable(baseObject, imageDetails) {
			baseObject.draggable({
				containment: 'svgObject',
//				helper: "clone",
				helper: function(event, ui) {

					var helper = null;
					//var imageID = $(this).attr("rel");
					//var imageDetails = getViewDetailsById(imageID);
					if( imageDetails.draggable != undefined ) {
						helper = jQuery("<img />",{ "src": imageDetails.draggable, "rel" : imageDetails.id} );
					}else {
						helper = $(this).clone();
					}

					return helper;
				},
				cursor: "move",
				appendTo: "body",
				snap: true,
				start: function(event, ui){

//					console.log(ui.helper);
					if(event.shiftKey) {
						$(this).draggable( "option", "snap", true );
					}else {
						$(this).draggable( "option", "snap", false );
					}	
				 },
				 drag : function( event, ui) {
					if(event.shiftKey) {
						$(this).draggable( "option", "snap", true );
					}else {
						$(this).draggable( "option", "snap", false );
					}	
					var originalWidth = ui.helper[0].naturalWidth;
					var originalHeight = ui.helper[0].naturalHeight;
					var newWidth =  originalWidth * window.zoomRatio	* 0.9;
					var newHeight = originalHeight * window.zoomRatio  * 0.9;
					ui.helper.removeClass("accordeonImage");
					if(newWidth != 0 && newHeight != 0) {
						ui.helper.css({"width":newWidth+'px'
						,"height":newHeight+'px',"z-index":window.currentProject.zIndexCounterUp
						});
					}
	
				 }

			});
		}
	