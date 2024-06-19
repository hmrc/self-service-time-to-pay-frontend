# self-service-time-to-pay-frontend

---

### About 
Self Service Time To Pay Frontend is the frontend microservice for Pay What You Owe In Instalments, used for self-assessment tax liabilities.
It displays the web pages that users interact with and contacts multiple backend services to retrieve and store information.
It has multiple endpoints that are navigated through in sequence, beginning with eligibility, through to calculator and then arrangement.
The following diagram shows the overview of the SSTTP architecture.

<a href="https://github.com/hmrc/self-service-time-to-pay-frontend">
    <p align="center">
      <img src="docs/ServiceOverview.png" alt="ServiceOverview">
    </p>
</a>

### Run locally

Start up supporting services with ```sm2 --start SSTTP```service manager profile

To run with test endpoints enabled: `sbt runTestOnly`

If the dependent services for the webchat link on "Set up a payment plan with an adviser" page are required
run ```sm2 --start DIGITAL_ENGAGEMENT_PLATFORM_ALL``` as well

Navigate to http://localhost:9063/pay-what-you-owe-in-instalments/test-only/inspector

For all other enviroments see: https://confluence.tools.tax.service.gov.uk/display/SSTTP/Service+Links

- First go to http://localhost:9063/pay-what-you-owe-in-instalments/test-only/inspector
- Choose 'create user and log in' link
- Accept default 'Frozen Date' so this is in alignment with the default test data in the stubs for the happy path where the customer is eligible.
- Click 'create user and log in' button
- Go to the first page at http://localhost:9063/pay-what-you-owe-in-instalments 
- Clicking the 'Start now' button will now take you to http://localhost:9063/pay-what-you-owe-in-instalments/calculator/tax-liabilities as if you were an eligible taxpayer

For all other enviroments see: https://confluence.tools.tax.service.gov.uk/display/SSTTP/Service+Links

---

#### Note about integration tests

Nb: Running the integration tests locally - i.e. `sbt test`- may require that ASSETS_FRONTEND is not running locally.
  
### Further information

- [Confluence space](https://confluence.tools.tax.service.gov.uk/display/SSTTP)
- [Test data and environment details](https://confluence.tools.tax.service.gov.uk/display/SSTTP/Testing+-+Development+environment+test+data)

- [UI acceptance tests](https://github.com/hmrc/self-service-time-to-pay-acceptance-tests)

- [Performance tests](https://github.com/hmrc/self-service-time-to-pay-performance-tests)

---

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

