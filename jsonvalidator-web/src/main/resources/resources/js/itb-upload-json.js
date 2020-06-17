addListener('ADDED_EXTERNAL_ARTIFACT_INPUT', toggleSchemaCombination);
addListener('REMOVED_EXTERNAL_ARTIFACT_INPUT', toggleSchemaCombination);
addListener('RESET_EXTERNAL_ARTIFACT_INPUTS', toggleSchemaCombination);

function toggleSchemaCombination() {
	if ($('.externalDiv_default').length > 1) {
	    $('#schemaCombinationDiv').show();
	} else {
	    $('#schemaCombinationDiv').hide();
	    $('#combinationType').val('allOf');
	}
}