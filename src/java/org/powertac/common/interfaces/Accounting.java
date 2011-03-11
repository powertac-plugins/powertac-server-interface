/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common.interfaces;

import org.powertac.common.CustomerInfo;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Product;
import org.powertac.common.Tariff;
import org.powertac.common.TariffTransaction;
import org.powertac.common.enumerations.TariffTransactionType;

import java.math.BigDecimal;

/**
 * Common interface for the PowerTAC accounting service.
 * The accouting service module is responsible for bookeeping, with respect to
 * cash (broker bank accounts) and market positions on behalf of brokers. All activities
 * that could potentially affect cash or market positions must be recorded by making
 * calls on the Accounting API. Summaries of accounts, along with all new transactions,
 * are sent to brokers at the end of each timeslot. 
 * <p>
 * Accounting is also a TimeslotPhaseProcessor, running in the last phase of the
 * simulation cycle. All processors that can generate transactions must run in
 * earlier phases.</p>
 *
 * @author John Collins
 */
public interface Accounting extends TimeslotPhaseProcessor
{
  /**
   * Adds a market transaction that includes both a cash component
   * and a product commitment
   */
  MarketTransaction addMarketTransaction (Product product,
                                          BigDecimal positionChange,
                                          BigDecimal cashChange);

  /**
   * Adds a tariff transaction to the current-day transaction list.
   */
  TariffTransaction addTariffTransaction (TariffTransactionType txType, Tariff tariff, 
                                          CustomerInfo customer, int customerCount,
                                          BigDecimal amount, BigDecimal charge);
}
