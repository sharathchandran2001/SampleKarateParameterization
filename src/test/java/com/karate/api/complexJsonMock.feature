# test-jsonpath.feature

Feature: Verify JSONPath Filtering Logic with Hardcoded Response (hits under hits)

  Background:
    # Hardcode the 'response' object directly for testing
    # Now, 'hits' is wrapped under a top-level 'hits' object
    * def response =
    """
    {
      "hits": {
        "hits": [
          {
            "_source": {
              "text": "This is a log message for Successful openshift CLI validation done."
            }
          },
          {
            "_source": {
              "text": "Another log for something else, not relevant."
            }
          },
          {
            "_source": {
              "text": "Failed openshift validation: The deployment encountered an error."
            }
          },
          {
            "_source": {
              "text": "Successful openshift CLI validation - test 2."
            }
          },
          {
            "_source": {
              "text": "Failed openshift validation: Image pull error."
            }
          },
          {
            "_source": {
              "text": "Just another log entry."
            }
          }
        ]
      }
    }
    """

  Scenario: Filter logs for "Successful" without "Failed" in nested hits
    # The 'response' already has the structure `response.hits.hits`
    # No need for an intermediate 'responseHits' variable if 'response' is the direct root.
    # The JSONPath needs to start with `$.hits.hits` to reach the array.

    # Your JSONPath expression for filtering, adjusted for the nested 'hits'
    * def SequenceServer = karate.jsonPath(response, '$.hits.hits[?(@._source.text contains "Successful openshift CLI validation" && !(@._source.text contains "Failed openshift validation"))]')

    # Print the result for verification (useful for debugging)
    And print 'Filtered SequenceServer:', SequenceServer

    # Assertions:
    # We expect 2 successful logs from the hardcoded 'response'
    And match SequenceServer.length == 2

    # Assert that the first found item contains "Successful" and not "Failed"
    And match SequenceServer[0]._source.text contains 'Successful openshift CLI validation'
    And match SequenceServer[0]._source.text !contains 'Failed openshift validation'

    # Assert that the second found item contains "Successful" and not "Failed"
    And match SequenceServer[1]._source.text contains 'Successful openshift CLI validation'
    And match SequenceServer[1]._source.text !contains 'Failed openshift validation'

    # Verify that the filtered results do not contain any "Failed" messages
    # This loop ensures no "Failed" messages sneaked through
    * def i = 0
    * eval
      """
      for (i = 0; i < SequenceServer.length; i++) {
        karate.match('SequenceServer[' + i + ']._source.text !contains "Failed openshift validation"');
      }
      """
