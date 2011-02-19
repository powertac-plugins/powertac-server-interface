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

import org.powertac.common.TariffTransaction;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Weather;
import org.powertac.common.command.TariffDoSubscribeCmd;
import org.powertac.common.exceptions.CustomerInfoGenerationException;
import org.powertac.common.exceptions.MeterReadingException;
import org.powertac.common.exceptions.TariffSubscriptionException;
import org.powertac.common.exceptions.WeatherProcessingException;

import java.util.List;

//import org.powertac.common.command.TariffDoPublishCmd;
//import org.powertac.common.command.TariffDoReplyCmd;

/**
 * Interface that specifies common methods a Customer module needs to implement.
 *
 * @author Carsten Block
 * @version 0.1 - January 2nd, 2011
 */
public interface Customer {

  /**
   * Updates a customer's subscription set.
   * @param tariffList list of published tariffs
   * @return A list of tariff subscription records (possibly empty). For customer models with
   * multi-contracting capabilities, this list can contain several objects, each
   * representing a subscription of a certain share of the overall customer load / generation
   * capacity to a particular tariff. 
   * @throws org.powertac.common.exceptions.TariffSubscriptionException thrown if tariff processing by the customer fails
   */
  public List<TariffDoSubscribeCmd> processTariffList(List<Tariff> tariffList) throws TariffSubscriptionException;

  /**
   * Called when new weather forecasts are available
   *
   * @param weatherList new weather forecasts
   * @throws org.powertac.common.exceptions.WeatherProcessingException thrown if processing of the weather forecasts fails
   */
  public void processWeatherForecasts(List<Weather> weatherList) throws WeatherProcessingException;

  /**
   * Called to make the customer model produce its "real consumption" / real production
   * based on the given subscription (which includes current and future prices), and on
   * "real weather data" (which might only be relevant to customer
   * models that react to weather impact, such as PV or wind turbine customers.
   *
   * @param weather real measured weather data for a particular timeslot
   * @return real consumption / production of the customer for the timeslot specified in the given {@link Weather}
   * @throws org.powertac.common.exceptions.MeterReadingException thrown if the meter reading fails
   */
  public TariffTransaction generateMeterReading(TariffSubscription subscription, Weather weather) throws MeterReadingException;

  /**
   * As soon as this method is called the customer model is required to store / update
   * its own {@link org.powertac.common.Customer} instance in the database, which is used to publicly report some common properties describing the customer model
   * @see org.powertac.common.Customer
   * @return a customer object that contains customer master data (i.e. a generic description of the customer)
   * @throws org.powertac.common.exceptions.CustomerInfoGenerationException thrown if the generation of the customer object fails
   */
  public Customer generateCustomerInfo() throws CustomerInfoGenerationException;
}
