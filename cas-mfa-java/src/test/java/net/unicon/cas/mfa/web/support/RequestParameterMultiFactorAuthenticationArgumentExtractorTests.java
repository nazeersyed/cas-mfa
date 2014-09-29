package net.unicon.cas.mfa.web.support;

import net.unicon.cas.mfa.authentication.AuthenticationMethod;
import net.unicon.cas.mfa.authentication.DefaultAuthenticationMethodConfigurationProvider;
import org.jasig.cas.web.support.ArgumentExtractor;
import org.jasig.cas.web.support.CasArgumentExtractor;
import org.jasig.cas.web.support.SamlArgumentExtractor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class RequestParameterMultiFactorAuthenticationArgumentExtractorTests {

    private final Set<ArgumentExtractor> supportedArgumentExtractors;

    private final MfaWebApplicationServiceFactory mfaWebApplicationServiceFactory;

    public RequestParameterMultiFactorAuthenticationArgumentExtractorTests() {
        this.supportedArgumentExtractors = new HashSet<ArgumentExtractor>();
        this.supportedArgumentExtractors.add(new CasArgumentExtractor());
        this.supportedArgumentExtractors.add(new SamlArgumentExtractor());
        this.mfaWebApplicationServiceFactory = new DefaultMfaWebApplicationServiceFactory(true, null);
    }

    /**
     * When login presents no authentication method, the extractor extracts a null service.
     */
    @Test
    public void testMissingAuthenticationMethodParameterYieldsNullService() {

        // let's say we support all sorts of interesting authentication methods,
        // but this login request isn't going to require any of these
        final SortedSet<AuthenticationMethod> validAuthenticationMethods =
                new TreeSet<AuthenticationMethod>();
        validAuthenticationMethods.add(new AuthenticationMethod("fingerprint", 1));
        validAuthenticationMethods.add(new AuthenticationMethod("retina_scan", 2));
        validAuthenticationMethods.add(new AuthenticationMethod("personal_attestation", 3));
        validAuthenticationMethods.add(new AuthenticationMethod("strong_two_factor", 4));

        final DefaultAuthenticationMethodConfigurationProvider loader = new DefaultAuthenticationMethodConfigurationProvider(validAuthenticationMethods);

        final RequestParameterMultiFactorAuthenticationArgumentExtractor extractor =
                new RequestParameterMultiFactorAuthenticationArgumentExtractor(this.supportedArgumentExtractors,
                        this.mfaWebApplicationServiceFactory, new DefaultAuthenticationMethodVerifier(loader));

        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getParameter("service")).thenReturn("https://www.github.com");

        // let's say the authn_method request parameter is missing outright
        when(request.getParameter(MultiFactorAuthenticationSupportingWebApplicationService.CONST_PARAM_AUTHN_METHOD))
                .thenReturn(null);

        assertNull(extractor.extractService(request));
    }


    /**
     * When login presents an unrecognized authentication method, the extractor extracts a null service.
     */
    @Test(expected = UnrecognizedAuthenticationMethodException.class)
    public void testUnrecognizedAuthenticationMethodParameterYieldsNullService() {
        final DefaultAuthenticationMethodConfigurationProvider loader = new DefaultAuthenticationMethodConfigurationProvider();

        final RequestParameterMultiFactorAuthenticationArgumentExtractor extractor =
                new RequestParameterMultiFactorAuthenticationArgumentExtractor(this.supportedArgumentExtractors,
                        this.mfaWebApplicationServiceFactory, new DefaultAuthenticationMethodVerifier(loader));


        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getParameter("service")).thenReturn("https://www.github.com");
        when(request.getParameter(MultiFactorAuthenticationSupportingWebApplicationService.CONST_PARAM_AUTHN_METHOD))
                .thenReturn("unrecognized_authentication_method");

        extractor.extractService(request);
    }

    /**
     * When login presents the one recognized authentication method, extractor extracts a service conveying the
     * required authentication method.
     */
    @Test
    public void testRecognizedAuthenticationMethodParameterYieldsAuthenticationMethodRequiringService() {
        final SortedSet<AuthenticationMethod> validAuthenticationMethods =
                new TreeSet<AuthenticationMethod>();
        validAuthenticationMethods.add(new AuthenticationMethod("strong_two_factor", 1));
        final DefaultAuthenticationMethodConfigurationProvider loader = new DefaultAuthenticationMethodConfigurationProvider(validAuthenticationMethods);

        final RequestParameterMultiFactorAuthenticationArgumentExtractor extractor =
                new RequestParameterMultiFactorAuthenticationArgumentExtractor(this.supportedArgumentExtractors,
                        this.mfaWebApplicationServiceFactory, new DefaultAuthenticationMethodVerifier(loader));

        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getParameter("service")).thenReturn("https://www.github.com");
        when(request.getParameter(MultiFactorAuthenticationSupportingWebApplicationService.CONST_PARAM_AUTHN_METHOD))
                .thenReturn("strong_two_factor");

        assertTrue(extractor.extractService(request) instanceof MultiFactorAuthenticationSupportingWebApplicationService);

         final MultiFactorAuthenticationSupportingWebApplicationService authenticationMethodRequiringService =
                (MultiFactorAuthenticationSupportingWebApplicationService) extractor.extractService(request);

        assertEquals("strong_two_factor", authenticationMethodRequiringService.getAuthenticationMethod());
    }

    /**
     * When login presents a recognized authentication method among several supported methods,
     * extractor extracts a service conveying the required authentication method.
     */
    @Test
    public void testRecognizedAuthenticationMethodParamAmongMultipleSupportedYieldsService() throws IOException {

        // this is a bit of testing paranoia, but always want to check that one item isn't an edge case
        final SortedSet<AuthenticationMethod> validAuthenticationMethods =
                new TreeSet<AuthenticationMethod>();
        validAuthenticationMethods.add(new AuthenticationMethod("fingerprint", 1));
        validAuthenticationMethods.add(new AuthenticationMethod("retina_scan", 2));
        validAuthenticationMethods.add(new AuthenticationMethod("personal_attestation", 3));
        validAuthenticationMethods.add(new AuthenticationMethod("strong_two_factor", 4));

        final DefaultAuthenticationMethodConfigurationProvider loader = new DefaultAuthenticationMethodConfigurationProvider(validAuthenticationMethods);

        final RequestParameterMultiFactorAuthenticationArgumentExtractor extractor =
                new RequestParameterMultiFactorAuthenticationArgumentExtractor(this.supportedArgumentExtractors,
                        this.mfaWebApplicationServiceFactory, new DefaultAuthenticationMethodVerifier(
                        loader));


        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getParameter("service")).thenReturn("https://www.github.com");
        when(request.getParameter(MultiFactorAuthenticationSupportingWebApplicationService.CONST_PARAM_AUTHN_METHOD))
                .thenReturn("personal_attestation");

        assertTrue(extractor.extractService(request) instanceof MultiFactorAuthenticationSupportingWebApplicationService);

        final MultiFactorAuthenticationSupportingWebApplicationService authenticationMethodRequiringService =
                (MultiFactorAuthenticationSupportingWebApplicationService) extractor.extractService(request);

        assertEquals("personal_attestation", authenticationMethodRequiringService.getAuthenticationMethod());
    }

    /**
     * When login presents no service parameter, extractor extracts a null service.
     */
    @Test
    public void testMissingServiceParameterYieldsNullService() {

        // this is a bit of testing paranoia, but always want to check that one item isn't an edge case
        final SortedSet<AuthenticationMethod> validAuthenticationMethods =
                new TreeSet<AuthenticationMethod>();
        validAuthenticationMethods.add(new AuthenticationMethod("fingerprint", 1));
        validAuthenticationMethods.add(new AuthenticationMethod("retina_scan", 2));
        validAuthenticationMethods.add(new AuthenticationMethod("personal_attestation", 3));
        validAuthenticationMethods.add(new AuthenticationMethod("strong_two_factor", 4));

        final DefaultAuthenticationMethodConfigurationProvider loader = new DefaultAuthenticationMethodConfigurationProvider(validAuthenticationMethods);
        final RequestParameterMultiFactorAuthenticationArgumentExtractor extractor =
                new RequestParameterMultiFactorAuthenticationArgumentExtractor(this.supportedArgumentExtractors,
                        this.mfaWebApplicationServiceFactory, new DefaultAuthenticationMethodVerifier(loader));


        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getParameter("service")).thenReturn(null);
        when(request.getParameter(MultiFactorAuthenticationSupportingWebApplicationService.CONST_PARAM_AUTHN_METHOD))
                .thenReturn("personal_attestation");

        assertNull(extractor.extractService(request));

    }

    @Test
    public void testRecognizedAuthenticationMethodParameterInSamlRequest() {
        final SortedSet<AuthenticationMethod> validAuthenticationMethods =
                new TreeSet<AuthenticationMethod>();
        validAuthenticationMethods.add(new AuthenticationMethod("strong_two_factor", 1));
        final DefaultAuthenticationMethodConfigurationProvider loader = new DefaultAuthenticationMethodConfigurationProvider(validAuthenticationMethods);

        final RequestParameterMultiFactorAuthenticationArgumentExtractor extractor =
                new RequestParameterMultiFactorAuthenticationArgumentExtractor(this.supportedArgumentExtractors,
                        this.mfaWebApplicationServiceFactory, new DefaultAuthenticationMethodVerifier(loader));


        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getParameter("TARGET")).thenReturn("https://www.github.com");
        when(request.getParameter(MultiFactorAuthenticationSupportingWebApplicationService.CONST_PARAM_AUTHN_METHOD))
                .thenReturn("strong_two_factor");

        assertTrue(extractor.extractService(request) instanceof MultiFactorAuthenticationSupportingWebApplicationService);

        final MultiFactorAuthenticationSupportingWebApplicationService authenticationMethodRequiringService =
                (MultiFactorAuthenticationSupportingWebApplicationService) extractor.extractService(request);

        assertEquals("strong_two_factor", authenticationMethodRequiringService.getAuthenticationMethod());
    }
}