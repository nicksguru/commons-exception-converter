#@disabled
Feature: SubclassBeforeSuperclassExceptionIterator functionality
  The iterator processes exception chains ordering subclasses before superclasses

  Scenario: Constructor validation with null exception
    When a SubclassBeforeSuperclassExceptionIterator is created with null exception
    Then the exception message should contain "exception chain"

  Scenario Outline: Iterator processes exception chain in subclass-before-superclass order
    Given an exception chain with types "<exceptionTypes>"
    When a SubclassBeforeSuperclassExceptionIterator is created with the exception chain
    Then the iterator should have next elements
    And the iteration order should be "<expectedOrder>"
    Examples:
      | exceptionTypes                                    | expectedOrder                                     |
      | RuntimeException,Exception                        | RuntimeException,Exception                        |
      | Exception,RuntimeException                        | RuntimeException,Exception                        |
      | NotFoundException,UnauthorizedException,Exception | NotFoundException,UnauthorizedException,Exception |
      | Exception,UnauthorizedException,NotFoundException | UnauthorizedException,NotFoundException,Exception |

  Scenario: Iterator supports Streamable interface
    Given an exception chain with types "RuntimeException,Exception"
    When a SubclassBeforeSuperclassExceptionIterator is created with the exception chain
    Then the iterator should support stream operations
    And the stream should contain the same elements as iteration

  Scenario Outline: AcceptUntilResult with stateless visitor
    Given an exception chain with types "<exceptionTypes>"
    And a SubclassBeforeSuperclassExceptionIterator is created with the exception chain
    And a stateless visitor that returns result for "<targetType>"
    When acceptUntilResult is called with the stateless visitor
    Then the visitor result should be "<expectedResult>"
    Examples:
      | exceptionTypes              | targetType        | expectedResult |
      | RuntimeException,Exception  | RuntimeException  | found          |
      | RuntimeException,Exception  | NotFoundException | empty          |
      | NotFoundException,Exception | NotFoundException | found          |

  Scenario Outline: AcceptUntilResult with stateful visitor
    Given an exception chain with types "<exceptionTypes>"
    And a SubclassBeforeSuperclassExceptionIterator is created with the exception chain
    And a stateful visitor that returns result for "<targetType>"
    And visitor state is initialized
    When acceptUntilResult is called with the stateful visitor and state
    Then the visitor result should be "<expectedResult>"
    And the visitor state should be updated
    Examples:
      | exceptionTypes              | targetType        | expectedResult |
      | RuntimeException,Exception  | RuntimeException  | found          |
      | RuntimeException,Exception  | NotFoundException | empty          |
      | NotFoundException,Exception | NotFoundException | found          |

  Scenario: Iterator returns itself when iterator method is called
    Given an exception chain with types "RuntimeException,Exception"
    When a SubclassBeforeSuperclassExceptionIterator is created with the exception chain
    Then calling iterator method should return the same instance

  Scenario: Iterator handles duplicate exception types
    Given an exception chain with duplicate exception types
    When a SubclassBeforeSuperclassExceptionIterator is created with the exception chain
    Then the iterator should process unique exception types only
    And the iteration should complete successfully
