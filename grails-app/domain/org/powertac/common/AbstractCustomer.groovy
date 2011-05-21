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
import org.joda.time.Instant
import org.powertac.common.interfaces.NewTariffListener

/**
 * Abstract customer implementation
 * @author Antonios Chrysopoulos
 */
class AbstractCustomer {
  private static final log = LogFactory.getLog(this)

  def timeService
  def tariffMarketService

  /** The id of the Abstract Customer */
  String id

  /** The Customer specifications*/
  CustomerInfo customerInfo

  /** The list of the published tariffs, pusblished and refreshed within a certain period */
  //List<Tariff> publishedTariffs = []

  /** Number of distinct entities (households) represented by this model */
  //Integer population = 100

  /** >0: max power consumption (think consumer with fuse limit); <0: min power production (think nuclear power plant with min output) */
  BigDecimal upperPowerCap = 100.0

  /** >0: min power consumption (think refrigerator); <0: max power production (think power plant with max capacity) */
  BigDecimal lowerPowerCap = 0.0

  /** >=0 - gram CO2 per kW/h */
  BigDecimal carbonEmissionRate = 0.0

  /** measures how wind changes translate into load / generation changes of the customer */
  BigDecimal windToPowerConversion = 0.0

  /** measures how temperature changes translate into load / generation changes of the customer */
  BigDecimal tempToPowerConversion = 0.0

  /** measures how sun intensity changes translate into load /generation changes of the customer */
  BigDecimal sunToPowerConversion = 0.0

  //TODO: Possibly add parameters as the ones below that provide descriptive statistical information on historic power consumption / production of the customer
  /*
   BigDecimal annualPowerAvg // >0: customer is on average a consumer; <0 customer is on average a producer
   private BigDecimal minResponsiveness // define factor characterizing minimal responsiveness to price signals, i.e. "elasticity"
   private BigDecimal maxResponsiveness;   // define factor characterizing max responsiveness to price signals, i.e. "elasticity"
   */

  /** The subscriptions the customer is under at anytime. Must be at least one, beginning with the default tariff */
  static hasMany = [subscriptions: TariffSubscription]

  static constraints = {

    id(nullable: false, blank: false)
    customerInfo(nullable: false)
    upperPowerCap (nullable: false, scale: Constants.DECIMALS)
    lowerPowerCap (nullable: false, scale: Constants.DECIMALS)

  }

  static mapping = { id (generator: 'assigned') }

  static transients = ['population']

  static auditable = true

  public String toString() {
    return customerInfo.getName()
  }

  int getPopulation ()
  {
    return customerInfo.population
  }


  //============================= INITIALIZATION=================================================
  /** The initialization actions. We can add more
   * Subscribe to the default tariff for the beginning of the game */
  void init(){

    this.id = customerInfo.getId()

    def listener = [publishNewTariffs:{tariffList -> possibilityEvaluationNewTariffs(tariffList) }] as NewTariffListener
    tariffMarketService.registerNewTariffListener(listener)

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

    this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariff, this, customerCount))
    log.info "${this.toString()} was subscribed to the Tariff ${tariff.toString()} successfully."
    this.save()
  }

  /** Unsubscribing certain subscription */
  void unsubscribe(Tariff tariff, int customerCount) {
    TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)

    ts.unsubscribe(customerCount)
    log.info "${this.toString()} was unsubscribed from the Tariff ${tariff.toString()} successfully."
    ts.save()
    this.save()
  }

  /** Subscribing certain subscription */
  void subscribe(TariffSubscription ts) {

    this.addToSubscriptions(ts)
    log.info "${this.toString()} was subscribed to the subscription ${ts.toString()} successfully."
    ts.save()
    this.save()
  }

  /** Unsubscribing certain subscription */
  void unsubscribe(TariffSubscription ts) {

    this.removeFromSubscriptions(ts)
    log.info "${this.toString()} was unsubscribed from the subscription ${ts.toString()} successfully."
    ts.save()
    this.save()
  }

  //============================= CONSUMPTION - PRODUCTION =================================================

  /** The first implementation of the power consumption function.
   *  I utilized the mean consumption of a neighborhood of households with a random variable */
  void consumePower() {
    Timeslot ts =  Timeslot.currentTimeslot()
    double summary = 0
    subscriptions.each { sub ->

      if (ts == null) summary = getConsumptionByTimeslot(sub)
      else summary = getConsumptionByTimeslot(ts.serialNumber)

      log.info " Consumption Load: ${summary} / ${subscriptions.size()} "
      sub.usePower(summary/subscriptions.size())
    }
  }


  double getConsumptionByTimeslot(int serial) {

    int hour = (int) (serial % Constants.HOURS_OF_DAY)
    double ran = 0,summary = 0

    log.info " Hour: ${hour} "
    for (int i = 0; i < population;i++){

      if (hour < 7)
      {
        ran = Math.random()
        summary = summary + ran
      }
      else if (hour < 18){

        ran = 3 + Math.random()
        summary = summary + ran

      }
      else {
        ran = 2 + Math.random()
        summary = summary + ran
      }
      log.info "Summary: ${summary}"
      return summary
    }
  }

  double getConsumptionByTimeslot(TariffSubscription sub) {

    int hour = timeService.getHourOfDay()
    double ran = 0, summary = 0

    log.info "Hour: ${hour} "

    for (int i = 0; i < sub.customersCommitted;i++){

      if (hour < 7)
      {
        ran = Math.random()
        summary = summary + ran
      }
      else if (hour < 18){

        ran = 3 + Math.random()
        summary = summary + ran

      }
      else {
        ran = 2 + Math.random()
        summary = summary + ran
      }
      log.info "Summary: ${summary}"
      return summary
    }
  }

  /** The first implementation of the power consumption function.
   *  I utilized the mean consumption of a neighborhood of households with a random variable */
  void producePower(){

    subscriptions.each { sub ->

      def summary = 0

      for (int i=0;i < sub.customersCommitted;i++) {
        double ran = 6.15 + Math.random()
        summary = summary + ran
      }
      //it.usePower(summary)
    }
  }

  //============================= TARIFF SELECTION PROCESS =================================================

  /** The first implementation of the changing subscription function.
   * There are going to be many cases in the more detailed and complex models. */
  void changeSubscription(Tariff tariff, boolean flag) {

    TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)
    int populationCount = ts.customersCommitted
    this.unsubscribe(tariff, populationCount)

    this.selectTariff(flag).each { newTariff ->
      this.addToSubscriptions(tariffMarketService.subscribeToTariff(newTariff, this, populationCount))
    }

    this.save()
  }

  /** The first implementation of the tariff selection function.
   * This is a random chooser of the available tariffs, totally insensitive*/
  List<Tariff> selectTariff(boolean flag) {
    List<Tariff> available
    List<Tariff> result = []
    int ran, index
    customerInfo.powerTypes.each { powerType ->
      if (flag) {
        available = tariffMarketService.getActiveTariffList(powerType)
        log.info "Available Tariffs : ${available.toString()} "
        index = available.indexOf(tariffMarketService.getDefaultTariff(powerType))
        log.info "Index of Default Tariff: ${index} "
      }
      else {
        available = publishedTariffs
        index = available.indexOf(this.subscriptions?.tariff)
        log.info "Index of Current Tariff: ${index} "
      }

      ran = index

      while ( ran == index) {
        ran = available.size() * Math.random()
      }
      result << available.get(ran)
    }
    return result
  }


  /** The first implementation of the checking for revoked subscriptions function.*/
  void checkRevokedSubscriptions(){

    List<TariffSubscription> revoked = tariffMarketService.getRevokedSubscriptionList(this)
    log.info "Revoked Tariffs : ${revoked.toString()} "

    revoked.each { revokedSubscription ->

      TariffSubscription ts = revokedSubscription.handleRevokedTariff()
      this.unsubscribe(revokedSubscription,)
      this.subscribe(ts)

      ts.save()
      this.save()
    }
  }

  void simpleEvaluationNewTariffs(List<Tariff> newTariffs) {

    double minEstimation = Double.POSITIVE_INFINITY
    int index = 0, minIndex = 0

    newTariffs.each { tariff ->
      log.info "Tariff : ${tariff.toString()} Tariff Type : ${tariff.powerType}"

      if (tariff.isExpired() == false && customerInfo.powerTypes.find{tariff.powerType == it} ){

        minEstimation = (double)Math.min(minEstimation,this.costEstimation(tariff))
        minIndex = index

      }

      index++
    }

    log.info "Tariff:  ${newTariffs.getAt(minIndex).toString()} Estimation = ${minEstimation} "

    subscriptions.each { sub ->

      int populationCount = sub.customersCommitted
      this.unsubscribe(sub.tariff, populationCount)

      this.subscribe(newTariffs.getAt(minIndex),  populationCount)
    }

    this.save()

  }

  void possibilityEvaluationNewTariffs(List<Tariff> newTariffs) {

    Vector estimation = new Vector()

    newTariffs.each { tariff ->
      log.info "Tariff : ${tariff.toString()} Tariff Type : ${tariff.powerType}"

      if (tariff.isExpired() == false && customerInfo.powerTypes.find{tariff.powerType == it} ){
        estimation.add(-(costEstimation(tariff)))
      }

    }

    int minIndex = logitPossibilityEstimation(estimation)

    subscriptions.each { sub ->

      int populationCount = sub.customersCommitted
      this.unsubscribe(sub.tariff, populationCount)

      this.subscribe(newTariffs.getAt(minIndex),  populationCount)
    }

    this.save()

  }

  double costEstimation(Tariff tariff) {

    double costVariable = estimateVariableTariffPayment(tariff)
    double costFixed = estimateFixedTariffPayments(tariff)
    return costVariable + costFixed

  }

  double estimateFixedTariffPayments(Tariff tariff)
  {
    double lifecyclePayment = (double)tariff.getEarlyWithdrawPayment() + (double)tariff.getSignupPayment()
    double minDuration

    // When there is not a Minimum Duration of the contract, you cannot divide with the duration because you don't know it.
    if (tariff.getMinDuration() == 0) minDuration = (5 * TimeService.DAY)
    else minDuration = tariff.getMinDuration()

    log.info("Minimum Duration: ${minDuration}")

    return ((double)tariff.getPeriodicPayment() + (lifecyclePayment / minDuration))
  }

  double estimateVariableTariffPayment(Tariff tariff){

    int serial = ((timeService.currentTime.millis - timeService.base) / TimeService.HOUR)
    Instant base = timeService.currentTime - serial*TimeService.HOUR

    int day = (int) (serial / 24) + 1 // this will be changed to one or more random numbers
    Instant now = base + day * TimeService.DAY

    double costSummary = 0
    double summary = 0, cumulativeSummary = 0

    for (int i=0;i < 24;i++){

      summary = getConsumptionByTimeslot(i)

      cumulativeSummary += summary
      costSummary += tariff.getUsageCharge(now,summary,cumulativeSummary)

      log.info "Time:  ${now.toString()} costSummary: ${costSummary} "

      now = now + TimeService.HOUR
    }
    log.info "Variable cost Summary: ${costSummary}"
    return costSummary

  }

  int logitPossibilityEstimation(Vector estimation) {

    double lamda = 0.3 // 0 the random - 10 the logic

    double summedEstimations = 0
    Vector randomizer = new Vector()
    int[] possibilities = new int[estimation.size()]

    for (int i=0;i < estimation.size();i++){
      summedEstimations += Math.pow(2.7,lamda*estimation.get(i))
      log.info"Summary of Estimation: ${summedEstimations}"
    }

    for (int i = 0;i < estimation.size();i++){
      possibilities[i] = (int)(100 *(Math.pow(2.7,lamda*estimation.get(i)) / summedEstimations))
      for (int j=0;j < possibilities[i]; j++){
        randomizer.add(i)
      }
    }

    //log.info "Randomizer Vector: ${randomizer}"
    log.info "Possibility Vector: ${possibilities.toString()}"
    int index = randomizer.get((int)(randomizer.size()*Math.random()))
    log.info "Resulting Index = ${index}"
    return index

  }

  void step(){

    this.checkRevokedSubscriptions()
    this.consumePower()
  }
}
