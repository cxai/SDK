package com.cx.sdk.api.unittests;

import com.cx.sdk.api.CxClient;
import com.cx.sdk.api.CxClientException;
import com.cx.sdk.api.CxClientImpl;
import com.cx.sdk.api.SdkConfiguration;
import com.cx.sdk.api.dtos.*;
import com.cx.sdk.application.contracts.exceptions.NotAuthorizedException;
import com.cx.sdk.application.contracts.providers.*;
import com.cx.sdk.application.services.LoginService;
import com.cx.sdk.domain.Session;
import com.cx.sdk.domain.entities.EngineConfiguration;
import com.cx.sdk.domain.entities.Preset;
import com.cx.sdk.domain.entities.Team;
import com.cx.sdk.domain.enums.LoginType;
import com.cx.sdk.domain.exceptions.SdkException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by victork on 02/03/2017.
 */
public class CxClientImplTest {
    private final String USERNAME = "user";
    private final String PASSWORD = "pass";
    private final String PROJECT_NAME = "projectName";
    private final String TEAM_ID = "1";

    private Session session;

    private final LoginService loginService = mock(LoginService.class);
    private final SDKConfigurationProvider sdkConfigurationProvider = mock(SDKConfigurationProvider.class);
    private final ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
    private final PresetProvider presetProvider = mock(PresetProvider.class);
    private final TeamProvider teamProvider = mock(TeamProvider.class);
    private final ProjectProvider projectProvider = mock(ProjectProvider.class);


    private CxClient createClient() throws Exception {
        Constructor<?>[] constructors = CxClientImpl.class.getDeclaredConstructors();
        constructors[0].setAccessible(true);
        CxClient clientImp = (CxClientImpl) constructors[0].newInstance(
                loginService,
                sdkConfigurationProvider,
                configurationProvider,
                presetProvider,
                teamProvider,
                projectProvider);
        Field sessionField = CxClientImpl.class.getDeclaredField("singletonSession");
        sessionField.setAccessible(true);
        sessionField.set(clientImp, null);
        return clientImp;
    }

    private Session getSession(CxClientImpl cxClient) throws Exception {
        Field f = cxClient.getClass().getDeclaredField("singletonSession");
        f.setAccessible(true);
        Session session = (Session) f.get(cxClient);
        return session;
    }

    private void setSession(CxClientImpl cxClient, Session session) throws Exception{
        Field f = cxClient.getClass().getDeclaredField("singletonSession");
        f.setAccessible(true);
        f.set(cxClient, session);
    }

    @Before
    public void setup() {
        Map<String, String> cookies = new HashMap<>();
        cookies.put("key", "value");
        session = new Session("sessionId", cookies, true, true, true);
    }

    @Test
    public void createNewInstance_shouldSucceed_givenProvidedAllRequiredValues() {
        // Arrange
        SdkConfiguration configuration = mock(SdkConfiguration.class);
        when(configuration.getCxServerUrl()).thenReturn(getFakeUrl());
        when(configuration.getLoginType()).thenReturn(LoginTypeDTO.CREDENTIALS);
        // Act
        CxClient client = CxClientImpl.createNewInstance(configuration);

        // Assert
        assertNotNull(client);
    }

    @Test
    public void createNewInstance_shouldClearSingletonSession_givenSessionAlreadyExists() throws Exception {
        // Arrange
        SdkConfiguration configuration = mock(SdkConfiguration.class);
        when(configuration.getCxServerUrl()).thenReturn(getFakeUrl());
        when(configuration.getLoginType()).thenReturn(LoginTypeDTO.CREDENTIALS);
        CxClient client = CxClientImpl.createNewInstance(configuration);
        setSession((CxClientImpl)client, mock(Session.class));

        // Act
        client = CxClientImpl.createNewInstance(configuration);

        // Assert
         Session expectedSession = getSession((CxClientImpl)client);
        assertNull(expectedSession);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNewInstance_shouldThrow_givenMissingCxServerUrl() {
        // Arrange
        SdkConfiguration configuration = mock(SdkConfiguration.class);
        when(configuration.getLoginType()).thenReturn(LoginTypeDTO.CREDENTIALS);

        // Act & Assert
        CxClientImpl.createNewInstance(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNewInstance_shouldThrow_givenMissingLoginType() {
        // Arrange
        SdkConfiguration configuration = mock(SdkConfiguration.class);
        when(configuration.getCxServerUrl()).thenReturn(getFakeUrl());

        // Act & Assert
        CxClientImpl.createNewInstance(configuration);
    }

    @Test
    public void loginWithCredentials_shouldReturnSessionDto_givenSuccess() throws Exception {
        // Arrange
        CxClient client = createClient();
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);

        // Act
        SessionDTO result = client.login();

        // Assert
        validateSession(session, result);
    }

    @Test
    public void loginWithSSO_shouldReturnSessionDto_givenSuccess() throws Exception {
        // Arrange
        CxClient client = createClient();
        when(loginService.ssoLogin()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.SSO);

        // Act
        SessionDTO result = client.login();

        // Assert
        validateSession(session, result);
    }

    @Test
    public void loginWithSAML_shouldReturnSessionDto_givenSuccess() throws Exception {
        // Arrange
        CxClient client = createClient();
        when(loginService.samlLogin()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.SAML);

        // Act
        SessionDTO result = client.login();

        // Assert
        validateSession(session, result);
    }

    @Test
    public void getEngineConfigurations_shouldSucceed_givenValidSession() throws Exception {
        // Arrange
        CxClient client = createClient();
        setupIsLoggedInWithCredentials(client);
        List<EngineConfiguration> configs = new ArrayList<>();
        EngineConfiguration configuration = new EngineConfiguration("id", "name");
        configs.add(configuration);
        when(configurationProvider.getEngineConfigurations(session)).thenReturn(configs);

        // Act
        List<EngineConfigurationDTO> result = client.getEngineConfigurations();

        // Assert
        assertTrue(result.size() == 1);
        assertEquals(configs.get(0).getId(), configuration.getId());
        assertEquals(configs.get(0).getName(), configuration.getName());
    }

    @Test(expected = CxClientException.class)
    public void getEngineConfigurations_shouldFail_givenCannotLogin() throws Exception {
        // Arrange
        CxClient client = createClient();
        setupHasNoValidCredentials();

        // Act & Assert
        client.getEngineConfigurations();
    }

    @Test
    public void getPresets_shouldSucceed_givenValidSession() throws Exception {
        // Arrange
        CxClient client = createClient();
        setupIsLoggedInWithCredentials(client);
        List<Preset> presets = new ArrayList<>();
        Preset preset = new Preset("id", "name");
        presets.add(preset);
        when(presetProvider.getPresets(session)).thenReturn(presets);

        // Act
        List<PresetDTO> result = client.getPresets();

        // Assert
        assertTrue(result.size() == 1);
        assertEquals(presets.get(0).getId(), preset.getId());
        assertEquals(presets.get(0).getName(), preset.getName());
    }

    @Test(expected = CxClientException.class)
    public void getPresets_shouldFail_givenCannotLogin() throws Exception {
        // Arrange
        CxClient client = createClient();
        setupHasNoValidCredentials();

        // Act & Assert
        client.getPresets();
    }

    @Test
    public void getTeams_shouldSucceed_givenValidSession() throws Exception {
        // Arrange
        CxClient client = createClient();
        setupIsLoggedInWithCredentials(client);
        List<Team> teams = new ArrayList<>();
        Team team = new Team("id", "name");
        teams.add(team);
        when(teamProvider.getTeams(session)).thenReturn(teams);

        // Act
        List<TeamDTO> result = client.getTeams();

        // Assert
        assertTrue(result.size() == 1);
        assertEquals(teams.get(0).getId(), team.getId());
        assertEquals(teams.get(0).getName(), team.getName());
    }

    @Test(expected = CxClientException.class)
    public void getTeams_shouldFail_givenCannotLogin() throws Exception {
        // Arrange
        CxClient client = createClient();
        setupHasNoValidCredentials();

        // Act & Assert
        client.getTeams();
    }

    @Test
    public void handleAuthorizedActionInvoker_shouldLogin_givenHasNoSession() throws Exception {
        // Arrange
        CxClientImpl container = (CxClientImpl) createClient();
        CxClientImpl.AuthorizedActionInvoker<String> command = container.new AuthorizedActionInvoker();
        DummyInterface dummyInterface = mock(DummyInterface.class);
        String expectedResult = "my-result";
        when(dummyInterface.foo()).thenReturn(expectedResult);
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);

        // Act
        String result = command.invoke(dummyInterface::foo);

        // Assert
        assertEquals(result, expectedResult);
        verify(loginService).login();
    }

    @Test
    public void handleAuthorizedActionInvoker_shouldLogin_givenFailedDueToNotAuthenticated() throws Exception {
        // Arrange
        CxClientImpl container = (CxClientImpl) createClient();
        CxClientImpl.AuthorizedActionInvoker<String> command = container.new AuthorizedActionInvoker();
        DummyInterface dummyInterface = mock(DummyInterface.class);
        String expectedResult = "my-result";
        when(dummyInterface.foo())
                .thenThrow(new NotAuthorizedException("OMG!"))
                .thenReturn(expectedResult);
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);

        // Act
        String result = command.invoke(dummyInterface::foo);

        // Assert
        assertEquals(result, expectedResult);
        verify(loginService, times(2)).login();
    }

    @Test(expected = SdkException.class)
    public void handleAuthorizedActionInvoker_shouldThrow_givenUnhandledError() throws Exception {
        // Arrange
        CxClientImpl container = (CxClientImpl) createClient();
        CxClientImpl.AuthorizedActionInvoker<String> command = container.new AuthorizedActionInvoker();
        DummyInterface dummyInterface = mock(DummyInterface.class);
        when(dummyInterface.foo()).thenThrow(new SdkException("Runtime exception"));
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);

        // Act & Assert
        command.invoke(dummyInterface::foo);
    }

    private URL getFakeUrl() {
        try {
            return new URL("http://some-fake-url.com");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void validateSession(Session session, SessionDTO sessionDTO) {
        assertEquals(session.getSessionId(), sessionDTO.getSessionId());
        assertEquals(session.getCookies(), sessionDTO.getCookies());
        assertEquals(session.getIsScanner(), sessionDTO.getIsScanner());
        assertEquals(session.getIsAllowedToChangeNotExploitable(), sessionDTO.isAllowedToChangeNotExploitable());
    }

    private void setupIsLoggedInWithCredentials(CxClient cxClient) throws Exception {
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);
        cxClient.login();
    }

    private void setupHasNoValidCredentials() {
        when(loginService.login()).thenThrow(new SdkException("Login failed"));
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);
    }

    private interface DummyInterface {
        String foo() throws SdkException;
    }

    @Test
    public void validateProjectName_shouldSucceed_givenValidProjectName() throws Exception {
        // Arrange
        CxClientImpl container = (CxClientImpl) createClient();
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);
        when(projectProvider.isValidProjectName(session, PROJECT_NAME, TEAM_ID)).thenReturn(true);

        // Act & assert
        container.validateProjectName(PROJECT_NAME, TEAM_ID);
    }

    @Test(expected = CxClientException.class)
    public void validateProjectName_shouldThrow_givenInvalidProjectName() throws Exception {
        // Arrange
        CxClientImpl container = (CxClientImpl) createClient();
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);
        when(projectProvider.isValidProjectName(session, PROJECT_NAME, TEAM_ID)).thenReturn(false);

        // Act & assert
        container.validateProjectName(PROJECT_NAME, TEAM_ID);
    }

    @Test(expected = CxClientException.class)
    public void validateProjectName_shouldThrow_givenUnexpectedError() throws Exception {
        // Arrange
        CxClientImpl container = (CxClientImpl) createClient();
        when(loginService.login()).thenReturn(session);
        when(sdkConfigurationProvider.getLoginType()).thenReturn(LoginType.CREDENTIALS);
        when(projectProvider.isValidProjectName(session, PROJECT_NAME, TEAM_ID)).thenThrow(new SdkException("Unexpected Runtime exception"));

        // Act & assert
        container.validateProjectName(PROJECT_NAME, TEAM_ID);
    }
}