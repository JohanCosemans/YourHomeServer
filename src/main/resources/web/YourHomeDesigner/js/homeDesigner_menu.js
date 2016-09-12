	/** Menu **/

	$(document).ready(function(){
	
		$("#upload_background_file").customFile($("#upload_background_file").get(0), {
			url: '/api' + '/Images/backgroundImageSrc',
			error:	function(ev){ 
						$("#upload_background_button").text("Upload");
						$("#upload_background_button").attr("disabled",false);
					},
			success: function(data){ 
						var result = jQuery.parseJSON(data);
						setBackgroundImageSrc(result.path);
						$("#upload_background_button").text("Upload");
						$("#upload_background_button").attr("disabled",false);
					}
		});

		// Create datepicker for start date
		$("#wizard-scheduling-calendar-start").datetimepicker({ dateFormat: 'dd/mm/yy' });
		$("#wizard-scheduling-calendar-start").datetimepicker('setDate', new Date());
		// Create datepicker for end date
		//$("#wizard-scheduling-calendar-end").datetimepicker({ dateFormat: 'dd/mm/yy' });
		//var endDateDefault = new Date();
		//endDateDefault.setMonth(endDateDefault.getMonth()+1);
		//$("#wizard-scheduling-calendar-end").datetimepicker('setDate', endDateDefault);

		function resetWizard(){


				/* Step 1 */
				$("#wizard-scheduling").smartWizard('hideMessage');
				$("#wizard-scheduling [href=#step-1]").removeClass("error");

				/* Step 2 */
				$("#wizard-scheduling [href=#step-2]").removeClass("error");
				$("#wizard-scheduling-2-time").hide();
				$("#wizard-scheduling-2-zwave").hide();
				$("#wizard-scheduling-2-musicStarted").hide();
				//$("#wizard-scheduling-2-zwave-delay").val(0);

				// Default the "repeating" value on false
				$("#wizard-scheduling-repeating").val("false");
				$(".wizard-scheduling-repeating-details").fadeOut('fast');
				$("#wizard-scheduling-2-zwave-select").empty();

				/* Step 3 */
				$("#step-3 input[type=radio]").first().attr('checked', true);
				$("#wizard-scheduling [href=#step-3]").removeClass("error");

				/* Step 4 */
				$("#wizard-scheduling-4-input").val("");
				$("#wizard-scheduling [href=#step-4]").removeClass("error");

				$("#wizard-scheduling-4-zwave-select").empty();
				$("#wizard-scheduling").smartWizard("goToStep","1");
				$("#wizard-scheduling").smartWizard("disableStep","2");
				$("#wizard-scheduling").smartWizard("disableStep","3");
				$("#wizard-scheduling").smartWizard("disableStep","4");
		}
	
		$("#project-new-screen-input").val("New screen");
		$("#menu_set_grid").attr("checked","checked");
			
		$(".subMenu").each(function(key,value) {
			attach_menu(value.id);
		});

		$("#publish-project-dialog").dialog({
			autoOpen: false,
			modal: true,
			open : function() {
				$("#publish-project-loader").show();
				$("#publish-project-status").text("Publishing project ... ");
				saveProject(false);
				var toBePublished = jQuery.extend(true, {}, window.currentProject);
				toBePublished.pages.sort(byProperty("pageId","ASC"));	

				$.ajax({
				  url: //apiBase + 
					   '/api' + '/Project/Publish/'+window.currentProject.fileName.replace(".json",""),
				  type: 'POST',
				  data: JSON.stringify(window.currentProject),
				  success: function(data) {
					// Show message
					$("#publish-project-loader").hide();
					$("#publish-project-status").text("Your project has been published. Please restart your HomeController application to load your project");
				  },
				  error: function(error) {
					$("#publish-project-loader").hide();
					$("#publish-project-status").text("An error has occured. Your project has not been published");
				  }
				});
			},
			buttons: {
				"Ok" : function() {
					$( this ).dialog( "close" );
				}
			}
		});

		$("#project-save-as-dialog").dialog({
			autoOpen: false,
			modal: true,
			open : function() {
				var clearFilename = $("#project-save-as-dialog").data("clearFilename");
				var projectName = "";
				if(!clearFilename) {
					projectName = window.currentProject.fileName;						
					if(projectName != undefined) {
						projectName = projectName.replace(".json","")
					}
				}
				$("#project-save-as-input").val(projectName);
			},
			buttons: {
				"Cancel" : function() {
					$( this ).dialog( "close" );
				},
				"Save" : function() {
					$("#save-project-loader").show();
				$("#save-project-status").text("Saving project ... ");
				
				var projectPath = $("#project-save-as-input").val();	
				var currentProject = $.extend({},window.currentProject)
				currentProject.pages.sort(byProperty("index"),"ASC");
				$.ajax({
				  url: '/api' + '/Project' +'/' + projectPath,
				  type: 'POST',
				  data: JSON.stringify(currentProject),
				  success: function(data) {
					  
					var successFunction = $("#project-save-as-dialog").data("successFunction");
					
					// Show message
					$("#save-project-loader").hide();
					$("#save-project-status").text("Your project has been saved!");
					window.currentProject.fileName = data.fileName;
					$("#currentProject").text(window.currentProject.fileName.replace(".json",""));
					
					if($("#save-project-dialog").data("initialize")) {
						initialize();
					}
					$("#save-project-dialog").data("initialize", false);
					
					if(typeof(successFunction) == 'function') {
						setTimeout(successFunction,50);
					}
					$("#project-save-as-dialog").data("successFunction",null);
				  },
				  error: function(error) {
					
					$("#save-project-loader").hide();
					$("#save-project-status").text("An error has occured. Your project has not been saved.");
					$("#project-save-as-dialog").data("successFunction",null);

				  }
				});
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$("#project-save-as-dialog").data("successFunction",null);
				$( this ).dialog( "close" );
			}
		});

		$("#screen-size-dialog").dialog({
			autoOpen: false,
			/*height: '290',*/
			height: 'auto',
			width: '500',
			modal: true,
			buttons: {
				"Ok": function() {
					$("#htmlStage").width($( "#screen-size-width" ).val() * window.zoomRatio);
					$("#htmlStage").height($( "#screen-size-height" ).val() * window.zoomRatio);
					saveScreenSize();
					$("#svgObject").css("background-size", $("#htmlStage")[0].getBoundingClientRect().width+"px " + $("#htmlStage")[0].getBoundingClientRect().height+"px");

					$( this ).dialog( "close" );
				},
				"Cancel" : function() {
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$( this ).dialog( "close" );
			},
			close: function() {
			}
		});
		$("#devices-dialog").dialog({
			autoOpen: false,
			height: 'auto',
			width: '850',
			modal: true,
			open : function() {
				// devices-dialog-table
				var devicesTable = $("#devices-dialog").find("#devices-dialog-table");
				devicesTable.find("tr").each(function(no,item) {
					if(no > 0) { item.remove(); }
				});
				$.getJSON("/api/Info/Devices", function(data) {
					$.each(data,function(key,value) {
						var deviceTr = $("<tr />");
						deviceTr.append($("<td />").text(value.name));
						deviceTr.append($("<td />").text(value.height + "*" + value.width));
						deviceTr.append($("<td />").append(
							$("<a />", 
							{ "href"  : "javascript://", "class" : "ic-link" })
							.append(
								$("<i />", { "class" : "ic icon-trash-o ic-fw ic-color form-text", "title" : "Delete device" })
								.click(function() {
										$.ajax({
										  url: '/api/Info/Devices/'+value.registrationId,
										  type: 'DELETE',
										  success: function(data) {
											  deviceTr.remove();
										  },
										  error: function() {
										  }
										})
								})
								
							)
						));
						devicesTable.append(deviceTr);
					});
				});
			},
			buttons: {
				"Close" : function() {
					$( this ).dialog( "close" );
				}
			}
		});

		$("#project-save-dialog").dialog({
			autoOpen: false,
			modal: true,
			width: '500px',
			buttons: {
				"Save" : function() {
					$("#save-project-loader").show();
				$("#save-project-status").text("Saving project ... ");
				
				var projectPath = "";
				if	(window.currentProject.fileName != undefined) {
					projectPath = window.currentProject.fileName;
				}
				
				$.ajax({
					  url: '/api' + '/Project' +'/' + projectPath,
					  type: 'POST',
					  data: JSON.stringify(window.currentProject),
					  success: function(data) {
						// Show message
						$("#save-project-loader").hide();
						$("#save-project-status").text("Your project has been saved!");
						window.currentProject.fileName = data.fileName;
												if($("#save-project-dialog").data("initialize")) {
							initialize();
						}
						$("#save-project-dialog").data("initialize", false);
					  },
					  error: function(error) {
						
						$("#save-project-loader").hide();
						$("#save-project-status").text("An error has occured. Your project has not been saved.");

					  }
				});
					$( this ).dialog( "close" );
				},
				"Cancel" : function() {
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$( this ).dialog( "close" );
			},
			close: function() {
			},
			open: function() {
			}
			
		});
		$("#project-load-dialog").dialog({
			autoOpen: false,
			modal: true,
			width: '500px',
			buttons: {
				"Cancel" : function() {
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$( this ).dialog( "close" );
			},
			close: function() {
			},
			open: function() {
				$("#project-load-loader").show();
				$("#project-load-projects").empty();
				$.ajax({
					  url: '/api' + '/Project',
					  type: 'GET',
					  success: function(data) {
						  	var projects = $("#project-load-projects").hide();
						  	
							$.each(data, function(key,projectFileName){
						  		var li = jQuery("<li />");
						  		var a = jQuery("<a />", {
						  					id : projectFileName,
						  					text: projectFileName.replace(".json",""),
						  					href : "#",
						  					onClick : "importProject('"+projectFileName+"',true)",
											style: "width:150px;",
						  					});
								var aDelete = jQuery("<a />", { "href": "javascript://", title : "Delete "+projectFileName.replace(".json","")}).click(
									function() {
										showYesNoDialog("Are you sure you want to delete project "+projectFileName.replace(".json","")+"?", 
										function() { 
											$.ajax({
											  url: '/api/Project/'+projectFileName,
											  type: 'DELETE',
											  success: function(data) {
												// Remove line
												li.remove();
											  },
											  error: function() {
												showMessageDialog("The project could not be deleted.", "Project not deleted");
											  }
											});
										}, 
										function () {
										}, 
										"Delete project");
										
									}
								);
								aDelete.append(jQuery("<img/>", { "src" : "css/menu/delete.png"}));
						  		li.append(jQuery("<span />", { "class" : "dialog-row "}).append(jQuery("<span />", { "class" : "dialog-label "}).append(a)).append(aDelete));
								
						  		projects.append(li);
						  	});
							$("#project-load-loader").hide();
							$("#project-load-projects").show();
					  },
					  error: function(error) {

					  }
				});
			}
			
		});
		$("#project-publications-dialog").dialog({
			autoOpen: false,
			modal: true,
			width: '500px',
			buttons: {
				"Cancel" : function() {
					$( this ).dialog( "close" );
				}
			},
			cancel: function() {
				$( this ).dialog( "close" );
			},
			close: function() {
			},
			open: function() {
				$("#project-publications-loader").show();
				$("#project-publications").empty();
				$.ajax({
					  url: '/api' + '/Info',
					  type: 'GET',
					  success: function(data) {
						  	var projects = $("#project-publications").hide();
							$.each(data.configurations, function(key,configuration){
								
								var li = jQuery("<li />");
						  		var a = jQuery("<a />", {
						  					id : configuration.file,
						  					text: configuration.name+" (v"+configuration.version+")",
						  					href : "#",
						  					});
								var aDelete = jQuery("<a />", { "href": "javascript://", "title" : "Delete "+configuration.name}).click(
									function() {
										showYesNoDialog("Are you sure you want to unpublish project "+configuration.name+"?", 
										function() { 
											$.ajax({
											  url: '/api/Project/Published/'+configuration.file,
											  type: 'DELETE',
											  success: function(data) {
												// Remove line
												li.remove();
											  },
											  error: function() {
												showMessageDialog("The publication could not be deleted.", "Publication not deleted");
											  }
											});
										}, 
										function () {
										}, 
										"Unpublish project");
										
										
									}
								);
								aDelete.append(jQuery("<img/>", { "src" : "css/menu/delete.png"}));
						  		li.append(jQuery("<span />", { "class" : "dialog-row "}).append(jQuery("<span />", { "class" : "dialog-label "}).append(a)).append(aDelete));
								
								
								
								
								/*
								var li = jQuery("<li />");
						  		var a = jQuery("<a />", {
						  					id : configuration.file,
						  					text: configuration.name+" (v"+configuration.version+")",
						  					href : "#",
						  					});
						  		li.append(a);
								
								var aDelete = jQuery("<a />", { "href": "javascript://"}).click(
									function() {
										$.ajax({
										  url: '/api/Project/Published/'+configuration.file,
										  type: 'DELETE',
										  success: function(data) {
											// Remove line
											li.remove();
										  },
										  error: function() {
											showMessageDialog("The publication could not be deleted.", "Publication not deleted");
										  }
										});
									}
								);
								aDelete.append(jQuery("<img/>", { "src" : "css/menu/delete.png"}));
								li.append(aDelete);*/
								
							
						  		projects.append(li);
							});

							$("#project-publications-loader").hide();
							projects.show();
					  },
					  error: function(error) {

					  }
				});
			}
			
		});
		$("#stage-color-picker").minicolors( {
			change: function(hex, opacity) {
				//$("#htmlStage").css("background",hex);
				$("#svgObject").css("background-color",hex);
				$("#gridPattern line").attr("stroke",invertColor(hex));
			}
		});

		$("#svgObject").css("background",$("#stage-color-picker").val());
		$("#gridPattern line").attr("stroke",invertColor($("#stage-color-picker").val()));
		
		$("#settings-dialog").dialog({
			autoOpen: false,
			height: '600',
			width: '600',
			modal: true,
			open: function() {
				var changedSettings = {};
				$("#settings-dialog").data("changedSettings",changedSettings);
				var settingsDialogContainer = $("#settings-dialog-settings");
				settingsDialogContainer.empty();
				$.getJSON("/api/Info/Settings", function(data) {
					$.each(data.controllers, function(key, controllerData) {
						var controllerSettingsContainer = $("<div />", { "class" : "controller-container", "rel" : controllerData.controllerIdentifier }).appendTo(settingsDialogContainer).append($("<h3/>", { "text" : controllerData.controllerName }));
						$.each(controllerData.settings, function(key,settingsRow) {
							var settingsRowDiv = $("<div/>", { "class" : "controller-setting"});
							$("<label />", { "text" : settingsRow.setting.description }).appendTo(settingsRowDiv);
							$("<input />", { "type" : "text", "value": settingsRow.value, "rel" : settingsRow.setting.name, "placeholder" : settingsRow.setting.example }).appendTo(settingsRowDiv)
							.change(function(element) {

								var changedSetting = $(element.target); 
								if(changedSettings[controllerData.controllerIdentifier] == null) { changedSettings[controllerData.controllerIdentifier] = {}; }
								if(changedSettings[controllerData.controllerIdentifier][changedSetting.attr("rel")] == null) { changedSettings[controllerData.controllerIdentifier][changedSetting.attr("rel")] = {}; }

								changedSettings[controllerData.controllerIdentifier][changedSetting.attr("rel")] = changedSetting.val();
							});
							controllerSettingsContainer.append(settingsRowDiv);
						});
					});
					$("#settings-dialog").dialog("option", "position", "center"); 
				});
			},
			buttons: {
				"Cancel" : function() {
					$( this ).dialog( "close" );
				},
				"Save and restart": function() {
				var changedSettings = $("#settings-dialog").data("changedSettings");
				$.ajax({
				  url: '/api/Info/Settings',
				  type: 'PUT',
				  data: JSON.stringify(changedSettings),
				  success: function(data) {
					showMessageDialog("Settings have been saved successfully! The system is now restarting... This might take a few minutes.", "Settings saved", 
						function() { 
							 $("#settings-dialog").dialog("close"); 
						}
					);
				  },
				  error: function(error) {
					showMessageDialog("Settings could not be saved. Please check the log.", "Settings not saved");
				  }
				});
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
	$(document).mouseup(function (event) {
		// Menu: Hide all menus
		if ($(".subMenu").has(event.target).length === 0)
		{
			if( $(".ui-dialog").has(event.target).length === 0 && event.target.classList != undefined && !event.target.classList.contains("ui-widget-overlay") ) {		// Is there an open dialog window?
				hideSubMenus();
			}
		}
		if (event.target.id != "stage-color-picker" && 
			!$(event.target).parents().add(event.target).hasClass('minicolors')) {
			$("#stage-color-picker").minicolors('hide');
		}
	});
	function saveScreenSize() {
		window.currentProject.width = $( "#screen-size-width" ).val();
		window.currentProject.height = $( "#screen-size-height" ).val();
		window.currentProject.orientation = $('#screen-size-dialog input:radio[name=orientation]:checked').val();
	}
	function newProject() {
		showYesNoDialog("Would you like to save the current project?", 
					function() { 
						exportProject(true, false)
					}, 
					function () {
						initialize();
					}, 
					"Save current project");
					
					/*			buttons: {
				"Cancel" : function() {
					$( this ).dialog( "close" );
				}
			},*/

	}
	function attach_menu(menuId) {
		// Check if it is a dropdown menu
		if($("#dropdown_"+menuId).length > 0) {
			$("#"+menuId).hide();
			$("#dropdown_"+menuId).click(function() {
				if ($("#dropdown_"+menuId).hasClass("bt")) {
					$("#"+menuId).css({"position":"absolute","left":$("#dropdown_"+menuId).offset().left});
					$("#dropdown_"+menuId).removeClass("bt");
					$("#dropdown_"+menuId).addClass("clicked");
					$("#"+menuId).show();
				} else {
					hideSubMenus();
				}	
			});
		}
	}
	function hideSubMenus() {
		$(".subMenu").hide();
		$(".dropDownMenu").removeClass("clicked");
		$(".dropDownMenu").addClass("bt");
	}
	function openScreenSizePopup() {
		var width = window.currentProject.width;
		var height = window.currentProject.height;

		$( "#screen-size-width" ).val(width); //Math.round( $("#htmlStage").width()   / window.zoomRatio) );
		$( "#screen-size-height" ).val(height); //Math.round( $("#htmlStage").height() / window.zoomRatio) );
		$( "#screen-size-dialog" ).dialog( "open" );
	}
	

	function setVisibilityOfGrid() {
		if($("#grid").css('display') != "none") {;
			$("#grid").hide();
		}else {
			$("#grid").show()
		}
	}
	function deletePublication(fileName) {
	}
	function openPublicationsDialog() {
		$("#project-publications-dialog").dialog("open");
	}
	function openImportDialog() {
		$("#project-load-dialog").dialog("open");
	}
	function openSettingsDialog() {
		$("#settings-dialog").dialog("open");
	}
	
	function openDevicesDialog() {
		$("#devices-dialog").dialog("open");
	}