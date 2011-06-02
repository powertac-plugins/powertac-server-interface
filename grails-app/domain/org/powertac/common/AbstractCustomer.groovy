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
  String id

  /** The Customer specifications*/
  CustomerInfo customerInfo

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

  static fetchMode = [customerInfo:"eager"]

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
  void init()
  {
    this.id = customerInfo.getId()

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
  void consumePower()
  {
    Timeslot ts =  Timeslot.currentTimeslot()
    double summary = 0
    subscriptions.each { sub ->
      if (ts == null) summary = getConsumptionByTimeslot(sub)
      else summary = getConsumptionByTimeslot(ts.serialNumber)
      log.info "Consumption Load: ${summary} / ${subscriptions.size()} "
      sub.usePower(summary/subscriptions.size())
    }
  }


  double getConsumptionByTimeslot(int serial) {

    int hour = (int) (serial % Constants.HOURS_OF_DAY)
    double ran = 0,summary = 0

    log.info " Hour: ${hour} "
    for (int i = 0; i < population;i++){
      if (hour < Constants.MORNING_START_HOUR)
      {
        ran = Constants.MEAN_NIGHT_CONSUMPTION + Math.random()
        summary = summary + ran
      }
      else if (hour < Constants.EVENING_START_HOUR){
        ran = Constants.MEAN_MORNING_CONSUMPTION + Math.random()
        summary = summary + ran
      }
      else {
        ran = Constants.MEAN_EVENING_CONSUMPTION + Math.random()
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
      if (hour < Constants.MORNING_START_HOUR)
      {
        ran = Constants.MEAN_NIGHT_CONSUMPTION + Math.random()
        summary = summary + ran
      }
      else if (hour < Constants.EVENING_START_HOUR){
        ran = Constants.MEAN_MORNING_CONSUMPTION + Math.random()
        summary = summary + ran
      }
      else {
        ran = Constants.MEAN_EVENING_CONSUMPTION + Math.random()
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
      this.removeSubscription(revokedSubscription,)
      this.addSubscription(ts)
      this.save()
    }
  }

  void simpleEvaluationNewTariffs(List<Tariff> newTariffs) {

    // if there are no current subscriptions, then this is the
    // initial publication of default tariffs
    if (subscriptions == null || subscriptions.size() == 0) {
      subscribeDefault()
      return
    }

    double minEstimation = Double.POSITIVE_INFINITY
    int index = 0, minIndex = 0

    //adds current subscribed tariffs for reevaluation
    def evaluationTariffs = new ArrayList(newTariffs)
    Collections.copy(evaluationTariffs,newTariffs)
    evaluationTariffs.addAll(subscriptions?.tariff)


    log.debug("Estimation size for ${this.toString()}= " + evaluationTariffs.size())
    if (evaluationTariffs.size()> 1) {
      evaluationTariffs.each { tariff ->
        log.info "Tariff : ${tariff.toString()} Tariff Type : ${tariff.powerType}"
        if (tariff.isExpired() == false && customerInfo.powerTypes.find{tariff.powerType == it} ){
          minEstimation = (double)Math.min(minEstimation,this.costEstimation(tariff))
          minIndex = index
        }
        index++
      }
      log.info "Tariff:  ${evaluationTariffs.getAt(minIndex).toString()} Estimation = ${minEstimation} "

      subscriptions.each { sub ->
        log.info "Equality: ${sub.tariff.tariffSpec} = ${evaluationTariffs.getAt(minIndex).tariffSpec} "
        if (!(sub.tariff.tariffSpec == evaluationTariffs.getAt(minIndex).tariffSpec)) {
          log.info "Existing subscription ${sub.toString()}"
          int populationCount = sub.customersCommitted
          this.subscribe(evaluationTariffs.getAt(minIndex),  populationCount)
          this.unsubscribe(sub, populationCount)
        }
      }
      this.save()
    }
  }

  void possibilityEvaluationNewTariffs(List<Tariff> newTariffs)
  {
    // if there are no current subscriptions, then this is the
    // initial publication of default tariffs
    if (subscriptions == null || subscriptions.size() == 0) {
      subscribeDefault()
      return
    }
    log.info "Tariffs: ${Tariff.list().toString()}"
    Vector estimation = new Vector()

    //adds current subscribed tariffs for reevaluation
    def evaluationTariffs = new ArrayList(newTariffs)
    Collections.copy(evaluationTariffs,newTariffs)
    evaluationTariffs.addAll(subscriptions?.tariff)

    log.debug("Estimation size for ${this.toString()}= " + evaluationTariffs.size())
    if (evaluationTariffs.size()> 1) {
      evaluationTariffs.each { tariff ->
        log.info "Tariff : ${tariff.toString()} Tariff Type : ${tariff.powerType} Tariff Expired : ${tariff.isExpired()}"
        if (!tariff.isExpired() && customerInfo.powerTypes.find{tariff.powerType == it}) {
          estimation.add(-(costEstimation(tariff)))
        }
        else estimation.add(Double.NEGATIVE_INFINITY)
      }
      int minIndex = logitPossibilityEstimation(estimation)

      subscriptions.each { sub ->
        log.info "Equality: ${sub.tariff.tariffSpec} = ${evaluationTariffs.getAt(minIndex).tariffSpec} "
        if (!(sub.tariff.tariffSpec == evaluationTariffs.getAt(minIndex).tariffSpec)) {
          log.info "Existing subscription ${sub.toString()}"
          int populationCount = sub.customersCommitted
          this.unsubscribe(sub, populationCount)
          this.subscribe(evaluationTariffs.getAt(minIndex),  populationCount)
        }
      }
      this.save()
    }
  }

  double costEstimation(Tariff tariff)
  {
    double costVariable = estimateVariableTariffPayment(tariff)
    double costFixed = estimateFixedTariffPayments(tariff)
    return (costVariable + costFixed)/Constants.MILLION
  }

  double estimateFixedTariffPayments(Tariff tariff)
  {
    double lifecyclePayment = (double)tariff.getEarlyWithdrawPayment() + (double)tariff.getSignupPayment()
    double minDuration

    // When there is not a Minimum Duration of the contract, you cannot divide with the duration because you don't know it.
    if (tariff.getMinDuration() == 0) minDuration = Constants.MEAN_TARIFF_DURATION * TimeService.DAY
    else minDuration = tariff.getMinDuration()

    log.info("Minimum Duration: ${minDuration}")
    return ((double)tariff.getPeriodicPayment() + (lifecyclePayment / minDuration))
  }

  double estimateVariableTariffPayment(Tariff tariff){

    double costSummary = 0
    double summary = 0, cumulativeSummary = 0

    int serial = ((timeService.currentTime.millis - timeService.base) / TimeService.HOUR)
    Instant base = timeService.currentTime - serial*TimeService.HOUR
    int day = (int) (serial / Constants.HOURS_OF_DAY) + 1 // this will be changed to one or more random numbers
    Instant now = base + day * TimeService.DAY

    for (int i=0;i < Constants.HOURS_OF_DAY;i++){
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

    double lamda = 50 // 0 the random - 10 the logic
    double summedEstimations = 0
    Vector randomizer = new Vector()
    int[] possibilities = new int[estimation.size()]

    for (int i=0;i < estimation.size();i++){
      summedEstimations += Math.pow(Constants.EPSILON,lamda*estimation.get(i))
      "Cost variable: ${estimation.get(i)}"
      log.info"Summary of Estimation: ${summedEstimations}"
    }

    for (int i = 0;i < estimation.size();i++){
      possibilities[i] = (int)(Constants.PERCENTAGE *(Math.pow(Constants.EPSILON,lamda*estimation.get(i)) / summedEstimations))
      for (int j=0;j < possibilities[i]; j++){
        randomizer.add(i)
      }
    }

    log.info "Randomizer Vector: ${randomizer}"
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
