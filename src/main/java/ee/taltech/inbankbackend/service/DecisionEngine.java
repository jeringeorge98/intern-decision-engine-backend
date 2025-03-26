package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;
import ee.taltech.inbankbackend.exceptions.InvalidAgeException;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod,int age)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,InvalidAgeException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod,age);
        } catch (Exception e) {
            return new Decision(false,null, null,age, e.getMessage());
        }
        creditModifier = getCreditModifier(personalCode);
        if (creditModifier == 0) {
            // user has debt return 200 with loan rejected
            return new Decision(false,0,0,age,"User has debt");
        } else if (creditModifier == -1) {
            throw new InvalidPersonalCodeException("Personal Code is not supported");
        }

            float creditScore = calculateCreditScore(creditModifier, loanAmount, loanPeriod);
            int maxAmount;
            maxAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT,highestValidLoanAmount(loanPeriod));

        // For credit score >= 0.1, we can directly return the max amount
        if (creditScore >= 0.1 && maxAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            return new Decision(true, maxAmount, loanPeriod, age, null);
        }

        // For credit score < 0.1 or if max amount is below minimum,
        // try to find a valid loan by increasing period

            for (int newPeriod = loanPeriod + 1; newPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD; newPeriod++) {
                int newAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(newPeriod));
                if (newAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
                    return new Decision(true, newAmount, newPeriod, age, null);
                }
            }
     throw new NoValidLoanException("No Valid Loan found !");
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * We are considering only the four examples as given in the requirements text
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode){
        return switch (personalCode) {
            case "49002010965" -> 0;
            case "49002010976" -> DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
            case "49002010987" -> DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
            case "49002010998" -> DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
            default -> -1;
        };
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws InvalidAgeException If the age is not within the acceptable range
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod,int age)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,InvalidAgeException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

        if (!((DecisionEngineConstants.MIN_AGE_CUSTOMER <= age) && ((DecisionEngineConstants.MAX_AGE_CUSTOMER * 12) >= ((age * 12)+DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)))){
            throw  new InvalidAgeException("Age " +age+ " is not within the acceptable range!");
        }

    }

    private float calculateCreditScore(int creditModifier,long loanAmount,int loanPeriod){
        return (((float) creditModifier / loanAmount) * loanPeriod) /10;
    }


}
