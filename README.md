# self-service-time-to-pay-frontend


Tip: Connect to VPN in order to build it.  

### About 
Self Service Time To Pay Frontend is the frontend microservice for Pay What You Owe In Instalments.
It displays the web pages that users interact with and contacts multiple backend services to retrieve and store information.
It has multiple endpoints that are navigated through in sequence, beginning with eligibility, through to calculator and then arrangement.
The following diagram shows the overview of the SSTTP architecture.

<a href="https://github.com/hmrc/self-service-time-to-pay-frontend">
    <p align="center">
      <img src="https://raw.githubusercontent.com/hmrc/self-service-time-to-pay-frontend/master/public/ServiceOverview.png" alt="ServiceOverview">
    </p>
</a>

### Quickstart in local
- Service manager profile: ```sm --start SSTTP```

- Running from source with test only routes enabled: ```sbt runTestOnly```

- View in the browser
  http://localhost:9063/pay-what-you-owe-in-instalments

- For session debug purposes
  http://localhost:9063/pay-what-you-owe-in-instalments/test-only/inspector
  
### Further information

- [Confluence space](https://confluence.tools.tax.service.gov.uk/display/SSTTP)

- [UI acceptance tests](https://github.com/hmrc/self-service-time-to-pay-acceptance-tests)

- [Performance tests](https://github.com/hmrc/self-service-time-to-pay-performance-tests)

---

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
