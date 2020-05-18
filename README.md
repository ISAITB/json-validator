# Introduction

Application used for the validation of JSON content by means of:

* A SOAP API.
* A web form.

The validator can be used with a single or multiple validation domains, i.e. validation cases that should be considered
as distinct. Note that each such domain can still contain within it multiple validation types - the validation domain
is used more to split distinct user groups, whereas the validation type is used for different types of validation within
a specific user group.

Configuration of each domain's validation is via a property file contained within the domain's subfolder under the 
configured resource root. 

The different application modes can be disabled/enabled as configuration properties in this domain configuration.

The application is built using Spring-Boot. The project includes a sample definition of CPSV-AP to work out of 
the box (under the `etc` folder). This however should be replaced with the actual implementation depending on the 
installation. 

# Building

Issue `mvn clean install`

The resulting artefact can then be retrieved from `shaclvalidator-war`.

## For development

To override settings the simplest approach is to define environment variables matching the configuration properties to
override. Alternatively a file `application-dev.properties` can be provided that is enabled by activating the `dev`
profile (see Spring Boot documentation on this).  

To run change first to the required module:
- `shaclvalidator-war` to run as a web app

Then, from this directory do

```
mvn spring-boot:run
```

# Running the applications

Both web and standalone versions require at least Java 8 to run.

## Web application

The application is accessible at:

* SOAP-API: http://localhost:8080/json/soap/DOMAIN/validation?wsdl
* Web form: http://localhost:8080/json/DOMAIN/upload

Note that all configuration properties in `application.properties` can be overriden by means of environment variables
(e.g. set in a downstream Dockerfile). 

# Configuration and use

The key point to consider when using this validator is the validation domains it will consider. This is defined through
the **mandatory configuration property** `validator.resourceRoot` which needs to point to a folder that contains for 
each supported domain, a sub-folder with the validation artefacts (that can be arbitrarily organised) and a property
file (e.g. `config.properties`) that defines the configuration for that specific domain. This property can be defined
either in a separate `application.properties` file, or more simply, passed as a system property or environment variable
(using an environment variable a particularly easy approach when creating a downstream docker image).

The name of the domain sub-folder will be used as the domain name. In addition, if the `validator.resourceRoot` parent
folder contains also directories that should not be considered, the specific domains (i.e. sub-folder named) to be 
considered can be specified explicitly through the `validator.domain` property, a comma-separate listing of the domain
folder named to consider.

## Use via docker

To use as a docker container do the following:
1. Create a folder `my-app`.
2. In this folder copy create a folder named e.g. `domain`.
3. Copy in the `domain` folder the validation artefacts and domain configuration property file.
4. Create a Dockerfile as follows:
```
FROM isaitb/json-validator:latest

ENV validator.resourceRoot /validator/
COPY domain /validator/domain/
```  
5. Build the docker image and proceed to use it.

**Important:** The naming of the `domain` folder in the above example is important as it will be used for the 
request paths for all communication channels (e.g. http://localhost:8080/json/soap/DOMAIN/validation?wsdl for the SOAP
web service endpoint). This can be overriden with the `validator.domainName.XYZ` property. 

## Configuration property reference

The tool supports configuration at two levels:
* The overall application.
* Each configured validation domain.

## Application-level configuration

The properties defined here can be specified in a separate Spring Boot `application.properties` file or passed in via
system properties or environment variables. Apart from what is listed, any Spring Boot configuration property can be
defined.

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.resourceRoot` | The root folder under which domain sub-folders will be loaded. | String | - |
| `validator.domain` | The names of the domain sub-folders to consider. | Comma-separated Strings | - |
| `validator.domainName.XYZ` | The name to display for a given domain folder. | String | The folder name is used |  
| `logging.path` | Logging path. | String | `/validator/logs` |
| `validator.tmpFolder` | Temp folder path. | String | `/validator/tmp` |
| `validator.reportFolder` | Temp report path. | String | `/validator/reports` |
| `validator.acceptedSchemaExtensions` | Accepted JSON Schema extensions.  | Comma-separated Strings | `json` |

Check file `application.properties` for additional properties that can be configured.

## Domain-level configuration

The properties here define how a specific validation domain is configured. They need to be placed in a property file
(any file name ending with `.properties`) within a domain sub-folder under the configured `validator.resourceRoot`.

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.channels` | Comma separated list of features to have enabled. Possible values are (`form`, `soap_api`). | Comma-separated Strings | `form,soap_api` |
| `validator.type` | A comma-separated list of supported invoice types. Values need to be reflected in properties `validator.typeLabel`, `validator.schemaFile`, `validator.externalSchemas`. | Comma-separated Strings | - |
| `validator.typeLabel.XYZ` | Label to display for a given validator type (added as a postfix of validator.typeLabel). | String | - |
| `validator.schemaFile.XYZ` | A comma-separated list of Schema files loaded for a given validation type (added as a postfix). These can be file or folders. | Comma-separated Strings | - |
| `validator.schemaFile.XYZ.remote.A.url` | The Schema files loaded for a given validation type (added as a postfix) as URL. | String | - |
| `validator.schemaFile.XYZ.combinationApproach` | In case multiple pre-configured schema files are defined this specifies how they are combined. | One of `allOf`, `anyOf`, `oneOf` | `allOf` |
| `validator.externalSchemas.XYZ` | External schemas are allowed for a given validation type (added as a postfix) as Boolean. | Boolean | `false` |
| `validator.externalSchemaCombinationApproach.XYZ` | In case multiple external schemas are provided this specifies how they are combined. | One of `allOf`, `anyOf`, `oneOf` | `allOf` |
| `validator.webServiceId` | The ID of the web service. | String | `ValidatorService` |
| `validator.webServiceDescription.contentToValidate` | The description of the web service for element "contentToValidate". | String | - |
| `validator.webServiceDescription.validationType` | The description of the web service for element "validationType". | String | - |
| `validator.webServiceDescription.embeddingMethod` | The description of the web service for element "embeddingMethod". | String | - |
| `validator.webServiceDescription.externalSchemas` | The description of the web service for element "externalSchemas". | String | - |
| `validator.webServiceDescription.externalSchemaCombinationApproach` | The description of the web service for element "externalSchemaCombinationApproach". | String | - |
| `validator.webServiceDescription.locationAsPointer` | The description of the web service for element "locationAsPointer". | String | - |
| `validator.supportMinimalUserInterface` | A minimal UI is available if this is enabled. | Boolean | `false` |
| `validator.bannerHtml` | Configurable HTML banner replacing the text title. | String | - |
| `validator.footerHtml` | Configurable HTML footer. | String | - |

