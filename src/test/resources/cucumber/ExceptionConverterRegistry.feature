@exceptions #@disabled
Feature: ExceptionConverterRegistry

  Scenario: Finding converter for an exception
    Given an exception converter registry with converters for different exception types
    When a converter is requested for exception type "java.lang.IllegalArgumentException"
    Then a converter should be found
    And the converter can convert the exception

  Scenario: Finding converter for an exception using inheritance
    Given an exception converter registry with converters for different exception types
    When a converter is requested for exception type "java.lang.ArrayIndexOutOfBoundsException"
    Then a converter should be found
    And the converter can convert the exception

  Scenario: No converter found for an exception
    Given an exception converter registry with converters for different exception types
    When a converter is requested for exception type "java.sql.SQLException"
    Then no converter should be found

  Scenario: Verifying converter order with subclass before superclass
    Given an exception converter registry with converters
    When the registry is initialized

  Scenario: Converting exceptions with found converter
    Given an exception converter registry with converters for different exception types
    When an exception of type "java.lang.IllegalArgumentException" is converted
    Then the result should be a business exception

  Scenario Outline: Finding converters for different exception types
    Given an exception converter registry with converters for different exception types
    When a converter is requested for exception type "<exceptionType>"
    Then a converter <shouldBeFound> be found
    Examples:
      | exceptionType                           | shouldBeFound |
      | java.lang.IllegalArgumentException      | should        |
      | java.lang.NullPointerException          | should        |
      | java.lang.UnsupportedOperationException | should        |
      | java.io.IOException                     | should not    |
      | java.sql.SQLException                   | should not    |

  Scenario Outline: Converting different exceptions with specific converters
    Given an exception of type "<exceptionType>" with message "<message>"
    And no exception should be thrown
    When the appropriate converter converts the exception
    Then no exception should be thrown
    And the result should be a "<resultType>"
    And the original exception should be the cause
    Examples:
      | exceptionType                                             | message            | resultType                                                 |
      | java.net.ConnectException                                 | Connection refused | guru.nicks.commons.exception.http.ServiceTimeoutException  |
      | java.lang.SecurityException                               | Security error     | guru.nicks.commons.exception.http.UnauthorizedException    |
      | org.apache.commons.lang3.NotImplementedException          | Not implemented    | guru.nicks.commons.exception.http.NotImplementedException  |
      | org.springframework.web.bind.MissingRequestValueException | Missing header     | guru.nicks.commons.exception.http.BadRequestException      |
      | org.springframework.web.multipart.MultipartException      | Upload too big     | guru.nicks.commons.exception.http.PayloadTooLargeException |
      | org.springframework.security.access.AccessDeniedException | Access denied      | guru.nicks.commons.exception.http.UnauthorizedException    |
