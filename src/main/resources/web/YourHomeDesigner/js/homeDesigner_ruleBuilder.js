/* Rule Builder */
$(document).ready(function(){
	
	/*$("#wizard-rule-builder-dialog")
	.find("#rule-builder-action").empty().append($("scene-builder-definition"));*/
	
	
	$("#wizard-rule-builder-dialog").dialog({
		autoOpen: false,
		modal: true,
		width: '1000',
		height: 'auto',
		open : function() {
			var wizardContainer = 
			$('#wizard-rule-builder-template')
			.clone()
			.attr("id","wizard-rule-builder")
			.appendTo($(this));
			
			wizardContainer.find("#rule-builder-action-container").empty().append($("#scene-builder-definition-container").clone());
							

			wizardContainer.find("#rule-builder-description-limitation-active")
			.change(function(element) {
				if(element.currentTarget.checked) {
					wizardContainer.find("#rule-builder-description-limitation-period").prop("disabled",false);
				}else{
					wizardContainer.find("#rule-builder-description-limitation-period")
					.prop("disabled",!element.currentTarget.checked)
					.val("");	
				}
			})
			.trigger("change");

			var existingRule = $("#wizard-rule-builder-dialog").data("existingRule");
			
			wizardContainer.show()
			.smartWizard(
			 {
			  // Properties
				selected: 0,  // Selected Step, 0 = first step   
				keyNavigation: false, // Enable/Disable key navigation(left and right keys are used if enabled)
				enableAllSteps: existingRule!=null,  // Enable/Disable all steps on first load
				transitionEffect: 'fade', // Effect on navigation, none/fade/slide/slideleft
				contentURL:null, // specifying content url enables ajax content loading
				contentCache:true, // cache step contents, if false content is fetched always from ajax url
				cycleSteps: false, // cycle step navigation
				enableFinishButton: false, // makes finish button enabled always
				errorSteps:[],    // array of step numbers to highlighting as error steps
				labelNext:'Next', // label for Next button
				labelPrevious:'Previous', // label for Previous button
				labelFinish:existingRule==null?'Finish':'Update',  // label for Finish button        
				noForwardJumping:true,
			  // Events
				onLeaveStep: wizardRuleBuilderNext, // triggers when leaving a step
				onShowStep: wizardRuleBuilderBefore,  // triggers when showing a step
				onFinish: wizardRuleBuilderFinish  // triggers when Finish button is clicked
			 }
			);
			
			// Fill original rule (in case of an edit)
			wizardContainer.find(".builder-loading").show();

			wizardContainer.find("#scene-builder-add-action-btn").prop("disabled",true);
			wizardContainer.find(".button").prop("disabled",true);
			$.getJSON(rootUrl+apiBase + '/Controllers/Values'+json, function(controllersData) {	
				$.getJSON(rootUrl+apiBase + '/Controllers/Triggers'+json, function(triggersData) {	
					wizardContainer.find(".builder-loading").hide();
					wizardContainer.find("#scene-builder-add-action-btn").prop("disabled",false);
					wizardContainer.find(".button").prop("disabled",false);

					if(existingRule != null) {
						wizardRuleBuilderLoad(wizardContainer, existingRule, triggersData, controllersData);
					}else {
						var firstRule = createRuleGroup(0, null,triggersData );
						wizardContainer.find(".rule-builder").append(firstRule);
					}
					wizardContainer.find("#scene-builder-add-action-btn").click(function() {	
						wizardSceneBuilderAddValue(controllersData, wizardContainer);
						$("#wizard-rule-builder-dialog").dialog("option", "position", "center"); 
					});
				});
			});
			
			$("#wizard-rule-builder-dialog").dialog("option", "position", "center"); 
			
		},
		close: function() {
			$(this).empty();
			$("#wizard-rule-builder-dialog").data("existingRule",null);
		}
	});
	
	
	//openRuleBuilderDialog();
	$("#rules-overview-dialog").dialog({
		autoOpen: false,
		modal: true,
		width: '600',
		height: '500',
		open : function() {
			fillRuleOverviewDialog();
		
			//$(this).dialog("option","height",600);
			$(this).parent().find(".ui-dialog-buttonpane button").last().focus()
		},
		close: function() {			
		},
		buttons: {
			"Close": function() {
				$( this ).dialog( "close" );
			},
			"Create new": function() {
				openRuleBuilderDialog();
			},				
		}
	});

//	$("#rules-overview-dialog").dialog("open");
});

function wizardRuleBuilderLoad(wizardContainer, ruleObject, triggersData, controllersData) {

	if(ruleObject != null) {
		/* Step 1 - Triggers */

		//$.getJSON(rootUrl+apiBase + '/Controllers/Triggers'+json, function(triggersData) {	
			wizardContainer.find(".rule-builder").append(createRuleGroup(0,ruleObject.triggers.details, triggersData));
		//});
		/* Step 2 - Definition */
		//$.getJSON(rootUrl+apiBase + '/Controllers/Values'+json, function(controllersData) {		
			$.each(ruleObject.actions, function(index,action) {
				wizardSceneBuilderAddValue(controllersData, wizardContainer, action);
			});
			
		//});
		/* Step 3 - Description */
		wizardContainer.find("#rule-builder-description-name").val(ruleObject.description.name);
		
		if(ruleObject.description.isLimited) {
			wizardContainer.find("#rule-builder-description-limitation-active")
							.prop("checked", true)
							.trigger("change");
			wizardContainer.find("#rule-builder-description-limitation-period")
							.val(ruleObject.description.period);
		}else {
			wizardContainer.find("#rule-builder-description-limitation-active").prop("checked", false);
		}
	}
}
function openRuleBuilderDialog(existingRule) {
	if(existingRule != null) {
		$("#wizard-rule-builder-dialog").data("existingRule",existingRule);
	}
	$("#wizard-rule-builder-dialog").dialog("open");
}
function wizardRuleBuilderNext(stepObj,wizardContext) {
	var stepId = stepObj.attr('href');
	
	/*if( !(wizardContext.fromStep == 4 && wizardContext.toStep == 1 )  &&
		!(wizardContext.fromStep >= wizardContext.toStep)) {
		return wizardSceneBuilderValidate(stepId);
	}else {
		return true;
	}*/
	return true;
}
function wizardRuleBuilderValidate(step) {
	var error = false;
	var warning = false;

	if(error) {
		$('#wizard-scheduling').smartWizard('setError',{stepnum:step ,iserror:true});  
		return false;
	}else if(warning) {
		$('#wizard-scheduling').smartWizard('setError',{stepnum:step ,iserror:true});  
	}
	return true;
}
function wizardRuleBuilderBefore(stepObj) {	
	var stepId = stepObj.attr('href');
	
	if(stepId == "#scene-builder-description") {
		// Initialize value lists
		//initControllerValues();
	}
}

function readRuleGroupValuesIn(container) {
	var ruleGroup = {};
	
	var idSuffix = '-'+container.attr("rel");
	// Read condition (and/or)
	ruleGroup.condition = container.find("#builder-basic-group-header-conditions-and-or"+idSuffix+" :radio:checked").val();
	ruleGroup.items = [];

	// Read rules
	container.find("#rules-list"+idSuffix +"> li").each(function(a,element) {
		var listItem = {};
		var listElement = $(element);
		
		// Check whether this is a rule or a rule group
		if(listElement.children().first().hasClass("rule-group-container-main")) {
			// This is a group
			listItem.type = "group";
			listItem.details = readRuleGroupValuesIn(listElement.children().first());
		}else {
			// This is a trigger
			listItem.type = "trigger";
			var trigger = {};

			trigger.controllerIdentifier = listElement.find("#rules-basic-rule-conditions-controllers"+idSuffix).val();	
			if(trigger.controllerIdentifier != "") {
				trigger.nodeIdentifier = listElement.find("#rules-basic-rule-conditions-nodes"+idSuffix).val();		
				
				var valueSelect = listElement.find("#rules-basic-rule-conditions-values"+idSuffix);
				var valueObject = valueSelect.data("valueObject");
				trigger.valueIdentifier = valueSelect.val();
				listItem.valueType = valueObject.valueType;
			
				var valueDetailsDiv = listElement.find("#rules-basic-rule-conditions-details"+idSuffix);
				trigger.details = getValueFromValueSelector(valueObject,valueDetailsDiv.children().first(),idSuffix, true);
				
				listItem.details = trigger;
			}			
		}
		ruleGroup.items.push(listItem);
	});
	
	return ruleGroup;
}
function wizardRuleBuilderFinish(stepObjects) {
	var wizardContainer = $(stepObjects.context);
	var ruleObject = {};
	/* Step 3 - Description */
	ruleObject.description = {};
	ruleObject.description.name = wizardContainer.find("#rule-builder-description-name").val();
	ruleObject.description.isLimited = wizardContainer.find("#rule-builder-description-limitation-active").prop('checked');
	if(ruleObject.description.isLimited) {
		ruleObject.description.period = wizardContainer.find("#rule-builder-description-limitation-period").val();
	}
	
	/* Step 1 - Triggers */
	ruleObject.triggers = {
							"type" : "group",
							 "details" : readRuleGroupValuesIn(wizardContainer.find("#rule-builder").children().first())
							};
	
	/* Step 2 - Actions */
	ruleObject.actions = [];
	var i=0;
	wizardContainer.find(".screenSortableList li").each(function(a,element) {
		var actionElement = $(element);
		var idSuffix = "-"+actionElement.attr("rel");
		var actionObject = {};
		actionObject.sequence = i++;
		actionObject.controllerIdentifier = actionElement.find("#scene-builder-definition-controllers"+idSuffix).val();		
		actionObject.nodeIdentifier = actionElement.find("#scene-builder-definition-nodes"+idSuffix).val();		
		
		var valueSelect = actionElement.find("#scene-builder-definition-values"+idSuffix);
		var valueObject = valueSelect.data("valueObject");
		actionObject.valueIdentifier = valueSelect.val();
		actionObject.valueType = valueObject.valueType;
		
		var valueDetailsDiv = actionElement.find("#scene-builder-definition-value-details"+idSuffix);
		actionObject.details = getValueFromValueSelector(valueObject,valueDetailsDiv.children().first(),idSuffix,false);
		ruleObject.actions.push(actionObject);
	});
	
	//console.log(ruleObject);
	
	// Are we doing an update or an insert?
	var sourceRule = $("#wizard-rule-builder-dialog").data("existingRule");
	
	if(sourceRule == null) {			
		$.ajax({
		  url: '/api' + '/Rules',
		  type: 'POST',
		  data: JSON.stringify(ruleObject),
		  success: function(data) {
			showMessageDialog("Rule has been created successfully!", "Rule created", 
				function() { 
					fillRuleOverviewDialog()
					wizardContainer.parent().dialog("close"); 
				}
			);
		  },
		  error: function(error) {
			showMessageDialog("Rule could not be created.", "Rule create error");
		  }
		});
	}else {
		$.ajax({
		  url: '/api' + '/Rules/'+sourceRule.id,
		  type: 'PUT',
		  data: JSON.stringify(ruleObject),
		  success: function(data) {
			showMessageDialog("Rule has been updated successfully!", "Rule updated", 
				function() { 
					$("#wizard-rule-builder-dialog").data("existingRule", null);
					fillRuleOverviewDialog()
					wizardContainer.parent().dialog("close"); 
				}
			);
		  },
		  error: function(error) {
			showMessageDialog("Rule could not be updated.", "Rule update error");
		  }
		});
	}
	
}
function createRuleGroup(suffixNumber, fromObject, triggersData){
	var idSuffix = '-'+suffixNumber;
	var ruleGroupsBelow = 0;
	var basicGroupMainDiv = $("<div />", { "id" : "builder-basic-group-main"+idSuffix, "class" : "rule-group-container-main", "rel" : suffixNumber} );
	var basicGroupDiv = $("<div />", { "id" : "builder-basic-group"+idSuffix, "class" : "rule-group-container"} ).appendTo(basicGroupMainDiv);
	
	// Body
	var basicGroupBodyDiv = $("<div />", { "class" : "rule-group-body"} );
	var rulesListUl = $("<ul />", { "id" : "rules-list" +idSuffix, "class" : "rules-list"} ).appendTo(basicGroupBodyDiv);
	
	// Header
	var basicGroupHeaderDiv = $("<div />", { "class" : "rule-group-header"} );
	
		// Conditions
		var basicGroupHeaderConditionsDiv = $("<div />", { "id" : "builder-basic-group-header-conditions"+idSuffix, "class" : "rule-group-header-and-or"} );
		var basicGroupHeaderConditionsAndOrDiv = $("<div />", { "id" : "builder-basic-group-header-conditions-and-or"+idSuffix, "class" : "rules-and-or"} );

		basicGroupHeaderConditionsAndOrDiv.append($("<input />", { "value" : "AND", "type" : "radio", "id" : "builder-basic-group-condition-and"+idSuffix, "name" : "builder-basic-group-condition"+idSuffix}));
		basicGroupHeaderConditionsAndOrDiv.append($("<label />", { "text" : "AND", "for" : "builder-basic-group-condition-and"+idSuffix })); 
		basicGroupHeaderConditionsAndOrDiv.append($("<input />", { "value" : "OR", "type" : "radio", "id" : "builder-basic-group-condition-or"+idSuffix, "name" : "builder-basic-group-condition"+idSuffix })); 
		basicGroupHeaderConditionsAndOrDiv.append($("<label />", { "text" : "OR", "for" : "builder-basic-group-condition-or"+idSuffix })); 

		if(fromObject == null) { 
			basicGroupHeaderConditionsAndOrDiv
			.find('input:radio[name="builder-basic-group-condition'+idSuffix+'"]')
			.filter('[value="AND"]')
			.prop('checked', true);
			basicGroupHeaderConditionsAndOrDiv.buttonset(); 
		}
		
		// Action buttons		
		var basicGroupHeaderActionsDiv = $("<div />", { "id" : "builder-basic-group-header-actions"+idSuffix, "class" : "rule-group-header-actions"} );
		var addButton = $("<button />", { "autofocus":"autofocus", "class" : "button button-with-ic"});
		basicGroupHeaderActionsDiv.append(  addButton
											.append($("<i />", { "class" : "ic icon-plus-circle ic-color ic-in-button"}))
											.append($("<span />", { "text" : "Rule" }))
											.click(function() {
												// Add rule to body
												rulesListUl.append(createRule(idSuffix, null, triggersData));
											})
										);
		basicGroupHeaderActionsDiv.append( $("<button />", { "class" : "button button-with-ic"}) 
											.append($("<i />", { "class" : "ic icon-plus-circle ic-color ic-in-button"}))
											.append($("<span />", { "text" : "Group" }))
											.click(function() {
												// Add new group to body
												var ruleLi = $("<li />", { "class" : "rule", "id" : "rules-basic-rule"+idSuffix});
												rulesListUl.append(ruleLi.append(createRuleGroup(++suffixNumber + ruleGroupsBelow++, null, triggersData)));
											})
										);
		if(suffixNumber > 0) {
			basicGroupHeaderActionsDiv.append( $("<button />", { "class" : "button button-with-ic"}) 
											.append($("<i />", { "class" : "ic icon-close ic-color ic-in-button"}))
											.append($("<span />", { "text" : "Delete" }))
											.click(function() {
												//if(basicGroupMainDiv.parent().prop("tagName") == "LI") {

												if(basicGroupMainDiv.parent().prop("tagName") == "LI"
													&& basicGroupMainDiv.parent().parent().children().length > 1) {
													basicGroupMainDiv.parent().remove();
												}
											})
										);
		}
	basicGroupHeaderDiv.append(basicGroupHeaderConditionsDiv.append(basicGroupHeaderConditionsAndOrDiv));
	basicGroupHeaderDiv.append(basicGroupHeaderActionsDiv);
	
	
	basicGroupDiv.append(basicGroupHeaderDiv);
	basicGroupDiv.append(basicGroupBodyDiv);
	

	if(fromObject != null) {
		// Set condition
		//basicGroupHeaderConditionsAndOrDiv.find('input:radio[name="builder-basic-group-condition'+idSuffix+'"]').filter('[value!="'+fromObject.condition+'"]').removeAttr("checked");

		basicGroupHeaderConditionsAndOrDiv
		.find('input:radio[name="builder-basic-group-condition'+idSuffix+'"]')
		.filter('[value="'+fromObject.condition+'"]')
		.prop('checked', true);
		basicGroupHeaderConditionsAndOrDiv.buttonset();
		
		// Loop on elements
		$.each(fromObject.items,function(key,item) {
			switch(item.type) {
				case "trigger":
					// Add rule to body
					rulesListUl.append(createRule(idSuffix, item.details, triggersData));
				break;
				case "group":
					var ruleLi = $("<li />", { "class" : "rule", "id" : "rules-basic-rule"+idSuffix});
					rulesListUl.append(ruleLi.append(createRuleGroup(++suffixNumber + ruleGroupsBelow++, item.details, triggersData)));
				break;
			}
		});
		

	}else {
		addButton.trigger("click");
	}


	return basicGroupMainDiv;
}
function createRule(idSuffix, fromObject, triggersData) {
	var ruleLi = $("<li />", { });
	var ruleDiv = $("<div />", {"class" : "rule", "id" : "rules-basic-rule"+idSuffix }).appendTo(ruleLi);
	
	var ruleHeaderDiv = $("<div />", { "class" : "rule-header" }).appendTo(ruleDiv);
	ruleHeaderDiv.append( $("<a />", { "href" : "javascript://", "class": "ic-link"}) 
						.append($("<i />", { "class" : "ic icon-trash-o ic-color ic-in-button"}))
						.click(function() {
							if(ruleLi.parent().children().length > 1) {
								ruleLi.remove();
							}
						})
					);
											
	var ruleConditionsDiv = $("<div />", { "class" : "rule-conditions", "id" : "rules-basic-rule-conditions"+idSuffix}).appendTo(ruleDiv);
	
	var triggerDropdowns = createControllerTriggerDropdowns(idSuffix,fromObject, triggersData);
	ruleConditionsDiv.append(triggerDropdowns);
	
	return ruleLi;
}

function fillControllerTriggerDropdowns(idSuffix, triggersData, actionObject, allSpan,controllerSelect,nodeSelect,valueSelect,valueDetailsDiv, triggersData) {
		// Fill selects
		controllerSelect.change(function(event) {
			// Change value list for nodes
			var selectedController = event.currentTarget.value;
			if(selectedController == "") {
				nodeSelect.hide();
				valueSelect.hide();
				valueDetailsDiv.hide();
			}else {
				nodeSelect.fadeIn("fast");
				valueSelect.fadeIn("fast");
				valueDetailsDiv.fadeIn("fast");
				
				var controllerObject = findByAttribute(triggersData, "identifier", selectedController)
				nodeSelect.empty();
				for(var i=0;i<controllerObject.nodes.length;i++) {
					var nodeObject = controllerObject.nodes[i];
					var option = $("<option />", {
													"value" : nodeObject.identifier,
													"text" : nodeObject.name
												});
					nodeSelect.append(option);
				}
				if(actionObject != null) {
					nodeSelect.val(actionObject.nodeIdentifier);
					nodeSelect.trigger("change");
				}else if(controllerObject.nodes.length > 0) {
					nodeSelect.val(controllerObject.nodes[0].identifier)
					nodeSelect.trigger("change");
				}
			}
		});		
		nodeSelect.change(function(event) {
			// Change value list for values
			var selectedController = controllerSelect.val();
			var selectedNode = nodeSelect.val();
			
			var controllerObject = findByAttribute(triggersData, "identifier", selectedController);
			var nodeObject = findByAttribute(controllerObject.nodes, "identifier", selectedNode);
			
			if(nodeObject != null) {
				valueSelect.empty();
				for(var i=0;i<nodeObject.values.length;i++) {
					var valueObject = nodeObject.values[i];
					var valueObject = nodeObject.values[i];
					var option = $("<option />", {
													"value" : valueObject.identifier,
													"text" : valueObject.name
												});
					valueSelect.append(option);
				}
			}
			if(actionObject != null) {
				valueSelect.val(actionObject.valueIdentifier);
				valueSelect.trigger("change");
			}else if(nodeObject.values.length > 0) {
				var newValue = nodeObject.values[0];
				valueSelect.val(newValue.identifier);
				valueSelect.trigger("change");
			}
		});
		valueSelect.change(function(event) {
			// Update value selector
			var selectedController = controllerSelect.val();
			var selectedNode = nodeSelect.val();
			var selectedValue = valueSelect.val();
			
			var controllerObject = findByAttribute(triggersData, "identifier", selectedController);
			if(controllerObject != null) {
			var nodeObject = findByAttribute(controllerObject.nodes, "identifier", selectedNode);
			if(nodeObject != null) {
				var valueObject = findByAttribute(nodeObject.values, "identifier", selectedValue);
				var valueSelector = getValueSelectorByType(valueObject,$("<span/>"),idSuffix,actionObject,true);
				valueDetailsDiv.empty();
				valueDetailsDiv.append(valueSelector);
				valueSelect.data("valueObject",valueObject)
			}
			}
		});
		
		controllerSelect.append($("<option />", {"value" : "",	"text" : "-- Controller --" }));
		for(var i=0;i<triggersData.length;i++) {
			var controllerObject = triggersData[i];
			var option = $("<option />", {
											"value" : controllerObject.identifier,
											"text" : controllerObject.name
										});
			controllerSelect.append(option);
		}
		if(actionObject != null) {
			controllerSelect.val(actionObject.controllerIdentifier);
			controllerSelect.trigger("change");
		}else if(triggersData.length > 0) {
			//controllerSelect.val(triggersData[0].identifier)
			controllerSelect.val("");
			controllerSelect.trigger("change");
		}
		actionObject = null;
		return allSpan;
}
function createControllerTriggerDropdowns(idSuffix, actionObject, triggersData) {
	
	// Get data
	//var triggersData = $("#wizard-rule-builder").data("triggersData");
	var allSpan = $("<span />", { "id" : "rules-basic-rule-conditions-group"+idSuffix });
	var controllerSelect = $("<select />", { "id" : "rules-basic-rule-conditions-controllers"+idSuffix}).appendTo(allSpan);
	var nodeSelect = $("<select />", { "id" : "rules-basic-rule-conditions-nodes"+idSuffix}).appendTo(allSpan);
	var valueSelect = $("<select />", { "id" : "rules-basic-rule-conditions-values"+idSuffix}).appendTo(allSpan);
	var valueDetailsDiv = $("<span />", { "id" : "rules-basic-rule-conditions-details"+idSuffix}).appendTo(allSpan);

	/*if(triggersData == null) {
		$.getJSON(rootUrl+apiBase + '/Controllers/Triggers'+json, function(data) {	
			$("#wizard-rule-builder").data("triggersData",data);
			fillControllerTriggerDropdowns(idSuffix,data,actionObject, allSpan, controllerSelect, nodeSelect, valueSelect, valueDetailsDiv);
		});
	}else {*/
		fillControllerTriggerDropdowns(idSuffix, triggersData,actionObject, allSpan, controllerSelect, nodeSelect, valueSelect, valueDetailsDiv, triggersData);
	//}
	return allSpan;
}
function fillRuleOverviewDialog() {
	$("#rules-overview").find(">div").each(function(a,element) { $(element).remove(); });
	var initialized = false;
	$.getJSON(rootUrl+apiBase + '/Rules'+json, function(data) {
		$.each(data,function(index,ruleObject) {
			initialized = true;
			var rowDiv = $("<div />");
			rowDiv.append($("<div />", {"class" : "labels form-text"}).append($("<label/>").append(ruleObject.description.name)));
			var enableDisableBtn = 	$("<i />", { "class" : "ic ic-fw ic-color form-text" });
			if(ruleObject.active) {
				enableDisableBtn.addClass("icon-pause-circle");
				enableDisableBtn.attr("title" , "Disable this rule");
			}else {
				enableDisableBtn.addClass("icon-play-circle");
				enableDisableBtn.attr("title" , "Enable this rule");
			}
			enableDisableBtn.click(function() {
								var status = false;
								var icon = $(this);
								if(!icon.hasClass("icon-pause-circle")) {
									status = true;
								}
								$.ajax({
								  url: '/api' + '/Rules/'+ruleObject.id+"/"+status,
								  type: 'PUT',
								  success: function(data) {
									icon.toggleClass("icon-play-circle icon-pause-circle");
									if(status) {
										icon.attr("title", "Disable this rule");
									}else {
										icon.attr("title", "Enable this rule");
									}
								  },
								  error: function(error) {
									showMessageDialog("Rule could not be updated.", "Rule update error");
								  }
								});
							});
			rowDiv.append(
				$("<div/>", { "class" : "ic-group" })
				.append(
					$("<a />", { "href" : "javascript://", "class" : "ic-link" })
					.append(enableDisableBtn)
				)
				.append(
					$("<a />", { "href" : "javascript://", "class" : "ic-link" })
					.append(
						$("<i />", { "class" : "ic icon-pencil2 ic-fw ic-color form-text", "title" : "Edit this rule" })
							.click(function() {
								openRuleBuilderDialog(ruleObject);
							})
						)
				)
				.append(
					$("<a />", { "href" : "javascript://", "class" : "ic-link" })
					.append(
						$("<i />", { "class" : "ic icon-trash-o ic-fw ic-color form-text", "title" : "Delete this rule" })
							.click(function() {
								
								showYesNoDialog("Are you sure you want to delete rule "+ruleObject.description.name+"?", 
									function() { 
										$.ajax({
										  url: '/api/Rules/'+ruleObject.id,
										  type: 'DELETE',
										  success: function(data) {
											if(rowDiv.parent().find(">div").length == 1) {
												$("#rules-overview").find(".norules").show();
											}
											// Remove line
											rowDiv.remove();
										  },
										  error: function() {
											showMessageDialog("The rule could not be deleted.", "Rule not deleted");
										  }
										});
									}, 
									function () {
									}, 
									"Delete Rule"
								);
							})
						)
				)
				);
			$("#rules-overview").append(rowDiv);
		});
		if(!initialized) { 
			$("#rules-overview").find(".norules").show();
		}else {
			$("#rules-overview").find(".norules").hide();
		}
		$("#rules-overview-dialog").dialog("option", "position", "center"); 
	});
}

/* Scene builder */
$(document).ready(function(){

	$("#wizard-scene-builder-dialog").dialog({
		autoOpen: false,
		modal: true,
		width: '850',
		height: 'auto',
		open : function() {
			var wizardContainer = 
			$('#wizard-scene-builder-template')
			.clone()
			.attr("id","wizard-scene-builder")
			.appendTo($(this));

			var sourceScene = $("#wizard-scene-builder-dialog").data("existingScene");
	
			wizardContainer.show()
			.smartWizard(
			 {
			  // Properties
				selected: 0,  // Selected Step, 0 = first step   
				keyNavigation: false, // Enable/Disable key navigation(left and right keys are used if enabled)
				enableAllSteps: sourceScene!=null,  // Enable/Disable all steps on first load
				transitionEffect: 'fade', // Effect on navigation, none/fade/slide/slideleft
				contentURL:null, // specifying content url enables ajax content loading
				contentCache:true, // cache step contents, if false content is fetched always from ajax url
				cycleSteps: false, // cycle step navigation
				enableFinishButton: false, // makes finish button enabled always
				errorSteps:[],    // array of step numbers to highlighting as error steps
				labelNext:'Next', // label for Next button
				labelPrevious:'Previous', // label for Previous button
				labelFinish: sourceScene==null?'Finish':'Update',  // label for Finish button        
				noForwardJumping:true,
			  // Events
				onLeaveStep: wizardSceneBuilderNext, // triggers when leaving a step
				onShowStep: wizardSceneBuilderBefore,  // triggers when showing a step
				onFinish: wizardSceneBuilderFinish  // triggers when Finish button is clicked
			 }
			);
			
			// Fill original scene (in case of an edit)

			wizardContainer.find(".builder-loading").show();
			wizardContainer.find("#scene-builder-add-action-btn").prop("disabled",true);
			$.getJSON(rootUrl+apiBase + '/Controllers/Values'+json, function(controllersData) {
				wizardContainer.find(".builder-loading").hide();
				wizardContainer.find("#scene-builder-add-action-btn").prop("disabled",false);
				wizardSceneBuilderLoad(wizardContainer, sourceScene, controllersData);
				wizardContainer.find("#scene-builder-add-action-btn").click(function() {
					wizardSceneBuilderAddValue(controllersData, wizardContainer);
				});
				$("#wizard-scene-builder-dialog").dialog("option", "position", "center"); 
			});
		},
		close: function() {
			$(this).empty();
			$("#wizard-scene-builder-dialog").data("existingScene",null);
		}
	});
	$("#scenes-overview-dialog").dialog({
		autoOpen: false,
		modal: true,
		width: '600',
		height: '500',
		open : function() {
			fillSceneOverviewDialog();
		
			$(this).parent().find(".ui-dialog-buttonpane button").last().focus()
			//$(this).dialog("option","height",600);
		},
		close: function() {			
		},
		buttons: {
			"Close": function() {
				$( this ).dialog( "close" );
			},
			"Create new": function() {
				openSceneBuilderDialog();
			},				
		}
	});

	//openSceneBuilderDialog()
	//openScenesOverviewDialog()
});
function fillSceneOverviewDialog() {
	
	$("#scenes-overview").find(">div").each(function(a,element) { 
		$(element).remove(); 
	});

	var initialized = false;
	$.getJSON(rootUrl+apiBase + '/Scenes'+json, function(data) {
		$.each(data,function(index,sceneObject) {
			initialized = true;
			var rowDiv = $("<div />");
			rowDiv.append($("<div />", {"class" : "labels form-text"}).append($("<label/>").append(sceneObject.description.name)));
			rowDiv.append(
				$("<div/>", { "class" : "ic-group" })
				.append(
					$("<a />", { "href" : "javascript://", "class" : "ic-link" })
					.append(
						$("<i />", { "class" : "ic icon-check ic-fw ic-color form-text", "title" : "Activate this scene" })
							.click(function() {
								$.getJSON(rootUrl+apiBase + '/Scenes/Activate/'+sceneObject.id+json, function(data) {
									showMessageDialog("Scene \""+sceneObject.description.name+"\" activated", "Scene activated");
								});
							})
						)
				)
				.append(
					$("<a />", { "href" : "javascript://", "class" : "ic-link" })
					.append(
						$("<i />", { "class" : "ic icon-pencil2 ic-fw ic-color form-text", "title" : "Edit this scene" })
							.click(function() {
								openSceneBuilderDialog(sceneObject);
							})
						)
				)
				.append(
					$("<a />", { "href" : "javascript://", "class" : "ic-link" })
					.append(
						$("<i />", { "class" : "ic icon-trash-o ic-fw ic-color form-text", "title" : "Delete this scene" })
							.click(function() {
								
								showYesNoDialog("Are you sure you want to delete scene "+sceneObject.name+"?", 
									function() { 
										$.ajax({
										  url: '/api/Scenes/'+sceneObject.id,
										  type: 'DELETE',
										  success: function(data) {
											  if(rowDiv.parent().find(">div").length == 1) {
												$("#scenes-overview").find(".noscenes").show();
											}
											// Remove line
											rowDiv.remove();
											
											// Remove tree item
											var tree = $.jstree.reference("#binding-selector-tree");
											var startNode = tree.get_node("general-Scenes-"+sceneObject.id+"-");
											tree.delete_node(startNode);
										  },
										  error: function() {
											showMessageDialog("The scene could not be deleted.", "Scene not deleted");
										  }
										});
									}, 
									function () {
									}, 
									"Delete Scene"
								);
							})
						)
				)
				);
			$("#scenes-overview").append(rowDiv);
		});
		
		if(!initialized) { 
			$("#scenes-overview").find(".noscenes").show();
		}else {
			$("#scenes-overview").find(".noscenes").hide();
		}
		$("#scenes-overview-dialog").dialog("option", "position", "center"); 
	});
}
function getValueSelectorByType(valueObject, beforeDescriptionElement, idSuffix, actionObject, forTrigger) {
	switch(valueObject.valueType) {
		case "color_bulb" : 
			var colorPicker = $("<input />", {
														"type": "text",
														"class": "minicolors minicolors-input",
														"size": "7",
														"maxlength": "7"
														//"value" : (actionObject!=null&&actionObject.details!=null&&actionObject.details.value!=null)?actionObject.details.value:"#ffc400"
											});
			//beforeDescriptionElement.append(colorPicker);
			colorPicker.minicolors( {	control: 'brightness',
										defaultValue: (actionObject!=null&&actionObject.details!=null&&actionObject.details.value!=null)?actionObject.details.value:"#ffc400" });
			
			beforeDescriptionElement.text("Set ");	
			return $("<span />",{ "text" : " to " }).append(colorPicker.parent());
		break;
		case "period" :
			var jqCron = $("<span />", { "class" : "rule-conditions-time" }).jqCron( { 
				enabled_minute: true,
				multiple_dom: true,
				multiple_month: true,
				multiple_mins: true,
				multiple_dow: true,
				multiple_time_hours: true,
				multiple_time_minutes: true,
				lang: 'en',
				default_value : '00 12 * * *'
			});
			if(actionObject != null) {
				jqCron.jqCronGetInstance().setCron(actionObject.details.value);
			}
			return jqCron;
		break;
		case "switch_binary":
			// Add on/off selector
			var selectOnOff = jQuery("<select />", { "id" : "value-on-off"+idSuffix});
			selectOnOff.append(selectOnOff).append('<option value="true">On</option>')
										   .append('<option value="false">Off</option>');	

			/*if(disabled) {
				selectOnOff.attr("disabled","disabled");
			}*/
			if(actionObject != undefined) {
				selectOnOff.val(actionObject.details.value);
			}
			if(forTrigger) {
				var isBecomesSelect = $('<select id="value-is-becomes'+idSuffix+'"> 		 	\
							<option value="BECOMES" selected="">becomes</option>			 	\
							<option value="IS">is</option> 	\
					</select>');
				if(actionObject != undefined) {
					isBecomesSelect.val(actionObject.details.isBecomes);
				}
				return $("<span />",{ "text" : "equal to " }).prepend(isBecomesSelect).append(selectOnOff);
			}else {
				beforeDescriptionElement.text("Set ");	
				return $("<span />",{ "text" : " to " }).append(selectOnOff);
			}
		break;
		case "dimmer":
			// Add slider
			var value = 99;
			if(actionObject != undefined) {
				value = actionObject.details.value;
			}
			var spanValue = jQuery("<span />", { "class" : "value"}).text(value+"%");
			var resultDiv = jQuery("<span />", {"style":"vertical-align: middle; line-height:30px"});
			spanValue.attr("style","margin-left: 10px;");
			var divSelector = jQuery("<span />").slider({ 
														value : value,
														max:99,
														change: function( event, ui ) {
															spanValue.text(ui.value+"%");
														}
														});
			divSelector.attr("style","width: 200px; display: inline-block; margin-left: 10px;");
			resultDiv.append(divSelector);
			resultDiv.append(spanValue);
			/*if(disabled) {
				divSelector.slider("disable");
			}*/
			if(forTrigger) {
				var isBecomesSelect = $('<select id="value-is-becomes'+idSuffix+'"> 		 	\
							<option value="BECOMES" selected="">becomes</option>			 	\
							<option value="IS">is</option> 	\
					</select>');
				var operandSelect = $('<select id="value-operand'+idSuffix+'"> 		 	\
											<option value="EQ" selected="">equal to</option> 	\
											<option value="G">greater than</option>			 	\
											<option value="GE">greater or equal to</option>	\
											<option value="L">lower than</option>				\
											<option value="LE">lower or equal to</option>	 	\
									</select>');
				if(actionObject != undefined) {
					isBecomesSelect.val(actionObject.details.isBecomes);
					operandSelect.val(actionObject.details.operand);
				}	
									
				return $("<span />").append(isBecomesSelect).append(operandSelect).append(resultDiv);
			}else {
				beforeDescriptionElement.text("Set ");	
				return $("<span />",{ "text" : " to " }).append(resultDiv);
			}
			
		break;
		case "heating":
		case "meter":
		case "sensor_temperature":
		case "sensor_humidity":
		case "sensor_luminosity":
		case "sensor_general":
		case "sensor_alarm":
			// Inputfield with numeric values
			var inputObject = jQuery("<input />", {
				"class":"numbersOnly",
				"style":"width:70px;"
			});
			if(actionObject != undefined) {
				inputObject.val(actionObject.details.value);
			}
			/*if(disabled) {
				inputObject.attr("disabled","disabled");
			}*/
			if(forTrigger) {
				
				var isBecomesSelect = $('<select id="value-is-becomes'+idSuffix+'"> 		 	\
							<option value="BECOMES" selected="">becomes</option>			 	\
							<option value="IS">is</option> 	\
					</select>');
				var operandSelect = $('<select id="value-operand'+idSuffix+'"> 		 	\
											<option value="EQ" selected="">equal to</option> 	\
											<option value="G">greater than</option>			 	\
											<option value="GE">greater or equal to</option>	\
											<option value="L">lower than</option>				\
											<option value="LE">lower or equal to</option>	 	\
									</select>');
				if(actionObject != undefined) {
					isBecomesSelect.val(actionObject.details.isBecomes);
					operandSelect.val(actionObject.details.operand);
				}	
				var wrappedInput = $("<span />").append(isBecomesSelect).append(operandSelect).append(inputObject);
				inputObject.spinner({ step: 0.50 });
				return wrappedInput;
			}else {		
				beforeDescriptionElement.text("Set ");
				var wrappedInput = $("<span />", { "text" : " to "}).append(inputObject);
				inputObject.spinner({ step: 0.50 });
				return wrappedInput;
			}
		break;
		
		case "wait":
			// Inputfield with numeric values
			var inputObject = jQuery("<input />", {
				"class":"numbersOnly",
				"style":"width:70px;"
			});
			/*if(disabled) {
				inputObject.attr("disabled","disabled");
			}*/
			if(actionObject != undefined) {
				inputObject.val(actionObject.details.value);
			}
			beforeDescriptionElement.text("Activate ");
			return $("<span />", { "text" : " seconds"}).prepend(inputObject);
		break;
		
		case "send_notification":
			var notificationContent = $("#scene-builder-definition-notification").clone().removeAttr("id").show();
			addSuffix(notificationContent,"scene-builder-definition", idSuffix);

		
			var type = notificationContent.find("#scene-builder-definition-notification-type"+idSuffix);
			var phoneNumber = type.parent().find("#scene-builder-definition-notification-phoneNumber-tr"+idSuffix);
			var email = type.parent().find("#scene-builder-definition-notification-email-tr"+idSuffix);
			var subject = type.parent().find("#scene-builder-definition-notification-subject-tr"+idSuffix);
			var message = type.parent().find("#scene-builder-definition-notification-message-tr"+idSuffix);
			var camera = type.parent().find("#scene-builder-definition-notification-camera-tr"+idSuffix);
			var cameraSelect = camera.find("select");
			cameraSelect.change(function() {
				if($(this).val() != null && $(this).val() != "") {
					message.hide();
				}else {
					message.show();
				}
			});
			type.change(function() {
				switch($(this).val()) {
					case "mobile":
						phoneNumber.hide();
						email.hide();
						subject.show();
						message.show();
						camera.show();
					break;
					case "sms":
						phoneNumber.show();
						email.hide();
						subject.hide();
						message.show();
						camera.hide();
					break;
					case "email":
						phoneNumber.hide();
						email.show();
						subject.show();
						message.show();
						camera.hide();
					break;
				}
			});
			if(actionObject != null) {
				fillDropdownCameras(notificationContent.find("#scene-builder-definition-notification-camera"+idSuffix), actionObject.details.includeSnapshotOfCamera);

				phoneNumber.find("input").val(actionObject.details.phoneNumber);
				email.find("input").val(actionObject.details.email);
				subject.find("input").val(actionObject.details.subject);
				message.find("textarea").val(actionObject.details.message);
				type.val(actionObject.details.type);
			}else {
				fillDropdownCameras(notificationContent.find("#scene-builder-definition-notification-camera"+idSuffix));
			}	
			type.trigger("change");
			
			return notificationContent;
		break;
		//case "scene_activation":
			/*if(forTrigger) { 
				// Inputfield with numeric values
				var inputObject = jQuery("<input />", {
					"class":"numbersOnly",
					"style":"width:70px;"
				});
				if(actionObject != undefined) {
					inputObject.val(actionObject.detail.value);
				}
				return $("<span />", { "text" : ""}).append(inputObject);
			}else {
				beforeDescriptionElement.text("Activate ");
			}*/
		//break;
		case "system_command":
			// free inputfield
			var inputObject = jQuery("<input />", {
				"style":"width:500px;",
				"placeholder":'e.g.: echo "standby 3" | cec-client -s -d 1'
			});
			/*if(disabled) {
				inputObject.attr("disabled","disabled");
			}*/

			if(actionObject != undefined) {
				inputObject.val(actionObject.details.value);
			}
			beforeDescriptionElement.text("Activate ");
			return $("<span />", { "text" : " "}).append(inputObject);
		break;
		default:
			if(!forTrigger) {
				beforeDescriptionElement.text("Activate ");
			}
		return null;
	}
}

function getValueFromValueSelector(valueObject, valueSelectorElement, idSuffix, forTrigger) {
	switch(valueObject.valueType) {
		case "color_bulb":
			return {
				"value" : valueSelectorElement.find("input").val()
			}
		break;
		case "period" :
			return {
				"value" : valueSelectorElement.jqCronGetInstance().getCron()
			}
		break;
		case "switch_binary":
			var returnObject = { 
				"value" : valueSelectorElement.find("#value-on-off"+idSuffix).val(),
				"isBecomes" : valueSelectorElement.find('#value-is-becomes'+idSuffix).val(),
			};
			if(forTrigger) {
				returnObject.operand = "EQ";
			}
			
			return returnObject;
		break;
		case "dimmer":		
			var valueText = valueSelectorElement.find(".value").text();
			valueText = valueText.substr(0,valueText.length-1);
			var returnObject = { 
				"value" : valueText
			};
			if(forTrigger) {
				returnObject.isBecomes = valueSelectorElement.find('#value-is-becomes'+idSuffix).val();
				returnObject.operand = valueSelectorElement.find('#value-operand'+idSuffix).val();
			}
			
			return returnObject;
		break;
		case "heating":
		case "meter":
		case "sensor_temperature":
		case "sensor_humidity":
		case "sensor_luminosity":
		case "sensor_general":
		case "sensor_alarm":
		case "wait":
		case "activate_scene":
		case "system_command":
			var returnObject = { 
				"value" : valueSelectorElement.find("input").val()
			};
			if(forTrigger) {
				returnObject.isBecomes = valueSelectorElement.find('#value-is-becomes'+idSuffix).val();
				returnObject.operand = valueSelectorElement.find('#value-operand'+idSuffix).val();
			}
			
			return returnObject;
		break;
		case "send_notification":
			return { 
				"type" : valueSelectorElement.find("#scene-builder-definition-notification-type"+idSuffix).val(),
				"phoneNumber" : valueSelectorElement.find("#scene-builder-definition-notification-phoneNumber"+idSuffix).val(),
				"email" : valueSelectorElement.find("#scene-builder-definition-notification-email"+idSuffix).val(),
				"subject" : valueSelectorElement.find("#scene-builder-definition-notification-subject"+idSuffix).val(),
				"message" : valueSelectorElement.find("#scene-builder-definition-notification-message"+idSuffix).val(),
				"includeSnapshotOfCamera" : valueSelectorElement.find("#scene-builder-definition-notification-camera"+idSuffix).val()				
			};
		break;
	}
	return null;
}
function openRulesOverviewDialog() {
	$("#rules-overview-dialog").dialog("open");
}
function openScenesOverviewDialog() {
	$("#scenes-overview-dialog").dialog("open");
}
function openSceneBuilderDialog(existingScene) {
	if(existingScene != null) {
		$("#wizard-scene-builder-dialog").data("existingScene",existingScene);
	}
	$("#wizard-scene-builder-dialog").dialog("open");
}
function wizardSceneBuilderNext(stepObj,wizardContext) {
	var stepId = stepObj.attr('href');
	
	if( !(wizardContext.fromStep == 4 && wizardContext.toStep == 1 )  &&
		!(wizardContext.fromStep >= wizardContext.toStep)) {
		return wizardSceneBuilderValidate(stepId);
	}else {
		return true;
	}
}
function wizardSceneBuilderValidate(step) {
	var error = false;
	var warning = false;

	if(error) {
		$('#wizard-scheduling').smartWizard('setError',{stepnum:step ,iserror:true});  
		return false;
	}else if(warning) {
		$('#wizard-scheduling').smartWizard('setError',{stepnum:step ,iserror:true});  
	}
	return true;
}
function wizardSceneBuilderBefore(stepObj) {
	
	var stepId = stepObj.attr('href');
	
	if(stepId == "#scene-builder-description") {
		// Initialize value lists
		//initControllerValues();
	}
}
function wizardSceneBuilderFinish(stepObjects) {
	var wizardContainer = $(stepObjects.context);
	var sceneObject = {};
	
	/* Step 1 - Description */
	sceneObject.description = {};
	sceneObject.description.name = wizardContainer.find("#scene-builder-description-name").val();
	
	/* Step 2 - Definition */
	sceneObject.actions = [];
	var i=0;
	wizardContainer.find(".screenSortableList li").each(function(a,element) {
		var actionElement = $(element);
		var idSuffix = "-"+actionElement.attr("rel");
		var actionObject = {};
		actionObject.sequence = i++;
		actionObject.controllerIdentifier = actionElement.find("#scene-builder-definition-controllers"+idSuffix).val();		
		actionObject.nodeIdentifier = actionElement.find("#scene-builder-definition-nodes"+idSuffix).val();		
		
		var valueSelect = actionElement.find("#scene-builder-definition-values"+idSuffix);
		var valueObject = valueSelect.data("valueObject");
		actionObject.valueIdentifier = valueSelect.val();
		actionObject.valueType = valueObject.valueType;
		
		var valueDetailsDiv = actionElement.find("#scene-builder-definition-value-details"+idSuffix);
		actionObject.details = getValueFromValueSelector(valueObject,valueDetailsDiv.children().first(),idSuffix, false);
		sceneObject.actions.push(actionObject);
	});
	
	
	// Are we doing an update or an insert?
	var sourceScene = $("#wizard-scene-builder-dialog").data("existingScene");
	
	if(sourceScene == null) {			
		$.ajax({
		  url: '/api' + '/Scenes',
		  type: 'POST',
		  data: JSON.stringify(sceneObject),
		  success: function(data) {
			  // Add scene to the tree
				var tree = $.jstree.reference("#binding-selector-tree");
				var startNode = tree.get_node("general-Scenes");
				tree.create_node(startNode,  { 	
											"id" : "general-Scenes-"+data.id+"-", 
											"text" : sceneObject.description.name , 
											"type" : "scene_activation",
											"rel"  : "scene_activation",
											"li_attr" : {
														"type" : "scene_activation",
														"identifier" : data.id,
														"rel" : "scene_activation"
													 }
										  }, "first", function(a) { }); 							
			fillSceneOverviewDialog()					  
			showMessageDialog("Scene has been created successfully!", "Scene created", 
				function() { 			
					wizardContainer.parent().dialog("close"); 
				}
			);
		  },
		  error: function(error) {
			showMessageDialog("Scene could not be created.", "Scene create error");
		  }
		});
	}else {
		$.ajax({
		  url: '/api' + '/Scenes/'+sourceScene.id,
		  type: 'PUT',
		  data: JSON.stringify(sceneObject),
		  success: function(data) {
			var tree = $.jstree.reference("#binding-selector-tree");
			var startNode = tree.get_node("general-Scenes-"+sourceScene.id+"-");
			tree.rename_node(startNode, sceneObject.description.name);
					
			showMessageDialog("Scene has been updated successfully!", "Scene updated", 
				function() { 
					
					$("#wizard-scene-builder-dialog").data("existingScene", null);
					fillSceneOverviewDialog()
					wizardContainer.parent().dialog("close"); 
					
					
				}
			);
		  },
		  error: function(error) {
			showMessageDialog("Scene could not be updated.", "Scene update error");
		  }
		});
	}
}
function wizardSceneBuilderLoad(wizardContainer, sceneObject, controllersData) {

	if(sceneObject != null) {
		/* Step 1 - Description */
		wizardContainer.find("#scene-builder-description-name").val(sceneObject.description.name);

		/* Step 2 - Definition */
		//$.getJSON(rootUrl+apiBase + '/Controllers/Values'+json, function(controllersData) {		
		$.each(sceneObject.actions, function(index,action) {
			wizardSceneBuilderAddValue(controllersData, wizardContainer, action);
		});
		//});
		
	}
}
function addSuffix(elementContainer, prefixToCheck, suffixToAdd) {
	elementContainer.attr("id",elementContainer.attr("id")+suffixToAdd);
	elementContainer.find("[id^='"+prefixToCheck+"']").each(function(i) {
		var oldId = $(this).attr("id");
		var newId = oldId+(suffixToAdd);
		$(this).attr('id', newId);
	});
}
function wizardSceneBuilderAddValue(controllersData, baseContainer, actionObject) {
	var allValues = baseContainer.find("#scene-builder-definition-values ul");
	var valueContainer = baseContainer.find("#scene-builder-definition-value-container");
	
	allValues.first().sortable( { cancel: ":input, button, .minicolors"});
	var actionContainer = valueContainer.clone();
	var numberOfActions = allValues.children().length;

	addSuffix(actionContainer,"scene-builder-definition", "-"+(numberOfActions))
	actionContainer.addClass("wizard-scheduling-action");
	actionContainer.removeAttr("style");
	
	fillControllerValueDropdowns(controllersData,actionContainer ,"-"+numberOfActions, actionObject);

	// Wrap in li
	var liObj = jQuery("<li />", { "rel" : numberOfActions });
	liObj.append(actionContainer);
	
	allValues.append(liObj);
	
	return actionContainer;
}
function wizardSceneBuilderRemoveValue(callerObject) {
	showYesNoDialog("Are you sure you want to delete this action?", 
	function() { 
		$(callerObject).parent().parent().parent().parent().remove();
	}, 
	function () {}, 
	"Delete action");
}
function wizardSceneBuilderCollapse(callerObject) {
	$(callerObject).parent().parent().parent().find(".actionDefinition").slideToggle(500, function () {
		$(callerObject).toggleClass("icon-folder-open icon-folder")
	});
}


function fillControllerValueDropdowns(controllersData,forContainer, idSuffix, actionObject) {
	
	// Get data
	//var controllersData = $("#wizard-scene-builder #scene-builder-definition-value-container").data("controllersData");
	//var controllersData = baseContainer.data("controllersData");
	if(controllersData == null) {
		
	}else {
		
		// Fill selects
		var beforeDescriptionSpan = forContainer.find("#scene-builder-definition-before-description"+idSuffix);
		var controllerSelect = forContainer.find("#scene-builder-definition-controllers"+idSuffix);
		var nodeSelect = forContainer.find("#scene-builder-definition-nodes"+idSuffix);
		var valueSelect = forContainer.find("#scene-builder-definition-values"+idSuffix);
		var valueDetailsDiv = forContainer.find("#scene-builder-definition-value-details"+idSuffix);
		
		controllerSelect.change(function(event) {
			// Change value list for nodes
			var selectedController = event.currentTarget.value;
			
			var controllerObject = findByAttribute(controllersData, "identifier", selectedController)
			if(controllerObject != null) {
				nodeSelect.empty();
				for(var i=0;i<controllerObject.nodes.length;i++) {
					var nodeObject = controllerObject.nodes[i];
					var option = $("<option />", {
													"value" : nodeObject.identifier,
													"text" : nodeObject.name
												});
					nodeSelect.append(option);
				}
			}
			if(actionObject != null) {
				nodeSelect.val(actionObject.nodeIdentifier);
				nodeSelect.trigger("change");
			}else if(controllerObject.nodes.length > 0) {
				nodeSelect.val(controllerObject.nodes[0].identifier)
				nodeSelect.trigger("change");
			}
		});		
		nodeSelect.change(function(event) {
			// Change value list for values
			var selectedController = controllerSelect.val();
			var selectedNode = nodeSelect.val();
			
			var controllerObject = findByAttribute(controllersData, "identifier", selectedController);
			var nodeObject = findByAttribute(controllerObject.nodes, "identifier", selectedNode);
			
			
			valueSelect.empty();
			if(nodeObject != null) {
				for(var i=0;i<nodeObject.values.length;i++) {
					var valueObject = nodeObject.values[i];
					var valueObject = nodeObject.values[i];
					var option = $("<option />", {
													"value" : valueObject.identifier,
													"text" : valueObject.name
												});
					valueSelect.append(option);
				}
			}
			if(actionObject != null) {
				valueSelect.val(actionObject.valueIdentifier);
				valueSelect.trigger("change");
			}else if(nodeObject.values.length > 0) {
				var newValue = nodeObject.values[0];
				valueSelect.val(newValue.identifier);
				valueSelect.trigger("change");
			}
		});
		valueSelect.change(function(event) {
			// Update value selector
			var selectedController = controllerSelect.val();
			var selectedNode = nodeSelect.val();
			var selectedValue = valueSelect.val();
			
			var controllerObject = findByAttribute(controllersData, "identifier", selectedController);
			var nodeObject = findByAttribute(controllerObject.nodes, "identifier", selectedNode);
			if(nodeObject != null) {
				var valueObject = findByAttribute(nodeObject.values, "identifier", selectedValue);
				if(valueObject != null) {
					var valueSelector = getValueSelectorByType(valueObject,beforeDescriptionSpan,idSuffix,actionObject,false);
					valueDetailsDiv.empty();
					valueDetailsDiv.append(valueSelector);
					valueSelect.data("valueObject",valueObject)
				}
			}
		});
		
		for(var i=0;i<controllersData.length;i++) {
			var controllerObject = controllersData[i];
			var option = $("<option />", {
											"value" : controllerObject.identifier,
											"text" : controllerObject.name
										});
			controllerSelect.append(option);
		}
		if(actionObject != null) {
			controllerSelect.val(actionObject.controllerIdentifier);
			controllerSelect.trigger("change");
		}else if(controllersData.length > 0) {
			controllerSelect.val(controllersData[0].identifier)
			controllerSelect.trigger("change");
		}
		actionObject = null;
	}
}
//scene-builder-definition-notification-camera
function fillDropdownCameras(dropdownObj, defaultValue) {
	if(dropdownObj.children().length == 0) {
		dropdownObj.append('<option value="">None</option>')
		$.getJSON(rootUrl+apiBase + '/IPCameras'+json, function(data) {
			// Put all ip cameras in the tree
			for(var i=0;i<data.cameras.length;i++) {
				var ipCamera = data.cameras[i];
				var cameraOption = $("<option />", { "value" : ipCamera.id, "text" : ipCamera.name+" ("+ipCamera.id+")"})
				if(defaultValue != null && ipCamera.id == defaultValue) {
					cameraOption.attr("selected", "selected");
				}
				dropdownObj.append(cameraOption);
				//dropdownObj.append('<option value="'+ipCamera.id+'">'+ipCamera.name +' ('+ipCamera.id +')</option>');
			}	
			
			dropdownObj.trigger("change");
		});

	}
}