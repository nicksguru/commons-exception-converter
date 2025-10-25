#@disabled
Feature: FieldErrorDiscovererVisitor functionality

  Scenario Outline: ConstraintViolationException processing
    Given a ConstraintViolationException with violation property path "<propertyPath>" and message "<message>"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have field name "<expectedFieldName>"
    And the field error should have error code "Constraint"
    And the field error should have error message "<expectedMessage>"
    Examples:
      | propertyPath          | message                       | expectedFieldName | expectedMessage               |
      | username              | must not be blank             | username          | Must not be blank             |
      | user.email            | must be a well-formed email   | email             | Must be a well-formed email   |
      | products[0].productId | must not be null              | productId         | Must not be null              |
      | nested.field.value    | size must be between 1 and 10 | value             | Size must be between 1 and 10 |

  Scenario Outline: MissingServletRequestParameterException processing
    Given a MissingServletRequestParameterException with parameter name "<parameterName>"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have field name "<expectedFieldName>"
    And the field error should have error code "NotNull"
    And the field error should have error message "Missing mandatory parameter"
    Examples:
      | parameterName | expectedFieldName |
      | userId        | userId            |
      | page.size     | size              |
      | filter.       | filter.           |

  Scenario Outline: MethodArgumentTypeMismatchException processing
    Given a MethodArgumentTypeMismatchException with name "<parameterName>", error code "<errorCode>", and message "<message>"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have field name "<expectedFieldName>"
    And the field error should have error code "<expectedErrorCode>"
    And the field error should have error message "<expectedMessage>"
    Examples:
      | parameterName | errorCode    | message                                  | expectedFieldName | expectedErrorCode | expectedMessage |
      | status        | typeMismatch | Failed to convert enum value             | status            | TypeMismatch      | Enumeration     |
      | count         | typeMismatch | Failed to convert string to integer      | count             | TypeMismatch      |                 |
      | user.active   | typeMismatch | Failed to convert enum STATUS to boolean | active            | TypeMismatch      | Enumeration     |

  Scenario: MethodArgumentNotValidException processing
    Given a MethodArgumentNotValidException with field errors
      | fieldName | errorCode | errorMessage        |
      | username  | NotBlank  | must not be blank   |
      | email     | Email     | must be valid email |
    When the visitor processes the exception
    Then field errors should be discovered
    And the field errors should contain 2 items

  Scenario: BindException processing
    Given a BindException with field errors
      | fieldName | errorCode | errorMessage      |
      | password  | Size      | size must be 8-20 |
    When the visitor processes the exception
    Then field errors should be discovered
    And the field errors should contain 1 items

  Scenario Outline: ValidationException processing
    Given a ValidationException with cause type "<causeType>"
    When the visitor processes the exception
    Then field errors should be <result>
    Examples:
      | causeType        | result     |
      | BindException    | discovered |
      | RuntimeException | empty      |

  Scenario: Unknown exception type processing
    Given an unknown exception type
    When the visitor processes the exception
    Then field errors should be empty

  Scenario: Null exception processing
    Given a null exception
    When the visitor processes the exception
    Then field errors should be empty

  Scenario: ConstraintViolationException with multiple violations
    Given multiple ConstraintViolationExceptions with violations:
      | propertyPath | message                     |
      | username     | must not be blank           |
      | email        | must be a well-formed email |
      | age          | must be greater than 0      |
    When the visitor processes the exception
    Then field errors should be discovered
    And the field errors should contain 3 items
    And the field errors should contain field names "username", "email", "age"

  Scenario Outline: Field name masking for nested properties
    Given a ConstraintViolationException with violation property path "<propertyPath>" and message "validation failed"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have field name "<expectedFieldName>"
    Examples:
      | propertyPath                 | expectedFieldName |
      | user                         | user              |
      | user.profile                 | profile           |
      | user.profile.settings        | settings          |
      | users[0].name                | name              |
      | products[10].details.price   | price             |
      | nested.very.deep.field.value | value             |

  Scenario: ConstraintViolationException with empty violations set
    Given a ConstraintViolationException with no violations
    When the visitor processes the exception
    Then field errors should be discovered
    And the field errors should contain 0 items

  Scenario Outline: MethodArgumentTypeMismatchException with various error codes
    Given a MethodArgumentTypeMismatchException with name "param", error code "<errorCode>", and message "<message>"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have error code "<expectedErrorCode>"
    And the field error should have error message "<expectedMessage>"
    Examples:
      | errorCode     | message                      | expectedErrorCode | expectedMessage |
      | typeMismatch  | Failed to convert enum value | TypeMismatch      | Enumeration     |
      | typeMismatch  | Failed to convert string     | TypeMismatch      |                 |
      | invalidFormat | Invalid date format          | InvalidFormat     |                 |
      | numberFormat  | Number format exception      | NumberFormat      |                 |

  Scenario: MissingServletRequestParameterException with complex parameter names
    Given a MissingServletRequestParameterException with parameter name "filter.search.criteria"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have field name "criteria"
    And the field error should have error code "NotNull"
    And the field error should have error message "Missing mandatory parameter"

  Scenario: ValidationException with nested BindException containing multiple errors
    Given a ValidationException with BindException cause containing field errors:
      | fieldName | errorCode | errorMessage          |
      | firstName | NotBlank  | must not be blank     |
      | lastName  | Size      | size must be 2-50     |
      | email     | Email     | must be a valid email |
    When the visitor processes the exception
    Then field errors should be discovered
    And the field errors should contain 3 items

  Scenario: ValidationException with null cause
    Given a ValidationException with null cause
    When the visitor processes the exception
    Then field errors should be empty

  Scenario Outline: Exception inheritance hierarchy testing
    Given an exception of type "<exceptionType>"
    When the visitor processes the exception
    Then field errors should be <result>
    Examples:
      | exceptionType                           | result     |
      | ConstraintViolationException            | discovered |
      | MissingServletRequestParameterException | discovered |
      | MethodArgumentTypeMismatchException     | discovered |
      | MethodArgumentNotValidException         | discovered |
      | BindException                           | discovered |
      | ValidationException                     | empty      |
      | RuntimeException                        | empty      |
      | IllegalArgumentException                | empty      |
      | NullPointerException                    | empty      |

  Scenario: Field error message capitalization
    Given a ConstraintViolationException with violation property path "email" and message "must be a well-formed email address"
    When the visitor processes the exception
    Then field errors should be discovered
    And the field error should have error message "Must be a well-formed email address"
