package org.powertac.common


import java.util.List;
import org.powertac.common.interfaces.NewTariffListener
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;

class AbstractCustomer {

	def timeService
	def tariffMarketService
	
	/** Name of the customer model */
	String name
  
	/** gives a "rough" classification what type of customer to expect based on an enumeration, i.e. a fixed set of customer types */
	CustomerType customerType
	
	/** The list of the published tariffs, pusblished and refreshed within a certain period */
	List<Tariff> publishedTariffs = []
	
	PowerType powerType
  
	/** describes whether or not this customer engages in multiple contracts at the same time */
	Boolean multiContracting = false
  
	/** describes whether or not this customer negotiates over contracts */
	Boolean canNegotiate = false
  
	/** >0: max power consumption (think consumer with fuse limit); <0: min power production (think nuclear power plant with min output) */
	BigDecimal upperPowerCap = 100.0
  
	/** >0: min power consumption (think refrigerator); <0: max power production (think power plant with max capacity) */
	BigDecimal lowerPowerCap = 0.0
	
	/** The subscriptions the customer is under at anytime. Must be at least one, beginning with the default tariff */
	static hasMany = [subscriptions: TariffSubscription]
	
    static constraints = {
		
		name (blank: false, unique: true)
		customerType(nullable: false)
		powerType(nullable: false)
		publishedTariffs(nullable:true)
		multiContracting (nullable: false)
		canNegotiate (nullable: false)
		upperPowerCap (nullable: false, scale: Constants.DECIMALS)
		lowerPowerCap (nullable: false, scale: Constants.DECIMALS)
		
    }
	
	public String toString() {
		return name
	  }
	
	/** The initialization actions. We can add more
	 * Subscribe to the default tariff for the beginning of the game */
	void init(){
		
		def listener = [publishNewTariffs:{tariffList -> publishedTariffs = tariffList }] as NewTariffListener
		tariffMarketService.registerNewTariffListener(listener)
		
		this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(powerType), this, 1))
		println(subscriptions.toString())
		this.save(flush:true)
	}

	/** The first implementation of the tariff selection function. 
	 * This is a random chooser of the available tariffs, totally insensitive*/
	Tariff SelectTariff(boolean flag){
		
		List<Tariff> available
		
		if (flag){
			
			available = tariffMarketService.getActiveTariffList(powerType)
		}
		else {
		 
			available = publishedTariffs
		}
			
		int ran = available.size() * Math.random()
		println(ran);
		return available.get(ran)
		
	}
	
	/** The first implementation of the power consumption function.
	 *  I utilized the mean consumption of a neighborhood of households with a random variable */
	void consumePower(){
		
		subscriptions.each {
		
			double ran = 6.15 + Math.random()
			println(ran);
			it.usePower(ran)
		
		}
	}
	
	/** The first implementation of the power consumption function.
	*  I utilized the mean consumption of a neighborhood of households with a random variable */
	void producePower(){
		
	
	}
	
	/** Unsubscribing the current subscription */
		
	void unsubscribeCurrent(){
		
		if (multiContracting == false && subscriptions.size() == 1){
		
		
			subscriptions.each {
			
					it.unsubscribe(1)
					removeFromSubscriptions(it)
					it.save(flush:true)					
			}
		}
		
	this.save(flush:true)	
	}
	
	/** Unsubscribing a certain subscription */
	void unsubscribe(TariffSubscription ts){
		
		ts.unsubscribe(1)
		removeFromSubscriptions(ts)
		ts.save(flush:true)
		this.save(flush:true)
	}
	
	
	
	/** The first implementation of the checking for revoked subscriptions function.*/
	void checkRevokedSubscriptions(){
		
		List<TariffSubscription> revoked = tariffMarketService.getRevokedSubscriptionList(this)
		
		println(revoked.size())
		
		revoked.each {
			
			TariffSubscription ts = it.handleRevokedTariff()
			unsubscribe(it)
			this.addToSubscriptions(ts)
			this.save(flush:true)
		}
		
	}
	
	/** The first implementation of the changing subscription function.
	* There are going to be many cases in the more detailed and complex models. */
	void changeSubscription(boolean flag) {
	
			this.unsubscribeCurrent()
			this.addToSubscriptions(tariffMarketService.subscribeToTariff(this.SelectTariff(flag), this, 1))
			this.save(flush:true)
		
	}
	
		
}
