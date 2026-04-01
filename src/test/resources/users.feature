@E2E
Feature: user management
  Scenario: client creates and retrieves users
    When the client creates a user named "Anakin" with email anakin@jedi.org
    Then the user response status code is 201
    When the client creates a user named "Obi-Wan" with email obiwan@jedi.org
    Then the user response status code is 201
    When the client retrieves all users
    Then the client can see at least 2 users

  Scenario: client retrieves user by id
    Given a user named "Yoda" with email yoda@jedi.org exists
    When the client retrieves the user by id
    Then the user response status code is 200
    And the user has name "Yoda" and email yoda@jedi.org

  Scenario: client retrieves user by email
    Given a user named "Mace" with email mace@jedi.org exists
    When the client retrieves the user by email mace@jedi.org
    Then the user response status code is 200
    And the user has name "Mace" and email mace@jedi.org

  Scenario: client updates user name
    Given a user named "Padme" with email padme@naboo.org exists
    When the client changes the user name to "Queen Amidala"
    Then the user response status code is 200
    And the user has name "Queen Amidala" and email padme@naboo.org

  Scenario: client deletes user
    Given a user named "Jar Jar" with email jarjar@naboo.org exists
    When the client deletes the user
    Then the user response status code is 204
    When the client tries to retrieve the deleted user by id
    Then the user response status code is 404

  Scenario: client cannot create duplicate email
    Given a user named "Qui-Gon" with email quigon@jedi.org exists
    When the client tries to create a user named "Qui-Gon Clone" with email quigon@jedi.org
    Then the user response status code is 400