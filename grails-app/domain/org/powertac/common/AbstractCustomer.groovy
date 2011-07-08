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
package org.powertac.common

import java.util.List

import org.apache.commons.logging.LogFactory
import org.powertac.common.enumerations.PowerType

/**
 * Abstract customer implementation
 * @author Antonios Chrysopoulos
 */
class AbstractCustomer {
  private static final log = LogFactory.getLog(this)

  def timeService
  def tariffMarketService

  /** The id of the Abstract Customer */
  String custId

  /** The Customer specifications*/
  CustomerInfo customerInfo

  /** >0: max power consumption (think consumer with fuse limit); <0: min power production (think nuclear power plant with min output) */
  double upperPowerCap = 100.0

  /** >0: min power consumption (think refrigerator); <0: max power production (think power plant with max capacity) */
  double lowerPowerCap = 0.0

  /** >=0 - gram CO2 per kW/h */
  double carbonEmissionRate = 0.0

  /** measures how wind changes translate into load / generation changes of the customer */
  double windToPowerConversion = 0.0

  /** measures how temperature changes translate into load / generation changes of the customer */
  double tempToPowerConversion = 0.0

  /** measures how sun intensity changes translate into load /generation changes of the customer */
  double sunToPowerConversion = 0.0


  /** The subscriptions the customer is under at anytime. Must be at least one, beginning with the default tariff */
  static hasMany = [subscriptions: TariffSubscription]

  static fetchMode = [customerInfo:"eager"]

  static constraints = {
    custId(nullable: false, blank: false)
    customerInfo(nullable: false)
    upperPowerCap (scale: Constants.DECIMALS)
    lowerPowerCap (scale: Constants.DECIMALS)
  }

  static transients = ['population']

  static auditable = true

  public String toString() {
    return customerInfo.getName()
  }

  int getPopulation () {
    return customerInfo.population
  }


  //============================= INITIALIZATION=================================================
  /** The initialization actions. We can add more
   * Subscribe to the default tariff for the beginning of the game */
  void init()
  {
    this.custId = customerInfo.getId()

    this.save()
  }


  //============================= SUBSCRIPTION =================================================

  /** Function utilized at the beginning in order to subscribe to the default tariff */
  void subscribeDefault() {

    customerInfo.powerTypes.each { type ->

      if (tariffMarketService.getDefaultTariff(type) == null){
        log.info "No default Subscription for type ${type.toString()} for ${this.toString()} to subscribe to."
      }
      else {
        this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(type), this, population))
        log.info "${this.toString()} was subscribed to the default broker successfully."
      }
    }
    this.save()
  }

  /** Subscribing certain subscription */
  void subscribe(Tariff tariff, int customerCount){
    this.addSubscription(tariffMarketService.subscribeToTariff(tariff, this, customerCount))
    log.info "${this.toString()} was subscribed to ${tariff.toString()} successfully."
  }

  /** Unsubscribing certain subscription */
  void unsubscribe(TariffSubscription subscription, int customerCount) {
    subscription.unsubscribe(customerCount)
    log.info "${this.toString()} was unsubscribed from ${subscription.tariff.toString()} successfully."
    if (subscription.customersCommitted == 0)removeSubscription(subscription)
    else subscription.save()
    this.save()
  }

  /** Subscribing certain subscription */
  void addSubscription(TariffSubscription ts)
  {
    this.addToSubscriptions(ts)
    log.info "${this.toString()} was subscribed to the subscription ${ts.toString()} successfully."
    this.save()
  }

  /** Unsubscribing certain subscription */
  void removeSubscription(TariffSubscription ts)
  {
    this.removeFromSubscriptions(ts)
    log.info "${this.toString()} was unsubscribed from the subscription ${ts.toString()} successfully."
    ts.delete()
    this.save()
  }

  //============================= CONSUMPTION - PRODUCTION =================================================

  /** The first implementation of the power consumption function.
   *  I utilized the mean consumption of a neighborhood of households with a random variable */
  void consumePower(){ }



  /** The first implementation of the power consumption function.
   *  I utilized the mean consumption of a neighborhood of households with a random variable */
  void producePower(){}


  //============================= TARIFF SELECTION PROCESS =================================================

  /** The first implementation of the changing subscription function.
   *  Here we just put the tariff we want to change and the whole population 
   * is moved to another random tariff.
   * @param tariff
   */
  void changeSubscription(Tariff tariff)
  {
    TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)
    int populationCount = ts.customersCommitted
    unsubscribe(ts, populationCount)

    def newTariff = selectTariff(tariff.tariffSpec.powerType)
    subscribe(newTariff,populationCount)
    this.save()
  }

  /** In this overloaded implementation of the changing subscription function,
   *  Here we just put the tariff we want to change and the whole population 
   * is moved to another random tariff.
   * @param tariff
   */
  void changeSubscription(Tariff tariff, Tariff newTariff)
  {
    TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)
    int populationCount = ts.customersCommitted
    unsubscribe(ts, populationCount)
    subscribe(newTariff,populationCount)
    this.save()
  }


  /** In this overloaded implementation of the changing subscription function,
   * Here we just put the tariff we want to change and amount of the population 
   * we want to move to the new tariff.
   * @param tariff
   */
  void changeSubscription(Tariff tariff, Tariff newTariff, int populationCount)
  {
    TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)
    unsubscribe(ts, populationCount)
    subscribe(newTariff,populationCount)
    this.save()
  }


  /** The first implementation of the tariff selection function.
   * This is a random chooser of the available tariffs, totally insensitive.*/
  Tariff selectTariff(PowerType powerType) {
    Tariff result = new Tariff()
    List<Tariff> available = []
    int ran, index
    available = tariffMarketService.getActiveTariffList(powerType)
    log.info "Available Tariffs for ${powerType}: ${available.toString()} "
    index = available.indexOf(tariffMarketService.getDefaultTariff(powerType))
    log.info "Index of Default Tariff: ${index} "

    ran = index
    while ( ran == index) {
      ran = available.size() * Math.random()
    }
    result = available.get(ran)
    return result
  }


  /** The first implementation of the checking for revoked subscriptions function.*/
  void checkRevokedSubscriptions(){

    List<TariffSubscription> revoked = tariffMarketService.getRevokedSubscriptionList(this)
    log.info "Revoked Tariffs : ${revoked.toString()} "

    revoked.each { revokedSubscription ->

      TariffSubscription ts = revokedSubscription.handleRevokedTariff()
      this.removeSubscription(revokedSubscription)
      this.addSubscription(ts)
      this.save()
    }
  }


  /** This function returns the bootstrap data of the certain customer in the correct form
   * 
   * @return
   */
  def getBootstrapData(){}


  void step(){ }
}
