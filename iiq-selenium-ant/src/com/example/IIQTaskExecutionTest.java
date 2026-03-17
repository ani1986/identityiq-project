package com.example.iiq;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;
import java.util.List;

public class IIQTaskExecutionTest {

    public static void main(String[] args) {
        WebDriver driver = null;

        String baseUrl = System.getenv().getOrDefault("IIQ_BASE_URL", "http://localhost:8080/identityiq");
        String username = System.getenv().getOrDefault("IIQ_USERNAME", "spadmin");
        String password = System.getenv().getOrDefault("IIQ_PASSWORD", "Virtual@123");
        String taskName = System.getenv().getOrDefault("IIQ_TASK_NAME", "HRMS Employee Aggregation");

        try {
            ChromeOptions options = new ChromeOptions();

options.addArguments("--headless=new");
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");
options.addArguments("--user-data-dir=/tmp/selenium-chrome-" + System.currentTimeMillis());
            configureChromeBinaryIfProvided(options);
            configureChromeDriverIfProvided();

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            login(driver, wait, baseUrl, username, password);
            openTasksPage(driver, wait, baseUrl);
            WebElement taskElement = openTask(driver, wait, taskName);
            executeTask(driver, wait, taskElement, baseUrl);
            verifyTaskResult(driver, wait, baseUrl, taskName);

            System.out.println("TEST PASSED");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("TEST FAILED: " + e.getMessage());
            System.exit(1);

        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
            killChromeProcesses();
        }
    }

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless=new");
        options.addArguments("--window-size=1600,1000");
        options.addArguments("--disable-notifications");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        return options;
    }

    private static void configureChromeBinaryIfProvided(ChromeOptions options) {
        String chromeBinary = System.getenv("CHROME_BINARY");
        if (chromeBinary != null && !chromeBinary.isBlank()) {
            System.out.println("Using Chrome binary: " + chromeBinary);
            options.setBinary(chromeBinary);
        }
    }

    private static void configureChromeDriverIfProvided() {
        String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromeDriverPath != null && !chromeDriverPath.isBlank()) {
            System.out.println("Using ChromeDriver: " + chromeDriverPath);
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        }
    }

    private static void killChromeProcesses() {
        runCleanupCommand("pkill -f chromedriver");
        runCleanupCommand("pkill -f 'chrome|chromium'");
    }

    private static void runCleanupCommand(String command) {
        try {
            new ProcessBuilder("bash", "-c", command).start().waitFor();
        } catch (Exception ignored) {
        }
    }

    private static void login(WebDriver driver, WebDriverWait wait,
                              String baseUrl, String username, String password) {
        driver.get(baseUrl + "/login.jsf");

        System.out.println("Page title: " + driver.getTitle());
        System.out.println("Current URL: " + driver.getCurrentUrl());

        WebElement userField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("loginForm:accountId")));

        WebElement passField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("loginForm:password")));

        userField.clear();
        userField.sendKeys(username);

        passField.clear();
        passField.sendKeys(password);

        WebElement loginButton;
        try {
            loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("loginForm:loginButton")));
        } catch (Exception e) {
            loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//input[@type='submit' or @id='loginForm:loginButton'] | //button[@id='loginForm:loginButton']")));
        }

        loginButton.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        System.out.println("Login submitted");
    }

    private static void openTasksPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        driver.get(baseUrl + "/monitor/tasks/viewTasks.jsf");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        System.out.println("Opened Tasks page");
    }

    private static WebElement openTask(WebDriver driver, WebDriverWait wait, String taskName) {
        searchIfPresent(driver, wait, taskName);

        List<By> taskLocators = List.of(
                By.xpath("//a[normalize-space()='" + taskName + "']"),
                By.xpath("//*[normalize-space(text())='" + taskName + "']"),
                By.xpath("//tr[.//*[normalize-space(text())='" + taskName + "']]"),
                By.xpath("//div[.//*[normalize-space(text())='" + taskName + "']]")
        );

        for (By locator : taskLocators) {
            try {
                WebElement taskElement = wait.until(ExpectedConditions.elementToBeClickable(locator));
                taskElement.click();
                sleep(500);
                System.out.println("Clicked task: " + taskName);
                return taskElement;
            } catch (Exception ignored) {
            }
        }

        throw new RuntimeException("Could not open task: " + taskName);
    }

    private static void executeTask(WebDriver driver, WebDriverWait wait, WebElement taskElement, String baseUrl) {
        List<By> executeLocators = List.of(
                By.xpath("//*[self::button or self::a or self::span][normalize-space()='Execute']"),
                By.xpath("//*[self::button or self::a][contains(normalize-space(.),'Execute')]"),
                By.xpath("//*[contains(@title,'Execute')]"),
                By.xpath("//*[contains(@aria-label,'Execute')]")
        );

        for (By locator : executeLocators) {
            try {
                WebElement executeButton = wait.until(ExpectedConditions.elementToBeClickable(locator));
                executeButton.click();
                System.out.println("Clicked Execute");
                confirmIfPresent(driver, wait);
                sleep(1000);

                String currentUrl = driver.getCurrentUrl();
                System.out.println("URL after execute: " + currentUrl);

                if (currentUrl.contains("/monitor/tasks/taskResults.jsf")) {
                    System.out.println("Detected broken redirect to taskResults.jsf, returning to valid page");
                    driver.get(baseUrl + "/monitor/tasks/viewTasks.jsf");
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                }
                return;
            } catch (Exception ignored) {
            }
        }

        List<By> actionLocators = List.of(
                By.xpath("//*[self::button or self::a or self::span][contains(normalize-space(.),'Actions')]"),
                By.xpath("//*[contains(@title,'Actions')]"),
                By.xpath("//*[contains(@class,'menu') or contains(@class,'gear') or contains(@class,'action')]")
        );

        for (By locator : actionLocators) {
            try {
                WebElement actions = wait.until(ExpectedConditions.elementToBeClickable(locator));
                actions.click();
                sleep(500);

                WebElement executeMenu = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[self::a or self::span or self::div][normalize-space()='Execute' or contains(normalize-space(.),'Execute')]")));
                executeMenu.click();
                System.out.println("Clicked Execute from Actions menu");
                confirmIfPresent(driver, wait);
                sleep(1000);

                String currentUrl = driver.getCurrentUrl();
                System.out.println("URL after execute menu click: " + currentUrl);

                if (currentUrl.contains("/monitor/tasks/taskResults.jsf")) {
                    System.out.println("Detected broken redirect to taskResults.jsf, returning to valid page");
                    driver.get(baseUrl + "/monitor/tasks/viewTasks.jsf");
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                }
                return;
            } catch (Exception ignored) {
            }
        }

        try {
            Actions actions = new Actions(driver);
            actions.doubleClick(taskElement).perform();
            System.out.println("Tried double click on task row");
            confirmIfPresent(driver, wait);
            sleep(1000);

            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.contains("/monitor/tasks/taskResults.jsf")) {
                driver.get(baseUrl + "/monitor/tasks/viewTasks.jsf");
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            }
            return;
        } catch (Exception ignored) {
        }

        throw new RuntimeException("Could not find any Execute action for selected task");
    }

    private static void confirmIfPresent(WebDriver driver, WebDriverWait wait) {
        List<By> confirmLocators = List.of(
                By.xpath("//*[self::button or self::a or self::span][normalize-space()='OK']"),
                By.xpath("//*[self::button or self::a or self::span][normalize-space()='Yes']"),
                By.xpath("//*[self::button or self::a or self::span][contains(normalize-space(.),'Confirm')]"),
                By.xpath("//input[@type='submit' and (@value='OK' or @value='Yes')]")
        );

        for (By locator : confirmLocators) {
            try {
                WebElement confirm = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(locator));
                confirm.click();
                System.out.println("Confirmed execution");
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static void verifyTaskResult(WebDriver driver, WebDriverWait wait,
                                         String baseUrl, String taskName) {
        long timeoutAt = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();

        if (driver.getCurrentUrl().contains("/monitor/tasks/taskResults.jsf") || is404Page(driver)) {
            driver.get(baseUrl + "/monitor/tasks/viewTasks.jsf");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        }

        openTaskResultsTab(driver, wait);

        while (System.currentTimeMillis() < timeoutAt) {
            try {
                System.out.println("Polling task result for: " + taskName);

                dumpTaskResultsPageElements(driver);

                WebElement searchBox = findTaskResultsSearchBox(driver, wait);
                if (searchBox == null) {
                    throw new RuntimeException("Could not find Task Results search box");
                }

                searchTaskResult(driver, wait, searchBox, taskName);

                WebElement resultTarget = findTaskResultTarget(driver, wait, taskName);
                if (resultTarget == null) {
                    System.out.println("Task result target not found yet for: " + taskName);
                    sleep(1000);
                    driver.navigate().refresh();
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                    openTaskResultsTab(driver, wait);
                    continue;
                }

                System.out.println("Clicking task result target: " + resultTarget.getText());
                clickElement(driver, resultTarget);
                sleep(1500);

                String pageText = driver.findElement(By.tagName("body")).getText();
                System.out.println("TASK RESULT DETAILS >>>");
                System.out.println(pageText);

                String summary = extractTaskFailureSummary(driver);
                String lower = pageText.toLowerCase();

                if (lower.contains("fail") || lower.contains("failed") || lower.contains("error")) {
                    throw new RuntimeException("Task failed.\n" + summary);
                }

                if (lower.contains("status") && lower.contains("success")) {
                    System.out.println("Task completed successfully");
                    return;
                }

                if (lower.contains("running") || lower.contains("pending")
                        || lower.contains("queued") || lower.contains("in progress")
                        || lower.contains("waiting")) {
                    System.out.println("Task still in progress...");
                    driver.navigate().back();
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                    openTaskResultsTab(driver, wait);
                } else {
                    System.out.println("Opened result details, but final status not recognized yet");
                    driver.navigate().back();
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                    openTaskResultsTab(driver, wait);
                }

            } catch (StaleElementReferenceException e) {
                System.out.println("Stale element while reading task result, retrying...");
            } catch (TimeoutException e) {
                System.out.println("Timed out waiting for task result elements, retrying...");
            }

            sleep(1000);
            driver.navigate().refresh();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            openTaskResultsTab(driver, wait);
        }

        throw new RuntimeException("Task did not complete within timeout for task: " + taskName);
    }

    private static String extractTaskFailureSummary(WebDriver driver) {
        String bodyText = driver.findElement(By.tagName("body")).getText();

        String status = "";
        String exception = "";

        try {
            List<WebElement> failEls = driver.findElements(By.xpath("//*[contains(@class,'failBox')]"));
            if (!failEls.isEmpty()) {
                status = failEls.get(0).getText().trim();
            }
        } catch (Exception ignored) {
        }

        try {
            if (bodyText.contains("Exception during aggregation")) {
                int start = bodyText.indexOf("Exception during aggregation");
                int end = bodyText.indexOf("Attributes", start);
                if (end > start) {
                    exception = bodyText.substring(start, end).trim();
                } else {
                    exception = bodyText.substring(start).trim();
                }
            }
        } catch (Exception ignored) {
        }

        return "Status: " + status + "\n" + exception;
    }

    private static WebElement findTaskResultsSearchBox(WebDriver driver, WebDriverWait wait) {
        List<By> locators = List.of(
                By.xpath("//input[contains(@placeholder,'Search')]"),
                By.xpath("//input[contains(@aria-label,'Search')]"),
                By.xpath("//input[contains(@name,'search')]"),
                By.xpath("//input[contains(@id,'search')]"),
                By.xpath("//input[@type='text' and not(@disabled)]")
        );

        for (By locator : locators) {
            try {
                List<WebElement> found = driver.findElements(locator);
                for (WebElement el : found) {
                    if (el.isDisplayed() && el.isEnabled()) {
                        System.out.println("Using search box id=" + el.getAttribute("id")
                                + ", name=" + el.getAttribute("name")
                                + ", placeholder=" + el.getAttribute("placeholder"));
                        return el;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static void searchTaskResult(WebDriver driver, WebDriverWait wait, WebElement searchBox, String taskName) {
        try {
            searchBox.click();
            sleep(100);

            try {
                searchBox.clear();
            } catch (Exception ignored) {
            }

            searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            searchBox.sendKeys(Keys.DELETE);
            sleep(100);
            searchBox.sendKeys(taskName);
            sleep(200);
            searchBox.sendKeys(Keys.ENTER);
            sleep(1000);

            System.out.println("Searched Task Results for: " + taskName);
            return;
        } catch (Exception e) {
            System.out.println("Normal search typing failed, trying JavaScript fallback: " + e.getMessage());
        }

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].value = arguments[1];", searchBox, taskName);
            js.executeScript("arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", searchBox);
            js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", searchBox);
            searchBox.sendKeys(Keys.ENTER);
            sleep(1000);
            System.out.println("Searched Task Results using JavaScript fallback for: " + taskName);
        } catch (Exception e) {
            throw new RuntimeException("Unable to search Task Results for task: " + taskName, e);
        }
    }

    private static WebElement findTaskResultTarget(WebDriver driver, WebDriverWait wait, String taskName) {
        List<By> locators = List.of(
                By.xpath("//a[normalize-space()='" + taskName + "']"),
                By.xpath("//a[contains(normalize-space(.),'" + taskName + "')]"),
                By.xpath("//*[contains(normalize-space(.),'" + taskName + "')]/ancestor::tr[1]//a[1]"),
                By.xpath("//tr[.//*[contains(normalize-space(.),'" + taskName + "')]]"),
                By.xpath("//*[contains(normalize-space(.),'" + taskName + "')]")
        );

        for (By locator : locators) {
            try {
                List<WebElement> found = driver.findElements(locator);
                for (WebElement el : found) {
                    if (el.isDisplayed()) {
                        System.out.println("Found result target text=" + el.getText()
                                + ", tag=" + el.getTagName()
                                + ", id=" + el.getAttribute("id"));
                        return el;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static void clickElement(WebDriver driver, WebElement element) {
        try {
            element.click();
            return;
        } catch (Exception ignored) {
        }

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
            return;
        } catch (Exception ignored) {
        }

        try {
            new Actions(driver).moveToElement(element).click().perform();
        } catch (Exception e) {
            throw new RuntimeException("Unable to click element with text: " + element.getText(), e);
        }
    }

    private static void dumpTaskResultsPageElements(WebDriver driver) {
        try {
            List<WebElement> inputs = driver.findElements(By.tagName("input"));
            System.out.println("=== VISIBLE INPUTS ON TASK RESULTS PAGE ===");
            for (WebElement input : inputs) {
                try {
                    if (input.isDisplayed()) {
                        System.out.println("INPUT type=" + input.getAttribute("type")
                                + ", id=" + input.getAttribute("id")
                                + ", name=" + input.getAttribute("name")
                                + ", placeholder=" + input.getAttribute("placeholder")
                                + ", value=" + input.getAttribute("value"));
                    }
                } catch (Exception ignored) {
                }
            }

            List<WebElement> links = driver.findElements(By.tagName("a"));
            System.out.println("=== VISIBLE LINKS ON TASK RESULTS PAGE ===");
            for (WebElement link : links) {
                try {
                    String text = link.getText().trim();
                    if (link.isDisplayed() && !text.isEmpty()) {
                        System.out.println("LINK text=" + text
                                + ", id=" + link.getAttribute("id")
                                + ", href=" + link.getAttribute("href"));
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("Could not dump page elements: " + e.getMessage());
        }
    }

    private static void openTaskResultsTab(WebDriver driver, WebDriverWait wait) {
        List<By> tabLocators = List.of(
                By.xpath("//a[normalize-space()='Task Results']"),
                By.xpath("//span[normalize-space()='Task Results']"),
                By.xpath("//*[self::a or self::span or self::div][normalize-space()='Task Results']"),
                By.xpath("//li[.//*[normalize-space()='Task Results']]"),
                By.xpath("//*[contains(@title,'Task Results')]")
        );

        for (By locator : tabLocators) {
            try {
                WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(locator));
                clickElement(driver, tab);
                sleep(1000);
                System.out.println("Opened Task Results tab");
                return;
            } catch (Exception ignored) {
            }
        }

        throw new RuntimeException("Could not find Task Results tab");
    }

    private static boolean is404Page(WebDriver driver) {
        try {
            String pageText = driver.findElement(By.tagName("body")).getText();
            return pageText.contains("404") || pageText.contains("Not Found");
        } catch (Exception e) {
            return false;
        }
    }

    private static void searchIfPresent(WebDriver driver, WebDriverWait wait, String text) {
        List<By> searchLocators = List.of(
                By.xpath("//input[contains(@placeholder,'Search')]"),
                By.xpath("//input[contains(@name,'search')]"),
                By.xpath("//input[contains(@id,'search')]"),
                By.xpath("//input[@type='text']")
        );

        for (By locator : searchLocators) {
            try {
                WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                searchBox.clear();
                searchBox.sendKeys(text);
                searchBox.sendKeys(Keys.ENTER);
                sleep(1000);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }
}
