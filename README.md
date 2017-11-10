# self-service-time-to-pay-frontend


[![Build Status](https://travis-ci.org/hmrc/self-service-time-to-pay-frontend.svg?branch=master)](https://travis-ci.org/hmrc/self-service-time-to-pay-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/self-service-time-to-pay-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/self-service-time-to-pay-frontend/_latestVersion)

Self Service Time To Pay Frontend

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

### Development Mode

To view the UI **during development**,

1. Start all the related services
    ```bash
    sm --start SELF_SERVICE_TIME_TO_PAY_ALL -f
    ```

2. Run the micro-service from sbt, after stopping any already running SELF_SERVICE_TIME_TO_PAY_FRONTEND service
    ```bash
    sm --stop SELF_SERVICE_TIME_TO_PAY_FRONTEND
    sbt "~run 9063"
    ```
    
3. View in the browser
 http://localhost:9063/pay-what-you-owe-in-instalments

4. For debug purposes
  http://localhost:9063/pay-what-you-owe-in-instalments/test-only/inspector


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
