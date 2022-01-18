# Incorporated Entity Identification

This is an application to allow Limited Companies to provide their information to HMRC.

### How to run the service

1. Make sure any dependent services are running using the following service-manager command
   `sm --start INCORPORATED_ENTITY_IDENTIFICATION_ALL`

2. Stop the backend in service manager using
   `sm --stop INCORPORATED_ENTITY_IDENTIFICATION`

3. Run the backend locally using
   `sbt 'run 9719 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'`

### How to test the service

See [TestREADME](TestREADME.md) for more information about test data and endpoints

## End-Points

### POST /journey

---
Creates a new journeyId and stores it in the database

#### Request:

No body is required for this request

#### Response:

Status: **Created(201)**

Example Response body:

```
{“journeyId”: "<random UUID>"}
```

### GET /journey/:journeyId

---
Retrieves all the journey data that is stored against a specific journeyID.

#### Request:

A valid journeyId must be sent in the URI

#### Response

| Expected Status                         | Reason                           |
|-----------------------------------------|----------------------------------|
| ```OK(200)```                           | ```JourneyId exists```           |
| ```NOT_FOUND(404)```                    | ```JourneyId does not exist```   |

Example response body:

```
{
    "companyProfile": {
                       "companyName":"TestCompanyLtd”,
                       “companyNumber":"01234567",
                       "dateOfIncorporation":"2020-01-01"
    },
    "ctutr":"1234567890",
    "identifiersMatch":true,
    "businessVerification": {
                             "verificationStatus":"PASS"
    },
    "registration": {
                     "registrationStatus":"REGISTERED",
                     "registeredBusinessPartnerId":"X00000123456789"
    }
}
```

### GET /journey/:journeyId/:dataKey

---
Retrieves all the journey data that matches the dataKey for a specific journeyID.

#### Request:

Example Request URI

```
/journey/testJourneyId/ctutr
```

#### Response:

| Expected Status                         | Reason                                        |
|-----------------------------------------|-----------------------------------------------|
| ```OK(200)```                           | ```JourneyId exists```                        |
| ```NOT_FOUND(404)```                    | ```No data exists for JourneyId or dataKey``` |
| ```FORBIDDEN(403)```                    | ```Auth Internal IDs do not match```          |

Response body for example URI:

```
{"1234567890"}
```

### PUT /journey/:journeyId/:dataKey

---
Stores the json body against the data key and journey id provided in the uri

#### Request:

Requires a valid journeyId and user must be authorised to make changes to the data

Example request URI:

```
/journey/testJourneyId/ctutr
```

Example request body:

```
{"1234567890"}
```

#### Response:

| Expected Status                         | Reason                               |
|-----------------------------------------|--------------------------------------|
| ```OK(200)```                           | ```OK```                             |
| ```FORBIDDEN(403)```                    | ```Auth Internal IDs do not match``` |

### DELETE /journey/:journeyId/:dataKey

---
Removes the data that is stored against the dataKey provided for the specific journeyId

#### Request:

Requires a valid journeyId and dataKey

Example request URI:
`testJourneyId = <random UUID>`

```
/journey/remove/testJourneyId/nino
```

#### Response:

| Expected Status                         | Reason                                         |
|-----------------------------------------|------------------------------------------------|
| ```NO_CONTENT(204)```                   | ```Field successfully deleted from database``` |
| ```FORBIDDEN(403)```                    | ```Auth Internal IDs do not match```           |

### DELETE /journey/:journeyId

---
Removes all the data that is stored for the specific journeyId

#### Request:

Requires a valid journeyId

Example request URI:
`testJourneyId = <random UUID>`

```
/journey/remove/testJourneyId
```

#### Response:

| Expected Status                         | Reason                                        |
|-----------------------------------------|-----------------------------------------------|
| ```NO_CONTENT(204)```                   | ```Data successfully deleted from database``` |

### POST /validate-details

---
Checks if the user entered identifiers match what is held in the database. This endpoint is feature switched using
the `Use stub for Get CT Reference` switch, which returns a specific CTUTR based on the CRN.

#### Request:

Example Body:

```
{
    "companyNumber": 12345678,
    "ctutr": 1234567890
}
```

#### Response:

| Expected Status                         | Reason                                                           |
|-----------------------------------------|------------------------------------------------------------------|
| ```OK(200)```                           | ```Identifiers found in database and check returned a result```  |
| ```NOT_FOUND(404)```                    | ```No identifiers found in databse```                            |

Example response bodies:

```
{"matched":true}
```

or

```
{"matched":false}
```

### POST /register-limited-company or /register-registered-society

___
Submits a registration request to the downstream Register API. This API is feature switched behind
the `Use stub for submissions to DES` switch so it can be stubbed using the Register test endpoint described below.

#### Request:

Body:

```
{
    "crn": 12345678,
    "ctutr": 1234567890,
    "regime": "VATC"
}
```

Both of the endpoints require only the above identifiers to build the JSON that is sent to the downstream Registration
API.

#### Response:

Status: **OK(200)**
Attempted registration and returns result of call

Example response bodies:

```
{
    "registration":{
                    "registrationStatus":"REGISTERED",
                    "registeredBusinessPartnerId":"<randomm UUID>"
    }
}
```

or

```
{
    "registration":{
                    "registrationStatus":"REGISTRATION_FAILED",
    }
}
```

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
