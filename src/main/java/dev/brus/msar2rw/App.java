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

            String type = fields[1];
            String description = fields[1];
            Date entryDate = parser.parse(fields[0]);
            BigDecimal shares = BigDecimal.ZERO;
            BigDecimal price = BigDecimal.ZERO;
            if (description.equals(ActivityEntry.ACTIVITY_OPENING_VALUE) || description.equals(ActivityEntry.ACTIVITY_YOU_BOUGHT) || description.equals(ActivityEntry.ACTIVITY_SALE)) {
               shares = new BigDecimal(fields[4]);
               price = (BigDecimal)usDecimalFormat.parse(fields[5]);
            } else if (description.startsWith(ActivityEntry.ACTIVITY_RELEASE)) {
               type = ActivityEntry.ACTIVITY_RELEASE;
               shares = new BigDecimal(fields[4]);
               price = ((BigDecimal)usDecimalFormat.parse(fields[6])).divide(shares, RoundingMode.CEILING);
            } else if (description.equals(ActivityEntry.ACTIVITY_CLOSING_VALUE)) {
               closingValue = (BigDecimal) usDecimalFormat.parse(fields[5]);
               closingDate = entryDate;
            }

            if (shares != BigDecimal.ZERO) {
               activityEntries.add(new ActivityEntry().setType(type).setDescription(description).setDate(entryDate).setShares(shares).setPrice(price));
            }

            line = reader.readLine();
         }

         System.out.println("Source activity entries:");
         activityEntries.forEach(activityEntry -> System.out.println(activityEntry));


         // Aggregate activity entries
         ActivityEntry aggregatedActivityEntry = null;
         List<ActivityEntry> aggregatedActivityEntries = new ArrayList<>();
         for (ActivityEntry activityEntry: activityEntries) {
            if (aggregatedActivityEntry == null ||
               !aggregatedActivityEntry.getDate().equals(activityEntry.getDate()) ||
               !aggregatedActivityEntry.getPrice().equals(activityEntry.getPrice()) ||
               !aggregatedActivityEntry.getType().equals(activityEntry.getType()))
            {
               aggregatedActivityEntry = new ActivityEntry().setDate(activityEntry.getDate()).
                  setPrice(activityEntry.getPrice()).setShares(BigDecimal.ZERO).setType(activityEntry.getType());
               aggregatedActivityEntries.add(aggregatedActivityEntry);
            }
            aggregatedActivityEntry.setShares(aggregatedActivityEntry.getShares().add(activityEntry.getShares()));
         }


         // Decrement aggregated activity entries
         List<ReportEntry> reportEntries = new ArrayList<>();
         for (ActivityEntry activityEntry: aggregatedActivityEntries) {
            System.out.println(activityEntry);
            if (activityEntry.getShares().compareTo(BigDecimal.ZERO) < 0) {
               reportEntries.addAll(decrement(aggregatedActivityEntries, activityEntry));
            }
         }


         // Add remaining aggregated activity entries
         for (ActivityEntry activityEntry: aggregatedActivityEntries) {
            System.out.println(activityEntry);
            if (activityEntry.getShares().compareTo(BigDecimal.ZERO) > 0) {
               calendar.setTime(activityEntry.getDate());
               calendar.set(calendar.get(Calendar.YEAR), Calendar.DECEMBER, 31);

               reportEntries.add(new ReportEntry().
                  setStartDate(activityEntry.getDate()).
                  setEndDate(closingDate).
                  setShares(activityEntry.getShares()).
                  setStartPrice(activityEntry.getPrice()).
                  setEndPrice(closingValue));
            }
         }


         // Set exchange rates of report entries
         for (ReportEntry rwReportEntry: reportEntries) {
            rwReportEntry.setStartExchangeRate(getExchangeRate(rwReportEntry.getStartDate()));
            rwReportEntry.setEndExchangeRate(getExchangeRate(rwReportEntry.getEndDate()));
         }


         // Sort report entries
         reportEntries.sort(Comparator.comparing(ReportEntry::getStartDate).thenComparing(ReportEntry::getEndDate));


         System.out.println("Report report entries:");
         reportEntries.forEach(rwReportEntry -> System.out.println(rwReportEntry));


         // Write RW report
         File rwReportFile = new File("rw.rpt");
         try (PrintWriter rwReportPrintWriter = new PrintWriter(rwReportFile)) {
            rwReportPrintWriter.println(getRWHeader());
            for (int i = 0; i < reportEntries.size(); i++) {
               rwReportPrintWriter.println(convertToRWEntry(i, reportEntries.get(i)));
            }
         }
      }
   }

   private static String getRWHeader() {
      List<String> rwEntryFields = new ArrayList<>();


      //1 - Codice titolo possesso
      rwEntryFields.add("RW - PR");

      //1 - Codice titolo possesso
      rwEntryFields.add("RW - 01");

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

      rwEntryFields.add(" Shares");

      rwEntryFields.add("Start date");

      rwEntryFields.add("  End date");

      return rwEntryFields.stream().collect(Collectors.joining("\t"));
   }

   private static String convertToRWEntry(int index, ReportEntry entry) {
      List<String> rwEntryFields = new ArrayList<>();
      BigDecimal taxRate = new BigDecimal("0.002");
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ITALY);
      numberFormat.setMinimumFractionDigits(3);


      //0 - RW index
      rwEntryFields.add((index % 5 + 1) + "/" + (index / 5 + 1));

      //1 - Codice titolo possesso
      rwEntryFields.add("1"); //1 - proprietà

      //3 - Codice individuaz. bene
      rwEntryFields.add("20"); //20 - CONTO DEPOSITO TITOLI ALL’ESTERO

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
      int days = (int)Math.ceil((entry.getEndDate().getTime() - entry.getStartDate().getTime()) / 86400000d) + 1;
      rwEntryFields.add(String.valueOf(days));

      //14 - Codice
      rwEntryFields.add("3"); //3 - Compilazione quadro RT

      //29 - IVAFE
      BigDecimal taxValue = endValue.multiply(taxRate).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(365), RoundingMode.CEILING).setScale(0, RoundingMode.HALF_UP);
      rwEntryFields.add(taxValue.toString());

      rwEntryFields.add(numberFormat.format(entry.getShares()));
      
      rwEntryFields.add(dateFormat.format(entry.getStartDate()));

      rwEntryFields.add(dateFormat.format(entry.getEndDate()));

      return rwEntryFields.stream().map(s -> String.format("%7s", s)).collect(Collectors.joining("\t"));
   }

   private static List<ReportEntry> decrement(List<ActivityEntry> activityEntries, ActivityEntry negativeActivityEntry) {
      List<ReportEntry> decrementingReportEntries = new ArrayList<>();
      BigDecimal decrementingShares = negativeActivityEntry.getShares().abs();

      for (int i = activityEntries.size() - 1; i >= 0; i--) {
         ActivityEntry activityEntry = activityEntries.get(i);

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
