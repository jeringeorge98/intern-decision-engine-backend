package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DecisionEngineTest {

    @InjectMocks
    private DecisionEngine decisionEngine;

    private String debtorPersonalCode;
    private String segment1PersonalCode;
    private String segment2PersonalCode;
    private String segment3PersonalCode;
    private String noValidPersonalCode;

    @BeforeEach
    void setUp() {
        debtorPersonalCode = "49002010965";
        segment1PersonalCode = "49002010976";
        segment2PersonalCode = "49002010987";
        segment3PersonalCode = "49002010998";
        noValidPersonalCode = "37605031399";

    }

    @Test
    void testDebtorPersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException , InvalidAgeException {
    Decision decision = decisionEngine.calculateApprovedLoan(debtorPersonalCode,4000L,12,20);
    assertEquals(false,decision.getLoanApproval());
    assertEquals(0,decision.getLoanAmount());
    }

    @Test
    void testSegment1PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment1PersonalCode, 2000L, 24,20);
        assertEquals(2400, decision.getLoanAmount());
        assertEquals(24, decision.getLoanPeriod());
    }

    @Test
    void testSegment2PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 3000L, 36,20);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(36, decision.getLoanPeriod());
    }

    @Test
    void testSegment3PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment3PersonalCode, 4000L, 12,20);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testInvalidPersonalCode() {
        assertThrows(InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan(noValidPersonalCode, 4000L, 12,20));
    }

    @Test
    void testInvalidLoanAmount() {
        Long tooLowLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT - 1L;
        Long tooHighLoanAmount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT + 1L;

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooLowLoanAmount, 12,20));

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooHighLoanAmount, 12,20));
    }

    @Test
    void testInvalidLoanPeriod() {
        int tooShortLoanPeriod = DecisionEngineConstants.MINIMUM_LOAN_PERIOD - 1;
        int tooLongLoanPeriod = DecisionEngineConstants.MAXIMUM_LOAN_PERIOD + 1;

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooShortLoanPeriod,20));

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooLongLoanPeriod,20));
    }

    @Test
    void testFindSuitableLoanPeriod() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 2000L, 12,20);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testHighCreditScore() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException,InvalidAgeException{
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode,3000L,24,20);
        assertEquals(true,decision.getLoanApproval());
    }



    @Test
    void testWhenAgeIsLower(){
        int tooLowAge = DecisionEngineConstants.MIN_AGE_CUSTOMER - 1;
        int tooHighAge = DecisionEngineConstants.MAX_AGE_CUSTOMER + 1;

        assertThrows(InvalidAgeException.class,
                () -> decisionEngine.calculateApprovedLoan(segment2PersonalCode, 3000L, 12,tooLowAge));

        assertThrows(InvalidAgeException.class,
                () -> decisionEngine.calculateApprovedLoan(segment2PersonalCode, 3000L, 12,tooHighAge));
    }

    @Test
    void testNoValidLoanFound() {
        assertThrows(NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode,6000L,48,20));
    }

    }




