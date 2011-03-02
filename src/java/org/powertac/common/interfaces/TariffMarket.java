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

import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.command.TariffDoRevokeCmd;

/**
 * Tariff Market Receives, validates, and stores new tariffs, enforces tariff 
 * validity rules. Generates transactions to represent tariff publication fees.
 * Provides convenience methods to find tariffs that might be of interest to Customers.
 *
 * @author John Collins
 */
public interface TariffMarket {

  /**
   * Processes incoming {@link TariffSpecification} of a broker, 
   * turns it into a Tariff instance, and returns it.
   */
  public Tariff processNewTariff(TariffSpecification tariffSpec);

  /**
   * Evaluates a given tariff by checking tariff objects against a
   * pre-defined set of commonly agreed PowerTAC market rules. Only
   * tariff objects that comply with these rules can pass this filter.
   *
   * Sends a TariffRejectNotification notification back to the broker if an error occurs 
   *   during tariff validation. Note that non-conformant tariffs do *not* cause an 
   *   exception but simply result in "false" being returned. Exceptions should only 
   *   occur of non tariff objects are provided or if, e.g., database access is broken.
   */
  public boolean acceptTariff(Object tariff);

  /**
   * Method processes incoming {@link TariffDoRevokeCmd} of a broker. This method needs to
   * implement logic that leads to the given tariff being revoked from the list of
   * published tariffs.
   *
   * @param tariffDoRevokeCmd describing the tariff to be revoked
   * @return Tariff updated tariff object that reflects the revocation of the tariff
   */
  public Tariff processTariffRevoke(TariffDoRevokeCmd tariffDoRevokeCmd);

  /**
   * Method processes incoming {@link TariffDoSubscribeCmd}. This method implements the
   * logic required to make a customer subscribe to a particular tariff given either
   * (i) a published or (ii) an individually agreed tariff instance to subscribe to.<br/>
   * JEC - commented out until we know what it's for and who calls it
   *
   * @param tariffDoSubscribeCmd contains references to the subscribing customer and to the tariff instance to subscribe to
   * @return List of objects which can include {@link CashUpdate} and {@link Tariff}. The tariff object reflects the subscription of the customer defined in the {@link TariffDoSubscribeCmd} while the (optional) {@link CashUpdate} contains the cash booking of the (optional) signupFee into the broker's cash account
   * @throws TariffSubscriptionException is thrown if the subscription fails
   */
  //public List processTariffSubscribe (TariffDoSubscribeCmd tariffDoSubscribeCmd) throws TariffSubscriptionException;

  /**
   * Returns a list of all currently active (i.e. subscribeable) tariffs (which might be empty)<br/>
   * JEC - commented out until we know what it's supposed to do.
   *
   * @return a list of all active tariffs, which might be empty if no tariffs are published
   */
  //public List<Tariff> publishTariffList();

}
