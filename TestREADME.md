## Test End Points

### Setting Feature Switches

This can be done in one of two ways;
- Use the test-only endpoint described in the TestReadMe of Incorporated Entity Identification Frontend
- Using the APIs directly 
  - Call ```GET /test-only/api/feature-switches``` to view a JSON object with the current status of the feature switches.
  - Then call ```POST /test-only/api/feature-switches``` with a JSON body as described below with the ```isEnabled``` field set as you wish.
    
#### Example JSON body

```
[
    {
        "configName":"feature-switch.ct-reference-stub",
        "displayName":"Use stub for Get CT Reference",
        "isEnabled":false
    },
    {
        "configName":"feature-switch.des-stub",
        "displayName":"Use stub for submissions to DES",
        "isEnabled":true
    }
]
```

### GET /test-only/corporation-tax/identifiers/crn/:companyNumber

---
Stubs a call to retrieve a CTUTR from the database, must have the corresponding feature switch enabled.

#### Request:
URI must contain a company number

Company Numbers are mapped to specific CTUTRs

| Company Number  | CTUTR
|-----------------|------------------------------
| ```12345678```  |  ```1234567890```
| ```99999999```  |  ```0987654321```
| ```00000000```  |  ```None```


#### Response:

| Expected Status                         | Reason
|-----------------------------------------|------------------------------
| ```OK(200)```                           |  ```CTUTR found```
| ```NOT_FOUND(404)```                    |  ```No CTUTR found in database```

Example Response body:

```
"{CTUTR":"1234567890}"
```

### POST /test-only/cross-regime/register/:identifier

---
Stub for downstream Register API, must have the corresponding feature switch enabled.

#### Request:
This endpoint always returns a successful response regardless of the data sent.

#### Response:
Status: **OK(200)**

Example Response body:

```
{
    "identification": {
                       "idType":"SAFEID",
                       "idValue":"X00000123456789"
    }
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
