#@disabled
Feature: Error Dictionary Service
  The error dictionary service provides localized error messages for business error codes.

  Scenario: Dictionary is initialized with valid translations
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_ONE  | fr     | French message  |
      | CODE_TWO  | en     | Another message |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the error dictionary should contain 2 error codes
    And the supported locales should be "en, fr"
    And the dictionary version should not be blank

  Scenario: Dictionary filters out null error codes
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      |           | en     | Invalid message |
      | CODE_ONE  | en     | Valid message   |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the error dictionary should contain 1 error codes

  Scenario: Dictionary filters out null locales
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  |        | Invalid message |
      | CODE_ONE  | en     | Valid message   |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the error dictionary should contain 1 error codes
    And the supported locales should be "en"

  Scenario: Dictionary filters out invalid locales with blank language tags
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | 123    | Invalid message |
      | CODE_ONE  | en     | Valid message   |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the error dictionary should contain 1 error codes
    And the supported locales should be "en"

  Scenario: Dictionary filters out blank messages
    Given an error dictionary with the following translations:
      | errorCode | locale | message       |
      | CODE_ONE  | en     |               |
      | CODE_ONE  | fr     | Valid message |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the error dictionary should contain 1 error codes
    And the supported locales should be "fr"

  Scenario: Dictionary filters out empty locale maps
    Given an error dictionary with error code "CODE_ONE" having an empty locale map
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the error dictionary should contain 0 error codes

  Scenario Outline: Find translation with specific locales
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_ONE  | fr     | French message  |
      | CODE_ONE  | de     | German message  |
    And the default locale is "en"
    And the error dictionary service is initialized
    When finding translation for error code "CODE_ONE" with locales "<locales>"
    Then the translation should be "<expectedMessage>"
    Examples:
      | locales | expectedMessage |
      | en      | English message |
      | fr      | French message  |
      | de      | German message  |
      | en,fr   | English message |
      | fr,en   | French message  |

  Scenario: Find translation falls back to default locale
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_ONE  | fr     | French message  |
    And the default locale is "en"
    And the error dictionary service is initialized
    When finding translation for error code "CODE_ONE" with locales "de,es"
    Then the translation should be "English message"

  Scenario: Find translation returns empty when no translation exists
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
    And the default locale is "en"
    And the error dictionary service is initialized
    When finding translation for error code "CODE_TWO" with locales "en"
    Then the translation should be empty

  Scenario: Find translation with null locales collection
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
    And the default locale is "en"
    And the error dictionary service is initialized
    When finding translation for error code "CODE_ONE" with null locales
    Then the translation should be "English message"

  Scenario: Find translation skips null locales in collection
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_ONE  | fr     | French message  |
    And the default locale is "en"
    And the error dictionary service is initialized
    When finding translation for error code "CODE_ONE" with locales containing nulls and "fr"
    Then the translation should be "French message"

  Scenario: Find translation with locale priority from HTTP request
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_ONE  | fr     | French message  |
    And the default locale is "en"
    And the error dictionary service is initialized with HTTP request factory
    And the HTTP request has Accept-Language header "fr,en;q=0.9"
    When finding translation with locale priority for error code "CODE_ONE"
    Then the translation should be "French message"

  Scenario: Resolve locale priority without HTTP request
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
    And the default locale is "en"
    And the error dictionary service is initialized without HTTP request factory
    When resolving locale priority
    Then the locale priority should be empty

  Scenario: Dictionary version is consistent for same content
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_TWO  | fr     | French message  |
    And the default locale is "en"
    When the error dictionary service is initialized
    And another error dictionary service is initialized with the same translations
    Then both dictionary versions should be equal

  Scenario: Dictionary version changes when content changes
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
    And the default locale is "en"
    When the error dictionary service is initialized
    And another error dictionary service is initialized with different translations
    Then the dictionary versions should be different

  Scenario: Supported locales are sorted
    Given an error dictionary with the following translations:
      | errorCode | locale | message |
      | CODE_ONE  | fr     | French  |
      | CODE_ONE  | en     | English |
      | CODE_ONE  | de     | German  |
      | CODE_TWO  | es     | Spanish |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the supported locales should be "de, en, es, fr"

  Scenario: Incomplete dictionary is reported
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
    And the default locale is "en"
    And the error code enum has 3 constants
    When the error dictionary service is initialized
    Then an error should be logged about incomplete dictionary

  Scenario: Complete dictionary is reported
    Given an error dictionary with the following translations:
      | errorCode  | locale | message             |
      | CODE_ONE   | en     | English message     |
      | CODE_TWO   | en     | Another message     |
      | CODE_THREE | en     | Yet another message |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then an info should be logged about complete dictionary

  Scenario: Get missing error codes when dictionary is complete
    Given an error dictionary with the following translations:
      | errorCode  | locale | message             |
      | CODE_ONE   | en     | English message     |
      | CODE_TWO   | en     | Another message     |
      | CODE_THREE | en     | Yet another message |
    And the default locale is "en"
    When the error dictionary service is initialized
    Then the missing error codes should be empty

  Scenario: Get missing error codes when dictionary is incomplete
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
    And the default locale is "en"
    And the error code enum has 3 constants
    When the error dictionary service is initialized
    Then the missing error codes should contain "CODE_TWO, CODE_THREE"

  Scenario: Get missing error codes when dictionary is empty
    Given an empty error dictionary
    And the default locale is "en"
    And the error code enum has 3 constants
    When the error dictionary service is initialized
    Then the missing error codes should contain "CODE_ONE, CODE_TWO, CODE_THREE"

  Scenario: Get missing error codes after filtering invalid entries
    Given an error dictionary with the following translations:
      | errorCode | locale | message         |
      | CODE_ONE  | en     | English message |
      | CODE_TWO  |        | Invalid locale  |
      |           | en     | Invalid code    |
    And the default locale is "en"
    And the error code enum has 3 constants
    When the error dictionary service is initialized
    Then the missing error codes should contain "CODE_TWO, CODE_THREE"
