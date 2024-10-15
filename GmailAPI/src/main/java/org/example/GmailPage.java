package org.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GmailPage {

    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "C:\\Users\\user\\credentials_main.json"; // Update the path for your credentials
    private String extractedUrl; // To store the extracted URL

    private static Credential authorize() throws IOException, GeneralSecurityException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(CREDENTIALS_FILE_PATH));

        List<String> scopes = Collections.singletonList(GmailScopes.GMAIL_READONLY);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, scopes)
                .setAccessType("offline")
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens"))) // Store tokens
                .build();

        Credential credential = flow.loadCredential("user");
        if (credential == null) {
            String authorizationUrl = flow.newAuthorizationUrl()
                    .setRedirectUri("http://localhost")
                    .build();
            System.out.println("Please open the following URL in your browser:");
            System.out.println(authorizationUrl);
            System.out.println("Enter the authorization code:");

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String code = br.readLine();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri("http://localhost")
                    .execute();

            credential = flow.createAndStoreCredential(tokenResponse, "user");
        }
        return credential;
    }

    private static Gmail getGmailService() throws IOException, GeneralSecurityException {
        Credential credential = authorize();
        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public String getEmailUrl() {
        try {
            Gmail service = getGmailService();
            String subjectQuery = "subject:\"Meet the team behind Slack’s mobile experience\"";
            ListMessagesResponse messagesResponse = service.users().messages().list("me")
                    .setQ(subjectQuery)
                    .setMaxResults(1L)
                    .execute();

            List<Message> messages = messagesResponse.getMessages();
            if (messages != null && !messages.isEmpty()) {
                Message latestMessage = messages.get(0);
                Message fullMessage = service.users().messages().get("me", latestMessage.getId()).execute();

                // Extract URL
                extractedUrl = getEmailBody(fullMessage);
                if (extractedUrl != null) {
                    System.out.println("Extracted URL: " + extractedUrl);
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return extractedUrl; // Return the extracted URL
    }

    private static String getEmailBody(Message message) {
        if (message.getPayload() != null) {
            return processMessagePart(message.getPayload());
        }
        return null;
    }

    private static String processMessagePart(MessagePart part) {
        String contentType = part.getMimeType();

        if (part.getParts() != null && !part.getParts().isEmpty()) {
            for (MessagePart subPart : part.getParts()) {
                String url = processMessagePart(subPart);
                if (url != null) {
                    return url; // Return the first found URL
                }
            }
        } else if ("text/plain".equals(contentType) || "text/html".equals(contentType)) {
            String bodyData = part.getBody() != null ? part.getBody().getData() : null;
            if (bodyData != null) {
                String decodedData = decodeBase64(bodyData);
                return extractSpecificUrl(decodedData); // Extract specific URL
            }
        }
        return null; // No URL found
    }

    private static String extractSpecificUrl(String html) {
        String regex = "https://click\\\\.email\\\\.slackhq\\\\.com/\\\\?qs=[^\\\\s]+\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(0); // Return the matched specific URL
        }

        return null; // Return null if no specific URL is found
    }

    private static String decodeBase64(String base64String) {
        try {
            base64String = base64String.replace('-', '+').replace('_', '/').replaceAll("\\s+", "");
            int padding = base64String.length() % 4;
            if (padding > 0) {
                base64String += "=".repeat(4 - padding);
            }

            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            return new String(decodedBytes, "UTF-8");
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            System.err.println("Failed to decode base64 string: " + e.getMessage());
            return null;
        }
    }
}
