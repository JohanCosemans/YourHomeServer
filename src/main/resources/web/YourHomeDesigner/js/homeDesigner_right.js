/** Right panel **/

	$(document).ready(function(){
		
		//$("#sidebar-right-wrapper").resizable( { handles: { 'w': '#sidebar-right-wrapper-wgrip' } });
		/*var sidebarRight = $("#sidebar-right-wrapper");
		var rightResizer = $("#sidebar-right-wrapper-wgrip");
		rightResizer.on("mousedown", function(e) {
			var startX = parseFloat(e.clientX);
			var startWidth = sidebarRight.width();
			rightResizer.on("mousemove", function(e) {
				var newWidth = startWidth-(parseFloat(e.clientX) - startX);
				sidebarRight.css("width",newWidth+"px")
				console.log("startX: "+startX);
				console.log("e.clientX: "+e.clientX);
				console.log("startWidth: "+startWidth);
				console.log("newWidth: "+newWidth);
			});
			
			rightResizer.on("mouseup", function(e) {
				rightResizer.unbind("mousemove");
			});
			console.log(e);
		});*/

		$("#sidebar-right-wrapper .sidebar-title").click(function() {
			var widthToggled = false;
			if($("#sidebar-right-wrapper").hasClass("sidebar-right-wrapper-width")) {
				$("#sidebar-right-wrapper").toggleClass("sidebar-right-wrapper-width sidebar-right-wrapper-width-auto");
				widthToggled = true;
			}

			$("#sidebar-right-wrapper #binding-selector-tree").animate({width:'toggle', direction: "right"},500);
			$(this).find(".title").animate({width:'toggle'},500, function() {
				if(!widthToggled) {
					$("#sidebar-right-wrapper").toggleClass("sidebar-right-wrapper-width sidebar-right-wrapper-width-auto");
				}

				sizeWrapperWindow();
			});
			
		});

		initCommands();
		
		var logging = false;
		$( "#homeserver-log-dialog" ).dialog({
			autoOpen: false,
			modal: false,
			width: '930px',
			logType : undefined, // Will be set through the "option"
			open: function() {
				logging = true;
				//var logType = $( this ).dialog("option","logType");
				var logType = window.dialogOptions.logType;
				refreshLog(logType);
				$("#homeserver-log-dialog-download").attr("href","/api/Logs/"+logType+"/0");
				$("#homeserver-log-dialog-logarea").animate({
					scrollTop:$("#homeserver-log-dialog-logarea")[0].scrollHeight - $("#homeserver-log-dialog-logarea").height()
				},300)
			},
			close: function() { 
				logging = false;	},
			buttons: {
				"Close": function() {
					$( this ).dialog( "close" );
				}
			}
		});
		$( "#zwave-configuration-dialog" ).dialog({
			autoOpen: false,
			modal: true,
			width: '550' 
		});
		
		//logType: ZWave / HomeServer
		function refreshLog(logType) {
			$.ajax({
			  url: '/api/Logs/'+logType+'/10000',
			  type: 'GET',
			  success: function(data) {
				$("#homeserver-log-dialog-logarea").val(data);				
				if(logging) {
					setTimeout(function() { refreshLog(logType) }, 2000);
				}
			  }
			});
		}

		jQuery( "#zwave-node-associations-dialog" ).dialog({
			autoOpen: false,
			//height: '200',
			width: '650',
			modal: true,
			open: function() {		
				var nodeId = $("#zwave-node-associations-dialog-nodeId").val();

				// Replace the span objects with ID "node" with the current node ID
				$(this).find("#node").each(function() { $(this).text(nodeId);});

				// Default the current node
				$(this).find("#zwave-node-associations-fromNode").empty().append('<option value="'+nodeId+'"> Node '+nodeId+'</option>');
				var toDropdown = $(this).find("#zwave-node-associations-toNode").empty();
				for(var i=0;i<window.zwaveObjects.length;i++) {
					if(window.zwaveObjects[i].id != nodeId) {
						toDropdown.append('<option value="'+window.zwaveObjects[i].id+'"> Node '+window.zwaveObjects[i].id+'</option>');
					}	
				}
				// Fill the associations table for the selected node
				var associationsList = $("#zwave-node-associations-list");
				var associationGroupsList = $("#zwave-node-associations-associationClass");
				
				associationsList.empty();
				associationGroupsList.empty();
				associationsList.append('<tr>' +
										'<td class="dialog-table-header" width="25%">From node</td>' +
										'<td class="dialog-table-header" width="25%">To node</td>' +
										'<td class="dialog-table-header" width="40%">Association class</td>' +
										'<td class="dialog-table-header" width="10%"> </td>'+
										'</tr>');

				$.ajax({
					  url: '/api' + '/ZWave/Nodes/' + nodeId + '/Associations',
					  type: 'GET',
					  success: function(data) {
							if(data != undefined && data.associations.length > 0) {
								for(var i=0;i<data.associations.length;i++) {
									var association = data.associations[i];
									var associationClassName = association.associationClass;
									for(var j=0;j<data.associationGroups.length;j++) {
										if(association.associationClass == data.associationGroups[j].associationGroup) {
											associationClassName = associationClassName + ". " + data.associationGroups[j].label;
											break;
										}
									}
									
									var appendString =  
															"<tr><td>Node "+association.fromNode+"</td>"+
															"<td>Node"+association.toNode+"</td>"+
															"<td>"+associationClassName+"</td>"+
															"<td><a href=\"#\" onClick=\"deleteAssociation("+data.homeId+","+association.fromNode+","+association.toNode+","+association.associationClass+")\"><img src = \"css/menu/delete.png\" /></a></td>"+
															"</tr>";
									associationsList.append(appendString);
								}
								for(var i=0;i<data.associationGroups.length;i++) {
									var associationGroup = data.associationGroups[i];
									var appendString = "<option value=\""+associationGroup.associationGroup+"\">"+associationGroup.associationGroup+". "+associationGroup.label+" ( maximum "+associationGroup.maxAssociations+" )</option>";
									associationGroupsList.append(appendString);
								}
							} else {
								associationsList.append('<tr><td colspan = "5">There are no associations for this node</td></tr>');
							}
					  },
					  error: function(error) {

					  }
				});
			},
			buttons: {
				"Close": function() {
					$( this ).dialog( "close" );
				}
			}
		});

		$( "#zwave-value-alias-dialog" ).dialog({
			autoOpen: false,
			height: 'auto',
			width: '600',
			modal: true,
			buttons: {
				"Ok": function() {
					$("#zwave-value-alias-loader").show();
					$("#zwave-value-alias-dialog :input").attr("disabled", true);

					var startNode = window.dialogOptions.startNode;
					var valueDetails = getControlDetails(startNode);
					var alias =  $("#zwave-value-alias-input").val();

					// Rename value
					if(valueDetails.type == "VALUE") {
						$.ajax({
						  url: '/api/Views/Value/'+valueDetails.controllerIdentifier+"/"+valueDetails.nodeIdentifier+"/"+valueDetails.valueIdentifier+"/"+alias,
						  type: 'PUT',
						  data: valueDetails,
						  success: function(data) {
							// Rename node in tree
							var tree = $.jstree.reference("#binding-selector-tree");
							tree.rename_node($("#"+startNode.id), alias);
							$("#zwave-value-alias-dialog").dialog( "close" );
							$("#zwave-value-alias-dialog :input").attr("disabled", false);
						  },
						  error: function(error) {
						  	showMessageDialog("Could not save the new alias. Please try again.");
							$("#zwave-value-alias-dialog").dialog( "close" );
							$("#zwave-value-alias-dialog :input").attr("disabled", false);
						  }
						});
					}else // Rename value
					if(valueDetails.type == "NODE") {
						$.ajax({
						  url: '/api/Views/Node/'+valueDetails.controllerIdentifier+"/"+valueDetails.nodeIdentifier+"/"+alias,
						  type: 'PUT',
						  data: valueDetails,
						  success: function(data) {
							// Rename node in tree
							var tree = $.jstree.reference("#binding-selector-tree");
							tree.rename_node($("#"+startNode.id), alias);
							$("#zwave-value-alias-dialog").dialog( "close" );
							$("#zwave-value-alias-dialog :input").attr("disabled", false);
						  },
						  error: function(error) {
						  	showMessageDialog("Could not save the new alias. Please try again.");
							$("#zwave-value-alias-dialog").dialog( "close" );
							$("#zwave-value-alias-dialog :input").attr("disabled", false);
						  }
						});
					}
				},
				"Cancel" : function() {
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
					$( this ).dialog( "close" );
			},
			close: function() {
				$("#zwave-value-alias-loader").hide();
			}
		});

		$( "#zwave-value-selector-dialog" ).dialog({
			autoOpen: false,
			//height: '600',
			width: '650',
			modal: true,
			buttons: {
				"Ok": function() {
					$("#zwave-value-selector-loader").show();
					$("#zwave-value-selector-dialog").scrollTop($("#zwave-value-selector-dialog-form").height());
					// Read fields on the screen
					var subscriptionMessage = new Object();
					subscriptionMessage.nodeId = $("#zwave-value-selector-dialog-nodeId").val();
					subscriptionMessage.homeId = $("#zwave-value-selector-dialog-homeId").val();

					var checkboxArray = new Array();

					var first = true;
					$('#zwave-value-selector-dialog-form tr').each(function() {
						if(!first) {
							var checkboxPoll = $(this).find("#poll input[type=checkbox]");
							var checkboxSubscribe = $(this).find("#subscribe input[type=checkbox]");

							var subscriptionAndPollValues = new Object();						
							subscriptionAndPollValues.valueId = checkboxPoll.attr("id").split("_")[1].substr(1);
							subscriptionAndPollValues.instance = checkboxPoll.attr("id").split("_")[2].substr(1);
							subscriptionAndPollValues.subscribed = checkboxSubscribe.is(":checked");
							subscriptionAndPollValues.polled = checkboxPoll.is(":checked");
							checkboxArray.push(subscriptionAndPollValues);
						}else {
							first = false;
						}
					});

					subscriptionMessage.subscriptions = checkboxArray;
					var subscriptionJSON = JSON.stringify(subscriptionMessage);
					$("#zwave-value-selector-dialog-form :input").attr("disabled", true);
					$.ajax({
					  url: '/api/ZWave/Nodes/'+subscriptionMessage.nodeId+'/Subscriptions',
					  type: 'PUT',
					  data: subscriptionJSON,
					  success: function(data) {
						// Add node to tree
						var tree = $.jstree.reference("#binding-selector-tree");
						var startNode = window.dialogOptions.startNode;
						// Delete all children from node
						var childrenList = startNode.children.slice();
						$.each(childrenList, function(key, value) {	
							tree.delete_node($("#"+value));
						});
						$('#zwave-value-selector-dialog-form input[type=checkbox]').each(function () {
							if(this.checked 
								&& this.id.substr(0,("SUBSCRIBE_").length) == "SUBSCRIBE_" 
								&& $("#ZV_"+this.id.substr(("SUBSCRIBE_").length)).length == 0) {

									// Get value object
									var zwaveValue = getZWaveValue("ZV_"+this.id.substr(("SUBSCRIBE_").length));
									var nodeIdentifier = $('#'+startNode.id).attr("identifier");
									var node = tree.create_node(startNode,  { 	
												"id" : "zwave-"+nodeIdentifier+"-"+zwaveValue.controlId+"-", 
												"text" : zwaveValue.valueLabel, 
												"type" : zwaveValue.valueTypeOfValue.value,
												"rel"  : zwaveValue.valueTypeOfValue.value,
												"li_attr" : {
															"type" : zwaveValue.valueTypeOfValue.value,
															"identifier" : zwaveValue.controlId,
															"rel" : zwaveValue.valueTypeOfValue.value
														 }
											  }, "first", function(a) {
												//$.jstree.reference("#binding-selector-tree").open_node(startNode)
												//$('#'+startNode.id).attr("identifier",nodeIdentifier);
												//$('#'+a.id).attr("identifier",zwaveValue.controlId);
												//$('#'+a.id).attr("rel",zwaveValue.valueTypeOfValue.value);
											  }); 
							}
						});
						$("#zwave-value-selector-dialog").dialog( "close" );
					  },
					  error: function(error) {
		  				showMessageDialog("Could not save the subscriptions. Please try again.");
						$("#zwave-value-selector-dialog").dialog( "close" );
					  }
					});
				},
				"Cancel" : function() {
					$("#zwave-value-selector-dialog-form").empty();
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
					$( this ).dialog( "close" );
			},
			close: function() {
				$("#zwave-value-selector-dialog-form").empty();
				$("#zwave-value-selector-loader").hide();
			}
		});
		 
		$( "#zwave-value-configuration-dialog" ).dialog({
			autoOpen: false,
			height: '400',
			width: '600',
			modal: true,
			buttons: {
				"Ok": function() {

					$("#zwave-value-configuration-loader").show();
					$("#zwave-value-configuration-dialog").scrollTop($("#zwave-value-configuration-dialog-form").height());
					// Read fields on the screen and compare with initial setup: send the values that have changed
					var configurationMessage = new Object();
					configurationMessage.node = $("#zwave-value-configuration-dialog-nodeId").val();
					var configurationArray = new Array();
					$('#zwave-value-configuration-dialog-form input').each(function () {
						var inputValue = new Object();
						if(!this.disabled && this.type != "button") {
							// Read value from original object
							var zwaveValue = getZWaveValue($(this).attr("rel"));							
							inputValue.valueId = zwaveValue.valueId;
							
							// For inputfield:
							if(zwaveValue != null && this.type == "text" && zwaveValue.value != this.value) {
								// If it was changed, add it to the list
								// Read value from screen
								inputValue.nodeId = zwaveValue.nodeId;
								inputValue.value = this.value;
								inputValue.homeId = zwaveValue.homeId;
								inputValue.valueIndex = zwaveValue.valueIndex; // Still needed??
								inputValue.nodeInstance = zwaveValue.instance;
							
								configurationArray.push(inputValue);
							}
							// For checkboxes:
							if(zwaveValue != null && this.type == "checkbox" && 
								( (zwaveValue.value == "true" && !this.checked)  
									|| (zwaveValue.value == "false" && this.checked) )) {
								// If it was changed, add it to the list
								// Read value from screen
								inputValue.nodeId = zwaveValue.nodeId
								
								if(this.checked) {
									inputValue.value = "true";
								}else {
									inputValue.value = "false";
								}
								inputValue.homeId = zwaveValue.homeId;
								inputValue.valueIndex = zwaveValue.valueIndex; // Still needed??
								inputValue.nodeInstance = zwaveValue.instance;
							
								configurationArray.push(inputValue);
							}

						}
						
					});
					$('#zwave-value-configuration-dialog-form select').each(function () {
						var inputValue = new Object();
						if(!this.disabled) {
							// Read value from original object
							var zwaveValue = getZWaveValue($(this).attr("rel"));
							
							inputValue.valueId = zwaveValue.valueId;
							
							// For inputfield & checkboxvalues:
							if(zwaveValue != null && zwaveValue.valueListSelection != this.value) {
								// If it was changed, add it to the list
								// Read value from screen
								inputValue.nodeId = zwaveValue.nodeId
								inputValue.value = this.value;
								inputValue.homeId = zwaveValue.homeId;
								inputValue.valueIndex = zwaveValue.valueIndex; // Still needed??
								inputValue.nodeInstance = zwaveValue.instance;
							
								configurationArray.push(inputValue);
							}
						}
					});
					
					configurationMessage.configurations = configurationArray;
					var configurationJSON = JSON.stringify(configurationMessage);
					$("#zwave-value-configuration-dialog-form :input").attr("disabled", true);
					$.ajax({
					  url: apiBase + '/ZWave/Nodes/'+configurationMessage.node+'/Configurations',
					  type: 'PUT',
					  data: configurationJSON,
					  success: function(data) {
						//$("#binding-selector-tree").jstree("refresh",-1);
						$("#zwave-value-configuration-dialog").dialog( "close" );
					  },
					  error: function(error) {
					  	showMessageDialog("Could not save the configuration. Please try again.");
						$("#zwave-value-configuration-dialog").dialog( "close" );
					  }
					});

				},
				"Cancel" : function() {
					$("#zwave-value-configuration-dialog-form").empty();
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$( this ).dialog( "close" );
			},
			close: function() {
				$("#zwave-value-configuration-dialog-form").empty();
				$("#zwave-value-configuration-loader").hide();
			}
		});
		$("#zwave-transaction-dialog").dialog({
			autoOpen: false,
			modal: true,
			width: "450",
			open : function() {
				
				// Get transaction
				
				$("#zwave-transaction-dialog").data("continue", true);
				// Poll transaction status
				if($("#zwave-transaction-dialog").data("transaction") != null) {
					var i=0;
					var updateStatus = function() {
						$.getJSON( "/api/ZWave/Nodes/Transaction/"+$("#zwave-transaction-dialog").data("transaction").id, function( data ) {
							$("#zwave-transaction-dialog").find("#zwave-transaction-command").text(data.commandTxt==null?"None":data.commandTxt);
							$("#zwave-transaction-dialog").find("#zwave-transaction-state").text(data.stateTxt==null?"None":data.stateTxt);
							if(data.error == null ||data.error == "NONE") {
								$("#zwave-transaction-dialog").find("#zwave-transaction-error").hide().prev().hide();
							}else {
								$("#zwave-transaction-dialog").find("#zwave-transaction-error").text(data.errorTxt==null?"None":data.errorTxt).show().prev().show();
							}
						});
						i++;
						if(i<500 && $("#zwave-transaction-dialog").data("continue")) {
							setTimeout(updateStatus, 1000);
						}
					};
					
					setTimeout(updateStatus, 1000);
						
				}
			},
			buttons: {
				"Stop" : function() {
					$( this ).dialog( "close" );
				}
			},
			close: function( event, ui ) {
				$.ajax({
					  url: "/api/ZWave/Nodes/Transaction/"+$("#zwave-transaction-dialog").data("transaction").id,
					  type: 'DELETE',
					  success: function(data) {
					  },
					  error: function() {
					  }
					});
											
				$("#zwave-transaction-dialog").data("continue", false);
			}
		});
	
		$("#new-httpcommand-dialog").dialog({
				autoOpen: false,
				width: '600',
				modal: true,
				open: function() {		
					
				},
				buttons: {
					"Create": function() {
						
						var dialog = $( this );
						var startNode = window.dialogOptions.startNode;

						var newCommandCreateObject = new Object();
						newCommandCreateObject.parentNodeId = $('#'+startNode.id).attr("identifier");
						newCommandCreateObject.controllerIdentifier = "http";
						newCommandCreateObject.type = "HttpCommand";
						newCommandCreateObject.name = $("#new-httpcommand-name").val();
						newCommandCreateObject.commandDescription = $("#new-httpcommand-description").val();
						newCommandCreateObject.url = $("#new-httpcommand-url").val();
						newCommandCreateObject.httpMethod = $("#new-httpcommand-method").val();
						newCommandCreateObject.messageType = $("#new-httpcommand-msgty").val();
						newCommandCreateObject.messageBody = $("#new-httpcommand-msgbody").val();
						newCommandCreateObject.description = $("#new-httpcommand-description").val();
						
						newCommandCreateObject.additionalHeaders = {};
						var headerKey = $("#new-httpcommand-header-1-key").val();
						if(headerKey != null && headerKey != ""){ newCommandCreateObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-1-value").val(); }
						var headerKey = $("#new-httpcommand-header-2-key").val();
						if(headerKey != null && headerKey != ""){ newCommandCreateObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-2-value").val(); }
						var headerKey = $("#new-httpcommand-header-3-key").val();
						if(headerKey != null && headerKey != ""){ newCommandCreateObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-3-value").val(); }
						var headerKey = $("#new-httpcommand-header-4-key").val();
						if(headerKey != null && headerKey != ""){ newCommandCreateObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-4-value").val(); }
						
						var identifier = newCommandCreateObject.parentNodeId;
						var rel = $('#'+startNode.id).attr("rel");
						$.ajax({
						  url: "/api" + '/HttpCommands',
						  type: 'POST',
						  data: JSON.stringify(newCommandCreateObject),
						  success: function(data) {
							  var tree = $.jstree.reference("#binding-selector-tree");
							  var node = tree.create_node(startNode,  { 	
													"id" : "http-"+newCommandCreateObject.parentNodeId+"-"+data.id+"-", 
													"text" : newCommandCreateObject.name, 
													"li_attr" : {
														"rel" : "http_command",
														"type" : "http_command",
														"identifier" : data.id
														},
													"type" : "http_command",
													"rel"  : "http_command"
												  }, "first", function(a) {
												  	tree.open_node(startNode);
													//$('#'+startNode.id).attr("identifier",identifier);
													//$('#'+startNode.id).attr("rel",rel);
													//$('#'+a.id).attr("identifier",data.id);
													//$('#'+a.id).attr("rel","http_command");
												  }); 
						  	showMessageDialog("HTTP Command successfully created!");		
						  },
						  error: function(error) {
						  	showMessageDialog("HTTP Command could not be created. Please try again.");							  }
						});

						$( this ).dialog( "close" );
					},
					"Cancel": function() {
						$( this ).dialog( "close" );
					}
				}
			});
		$("#edit-httpcommand-dialog").dialog({
				autoOpen: false,
				width: '650',
				modal: true,
				open: function() {		
					var dialog = $( this );
					var startNode = window.dialogOptions.startNode;
					var commandId = $('#'+startNode.id).attr("identifier");
					$.ajax({
					  url: "/api" + '/HttpCommands/Command/'+commandId,
					  type: 'GET',
					  success: function(data) {
						$("#edit-httpcommand-name").val(data.name);
						$("#edit-httpcommand-method").val(data.httpMethod);
						$("#edit-httpcommand-url").val(data.url);
						$("#edit-httpcommand-msgty").val(data.messageType);
						$("#edit-httpcommand-description").val(data.description);
						$("#edit-httpcommand-msgbody").val(data.messageBody);
						$("#edit-httpcommand-parentId").val(data.parentNodeId);
						
						if(data.additionalHeaders != null) {
							var i = 0;
							$.each(data.additionalHeaders,function(key,value) {
								if(i<4){
									$("#edit-httpcommand-header-"+(i+1)+"-key").val(key);
									$("#edit-httpcommand-header-"+(i+1)+"-value").val(value);
								}
								i++;
							});
						}
					  },
					  error: function(error) {
					  }
					});				
				},
				buttons: {
					"Edit": function() {
						var dialog = $( this );
						var startNode = window.dialogOptions.startNode;
						var commandId = $("#"+startNode.id).attr("identifier");

						var commandEditObject = new Object();
						commandEditObject.parentNodeId = $("#edit-httpcommand-parentId").val();
						commandEditObject.id = commandId
						commandEditObject.controllerIdentifier = "http";
						commandEditObject.type = "HttpCommand";
						commandEditObject.name = $("#edit-httpcommand-name").val();
						commandEditObject.commandDescription = $("#edit-httpcommand-description").val();
						commandEditObject.url = $("#edit-httpcommand-url").val();
						commandEditObject.httpMethod = $("#edit-httpcommand-method").val();
						commandEditObject.messageType = $("#edit-httpcommand-msgty").val();
						commandEditObject.messageBody = $("#edit-httpcommand-msgbody").val();
						commandEditObject.description = $("#edit-httpcommand-description").val();
						commandEditObject.additionalHeaders = {};
						var headerKey = $("#edit-httpcommand-header-1-key").val();
						if(headerKey != null && headerKey != ""){ commandEditObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-1-value").val(); }
						var headerKey = $("#edit-httpcommand-header-2-key").val();
						if(headerKey != null && headerKey != ""){ commandEditObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-2-value").val(); }
						var headerKey = $("#edit-httpcommand-header-3-key").val();
						if(headerKey != null && headerKey != ""){ commandEditObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-3-value").val(); }
						var headerKey = $("#edit-httpcommand-header-4-key").val();
						if(headerKey != null && headerKey != ""){ commandEditObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-4-value").val(); }

						$.ajax({
						  url: "/api" + '/HttpCommands/Command/'+commandId,
						  type: 'PUT',
						  data: JSON.stringify(commandEditObject),
						  success: function(data) {
							/*var newCommand = tree.create(startNode);
							newCommand.attr("id","HTTPC_"+data.id);
							tree.set_type("http-command",newCommand);
							tree.rename_node(newCommand,$("#edit-httpcommand-name").val())
							alert("Test success! Result: " + JSON.stringify(data));*/
							// @TODO: Change name in tree
							var tree = $.jstree.reference("#binding-selector-tree");
							tree.rename_node(startNode, $("#edit-httpcommand-name").val());
							showMessageDialog("Http Command changed");

						  },
						  error: function(error) {
						  	showMessageDialog("Http Command could not be changed: " + JSON.stringify(error));
						  }
						});

						$( this ).dialog( "close" );
					},
					"Cancel": function() {
						$( this ).dialog( "close" );
					}
				}
			});

		$("#new-httpnode-dialog").dialog({
				autoOpen: false,
				width: '650',
				modal: true,
				startNode : undefined, // Will be set through the "option"
				open: function() {		
					
				},
				buttons: {
					"Create": function() {
						
						var dialog = $( this );
						var tree = jQuery.jstree.reference("#binding-selector-tree");
						var startNode = window.dialogOptions.startNode;
						
						var newNodeCreateObject = new Object();
						newNodeCreateObject.controllerIdentifier = "http";
						newNodeCreateObject.type = "HttpNode";
						//newNodeCreateObject.parentId = startNode.attr("id");
						newNodeCreateObject.name = $("#new-httpnode-name").val();

						$.ajax({
						  url: "/api" + '/HttpCommands',
						  type: 'POST',
						  data: JSON.stringify(newNodeCreateObject),
						  success: function(data) {
						  	var node = tree.create_node(startNode,  { 	
														"id" : "http-"+data.id, 
														"text" : newNodeCreateObject.name, 
														"type" : "http_node",
														"li_attr" : {
															"rel" : "http_node",
															"type" : "http_node",
															"identifier" : data.id
														}
													  }, "first", function(a) {
													  	tree.open_node(startNode);
														//$('#'+a.id).attr("identifier",data.id);
														//$('#'+a.id).attr("rel","http-node");
													  }); 

						  	showMessageDialog("Group successfully created!");	
						  },
						  error: function(error) {
						  	showMessageDialog("Group could not be created. Please try again.");	
						  }
						});
						$( this ).dialog( "close" );
					},
					"Cancel": function() {
						$( this ).dialog( "close" );
					}
				}
			});
		$("#new-radioChannel-dialog").dialog({
			autoOpen: false,
			width: '650',
			modal: true,
			startNode : undefined, // Will be set through the "option"
			open: function() {		
				
			},
			buttons: {
				"Create": function() {
					
					var dialog = $( this );
					//var tree = jQuery.jstree._reference("#binding-selector-tree");
					//var startNode = dialog.dialog("option","startNode");
					var startNode = window.dialogOptions.startNode;
					
					var newChannelCreateObject = new Object();
					newChannelCreateObject.channelName = $("#new-radioChannel-name").val();
					newChannelCreateObject.channelUrl = $("#new-radioChannel-url").val();

					$.ajax({
					  url: "/api" + '/Radio',
					  type: 'POST',
					  data: JSON.stringify(newChannelCreateObject),
					  success: function(data) {

					    var tree = $.jstree.reference("#binding-selector-tree");
						var node = tree.create_node(startNode,  { 	
																			"id" : "radio-RadioChannels-"+data.id+"-", 
																			"text" : newChannelCreateObject.channelName, 
																			"type" : "radio_station",
																			"li_attr" : {
																				"rel" : "radio_station",
																				"type" : "radio_station",
																				"identifier" : data.id
																			}
																		  }, "first", function(a) {
																		  	//$("#"+a.id).attr("identifier",data.id);
																		  	//$("#"+a.id).attr("rel","radio_station");
																		  }); 

//					    var tree = $.jstree.reference("#binding-selector-tree");
//					    tree.create_node(startNode, "test");

					  	/*
						var newChannel = tree.create(startNode);
						newChannel.attr("id",data.id);
						newChannel.rename_node(newChannel,newChannelCreateObject.channelName)*/
						showMessageDialog("Radio channel created successfully!");	

					  },
					  error: function(error) {						  	
					  	showMessageDialog("Radio channel could not be created. Please try again.");
					  }
					});
					$( this ).dialog( "close" );
				},
				"Cancel": function() {
					$( this ).dialog( "close" );
				}
			}
		});
		$("#new-ipcamera-dialog").dialog({
			autoOpen: false,
			width: '650',
			modal: true,
			open: function() {		
				
			},
			buttons: {
				"Create": function() {
					
					var dialog = $( this );
	  			    var tree = $.jstree.reference("#binding-selector-tree");
					var startNode = window.dialogOptions.startNode;
					var newCameraCreateObject = new Object();
					newCameraCreateObject.name = $("#new-ipcamera-name").val();
					newCameraCreateObject.controllerIdentifier = "ipcamera"; // Ip camera
					newCameraCreateObject.snapshotUrl = $("#new-ipcamera-snapshot-url").val();
					newCameraCreateObject.videoUrl = $("#new-ipcamera-mjpg-url").val();
					newCameraCreateObject.url = $("#new-httpcommand-url").val();
					newCameraCreateObject.type = "IPCamera";

					$.ajax({
					  url: "/api" + '/IPCameras',
					  type: 'POST',
					  data: JSON.stringify(newCameraCreateObject),
					  success: function(data) {

						var node = tree.create_node(startNode,  { 	
																"id" : "ipcamera-Cameras-"+data.id+"-", 
																"text" : newCameraCreateObject.name, 
																 "type" : "ip_camera",
																"li_attr" : {
																	"rel" : "ip_camera",
																	"type" : "ip_camera",
																	"identifier" : data.id
																}
															  }, "first", function(a) {
																//$('#'+a.id).attr("identifier",data.id);
																//$('#'+a.id).attr("rel","ip_camera");
															  }); 
						showMessageDialog("IP Camera created successfully!");
					  },
					  error: function(error) {
						showMessageDialog("IP Camera could not be created. Please try again.");	
					  }
					});

					$( this ).dialog( "close" );
				},
				"Cancel": function() {
					$( this ).dialog( "close" );
				}
			}
		});
			
		$("#edit-ipcamera-dialog").dialog({
				autoOpen: false,
				width: '650',
				modal: true,
				open: function() {		
					// Get selected camera
					var dialog = $( this );
					//var cameraId = dialog.dialog("option","cameraId");
					var cameraId = window.dialogOptions.cameraId;
					
					// Retrieve camera information
					$.ajax({
					  url: "/api" + '/IPCameras/'+cameraId,
					  type: 'GET',
					  success: function(data) {
						$("#edit-ipcamera-name").val(data.name);
						$("#edit-ipcamera-snapshot-url").val(data.snapshotUrl);
						$("#edit-ipcamera-mjpg-url").val(data.videoUrl);
					  },
					  error: function(error) {
					  }
					});
				},
				buttons: {
					"Update": function() {
						
						var dialog = $( this );
						var tree = jQuery.jstree.reference("#binding-selector-tree");
						
						// Get selected camera
						//var cameraId = dialog.dialog("option","cameraId");
						var cameraId = window.dialogOptions.cameraId;
						var startNode = window.dialogOptions.startNode;
						
						var editCameraObject = new Object();
						editCameraObject.id = cameraId;
						editCameraObject.name = $("#edit-ipcamera-name").val();
						editCameraObject.controllerIdentifier = "ipcamera"; // Ip camera
						editCameraObject.snapshotUrl = $("#edit-ipcamera-snapshot-url").val();
						editCameraObject.videoUrl = $("#edit-ipcamera-mjpg-url").val();
						editCameraObject.url = $("#edit-httpcommand-url").val();
						editCameraObject.type = "IPCamera";

						$.ajax({
						  url: "/api" + '/IPCameras/' + cameraId,
						  type: 'PUT',
						  data: JSON.stringify(editCameraObject),
						  success: function(data) {
							/*newCameraNode.attr("id","IPCAMERA_"+data.id);
							tree.set_type("ip-camera",newCameraNode);
							tree.rename_node(newCameraNode,editCameraObject.name)*/
							// @TODO: Change name in tree
							tree.rename_node(startNode,editCameraObject.name)
	  						showMessageDialog("Camera updated");
						  },
						  error: function(error) {
		  					showMessageDialog("Could not update camera. Please try again or check the log files.");
						  }
						});

						$( this ).dialog( "close" );
					},
					"Cancel": function() {
						$( this ).dialog( "close" );
					}
				}
			});

		});
			
	
	// Start of functions
	
	function initCommands() {
		$("#sidebar-loader").show();
		initControllerCommands();
		cacheZwaveObjects();
	}
	function initControllerCommands() {

		$.getJSON(rootUrl+apiBase + '/Controllers'+json, function(data) {
			
			for(var i=0;i<data.length;i++) {
				var controllerObject = data[i];
				var controllerLi = jQuery("<li />", {	 "text" : controllerObject.name,
														"data-jstree":'{"type":"'+controllerObject.identifier+'"}',
														"identifier" : controllerObject.identifier
													}
										 );
				var controllerUl = jQuery("<ul />", { id : controllerObject.identifier, rel: controllerObject.identifier });
				// Put all nodes and values of camera in the tree
				
				for(var j=0;j<controllerObject.nodes.length;j++) {
					var controllerNodeObject = controllerObject.nodes[j];
					var controllerNodeLi = jQuery("<li />", {
												id : controllerObject.identifier+'-'+controllerNodeObject.identifier,
												"rel" : controllerNodeObject.style,
												"data-jstree":'{"type":"'+controllerNodeObject.type+'"}',
												"text" : controllerNodeObject.name,
												"identifier" : controllerNodeObject.identifier
											});
					controllerUl.append(controllerNodeLi );	
					
					// Add a node's values
					var nodeValuesUl = jQuery("<ul/>");
					for(var k=0;k<controllerNodeObject.values.length;k++) {
						var controllerNodeValueObject = controllerNodeObject.values[k];
						var nodeValueLi = jQuery("<li />", {
									id : controllerObject.identifier+"-"+controllerNodeObject.identifier+"-"+controllerNodeValueObject.identifier+"-",
									//id : controllerNodeValueObject.identifier,
									"rel" : controllerNodeValueObject.valueType,
									"data-jstree":'{"type":"'+controllerNodeValueObject.valueType+'"}',
									"text" : controllerNodeValueObject.name,
									"class" : 'value',
									"identifier" : controllerNodeValueObject.identifier
								})
						nodeValuesUl.append(nodeValueLi);
					}
					controllerNodeLi.append(nodeValuesUl);
				};
				
				controllerLi.append(controllerUl);
				
				$("#binding-selector-tree > ul").append(controllerLi);
			}
			initJSTree();
			$("#sidebar-loader").hide();
		});	
		
	}
	function cacheZwaveObjects() {	
	
			$.getJSON(rootUrl+apiBase + '/ZWave/Nodes'+json, function(data) {	
				window.zwaveObjects = data;
			});
			
	}
	function initJSTree() {
		$('#binding-selector-tree').jstree({
		  "core" : { 
			'check_callback': function(operation, node, node_parent, node_position, more) {
                    // operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'
                    // in case of 'rename_node' node_position is filled with the new node name

                    if (operation === "move_node") {
                        return false;
                    }
                    return true;  //allow all other operations
                }
			}, 
		  "state" : { "key" : "homedesigner_tree" },
		  "dnd" : {
					is_draggable : function (a, event) {
					  var controlDetails = getControlDetailsById(a[0].id);
					  return controlDetails.type=="VALUE";
					},
					check_while_dragging: false,
				  },
				  
		  "types" : window.typeMap,
		  "contextmenu" : {
					show_at_node: false,
					select_node: true,
					items : function (node) {
						
						// Check in which tree we are:
						var objectOrigin = getControlDetailsById(node.id);	// spotifyList, ZWaveList, zwaveValue and zwaveRoot
						window.dialogOptions.startNode = node;

						// Show menu depening on where we clicked in the tree
						if(objectOrigin.controllerIdentifier == "general") {
							if(objectOrigin.type == "NODE" && objectOrigin.nodeIdentifier == "Scenes") {
								return { 
									"Create" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Create new",
										"action"            : true,
										action : function (treeItem) { 
											openSceneBuilderDialog();
										}
									}
								};
							}

						}else if(objectOrigin.controllerIdentifier == "spotify") {
							if(objectOrigin.type == "VALUE") {
								return { 
									"test" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Test",
										"action"            : true,
										action : function (treeItem) { 
											var actionId = treeItem.reference.parent().attr("identifier");
											var testObject = new Object();
											testObject.controllerIdentifier = objectOrigin.controllerIdentifier;
											testObject.type = actionId;
											if(actionId == "PlayPause") { actionId = "Play"; };
											if(actionId == "Playlists") { actionId = "GetPlaylists"; };
											$.ajax({
											  url: "/api" + '/messagehandler/',
											  type: 'POST',
											  data: JSON.stringify(testObject),
											  success: function(data) {
							  					showMessageDialog("Test success! Result: " + JSON.stringify(data));
											  },
											  error: function(error) {
							  					showMessageDialog("Test failed! Result: " + JSON.stringify(data));
											  }
											});
										}
									}
								};
							}
						}else if(objectOrigin.controllerIdentifier == "radio") {
							if(objectOrigin.nodeIdentifier == "RadioChannels") {
								if(objectOrigin.type == "NODE") {
									return { 
										"new_channel" : {
											"separator_before"  : false,
											"separator_after"   : false,
											"label"             : "New radio channel",
											action : function (treeItem) {						
												//$("#new-radioChannel-dialog").dialog("option","startNode",treeItem);	
												//window.dialogOptions.startNode = treeItem;
												$("#new-radioChannel-dialog").dialog("open");
											}
										}
									}
								}else if(objectOrigin.type == "VALUE") {
									return { 
										"delete_channel" : {
											"separator_before"  : false,
											"separator_after"   : false,
											"label"             : "Delete",
											action : function (treeItem) {						
												$.ajax({
												  url: '/api' + '/Radio/'+objectOrigin.valueIdentifier,
												  type: 'DELETE',
												  success: function(data) {
													var tree = $.jstree.reference("#binding-selector-tree");
												  	tree.delete_node(node);
												  	
								  					showMessageDialog("Radio station deleted");
												  },
												  error: function(error) {
								  					showMessageDialog("Radio station could not be deleted: "+error);
												  }
												});
											}
										}
									}
								}
							};
						}else if(objectOrigin.controllerIdentifier == "zwave") {
							if(objectOrigin.type == "CONTROLLER") {
								return { 
									"configuration" : {
										"separator_before"  : false,
										"separator_after"   : true,
										"label"             : "Network Configuration",
										"action"            : true,
										action : function (treeItem) {
											openZWaveConfiguration();
										}
									},
									"logs" : {
										"separator_before"  : false,
										"separator_after"   : true,
										"label"             : "ZWave Logs",
										"action"            : true,
										action : function (treeItem) {
											openLogFile('ZWave');
										}
									}
								};


							}else if(objectOrigin.type == "NODE") {
								if(objectOrigin.nodeIdentifier != "General") {
								return { 
									"chooseValues" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Values",
										"action"            : true,
										action : function (treeItem) {
										
											// Get selected node from treeItem
											var identifier = treeItem.reference.parent().attr("identifier");
											var nodeId = identifier.split("_")[1].substr(1);
											var homeId = identifier.split("_")[2].substr(1);
											fillSubscriptionValuesForNode(nodeId, homeId);
											$( "#zwave-value-selector-dialog" ).dialog( "open" );
												
										}
									},"chooseAssociations" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Associations",
										"action"            : true,
										action : function (treeItem) {
										
											// Get selected node from treeItem
											var identifier = treeItem.reference.parent().attr("identifier");
											var nodeId = identifier.split("_")[1].substr(1);
											var homeId = identifier.split("_")[2].substr(1);
											$("#zwave-node-associations-dialog-nodeId").val(nodeId);
											$("#zwave-node-associations-dialog-homeId").val(homeId);
											$( "#zwave-node-associations-dialog" ).dialog( "open" );

												
										}
									},
									"rename" : {
										"separator_before"  : true,
										"separator_after"   : true,
										"label"             : "Rename",
										"action"            : true,
										action : function (treeItem) {
											$( "#zwave-value-alias-loader").hide();
											var textContent = treeItem.reference.text();
											$( "#zwave-value-alias-input" ).val(textContent);
											$( "#zwave-value-alias-dialog" ).dialog( "open" );
										}
									},
									"configuration" : {
										"separator_before"  : false,
										"separator_after"   : true,
										"label"             : "Configuration",
										"action"            : true,
										action : function (treeItem) {
										
											// Get selected node from treeItem
											var selectedNodeId = treeItem.reference.parent().attr("identifier");
											
											var nodeId = selectedNodeId.split("_")[1].substr(1);
											fillConfigurationForNode(nodeId);
											$( "#zwave-value-configuration-dialog" ).dialog( "open" );
												
										}
									},
									"removeNode" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Remove",
										"action"            : true,
										action : function (treeItem) {
										
											// Get selected node from treeItem

											var treeItemID = treeItem.reference.parent().attr("identifier");
											var nodeId = treeItemID.split("_")[1].substr(1);
											var homeId = treeItemID.split("_")[2].substr(1);

											$.ajax({
											  url: apiBase + '/ZWave/Nodes/'+homeId+"/"+nodeId,
											  type: 'DELETE',
											  success: function(data) {
											  	var tree = $.jstree.reference("#binding-selector-tree");
											  	tree.delete_node(node);
											  	showMessageDialog("Node has been deleted");
											  },
											  error: function(error) {
											  	showMessageDialog("Node could not be deleted");
											  }
											});
												
										}
									}
									}
								};
							}else if(objectOrigin.type == "VALUE") {
								return { 
									"rename" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Rename",
										"action"            : true,
										action : function (treeItem) {							
											$( "#zwave-value-alias-loader").hide();
											var textContent = treeItem.reference.text();
											$( "#zwave-value-alias-input" ).val(textContent);
											$( "#zwave-value-alias-dialog" ).dialog( "open" );
										}
									}
							};
						}
						}else if(objectOrigin.controllerIdentifier == "http") {
							if(objectOrigin.type == "CONTROLLER") {
								return { 
									"new_node" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "New group",
										action : function (treeItem) {						
											$("#new-httpnode-dialog").dialog("option","startNode",treeItem);	
											//window.dialogOptions.startNode = treeItem;	
											$("#new-httpnode-dialog").dialog("open");
										}
									}
								};
							}else if(objectOrigin.type == "NODE") {
								return { 
									"new_command" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Create HTTP Command",
										action : function (treeItem) {		
											// Show dialog with popup
											//$("#new-httpcommand-dialog").dialog("option","startNode",treeItem);
											//window.dialogOptions.startNode = treeItem;
											$("#new-httpcommand-dialog").dialog("open");
										}
									},
									"delete_node" : {
											"separator_before"  : false,
											"separator_after"   : false,
											"label"             : "Delete node",
											action : function (treeItem) {						
												$.ajax({
												  url: '/api' + '/HttpCommands/Node/'+objectOrigin.nodeIdentifier,
												  type: 'DELETE',
												  success: function(data) {
													var tree = $.jstree.reference("#binding-selector-tree");
												  	tree.delete_node(node);
												  	
								  					showMessageDialog("Node has been deleted");
												  },
												  error: function(error) {
								  					showMessageDialog("Node could not be deleted: "+error);
												  }
												});
											}
										}
									
								};
							}else if(objectOrigin.type = "VALUE") {
								return { 
									"view_change" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "View/Change command",
										"action"            : true,
										action : function (treeItem) {							
											//$("#edit-httpcommand-dialog").dialog("option","startCommand",treeItem);
											//window.dialogOptions.startCommand = treeItem;
											$("#edit-httpcommand-dialog").dialog("open");
										}
									},
									"delete_value" : {
											"separator_before"  : false,
											"separator_after"   : false,
											"label"             : "Delete Value",
											action : function (treeItem) {						
												$.ajax({
												  url: '/api' + '/HttpCommands/Command/'+objectOrigin.valueIdentifier,
												  type: 'DELETE',
												  success: function(data) {
													var tree = $.jstree.reference("#binding-selector-tree");
												  	tree.delete_node(node);
												  	
								  					showMessageDialog("Value has been deleted");
												  },
												  error: function(error) {
								  					showMessageDialog("Value could not be deleted: "+error);
												  }
												});
											}
										},
									"test_command" : {
										"separator_before"  : false,
										"separator_after"   : false,
										"label"             : "Test",
										"action"            : true,
										action : function (treeItem) {							
											// Create activateHttpCommandMessage
											var activateHttpCommandMessage = new Object();
											activateHttpCommandMessage.controlIdentifiers = new Object();
											activateHttpCommandMessage.controlIdentifiers.nodeIdentifier = objectOrigin.nodeIdentifier;
											activateHttpCommandMessage.controlIdentifiers.valueIdentifier = objectOrigin.valueIdentifier;
											activateHttpCommandMessage.controlIdentifiers.controllerIdentifier = objectOrigin.controllerIdentifier;
											activateHttpCommandMessage.type = "Activation";
											activateHttpCommandMessage.commandId = objectOrigin.valueIdentifier;
											$.ajax({
											  url: "/api" + '/messagehandler',
											  type: 'POST',
											  data: JSON.stringify(activateHttpCommandMessage),
											  success: function(data) {
							  					showMessageDialog("Test success! Result: " + JSON.stringify(data));
											  },
											  error: function(error) {
							  					showMessageDialog("Test failed! Result: " + JSON.stringify(data));
											  }
											});
										}
									},
								};
							}
						}else if(objectOrigin.controllerIdentifier == "ipcamera") {
							if(objectOrigin.type == "VALUE" ) {
								return { 
											"edit_camera" : {
												"separator_before"  : false,
												"separator_after"   : false,
												"label"             : "Edit ip camera",
												action : function (treeItem) {			
													window.dialogOptions.cameraId = treeItem.reference.parent().attr("identifier");
													$("#edit-ipcamera-dialog").dialog("open");
												}
											},
											"delete_camera" : {
											"separator_before"  : false,
											"separator_after"   : false,
											"label"             : "Delete",
											action : function (treeItem) {						
												$.ajax({
												  url: '/api' + '/IPCameras/'+objectOrigin.valueIdentifier,
												  type: 'DELETE',
												  success: function(data) {
													var tree = $.jstree.reference("#binding-selector-tree");
												  	tree.delete_node(node);
												  	
								  					showMessageDialog("Camera deleted");
												  },
												  error: function(error) {
								  					showMessageDialog("Camera could not be deleted: "+error);
												  }
												});
											}
										}
										};
							}else if(objectOrigin.type == "NODE") {
								return { 
											"new_camera" : {
												"separator_before"  : false,
												"separator_after"   : false,
												"label"             : "New ip camera",
												action : function (treeItem) {				
													//window.dialogOptions.startNode = treeItem;		
													$("#new-ipcamera-dialog").dialog("open");
												}
											}
										};
							}
						}
					}
				},	
		  "plugins" : ["types", "dnd", "state", "contextmenu", "wholerow"]
		}).fadeIn(200);
		
        $(document)
            .on('dnd_move.vakata', function (e, data) {
                var target = $(data.event.target);
				
				$(".stageItem").removeClass("highlightRed");
				$(".stageItem").removeClass("highlight");
				
				if(target.hasClass("stageItem")) {
					var imageRel = target.parents(".draggable").find("#viewDetails").attr("rel");
					var viewDetails = getViewDetailsById(imageRel);
					if(viewDetails != undefined) {
						// Check if current type is in the allowed list of the hovered image
						var controlValueRel = $(data.element).parent().attr("rel");
						if($.inArray( controlValueRel, viewDetails.allowed ) >= 0) {
							target.addClass("highlight");
							data.helper.find('.jstree-icon').removeClass('jstree-er').addClass('jstree-ok');
							return;
						}else {
							target.addClass("highlightRed");
						}
					}
				}
				data.helper.find('.jstree-icon').removeClass('jstree-ok').addClass('jstree-er');
            })
            .on('dnd_stop.vakata', function (e, data) {
                var target = $(data.event.target);
                
                if(target.hasClass("highlight")) {
					var controlDetails = getControlDetailsById($(data.element).parent().attr("id"));
					var bindingProperties = getBindingPropertiesDiv(controlDetails,null);
					var popover = target.parent().parent().children().find("#popoverContent");
					popover.find("#controlDetails").remove();
					popover.prepend(bindingProperties);
					selectObject(target.parent().parent(), true);
                }
				
                target.removeClass("highlight");
				target.removeClass("highlightRed");
            })
		$('#binding-selector-tree').bind("select_node.jstree", function(evt, data){
			if(data.event != undefined) {	
				$.jstree.reference("#binding-selector-tree").toggle_node(data.node); 	
				var icon = $(data.event.currentTarget).find("i");
				if(data.node.state.opened && icon.hasClass("ic icon-folder")) {
					icon.removeClass("ic icon-folder").addClass("ic icon-folder-open");
				}else if(icon.hasClass("ic icon-folder-open")){
					$(data.event.currentTarget).find("i").removeClass("ic icon-folder-open").addClass("ic icon-folder");
				}	
			}
        });
	}

	function testNewHttpCommand() {
		var testObject = new Object();
		testObject.controlIdentifiers = new Object();
		testObject.controlIdentifiers.controllerIdentifier = "http";
		testObject.type = "HttpCommand";
		testObject.commandName = $("#new-httpcommand-name").val();
		testObject.url = $("#new-httpcommand-url").val();
		testObject.httpMethod = $("#new-httpcommand-method").val();
		testObject.messageType = $("#new-httpcommand-msgty").val();
		testObject.messageBody = $("#new-httpcommand-msgbody").val();
		testObject.description = $("#new-httpcommand-description").val();
		testObject.additionalHeaders = {};
		var headerKey = $("#new-httpcommand-header-1-key").val();
		if(headerKey != null && headerKey != ""){ 
			testObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-1-value").val(); 
		}
		var headerKey = $("#new-httpcommand-header-2-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-2-value").val(); }
		var headerKey = $("#new-httpcommand-header-3-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-3-value").val(); }
		var headerKey = $("#new-httpcommand-header-4-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#new-httpcommand-header-4-value").val(); }

		$.ajax({
		  url: "/api" + '/HttpCommands/Test',
		  type: 'POST',
		  data: JSON.stringify(testObject),
		  success: function(data) {
			showMessageDialog("Test success! Result: " + JSON.stringify(data));
		  },
		  error: function(error) {
			showMessageDialog("Test failed! Result: " + JSON.stringify(data));
		  }
		});
	}
	function testEditHttpCommand() {
		var testObject = new Object();
		testObject.controlIdentifiers = new Object();
		testObject.controlIdentifiers.controllerIdentifier = "http";
		testObject.type = "HttpCommand";
		testObject.commandName = $("#edit-httpcommand-name").val();
		testObject.url = $("#edit-httpcommand-url").val();
		testObject.httpMethod = $("#edit-httpcommand-method").val();
		testObject.messageType = $("#edit-httpcommand-msgty").val();
		testObject.additionalHeaders = {};
		var headerKey = $("#edit-httpcommand-header-1-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-1-value").val(); }
		var headerKey = $("#edit-httpcommand-header-2-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-2-value").val(); }
		var headerKey = $("#edit-httpcommand-header-3-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-3-value").val(); }
		var headerKey = $("#edit-httpcommand-header-4-key").val();
		if(headerKey != null && headerKey != ""){ testObject.additionalHeaders[headerKey] = $("#edit-httpcommand-header-4-value").val(); }
		testObject.messageBody = $("#edit-httpcommand-msgbody").val();
		testObject.description = $("#edit-httpcommand-description").val();

		$.ajax({
		  url: "/api" + '/HttpCommands/Test',
		  type: 'POST',
		  data: JSON.stringify(testObject),
		  success: function(data) {
		  	showMessageDialog("Test success! Result: " + JSON.stringify(data));
		  },
		  error: function(error) {
		  	showMessageDialog("Test failed! Result: " + JSON.stringify(data));
		  }
		});
	}
	function initHTTPCommands() {
		var httpUl = jQuery("<ul />", {
						id : 'HttpList'
					})
		$.getJSON(rootUrl+apiBase + '/HttpCommands'+json, function(data) {
		// Put all custom http commands in the tree
		
			// Build the node catalog: Start with nodes without a parent node
			var nodes = getHttpNodesByParent(0,data.nodeStructure);
			httpUl = appendHttpNodesToParent(httpUl,nodes,data.nodeStructure);

			$("#binding-selector-tree").find("#http").append(httpUl);
			refreshTree();
		});
	}
	function getHttpNodesByParent(parentNodeId, nodeStructure) {
		var returnNodes = new Array();
		for(var i=0;i<nodeStructure.length;i++) {
			if(nodeStructure[i].parentNodeId == parentNodeId) {
				returnNodes.push(nodeStructure[i]);
			}
		}
		return returnNodes;
	}
	
	function appendHttpNodesToParent(httpUl,nodes,nodeStructure) {
		for(var i=0;i<nodes.length;i++) {
			var httpNode = nodes[i];
			var liNode = jQuery("<li />", {
										id : 'HTTPN_'+httpNode.id,
										"rel" : 'http-node'
									}).append('<a>'+ httpNode.name + '</a>');
			httpUl.append(liNode);	
			// Add a node's children
			var childNodes = getHttpNodesByParent(httpNode.id,nodeStructure);
			httpUl = appendHttpNodesToParent(httpUl,childNodes,nodeStructure);

			// Add child commands
			var childCommands = httpNode.httpCommands;
			var ulChildCommands = jQuery("<ul/>");
			for(var j=0;j<childCommands.length;j++) {
				var httpCommand = childCommands[j];
				var liCommands = jQuery("<li />", {
							id : 'HTTPC_'+httpCommand.id,
							"rel" : 'http-command'
						}).append('<a>'+ httpCommand.name + '</a>');
				ulChildCommands.append(liCommands);
			}
			liNode.append(ulChildCommands);
		};
		return httpUl;
	}
	function initIPCameraCommands() {
		var cameraUl = jQuery("<ul />", {
						id : 'ipcamera'
					})
		$.getJSON(rootUrl+apiBase + '/IPCameras'+json, function(data) {
			// Put all ip cameras in the tree
			for(var i=0;i<data.cameras.length;i++) {
				var ipCamera = data.cameras[i];
				var liNode = jQuery("<li />", {
							id : 'IPCAMERA_'+ipCamera.id,
							"rel" : 'ipcamera'
						}).append('<a>'+ ipCamera.name + '</a>');
				cameraUl.append(liNode);
			}	
			$("#binding-selector-tree").find("#ipcamera").append(cameraUl);
			refreshTree();
		});	
	}
	function openLogFile(logType) {
		window.dialogOptions.logType = logType;
		$( "#homeserver-log-dialog" ).dialog( "open" );				
	}
	function openZWaveConfiguration() {
		$( "#zwave-configuration-dialog" ).dialog( "open" );
	}
	function fillSubscriptionValuesForNode(nodeId, homeId) {
		$("#zwave-value-selector-loader").show();
		// Get all values related to the selected node	
		$.getJSON(rootUrl+apiBase + '/ZWave/Nodes/'+nodeId+json, function(data) {

			$("#zwave-value-selector-dialog-form").empty();
			$("#zwave-value-selector-dialog-header").html('Please choose which values you want to display for node <strong>'+nodeId+'</strong>');
			$("#zwave-value-selector-dialog-nodeId").val(nodeId);
			$("#zwave-value-selector-dialog-homeId").val(homeId);
			
			// Add all configuration values to the form
			
			var tableHtml = '<table>';
			tableHtml += '<tr>'+
							'<td align = "center" class="zwave-value-selector-dialog zwave-value-selector-column-checkbox">Use Value</td>'+
							'<td align = "center" class="zwave-value-selector-dialog zwave-value-selector-column-current-value">Poll Value</td>'+
							'<td class="zwave-value-selector-dialog zwave-value-selector-column-description">Description</td>'+
							'<td class="zwave-value-selector-dialog zwave-value-selector-column-current-value">Current Value</td>'+
						'</tr>';
						
			$.each(data.values, function(key, value) {	
			

				if(value.valueGenre == '0' || value.valueGenre == '1') {		// Basic, User
					var  innerHTML = '<tr>';
					var innerIDSubscribe = 'SUBSCRIBE_V'+value.valueId + '_I' + value.instance + '_N' +nodeId+"_C"+value.commandClass+"_H"+value.homeId;
					innerHTML += '<td id = "subscribe" align = "center" ><input type="checkbox" name="value" id="'+innerIDSubscribe+'" class="zwave-value-selector-dialog" title="'+value.help+'"';
					if(value.subscribed) { innerHTML += " checked "; }
					innerHTML += '/></td>';

					var innerIDPoll = 'POLL_V'+value.valueId + '_I' + value.instance + '_N' +nodeId+"_C"+value.commandClass+"_H"+value.homeId;
					innerHTML += '<td id = "poll" align = "center" class="zwave-value-selector-dialog" zwave-value-selector-column-image>';
					innerHTML += '<input type="checkbox" name="value" id="'+innerIDPoll+'" class="zwave-value-selector-dialog" title="'+value.help+'"';
					if(value.polled) { innerHTML += " checked "; }
					innerHTML += '/></td>';
					innerHTML += '</td>';

					innerHTML += '<td><label for="'+innerIDSubscribe+'" class="zwave-value-selector-dialog">'+value.valueLabel+'</label></td>';
					innerHTML += '<td class="zwave-value-selector-dialog">'+value.value+' '+value.valueUnit+"</td>";
					innerHTML += '</tr>';
					tableHtml += innerHTML;	
				}
			});
			tableHtml += '</table>';

			$("#zwave-value-selector-loader").hide();
			$("#zwave-value-selector-dialog-form").append(tableHtml);
			
		});
	}
	function fillConfigurationForNode(nodeId) {
	
		// Get all values related to the selected node	
		$.getJSON(rootUrl+apiBase + '/ZWave/Nodes/'+nodeId+json, function(data) {
		
			$("#zwave-value-configuration-loader").show();
			$("#zwave-value-configuration-dialog-form").empty();
			$("#zwave-value-configuration-dialog-header").html('This is the configuration for the node <strong>'+nodeId+'</strong>');
			$("#zwave-value-configuration-dialog-nodeId").val(nodeId);
			
			// Add all configuration values to the form
			var tableHtml = '<table>';
			var node = getZWaveObjectByNodeId(nodeId);
			if(node != undefined) {
				node.values = data.values;
			}
			$.each(data.values, function(key, value) {	
				var  innerHTML = '';
				if(value.valueGenre == '3' || value.valueGenre == '2') {		// System or config
				
					//var innerID = 'CONFIG_'+value.controlId;
					
					switch(value.valueType) {
						case(0): // Bool				
							tableHtml += '<tr>';						
							innerHTML = '<td width="50%" valign="top"><label class="zwave-value-selector-dialog" title="'+value.help+'">'+value.valueLabel+'</label><div class="valueDescription">'+value.help+'</div></td>';
							innerHTML += '<td width="50%" valign="top"><input type="checkbox" name="value" rel="'+value.controlId+'" class="zwave-value-selector-dialog-input" ';
							if(value.value == "true") { innerHTML += ' checked '; }
							if(value.readOnly) { innerHTML += ' disabled="disabled" '; }
							innerHTML += '/>';
							tableHtml += innerHTML;						
							tableHtml += '</tr>';						
						break;
						case(1): // Byte	
						case(2): // Decimal	
						case(3): // Int
						case(5): // Schedule
						case(6): // Short
						case(7): // String
							tableHtml += '<tr>';		
							innerHTML = '<td width="50%" valign="top"><label class="zwave-value-selector-dialog" title="'+value.help+'">'+value.valueLabel+'</label><div class="valueDescription">'+value.help+'</div></td>';					
							innerHTML += '<td width="50%" valign="top"><input type="inputField" name="value" rel="'+value.controlId+'" style="width:100%;" class="zwave-value-selector-dialog-input" value="'+value.value+'"';
							if(value.readOnly) { innerHTML += ' disabled="disabled" '; }							
							innerHTML +='/>';
							innerHTML += '<label class="zwave-value-selector-dialog"> '+value.valueUnit+'</label></td>';	
							innerHTML += '</td>';
							tableHtml += innerHTML;						
							tableHtml += '</tr>';						
						break;				
						case(8): // Button
							tableHtml += '<tr>';		
							innerHTML =  '<td width="50%" valign="top"><div class="valueDescription">'+value.help+'</div></td>';
							innerHTML += '<td width="50%" valign="top"><button type="button" name="value" rel="'+value.controlId+'" class="zwave-value-selector-dialog" title="'+value.help+'">'+value.valueLabel+' inst '+value.instance+''+'</button></td>';
							innerHTML += '</td>';
							tableHtml += innerHTML;						
							tableHtml += '</tr>';						
						break;
						case(4): // List
							// Display the list
							tableHtml += '<tr>';						
							innerHTML = '<td width="60%" valign="top"><label class="zwave-value-selector-dialog" title="'+value.help+'">'+value.valueLabel+'</label><div class="valueDescription">'+value.help+'</div></td>';
							innerHTML += '<td width="40%" valign="top">';
							innerHTML += '<select name="value" rel="'+value.controlId+'" class="zwave-value-selector-dialog-input" style="width:100%;"';
								if(value.readOnly) { innerHTML += ' disabled="disabled" '; }
							innerHTML += '>';
							$.each(value.valueList, function(key, valueListValue) {	
								innerHTML += '<option value="' + valueListValue +'"';
								if(valueListValue == value.valueListSelection) {
									innerHTML += ' selected="selected" ';
								}
								innerHTML += '>' + valueListValue + '</option>';
							});
							innerHTML += '</select>';
							tableHtml += innerHTML;						
							tableHtml += '</tr>';											
						break;
					}
				}
			});
			tableHtml += '</table>';

			$("#zwave-value-configuration-loader").hide();
			$("#zwave-value-configuration-dialog-form").append(tableHtml);
		});
	}

	function createAssociation() {
		// Retrieve the data to create the association
		var associationObject = new Object();
		associationObject.fromNodeId = $("#zwave-node-associations-fromNode").val();
		associationObject.homeId = $("#zwave-node-associations-dialog-homeId").val();
		associationObject.toNodeId = $("#zwave-node-associations-toNode").val();
		associationObject.associationClass = $("#zwave-node-associations-associationClass").val();
		associationObject.description = $("#zwave-node-associations-description").val();
		
		// Create the association
		$.ajax({
		  url: apiBase + '/ZWave/Nodes/'+associationObject.fromNodeId+"/Associations",
		  type: 'PUT',
		  data: JSON.stringify(associationObject),
		  success: function(data) {
			showMessageDialog("The association was created successfully. For battery powered devices it will only be changed the next time the device wakes up. ", "Association deleted successfully");
			$("#zwave-node-associations-associationClass").val("");
			$("#zwave-node-associations-description").val("");
		  },
		  error: function(error) {
  			showMessageDialog("The association could not be created. Please check the log or try again.", "Association creation failed");
		  },
		});


	}
	function resetNetwork(confirmed) {
		if(!confirmed) {
			showYesNoDialog("Are you sure you want to reset your ZWave controller? This will reset destroy your network !", 
					function() { 
						resetNetwork(true)
					}, 
					function () {
					}, 
					"Reset Controller");
		}else {
			$.ajax({
				  url: //apiBase + 
					   '/api' + '/ZWave/Nodes/Reset',
				  type: 'POST',
				  success: function(data) {
					// Show message
					$("#publish-project-loader").hide();
					$("#publish-project-status").text("Your controller was reset. Please refresh this application.");
				  },
				  error: function(error) {
					$("#publish-project-loader").hide();
					$("#publish-project-status").text("An error has occured. Please try again.");
				  }
				});
		}
	}
	
	function startInclusionMode() {
		// Send the inclusion command to the controller
		$.ajax({
		  url: //apiBase + 
			   '/api' + '/ZWave/Nodes/Include',
		  type: 'POST',
		  success: function(data) {				
			$("#zwave-transaction-dialog").data("transaction",data);
			$("#zwave-transaction-dialog").dialog("open");
		  },
		  error: function(error) {
		  }
		});
	}
	function startRemovalMode() {
		$.ajax({
		  url: //apiBase + 
			   '/api' + '/ZWave/Nodes/Remove',
		  type: 'POST',
		  success: function(data) {				
			$("#zwave-transaction-dialog").data("transaction",data);
			$("#zwave-transaction-dialog").dialog("open");
		  },
		  error: function(error) {
		  }
		});
	}
	
	function healNetwork(confirmed) {
		if(!confirmed) {
			showYesNoDialog("Are you sure you want to start healing your network?", 
					function() { 
						healNetwork(true)
					}, 
					function () {
					}, 
					"Heal Network");
		}else {
		$.ajax({
			  url: //apiBase + 
				   '/api' + '/ZWave/Nodes/Heal',
			  type: 'POST',
			  success: function(data) {
			  },
			  error: function(error) {
			  }
			});
		}
	}
	
	function deleteAssociation(homeId,fromNode,toNode,associationClass) {

		$.ajax({
		  url: '/api' + '/ZWave/Nodes/'+homeId+'/'+fromNode+"/Associations/"+toNode+"/"+associationClass,
		  type: 'DELETE',
		  success: function(data) {
			showMessageDialog("The association was deleted successfully. For battery powered devices it will only be changed the next time the device wakes up. ", "Association deleted successfully");
		  },
		  error: function(error) {
  			showMessageDialog("The association could not be deleted. Please check the log or try again.", "Association deletion failed");
		  }
		});
		
	}
