Feature: web and api

Background:
* def vFileChooserPath = Java.type('com.karate.api.JavaUtil').fileChoose()



Scenario: API Main feature for later purpose
* print 'Mainfile vFileChooserPath'+vFileChooserPath

# Calling first feature file
* call read('1-karate-driver-and-api.feature')


 

