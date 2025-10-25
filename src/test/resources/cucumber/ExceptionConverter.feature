@exceptions #@disabled
Feature: ExceptionConverter

  Scenario: Getting source class from converter
    Given an exception converter from "java.lang.NullPointerException" to TestBusinessException
    When the source class is retrieved from the converter
    Then the source class should be "java.lang.NullPointerException"

  Scenario: Getting target class from converter
    Given an exception converter from "java.lang.IllegalStateException" to TestBusinessException
    When the target class is retrieved from the converter
    Then the target class should be TestBusinessException

  Scenario: Converting an exception using custom implementation
    Given a custom exception converter with specific conversion logic
    When the converter is used to convert an exception with message "test message"
    Then the converted exception should contain the original message

  Scenario Outline: Converting different exception types
    Given an exception converter from "<sourceType>" to TestBusinessException
    When the converter is used to convert an exception
    Then the converted exception should be of the target type
    And the converted exception should have the original exception as cause
    Examples:
      | sourceType                              |
      | java.lang.IllegalArgumentException      |
      | java.lang.NullPointerException          |
      | java.lang.UnsupportedOperationException |
      | java.lang.RuntimeException              |
      | java.lang.Exception                     |
