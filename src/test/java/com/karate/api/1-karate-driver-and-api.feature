Feature: web and api

Background:
# * configure driver = { type: 'chrome' }
* def pOne = read('pom.json')
#* def vFileChooserPath = Java.type('com.karate.api.JavaUtil').fileChoose()
* print 'vFileChooserPath'+vFileChooserPath
* def csvInput = read(vFileChooserPath) 
#* def csvInput = read('ApiParameterization.csv') 

   
# Scenario: Web 
# Given driver 'https://stackoverflow.com'

Scenario Outline: API

 #  =======================   ***********************  Refer below : define Input and output values for data parameterization   =======================   ****************
 * def vUrl = '<vIUrl>'
 * def vPath = '<vIPath>'
 * def vTransactionType = '<vITransactionType>'
 * def vResponseCode = '<vIResponseCode>'
  * print 'vResponseCode'+vResponseCode
 * def vResponseId = '<id>' 
 * def vResponseTitle = '<title>' 

  
 #  =======================   *********************** Refer below :  Java / java script code management   =======================   ***********************
 
   * def vResponseCodeValidation = Java.type('com.karate.api.JavaUtil').stringToInt(vResponseCode)
   * print 'vResponseCodeValidation'+vResponseCodeValidation

#  =======================   *********************** Refer below :  Java / java script code management   =======================   *********************** 
 Given url vUrl
 And path vPath
 And header Accept = 'application/json'
 When method vTransactionType
  
 Then match responseStatus == vResponseCodeValidation

 * def vResponseIdValidation = Java.type('com.karate.api.JavaUtil').convString(response.id)
 * match vResponseIdValidation == vResponseId
 
  * def vResponseTitleValidation = Java.type('com.karate.api.JavaUtil').convString(response.title)
 * match vResponseTitleValidation == vResponseTitle
 
  
  Examples:
  | csvInput | 


 

