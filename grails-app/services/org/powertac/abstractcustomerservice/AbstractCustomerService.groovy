/*
* Copyright 2011 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an
* "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
* either express or implied. See the License for the specific language
* governing permissions and limitations under the License.
*/
package org.powertac.abstractcustomerservice

import org.joda.time.Instant
import org.powertac.common.AbstractCustomer
import org.powertac.common.CustomerInfo
import org.powertac.common.PluginConfig
import org.powertac.common.enumerations.CustomerType
import org.powertac.common.enumerations.PowerType
import org.powertac.common.interfaces.TimeslotPhaseProcessor


class AbstractCustomerService implements TimeslotPhaseProcessor 
{
  static transactional = true

  def timeService // autowire
  def competitionControlService

  // JEC - this attribute is a property of a CustomerInfo. Why is it here?
  int population = 1

  void init(PluginConfig config) 
  {
    competitionControlService?.registerTimeslotPhase(this, 1)
    competitionControlService?.registerTimeslotPhase(this, 2)

    Integer value = config.configuration['population']?.toInteger()
    if (value == null) {
      log.error "Missing value for population. Default is ${population}"
    } 
    population = value
    
    Integer numberOfCustomers = config.configuration['numberOfCustomers']?.toInteger()
    if (value == null) {
      log.error "Missing value for numberOfCustomers. Default is 1"
      numberOfCustomers = 1
    }
    for (int i = 1; i < numberOfCustomers + 1; i++){
      def abstractCustomerInfo =
          new CustomerInfo(Name: "Customer " + i,customerType: CustomerType.CustomerHousehold,
                           population: population, powerTypes: [PowerType.CONSUMPTION])
      assert(abstractCustomerInfo.save())
      def abstractCustomer = new AbstractCustomer(customerInfo: abstractCustomerInfo)
      abstractCustomer.init()
      abstractCustomer.subscribeDefault()
      assert(abstractCustomer.save())
    }
  }

  void activate(Instant now, int phase) {

    log.info "Activate"
    def abstractCustomerList = AbstractCustomer.list()

    if (phase == 1){
      abstractCustomerList*.step()
    }  
    else {
      abstractCustomerList*.toString()
    }
  }
}