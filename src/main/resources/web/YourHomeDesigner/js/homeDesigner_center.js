
	/** Center panel **/
	function initialize() {
		/*** Declare Global variables ***/

		// UI related global variables
		window.zoomRatio = 1;
		window.currentPageId = -1;
		window.stageItemCounter	= 0;
		
		// Current project settings:
		window.currentProject = new Object();
		
		var date = new Date();
		window.currentProject.fileName	= "New Project "+date.getDate()+"-"+(date.getMonth()+1)+"-"+date.getFullYear()+".json";
		$("#currentProject").text(window.currentProject.fileName.replace(".json",""));
		window.currentProject.zIndexCounterUp	= 10001;
		window.currentProject.zIndexCounterDown = 10000;
		//window.currentProject.bindings = new Array();
		$( "#screen-size-width" ).spinner();
		$( "#screen-size-height" ).spinner();
		saveScreenSize();
		clearStage();
		$("#tooltip_menu_list").empty();

		// Load of a new project: Create one empty screen.
		createPage("New screen",true,false,undefined);

		sizeWrapperWindow();
		sizeHtmlStage();

		// Fit on screen
		zoomFit();
	
	}
	function createStageItem(objectDetails) {
			if(objectDetails.viewProperties != null) {
				var element = null;
				switch(objectDetails.viewProperties.type) {
					case "text":
						var contentProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "content");
						var colorProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "color");
						var fontSizeProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "size");
						var alignmentProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "alignment");
						var newFontSize = parseFloat(fontSizeProperty.value)*window.zoomRatio;
						element = $("<span />", { 
													"class" : "textProperty appFont",
													"text"	: contentProperty.value,
													"style"	: "color:"+colorProperty.value+"; font-size:"+newFontSize+"px;"
										 		});
										 		
						element.css("height","initial");
						element.css("width","initial");
						if(alignmentProperty != null) {
							var float = alignmentProperty.value;
							if(float=="center") { float = "initial" }
							//element.css("text-align",alignmentProperty.value);
							element.css("float",float);
						}
					break;
					case "shape":
						var colorProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "color");
						var borderRadiusProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "corner_radius");
						element = $("<div />", { 
													"class" : "shape",
													"style"	: "background-color:"+colorProperty.value+"; border-radius:"+borderRadiusProperty.value+"px;"
										 		});
										 		
						element.css("height","100%")
						element.css("width","100%")
					break;
					case "web_static_html":
						var contentProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "content");
						element = $("<div />", { 
													"class" : "web_static_html"
										 		});
										 		
						element.css("height","100%");
						element.css("width","100%");
						element.html(contentProperty.value);
					break;
					case "web_link":
						var linkProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "url");
						element = $("<div />", {"class" : "web_link"});
						var span = $("<span />", { 
													"text" : "This is an example - it could be shown differently on your device.",
													"style" : "background-color:red;font-size:8px;display: inline-block;"
										 		});
						var divOverlay = $("<div />", {"class": "webOverlay" });

						var iframe = $("<iframe />", { 
													"src"   : linkProperty.value,
													"frameBorder" : "0"
										 		});

						element.append(span);
						element.append(divOverlay);
						element.append(iframe);
										 		
						span.css("height","10%");
						span.css("width","100%");
						iframe.css("height","90%");
						iframe.css("width","100%");
						element.css("height","100%");
						element.css("width","100%");
					break;
					case "web_rss":
						var linkProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "url");
						var colorProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "color");
						var fullUrl = "/rss/rss.html?feedUrl="+encodeURIComponent(linkProperty.value)+"&textColor="+colorProperty.value.substr(1);
						element = $("<div />", {"class" : "web_link"});
						var divOverlay = $("<div />", {"class": "webOverlay" });
						
						var iframe = $("<iframe />", { 
													"src"   : fullUrl,
													"frameBorder" : "0"
										 		});

						element.append(divOverlay);
						element.append(iframe);
										 		
						iframe.css("height","100%");
						iframe.css("width","100%");
						element.css("height","100%");
						element.css("width","100%");
					break;
					case "clock_digital":
						var amPmProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "AMPM");
						var colorProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "color");
						var fontSizeProperty = findByAttribute(objectDetails.viewProperties.properties, "key", "size");
						var newFontSize = parseFloat(fontSizeProperty.value)*window.zoomRatio;
						element = $("<span />", { 
													"class" : "textProperty appFont",
													"style"	: "color:"+colorProperty.value+"; font-size:"+newFontSize+"px;"
										 		});
						var currentDate = new Date(); var currentDateString = "";
						if(amPmProperty.value == true || amPmProperty.value == 'true') {
							currentDateString = formatTime(currentDate, true);
						}else {
							currentDateString = formatTime(currentDate, false);
						}
						element.text(currentDateString);
						element.css("height","initial");
						element.css("width","initial");
					break;
					default:
						element = $("<img />", { 
													"src" : objectDetails.viewProperties.draggable,
										 		});

						element.css("height","100%")
						element.css("width","100%")
					break;
				}
				
				var wrappedElement = addSelectionHandlesTo(element, objectDetails.viewProperties);
				var popover = getPopover(wrappedElement,		// Wrapped stage object
											objectDetails.viewProperties, 		// View details
											objectDetails.bindingProperties,				// control details
										    null		    	// optional binding (for defaulting values)
									    );
									    
				element.removeAttr("rel");
				wrappedElement.append(popover);
				popover.hide();
				wrappedElement.find("#selectionWrapper").on("click", function (event) {
					if(event.ctrlKey){
						selectObject(event.target.parentNode, false);
					}else {
						selectObject(event.target.parentNode, true);
					}
				});
				
				wrappedElement.find("#selectionWrapper").css("z-index",++window.currentProject.zIndexCounterUp);
				wrappedElement.find(".stageItem").css("z-index",window.currentProject.zIndexCounterUp);
/*
				switch(objectDetails.viewProperties.type) {
					case "text":
					case "clock_digital":
					//wrappedElement.find("#selectionWrapper").css("overflow")
					break;
					default:
					break;
				}
*/


				}
				return wrappedElement;
		}

	$(document).ready(function(){

		// Set sizing of screens
		$(window).resize(function() { sizeWrapperWindow(); });
		
		// Make HTMLStage droppable
		$("#htmlStage").droppable({
			accept: "#sidebar-left-wrapper li img",
			drop: function(ev, ui) {
				var relId = $(ui.helper).attr("rel");
				var stageObjectDetails = new Object();
				stageObjectDetails.viewProperties = getViewDetailsById(relId);
				stageObjectDetails.viewProperties.top = ($(ui.helper).offset().top - $("#htmlStage").offset().top)/window.zoomRatio;
				stageObjectDetails.viewProperties.left = ($(ui.helper).offset().left  - $("#htmlStage").offset().left)/window.zoomRatio;
				stageObjectDetails.viewProperties.width = $(ui.helper).width()/window.zoomRatio;
				stageObjectDetails.viewProperties.height = $(ui.helper).height()/window.zoomRatio;
				
				var newStageItem = createStageItem(stageObjectDetails);
				$(this).append(newStageItem);	
				selectObject(newStageItem, true);
				newStageItem.find(".property input").trigger("input");
			}
		});
		$("#tooltip_menu_list").sortable( { 
			helper : 'clone',
			stop: function( event, ui ) {
				var uiObject = $(ui.item);
				var newIndex = uiObject.parent().children().index(ui.item);

				uiObject.parent().children()[0].childNodes[0].classList.remove("menu-bottom");
				uiObject.parent().children()[0].childNodes[0].classList.add("menu_top");

				if(newIndex  != 0) {
					// Middle
					ui.item[0].childNodes[0].classList.remove("menu_top");
					ui.item[0].childNodes[0].classList.remove("menu-bottom");
				}

				
				var pageID = uiObject.children().filter("a").attr("id").substr(("loadPage_").length);
				var page = getPageById(pageID);
				page.index = uiObject.index();
				setPageById(pageID,page);

				// Increment pageindex of all pages below
				for(var i=uiObject.index();i<uiObject.parent().children().size();i++) {
					pageID = $(uiObject.parent().children()[i]).children().filter("a").attr("id").substr(("loadPage_").length);
					var page = getPageById(pageID);
					page.index = i;
					setPageById(pageID,page);
				}

			}
		} );	
		$("#sidebar-left-wrapper").disableSelection();
		$("#toolbar-wrapper #box").disableSelection();
		$("#toolbar-wrapper .currentScreenText").disableSelection();

		// Set context menu for element
		setElementContextMenu();		

		// Set context menu for stage
		setStageContextMenu();
		
		// Make central stage scrollable
		//$('#center-wrapper').dragscrollable({dragSelector: '.dragger:first', acceptPropagatedEvent: false});

		// Make central stage selectable
		$('#center-selectable').selectable({
			filter: ".draggable #selectionWrapper",
			cancel: ".draggable",
			start: function(event, ui) {
				if($(event.toElement).parents(".draggable").length ==0 ) {
					// If the user clicks somewhere on the screen, show selection
					if(isSelected($(event.currentTarget)) || event.ctrlKey) {
						selectObject($(event.currentTarget), false);
					}else {
						selectObject($(event.currentTarget), true);
					}
				}
			},
			stop : function(event, ui) {
				$( ".ui-selected", this ).each(function() {
					selectObject(this,false);
				}); 
			}
		});
		
		//$('#center-wrapper').focus(function(event){ $(document).keydown(function(e) { return false; }); }); 
		//$('#center-wrappe').blur(function(event){ $(document).unbind('keydown'); });
		
		$(document).keydown(function(e){
			var STEP = 1;
			if(e.target.tagName != "INPUT") {
				switch(e.keyCode) {
					case(37):	// Left
						$(".stageItem").each(function() {
							var stageItem = $(this).parent().parent();
							if(isSelected(stageItem)) {
								var stageItemLeft = parseFloat(stageItem.css("left").substr(0,stageItem.css("left").length-2));
								stageItem.css("left",stageItemLeft-( STEP * window.zoomRatio )+"px");
								e.preventDefault();
							}
						});
					break;
					case(38):	// Up
						$(".stageItem").each(function() {
							var stageItem = $(this).parent().parent();
							if(isSelected(stageItem)) {
								var stageItemLeft = parseFloat(stageItem.css("top").substr(0,stageItem.css("top").length-2));
								stageItem.css("top",stageItemLeft-( STEP * window.zoomRatio )+"px");
								e.preventDefault();
							}
						});
					break;
					case(39):	// Right
						$(".stageItem").each(function() {
							var stageItem = $(this).parent().parent();
							if(isSelected(stageItem)) {
								var stageItemLeft = parseFloat(stageItem.css("left").substr(0,stageItem.css("left").length-2));
								stageItem.css("left",stageItemLeft+( STEP * window.zoomRatio )+"px");
								e.preventDefault();
							}
						});
					break;
					case(40):	// Down
						$(".stageItem").each(function() {
							var stageItem = $(this).parent().parent();
							if(isSelected(stageItem)) {
								var stageItemLeft = parseFloat(stageItem.css("top").substr(0,stageItem.css("top").length-2));
								stageItem.css("top",stageItemLeft+( STEP * window.zoomRatio )+"px");
								e.preventDefault();
							}
						});
					break;
					case(46): // Delete
						if($(e.target).parents(".popoverGroup").length == 0) {
							$(".stageItem").each(function() {
								$(".ui-resizable-handle:visible").each( function() {
									$(this).parent().parent().remove();
									e.preventDefault();
								});
							});
						}
					break;
				}
		    }
		});
		initialize();
		
		$("#stage-item-resize-dialog").dialog({
			autoOpen: false,
			height: 'auto',
			width: '500',
			modal: true,
			open: function() {
				var stageItem = $(this).data("stageItem");
				$(this).find("#stage-item-resize-width").val(parseFloat(stageItem[0].getBoundingClientRect().width/window.zoomRatio).toFixed(2));
				$(this).find("#stage-item-resize-height").val(parseFloat(stageItem[0].getBoundingClientRect().height/window.zoomRatio).toFixed(2));
			},
			buttons: {
				"Cancel" : function() {
					$( this ).dialog( "close" );
				},
				"Save": function() {
					var stageItem = $(this).data("stageItem");
					stageItem.width($(this).find("#stage-item-resize-width").val()*window.zoomRatio);
					stageItem.height($(this).find("#stage-item-resize-height").val()*window.zoomRatio);
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$( this ).dialog( "close" );
			},
			close: function() {
			}
		});
	});
	
	/*
	* This function will save the current project in JSON format in the window.currentProject global variable.
	*/

	function saveProject(hideMenus) {
	
		var currentStageInJson = convertStageToJson();
		window.currentProject.stageItemCounter = stageItemCounter;
		if(window.currentProject.pages == undefined) {
			// Project was not saved: create a new page array and add the current page to it
			window.currentProject.pages = new Array();
			window.currentProject.pages.push(currentStageInJson);
		}else {
			// update current stage page
			setPageById(window.currentPageId, currentStageInJson);
		}
		
		//console.log(window.currentProject);
		if(hideMenus) {
			hideSubMenus();
		}
		
	}	
	function importProject(projectName, hideMenu) {

	  $("#project-load-projects").hide();
	  $("#project-load-loader").show();
	  
		$.ajax({
			  url: '/YourHomeDesigner/projects/'+projectName,
			  type: 'GET',
			  success: function(data) {
				//loadProject(jQuery.parseJSON(data));
				loadProject(data, projectName);
				if(hideMenu) {
					hideSubMenus();
				}
				$("#project-load-dialog" ).dialog( "close" );
			  },
			  error: function(error) {
				showMessageDialog("An error has occured. Your project could not be imported.","Error in import");
				if(hideMenu) {
					hideSubMenus();
				}
				$("#project-load-dialog" ).dialog( "close" );
			  }
		});

	}
	function exportProject(hideMenu, clearFilename, successFunction) {
		saveProject(false);
		console.log(window.currentProject);
		$("#project-save-as-dialog").data("clearFilename",clearFilename);
		$("#project-save-as-dialog").data("successFunction",successFunction);
		$("#project-save-as-dialog").dialog("open");
	}
	function publishProject(hideMenu) {
		exportProject(true, false, function() {
			$("#publish-project-dialog" ).dialog( "open" );
		});
		//
	}
	function convertStageToJson() {
		var stageObjects = new Array();
		var page = new Object();
		
		var stageElements = $("#htmlStage .draggable");
		stageElements.sort(function(a, b) {
			var floatA = parseFloat(a.childNodes[0].style.zIndex);
			var floatB = parseFloat(b.childNodes[0].style.zIndex);
			//console.log("A"+$(a).attr("id")+" > B"+$(b).attr("id")+" : " + floatA +">"+floatB);
			return (floatA < floatB) ? -1 : (floatA > floatB) ? 1 : 0;
		   //return floatA > floatB;
		});
		stageElements.each(function(v,wrapper) {
			
			var wrapperObj = $(wrapper);
			//console.log("wrapper: "+wrapperObj.attr("id")+", Z"+wrapper.childNodes[0].style.zIndex);
			var stageObject = new Object();

			var imageSrc = wrapperObj.find("#selectionWrapper img");
			if(imageSrc.length > 0) {
				stageObject.imageSrc = abs2rel(imageSrc.attr("src"));
			}
			stageObject.id = wrapperObj.attr("id");

			// Parse binding from popover details
			var popoverObject = wrapperObj.find("#popoverContent");
			//stageObject.type = child.tagName;
			if(popoverObject.length > 0) {

				/* Parse view properties */
				// Parse all view properties (see getViewPropertiesDiv)
				var viewDetailsDiv = popoverObject.find("#viewDetails");
				var propertiesTable = viewDetailsDiv.find("table");
				stageObject.viewProperties = getViewDetailsById(viewDetailsDiv.attr("rel"));
				if(stageObject.viewProperties != null) {
					stageObject.viewProperties.width = wrapperObj[0].getBoundingClientRect().width / window.zoomRatio; //parseFloat(wrapper.style.width.substr(0,wrapper.style.width.length-2))		/ window.zoomRatio; 
					stageObject.viewProperties.height = wrapperObj[0].getBoundingClientRect().height / window.zoomRatio; //parseFloat(wrapper.style.height.substr(0,wrapper.style.height.length-2))  	/ window.zoomRatio; 
					stageObject.viewProperties.left = parseFloat(wrapperObj.css("left").substr(0,wrapper.style.left.length-2)) 			/ window.zoomRatio; // -PX
					stageObject.viewProperties.top = parseFloat(wrapperObj.css("top").substr(0,wrapper.style.top.length-2)) 			/ window.zoomRatio; // -PX

					//stageObject.viewProperties.rel = $(child).attr("rel");
					//stageObject.viewProperties.zIndex = parseFloat(child.parentNode.style.zIndex);
					stageObject.viewProperties.rotation = getRotationDegrees(wrapperObj.find("#selectionWrapper"));

					propertiesTable.find("tr").each(function() {
						var property = $(this);
						var propertyKey = property.attr("id");
						var propertyType = property.attr("rel");
						var propertyObject = findByAttribute(stageObject.viewProperties.properties, "key", propertyKey);
						if(propertyObject != null) {
							//var propertyObject = new Object();
							var tds = property.find("td");
							switch(propertyType) {
								case "image":
									if(tds.length == 2) {
										propertyObject.description = $(tds[0]).find("label span").text();
										propertyObject.value = abs2rel($(tds[1]).find("img").attr("src"));
									}
								break;
								case "image_state":
									if(tds.length == 3) {
										propertyObject.description = $(tds[1]).find("input").val();
										propertyObject.value = abs2rel($(tds[2]).find("img").attr("src"));
									}
								break;
								case "string":				
								case "double":
									if(tds.length == 2) {
										propertyObject.description = $(tds[0]).find("label span").text();
										propertyObject.value = $(tds[1]).find("input").val();
									}
								break;
								case "color":
									if(tds.length == 2) {
										propertyObject.description = $(tds[0]).find("label span").text();
										propertyObject.value = $(tds[1]).find("input").val();
									}
								break;
								case "boolean":
									if(tds.length == 2) {
										propertyObject.description = $(tds[0]).find("label span").text();
										propertyObject.value = $(tds[1]).find("input:checkbox").is(":checked");
									}
								break;
								case "alignment":
									if(tds.length == 2) {
										propertyObject.description = $(tds[0]).find("label span").text();
										propertyObject.value = $(tds[1]).find("input:radio:checked").val();
									}
								break;
							}
						}
					});
				}

				/* Parse binding properties */
				var bindingPropertiesDiv = $(popoverObject.find("#controlDetails"));
				if(bindingPropertiesDiv.length == 0) {
					stageObject.bindingProperties = null;
				}else {
					stageObject.bindingProperties = new Object();
					var controlDetails = getControlDetailsByIdentifiers(bindingPropertiesDiv.find("#controllerIdentifier").val(), bindingPropertiesDiv.find("#nodeIdentifier").val(), bindingPropertiesDiv.find("#valueIdentifier").val());
					stageObject.bindingProperties = controlDetails;
				}
			}
			stageObjects.push(stageObject);

		});
		
		page.width = window.currentProject.width;
		page.height = window.currentProject.height;		// To be removed?
		page.title = $("#currentScreen").text();
		//stageObjects.sort(byProperty("viewProperties.zIndex","ASC"));
		page.objects = stageObjects;
		page.zoomRatio = window.zoomRatio;
		page.backgroundColor = $("#stage-color-picker").val();		
		//var backgroundCSS = $("#svgObject")[0].style.backgroundImageSrc;
		//page.backgroundImageSrc = backgroundCSS.substr(backgroundCSS.indexOf('http'),backgroundCSS.length-7);
		//page.backgroundImageSrc = $("#svgObject").css('background-image').replace(/^url|[\(\)]/g, '');
		page.backgroundImageSrc = $("#svgObject")[0].style.backgroundImage.replace(/^url|[\(\)\"]/g, '')
		if(page.backgroundImageSrc == "none" || page.backgroundImageSrc == "initial") { 
			page.backgroundImageSrc = ""; 
		}else {		
			// If path is absolute, make it relative
			page.backgroundImageSrc = abs2rel(page.backgroundImageSrc);
		}
		page.pageId = window.currentPageId;
		page.index = $("#tooltip_menu_list li #loadPage_"+window.currentPageId).parent().index();
		//console.log(page);
		return page;
	}
	
	
	/*
	* This function will load a project
	*/ 
	function loadProject(project, projectFileName) {
		clearStage();
		$("#tooltip_menu_list").empty();
		
		project.fileName = projectFileName;
		window.currentProject = project;
		window.stageItemCounter = window.currentProject.stageItemCounter;
		$("#currentProject").text(window.currentProject.fileName.replace(".json",""));
		
		// Adjust screen size
		$("#htmlStage").width(window.currentProject.width * window.zoomRatio);
		$("#htmlStage").height(window.currentProject.height * window.zoomRatio);
					
		// Adjust menu of pages
		//project.pages.sort(byProperty("index","ASC")); They should be sorted already !
		for(var i=0;i<project.pages.length;i++) {
			if(project.pages[i] != null) {
				createPage( project.pages[i].title,	// Title
							false,					// Hide menu (no)
							true,					// Do not perform save before create
							project.pages[i].pageId // ID of page
							);
			}
		}
		
		// Load project in stage
		loadPageInStage(project.pages[0].pageId);
		
		selectObject(null,true);

	}
	function clearStage(){
		clearBackgroundImageSrc();
		var svgObject = $("#svgObject");
		$("#htmlStage").empty();
		$("#htmlStage").append(svgObject);
	}
	
	function saveAndLoadPageInStage(pageId,hideMenu) {
		saveProject();
		loadPageInStage(pageId,hideMenu);
	}
	function loadPageInStage(pageId,hideMenu) {
		// First save the project or the current stage
		clearStage();
		
		if(window.currentProject.pages != undefined ){
			var stageContent = getPageById(pageId);
			$("#currentScreen").text(stageContent.title);
			window.currentPageId = pageId;
			clearBackgroundImageSrc();
			// sort
			//stageContent.objects.sort(byProperty("viewProperties.zIndex","ASC")); They shouold be sorted already!
			if(stageContent.objects != undefined) {
				for(var i=0;i<stageContent.objects.length;i++) {
					
					
					var currentObject = stageContent.objects[i];
					/*
					
					var imgObject =  jQuery('<img/>',  {
															id: currentObject.id,
															src: currentObject.imageSrc
														}
											).css( {
											'position'	: 'absolute', 
											"width"		: currentObject.viewProperties.width	+ 'px',
											"height"	: currentObject.viewProperties.height	+ 'px',
											"left"		: currentObject.viewProperties.left	+ 'px',
											"top"		: currentObject.viewProperties.top   	+ 'px'
											});
					var wrappedElement = addSelectionHandlesTo(imgObject);
					wrappedElement.children().filter("#selectionWrapper").freetrans("rotate",currentObject.viewProperties.rotation);
					wrappedElement.children().filter("#selectionWrapper").css("z-index", window.currentProject.zIndexCounterUp++);
					// Add popover based on binding
					var popover = getPopover(wrappedElement,		// Wrapped stage object
							currentObject.viewProperties, 		// View details
							null,
							currentObject.bindingProperties		    	// optional binding (for defaulting values)
						);
					wrappedElement.append(popover);
					popover.hide();
					wrappedElement.find("#selectionWrapper").on("click", function (event) {
						if(event.ctrlKey){
							selectObject(event.target.parentNode, false);
						}else {
							selectObject(event.target.parentNode, true);
						}
					});*/
					var newStageItem = createStageItem(currentObject)
					$("#htmlStage").append(newStageItem);

				}
			}
			
			$("#stage-color-picker").minicolors('value',stageContent.backgroundColor);
			setBackgroundImageSrc(stageContent.backgroundImageSrc);
			
			
			// Update menu
			$(".currentlyLoaded").hide();
			$("#currentlyLoaded-"+pageId).show();
		}
		if(hideMenu) {
			hideSubMenus();
		}
		selectObject(null, true);
	}	
	function addSelectionHandlesTo(element, viewProperties) {
				
		//element.attr("class",element.attr("class") + " jstree-drop");
		element.addClass("jstree-drop");
		element.addClass("stageItem");
		element.removeClass("draggable");
		
		var newWidth = viewProperties.width	* window.zoomRatio;
		var newHeight = viewProperties.height	* window.zoomRatio;
		var newLeft = viewProperties.left	* window.zoomRatio;
		var newTop  = viewProperties.top	* window.zoomRatio;
		/*
		var newWidth = parseFloat(element.css("width").substr(0,element.css("width").length-2)) 	* window.zoomRatio;
		var newHeight = parseFloat(element.css("height").substr(0,element.css("height").length-2)) 	* window.zoomRatio;
		var newLeft = parseFloat(element.css("left").substr(0,element.css("left").length-2))		* window.zoomRatio;
		var newTop  = parseFloat(element.css("top").substr(0,element.css("top").length-2))			* window.zoomRatio;*/
		
		// Generate a unique ID for the element
		var id = element.attr("id");
		if(id == undefined) {
			id = "StageElement_"+window.stageItemCounter;
			window.stageItemCounter++;
		}
		
		// Create new element and add it to the html stage
			
		//element.css({"width":'100%',"height":'100%',"left":"auto","top":"auto","position":"relative"});
		element.css({"left":"auto","top":"auto","position":"relative"});
		//window.currentProject.zIndexCounterUp++;
		element.attr('id','img_'+id);
		
		var elementWithHandles = jQuery('<div/>', {
									id: "selectionWrapper"
								})
								  .append(element)
								  .append('<div class="ui-resizable-handle ui-resizable-nw" id="nwgrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-ne" id="negrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-sw" id="swgrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-se" id="segrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-w" id="wgrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-e" id="egrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-s" id="sgrip"></div>')
								  .append('<div class="ui-resizable-handle ui-resizable-n" id="ngrip"></div>');
								  //.css("overflow","hidden");

		var wrappedElement = jQuery('<div/>', {
									id: id,
									style: "position: relative;"
								}).append(elementWithHandles)
								.css({"position":"absolute","left":newLeft,"top":newTop,"width":newWidth+'px',"height":newHeight+'px'});
								  
			wrappedElement = activateInteractions(wrappedElement, viewProperties);
								  
		return wrappedElement;
	}
	function activateInteractions(element, viewProperties) {
		var dragLastPosition = null;	
		element.draggable({
				/*containment: [	0,			//x1
								0,			//y1
								$("#htmlStage").offset().left+$("#htmlStage").width()-element.width(), //x2
								$("#sidebar-left-wrapper").height()],				   //y2*/
			    handle: "#selectionWrapper",
		 		containment: 'center-wrapper',
				helper: "original",
				cursor: "move",
				appendTo: "htmlStage",
				snap: true,
				start: function(event, ui){
					// Check if source element is part of selectionWrapper (= draggable)
					if($(event.toElement).parents("div#selectionWrapper").length > 0) {
						dragLastPosition = {"left":ui.position.left,"top":ui.position.top};
						selectObject(ui.helper,false);
						ui.helper.children().filter(".popoverGroup").hide();
						if(event.shiftKey) {
							$(this).draggable( "option", "snap", true );
						}else {
							$(this).draggable( "option", "snap", false );
						}		
				 	}			
				},
				drag: function(event, ui){
					// Check if source element is part of selectionWrapper (= draggable)
					if($(event.toElement).parents("div#selectionWrapper").length > 0) {
						if(event.shiftKey) {
							$(this).draggable( "option", "snap", true );
						}else {
							$(this).draggable( "option", "snap", false );
						}

						var relativeLeft = ui.position.left - dragLastPosition.left;
						var relativeTop = ui.position.top - dragLastPosition.top;
						dragLastPosition.left = ui.position.left;
						dragLastPosition.top = ui.position.top;

						// Move all selected items
						$(".stageItem").each(function() {
							var currentStageItem = $(this).parent().parent();
							if(ui.helper != currentStageItem && isSelected(currentStageItem)) {
								var thisOffset = currentStageItem.offset();
								thisOffset.left = thisOffset.left + relativeLeft;
								thisOffset.top = thisOffset.top + relativeTop;
								currentStageItem.offset(thisOffset);
							}
						});
					}
				},
				stop: function(event, ui){
					dragStartPosition = null;
				}
		  }	)
			.addClass("draggable");
			
			
			//element.children().filter("#selectionWrapper")
			element
			.resizable({
			//alsoResize: "#selectionWrapper",
			handles: {
				'ne': '#negrip',
				'se': '#segrip',
				'sw': '#swgrip',
				'nw': '#nwgrip',
				'n': '#ngrip',
				's': '#sgrip',
				'w': '#wgrip',
				'e': '#egrip'
			},
			resize: function(event, ui) { 
			
				//var popover = ui.originalElement.parent().children().filter("#popover_"+ui.originalElement.parent().attr("id"));
				
				// Transfer resize to parent element
				/*
				ui.originalElement.parent().css("width",ui.originalElement.css("width"));
				ui.originalElement.parent().css("height",ui.originalElement.css("height"));
					ui.originalElement.css("width","100%");
				ui.originalElement.css("height","100%");*/
			},
			stop: function(event, ui) { 
/*
				var leftParent = parseFloat(ui.originalElement.parent().css("left").substr(0,ui.originalElement.parent().css("left").length-2));
				var leftChild =  parseFloat(ui.originalElement.css("left").substr(0,ui.originalElement.css("left").length-2));
				ui.originalElement.parent().css("left",(leftParent + leftChild)+"px");
				
				var topParent = parseFloat(ui.originalElement.parent().css("top").substr(0,ui.originalElement.parent().css("top").length-2));
				var topChild =  parseFloat(ui.originalElement.css("top").substr(0,ui.originalElement.css("top").length-2));
				ui.originalElement.parent().css("top",(topParent + topChild)+"px");
				
				ui.originalElement.css("top","0px");
				ui.originalElement.css("left","0px");	*/
			
			}});


			element.find("#selectionWrapper").freetrans();
			if(viewProperties!=null && viewProperties.rotation != null) {
				element.find("#selectionWrapper").freetrans("rotate",viewProperties.rotation);
			}
			
			
		return element;
	}
	function zoomFit() {
		var stageWidth = $("#htmlStage")[0].getBoundingClientRect().width / window.zoomRatio;
		var centerWidth = $("#center-wrapper")[0].getBoundingClientRect().width;
		var zoomLevel = centerWidth==0?100:centerWidth / stageWidth * 100;
		zoomStage(zoomLevel * 0.98);
	}
	/**
	* Zoom the stage: zooming is done in comparison to the global variable window.zoomRatio.
	*/
	function zoomStage(zoom) {
		zoom /= 100;
		if(zoom != zoomRatio) {
			$.each($("#htmlStage .draggable"), function(key,value) {	
				var oldWidth = parseFloat(value.style.width.substr(0,value.style.width.length-2)); // -PX
				var oldHeight = parseFloat(value.style.height.substr(0,value.style.height.length-2)); // -PX
				var oldLeft = parseFloat(value.style.left.substr(0,value.style.left.length-2)); // -PX
				var oldTop = parseFloat(value.style.top.substr(0,value.style.top.length-2)); // -PX

				
				var newWidth = oldWidth * zoom / window.zoomRatio;
				var newHeight = oldHeight * zoom / window.zoomRatio;
				var newTop = oldTop * zoom / window.zoomRatio;
				var newLeft = oldLeft * zoom / window.zoomRatio;
				
				value.style.top 	= newTop +"px";
				value.style.left 	= newLeft+"px";
				value.style.height 	= newHeight+"px";
				value.style.width 	= newWidth+"px";
			});

			// Update the properties that need zooming
			$.each($(".textProperty"), function(key,value) {	
				var valueObject = $(value);
				var newFontSize = parseFloat(valueObject.css("font-size"))* zoom / window.zoomRatio;
				$(value).css("font-size",newFontSize+"px");
			});

			// zoom in htmlstage			
			var oldWidth = $("#htmlStage")[0].getBoundingClientRect().width;
			var oldHeight = $("#htmlStage")[0].getBoundingClientRect().height;
		    var newWidth = $("#htmlStage")[0].getBoundingClientRect().width*zoom / window.zoomRatio;
			var newHeight = $("#htmlStage")[0].getBoundingClientRect().height*zoom / window.zoomRatio;


			$("#htmlStage").width(newWidth);
			$("#htmlStage").height(newHeight);

			$("#htmlStage-background").width(newWidth);
			$("#htmlStage-background").height(newHeight);

			// center htmlstage
			var newWidth = $("#htmlStage")[0].getBoundingClientRect().width;
			var newHeight = $("#htmlStage")[0].getBoundingClientRect().height;
			var newLeft = $("#htmlStage").offset().left + oldWidth/2-newWidth/2 - $("#center-wrapper").offset().left;
			var newTop = $("#htmlStage").offset().top + oldHeight/2-newHeight/2 + $("#center-wrapper").offset().height;
			//$("#htmlStage").css({"left":newLeft,"top":newTop});
			$("#htmlStage").css({"left":0,"top":newTop});

			// Set dimensions of background
			$("#svgObject").css("background-size", $("#htmlStage")[0].getBoundingClientRect().width+"px " + $("#htmlStage")[0].getBoundingClientRect().height+"px");

			// Set global zoom variable
			window.zoomRatio = zoom;
		}
		hideSubMenus();
	}
	function createPageInline() {
	
		var newPageID = createPage("New screen",false,false,undefined);
		renameScreen(newPageID);
		
	}
	function createPage(title,
						hideMenu,
						projectLoad, // Do not perform save before create
						pageID		// Optional: if not provided, a new ID will be generated
						) {


		var newPage = new Object();
		
		// First save the project or the current stage
		if(!projectLoad) {
			if(window.currentPageId >= 0) {
				saveProject();
				// Then empty the stage
				clearStage();
			}else {
				window.currentProject.pages = new Array();
			}
		}
		// Then create a new entry in the pages object
		if(pageID == undefined) {
			// get max page id
			var highest = 0;
			for(var i=0;i<window.currentProject.pages.length;i++) {
				if(window.currentProject.pages[i].pageId > highest) { 
					highest = window.currentProject.pages[i].pageId;
				}
			}
			newPage.pageId = highest+1;
			window.currentProject.pages.push(newPage);
		}else {
			newPage.pageId = pageID;
		}
		window.currentPageId = newPage.pageId;
		// Last but not least - add a reference to the menu and to the top screen
		
		newPage.title = title;
		if(title == undefined || title == "New screen") {
			newPage.title = "New screen ";
			if(newPage.pageId > 0) {
				newPage.title = newPage.title + " ("+newPage.pageId+")";
			}else {
				newPage.title = newPage.title;
			}
		}
//		$("#menu_pages #tooltip_menu .menu_top").removeClass("menu_top");
		var newPageMenuItemA = jQuery('<a/>', {
									id: "loadPage_"+newPage.pageId ,
//									"class":"menu_top",
									"onclick":"saveAndLoadPageInStage("+newPage.pageId+",1)",
									"href":"#",
									"class" : "menu_row"
									}
								).prepend('<i class="ic icon-mobile2 ic-color ic-in-button"></i>')
								.append("<span id='currentlyLoaded-txt-"+newPage.pageId+"'>"+newPage.title+"</span>")
								.append("<span id='currentlyLoaded-"+newPage.pageId+"' class='currentlyLoaded'> (Currently loaded)</span>");

		if(newPage.pageId == 0) {
			newPageMenuItemA.addClass("menu_top");
		}
	
		var newPageMenuEditIcon = jQuery(/*'<img/>', { 
														id 		: "editScreen-img-" + newPage.pageId,
														style	: "float:right;padding-top: 3px;padding-right:4px;",
														src 	: "css/menu/edit.png"
													}*/
										'<i class="ic icon-pencil2 ic-color ic-in-button menu_screens_screen ic-fw"></i>'
										)
										.attr("id","editScreen-img-" + newPage.pageId)
										.click(function(event) {
													renameScreen(newPage.pageId);
													event.stopImmediatePropagation();
												});
		var newPageMenuSaveIcon = jQuery(/*'<img/>', { 
														id 		: "saveScreen-img-" + newPage.pageId,
														style	: "float:right;padding-top: 3px;padding-right:4px;",
														src 	: "css/menu/ok.png"
													}*/
													
										'<i class="ic icon-check ic-color ic-in-button menu_screens_screen ic-fw"></i>'
										)
										.attr("id","saveScreen-img-" + newPage.pageId)
										.click(function(event) {
													var newText = $("#currentlyLoaded-input-"+newPage.pageId).val();
													if(window.currentPageId == newPage.pageId) {
														$("#currentScreen").text(newText);
													}

													$("#currentlyLoaded-txt-"+newPage.pageId).text(newText);
													$("#currentlyLoaded-input-"+newPage.pageId).remove();
													$("#currentlyLoaded-txt-"+newPage.pageId).show();
													$("#saveScreen-img-"+newPage.pageId).hide();
													$("#deleteScreen-img-"+newPage.pageId).show();
													$("#editScreen-img-"+newPage.pageId).show();
													$("#loadPage_"+newPage.pageId).attr("onclick","saveAndLoadPageInStage("+newPage.pageId+",1)");

													var projectPage = findByAttribute(window.currentProject.pages, "pageId", newPage.pageId);
													if(projectPage != null) {
														projectPage.title = newText;
													}
	
													alignNavigationNode();
													event.stopImmediatePropagation();
												})
										.hide();
		var newPageMenuDeleteIcon = jQuery(/*'<img/>', { 
														id 		: "deleteScreen-img-" + newPage.pageId,
														style	: "float:right;padding-top: 3px;padding-right:4px;",
														src 	: "css/menu/delete.png"
													}*/
													
										'<i class="ic icon-trash-o ic-color ic-in-button menu_screens_screen ic-fw"></i>'
										)
										.attr("id","deleteScreen-img-" + newPage.pageId)
										.click(function(event) {
													if(window.currentPageId >= 0) {
														showOkCancelDialog("Are you sure you want to delete this page and all of its content?", 
																	function() { 
																		/* OK  */

																		var amountOfPages = $("#loadPage_"+newPage.pageId).parent().parent().children().length;
																		if(amountOfPages == 1) {
																			// Show error that the last page cannot be deleted
																			showMessageDialog("Error: The last screen cannot be deleted.", "Error when deleting screen");
																			return;
																		}	
																		
																		if(window.currentPageId == newPage.pageId) {			// If the page is open, open the next one

																			// Retrieve next screen to load
																			var currentIndex = $("#loadPage_"+newPage.pageId).parent().index();
																			var nextIndex = currentIndex;
																			if(currentIndex == (amountOfPages-1)) {
																				nextIndex--;
																			}else {
																				nextIndex++;

																			}
																			var nextChild = $("#loadPage_"+newPage.pageId).parent().parent().children()[nextIndex].childNodes[0]; //a loadPage_X
																			var toOpenPage = nextChild.id.split('_')[1];

																			// Load it
																			window.currentPageId = toOpenPage;
																			loadPageInStage(window.currentPageId,false);
																		}

																		$("#loadPage_"+newPage.pageId).parent().remove();					// Remove from menu
																		//setPageById(newPage.pageId, undefined);	// Clear from memory
																		deletePage(newPage.pageId);
																	}	
																	, function() { } // Cancel, 
																	, "Confirm deletion"
																);
													}
													event.stopImmediatePropagation();
												});

			newPageMenuItemA.append(newPageMenuEditIcon)
							.append(newPageMenuSaveIcon)
							.append(newPageMenuDeleteIcon);

		var groupli = jQuery('<li/>');
			groupli.append(newPageMenuItemA);

		$("#menu_pages #tooltip_menu #tooltip_menu_list").append(groupli);
		$(".currentlyLoaded").hide();
		$("#currentlyLoaded-"+newPage.pageId).show();
		$("#currentScreen").text(newPage.title);

		// Set index reference in page
		var page = getPageById(newPage.pageId);
		page.index = groupli.index();
		setPageById(newPage.pageId, page);
		
		if(hideMenu) {
			hideSubMenus();
		}
		alignNavigationNode();
		return newPage.pageId;
	}
	function renameScreen(screenID) {
		var originalTextElement = $("#currentlyLoaded-txt-"+screenID);
		originalTextElement.parent().removeAttr("onclick");
		$("#currentlyLoaded-txt-"+screenID).after("<input type='text' id='currentlyLoaded-input-"+screenID+"' value='"+originalTextElement.text()+"'></input>");
		$("#saveScreen-img-"+screenID).show();
		$("#deleteScreen-img-"+screenID).hide();
		$("#editScreen-img-"+screenID).hide();
		originalTextElement.hide();
	}
	function sizeWrapperWindow() {
		var sidebarLeft = $("#sidebar-left-wrapper").width()>500?250:$("#sidebar-left-wrapper").width();
		var sidebarRight = $("#sidebar-right-wrapper").width()>500?310:$("#sidebar-right-wrapper").width();
		document.body.style.overflow = "hidden";
		var newWidth = $(window).width() - sidebarLeft- sidebarRight;
		document.body.style.overflow = "";		
		$("#center-wrapper").css("width",newWidth+"px");	// Scrollbar
		$("#center-wrapper").css("left",sidebarLeft);	// Scrollbar
		var newHeight = $(window).height() - $("#toolbar-wrapper").height()-3;
		$("#center-wrapper").height(newHeight);
	}
	function clearBackgroundImageSrc() {
		$("#svgObject").css("background",$("#stage-color-picker").val());
		$("#grid").show();
	}
	function setBackgroundImageSrc(imageUrl) {
		if(imageUrl == undefined) {
			imageUrl = $("#upload_background").val();
		}

		if(imageUrl != "") {
			abs2rel(imageUrl);
			$("#svgObject").css("background-image","url(''");
			$("#svgObject").css("background-image","url('"+imageUrl+"')");
			$("#svgObject").css("background-size", $("#htmlStage")[0].getBoundingClientRect().width+"px " + $("#htmlStage")[0].getBoundingClientRect().height+"px");
			$("#grid").hide();
		}
	}
	function sizeHtmlStage() {
		// Set standard values
		//window.currentProject.width = 1280;
		//window.currentProject.height = 720;

		$( "#screen-size-width" ).val(window.currentProject.width);
		$( "#screen-size-height" ).val(window.currentProject.height);
		
		$("#htmlStage").width(window.currentProject.width * window.zoomRatio);
		$("#htmlStage").height(window.currentProject.height * window.zoomRatio);
		//$("#htmlStage").width(window.currentProject.width+"px");
		//$("#htmlStage").height(window.currentProject.height+"px");

		//$("#svgObject").css("background-size", $("#htmlStage").width()+"px " + $("#htmlStage").height()+"px");
		//$("#svgObject").css("background-size", window.currentProject.width+"px " + window.currentProject.height+"px");
		$("#svgObject").css("background-size", window.currentProject.width * window.zoomRatio+"px " + window.currentProject.height * window.zoomRatio+"px");
		//console.log("size: "+$("#svgObject").css("background-size"));
	}
	function selectObject(selectedObject, hideOthers) {
		var stageObject = getStageObject(selectedObject);
		
		if(stageObject != undefined) {
			if(hideOthers) {
				// Hide all other stageObjects
				$(".ui-resizable-handle:not(#"+stageObject.attr("id")+" #selectionWrapper .ui-resizable-handle)").hide();
			}	
				// Hide all open popovers
				$(".popoverGroup:not(#"+stageObject.attr("id")+" .popoverGroup)").hide();

			// Select current stageObject
			var currentStageObjectHandles = stageObject.children().filter("#selectionWrapper").children().filter(".ui-resizable-handle");
			
			if(!currentStageObjectHandles.is(":visible")) {
				currentStageObjectHandles.fadeIn("fast");
			}			

			var currentStageObjectPopover = stageObject.children().filter(".popoverGroup");
			if(( currentStageObjectPopover.find("#viewDetails").children().length > 0
					|| currentStageObjectPopover.find("#controlDetails").children().length > 0)) {
				currentStageObjectPopover.fadeIn("fast");
			}else {
				currentStageObjectPopover.hide();
			}
		}else {
			// Clicked on other object: hide all selections
			$(".ui-resizable-handle").hide();
			$(".popoverGroup").hide();
			$(".ui-resizable-handle").hide();
			$(".popoverGroup").hide();
		}
	}	
	function getStageObject(stageObjectElement) {
		if(stageObjectElement == null) { return undefined; }

		var parentNode = $(stageObjectElement);
		while(parentNode != document
				&& parentNode.attr("id") != "htmlStage" 
				&& parentNode.attr("id") != "center-wrapper" ) {
			if(parentNode.attr("id") != undefined && 
					parentNode.attr("id").substr(0,("StageElement").length) == "StageElement") {
				return $(parentNode);
			}
			parentNode = parentNode.parent();
		}
		return undefined;
	}
	function openResizePopup(stageItem) {
		$("#stage-item-resize-dialog").data("stageItem",stageItem);
		$("#stage-item-resize-dialog").dialog("open");
	}
	function setStageContextMenu() {
		$.contextMenu({
			selector: '#htmlStage', 
			callback: function(key, options) {
 
				switch (key) {
					case 'paste':
						if( window.copyPasteMemory != null ) {
							var pastedObject = window.copyPasteMemory;
							var popoverContent = pastedObject.children().filter("#popover_"+pastedObject.attr("id")).children().filter("#popoverContent");
							popoverContent.children().filter("#popoverType_"+pastedObject.attr("id")).attr('id', "popoverType_"+pastedObject.attr("id")+"_copy");
							popoverContent.children().filter("#originalControl_"+pastedObject.attr("id")).attr('id', "originalControl_"+pastedObject.attr("id")+"_copy");
							popoverContent.children().filter("#ZWaveInfo_"+pastedObject.attr("id")).attr('id', "ZWaveInfo_"+pastedObject.attr("id")+"_copy");
							pastedObject.children().filter("#popover_"+pastedObject.attr("id")).attr('id', "popover_"+pastedObject.attr("id")+"_copy");
							pastedObject.children().each(function (a, child) {
								if (child.tagName == "IMG") {
									child.id = child.id + "_copy";
								}
							});	
						
							pastedObject = activateInteractions(pastedObject);
							pastedObject.attr("id", window.copyPasteMemory.attr("id")+"_copy");
							$("#htmlStage").append(pastedObject);
							window.copyPasteMemory = window.copyPasteMemory.clone();
						}

					break;
					case 'size':
						openScreenSizePopup();
					break;
				}


			},
			items: {
				"paste": {name: "Paste", icon: "paste"},
				"sep1": "---------",
				"size": {name: "Set canvas size",icon: "edit"}
			}
		});
	}
	function setElementContextMenu() {
		$.contextMenu({
			zIndex: '999999999',
			selector: '.stageItem', 
			 events: {
			   show : function(options){
					selectObject($(this), true);
					return true;
			   }
			},
			callback: function(key, options) {
				switch (key) {
					/*case 'cut':
						options.$trigger.removeClass("context-menu-active");
						window.copyPasteMemory = options.$trigger.parent().parent().clone();
						options.$trigger.parent().parent().remove();
					break;
					case 'copy':
						options.$trigger.removeClass("context-menu-active");
						window.copyPasteMemory = options.$trigger.parent().parent().clone();
					break;*/
					case 'resize':
						openResizePopup($(this).parent().parent());
					break;
					case 'delete':
						$(".ui-resizable-handle:visible").each( function() {
							$(this).parent().parent().remove();
						});
					break;
					case 'movef':
						options.$trigger.parents("#selectionWrapper").css("z-index",++window.currentProject.zIndexCounterUp)
																	 .find(".stageItem").css("z-index",window.currentProject.zIndexCounterUp);

					break;
					case 'moveb':
						options.$trigger.parents("#selectionWrapper").css("z-index",--window.currentProject.zIndexCounterDown)
																	 .find(".stageItem").css("z-index",window.currentProject.zIndexCounterDown);
					break;

				}


			},
			items: {
/*				"cut": {name: "Cut", icon: "cut"},
				"copy": {name: "Copy", icon: "copy"},*/
				"delete": {name: "Delete", icon: "delete"},
				"resize": {name: "Set size", icon: "edit"},
				"sep1": "---------",
				"movef": {name: "Move to front", icon: ""},
				"moveb": {name: "Move to back", icon: ""},
/*				"sep2": "---------",
				"config": {name: "Configuration", icon: "edit"}*/
			}
		});
	}
	/*function addPopover(stageObject, stageObjectChild, selectedControl) {

		var relID = stageObject.find("img").attr("rel");
		var imageDetails = getViewDetailsById(relID)
							
		var existingPopover = stageObject.children().filter("#popover_"+stageObject.attr("id"));

		if (existingPopover.length == 0 
			// Check for interactive elements that create the popover on creation (e.g. colorpicker)
			|| (imageDetails != undefined && imageDetails.objects[0].type == "interactive" && imageDetails.objects[0].details.addOptionsOnCreate == true )) {
			
			// Select correct content based on the type of control that was chosen.
			var controlDetails = getControlDetails(selectedControl);		
			var popover = getBindingPropertiesDiv(stageObject,
											stageObject.attr("id"),
											controlDetails.controllerIdentifier,
											selectedControl,
											undefined,
											stageObjectChild );
			if(popover != null) {
				stageObject.append(popover);
				centerPopover(stageObject,popover);
				popover.fadeIn("fast");
			}
				selectObject(stageObject,true);
			
		} else {
			existingPopover.fadeIn("fast");
		}
	}*/
	/*function centerPopover(stageObject, popover) {
		
		var popoverWidth = popover.width();
		var objectWidth = stageObject.width();
		
		var popoverLeft = ( objectWidth - popoverWidth ) / 2;// * window.zoomRatio;;
		popover.css("left",popoverLeft+"px");
		popover.children().filter("#popoverTriangle").css("margin-left",popoverWidth/2+"px");

		
	}*/
	
	function getPopover(stageObject,		// Wrapped stage object
						viewDetails, 		// View details
						controlDetails,		// Control details
					    bindingObject    	// optional binding (for defaulting values)
					    ) {
		/* Get view properties div */
		var viewPropertiesDiv = getViewPropertiesDiv(viewDetails, stageObject);

		/* get control details div */
		if(controlDetails != null || bindingObject != null) {
			var controlDetailsDiv = getBindingPropertiesDiv(controlDetails, bindingObject);
		}
		/* Build popover and add to stageObject */
		var popover = jQuery('<div/>', {
			"id"	:"popover_"+stageObject.attr("id"),
			"class" :"popoverGroup"
		})
		//.css({'display': 'block','position':'absolute'})
		.css({'display': 'block'})
		.append('<div id="popoverTriangle" class="arrow_box"></div>');
		var popoverContent = jQuery("<div />", { "id" : "popoverContent", "class" : "popover"});
		popoverContent.append(controlDetailsDiv);
		popoverContent.append(viewPropertiesDiv);
		popover.append(popoverContent);
		
		return popover;
	}
	function handleViewChange(viewDetails,propertyThatChanged,stageObject,element) {

		switch(viewDetails.type) {
			case "web_static_html":
				if(propertyThatChanged.key == "content"){
					var newValue = element.target.value;
					var div = stageObject.find("#selectionWrapper div").first();
					div.html(newValue);
				}
			break;
			case "web_link":

				if(propertyThatChanged.key == "url"){
					var newValue = element.target.value;
					var div = stageObject.find("#selectionWrapper iframe").first();
					div.attr("src",newValue);
				}
			break;
			case "web_rss":
				var currentSrc = stageObject.find("#selectionWrapper iframe").attr("src");
				var div = stageObject.find("#selectionWrapper iframe").first();
				if(propertyThatChanged.key == "url"){
					var newValue = element.target.value;
					var newUrl = $.query.load(currentSrc).set("feedUrl",newValue).toString();
					div.attr("src","/rss/rss.html"+newUrl);
				}else if(propertyThatChanged.key == "color"){
					var newUrl = $.query.load(currentSrc).set("textColor",element.substr(1)).toString();
					div.attr("src","/rss/rss.html"+newUrl);
				}
			break;
			case "text":			
				if(propertyThatChanged.key == "alignment"){
					var textSpan = stageObject.find("#selectionWrapper span").first();
					//textSpan.css("text-align",element.target.value);
					var float = element.target.value;
					if(float == "center") {
						float = "initial";
					}
					textSpan.css("float",float);
				}else if(propertyThatChanged.key == "content"){
					// Update the text in the view
					var newValue = element.target.value;
					var textSpan = stageObject.find("#selectionWrapper span").first();
					textSpan.text(newValue);
					stageObject.css("height",textSpan.outerHeight()+"px")
					stageObject.css("width",textSpan.outerWidth()+1+"px")
				}	
				else if(propertyThatChanged.key == "size"){
					// Update the text in the view
					var newValue = element.target.value;
					var textSpan = stageObject.find("#selectionWrapper span").first();
					var newSize = parseFloat(newValue)*window.zoomRatio;
					textSpan.css("font-size",newSize+"px");
					stageObject.css("height",textSpan.outerHeight()+"px")
					stageObject.css("width",textSpan.outerWidth()+1+"px")
				}	
				else if(propertyThatChanged.key == "color"){
					// Update the text in the view
					var textSpan = stageObject.find("#selectionWrapper span").first();
					textSpan.css("color",element);
				}	
			break;
			case "shape":
				if(propertyThatChanged.key == "color"){
					stageObject.find(".shape").css("background-color",element);
				}else if(propertyThatChanged.key == "corner_radius") {
					var newValue = element.target.value;
					stageObject.find(".shape").css("border-radius",newValue+"px");
				}	
			break;
			case "clock_digital":			
				if(propertyThatChanged.key == "AMPM"){
					var textSpan = stageObject.find("#selectionWrapper span").first();
					var currentDate = new Date(); var currentDateString = "";
					if(element.target.checked) {
							currentDateString = formatTime(currentDate, true);
						}else {
							currentDateString = formatTime(currentDate, false);
						}
					textSpan.text(currentDateString);

					stageObject.css("height",textSpan.outerHeight()+"px")
					stageObject.css("width",textSpan.outerWidth()+1+"px")
				}	
				else if(propertyThatChanged.key == "size"){
					// Update the text in the view
					var newValue = element.target.value;
					var textSpan = stageObject.find("#selectionWrapper span").first();
					var newSize = parseFloat(newValue)*window.zoomRatio;
					textSpan.css("font-size",newSize+"px");
					stageObject.css("height",textSpan.outerHeight()+"px")
					stageObject.css("width",textSpan.outerWidth()+1+"px")
				}	
				else if(propertyThatChanged.key == "color"){
					// Update the text in the view
					var textSpan = stageObject.find("#selectionWrapper span").first();
					textSpan.css("color",element);
				}	
			break;
			default:
			break;
		}
	}
	function getViewPropertiesDiv(  viewDetails, 		// Control element details
									stageObject
								   ) {

		var viewPropertiesDiv = jQuery("<div />", { "id":"viewDetails", "class" : "popover-properties", "rel":viewDetails.id })
		
		viewPropertiesDiv.append("<h2>View Details</h2>");
		if(viewDetails.title != null && viewDetails.title != "") {
			viewPropertiesDiv.append("<span>View Type: "+viewDetails.title+"</span>");
		}
		var allowsList = getAllowsImageList(viewDetails);
		viewPropertiesDiv.append(allowsList);
		
		/* Parse properties of view details */
		var viewPropertiesTable = $("<table/>", { "cellspacing" : "0", "cellpadding" : "0" });
		var propertiesSet = false;
		$.each(viewDetails.properties, function( propertyName, propertyDetails ) {
			propertiesSet = true;
			var currentOptionTr = jQuery('<tr/>', { "id" : propertyDetails.key, "rel" : propertyDetails.type, "class" : "property" });
			viewPropertiesTable.append(currentOptionTr);
			switch(propertyDetails.type) {
				case "image":
					currentOptionTr.append(
							'<td> <label><span>'+propertyDetails.description+'</span>: </label> </td> 														\
							 <td colspan="2"> 														 											\
								  <a href="javascript://" style="display: inline-block;">														\
									<img class="accordeonImage" src="'+propertyDetails.value+'" onclick="openImageValueHelpFor(this, \'\')"> 	\
								  </a>																											\
							 </td>');
				break;
				case "image_state":
					currentOptionTr.append(
							'<td> <label><span>'+propertyDetails.key+'</span>: </label> </td> 														\
							 <td> <input value="'+propertyDetails.description+'" style="width: 50px;"> </td> 									\
							 <td> 														 														\
								  <a href="javascript://" style="display: inline-block;">														\
									<img class="accordeonImage" src="'+propertyDetails.value+'" onclick="openImageValueHelpFor(this, \'\')"> 	\
								  </a>																											\
							 </td>');
				break;
				case "string":
					var firstTd = $("<td/>");
					firstTd.append("<label><span>"+propertyDetails.description+"</span>: </label>");

					var secondTd = $("<td/>", { "colspan" : "2"});
					var input = $("<input />", {		"type" : "text",
														/*"style": "width: 100px;",*/
														"value" : propertyDetails.value
													});
					input.on('input',function(element) {
						handleViewChange(viewDetails,propertyDetails,stageObject,element);
					});
					input.appendTo(secondTd)
					currentOptionTr.append(firstTd);
					currentOptionTr.append(secondTd);
				break;				
				case "double":
					var firstTd = $("<td/>");
					firstTd.append("<label><span>"+propertyDetails.description+"</span>: </label>");

					var secondTd = $("<td/>", { "colspan" : "2"});
					var input = $("<input />", {		"type" : "text",
														"value" : propertyDetails.value
													});
					input.on('input',function(element) {
						handleViewChange(viewDetails,propertyDetails,stageObject,element);
					});
					input.appendTo(secondTd)
					input.spinner(
					{
						step : 0.5,  
						spin: function( element ) { 
							handleViewChange(viewDetails,propertyDetails,stageObject,element);
						}
					});
					currentOptionTr.append(firstTd);
					currentOptionTr.append(secondTd);
					input.on('input',function(element) {
							handleViewChange(viewDetails,propertyDetails,stageObject,element);
						});
				break;
				case "alignment" :

					var firstTd = $("<td/>").appendTo(currentOptionTr);
					firstTd.append("<label><span>"+propertyDetails.description+"</span>: </label>");

					var secondTd = $("<td/>", { "colspan" : "2"}).appendTo(currentOptionTr);
					var alignmentDiv = $("<div />", { "id" : stageObject.attr("id")+"-alignment" } ).appendTo(secondTd);
					alignmentDiv.append($("<input />", { "id" : stageObject.attr("id")+"-left", "value" : "left", "type" : "radio", "name" : stageObject.attr("id")+"-alignment"}));
					alignmentDiv.append($("<label />", { "text" : "", "for" : stageObject.attr("id")+"-left", "class" : "ic icon-align-left ic-tree  ic-2x ic-color"})); 
					alignmentDiv.append($("<input />", { "id" : stageObject.attr("id")+"-center","value" : "center", "type" : "radio", "name" : stageObject.attr("id")+"-alignment"}));
					alignmentDiv.append($("<label />", { "text" : "", "for" : stageObject.attr("id")+"-center", "class" : "ic icon-align-center  ic-2x ic-tree ic-color"})); 
					alignmentDiv.append($("<input />", { "id" : stageObject.attr("id")+"-right","value" : "right", "type" : "radio", "name" : stageObject.attr("id")+"-alignment"}));
					alignmentDiv.append($("<label />", { "text" : "", "for" : stageObject.attr("id")+"-right", "class" : "ic icon-align-right ic-2x ic-tree  ic-color"})); 
					alignmentDiv.find("[value="+propertyDetails.value+"]").prop('checked', true);
					alignmentDiv.find("input[type=radio]").change(function(sourceRadio,a,b) {
						handleViewChange(viewDetails,propertyDetails,stageObject,sourceRadio);
					});

					alignmentDiv.buttonset();
				break;
				case "color":
					var firstTd = $("<td/>");
					firstTd.append("<label><span>"+propertyDetails.description+"</span>: </label>");

					var secondTd = $("<td/>", { "colspan" : "2"});
					var colorPicker = $("<input />", {
														"type": "text",
														"class": "minicolors minicolors-input",
														"size": "7",
														"maxlength": "7",
														"value" : propertyDetails.value
													});

					colorPicker.appendTo(secondTd);
					currentOptionTr.append(firstTd);
					currentOptionTr.append(secondTd);

					colorPicker.minicolors( { change: function(hex, opacity) { 
							handleViewChange(viewDetails,propertyDetails,stageObject,hex);
						} });
				
						break;
				case "boolean":
					var firstTd = $("<td/>");
					firstTd.append("<label><span>"+propertyDetails.description+"</span>: </label>");

					var secondTd = $("<td/>", { "colspan" : "2"});
					var checkBox = $("<input />", {
														"type": "checkbox"
													});
					if(propertyDetails.value == true || propertyDetails.value=="true") {
						checkBox.attr("checked","checked");
					}
	
					checkBox.change(function(element) {
						handleViewChange(viewDetails,propertyDetails,stageObject,element);
					});
					checkBox.appendTo(secondTd)
					currentOptionTr.append(firstTd);
					currentOptionTr.append(secondTd);

				break;
			}
		});
		if(propertiesSet) {
			viewPropertiesDiv.append(viewPropertiesTable);
		}
		
		
		
		
		
		return viewPropertiesDiv;
	}
	function getBindingPropertiesDiv( 	controlDetails, // Control element details
										bindingObject    // optional binding (for defaulting values)
								    ) {
		if(controlDetails == null && bindingObject != null) {
			// Initialize controlDetails from binding
			controlDetails = getControlDetailsByIdentifiers(bindingObject.controllerIdentifier, bindingObject.nodeIdentifier, bindingObject.valueIdentifier);
		}
		
		var bindingPropertiesDiv = jQuery("<div />", { "id":"controlDetails", "class" : "popover-properties" })
		bindingPropertiesDiv.append("<h2>Control Details</h2>");
//		bindingPropertiesDiv.append("<span>Control type: "+controlDetails.controllerName +" > " + controlDetails.nodeName + " > "+ controlDetails.valueName+"</span>");
		var typeIcon = getTypeIcon(controlDetails.valueType);
		if(typeIcon != null) {
			bindingPropertiesDiv.append($("<span/>", { "text" : "Control type: "}).append(typeIcon.addClass("ic-2x")));
		}
		bindingPropertiesDiv.append($("<span/>", { "text" : controlDetails.controllerName +" > " + controlDetails.nodeName + " > "+ controlDetails.valueName }));
		
		
		bindingPropertiesDiv.append(jQuery("<input />", { "type":"hidden", "id":"valueIdentifier","value":controlDetails.valueIdentifier }));
		bindingPropertiesDiv.append(jQuery("<input />", { "type":"hidden", "id":"nodeIdentifier","value":controlDetails.nodeIdentifier }));
		bindingPropertiesDiv.append(jQuery("<input />", { "type":"hidden", "id":"controllerIdentifier", "value":controlDetails.controllerIdentifier }));

		/* Parse properties of binding details */
		switch(controlDetails.valueType) {
			case "sensor_general":
			case "sensor_temperature":
			case "sensor_humidity":
			case "sensor_binary":
			case "switch_binary":
			case "sensor_motion":
			case "dimmer":
			case "heating":
			case "general_command":
			case "ip_camera":
			case "radio_station":
			case "music_play_pause":
			case "music_random":
			case "music_action":
			case "http_command":		  
			break;
		}
		return bindingPropertiesDiv;
	}
	
	function isSelected(stageObject) {
		return stageObject.children().filter("#selectionWrapper").children().filter("#nwgrip").is(":visible");
	}
	function setImageCallback(value, selector) {
		$(selector).attr("src",value);
	}