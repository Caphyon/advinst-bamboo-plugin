package caphyon.advinst.bamboo;

public class AdvinstException extends Exception
{

  public AdvinstException(String message, Throwable t)
  {
    super(message, t);
  }

  public AdvinstException(String message)
  {
    super(message);
  }

  public AdvinstException(Throwable t)
  {
    super(t);
  }
}
