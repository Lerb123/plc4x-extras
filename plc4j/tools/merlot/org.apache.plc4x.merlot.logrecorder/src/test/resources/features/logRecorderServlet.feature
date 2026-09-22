<<<<<<< HEAD
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

=======
>>>>>>> 169fec95 (General Changes Following the Migration to Version 1.0.0 of plc4x)
Feature: Create a fault report
    This test simulates what would happen if you created a log from the 
    CsStudio/Phoebus graphical interface, as well as viewing it from its
    logbook interface.

  Scenario: Maintenance Report
    Given The user enters their credentials: username: "operator" password: "operator"
    And The user selects the "URGENT" reporting level
    And The user selects the "Maintenance" tag and the "Breakdown" category
    And The user adds a description: "Check the power wiring on the local panel"
    And Attached image from the maintenance screen "olog_2132687163876.png"
    And Add a title: "Request for Replacement Parts for Machine XX-YY-ZZ"
    When The user clicks the submit button
    Then Returns the JSON from the created report and an HTTP 200 response


