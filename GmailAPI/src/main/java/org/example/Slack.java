package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;



import java.util.concurrent.TimeUnit;

public class Slack {

    public static void main(String[] args) throws Exception {
        // Set up WebDriver
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // Open the URL
        driver.get("https://google.com"); //enter your website url

        // Initialize Page Objects

        GmailPage gmailPage = new GmailPage(); // Initialize as needed

        // Retrieve URL from email
        String extractedUrl = gmailPage.getEmailUrl(); // Get the extracted URL
        if (extractedUrl != null) {
            System.out.println("Navigating to URL: " + extractedUrl);
            driver.get(extractedUrl);

        } else {
            System.out.println("No URL found in the email.");
        }

        // Close the driver
        driver.quit();
    }
}