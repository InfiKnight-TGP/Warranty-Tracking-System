# Warrify

A Java 17 and Spring Boot application for tracking purchases, bills, warranties, reminders, and service claims. The web UI is static HTML, CSS, and vanilla JavaScript; REST APIs use JSON, with a SOAP warranty lookup for integration practice.

The product name is **Warrify**. The web app logo is in `src/main/resources/static/images/warrify-logo.svg`; its favicon is a white **W** on a near-black rounded tile, matching the supplied Warrify wordmark. The Chrome extension uses the same favicon in `chrome-extension/icons/favicon.svg` and matching 16, 48, and 128 pixel toolbar icons. Regenerate the extension PNGs on Windows with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\chrome-extension\icons\generate-icons.ps1
```

## Demo prices and currency

Prices are entered and stored as numeric INR amounts and displayed using Indian currency formatting (`en-IN`, `INR`) in the web app. The Chrome extension labels its price field **Price (INR)**. Demo seed prices are set to realistic sample amounts:

| Demo product | Sample price |
| --- | ---: |
| Whirlpool refrigerator | ₹34,990 |
| Lenovo laptop | ₹64,990 |
| LG washing machine | ₹28,990 |
| Samsung smartphone | ₹24,999 |
| Sony television | ₹54,990 |

On startup, the seeder refreshes these five prices for matching demo serial numbers owned by `user@vault.com`, including in an existing database. It does not convert or change other user purchases. These amounts are examples; prices entered through the app are kept as entered.

## Requirements

- JDK 17
- MySQL 8
- Internet access for the first Maven wrapper build
- No global Maven installation is required

## MySQL setup

Start MySQL 8. The default connection in `src/main/resources/application.properties` uses database `warranty_vault`, username `root`, and password `root`. Change these settings for your local installation. `createDatabaseIfNotExist=true` creates the database when needed, and Hibernate updates the schema.

The ORM exercise adds required `updated_at` columns and a notification discriminator. For this seed-data project, reset an older schema from PowerShell with the MySQL client installed and on `PATH`:

```powershell
mysql -u root -proot -e "DROP DATABASE IF EXISTS warranty_vault; CREATE DATABASE warranty_vault;"
```

This deletes all data in `warranty_vault`. On the next app startup, `DataSeeder` recreates the three demo accounts and five purchases because the users table is empty.

## Import into Eclipse

1. Choose **File > Import > Maven > Existing Maven Projects**.
2. Select the project directory containing `pom.xml` and finish the import.
3. Set the project JRE to JDK 17 if Eclipse does not choose it automatically.

## Run the application

In Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd spring-boot:run
```

On macOS or Linux with JDK 17:

```sh
./mvnw spring-boot:run
```

Open <http://localhost:8080>. The app uses an embedded Artemis broker; no external broker is required.

## Demo accounts

Accounts and sample purchases are created only when the users table is empty:

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@vault.com` | `admin123` |
| Service centre | `service@vault.com` | `service123` |
| User | `user@vault.com` | `user123` |

Sample serial numbers include `WR-1001` (Refrigerator/Whirlpool), `LG-3003` (expiring in about 20 days), and `SM-4004` (expiring in about 5 days).

## SOAP warranty lookup

The WSDL is available at <http://localhost:8080/ws/warranty.wsdl>. The operation accepts a seeded product serial number and returns the product, brand, expiry date, remaining days, and status. Example in Windows PowerShell:

```powershell
$soap = @'
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:w="http://vault.com/ws/warranty">
  <soap:Header />
  <soap:Body>
    <w:GetWarrantyStatusRequest><w:serialNumber>WR-1001</w:serialNumber></w:GetWarrantyStatusRequest>
  </soap:Body>
</soap:Envelope>
'@
Invoke-WebRequest -Uri http://localhost:8080/ws -Method Post `
  -ContentType 'text/xml; charset=utf-8' -Body $soap
```

## CSV import and admin batch jobs

The Purchases page accepts a CSV with this header:

```text
productName,brand,category,serialNumber,storeName,purchaseDate,price,warrantyMonths,supportContact
```

Dates use ISO format (`yyyy-MM-dd`). Each imported row is validated; the response shows how many rows were imported and skipped. Admins can run the reminder job or export all purchases as CSV from the Batch Jobs section of the admin page. The executions table shows job status and read, write, and skip counts.

The batch reminder job uses a cursor reader so updates to reminder flags do not alter the rows being iterated. It sends messages through the configured JMS template, preserving the same message conversion used by `ReminderListener`.

## Trigger reminders quickly

The normal schedule runs daily at 09:00 using `vault.scheduler.cron`. For a demo, either click **Run reminder job** on the admin page or temporarily set:

```properties
vault.scheduler.cron=0 * * * * *
```

Restart the app to run the scheduled job once a minute. Check Notifications using the demo user. Restore `0 0 9 * * *` afterwards. The per-purchase 30-day and 7-day flags prevent duplicate reminders.

## Validation and ORM notes

Purchase creation and update use separate validation groups. The custom date constraint requires a purchase date from 2000-01-01 through today; the custom warranty-period constraint limits the warranty to 1–120 months. Validation responses include a `fields` object for the UI.

Entities inherit `createdAt` and `updatedAt` timestamps from `BaseEntity`. Notifications use a single table with a discriminator for expiry and claim notifications. Purchase search uses JPA specifications; summary and admin views use projections and aggregate queries, with in-memory caching for dashboard counts.

## Chrome Extension

The optional Manifest V3 extension is a second client for the existing API. It only works while the Warrify back end is running on `localhost` (default: `http://localhost:8080`). To install it in Chrome:

1. Open `chrome://extensions` and turn on **Developer mode**.
2. Select **Load unpacked** and choose this project's `chrome-extension/` folder.
3. Open the Warrify extension, sign in with a USER account, and use **Server settings** if the back end uses a different local address.
4. Try **Add purchase** (including the optional visible-page screenshot), **Expiring soon**, and **Notifications** with **Mark read**. The extension checks immediately on sign-in, when its popup opens, at browser startup, and every minute while Chrome is running; it updates its badges and shows desktop alerts for newly detected unread notifications. You can also switch themes, autofill demo credentials, auto-detect product and store details from a page, mark all notifications read, and test the server connection in settings.

The extension authenticates using a bearer token created by `POST /api/auth/token`. The server stores only a SHA-256 hash of each token. Logging out revokes the current token. Existing session-based web login continues to work as before. The extension calls the existing notification read route, `PUT /api/notifications/{id}/read`.

## Build and tests

```powershell
.\mvnw.cmd clean package
```

Tests cover warranty expiry calculation, claim status transitions, scheduler windows, custom purchase validation, search specifications, batch reminder selection and CSV import counts, SOAP endpoint/WSDL behavior, and Spring context loading. H2 is test scope only; the app uses MySQL.

## Git setup

Generated build output, uploaded bills, local Maven caches, exported CSV files, environment files, and runtime logs are excluded by `.gitignore`. The Maven Wrapper files and the Warrify brand assets are part of the project and should be committed.

To connect this local repository to a remote, replace the URL with your Git host's repository URL:

```powershell
git remote add origin <repository-url>
git add .
git commit -m "Prepare Warrify project"
git push -u origin main
```

If Git reports that the workspace has “dubious ownership,” add this folder to Git's safe-directory list from your Windows account:

```powershell
git config --global --add safe.directory 'E:/Warranty Tracker'
```
