import appliances.*
import consumers.*
import persons.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant;
import org.powertac.common.*
import org.powertac.tariffmarket.TariffMarketService
import org.powertac.common.Broker
import org.powertac.common.HourlyCharge
import org.powertac.common.enumerations.PowerType
import org.powertac.common.enumerations.CustomerType
import org.powertac.common.enumerations.TariffTransactionType
import org.powertac.common.Rate
import org.powertac.common.Tariff
import org.powertac.common.Timeslot
import org.powertac.common.interfaces.CompetitionControl
import org.powertac.common.interfaces.NewTariffListener
import org.powertac.common.TariffTransaction
import org.powertac.common.TariffSpecification
import org.powertac.common.msg.TariffExpire
import org.powertac.common.msg.TariffRevoke
import org.powertac.common.msg.TariffStatus
import org.powertac.common.msg.VariableRateUpdate
import org.powertac.common.TimeService
import org.powertac.tariffmarket.TariffMarketService

class BootStrap {

	def timeService
	def tariffMarketService
	
	def init = { servletContext -> 
	
		def registrationThing = null
		def registrationPhase = -1
		
		def competitionControlService =
		[registerTimeslotPhase: { thing, phase ->
		  registrationThing = thing
		  registrationPhase = phase
		}] as CompetitionControl
		
		tariffMarketService.competitionControlService = competitionControlService
		tariffMarketService.afterPropertiesSet()
		
		tariffMarketService.publicationInterval = 3 // hours
		
		TariffSpecification tariffSpec // instance var
	  
		Scanner sc = new Scanner(System.in);
		def conf = new persons.Config();
		conf.readConf();
		int vil = 2
					
		def env = new Environment()
		env.initialize(conf.hm ,vil)
		env.save(flush:true)
		
		Instant start
		Instant exp
		def ac
		def broker1
		def broker2
		def customer
		int idCount = 0
		def txs = []
	
		TariffSpecification.list()*.delete()
		Tariff.list()*.delete()
    	tariffMarketService.newTariffs = []
		start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
		timeService.start = start.millis
		timeService.setCurrentTime(start)
		
		broker1 = new Broker(username: 'Anna')
		broker1.save(flush:true)
		
		broker2 = new Broker(username: 'Bob')
		broker2.save(flush:true)
		
       tariffMarketService.tariffPublicationFee = 42.0
	   tariffMarketService.tariffRevocationFee = 420.0
	   exp = new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
	   tariffSpec = new TariffSpecification(brokerId: broker1.id, expiration: exp,
											   minDuration: TimeService.WEEK * 8,powerType: PowerType.CONSUMPTION)
		Rate r1 = new Rate(value: 0.121)
		tariffSpec.addToRates(r1)
	
		tariffMarketService.setDefaultTariff(tariffSpec)
		ac = new AbstractCustomer(name: 'Anty', customerType: CustomerType.CustomerHousehold, powerType: PowerType.CONSUMPTION).save(flush:true)
		ac.init()
		
		env.villages.each{
			
			it.init()
			 
		}
		
		
	  def tsc1 = new TariffSpecification(brokerId: broker2.id,
			expiration: new Instant(start.millis + TimeService.DAY),
			minDuration: TimeService.WEEK * 8, powerType: PowerType.CONSUMPTION)
	  def tsc2 = new TariffSpecification(brokerId: broker2.id,
			expiration: new Instant(start.millis + TimeService.DAY * 2),
			minDuration: TimeService.WEEK * 8, powerType: PowerType.CONSUMPTION)
	  def tsc3 = new TariffSpecification(brokerId: broker2.id,
			expiration: new Instant(start.millis + TimeService.DAY * 3),
			minDuration: TimeService.WEEK * 8, powerType: PowerType.CONSUMPTION)
	  Rate r2 = new Rate(value: 0.222)
	  tsc1.addToRates(r2)
	  tsc2.addToRates(r2)
	  tsc3.addToRates(r2)
	  tariffMarketService.processTariff(tsc1)
	  tariffMarketService.processTariff(tsc2)
	  tariffMarketService.processTariff(tsc3)
		
     // println(ac.SelectTariff(PowerType.CONSUMPTION))

	  for (int i = 0;i < 10;i++){
	  
		  timeService.setCurrentTime(new Instant(start.millis + (TimeService.HOUR * i)))
		  
		  println(timeService.getCurrentTime())
		  		  
		  ac.consumePower()
		  
		  env.villages.each{
			  
			  it.consumePower()
			   
		  }
	  
	  }
	  
	  println(tariffMarketService.getActiveTariffList(ac.powerType).toString())
	  
	  ac.changeSubscription(true)
	  
	  env.villages.each{
		  
		  it.changeSubscription(true)
		   
	  }
	  
	  timeService.setCurrentTime(new Instant(start.millis + TimeService.HOUR ))
		  
	  for (int i = 0;i < 10;i++){
		  
			  timeService.setCurrentTime(new Instant(start.millis + (TimeService.HOUR * i)))
			  
			  println(timeService.getCurrentTime())
						
			  ac.consumePower()
			  
			  env.villages.each{
				  
				  it.consumePower()
				   
			  }
		  
	  }
	  
	  Tariff tariff1 = Tariff.findByBrokerAndTariffSpec(broker2,tsc1)
	  Tariff tariff2 = Tariff.findByBrokerAndTariffSpec(broker2,tsc2)
	  Tariff tariff3 = Tariff.findByBrokerAndTariffSpec(broker2,tsc3)
	  
	  TariffRevoke tex1 = new TariffRevoke(tariffId: tariff1.id, brokerId: tariff1.brokerId)
	  TariffRevoke tex2 = new TariffRevoke(tariffId: tariff2.id, brokerId: tariff2.brokerId)
	  TariffRevoke tex3 = new TariffRevoke(tariffId: tariff3.id, brokerId: tariff3.brokerId)
	  
	  def status1 = tariffMarketService.processTariff(tex1)
	  def status2 = tariffMarketService.processTariff(tex2)
	  def status3 = tariffMarketService.processTariff(tex3)
	  
	  println(tariffMarketService.getActiveTariffList(ac.powerType).toString())
	  
	  ac.checkRevokedSubscriptions()
	  
	  env.villages.each{
		  
		  it.checkRevokedSubscriptions()
		   
	  }
	  	  
	 
	  println("Registrations: " + tariffMarketService.registrations.size())
	  println("Published Tariffs: " + ac.getPublishedTariffs().size())
	  
	  def newtsc1 = new TariffSpecification(brokerId: broker1.id,
		  expiration: new Instant(start.millis + TimeService.DAY),
		  minDuration: TimeService.WEEK * 8, powerType: PowerType.CONSUMPTION)
	  newtsc1.addToRates(r1)
	  tariffMarketService.processTariff(newtsc1)
	  timeService.currentTime += TimeService.HOUR
	  // it's 13:00
	  println("Time: " + timeService.getCurrentTime())
	  println("Published Tariffs: " + ac.getPublishedTariffs().size())
	  
	  tariffMarketService.activate(timeService.currentTime, 2)
	  
	  def newtsc2 = new TariffSpecification(brokerId: broker1.id,
		  expiration: new Instant(start.millis + TimeService.DAY * 2),
		  minDuration: TimeService.WEEK * 8, powerType: PowerType.CONSUMPTION)
	  newtsc2.addToRates(r1)
	  tariffMarketService.processTariff(newtsc2)
	  def newtsc3 = new TariffSpecification(brokerId: broker1.id,
		  expiration: new Instant(start.millis + TimeService.DAY * 3),
		  minDuration: TimeService.WEEK * 8, powerType: PowerType.CONSUMPTION)
	  newtsc3.addToRates(r1)
	  tariffMarketService.processTariff(newtsc3)
	  timeService.currentTime += TimeService.HOUR
	  // it's 14:00
	  tariffMarketService.activate(timeService.currentTime, 2)
	  
	  println("Time: " + timeService.getCurrentTime())
	  println("Published Tariffs: " + ac.getPublishedTariffs().size())
	  
	  
	  def tsp1 = new TariffSpecification(brokerId: broker2.id,
		  expiration: new Instant(start.millis + TimeService.DAY),
		  minDuration: TimeService.WEEK * 8, powerType: PowerType.PRODUCTION)
	  def tsp2 = new TariffSpecification(brokerId: broker2.id,
		  expiration: new Instant(start.millis + TimeService.DAY * 2),
		  minDuration: TimeService.WEEK * 8, powerType: PowerType.PRODUCTION)
	  tsp1.addToRates(r2)
	  tsp2.addToRates(r2)
	  tariffMarketService.processTariff(tsp1)
	  tariffMarketService.processTariff(tsp2)
	  
	  timeService.currentTime += TimeService.HOUR
	  tariffMarketService.activate(timeService.currentTime, 2)
	  
	  println("Time: " + timeService.getCurrentTime())
	  println("Published Tariffs: " + ac.getPublishedTariffs().size())
	  println("Published Tariffs: " + ac.getPublishedTariffs().toString())
	  
	  ac.changeSubscription(true)
	  
	  timeService.currentTime += TimeService.HOUR
	  tariffMarketService.activate(timeService.currentTime, 2)
	  
	  println("Time: " + timeService.getCurrentTime())
	  println("Published Tariffs: " + ac.getPublishedTariffs().size())
	  
	  	  
/*
	def comp = new Competition(name: "Anty", description: " My Competition ",simulationBaseTime: new DateTime(2008, 6, 21, 1, 0, 0, 0, DateTimeZone.UTC));
		
	comp.save()
			
	Scanner sc = new Scanner(System.in);
	def conf = new persons.Config();
	conf.readConf();
	int vil = 2
				
	def env = new Environment()
	env.initialize(conf.hm ,vil , comp)
	env.save()
		
		
	DateTime theBase = new DateTime(2008, 6, 21, 1, 0, 0, 0, DateTimeZone.UTC)
	DateTime theStart = new DateTime(DateTimeZone.UTC)
	int theRate = 180
	int theMod = 15*60*1000
	def interval = 15 * 60 * 1000
	TimeService ts = new TimeService(base: theBase.millis,
					 start: theStart.millis,
					 rate: theRate,
					 modulo: theMod)
	
	ts.updateTime()
	int offset = 0;
	def add
	add = { ts.addAction(ts.currentTime.plus(interval), { env.step(offset); add()}) }
	add()
	
	Thread.sleep(5000) // 5 seconds
	
	while(offset < 1000){
		
		ts.updateTime()
		offset = (ts.currentTime.millis - theBase.millis)/900000
		println(ts.currentTime.toString() + " " + offset)
		Thread.sleep(5000) // 5 seconds

	}
	
/*
    def init = { servletContext ->

	def comp = new Competition(name: "Anty", description: " My Competition ",simulationBaseTime: new DateTime(2008, 6, 21, 1, 0, 0, 0, DateTimeZone.UTC));	
	
	comp.save()
	
	def ts
	
	def startTime = comp.simulationBaseTime
	def interval = 60 * 60 * 1000
	Long serial = 0;
	
	for (int i = 0; i < comp.timeslotsOverall;i++){
		
		serial = ((startTime.millis - comp.simulationBaseTime.millis)/3600000) + 1
		
		ts = new Timeslot(serialNumber: serial, startDateTime: startTime, endDateTime:startTime.plus(interval), enabled: true, current: false)
		
		comp.addToTimeslots(ts)
		
		startTime = startTime.plus(interval)
		
	}
	
	def broker = new Broker(userName: "Babis")
	
	comp.addToBrokers(broker)
	
	def tsp = new TariffSpecification(brokerId: broker.id, minDuration: 24*60*60*1000l)
	
	tsp.save()

	def tariff = new Tariff(tariffSpec: tsp, offerDate: startTime)
	
	broker.addToTariffs(tariff)
	comp.addToTariffs(tariff)
		
	def tarsub = new TariffSubscription()
	
	tariff.addToSubscriptions(tarsub)

	Scanner sc = new Scanner(System.in);
	def conf = new persons.Config();
	conf.readConf();
	int vil = 2
		
	def env = new Environment()
	env.initialize(conf.hm ,vil , comp)
	env.save()
		
		
	comp.customers.each{
		
		tarsub.addToCustomers(it)
		
	}


	comp.timeslots.each{
		
	def temp = it
		
		comp.customers.each{
			
			it.createTransaction(temp)
			
		}
	}

			
/*		
		DateTime theBase = new DateTime(2008, 6, 21, 1, 0, 0, 0, DateTimeZone.UTC)
		DateTime theStart = new DateTime(DateTimeZone.UTC)
		int theRate = 180
		int theMod = 15*60*1000
		def interval = 15 * 60 * 1000
		TimeService ts = new TimeService(base: theBase.millis,
                         start: theStart.millis,
                         rate: theRate,
                         modulo: theMod)
		ts.updateTime()
		int offset = 0;
		def add
		add = { ts.addAction(ts.currentTime.plus(interval), { env.step(offset); add()}) }
		add()
		
		Thread.sleep(5000) // 5 seconds
		
		while(offset < 100){
			
			ts.updateTime()
			offset = (ts.currentTime.millis - theBase.millis)/900000
			println(ts.currentTime.toString() + " " + offset)
			Thread.sleep(5000) // 5 seconds
		}*/
	}
	
    def destroy = {
    }
}
