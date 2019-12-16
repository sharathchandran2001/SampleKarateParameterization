Feature: web and api

Background:
* configure driver = { type: 'chrome' }
 * def jsonE =  call read('pom.json')
* print jsonE

Scenario: Web 
Given driver 'https://stackoverflow.com'
And waitForUrl('https://www.cgi.com/en')
And waitFor('element1')
And highlight('#edit-keyword')
And input('#edit-keyword','Gartner')
When submit().click(testAccounts.submit)
* waitForUrl('https://www.cgi.com/en/search/site?keyword=gartner')
When submit().click("//div[@id='main-nav']/div/nav/ul/li[1]/a")

Scenario: API
 
 Given url 'https://community-open-weather-map.p.rapidapi.com'
 And path '/weather'
  # And request { query: '#(query)' }
  And header Accept = 'application/json'
  When method GET
  Then status 200
  Then match response == dbQuery1 

 

