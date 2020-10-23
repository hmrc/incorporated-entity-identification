
# incorporated-entity-identification

This is an application to allow Limited Companies to provide their information to HMRC.

### How to run the service
1. Make sure any dependent services are running using the following service-manager command
`sm --start INCORPORATED_ENTITY_IDENTIFICATION_ALL`

2. Stop the frontend in service manager using
 `sm --stop INCORPORATED_ENTITY_IDENTIFICATION_FRONTEND`
 
3. Run the frontend locally using
`sbt 'run 9718 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'`

### End-Points
#### POST /Journey

---
Creates a new journey, storing the journeyConfig against the journeyId
##### Request:
No body is required for this request

##### Response:
Status: **Created(201)**

Example Response body: 

```
{“journeyStartUrl” : "/testUrl"}
```
#### GET /Journey/:journeyId  

---
Retrieves all the journey data that is stored against a specific journeyID.
##### Request:
A valid journeyId must be sent in the URI
##### Response:
Status:

| Expected Response                       | Reason  
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```JourneyId exists```       
| ```NOT_FOUND(404)```                    | ```JourneyId doesn't exist```

Example response body:
```
{"companyProfile":
    {"companyName":"TestCompanyLtd”,
    “companyNumber":"01234567",
    "dateOfIncorporation":"2020-01-01"},
"ctutr":"1234567890",
"identifiersMatch":true,
"businessVerification":
    {"verificationStatus":"PASS"},
"registration":
    {"registrationStatus":"REGISTERED",
    "registeredBusinessPartnerId":"X00000123456789"}
}
```
#### GET /Journey/:journeyId/:dataKey

---
Retrieves all the journey data that matches the dataKey in a specific journeyID.
##### Request:
A valid journeyId and dataKey must be sent in the URI
```
example: JourneyDataController.getJourneyDataByKey(journeyId, ctutr)
```

##### Response:
Status:

| Expected Response                       | Reason  
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```JourneyId exists```       
| ```NOT_FOUND(404)```                    | ```No data exists for JourneyId or dataKey```
| ```FORBIDDEN(403)```                    | ```Auth Internal IDs do not match```


Example response body:
```
"ctutr":"1234567890"
```

#### PUT  /journey/:journeyId/:dataKey

---
stores the json body as the value to the given key in the journey given by the jorneyId
##### Request:
requires a valid journeyId, dataKey and json request

##### Response:

Status:

| Expected Response                       | Reason  
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```OK```       
| ```FORBIDDEN(403)```                    | ```Auth Internal IDs do not match```

Example response body:
```
"set":Json.obj(dataKey -> data)
```

####  POST        /validate-details 
validates the input against the database
##### Request:
No body is required for this request
##### Response:

Status:

| Expected Response                       | Reason  
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```DetailsMatched```       
| ```OK(200)```                           |  ```DetailsMismatched```     
| ```NOT_FOUND(404)```                    | ```DetailsNotFound```

Example response body:
```
"OK":{"matched":true}
```

####  POST        /register  
calls the register api
##### Request:
No body is required for this request
##### Response:
Status:

| Expected Response                       | Reason  
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```Registered```       
| ```OK(200)```                           |  ```RegistrationFailed```  
Example response body:
```
"registration":{
                "registrationStatus":"REGISTERED",
                "registeredBusinessPartnerId":"1030310a-97f5-4893-8d26-3f128f330d54"}
```


#### GET    /corporation-tax/identifiers/crn/:companyNumber  

---
returns the CT reference of the given company
##### Request:
requires a valid Company Number
##### Response:
Status: 

| Expected Response                       | Reason  
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```successful ```       
| ```NOT_FOUND(404)```                    |  ```The back end has indicated that CT UTR cannot be returned```  

Example Response body: 

```
"CTUTR":"1234567890"
```

#### POST       /cross-regime/register/VATC   

---
stub for registerWithMultipleIdentifiers
##### Request:
No body is required for this request

##### Response:
Status: **OK(200)**

Example Response body: 

```
"identification":(
          "idType":"SAFEID",
          "idValue":"X00000123456789")
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
