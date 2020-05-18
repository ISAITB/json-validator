function validationTypeChanged() {
	cleanExternalArtefacts("externalSchema");
	checkForSubmit();
	externalArtefactsEnabled();
}

function cleanExternalArtefacts(type){
    var elements = $("."+type+"Div");
	
	for (var i=0; i<elements.length; i++){
		removeElement(elements[i].getAttribute('id'), type);
	}
    
    if($("."+type+"Class").is(":hidden")){
    	$("."+type+"Class").toggle();
    }
	
}

function externalArtefactsEnabled(){
	$(".includeExternalArtefacts").addClass('hidden');	
	$("#externalArtefactsCheck").prop('checked', false);
	
	var includeExternalArtefacts = getExternalType("externalSchema");

	if(includeExternalArtefacts == "none"){
		$(".includeExternalArtefacts").addClass('hidden');
	} else {
		if (includeExternalArtefacts == "required") {
			$(".includeExternalArtefacts").addClass('hidden');
		}else{
			$(".includeExternalArtefacts").removeClass('hidden');
		}
	}
	
	toggleExternalArtefactsClassCheck();
}

function addExternalSchema() {
	var toggleExt = getExternalType("externalSchema");
    var elements = $(".externalSchemaDiv");

	if (toggleExt == "required" && elements.length == 1) {
		var indexInt = getLastExternalIdNumber(".externalSchemaDiv");
		$('#rmvButton-externalSchema-'+indexInt).removeClass('hidden');
		$('#fileToValidate-class-externalSchema-'+indexInt).removeClass('col-sm-12');
		$('#fileToValidate-class-externalSchema-'+indexInt).addClass('col-sm-11');
		$('#uriToValidate-externalSchema-'+indexInt).removeClass('col-sm-12');
		$('#uriToValidate-externalSchema-'+indexInt).addClass('col-sm-11');
	}
	addElement("externalSchema", externalSchemaPlaceholder, true);
    toggleSchemaCombination();
}

function toggleSchemaCombination() {
	if ($(".externalSchemaDiv").length > 1) {
	    $('#schemaCombinationDiv').show();
	} else {
	    $('#schemaCombinationDiv').hide();
	    $('#combinationType').val('allOf');
	}
}

function removeElement(elementId, type) {
	document.getElementById(elementId).remove();
	
	var toggleExt = getExternalType("externalSchema");
    var elements = $("."+type+"Div");

	if (toggleExt == "required" && elements.length==1){
		var indexInt = getLastExternalIdNumber("."+type+"Div");
		$('#rmvButton-externalSchema-'+indexInt).addClass('hidden');
		$('#fileToValidate-class-externalSchema-'+indexInt).removeClass('col-sm-11');
		$('#fileToValidate-class-externalSchema-'+indexInt).addClass('col-sm-12');
		$('#uriToValidate-externalSchema-'+indexInt).removeClass('col-sm-11');
		$('#uriToValidate-externalSchema-'+indexInt).addClass('col-sm-12');
	}
    toggleSchemaCombination();
    checkForSubmit();
}

function getLastExternalIdNumber(type){
    var elements = $(type);
	var index = elements.attr('id').indexOf("-")+1;
	var indexInt = parseInt(elements.attr('id').substring(index));
	
	return indexInt;
}

function triggerFileUploadSchema(elementId) {
    $("#"+elementId).click();
}
function fileInputChangedSchema(type){
	if($('#contentType-'+type).val()=="fileType" && $("#inputFile-"+type+"")[0].files[0]!=null){
		$("#inputFileName-"+type+"").val($("#inputFile-"+type+"")[0].files[0].name);
	}
	fileInputChanged();
}
function fileInputChanged() {
	if($('#contentType').val()=="fileType" && $('#inputFile')[0].files[0]!=null){
		$('#inputFileName').val($('#inputFile')[0].files[0].name);
	}
	checkForSubmit();
}
function contentTypeChangedSchema(elementId){
	var type = $('#contentType-'+elementId).val();

	if (type == "uriType"){
		$("#uriToValidate-"+elementId).removeClass('hidden');
		$("#fileToValidate-"+elementId).addClass('hidden');
	} else if (type == "fileType"){
		$("#fileToValidate-"+elementId).removeClass('hidden');
		$("#uriToValidate-"+elementId).addClass('hidden');
	}
	
	fileInputChangedSchema(elementId);
}

function addElement(type, placeholderText, focus) {
    var elements = $("."+type+"Div");
    var indexLast = 0;
	
	for (var i=0; i<elements.length; i++){
		var index = elements[i].getAttribute('id').indexOf("-")+1;
		var indexInt2 = parseInt(elements[i].getAttribute('id').substring(index));
		
		if(indexInt2>indexLast){
			indexLast = indexInt2;
		}
	}
	var elementId = type+"-"+(indexLast+1);

    $("<div class='row form-group "+type+"Div' id='"+elementId+"'>" +
    	"<div class='col-sm-2'>"+
			"<select class='form-control' id='contentType-"+elementId+"' name='contentType-"+type+"' onchange='contentTypeChangedSchema(\""+elementId+"\")'>"+
				"<option value='fileType' selected='true'>"+labelFile+"</option>"+
				"<option value='uriType'>"+labelURI+"</option>"+
		    "</select>"+
		"</div>"+
		"<div class='col-sm-10'>" +
		    "<div class='row'>" +
                "<div id='fileToValidate-class-"+elementId+"' class='col-sm-11'>" +
                    "<div class='input-group' id='fileToValidate-"+elementId+"'>" +
                        "<div class='input-group-btn'>" +
                            "<button class='btn btn-default' type='button' onclick='triggerFileUploadSchema(\"inputFile-"+elementId+"\")'><i class='far fa-folder-open'></i></button>" +
                        "</div>" +
                        "<input type='text' id='inputFileName-"+elementId+"' placeholder='"+placeholderText+"' class='form-control clickable' onclick='triggerFileUploadSchema(\"inputFile-"+elementId+"\")' readonly='readonly'/>" +
                    "</div>" +
                "</div>" +
                "<div class='col-sm-11 hidden' id='uriToValidate-"+elementId+"'>"+
                    "<input type='url' class='form-control' id='uri-"+elementId+"' name='uri-"+type+"' onchange='fileInputChangedSchema(\""+elementId+"\")'>"+
                "</div>"+
                "<input type='file' class='inputFile' id='inputFile-"+elementId+"' name='inputFile-"+type+"' onchange='fileInputChangedSchema(\""+elementId+"\")'/>" +
                "<div class='col-sm-1'>" +
                    "<button class='btn btn-default' id='rmvButton-"+elementId+"' type='button' onclick='removeElement(\""+elementId+"\", \""+type+"\")'><i class='far fa-trash-alt'></i></button>" +
                "</div>" +
    		"</div>"+
		"</div>"+
    "</div>").insertBefore("#"+type+"AddButton");
    if (focus) {
        $("#"+elementId+" input").focus();
    }
}

function checkForSubmit() {
	var type = $('#contentType').val();
	var inputType = $('#validationType');
	$('#inputFileSubmit').prop('disabled', true);	
	var submitDisabled = true
	if (type == "fileType") {
		var inputFile = $("#inputFileName");
		submitDisabled = (inputFile.val() && (!inputType.length || inputType.val()))?false:true
	} else if(type == "uriType") {
		var uriInput = $("#uri");
		submitDisabled = (uriInput.val() && (!inputType.length || inputType.val()))?false:true
	} else if(type == "stringType") {
		var stringType = getCodeMirrorNative('#text-editor').getDoc();
		submitDisabled = (stringType.getValue() && (!inputType.length || inputType.val()))?false:true
	}
	if (!submitDisabled) {
        if (getExternalType("externalSchema") == "required") {
            submitDisabled = !externalElementHasValue(document.getElementsByName("contentType-externalSchema"));
        }
	}
    $('#inputFileSubmit').prop('disabled', submitDisabled);
}

function externalElementHasValue(elementExt) {
	if (elementExt.length > 0) {
	    for (var i=0; i < elementExt.length; i++) {
            var type = elementExt[i].options[elementExt[i].selectedIndex].value;
            var id = elementExt[i].id.substring("contentType-".length, elementExt[i].id.length);
            if (type == "fileType" && $("#inputFileName-"+id).val() || type == "uriType" && $("#uri-"+id).val()) {
                    return true;
            }
	    }
	}
	return false;
}

function triggerFileUpload() {
	$('#inputFile').click();
}
function uploadFile() {
	waitingDialog.show('Validating input', {dialogSize: 'm'}, isMinimal?'busy-modal-minimal':'busy-modal');
	return true;
}

function contentTypeChanged(){
	var type = $('#contentType').val();
	$('#inputFileSubmit').prop('disabled', true);
	
	if(type == "uriType"){
		$("#uriToValidate").removeClass('hidden');
		$("#fileToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	} else if(type == "fileType"){
		$("#fileToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	} else if(type == "stringType"){
		$("#stringToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#fileToValidate").addClass('hidden');
		setTimeout(function() {
            var codeMirror = getCodeMirrorNative('#text-editor')
            codeMirror.refresh();
		}, 0);
	}
}
function getCodeMirrorNative(target) {
    var _target = target;
    if (typeof _target === 'string') {
        _target = document.querySelector(_target);
    }
    if (_target === null || !_target.tagName === undefined) {
        throw new Error('Element does not reference a CodeMirror instance.');
    }
    
    if (_target.className.indexOf('CodeMirror') > -1) {
        return _target.CodeMirror;
    }

    if (_target.tagName === 'TEXTAREA') {
        return _target.nextSibling.CodeMirror;
    }
    
    return null;
}
function contentSyntaxChanged() {
	checkForSubmit();
}

function toggleExternalArtefacts(){
	var ext = getExternalType("externalSchema");
	if (ext == "optional"){
		$(".externalSchemaClass").toggle();
	}
}

function getExternalType(extElementId){
	var type = $('#validationType').val();
	var ext = document.getElementById(extElementId);
	
	var externalType = "none";
	
	for (var i=0; i<ext.length; i++){
		if (ext[i].text == type || ext.length == 1){
			externalType = ext[i].value;
		}
	}
	
	return externalType;
}

function toggleExternalArtefactsClassCheck() {
	var toggleExt = getExternalType("externalSchema");
	
	if (toggleExt == "required"){
		
		if (toggleExt == "none"){
			$(".externalSchemaClass").toggle();
		}

		if (toggleExt == "required"){
			addElement("externalSchema", externalSchemaPlaceholder, false);
			$('#rmvButton-externalSchema-1').addClass('hidden');

			$('#fileToValidate-class-externalSchema-1').removeClass('col-sm-11');
			$('#fileToValidate-class-externalSchema-1').addClass('col-sm-12');
			$('#uriToValidate-externalSchema-1').removeClass('col-sm-11');
			$('#uriToValidate-externalSchema-1').addClass('col-sm-12');
		}
		
	} else {
		if (toggleExt == "none") {
		    if ($(".externalSchemaClass").is(":visible")) {
		    	$(".externalSchemaClass").toggle();
		    }
		} else {
			$(".externalSchemaClass").toggle();
		}
	}
    toggleSchemaCombination();
}

$(document).ready(function() {
	checkForSubmit();
	externalArtefactsEnabled();

	if (document.getElementById('text-editor') !== null){
		var editableCodeMirror = CodeMirror.fromTextArea(document.getElementById('text-editor'), {
	        mode: {name: "javascript", json: true},
	        lineNumbers: true
	    }).on('change', function(){
	    	contentSyntaxChanged();
	    });
	}
});