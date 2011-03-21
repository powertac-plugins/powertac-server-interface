package org.powertac.common

import grails.test.GrailsUnitTestCase
import org.powertac.common.enumerations.CustomerType
import org.powertac.common.enumerations.PowerType

class TariffSubscriptionTests extends GrailsUnitTestCase
{
  TariffSubscription subscription
  
  protected void setUp() 
  {
    super.setUp()
    subscription = new TariffSubscription()
    mockForConstraintsTests(TariffSubscription, [subscription])
  }

  protected void tearDown() 
  {
    super.tearDown()
  }

  void testNullableValidationLogic() 
  {
    TariffSubscription tariffSubscription = new TariffSubscription()
    tariffSubscription.id = null
    assertFalse(tariffSubscription.validate())
    assertEquals('nullable', tariffSubscription.errors.getFieldError('id').getCode())
    assertEquals('nullable', tariffSubscription.errors.getFieldError('customer').getCode())
    assertEquals('nullable', tariffSubscription.errors.getFieldError('tariff').getCode())
  }
}