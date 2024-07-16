package dev.brus.msar2rw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Hello world!
 */
public class App {

   public static void main(String[] args) throws Exception {
      Date closingDate = null;
      BigDecimal closingValue = BigDecimal.ZERO;
      List<ActivityEntry> activityEntries = new ArrayList<>();

      GregorianCalendar calendar = new GregorianCalendar();

      try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
         String line = reader.readLine();
         while (line != null) {
            System.out.println(line);
            String[] fields = line.split("\t");

            SimpleDateFormat parser = new SimpleDateFormat("dd-MMM-yyyy");
            DecimalFormat usDecimalFormat = (DecimalFormat)NumberFormat.getCurrencyInstance(Locale.US);
            usDecimalFormat.setParseBigDecimal(true);

            Date entryDate = parser.parse(fields[0]);
            BigDecimal shares = BigDecimal.ZERO;
            BigDecimal price = BigDecimal.ZERO;
            if (fields[1].equals(ActivityEntry.ACTIVITY_OPENING_VALUE) || fields[1].equals(ActivityEntry.ACTIVITY_YOU_BOUGHT) || fields[1].equals(ActivityEntry.ACTIVITY_SALE)) {
               shares = new BigDecimal(fields[4]);
               price = (BigDecimal)usDecimalFormat.parse(fields[5]);
            } else if (fields[1].startsWith(ActivityEntry.ACTIVITY_RELEASE)) {
               shares = new BigDecimal(fields[4]);
               price = ((BigDecimal)usDecimalFormat.parse(fields[6])).divide(shares, RoundingMode.CEILING);
            } else if (fields[1].equals(ActivityEntry.ACTIVITY_CLOSING_VALUE)) {
               closingValue = (BigDecimal) usDecimalFormat.parse(fields[5]);
               closingDate = entryDate;
            }

            if (shares != BigDecimal.ZERO) {
               activityEntries.add(new ActivityEntry().setType(fields[1]).setDate(entryDate).setShares(shares).setPrice(price));
            }

            line = reader.readLine();
         }

         System.out.println("Process activity entries:");

         List<ReportEntry> rwReportEntries = new ArrayList<>();
         for (ActivityEntry activityEntry: activityEntries) {
            System.out.println(activityEntry);
            if (activityEntry.getShares().compareTo(BigDecimal.ZERO) < 0) {
               rwReportEntries.addAll(decrement(activityEntries, activityEntry));
            }
         }

         for (ActivityEntry activityEntry: activityEntries) {
            System.out.println(activityEntry);
            if (activityEntry.getShares().compareTo(BigDecimal.ZERO) > 0) {
               calendar.setTime(activityEntry.getDate());
               calendar.set(calendar.get(Calendar.YEAR), Calendar.DECEMBER, 31);

               rwReportEntries.add(new ReportEntry().
                  setStartDate(activityEntry.getDate()).
                  setEndDate(closingDate).
                  setShares(activityEntry.getShares()).
                  setStartPrice(activityEntry.getPrice()).
                  setEndPrice(closingValue));
            }
         }

         rwReportEntries.sort(Comparator.comparing(ReportEntry::getStartDate).thenComparing(ReportEntry::getEndDate));

         System.out.println("Print report entries:");

         for (ReportEntry rwReportEntry: rwReportEntries) {
            rwReportEntry.setStartExchangeRate(getExchangeRate(rwReportEntry.getStartDate()));
            rwReportEntry.setEndExchangeRate(getExchangeRate(rwReportEntry.getEndDate()));
            System.out.println(rwReportEntry);
         }


         File rwReportFile = new File("rw.rpt");
         try (PrintWriter rwReportPrintWriter = new PrintWriter(rwReportFile)) {
            rwReportPrintWriter.println(getRWHeader());
            for (int i = 0; i < rwReportEntries.size(); i++) {
               rwReportPrintWriter.println(convertToRWEntry(i, rwReportEntries.get(i)));
            }
         }

         for (ReportEntry rwReportEntry: rwReportEntries) {
            rwReportEntry.setStartExchangeRate(getExchangeRate(rwReportEntry.getStartDate()));
            rwReportEntry.setEndExchangeRate(getExchangeRate(rwReportEntry.getEndDate()));
            System.out.println(rwReportEntry);
         }

      }
   }

   private static String getRWHeader() {
      List<String> rwEntryFields = new ArrayList<>();


      //1 - Codice titolo possesso
      rwEntryFields.add("RW - PR");

      //1 - Codice titolo possesso
      rwEntryFields.add("RW - 01");

      //2 - Tipo contribuente
      rwEntryFields.add("RW - 02");

      //3 - Codice individuaz. bene
      rwEntryFields.add("RW - 03");

      //4 - Codice Stato estero
      rwEntryFields.add("RW - 04");

      //5 - Quota di possesso
      rwEntryFields.add("RW - 05");

      //6 - Criterio determin. valore
      rwEntryFields.add("RW - 06");

      //7 - Valore iniziale
      rwEntryFields.add("RW - 07");

      //8 - Valore finale
      rwEntryFields.add("RW - 08");

      //10 - Giorni IVAFE- IC
      rwEntryFields.add("RW - 10");

      //14 - Codice
      rwEntryFields.add("RW - 14");

      //29 - IVAFE
      rwEntryFields.add("RW - 29");

      rwEntryFields.add("Azioni");

      rwEntryFields.add("Data inizio");

      rwEntryFields.add("Data fine");

      return rwEntryFields.stream().collect(Collectors.joining("\t"));
   }

   private static String convertToRWEntry(int index, ReportEntry entry) {
      List<String> rwEntryFields = new ArrayList<>();
      BigDecimal taxRate = new BigDecimal("0.002");


      //0 - RW index
      rwEntryFields.add((index % 5 + 1) + "/" + (index / 5 + 1));

      //1 - Codice titolo possesso
      rwEntryFields.add("1"); //1 - proprietà

      //2 - Tipo contribuente
      rwEntryFields.add("2"); //2 - titolare effettivo

      //3 - Codice individuaz. bene
      rwEntryFields.add("2"); //2 - PARTECIPAZIONI AL CAPITALE O AL PATRIMONIO DI SOCIETÀ NON RESIDENTI

      //4 - Codice Stato estero
      rwEntryFields.add("069"); //069 - STATI UNITI D’AMERICA

      //5 - Quota di possesso
      rwEntryFields.add("100");

      //6 - Criterio determin. valore
      rwEntryFields.add("1"); //1 - valore di mercato

      //7 - Valore iniziale
      BigDecimal startValue = entry.getShares().multiply(entry.getStartPrice()).divide(entry.getStartExchangeRate(), RoundingMode.CEILING).setScale(0, RoundingMode.HALF_UP);
      rwEntryFields.add(startValue.toString());

      //8 - Valore finale
      BigDecimal endValue = entry.getShares().multiply(entry.getEndPrice()).divide(entry.getEndExchangeRate(), RoundingMode.CEILING).setScale(0, RoundingMode.HALF_UP);
      rwEntryFields.add(endValue.toString());

      //10 - Giorni IVAFE- IC
      int days = (int)Math.ceil((entry.getEndDate().getTime() - entry.getStartDate().getTime()) / 86400000d);
      rwEntryFields.add(String.valueOf(days));

      //14 - Codice
      rwEntryFields.add("3"); //3 - Compilazione quadro RT

      //29 - IVAFE
      BigDecimal taxValue = endValue.multiply(taxRate).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(365), RoundingMode.CEILING).setScale(0, RoundingMode.HALF_UP);
      rwEntryFields.add(taxValue.toString());

      rwEntryFields.add(entry.getShares().toString());
      
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

      rwEntryFields.add(dateFormat.format(entry.getStartDate()));

      rwEntryFields.add(dateFormat.format(entry.getEndDate()));

      return rwEntryFields.stream().map(s -> String.format("%7s", s)).collect(Collectors.joining("\t"));
   }

   private static List<ReportEntry> decrement(List<ActivityEntry> activityEntries, ActivityEntry negativeActivityEntry) {
      List<ReportEntry> decrementingReportEntries = new ArrayList<>();
      BigDecimal decrementingShares = negativeActivityEntry.getShares().abs();

      for (ActivityEntry activityEntry: activityEntries) {
         if (activityEntry.getShares().compareTo(BigDecimal.ZERO) > 0) {
            if (activityEntry.getShares().compareTo(decrementingShares) >= 0) {
               decrementingReportEntries.add(new ReportEntry().setStartDate(activityEntry.getDate()).
                  setEndDate(negativeActivityEntry.getDate()).
                  setShares(decrementingShares).
                  setStartPrice(activityEntry.getPrice()).
                  setEndPrice(negativeActivityEntry.getPrice()));

               activityEntry.setShares(activityEntry.getShares().subtract(decrementingShares));
               negativeActivityEntry.setShares(BigDecimal.ZERO);
               decrementingShares = BigDecimal.ZERO;
            } else {
               decrementingReportEntries.add(new ReportEntry().setStartDate(activityEntry.getDate()).
                  setEndDate(negativeActivityEntry.getDate()).
                  setShares(activityEntry.getShares()).
                  setStartPrice(activityEntry.getPrice()).
                  setEndPrice(negativeActivityEntry.getPrice()));

               decrementingShares = decrementingShares.subtract(activityEntry.getShares());
               negativeActivityEntry.setShares(negativeActivityEntry.getShares().add(activityEntry.getShares()));
               activityEntry.setShares(BigDecimal.ZERO);
            }

            if (decrementingShares.compareTo(BigDecimal.ZERO) == 0) {
               break;
            }
         }
      }

      return decrementingReportEntries;
   }

   private static BigDecimal getExchangeRate(Date date) throws Exception {
      BigDecimal exchangeRate = null;
      GregorianCalendar calendar = new GregorianCalendar();
      SimpleDateFormat referenceDateFormat = new SimpleDateFormat("yyyy-MM-dd");

      while (exchangeRate == null) {
         URL upstreamJIRA = new URL("https://tassidicambio.bancaditalia.it/terzevalute-wf-web/rest/v1.0/dailyRates?baseCurrencyIsoCode=USD&currencyIsoCode=EUR&referenceDate=" + referenceDateFormat.format(date));
         HttpURLConnection connection = (HttpURLConnection)upstreamJIRA.openConnection();
         connection.setRequestProperty("Accept", "application/json");

         try {
            try (InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream())) {
               JsonObject jsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();

               JsonArray rates = jsonObject.getAsJsonArray("rates");

               if (rates.size() > 0) {
                  exchangeRate =new BigDecimal(rates.get(0).getAsJsonObject().
                     getAsJsonPrimitive("avgRate").getAsString());
               } else {
                  calendar.setTime(date);
                  calendar.add(Calendar.DATE, -1);
                  date = calendar.getTime();
               }
            }
         } finally {
            connection.disconnect();
         }
      }

      return exchangeRate;
   }

}
