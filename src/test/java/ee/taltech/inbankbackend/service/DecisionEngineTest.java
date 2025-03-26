package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DecisionEngine service.
 * These tests verify the loan approval calculation logic for different
 * customer segments, input validation, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class DecisionEngineTest {

    @InjectMocks
    private DecisionEngine decisionEngine;

    // Test personal codes for different customer segments
    private String debtorPersonalCode;    // Customer with debt (credit modifier 0)
    private String segment1PersonalCode;  // Customer in segment 1 (credit modifier 100)
    private String segment2PersonalCode;  // Customer in segment 2 (credit modifier 300)
    private String segment3PersonalCode;  // Customer in segment 3 (credit modifier 1000)
    private String noValidPersonalCode;   // Invalid personal code

    @BeforeEach
    void setUp() {
        debtorPersonalCode = "49002010965";
        segment1PersonalCode = "49002010976";
        segment2PersonalCode = "49002010987";
        segment3PersonalCode = "49002010998";
        noValidPersonalCode = "37605031399";

    }

    /**
     * Tests that a customer with a debtor personal code (credit modifier 0)
     * is rejected for a loan with appropriate values returned.
     */
    @Test
    void testDebtorPersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException , InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(debtorPersonalCode,4000L,12,20);
        assertEquals(false,decision.getLoanApproval());
        assertEquals(0,decision.getLoanAmount());
    }

    /**
     * Tests that a customer with a segment 1 personal code (credit modifier 100)
     * gets appropriate loan amount and period approved.
     */
    @Test
    void testSegment1PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment1PersonalCode, 2000L, 24,20);
        assertEquals(2400, decision.getLoanAmount());
        assertEquals(24, decision.getLoanPeriod());
    }

    /**
     * Tests that a customer with a segment 2 personal code (credit modifier 300)
     * gets appropriate loan amount and period approved, capped at maximum loan amount.
     */
    @Test
    void testSegment2PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 3000L, 36,20);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(36, decision.getLoanPeriod());
    }

    /**
     * Tests that a customer with a segment 3 personal code (credit modifier 1000)
     * gets appropriate loan amount and period approved, capped at maximum loan amount.
     */
    @Test
    void testSegment3PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment3PersonalCode, 4000L, 12,20);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    /**
     * Tests that an invalid personal code throws InvalidPersonalCodeException.
     * Verifies the validation of Estonian personal ID codes.
     */
    @Test
    void testInvalidPersonalCode() {
        assertThrows(InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan(noValidPersonalCode, 4000L, 12,20));
    }

    /**
     * Tests that loan amounts below minimum and above maximum throw InvalidLoanAmountException.
     * Verifies the boundary validation for loan amounts.
     */
    @Test
    void testInvalidLoanAmount() {
        Long tooLowLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT - 1L;
        Long tooHighLoanAmount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT + 1L;

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooLowLoanAmount, 12,20));

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooHighLoanAmount, 12,20));
    }

    /**
     * Tests that loan periods below minimum and above maximum throw InvalidLoanPeriodException.
     * Verifies the boundary validation for loan periods.
     */
    @Test
    void testInvalidLoanPeriod() {
        int tooShortLoanPeriod = DecisionEngineConstants.MINIMUM_LOAN_PERIOD - 1;
        int tooLongLoanPeriod = DecisionEngineConstants.MAXIMUM_LOAN_PERIOD + 1;

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooShortLoanPeriod,20));

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooLongLoanPeriod,20));
    }

    /**
     * Tests that the system can calculate a suitable loan amount
     * based on the credit modifier and requested period. 
     * This test validates that a segment 2 customer can get an appropriate loan amount
     * without having to increase the period.
     */
    @Test
    void testFindSuitableLoanPeriod() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 2000L, 12,20);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    /**
     * Tests that a customer with high credit score gets loan approval.
     * This test verifies that a segment 2 customer with suitable loan parameters
     * receives approval for their loan request.
     */
    @Test
    void testHighCreditScore() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException{
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode,3000L,24,20);
        assertEquals(true,decision.getLoanApproval());
    }



    /**
     * Tests age validation by verifying that ages outside allowed range
     * throw InvalidAgeException. Tests both lower and upper boundaries.
     */
    @Test
    void testWhenAgeIsLower(){
        int tooLowAge = DecisionEngineConstants.MIN_AGE_CUSTOMER - 1;
        int tooHighAge = DecisionEngineConstants.MAX_AGE_CUSTOMER + 1;

        assertThrows(InvalidAgeException.class,
                () -> decisionEngine.calculateApprovedLoan(segment2PersonalCode, 3000L, 12,tooLowAge));

        assertThrows(InvalidAgeException.class,
                () -> decisionEngine.calculateApprovedLoan(segment2PersonalCode, 3000L, 12,tooHighAge));
    }

    /**
     * Tests that NoValidLoanException is thrown when no valid loan can be found.
     * This happens when credit score requirements can't be met even with period extension.
     * This test case uses a segment 1 customer with high loan amount and maximum period.
     */
    @Test
    void testNoValidLoanFound() {
        assertThrows(NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode,6000L,48,20));
    }

    }




