package org.powertac.common.exceptions;

/**
 * Thrown if a Cash Update fails
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class CashUpdateException extends PowerTacException {
  public CashUpdateException() {
  }

  public CashUpdateException(String s) {
    super(s);
  }

  public CashUpdateException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public CashUpdateException(Throwable throwable) {
    super(throwable);
  }
}
