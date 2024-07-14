package dev.brus.msar2rw;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

public class ReportEntry {
   Date startDate;
   Date endDate;
   BigDecimal shares;
   BigDecimal startPrice;
   BigDecimal endPrice;
   BigDecimal startExchangeRate;
   BigDecimal endExchangeRate;

   public Date getStartDate() {
      return startDate;
   }

   public ReportEntry setStartDate(Date startDate) {
      this.startDate = startDate;
      return this;
   }

   public Date getEndDate() {
      return endDate;
   }

   public ReportEntry setEndDate(Date endDate) {
      this.endDate = endDate;
      return this;
   }

   public BigDecimal getShares() {
      return shares;
   }

   public ReportEntry setShares(BigDecimal shares) {
      this.shares = shares;
      return this;
   }

   public BigDecimal getStartPrice() {
      return startPrice;
   }

   public ReportEntry setStartPrice(BigDecimal startPrice) {
      this.startPrice = startPrice;
      return this;
   }

   public BigDecimal getEndPrice() {
      return endPrice;
   }

   public ReportEntry setEndPrice(BigDecimal endPrice) {
      this.endPrice = endPrice;
      return this;
   }

   public BigDecimal getStartExchangeRate() {
      return startExchangeRate;
   }

   public ReportEntry setStartExchangeRate(BigDecimal startExchangeRate) {
      this.startExchangeRate = startExchangeRate;
      return this;
   }

   public BigDecimal getEndExchangeRate() {
      return endExchangeRate;
   }

   public ReportEntry setEndExchangeRate(BigDecimal endExchangeRate) {
      this.endExchangeRate = endExchangeRate;
      return this;
   }

   @Override
   public String toString() {
      return String.format("%s %s %.3f %.2f %.2f %.4f %.4f", DateFormat.getDateInstance().format(startDate),
         DateFormat.getDateInstance().format(endDate), shares, startPrice, endPrice, startExchangeRate, endExchangeRate);
   }
}
