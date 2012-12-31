
package no.priv.garshol.duke.cleaners;

import java.util.Map;
import java.util.HashMap;
import no.priv.garshol.duke.Cleaner;

/**
 * <p>Cleaner for international phone numbers. It assumes that it can
 * get the same phone number in forms like:
 *
 * <ul>
 *   <li>+47 55301400
 *   <li>47-55-301400
 * </ul>
 */
// +44 020 77921414
// +44 20 78376470 
// 00 46 8 506 16100
// 00468343801
// +47 55301400
// 23 21 20 00
// 47-55-301400
// 00 49 30 881 3001
public class PhoneNumberCleaner implements Cleaner {
  private Cleaner sub;
  private static Map<String,CountryCode> ccodes = initcodes();

  public PhoneNumberCleaner() {
    this.sub = new DigitsOnlyCleaner();
  }
  
  public String clean(String value) {
    String orig = value;
    
    // check if there's a + before the first digit
    boolean initialplus = findPlus(value);
    
    // remove everything but digits
    value = sub.clean(value);
    if (value == null)
      return null;

    // check for initial '00'
    boolean zerozero = !initialplus && value.startsWith("00");
    if (zerozero)
      value = value.substring(2); // strip off the zeros

    // look for country code
    CountryCode ccode = findCountryCode(value);
    if (ccode == null) {
      // no country code, let's do what little we can
      if (initialplus || zerozero)
        return orig; // this number is messed up. dare not touch
      return value;

    } else {
      value = value.substring(ccode.getPrefix().length()); // strip off ccode
      if (ccode.getStripZero() && value.startsWith("0"))
        value = value.substring(1); // strip the zero

      if (ccode.isRightFormat(value))
        return "+" + ccode.getPrefix() + " " + value;
      else
        return orig; // don't dare touch this
    }
  }

  private CountryCode findCountryCode(String value) {
    for (int ix = 1; ix < Math.min(4, value.length()); ix++) {
      String code = value.substring(0, ix);
      CountryCode ccode = ccodes.get(code);
      if (ccode != null)
        return ccode;
    }
    return null;
  }
  
  private boolean findPlus(String value) {
    for (int ix = 0; ix < value.length(); ix++) {
      char ch = value.charAt(ix);
      if (ch == '+')
        return true;
      else if (ch >= '0' && ch <= '9')
        return false;
    }
    return false;
  }

  // algorithm: 
  // first look for + or 00. if they're there, then find country code.
  // look for zero after country code, and remove if present

  static class CountryCode {
    private String prefix;
    private boolean strip_zero; // strip initial zero after country code
    private int length; // length of phone numbers

    public CountryCode(String prefix) {
      this(prefix, false);
    }

    public CountryCode(String prefix, int length) {
      this(prefix, false, length);
    }
    
    public CountryCode(String prefix, boolean strip_zero) {
      this(prefix, strip_zero, 0);
    }
    
    public CountryCode(String prefix, boolean strip_zero, int length) {
      this.prefix = prefix;
      this.strip_zero = strip_zero;
      this.length = length;
    }

    public String getPrefix() {
      return prefix;
    }

    public boolean getStripZero() {
      return strip_zero;
    }

    // returns true iff the phone number is in a correct format for
    // this country.
    public boolean isRightFormat(String value) {
      if (length == 0)
        return true; // anything can be right as far as we know
      return value.length() == length;
    }
  }

  private static Map<String, CountryCode> initcodes() {
    Map<String, CountryCode> ccs = new HashMap();

    ccs.put("1",   new CountryCode("1", false, 10)); // US
    ccs.put("31",  new CountryCode("31"));           // Netherlands
    ccs.put("33",  new CountryCode("33"));           // France
    ccs.put("358", new CountryCode("358"));          // Finland
    ccs.put("44",  new CountryCode("44", true));     // UK
    ccs.put("45",  new CountryCode("45", 8));        // Denmark
    ccs.put("46",  new CountryCode("46", true, 9));  // Sweden
    ccs.put("47",  new CountryCode("47", 8));        // Norway
    ccs.put("49",  new CountryCode("49"));           // Germany
    
    return ccs;
  }
}