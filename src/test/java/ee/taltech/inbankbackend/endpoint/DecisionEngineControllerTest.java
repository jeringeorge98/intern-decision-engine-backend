package ee.taltech.inbankbackend.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.config.LoanStatus;
import ee.taltech.inbankbackend.exceptions.*;
import ee.taltech.inbankbackend.service.Decision;
import ee.taltech.inbankbackend.service.DecisionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class holds integration tests for the DecisionEngineController endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public class DecisionEngineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DecisionEngine decisionEngine;

    private ObjectMapper objectMapper;

    private String debtorPersonalCode;
    private String segment1PersonalCode;
    private String segment2PersonalCode;
    private String segment3PersonalCode;
    private String noValidPersonalCode;

    @BeforeEach
    public void setup() {

        objectMapper = new ObjectMapper();
        debtorPersonalCode = "37605030299";
        segment1PersonalCode = "50307172740";
        segment2PersonalCode = "38411266610";
        segment3PersonalCode = "35006069515";
        noValidPersonalCode = "37605031399";
        objectMapper = new ObjectMapper();
    }

    /**
     * This method tests the /loan/decision endpoint with valid inputs.
     */

    /**
     * This test tests the happy path
     * @throws Exception
     * @throws InvalidLoanPeriodException
     * @throws NoValidLoanException
     * @throws InvalidPersonalCodeException
     * @throws InvalidLoanAmountException
     * @throws InvalidAgeException
     */
    @Test
    public void givenValidRequest_whenRequestDecision_thenReturnsExpectedResponse()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        Decision decision = new Decision(true,1000, 12, 24,null);
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt())).thenReturn(decision);

        DecisionRequest request = new DecisionRequest("1234", 10L, 10,24);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").value(1000))
                .andExpect(jsonPath("$.loanPeriod").value(12))
                .andExpect(jsonPath("$.errorMessage").isEmpty())
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert Objects.equals(response.getLoanApproval(), LoanStatus.APPROVED.toString());
        assert response.getLoanAmount() == 1000;
        assert response.getLoanPeriod() == 12;
        assert response.getErrorMessage() == null;
    }

    /**
     * This test tests the negative path of the OK response
     * @throws Exception
     * @throws InvalidLoanPeriodException
     * @throws NoValidLoanException
     * @throws InvalidPersonalCodeException
     * @throws InvalidLoanAmountException
     * @throws InvalidAgeException
     */
    @Test
    public void givenDebtorRequest_whenRequestDecision_thenReturnsExpectedResponse()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        Decision decision = new Decision(false,null, null, 24,null);
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt())).thenReturn(decision);

        DecisionRequest request = new DecisionRequest(debtorPersonalCode, 10L, 10,24);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert Objects.equals(response.getLoanApproval(), LoanStatus.REJECTED.toString());
        assert response.getLoanPeriod() == null;
    }


    @Test
    public void givenValidRequest_whenCreditScoreIsLow_thenReturnsExpectedResponse()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        // Create a decision with approved=false and appropriate error message
        Decision decision = new Decision(false, null, null, 24,"Credit Score is too low");
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt())).thenReturn(decision);

        DecisionRequest request = new DecisionRequest(segment1PersonalCode, 8000L, 12,24);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.loanApproval").value(LoanStatus.REJECTED.toString()))
                .andExpect(jsonPath("$.errorMessage").value("Credit Score is too low"))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanApproval().equals(LoanStatus.REJECTED.toString());
        assert response.getLoanAmount() == null;
        assert response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("Credit Score is too low");
    }

    @Test
    public void givenValidRequest_whenCreditScoreIsHigh_thenReturnsExpectedResponse()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException, InvalidAgeException {
        // Create a decision with approved=true for high credit score
        Decision decision = new Decision(true, 3000, 24, 18,null);
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt())).thenReturn(decision);

        DecisionRequest request = new DecisionRequest(segment3PersonalCode, 3000L, 24,18);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").value(3000))
                .andExpect(jsonPath("$.loanPeriod").value(24))
                .andExpect(jsonPath("$.loanApproval").value(LoanStatus.APPROVED.toString()))
                .andExpect(jsonPath("$.errorMessage").isEmpty())
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanApproval().equals(LoanStatus.APPROVED.toString());
        assert response.getLoanAmount() == 3000;
        assert response.getLoanPeriod() == 24;
        assert response.getErrorMessage() == null;
    }





    @Test
    public void givenValidRequest_whenDecisionIsNegative_thenReturnsExpectedResponse()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        Decision decision = new Decision(false,0, 0,20, "User has debt");
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt())).thenReturn(decision);

        DecisionRequest request = new DecisionRequest("1234", 10L, 10,20);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").value(0))
                .andExpect(jsonPath("$.loanPeriod").value(0))
                .andExpect(jsonPath("$.errorMessage").isNotEmpty())
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert Objects.equals(response.getLoanApproval(), LoanStatus.REJECTED.toString());
        assert response.getLoanAmount() == 0;
        assert response.getLoanPeriod() == 0;
        assert response.getErrorMessage().equals("User has debt");
    }


    /**
     * This test ensures that if an invalid personal code is provided, the controller returns
     * an HTTP Bad Request (400) response with the appropriate error message in the response body.
     */
    @Test
    public void givenInvalidPersonalCode_whenRequestDecision_thenReturnsBadRequest()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt()))
                .thenThrow(new InvalidPersonalCodeException("Invalid personal code"));

        DecisionRequest request = new DecisionRequest("1234", 10L, 10,20);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("Invalid personal code"))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanAmount() == null;
        assert response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("Invalid personal code");
    }

    /**
     * This test ensures that if an invalid loan amount is provided, the controller returns
     * an HTTP Bad Request (400) response with the appropriate error message in the response body.
     */
    @Test
    public void givenInvalidLoanAmount_whenRequestDecision_thenReturnsBadRequest()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt()))
                .thenThrow(new InvalidLoanAmountException("Invalid loan amount"));

        DecisionRequest request = new DecisionRequest("1234", 10L, 10,20);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("Invalid loan amount"))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanAmount() == null;
        assert response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("Invalid loan amount");
    }

    /**
     * This test ensures that if an invalid loan period is provided, the controller returns
     * an HTTP Bad Request (400) response with the appropriate error message in the response body.
     */
    @Test
    public void givenInvalidLoanPeriod_whenRequestDecision_thenReturnsBadRequest()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt()))
                .thenThrow(new InvalidLoanPeriodException("Invalid loan period"));

        DecisionRequest request = new DecisionRequest("1234", 10L, 10,20);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("Invalid loan period"))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanAmount() == null;
        assert response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("Invalid loan period");
    }

    /**
     * This test ensures that if no valid loan is found, the controller returns
     * an HTTP Bad Request (400) response with the appropriate error message in the response body.
     */
    @Test
    public void givenNoValidLoan_whenRequestDecision_thenReturnsBadRequest()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt()))
                .thenThrow(new NoValidLoanException("No valid loan available"));

        DecisionRequest request = new DecisionRequest(segment1PersonalCode, 6000L, 48,20);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("No valid loan available"))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanAmount() == null;
        assert response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("No valid loan available");
    }

    /**
     * This test ensures that if an invalid age is entered then a 404 bad request is returned to the user with the appropriate message
     */
    @Test
    public void givenInValidAge_whenRequestDecision_thenReturnsBadRequestError()
            throws Exception, InvalidLoanPeriodException, InvalidAgeException, InvalidLoanAmountException, NoValidLoanException, InvalidPersonalCodeException {
        int invalidAge = DecisionEngineConstants.MAX_AGE_CUSTOMER +1;
        when(decisionEngine.calculateApprovedLoan(anyString(),anyLong(),anyInt(),anyInt())).thenThrow(new InvalidAgeException("Not in the Valid Age Range"));

        DecisionRequest request = new DecisionRequest(segment1PersonalCode, 8000L, 12,invalidAge);
        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("Not in the Valid Age Range"))
                .andReturn();
        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert  response.getLoanAmount() == null;
        assert  response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("Not in the Valid Age Range");

    }

    /**
     * This test ensures that if an unexpected error occurs when processing the request, the controller returns
     * an HTTP Internal Server Error (500) response with the appropriate error message in the response body.
     */
    @Test
    public void givenUnexpectedError_whenRequestDecision_thenReturnsInternalServerError()
            throws Exception, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException,
            InvalidLoanAmountException,InvalidAgeException {
        when(decisionEngine.calculateApprovedLoan(anyString(), anyLong(), anyInt(),anyInt())).thenThrow(new RuntimeException());

        DecisionRequest request = new DecisionRequest("1234", 10L, 10,20);

        MvcResult result = mockMvc.perform(post("/loan/decision")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loanAmount").isEmpty())
                .andExpect(jsonPath("$.loanPeriod").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("An unexpected error occurred"))
                .andReturn();

        DecisionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DecisionResponse.class);
        assert response.getLoanAmount() == null;
        assert response.getLoanPeriod() == null;
        assert response.getErrorMessage().equals("An unexpected error occurred");
    }
}
