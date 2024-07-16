package dev.brus.msar2rw;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

public class ActivityEntry {

   public static final String ACTIVITY_OPENING_VALUE = "Opening Value";
   public static final String ACTIVITY_YOU_BOUGHT = "You bought";
   public static final String ACTIVITY_RELEASE = "Release";
   public static final String ACTIVITY_SALE = "Sale";
   public static final String ACTIVITY_CLOSING_VALUE = "Closing Value";

   String type;
   String description;
   Date date;
   BigDecimal shares;
   BigDecimal price;

   public String getType() {
      return type;
   }

   public ActivityEntry setType(String type) {
      this.type = type;
      return this;
   }

   public String getDescription() {
      return description;
   }

   public ActivityEntry setDescription(String description) {
      this.description = description;
      return this;
   }

   public Date getDate() {
      return date;
   }

   public ActivityEntry setDate(Date date) {
      this.date = date;
      return this;
   }

   public BigDecimal getShares() {
      return shares;
   }

   public ActivityEntry setShares(BigDecimal shares) {
      this.shares = shares;
      return this;
   }

   public BigDecimal getPrice() {
      return price;
   }

   public ActivityEntry setPrice(BigDecimal price) {
      this.price = price;
      return this;
   }

   @Override
   public String toString() {
      return String.format("%s %s %.3f %.2f", DateFormat.getDateInstance().format(date), description, shares, price);
   }
}
