# self-service-time-to-pay-frontend

[![Build Status](https://travis-ci.org/hmrc/self-service-time-to-pay-frontend.svg?branch=master)](https://travis-ci.org/hmrc/self-service-time-to-pay-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/self-service-time-to-pay-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/self-service-time-to-pay-frontend/_latestVersion)

Self Service Time To Pay Frontend

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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
