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

import org.powertac.common.CashUpdate;
import org.powertac.common.PositionUpdate;
import org.powertac.common.exceptions.*;
import org.powertac.common.msg.*;

import java.util.List;

/**
 * Common interface for the PowerTAC accounting service.
 * The accouting service module is owner of the {@link org.powertac.common.CashUpdate},
 * {@link org.powertac.common.PositionUpdate}, {@link org.powertac.common.TariffTransaction},
 * and {@link org.powertac.common.Tariff} database
 * tables. It's main purpose is to do the bookeeping throught the competition by
 * manipulating the above mentioned database tables recording cash, position, tariff or
 * metering changes.
 *
 * @author Carsten Block
 * @version 0.2 - January 14th, 2011
 */
public interface AccountingService {

  /**
   * Processes {@link PositionDoUpdateCmd} objects adjusting the booked amounts
   * of a product (e.g. energy futures) for particular broker and a particular timeslot
   *
   * @param positionDoUpdateCmd the object that describes what position change to book in the database
   * @return PositionUpdate Latest {@link PositionUpdate} which contains relative change, new overall balance, origin and reason for the position change
   * @throws org.powertac.common.exceptions.PositionUpdateException is thrown if a position updated fails
   */
  public PositionUpdate processPositionUpdate (PositionDoUpdateCmd positionDoUpdateCmd) throws PositionUpdateException;

  /**
   * Processes {@link CashDoUpdateCmd} objects adjusting the booked amounts of cash for a specific broker.
   * @param cashDoUpdateCmd the object that describes what cash change to book in the database
   * @return CashUpdate Latest {@link CashUpdate} which contains relative change, new overall balance, origin and reason for the cash update
   */
  public CashUpdate processCashUpdate(CashDoUpdateCmd cashDoUpdateCmd);

  /**
   * Publishes the list of available customers (which might be empty)
   * JEC - I don't know what this is for, who would call it, or why it's part of AccountingService
   * @return a list of all available customers, which might be empty if no customers are available
   */
  //public List<Customer> publishCustomersAvailable();

}
