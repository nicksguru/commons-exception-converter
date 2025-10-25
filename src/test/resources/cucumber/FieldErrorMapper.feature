#@disabled
Feature: FieldErrorMapper

  Scenario: Masking field name with dot notation
    Given a field name "user.profile.firstName"
    When the field name is masked
    Then the masked field name should be "firstName"

  Scenario: Masking field name without dots
    Given a field name "username"
    When the field name is masked
    Then the masked field name should be "username"

  Scenario: Masking null field name
    Given a field name that is null
    When the field name is masked
    Then the masked field name should be null

  Scenario: Masking field name with dot at the end
    Given a field name "user.profile."
    When the field name is masked
    Then the masked field name should be "user.profile."

  Scenario: Converting field error to DTO
    Given a field error for field "user.email" with code "NotBlank" and message "must not be blank"
    When the field error is converted to DTO
    Then the field error DTO should have field name "email"
    And the field error DTO should have error code "NotBlank"

  Scenario: Converting field error with null code
    Given a field error for field "username" with code null and message "invalid username"
    When the field error is converted to DTO
    Then the field error DTO should have field name "username"
    And the field error DTO should have null error code

  Scenario Outline: Masking different field names
    Given a field name "<fieldName>"
    When the field name is masked
    Then the masked field name should be "<expectedMaskedName>"
    Examples:
      | fieldName                      | expectedMaskedName |
      | customer.address.street        | street             |
      | orders[0].items[0].productName | productName        |
      | simple                         | simple             |
      | parent.child.                  | parent.child.      |
      | parent..child                  | child              |
      | .field                         | field              |
