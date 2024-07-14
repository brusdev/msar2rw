package dev.brus.msar2rw;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

public class ActivityEntry {
   Date date;
   BigDecimal shares;
   BigDecimal price;

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
      return String.format("%s %.3f %.2f", DateFormat.getDateInstance().format(date), shares, price);
   }
}
