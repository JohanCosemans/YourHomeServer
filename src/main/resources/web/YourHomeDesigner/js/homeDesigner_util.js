	window.typeMap = {

			"default" : {
				"icon" : " ic icon-folder ic-fw ic-color"
			},

			// Controllers:
			"zwave" : {
				//"icon" : "zwave"
				"icon" : "ic icon-waves ic-tree ic-fw ic-color"
			},
			"spotify" : {
				"icon" :  "ic icon-spotify ic-tree ic-fw ic-color"
			}
			,"http" : {
				"icon" : "ic icon-globe ic-tree ic-fw ic-color"
			},
			"radio" : {
				"icon" : "ic icon-radio ic-tree ic-fw ic-color"
			},
			"ipcamera" : {
				"icon" : "ic icon-camera ic-tree ic-fw ic-color"
			},
			"thermostat" : {
				"icon" : "ic icon-cloud ic-tree ic-fw ic-color"
			},
			"general" : {
				"icon" : "ic icon-folder ic-tree ic-fw ic-color"
			},
			"demo" : {
				"icon" : "ic icon-folder ic-tree ic-fw ic-color"
			},
			"philips_hue" : {
				"icon" : "ic icon-bridge_v2 ic-tree ic-fw ic-color"
			},

			// Nodes
			"http_node" : {
				"icon" : "ic icon-list ic-fw ic-tree ic-color"
			},
			"unknown_node" : {
				"icon" : " ic icon-folder ic-fw ic-tree ic-color"
			},
			"scenes" : {
				"icon" : " ic icon-clapperboard ic-fw ic-tree ic-color"
			},
			"navigation_node" : {
				"icon" : " ic icon-external-link-square ic-fw ic-tree ic-color"	
			},
			// End nodes
			
			// ValueTypes
			"zwave-dead" : {
				"icon" : "ic icon-skull ic-tree ic-fw ic-color"
			},
			"dimmer" : {
				"icon" : "ic icon-lightbulb-o ic-fw ic-color",
				"description" : "Dimmer"
			},
			"color_bulb" : {
				"icon" : "ic icon-white_and_color_e27_b22 ic-tree ic-fw ic-color"
			},
			"text" : {
				"icon" : "ic icon-document209 ic-fw ic-color",
				"description" : "Text"
			},
			"web_static_html" : {
				"icon" : "ic icon-code ic-fw ic-color",
				"description" : "HTML"
			},
			"switch_binary" : {
				"icon" : "ic icon-toggle-on ic-fw ic-color",
				"description" : "Binary Switch"
			},
			"sensor_general" : {
				"icon" : "ic icon-general_sensor ic-fw ic-color",
				"description" : "General Sensor"			
			},
			"sensor_luminosity" : {
				"icon" : "ic icon-sun-o ic-fw ic-color",
				"description" : "Luminosity Sensor"			
			},
			"sensor_temperature" : {
				"icon" : "ic icon-thermometer ic-fw ic-color",
				"description" : "Temperature Sensor"			
			},
			"sensor_binary" : {
				"icon" : "ic icon-motion ic-fw ic-color",
				"description" : "Binary Sensor"
			},
			"sensor_motion" : {
				"icon" : "ic icon-motion ic-fw ic-color",
				"description" : "Binary Sensor"
			},
			"sensor_humidity" : {
				"icon" : "ic icon-droplet ic-fw ic-color",
				"description" : "Humidity Sensor"
			},
			"sensor_alarm" : {
				"icon" : "ic icon-siren ic-fw ic-color",
				"description" : "Alarm Sensor"
			},
			"meter" : {
				"icon" : "ic icon-tachometer ic-fw ic-color",
				"description" : "Meter"			
			},
			"heating" : {
				"icon" : "ic icon-fire  ic-fw ic-color",
				"description" : "Heating"
			},
			"http_command" : {
				"icon" : "ic icon-terminal ic ic-fw ic-color",
				"description" : "HTTP Command"
			},
			"radio_station" : {
				"icon" : "ic icon-radio ic ic-fw ic-color",
				"description" : "Radio Channel"
			},
			"general_command" : {
//				"icon" : "ic icon-th-small ic ic-fw ic-color",
				"icon" : "ic icon-infinity2 ic ic-fw ic-color",
				"description" : "General Command (all off, all on)"
			},
			"music_play_pause" : {
				"icon" : "ic icon-play-circle-o ic ic-fw ic-color",
				"description" : "Play / Pause"
			},
			"music_action" : {
				"icon" : "ic icon-dot-circle-o ic ic-fw ic-color",
				"description" : "Music Action (next, previous, ...)"
			},
			"music_random" : {
				"icon" : "ic icon-random ic ic-fw ic-color",
				"description" : "Random On / Off"
			},
			"music_album_image" : {
				"icon" : "ic icon-picture-o ic ic-fw ic-color",
				"description" : "Album image"
			},
			"music_playlist" : {
				"icon" : "ic icon-dot-circle-o ic ic-fw ic-color",
				"description" : "Playlist"
			},
			"music_playlists" : {
				"icon" : "ic icon-eject ic ic-fw ic-color",
				"description" : "Open playlist"
			},
			"music_progress" : {
				"icon" : "ic icon-percent ic ic-fw ic-color",
				"description" : "Music Progressbar"
			},
			"music_track_display" : {
				"icon" : "ic icon-dot-circle-o ic ic-fw ic-color",
				"description" : "Current playing track"
			},"ip_camera" : {
				"icon" : "ic icon-video-camera ic ic-fw ic-color",
				"description" : "IP Camera"
			},"scene_activation" : {
				"icon" : "ic icon-clapperboard ic-fw ic-color",
				"description" : "Scene Activation"
			},"page_navigation" : {
				"icon" : "ic icon-chain ic-fw ic-color",
				"description" : "Navigation"
			}
			// End ValueTypes
		  };
	
	$(document).ready(function(){
	
		// Browser supports HTML5 multiple file?
		var multipleSupport = typeof $('<input/>')[0].multiple !== 'undefined',
		isIE = /msie/i.test( navigator.userAgent );
	  
		$.fn.customFile = function(file,options) {
											
			return this.each(function() {
	
			  var $file = $(this).addClass('customfile'), // the original file input
				  $wrap = $('<div class="customfile-wrap">'),
				  $input = $('<input type="text" class="customfile-filename" />'),
				  // Button that will be used in non-IE browsers
				  $button = $('<button type="button" class="button">Upload</button>'),
				  // Hack for IE
				  $label = $('<label class="button" for="'+ $file[0].id +'">Open</label>');

			  // Hide by shifting to the left so we
			  // can still trigger events
			  $file.css({
				position: 'absolute',
				left: '-9999px'
			  });

			  $wrap.insertAfter( $file )
				.append( $file, $input, ( isIE ? $label : $button ) );

			  // Prevent focus
			  $file.attr('tabIndex', -1);
			  $button.attr('tabIndex', -1);

			  $input.click(function () {
				$file.focus().click(); // Open dialog
			  });
			
			  $file.change(function() {

				var files = [], fileArr, filename;

				// If multiple is supported then extract
				// all filenames from the file array
				if ( multipleSupport ) {
				  fileArr = $file[0].files;
				  for ( var i = 0, len = fileArr.length; i < len; i++ ) {
					files.push( fileArr[i].name );
				  }
				  filename = files.join(', ');

				// If not supported then just take the value
				// and remove the path to just show the filename
				} else {
				  filename = $file.val().split('\\').pop();
				}

				$input.val( filename ) // Set the value
				  .attr('title', filename) // Show filename in title tootlip
				  .focus(); // Regain focus

			  });

			  $input.on({
				blur: function() { $file.trigger('blur'); },
				keydown: function( e ) {
				  if ( e.which === 13 ) { // Enter
					if ( !isIE ) { $file.trigger('click'); }
				  } else if ( e.which === 8 || e.which === 46 ) { // Backspace & Del
					// On some browsers the value is read-only
					// with this trick we remove the old input and add
					// a clean clone with all the original events attached
					$file.replaceWith( $file = $file.clone( true ) );
					$file.trigger('change');
					$input.val('');
				  } else if ( e.which === 9 ){ // TAB
					return;
				  } else { // All other keys
					return false;
				  }
				}
			  });

			  
			  var originalButtonText = $button.text();
			  
			var $uploader = new uploader(  file, 
									{
										url: options.url,
										progress: function(ev) { 
													$button.attr("disabled",true);
													$button.text(originalButtonText + " "+(Math.round((ev.loaded/ev.total),3)*100)+"%") 
													if(options.progress != undefined) {
														setTimeout(options.progress,50,ev);
													}
												  },
										error: function(ev) {
													$button.attr("disabled",false);
													$button.text(originalButtonText);
													if(options.error != undefined) {
														setTimeout(options.error,50,ev);
													}
												},
										success: function(ev) {
													$button.attr("disabled",false);
													$button.text(originalButtonText);
													if(options.success != undefined) {
														setTimeout(options.success,50,ev);
													}
												}
									}
								);		
								
			  $button.click(function () {
				$uploader.send();
			  });
			});

		  };
	  
		$.fn.restrict = function(regExp, additionalRestriction) {
			function restrictCharacters(myfield, e, restrictionType) {
				var code = e.which;
				var character = String.fromCharCode(code);
				// if they pressed esc... remove focus from field...
				if (code==27) { this.blur(); return false; }
				// ignore if they are press other keys
				// strange because code: 39 is the down key AND ' key...
				// and DEL also equals .
				if (!e.originalEvent.ctrlKey 
						&& code!=9 
						&& code!=0 
						&& code!=8 
						&& code!=36 
						&& code!=37 
						&& code!=38 
						&& (code!=39 || (code==39 && character=="'")) 
						&& code!=40 ) {
					if (character.match(restrictionType)) {
						return additionalRestriction(myfield.value, character);
					} else {
						return false;
					}
				}
				return true;
			}
			this.keypress(function(e){
				if (!restrictCharacters(this, e, regExp)) {
					e.preventDefault();
				}
			});
		};
	});

	$.fn.togglePanels = function(){
	  return this.each(function(){
		$(this).addClass("ui-accordion ui-accordion-icons ui-widget ui-helper-reset")
		.find("h3")
		.addClass("ui-accordion-header ui-helper-reset ui-state-default ui-corner-top jstree-default")
		.hover(function() { $(this).toggleClass("ui-state-hover"); })
		.prepend('<i class="ic icon-folder ic-fw ic-tree ic-color ic-margin"></i>')
		.prepend('<i class="jstree-open jstree-ocl jstree-icon node-closed"></i>')
		.click(function() {
		  $(this)
			.find(".jstree-icon").toggleClass("node-open node-closed");
		  $(this).find("> .ic").toggleClass("ic icon-folder-open ic icon-folder")
			.end()
			.next()
			.slideToggle();
		  return false;
		})
		.next()
		  .addClass("ui-accordion-content ui-helper-reset ui-widget-content ui-corner-bottom")
		  .hide();
	  });
	};
	
	function ptToPx(pt) {
		return parseFloat(pt)*1.333333;
	}
	function pxToPt(px) {
		return parseFloat(px)/1.333333;
	}
	
	function abs2rel(absolutePath) {
		var pattern = "YourHomeDesigner/";
		var homeDesignerIndex = absolutePath.indexOf(pattern);
		if(homeDesignerIndex > 0) {
			return absolutePath.substr(homeDesignerIndex+pattern.length);
		}
		return absolutePath;
	}
	function formatTime(date, amPm) {
	  var hours = date.getHours();
	  var minutes = date.getMinutes();
	  if(amPm) {
	  	var ampm = hours >= 12 ? 'PM' : 'AM';
	  	hours = hours % 12;
	  	hours = hours ? hours : 12; // the hour '0' should be '12'
	  }
	  minutes = minutes < 10 ? '0'+minutes : minutes;
	  hours = hours < 10 ? '0'+hours:hours;
	  var strTime = hours + ':' + minutes;
	  if(ampm) {
	  	strTime += '' + ampm;
	  } 
	  return strTime;
	}
	function getStageObjectFromChildElement(childElement) {
		var found = false;
		var stageObject = $(childElement);
		
		while(!found) {
			if($(stageObject).attr("id") != undefined && $(stageObject).attr("id").substr(0,("StageElement_").length) == "StageElement_") {
				found = true;
			}else {
				stageObject = stageObject.parent();
			}
				
			//Fallback
			if(stageObject.attr("id") == "htmlStage") { return undefined };
		}
		return stageObject;
	}
	function getRotationDegrees(object) {
	    var matrix = object.css("-webkit-transform") ||
	    object.css("-moz-transform")    ||
	    object.css("-ms-transform")     ||
	    object.css("-o-transform")      ||
	    object.css("transform");
	    var angle = 0;
	    if(typeof matrix === 'string' && matrix !== 'none') {
	        var values = matrix.split('(')[1].split(')')[0].split(',');
	        var a = values[0];
	        var b = values[1];
	        angle = Math.round(Math.atan2(b, a) * (180/Math.PI));
	    } else { 
	    	angle = 0; 
	    }
	    return angle;
   };
	function findByAttribute( objectArray, attributeName, attributeValue) {
		var i = 0; var found = false; var returnObject = null;
		while(!found && i<objectArray.length) {
			if(objectArray[i][attributeName] == attributeValue) {
				found = true;
				returnObject = objectArray[i];
			}
			i++;
		}
		
		return returnObject;
	}
	function invertColor(hexTripletColor) {
		var color = hexTripletColor;
		color = color.substring(1);           // remove #
		color = parseInt(color, 16);          // convert to integer
		color = 0xFFFFFF ^ color;             // invert three bytes
		color = color.toString(16);           // convert to hex
		color = ("000000" + color).slice(-6); // pad with leading zeros
		color = "#" + color;                  // prepend #
		return color;
	}
	function getViewDetailsById(imageID) {
	
		var clonedImage = null;
		var returnObjects = jQuery.grep( window.images, 
										function (image) { 
											if(image.objects != undefined) {
												var returnImages = jQuery.grep( image.objects, 
																	 function(imageObject) {
																		return imageObject.id == imageID;
																	 });
												if (returnImages.length > 0) {
													clonedImage = jQuery.extend(true, {}, returnImages[0]);
													return true; 
												}
											}
											return false; 
										}
									);
		return clonedImage;
	}
	function getControlDetailsByIdentifiers(controllerIdentifier, nodeIdentifier, valueIdentifier) {
		return getControlDetailsById(controllerIdentifier+"-"+nodeIdentifier+"-"+valueIdentifier+"-");
	}
	function getControlDetailsById(treeNodeId) {
		// Navigate in jstree structure to find the element with that control id.
		var treeNode = $.jstree.reference("#binding-selector-tree").get_node("#"+treeNodeId);
		if(treeNode) {
			return getControlDetails(treeNode);
		}else {
			return null;
		}
	}
	function getControlDetails(treeNode) {
		
		// Check if element is in $("#binding-selector-tree")
		// Check if element is a controller, node or value
		//var treeElement = $("#binding-selector-tree").parents().length;
		//var childElement = $(childObject).parents().length;
		//var elementDistance = childElement - treeElement
		var tree = $.jstree.reference("#binding-selector-tree");
		var returnObject = new Object();
		
		if(treeNode.parents.length == 1) {
			// Controller
			returnObject.type = "CONTROLLER";
			returnObject.controllerIdentifier = treeNode.li_attr.identifier;
			returnObject.controllerName = treeNode.text;
		}else if(treeNode.parents.length == 2) {
			// Node
			returnObject.type = "NODE";
			var controller = tree.get_node("#"+treeNode.parents[0]);
			returnObject.controllerIdentifier = controller.li_attr.identifier;
			returnObject.controllerName = controller.text;
			returnObject.nodeIdentifier = treeNode.li_attr.identifier;
			returnObject.nodeName = treeNode.text;
		}else if(treeNode.parents.length == 3) {
			// Value
			returnObject.type = "VALUE";
			var controller = tree.get_node("#"+treeNode.parents[1]);
			returnObject.controllerIdentifier = controller.li_attr.identifier;
			returnObject.controllerName = controller.text;
			var node = tree.get_node("#"+treeNode.parents[0]);
			returnObject.nodeIdentifier = node.li_attr.identifier;
			returnObject.nodeName = node.text;
			returnObject.valueIdentifier = treeNode.li_attr.identifier;
			returnObject.valueName = treeNode.text;
			returnObject.valueType = treeNode.li_attr.rel;
		}
		
		return returnObject;
	}
	// Array sorting: sort(byProperty("zIndex"));
	var byProperty = function(prop,direction) {
		if(direction == undefined) {
			direction = 'ASC';
		}
		
		return function(a,b) {
			if(a != undefined && b != undefined) {
				if(direction == "ASC") {
					if (typeof Object.byString(a, prop) == "number") {
						return (Object.byString(a, prop) - Object.byString(b, prop));
					} else {
						return ((Object.byString(a, prop) < Object.byString(b, prop)) ? -1 : ((Object.byString(a, prop) > Object.byString(b, prop)) ? 1 : 0));
					}
				}else {
					if (typeof Object.byString(a, prop) == "number") {
						return (Object.byString(b, prop) - Object.byString(a, prop));
					} else {
						return ((Object.byString(a, prop) > Object.byString(b, prop)) ? -1 : ((Object.byString(a, prop) < Object.byString(b, prop)) ? 1 : 0));
					}
				}
			}
			return a;
		};
		/*return function(a,b) {
			if(a != undefined && b != undefined) {
				if(direction == "ASC") {
					if (typeof a[prop] == "number") {
						return (a[prop] - b[prop]);
					} else {
						return ((a[prop] < b[prop]) ? -1 : ((a[prop] > b[prop]) ? 1 : 0));
					}
				}else {
					if (typeof a[prop] == "number") {
						return (b[prop] - a[prop]);
					} else {
						return ((a[prop] > b[prop]) ? -1 : ((a[prop] < b[prop]) ? 1 : 0));
					}
				}
			}
			return a;
		};*/
	};
	
	Object.byString = function(o, s) {
		s = s.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
		s = s.replace(/^\./, '');           // strip a leading dot
		var a = s.split('.');
		for (var i = 0, n = a.length; i < n; ++i) {
			var k = a[i];
			if (k in o) {
				o = o[k];
			} else {
				return;
			}
		}
		return o;
	}

	function getZWaveObjectByNodeId(nodeId) {
		var returnValues = jQuery.grep( window.zwaveObjects, 
			function(zwaveValueObject) {
				return zwaveValueObject.id == nodeId;
			});
		   if (returnValues.length > 0) {
				return returnValues[0];
			}
	}
	
	function getZWaveValue(controlId) {

		var returnValuesIDs = new Array();
		var returnObjects = jQuery.grep( window.zwaveObjects, 
										function (zwaveNode) { 
											var returnValues = jQuery.grep( zwaveNode.values, 
																 function(zwaveValueObject) {
																 return zwaveValueObject.controlId == controlId;
															   });
											if (returnValues.length > 0) {
												returnValuesIDs.push(returnValues[0]);
												return true; 
											}
											return false; 
										}
									);
		if(returnValuesIDs.length > 0) {
			return returnValuesIDs[0];
		}else {
			return null;
		}
	}

	function showOkCancelDialog(dialogText, okFunc, cancelFunc, dialogTitle) {
		$('<div style="padding": 10px;max-width:500px;word-wrap:break-word">'+dialogText+'</div>').dialog( 
		{
			autoOpen: false,
			modal: true,
			title: dialogTitle || 'Confirm',
			buttons: {
				"Ok": function() {
					if(typeof(okFunc) == 'function') {
						setTimeout(okFunc,50);
					}
					$(this).dialog('destroy');
				},
				"Cancel" : function() {
					if(typeof(cancelFunc) == 'function') {
						setTimeout(cancelFunc,50);
					}
					$(this).dialog('destroy');
				}
			}
		}).dialog('open');
	}
	function showYesNoDialog(dialogText, okFunc, noFunc, dialogTitle) {
		$('<div style="padding": 10px;max-width:500px;word-wrap:break-word">'+dialogText+'</div>').dialog( 
		{
			autoOpen: false,
			modal: true,
			title: dialogTitle || 'Confirm',
			buttons: {
				"Yes": function() {
					if(typeof(okFunc) == 'function') {
						setTimeout(okFunc,50);
					}
					$(this).dialog('destroy');
				},
				"No" : function() {
					if(typeof(noFunc) == 'function') {
						setTimeout(noFunc,50);
					}
					$(this).dialog('destroy');
				}
			}
		}).dialog('open');
	}
	function showMessageDialog(dialogText, dialogTitle, optionalFunction) {
		$('<div style="padding": 10px;max-width:500px;word-wrap:break-word;z-index:99999999999" class="dialog">'+dialogText+'</div>').dialog( 
		{
			autoOpen: false,
			modal: true,
			title: dialogTitle || 'Message',
			buttons: {
				"Ok": function() {
					if(typeof(optionalFunction) == 'function') {
						setTimeout(optionalFunction,50);
					}
					$(this).dialog('destroy');
				}
			}
		}).dialog('open');
	}
	
	
	function getPageById(pageId) {
		var found = false;
		var i = 0;
		while(!found && i < window.currentProject.pages.length) {
			if(window.currentProject.pages[i] != undefined && window.currentProject.pages[i].pageId == pageId) {
				found = true;
				return window.currentProject.pages[i];
			}
			i++;
		}
	}
	function setPageById(pageId, page) {
		var found = false;
		var i = 0;
		while(!found && i < window.currentProject.pages.length) {
			if(window.currentProject.pages[i] != undefined && window.currentProject.pages[i].pageId == pageId) {
				if(page == undefined) {
					window.currentProject.pages.splice(i,1);
				}else {
					window.currentProject.pages[i] = page;
				}
				found = true;
			}
			i++;
		}
		return found;
	}
	function deletePage(pageId) {
		var cont = true;
		var i = 0;
		while(cont & i < window.currentProject.pages.length) {
			var currentPage = window.currentProject.pages[i];
			if(currentPage.pageId == pageId) {
				cont = false;
				window.currentProject.pages.splice(i,1);
			}
			i++;
		}

		alignNavigationNode();
	}
	function openImageValueHelp(setFunction, selector, url) {
		$('<div></div>').dialog( 
		{
			width: "800px",
			height: "auto",
			autoOpen: false,
			modal: true,
			title: 'Choose image',
			open : function() {
				createImageValueHelp($(this), url);
			},
			buttons: {
				"Ok": function() {
					var selectedImage = $(this).find("[style='display: block;'] ul .selected img");
					var imageSrc = selectedImage.attr('src');
					setFunction(imageSrc, selector);
					$(this).dialog('destroy');
				},
				"Cancel": function() {
					$(this).dialog('destroy');
					return "";
				}
			}
		}).dialog('open');
	
	}	
	function openImageValueHelpFor(imgElement, url) {
		$('<div></div>').dialog( 
				{
					width: "800px",
					height: "auto",
					autoOpen: false,
					modal: true,
					title: 'Choose image',
					open : function() {
						createImageValueHelp($(this), url);
					},
					buttons: {
						"Ok": function() {
							var selectedImage = $(this).find("[style='display: block;'] ul .selected img");
							var imageSrc = selectedImage.attr('src');
							$(imgElement).attr("src",imageSrc);
							$(this).dialog('destroy');
						},
						"Cancel": function() {
							$(this).dialog('destroy');
							return "";
						}
					}
				}).dialog('open');
	}
	
	function createImageValueHelp(where, link) {

		var imageTabsContainer = jQuery('<div/>',  { 'id' : 'imagesValueHelpTab' });
		where.append(imageTabsContainer);
		imageTabsContainer.tabs();
		var imageUlContainer =  jQuery('<ul/>');
		// Retrieve JSON content with the images

		var url = rootUrl+' /api' + '/Images';
		if ( link != "" ) {
			url = url + '/' + link;
		}
		var i=0;
		$.getJSON(url, function(data) {

			// Parse the content: Create a tab for all the types
			$.each(data, function(key, imageSection) {	
					var imagePicker = jQuery('<select/>', { "class" : "image-picker" });
					var imageTabObject = jQuery('<li/>');
					var tabTitle = jQuery('<a/>',  { 'href' : '#tab_'+i, 'text' : imageSection.title });
					var imageTabContent = jQuery('<div/>',  { 'id' : 'tab_'+i });

					// Fill content of tab
					var j = 1;
					$.each(imageSection.objects, function(key,imageObjects ) {	
						if(imageObjects.type == 'image' || imageObjects.type == 'image_button') {
							var imageOption = jQuery('<option/>',  { 'data-img-src' : imageObjects.icon, "value" : j, "class":"accordeonImage" });
							imagePicker.append(imageOption);	
							j++;
						}
					});
					imageTabContent.append(imagePicker);
					imagePicker.imagepicker();
					// Append title to list and content to container
					imageTabObject.append(tabTitle);
					imageUlContainer.append(imageTabObject);
					imageTabsContainer.append(imageTabContent);
					i++;
			});
			imageTabsContainer.prepend(imageUlContainer);
			imageTabsContainer.tabs( "refresh" ).tabs("option", "active", 0);
		});
	}



	


