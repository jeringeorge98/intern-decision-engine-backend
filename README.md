# InBank Backend Service

This service provides a REST API for calculating an approved loan amount and period for a customer.
The loan amount is calculated based on the customer's credit modifier, which is determined by the last four
digits of their ID code.

## Technologies Used

- Java 17
- Spring Boot
- [estonian-personal-code-validator:1.6](https://github.com/vladislavgoltjajev/java-personal-code)

## Requirements

- Java 17
- Gradle

## Installation

To install and run the service, please follow these steps:

1. Clone the repository.
2. Navigate to the root directory of the project.
3. Run `gradle build` to build the application.
4. Run `java -jar build/libs/inbank-backend-1.0.jar` to start the application

The default port is 8080.

## Endpoints

The application exposes a single endpoint:

### POST /loan/decision

The request body must contain the following fields:

- personalCode: The customer's personal ID code.
- loanAmount: The requested loan amount.
- loanPeriod: The requested loan period.

**Request example:**

```json
{
"personalCode": "50307172740",
"loanAmount": "5000",
"loanPeriod": "24"
}
```

The response body contains the following fields:

- loanAmount: The approved loan amount.
- loanPeriod: The approved loan period.
- errorMessage: An error message, if any.

**Response example:**

```json
{
"loanAmount": 2400,
"loanPeriod": 24,
"errorMessage": null
}
```

## Error Handling

The following error responses can be returned by the service:

- `400 Bad Request` - in case of an invalid input
    - `Invalid personal ID code!` - if the provided personal ID code is invalid
    - `Invalid loan amount!` - if the requested loan amount is invalid
    - `Invalid loan period!` - if the requested loan period is invalid
- `404 Not Found` - in case no valid loans can be found
    - `No valid loan found!` - if there is no valid loan found for the given ID code, loan amount, and loan period
- `500 Internal Server Error` - in case the server encounters an unexpected error while processing the request
    - `An unexpected error occurred` - if there is an unexpected error while processing the request

## Architecture

The service consists of two main classes:

- DecisionEngine: A service class that provides a method for calculating an approved loan amount and period for a customer.
- DecisionEngineController: A REST endpoint that handles requests for loan decisions.

## INTERNSHIP ASSIGNMENT 
# TASK 1
- So Ticket-101 asks us to build an automated loan decision engine where the inputs are personal code,loan amount ,loan period in months and returns a decision (negative or positive ) and the amount.
- My fellow colleague started with a fairly descent implementation ,he has created the entities for the request and response and followed SOLID principles more or less with this implemntation
- Positives in the Code : 
- The Code base is well structured ,there are exceptions classes sperately responsible for handling each exception, the tests were very much thorough and well covered.
- The person has tried to follow SOLID in a few places mainly allowing Dependency Injection in the controller class and so forth.
- The code was very well documented with sufficient comments and explanation and followed good naming conventions .
- Overall the code-base provided a very solid foundation for me to implement the rest of the features.
# Improvements
- After assessing the code in line with the S.O.L.I.D principles there are a few improvements that I would recommend
- DecisionEngine class right now handles multiple responsibilities .One recommendation would be move the validation logic and the credit calculation logic to seperate classes and have teh decision engine only calculate the loan amount and the decision to provide the loan,this would be in line with the first principle of S.O.L.I.D -Single Responsibility principle.
- Using dependency Injection more often would be another recommendation ,The EstonianPersonalCodeValidator class is directly instantiated in the decisionEngine instead of maybe using dependency Injection this is in violation of teh last principle of SOLID ,thus we could remove that and have it instantiated in the constructor.Currently it is used only once so it could be accpetable but once the vaildator class is used more often having it as a Component would make sense.
- It would be also nice to have a REST file which would make it easier to test the apis in real time .   
# Biggest shortcoming/bug :
- There were couple of  shortcomings/bugs in the implementation that I have discovered and proposed a fix:
- Credit Score Implementation - The requirement clearly stated that a credit score evaluation algorithmn had to be implemented based on the inputs and any score < 0.1 would not qualify for a loan .This requirement was not fulfilled and thus i have added this fix in my iteration.
- Response Format - The requirement stated that the response needs to have a positive or negative message based on the descision engine and then the amount which was not fully implemented .We were handling the 400,404,500 cases of the requests but the Ok response also has to have a field that tells about the status of the loan and the amount which was then added in my iteration.
- MAXIMUM LOAN PERIOD -The maximum loan period suggested in the requirements is 48 but in the code the constant was fixed to 60 which is a major bug .
- Credit Modifier logic -The requirement has provided example that do not match with the implementation of the credit modifier logic that was implemented , the credit modifier considers last 4 digits to group a personal code in to one of the four segments but these do not match with the logic of the example in the requirement pdf.Thus i have replaced the credit modifier logic with hardcoded values based on the requirements.
# Task 2
 - To Implement Ticket 102 an age field was added to the Decision Request and to the Decision Entity
 - The InvalidAge Exception class was created to handle the invalid age error
 - The logic for handling the age range is written in decision engine class
 - The error message is then sent to the client. 
 - Minimum age was assumed to be 18 and maximum age was assumed to be 70. 
 - Personal Code is assumed to be the same 