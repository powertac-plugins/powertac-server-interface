package org.powertac.common


import java.util.List;
import org.powertac.common.interfaces.NewTariffListener

class AbstractCustomer {

	def timeService
	def tariffMarketService
	
	/** The id of the Abstract Customer */
	String id
  
	/** The Customer specifications*/
	CustomerInfo customerInfo
	
	/** The list of the published tariffs, pusblished and refreshed within a certain period */
	List<Tariff> publishedTariffs = []
  
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
		publishedTariffs(nullable:true)
		upperPowerCap (nullable: false, scale: Constants.DECIMALS)
		lowerPowerCap (nullable: false, scale: Constants.DECIMALS)
		subscriptions (nullable:true)
		//powerType(nullable: true)
		//multiContracting (nullable: false)
		//canNegotiate (nullable: false)
    }
	
	static mapping = {
		id (generator: 'assigned')
	  }
	
	static auditable = true
	
	public String toString() {
		return id + customerInfo.getName()
	}
	
	/** The initialization actions. We can add more
	 * Subscribe to the default tariff for the beginning of the game */
	void init(){
		
		this.id = customerInfo.getId()

		def listener = [publishNewTariffs:{tariffList -> publishedTariffs = tariffList }] as NewTariffListener
		tariffMarketService.registerNewTariffListener(listener)
		this.save()
	
	}

	/** Function utilized at the beginning in order to subscribe to the default tariff */
	void subscribeDefault(){
		
		this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(customerInfo.powerType), this, 1))
		
		this.save()
		
	}
	
	/** Subscribing certain subscription */
	void subscribe(Tariff tariff, int customercount){
		
		this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariff, this, customercount))
		tariff.save()
		this.save()
		
	}
	
	/** Unsubscribing certain subscription */
	void unsubscribe(Tariff tariff, int customerCount){
		
		TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)
		
		ts.unsubscribe(customerCount)
		this.removeFromSubscriptions(ts)
		tariff.save()
		ts.save()
		this.save()
	}
	
	/** Subscribing certain subscription */
	void subscribe(TariffSubscription ts){
		
		ts.subscribe(ts.customersCommitted)
		this.addToSubscriptions(ts)
		ts.save()
		this.save()
		
	}
	
	/** Unsubscribing certain subscription */
	void unsubscribe(TariffSubscription ts){
		
		ts.unsubscribe(ts.customersCommitted)
		this.removeFromSubscriptions(ts)
		ts.save()
		this.save()
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
	

	/** The first implementation of the changing subscription function.
	* There are going to be many cases in the more detailed and complex models. */
	void changeSubscription(Tariff tariff, boolean flag) {
	
			TariffSubscription ts = TariffSubscription.findByTariffAndCustomer(tariff, this)
		
			this.unsubscribe(tariff, ts.customersCommitted)
			this.addToSubscriptions(tariffMarketService.subscribeToTariff(this.SelectTariff(flag), this, 1))
			this.save()
		
	}
	
		
	/** The first implementation of the tariff selection function.
	* This is a random chooser of the available tariffs, totally insensitive*/
   Tariff SelectTariff(boolean flag){
	   
	   List<Tariff> available
	   int ran, index
	   if (flag){
		   
		   available = tariffMarketService.getActiveTariffList(customerInfo.powerType)
		   println(available.toString())
		   index = available.indexOf(tariffMarketService.getDefaultTariff(customerInfo.powerType))
		   println("Index of Default Tariff:" + index)
	   }
	   else {
		
		   available = publishedTariffs
		   index = available.indexOf(this.subscriptions?.tariff)
		   println("Index of Current Tariff:" + index)
	   }
	   
	   
	   ran = index
	   
	   while ( ran == index){
		   ran = available.size() * Math.random()
		   println(ran);
	   }
	   
	   return available.get(ran)
	   
   }
	
   
   /** The first implementation of the checking for revoked subscriptions function.*/
   void checkRevokedSubscriptions(){
	   
	   List<TariffSubscription> revoked = tariffMarketService.getRevokedSubscriptionList(this)
	   println(revoked.toString())
	   
	   println("Revoked Size: " + revoked.size())
	   
	   revoked.each {
		   
		   TariffSubscription ts = it.handleRevokedTariff()
		   this.unsubscribe(it)
	   
		   println(ts.toString())
		   this.subscribe(ts)
		   ts.save()
		   this.save()
	   }
	   
   }
   
	
	
}
