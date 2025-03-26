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
    
    // Constants for credit modifiers
    private static final int HAS_DEBT = 0;
    private static final int UNSUPPORTED_PERSONAL_CODE = -1;
    private static final float CREDIT_SCORE_THRESHOLD = 0.1f;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 48 months (inclusive).
     * The loan amount must be between 2000 and 10000€ (inclusive).
     *
     * @param personalCode ID code of the customer that made the request
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @param age Customer's age
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws InvalidAgeException If the age is not within the acceptable range
     * @throws NoValidLoanException If there is no valid loan found for the given inputs
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod, int age)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, 
            InvalidAgeException, NoValidLoanException {
        
        // Validate all inputs - Let specific exceptions propagate
        verifyInputs(personalCode, loanAmount, loanPeriod, age);
        
        // Get customer's credit modifier
        int creditModifier = getCreditModifier(personalCode);
        
        if (creditModifier == HAS_DEBT) {
            return createRejectedDecision(age, "User has debt");
        } 
        
        if (creditModifier == UNSUPPORTED_PERSONAL_CODE) {
            throw new InvalidPersonalCodeException("Personal Code is not supported");
        }
        
        return calculateLoanDecision(creditModifier, loanAmount, loanPeriod, age);
    }
    
    /**
     * Creates a Decision object for a rejected loan application
     * 
     * @param age Customer's age
     * @param reason Reason for rejection
     * @return Decision object with rejection details
     */
    private Decision createRejectedDecision(int age, String reason) {
        return new Decision(false, 0, 0, age, reason);
    }
    
    /**
     * Calculates the loan decision based on credit modifier, amount and period
     * 
     * @param creditModifier Customer's credit modifier
     * @param requestedAmount Requested loan amount
     * @param requestedPeriod Requested loan period
     * @param age Customer's age
     * @return Decision object with approved loan details or null
     * @throws NoValidLoanException If no valid loan can be found
     */
    private Decision calculateLoanDecision(int creditModifier, long requestedAmount, int requestedPeriod, int age) 
            throws NoValidLoanException {
        
        float creditScore = calculateCreditScore(creditModifier, requestedAmount, requestedPeriod);
        int maxAmount = calculateMaxAmount(creditModifier, requestedPeriod);
        
        // Check if requested period works
        if (creditScore >= CREDIT_SCORE_THRESHOLD && maxAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            return new Decision(true, maxAmount, requestedPeriod, age, null);
        }
        
        // Try to find a valid loan by increasing period
        return findValidLoanWithIncreasedPeriod(creditModifier, requestedPeriod, age);
    }
    
    /**
     * Attempts to find a valid loan by increasing the loan period
     * 
     * @param creditModifier Customer's credit modifier
     * @param startPeriod Starting loan period
     * @param age Customer's age
     * @return Decision object with approved loan details
     * @throws NoValidLoanException If no valid loan can be found
     */
    private Decision findValidLoanWithIncreasedPeriod(int creditModifier, int startPeriod, int age) 
            throws NoValidLoanException {
        
        for (int newPeriod = startPeriod + 1; newPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD; newPeriod++) {
            int newAmount = calculateMaxAmount(creditModifier, newPeriod);
            if (newAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
                return new Decision(true, newAmount, newPeriod, age, null);
            }
        }
        
        throw new NoValidLoanException("No valid loan found!");
    }

    /**
     * Calculates the maximum possible loan amount based on credit modifier and period
     *
     * @param creditModifier Customer's credit modifier
     * @param loanPeriod Loan period in months
     * @return Maximum possible loan amount
     */
    private int calculateMaxAmount(int creditModifier, int loanPeriod) {
        int calculatedAmount = creditModifier * loanPeriod;
        return Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, calculatedAmount);
    }

    /**
     * Calculates the credit modifier of the customer according to their personal code.
     * We are considering only the specific examples as given in the requirements.
     * 
     * @param personalCode ID code of the customer
     * @return Credit modifier value based on customer segment
     */
    private int getCreditModifier(String personalCode) {
        return switch (personalCode) {
            case "49002010965" -> HAS_DEBT;
            case "49002010976" -> DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
            case "49002010987" -> DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
            case "49002010998" -> DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
            default -> UNSUPPORTED_PERSONAL_CODE;
        };
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested
     *                   loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws InvalidAgeException If the age is not within the acceptable range
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod, int age)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, InvalidAgeException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        
        if (loanAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT || 
            loanAmount > DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        
        if (loanPeriod < DecisionEngineConstants.MINIMUM_LOAN_PERIOD || 
            loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }
        
        if (!isValidAge(age, loanPeriod)) {
            throw new InvalidAgeException("Age " + age + " is not within the acceptable range!");
        }
    }
    
    /**
     * Checks if the customer's age is valid for the requested loan period
     * 
     * @param age Customer's age
     * @param loanPeriod Requested loan period
     * @return true if age is valid, false otherwise
     */
    private boolean isValidAge(int age, int loanPeriod) {
        boolean isAboveMinimumAge = age >= DecisionEngineConstants.MIN_AGE_CUSTOMER;
        boolean willNotExceedMaxAge = (age * 12) + loanPeriod <= (DecisionEngineConstants.MAX_AGE_CUSTOMER * 12);
        return isAboveMinimumAge && willNotExceedMaxAge;
    }

    /**
     * Calculates credit score based on credit modifier, loan amount and period
     * 
     * @param creditModifier Customer's credit modifier
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return Calculated credit score
     */
    private float calculateCreditScore(int creditModifier, long loanAmount, int loanPeriod) {
        return (((float) creditModifier / loanAmount) * loanPeriod) / 10;
    }


}
