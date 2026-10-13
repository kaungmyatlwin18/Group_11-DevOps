# Software Engineering Methods

* Master Build Status [![Master Build Status](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/workflows/main.yml/badge.svg?branch=master)](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/workflows/main.yml)
* Develop Build Status [![Develop Build Status](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/workflows/main.yml/badge.svg?branch=develop)](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/workflows/main.yml)
* License [![LICENSE](https://img.shields.io/github/license/kaungmyatlwin18/Group_11-DevOps.svg?style=flat-square)](https://github.com/kaungmyatlwin18/Group_11-DevOps/blob/master/LICENSE)
* Release [![Releases](https://img.shields.io/github/release/kaungmyatlwin18/Group_11-DevOps/all.svg?style=flat-square)](https://github.com/kaungmyatlwin18/Group_11-DevOps/releases)

# Group 11 — DevOps Project Report

**Module:** SET09803 DevOps  
**Project:** Population Reporting System  
**Code Review:** 1  
**Date:** 14 October 2026

## 1. Team Members

| Role          | Member          |
| ------------- | --------------- |
| Product Owner | Hein Htoo Naing|
| Scrum Master  | Kaung Myat Lwin |
| Developer 1   | Htoo Aung Lin   |
| Developer 2   | Nyan Lin Htet   |
| Developer 3   | Dana Aung       |
| Developer 4   | Thet Aung Hein  |

## 2. Technologies Used

| Technology        | Version / Details               | Purpose                                               |
| ----------------- | ------------------------------- | ----------------------------------------------------- |
| Java              | JDK 25 (Eclipse Temurin)        | Application development                               |
| Apache Maven      | Version to confirm              | Building the Java application and running tests       |
| MySQL             | 8.4                             | World population database                             |
| MySQL Connector/J | JDBC driver                     | Connecting Java to MySQL                              |
| Docker            | Docker Desktop / Docker Compose | Running and configuring containers                    |
| Git               | Version control                 | Managing source code changes                          |
| GitHub            | Repository hosting              | Issues, branches, pull requests and collaboration     |
| GitHub Actions    | CI workflow                     | Automated Maven builds, tests and Docker verification |
| JUnit             | Version to confirm              | Automated testing                                     |
| IntelliJ IDEA     | 2026.2.2                        | Java development and debugging                        |

## 3. Use Case Diagram

<img width="548" height="632" alt="use-case-diagram" src="https://github.com/user-attachments/assets/2a540d11-d768-481a-bdfb-a5fedcde233a" />


The use case diagram illustrates how a user interacts with the Population Reporting System to generate reports from the MySQL World database.

The system supports reporting on countries, cities, capital cities, populations and language statistics.

Users can select relevant locations, such as continents and regions, and request reports ordered by population. Some reports allow the user to specify a Top N value.

For Code Review 1, our team implemented and verified the first six country reporting requirements.

## 4. Results and Outcomes

### Implementation Progress

**6 of 32 requirements completed (18.75%).**

The six completed requirements cover country reporting features. The remaining 26 requirements are planned for future development.

### 4.1 Country Reports

| ID | Requirement                                | GitHub Issue | Met? | Output Screenshot |
| -- | ------------------------------------------ | ------------ | ---- | ----------------- |
| 1  | All countries in the world by population   | #2           | Yes  | <img width="1448" height="796" alt="issue-02" src="https://github.com/user-attachments/assets/11a26ef7-d82f-4a62-bca7-fddb589a4aae" />|
| 2  | All countries in a continent by population | #4           | Yes  | <img width="1096" height="760" alt="issue-04" src="https://github.com/user-attachments/assets/1441f53c-38d0-45b9-94e5-82bc24bd64fe" />|
| 3  | All countries in a region by population    | #6           | Yes  | <img width="787" height="390" alt="issue-06" src="https://github.com/user-attachments/assets/b6bc45e4-cf2e-40e3-a337-5b69f744d03f" /> |
| 4  | Top N countries in the world               | #7           | Yes  | <img width="957" height="312" alt="issue-07" src="https://github.com/user-attachments/assets/626f8943-ccb4-47bc-95d5-aa35c032373a" /> |
| 5  | Top N countries in a continent             | #8           | Yes  | <img width="896" height="247" alt="issue-08" src="https://github.com/user-attachments/assets/46d843d6-6d04-4711-be33-91599c4f07fe" /> |
| 6  | Top N countries in a region                | #9           | Yes  | <img width="847" height="255" alt="issue-09" src="https://github.com/user-attachments/assets/943daaff-1338-4144-a597-ad8fcf9350c1" /> |

### 4.2 City Reports

| ID | Requirement                             | Met? | Output Screenshot |
| -- | --------------------------------------- | ---- | ----------------- |
| 7  | All cities in the world by population   | No   | —                 |
| 8  | All cities in a continent by population | No   | —                 |
| 9  | All cities in a region by population    | No   | —                 |
| 10 | All cities in a country by population   | No   | —                 |
| 11 | All cities in a district by population  | No   | —                 |
| 12 | Top N cities in the world               | No   | —                 |
| 13 | Top N cities in a continent             | No   | —                 |
| 14 | Top N cities in a region                | No   | —                 |
| 15 | Top N cities in a country               | No   | —                 |
| 16 | Top N cities in a district              | No   | —                 |

### 4.3 Capital City Reports

| ID | Requirement                         | Met? | Output Screenshot |
| -- | ----------------------------------- | ---- | ----------------- |
| 17 | All capital cities in the world     | No   | —                 |
| 18 | All capital cities in a continent   | No   | —                 |
| 19 | All capital cities in a region      | No   | —                 |
| 20 | Top N capital cities in the world   | No   | —                 |
| 21 | Top N capital cities in a continent | No   | —                 |
| 22 | Top N capital cities in a region    | No   | —                 |

### 4.4 Population Distribution Reports

| ID | Requirement                                                   | Met? | Output Screenshot |
| -- | ------------------------------------------------------------- | ---- | ----------------- |
| 23 | Population living and not living in cities for each continent | No   | —                 |
| 24 | Population living and not living in cities for each region    | No   | —                 |
| 25 | Population living and not living in cities for each country   | No   | —                 |

### 4.5 Population Information

| ID | Requirement               | Met? | Output Screenshot |
| -- | ------------------------- | ---- | ----------------- |
| 26 | Population of the world   | No   | —                 |
| 27 | Population of a continent | No   | —                 |
| 28 | Population of a region    | No   | —                 |
| 29 | Population of a country   | No   | —                 |
| 30 | Population of a district  | No   | —                 |
| 31 | Population of a city      | No   | —                 |

### 4.6 Language Statistics

| ID | Requirement                                                                            | Met? | Output Screenshot |
| -- | -------------------------------------------------------------------------------------- | ---- | ----------------- |
| 32 | Population speaking Chinese, English, Hindi, Spanish and Arabic, including percentages | No   | —                 |

## 5. Completed Feature Evidence

### Requirement 1 — All Countries in the World

This report displays all countries from the World database in descending order of population. It includes the country code, name, continent, region, population and capital.

**[Insert Issue #2 output screenshot]**

### Requirement 2 — Countries in a Continent

This report displays the countries in a selected continent, such as Asia, ordered from the largest population to the smallest.

**[Insert Issue #4 output screenshot]**

### Requirement 3 — Countries in a Region

This report displays all countries in a selected region, such as Eastern Asia, sorted by population.

**[Insert Issue #6 output screenshot]**

### Requirement 4 — Top N Countries in the World

This report allows a configurable N value and displays the N most populated countries in the world. For testing, the system was configured to display the Top 5 countries.

**[Insert Issue #7 output screenshot]**

### Requirement 5 — Top N Countries in a Continent

This report displays the N most populated countries in a selected continent. The current demonstration shows the Top 3 countries in Asia.

**[Insert Issue #8 output screenshot]**

### Requirement 6 — Top N Countries in a Region

This report displays the N most populated countries in a selected region. The current demonstration shows the Top 3 countries in Eastern Asia.

**[Insert Issue #9 output screenshot]**

## 6. Testing and Outcome

For Code Review 1, Group 11 successfully implemented six country reporting requirements.

The integrated application connected to the MySQL World database and generated the required report outputs.

Verification included:

- Successful Maven build.
- 21 passing JUnit tests.
- Successful GitHub Actions checks.
- Successful execution of all six reports.
- Application finished with exit code 0.

The remaining requirements will be implemented during subsequent development work.

## 7. Conclusion

Group 11 has established the initial DevOps development workflow and implemented six of the 32 population reporting requirements.

The completed features demonstrate successful database integration, population-based sorting, configurable Top N reporting, and automated testing.

Further development will focus on completing the remaining city, capital city, population and language reporting requirements.

## 8. Individual Contribution Percentages
| Matric            | Code Review 1                   | Code Review 2                                         |
| ----------------- | ------------------------------- | ----------------------------------------------------- |
| 40841053          | 17                              | -                                                     |
| 40841054          | 17                              | -                                                     |
| 40841060          | 17                              | -                                                     |
| 40841062          | 17                              | -                                                     |
| 40841068          | 15                              | -                                                     |
| 40840490          | 17                              | -                                                     |
| Total             | 100                             | -                                                     |
