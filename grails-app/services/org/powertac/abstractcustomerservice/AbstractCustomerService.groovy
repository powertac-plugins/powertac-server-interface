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


class AbstractCustomerService implements TimeslotPhaseProcessor {

  static transactional = true

  def timeService // autowire
  def competitionControlService

  PluginConfig configuration

  void afterPropertiesSet ()
  {
    competitionControlService.registerTimeslotPhase(this, 1)
    competitionControlService.registerTimeslotPhase(this, 2)
  }

  // ----------------- Configuration access ------------------
  BigDecimal getPopulation()
  {
    return configuration.configuration['population'].toBigDecimal()
  }
  
  BigDecimal getNumberOfCustomers()
  {
    return configuration.configuration['numberOfCustomers'].toBigDecimal()
  }


  void init(PluginConfig config) {

    configuration = config
    
    for (int i = 1; i < getNumberOfCustomers()+1;i++){
      def abstractCustomerInfo = new CustomerInfo(Name: "Customer " + i,customerType: CustomerType.CustomerHousehold, powerTypes: [PowerType.CONSUMPTION])
      assert(abstractCustomerInfo.save())
      def abstractCustomer = new AbstractCustomer(CustomerInfo: abstractCustomerInfo)
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