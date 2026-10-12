# Use Cases

## UC-01: View All Countries by Population

**Actor:** Organisation User

**Preconditions:**
- The application is running.
- The population database is available.

**Main Flow:**
1. The user requests to view all countries.
2. The system retrieves all country records.
3. The system sorts the countries from largest population to smallest.
4. The system displays the country report.

**Expected Output:**
- Code
- Name
- Continent
- Region
- Population
- Capital

**Postcondition:**
The country report is displayed successfully.

## UC-02: View City Reports

**Actor:** Organisation User

**Preconditions:**
- The application is running.
- The population database is available.

**Main Flow:**
1. The user requests a city report.
2. The user selects the required scope, such as world, continent, region, country, or district.
3. The system retrieves the matching city records.
4. The system sorts the cities from largest population to smallest.
5. The system displays the city report.

**Expected Output:**
- Name
- Country
- District
- Population

**Postcondition:**
The requested city report is displayed successfully.


## UC-03: View Capital City Reports

**Actor:** Organisation User

**Preconditions:**
- The application is running.
- The population database is available.

**Main Flow:**
1. The user requests a capital city report.
2. The user selects the required scope, such as world, continent, or region.
3. The system retrieves the matching capital city records.
4. The system sorts the capital cities from largest population to smallest.
5. The system displays the capital city report.

**Expected Output:**
- Name
- Country
- Population

**Postcondition:**
The requested capital city report is displayed successfully.


## UC-04: View Population Reports

**Actor:** Organisation User

**Preconditions:**
- The application is running.
- The population database is available.

**Main Flow:**
1. The user requests a population report.
2. The user selects a continent, region, or country.
3. The system retrieves the total population for the selected area.
4. The system calculates the population living in cities.
5. The system calculates the population not living in cities.
6. The system calculates the percentage for both categories.
7. The system displays the population report.

**Expected Output:**
- Name of the continent, region, or country
- Total population
- Population living in cities
- Percentage living in cities
- Population not living in cities
- Percentage not living in cities

**Postcondition:**
The requested population report is displayed successfully.


## UC-05: View Language Statistics

**Actor:** Organisation User

**Preconditions:**
- The application is running.
- The population database is available.

**Main Flow:**
1. The user requests the language population statistics.
2. The system retrieves population information for the required languages.
3. The system calculates the number of people who speak each language.
4. The system calculates each language's percentage of the world population.
5. The system orders the languages from greatest number of speakers to smallest.
6. The system displays the results.

**Languages Included:**
- Chinese
- English
- Hindi
- Spanish
- Arabic

**Expected Output:**
- Language
- Number of speakers
- Percentage of world population

**Postcondition:**
The language population statistics are displayed successfully.