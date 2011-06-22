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

/**
 * Abstract customer implementation
 * @author Antonios Chrysopoulos
 */
class AbstractProducer extends AbstractCustomer{
  private static final log = LogFactory.getLog(this)


  //============================= CONSUMPTION - PRODUCTION =================================================

  /** The first implementation of the power consumption function.
   *  I utilized the mean consumption of a neighborhood of households with a random variable */
  void producePower()
  {
    Timeslot ts =  Timeslot.currentTimeslot()
    double summary = 0
    subscriptions.each { sub ->
      if (ts == null) summary = getProductionByTimeslot(sub)
      else summary = getProductionByTimeslot(ts.serialNumber)
      log.info "Production Load: ${summary} / ${subscriptions.size()} "

      sub.usePower(-(summary/subscriptions.size()))
    }
  }


  double getProductionByTimeslot(int serial) {

    int hour = (int) (serial % Constants.HOURS_OF_DAY)

    log.info " Hour: ${hour} "

    Random ran = new Random()
    return 100 * ran.nextFloat()

  }

  double getProductionByTimeslot(TariffSubscription sub) {

    int hour = timeService.getHourOfDay()
    log.info "Hour: ${hour} "

    Random ran = new Random()
    return 100 * ran.nextFloat()

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

    double lamda = 250000 // 0 the random - 10 the logic
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
    println("Timeslot: " + timeService.currentTime + " Customer: " + this.toString())
    this.checkRevokedSubscriptions()
    this.producePower()
  }
}
