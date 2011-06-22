/*
 * Copyright (c) 2011 by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common

import java.util.List;

import org.powertac.common.interfaces.InitializationService

/**
 * Stores expiration array data for TariffSubscription instances
 * that hibernate is ignoring.
 * @author John Collins
 */
class TariffSubscriptionService
  implements InitializationService
{
  static transactional = false

  Map expirations = [:]

  @Override
  public void setDefaults ()
  {
    // nothing to do here    
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    expirations = [:]
    return 'TariffSubscriptionService'
  }

  List getExpirations (TariffSubscription subscription)
  {
    long subId = subscription.id
    if (expirations[subId] == null) {
      log.info "create expirations list for subscription ${subId}"
      expirations[subId] = []
    }
    return expirations[subId]
  }
  
  void addExpiration (TariffSubscription subscription, List data)
  {
    long subId = subscription.id
    if (expirations[subId] == null) {
      log.info "create expirations list for subscription ${subId}"
      expirations[subId] = []
    }
    expirations[subId].add(data)
  }
}
