/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package okcoin_balancedvolume_bot;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Marco
 */
public class Trader {

    private String symbol = "btc_cny";
    private String currencyCrypto = "btc";
    private String currencyFiat = "cny";
    private boolean demoMode;
    private double[] sequence;
    private int position;

    private long start;
    private long end;
    final int timeVolume = 1;

    double volumeTotal = 0;
    final double volumeTimeRatio = 20;

    int arrayLength = 600;
    long[] tidNArray = new long[arrayLength];
    double[] amountNArray = new double[arrayLength];
    double[] priceNArray = new double[arrayLength];
    String[] typeArray = new String[arrayLength];

    long[] tidNArrayStored = new long[arrayLength];
    double[] amountNArrayStored = new double[arrayLength];
    double[] priceNArrayStored = new double[arrayLength];
    String[] typeArrayStored = new String[arrayLength];

    String hystoryResultTemp = null;
    String tickerResultTemp = null;
    boolean up = false;
    boolean startComputation = false;

    private double amountTemp = 0;

    private double btcDemo = 10;
    private double cnyDemo = 1;
    private final double percTraded = 0.1;
    private final double btcMin = 0.01;
    private final double feeCoefficient = 1.002;

    double averagePriceBought = 0;
    double averagePriceSold = 0;
    double averagePriceBoughtTemp = 0;
    double averagePriceSoldTemp = 0;
    double amountBTC = 0;
    double amountCNY = 0;
    double assetBTC = 0;
    double assetCNY = 0;
    String id = "1";

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd - HH:mm:ss");

    public Trader(OKCoinAPI okcoinPost, OKCoinAPI okcoinGet, double[] initialSequence) throws Exception {
        this.sequence = initialSequence;
        this.setTime();
        if (demoMode == false) {
            this.setBalanceReal(okcoinPost, okcoinGet);
        }

        //this.identifier = id;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    void setBalanceReal(OKCoinAPI okcoinPost, OKCoinAPI okcoinGet) throws Exception {
        String userinfo = okcoinPost.userinfo();
        JSONObject jsonobject = new JSONObject(userinfo);
        String infoBTCAmount = jsonobject.getJSONObject("info").getJSONObject("funds").getJSONObject("free").getString(currencyCrypto);
        String infoCNYAmount = jsonobject.getJSONObject("info").getJSONObject("funds").getJSONObject("free").getString(currencyFiat);

        String tickerResult = okcoinGet.ticker(symbol);
        JSONObject jsonTicker = new JSONObject(tickerResult);
        String ask = jsonTicker.getJSONObject("ticker").getString("sell");
        String bid = jsonTicker.getJSONObject("ticker").getString("buy");

        //amountBTC = Double.parseDouble(infoBTCAmount);
        //amountCNY = Double.parseDouble(infoCNYAmount);
        averagePriceBought = Double.parseDouble(ask);
        averagePriceSold = Double.parseDouble(bid);
        averagePriceBoughtTemp = averagePriceBought;
        averagePriceSoldTemp = averagePriceSold;

        //assetBTC = amountBTC * averagePriceBought;
        //assetCNY = amountCNY / averagePriceSold;
        System.out.println(currencyCrypto + ": " + amountBTC + " at average: " + averagePriceBought
                + " for asset" + currencyCrypto + ": " + assetBTC + "   ,CNY: " + amountCNY + " at average: " + averagePriceSold + " for asset CNY: " + assetCNY);
    }

    public void setBalanceDemo(OKCoinAPI okcoinGet) throws HttpException, IOException, JSONException {
        String tickerResult = okcoinGet.ticker(symbol);
        int strLength = tickerResult.length();
        //System.out.println(tickerResult);
        JSONObject jsonobject = new JSONObject(tickerResult);

        String bidS = jsonobject.getJSONObject("ticker").getString("buy");
        //System.out.println("Bid: " + bidS);
        cnyDemo = btcDemo * Double.parseDouble(bidS);
    }

    public void setTime() {
        start = System.currentTimeMillis();
        end = start + (timeVolume * 60 * 1000);
    }

    public void setData(OKCoinAPI okcoinPost, OKCoinAPI okcoinGet) throws HttpException, IOException, JSONException, Exception {
        String historyResult = null;

        try {
            historyResult = okcoinGet.trades(symbol, "99");
        } catch (HttpException ex) {
            System.out.println(ex);
            historyResult = hystoryResultTemp;
        } catch (SocketTimeoutException ste) {
            System.out.println("I timed out!");
            historyResult = hystoryResultTemp;
        }
        hystoryResultTemp = historyResult;

        JSONArray jsonarray = new JSONArray(historyResult);

        int length = 0;
        if (jsonarray.length() < arrayLength) {
            length = jsonarray.length();
        } else {
            length = arrayLength;
        }

        for (int i = 0; i < length; i++) {
            JSONObject jsonobject = jsonarray.getJSONObject(i);

            String tid = jsonobject.getString("tid");
            long tidN = Long.parseLong(tid);
            //System.out.println(tidN);

            String amount = jsonobject.getString("amount");
            double amountN = Double.parseDouble(amount);
            //System.out.println(amountN);

            String price = jsonobject.getString("price");
            double priceN = Double.parseDouble(price);
            //System.out.println(priceN);

            String type = jsonobject.getString("type");
            //System.out.println(type);

            tidNArray[i] = tidN;
            amountNArray[i] = amountN;
            priceNArray[i] = priceN;
            typeArray[i] = type;

            //System.out.println("Position: " + i + " ,ID array readed: " + tid + " ,Amount array readed: " + amount + " ,Price array readed:   " + price + " ,Type array readed:   " + type);
        }
        //System.out.println();

        compareArray();

        volumeSum();

        if (startComputation == true) {
            computation(okcoinPost, okcoinGet);
        }
    }

    void compareArray() {
        int positionStart = (arrayLength - 1);
        int positionEnd = 0;

        for (int i = 0; i < arrayLength; i++) {
            if (tidNArrayStored[(arrayLength - 1)] <= tidNArray[i]) {
                //System.out.println(tidNArrayStored[(arrayLength - 1)] + "  " + tidNArray[i]);
                positionStart = i;
                i = arrayLength;
            }
            //System.out.println(tidNArrayStored[(arrayLength - 1)] + "  " + tidNArray[i]);
        }

        for (int k = 0; k < arrayLength; k++) {
            amountNArrayStored[k] = 0;
        }

        for (int j = positionStart; j < arrayLength; j++) {
            //System.out.println(tidNArrayStored[(j - positionStart)] + "  " + tidNArray[j]);
            tidNArrayStored[(j - positionStart)] = tidNArray[j];
            //System.out.println(tidNArrayStored[(j - positionStart)] + "  " + tidNArray[j]);
            amountNArrayStored[(j - positionStart)] = amountNArray[j];
            priceNArrayStored[(j - positionStart)] = priceNArray[j];
            typeArrayStored[(j - positionStart)] = typeArray[j];
            positionEnd = j;
        }

        if (positionStart != 0) {
            tidNArrayStored[(arrayLength - 1)] = tidNArrayStored[((arrayLength - 1) - positionStart)];
            amountNArrayStored[0] = 0;
        }

        /*System.out.println("PositionStart: " + positionStart + "  Position End: " + positionEnd);

        for (int m = 0; m < arrayLength; m++) {
            System.out.println("ID array readed: " + tidNArray[m] + " ,amount array readed:  " + amountNArray[m] + " ,ID array stored:  " + tidNArrayStored[m] + " ,amount array stored:  " + amountNArrayStored[m]);
        }
        System.out.println();
        System.out.println();
        System.out.println();*/
    }

    void volumeSum() {
        for (int m = 0; m < arrayLength; m++) {
            volumeTotal = volumeTotal + amountNArrayStored[m];
        }

        if (System.currentTimeMillis() > end) {
            this.sequence[0] = (volumeTotal / volumeTimeRatio);
            //this.sequence[1] = (volumeTotal / volumeTimeRatio);
            System.out.println("Volume Total: " + volumeTotal);
            System.out.println("Sequence Value: " + this.sequence[0]);
            setTime();
            volumeTotal = 0;
            startComputation = true;
        }
    }

    void computation(OKCoinAPI okcoinPost, OKCoinAPI okcoinGet) throws IOException, JSONException, Exception {
        double sum = 0;
        double bid = 0;
        double ask = 0;

        for (int i = 0; i < arrayLength; i++) {
            double amount = amountNArrayStored[i];
            String type = typeArrayStored[i];
            long tid = tidNArrayStored[i];

            sum = sumAmount(amount, type);
            /*if (amount != 0) {
                System.out.println("Sum: " + sum + ", Amount: " + amount + " of type: " + type + " at position: " + i + " tID: " + tid);
            }*/

            if (sumExed(getPosition()) == true) {
                i = arrayLength;

                String tickerResult;
                try {
                    tickerResult = okcoinGet.ticker(symbol);
                } catch (HttpException ex) {
                    System.out.println(ex);
                    tickerResult = tickerResultTemp;
                } catch (SocketTimeoutException ste) {
                    System.out.println("I timed out!");
                    tickerResult = tickerResultTemp;
                }
                tickerResultTemp = tickerResult;

                //System.out.println(tickerResult);
                JSONObject jsonobject = new JSONObject(tickerResult);

                String bidS = jsonobject.getJSONObject("ticker").getString("buy");
                //System.out.println("Bid: " + bidS);
                bid = Double.parseDouble(bidS);

                String askS = jsonobject.getJSONObject("ticker").getString("sell");
                //System.out.println("Ask: " + askS);
                ask = Double.parseDouble(askS);

                System.out.println("Sequence: " + sequence[getPosition()] + " Position: " + getPosition() + " Sum: " + sum + " Ask: " + ask + "  Bid: " + bid);

                if (sum > 0) {

                    calcOrdersReal(okcoinPost, okcoinGet, "buy");
                    sum = 0;
                    sequenceManaging();
                }

                if (sum < 0) {

                    calcOrdersReal(okcoinPost, okcoinGet, "sell");
                    sum = 0;
                    sequenceManaging();
                }
            }
        }
    }

    void sequenceManaging() {
        if (getPosition() == 0) {
            up = true;
        }
        if (getPosition() == (sequence.length - 1)) {
            //up = false;
            setPosition(-1);
        }

        if (up == true) {
            setPosition(getPosition() + 1);
//            setSequence(sequence[getPosition()]);
        } else {
            setPosition(getPosition() - 1);
            //           setSequence(sequence[getPosition()]);
        }
    }

    public double sumAmount(double amount, String type) {
        try {
            if (type.equals("buy")) {
                amountTemp = amountTemp + amount;
            } else {
                amountTemp = amountTemp - amount;
            }
        } catch (Exception exc) {
            System.out.println(exc);
        }

        //System.out.println("Sum: " + amountTemp);
        return amountTemp;
    }

    public boolean sumExed(int sequencePosition) {
        boolean exed = false;

        if (amountTemp > sequence[sequencePosition]) {
            exed = true;
            System.out.println("Exeded!!! Sum: " + amountTemp);
        }
        if (amountTemp < (-sequence[sequencePosition])) {
            exed = true;
            System.out.println("Exeded!!! Sum: " + amountTemp);
        }

        return exed;
    }

    public void equalizationReal(OKCoinAPI okcoinPost, String id) throws Exception {
        String lastTradeAmount = "";
        String lastTradePrice = "";
        String lastTradeType = "";

        String lastTrade = okcoinPost.order_info(symbol, id);
        JSONObject jsonobject = new JSONObject(lastTrade);
        JSONArray orders = jsonobject.getJSONArray("orders");

        for (int i = 0; i < orders.length(); i++) {
            JSONObject jsonorder = orders.getJSONObject(i);
            String idArray = jsonorder.getString("order_id");

            if (id.equals(idArray)) {
                lastTradeAmount = jsonorder.getString("deal_amount");
                lastTradePrice = jsonorder.getString("avg_price");
                lastTradeType = jsonorder.getString("type");
                i = orders.length();
            }
        }

        System.out.println(lastTradeAmount + "  " + lastTradePrice + "  " + lastTradeType);

        double lastTradeEquity = Double.parseDouble(lastTradeAmount) * Double.parseDouble(lastTradePrice);

        if (lastTradeType.equals("buy_market")) {
            amountBTC = amountBTC + Double.parseDouble(lastTradeAmount);
            if (amountCNY >= lastTradeEquity) {
                amountCNY = amountCNY - lastTradeEquity;
            }
            assetBTC = assetBTC + lastTradeEquity;
            assetCNY = amountCNY / averagePriceSold;

            if (assetBTC == 0 || amountBTC == 0) {
                averagePriceBought = averagePriceBoughtTemp;
            } else {
                averagePriceBought = Math.abs((assetBTC / amountBTC)) * feeCoefficient;
                averagePriceBoughtTemp = averagePriceBought;
            }

        }
        if (lastTradeType.equals("sell_market")) {
            if (amountBTC >= Double.parseDouble(lastTradeAmount)) {
                amountBTC = amountBTC - Double.parseDouble(lastTradeAmount);
            }
            amountCNY = amountCNY + lastTradeEquity;
            assetBTC = amountBTC * averagePriceBought;
            assetCNY = assetCNY + Double.parseDouble(lastTradeAmount);

            if (assetCNY == 0 || amountCNY == 0) {
                averagePriceSold = averagePriceSoldTemp;
            } else {
                averagePriceSold = Math.abs((amountCNY / assetCNY)) / feeCoefficient;
                averagePriceSoldTemp = averagePriceSold;
            }

        }

        System.out.println(currencyCrypto + ": " + amountBTC + " at average: " + averagePriceBought
                + " for asset" + currencyCrypto + ": " + assetBTC + "   ,CNY: " + amountCNY + " at average: " + averagePriceSold + " for asset CNY: " + assetCNY);
    }

    public void equalizationDemo() {

    }

    public void calcOrdersReal(OKCoinAPI okcoinPost, OKCoinAPI okcoinGet, String type) throws Exception {
        String accountInfo = okcoinPost.userinfo();
        //System.out.println(accountInfo);
        String ticker = okcoinGet.ticker(symbol);
        //System.out.println(ticker);

        JSONObject jsonobject = new JSONObject(accountInfo);
        //String balanceBTCstring = jsonobject.getJSONObject("info").getJSONObject("balance").getJSONObject(currencyCrypto).getString("amount");
        String balanceCNYstring = jsonobject.getJSONObject("info").getJSONObject("funds").getJSONObject("free").getString(currencyFiat);
        String balanceBTCstring = jsonobject.getJSONObject("info").getJSONObject("funds").getJSONObject("free").getString(currencyCrypto);
        //System.out.println(balanceCNYstring);

        JSONObject jsonobjectTicker = new JSONObject(ticker);
        String lastTrade = jsonobjectTicker.getJSONObject("ticker").getString("last");
        //System.out.println(lastTrade);

        String ask = jsonobjectTicker.getJSONObject("ticker").getString("sell");
        String bid = jsonobjectTicker.getJSONObject("ticker").getString("buy");

        //double balanceBTC = Double.parseDouble(balanceBTCstring);
        double balanceBTC = 0;
        double balanceCNY = (Double.parseDouble(balanceCNYstring) + (Double.parseDouble(balanceBTCstring) * Double.parseDouble(bid)));

        //System.out.println("Balance BTC: " + balanceBTC);
        //System.out.println("Balance CNY: " + balanceCNY);
        balanceBTC = balanceBTC + (balanceCNY / Double.parseDouble(lastTrade));

        double amountToTrade = (balanceBTC / 100) * percTraded;
        amountToTrade = Math.floor(amountToTrade * 1e3) / 1e3;
        if (amountToTrade < btcMin) {
            amountToTrade = btcMin;
        }

        //////////////////////////////
        //amountToTrade = btcMin * 1.2;
        //////////////////////////////
        //////TEST for real bid and ask value
        /* String tickerResult;    
        tickerResult = btcc.getTicker(symbol);       
        JSONObject jsonobjectTicker = new JSONObject(tickerResult);
        String bidS = jsonobjectTicker.getJSONObject("ticker").getString("buy");
        String askS = jsonobjectTicker.getJSONObject("ticker").getString("sell");
        System.out.println("Ask: " + askS + "  Bid: " + bidS);      */
        //System.out.println("Balance Total: " + balanceBTC);
        //System.out.println("Balance to Trade : " + amountToTrade);
        //Thread.sleep(200);
        if (type.equals("sell") && balanceCNY >= (amountToTrade * Double.parseDouble(ask)) && Double.parseDouble(ask) < averagePriceSold) {
            String trade = okcoinPost.trade(symbol, "buy_market", String.valueOf(amountToTrade * Double.parseDouble(ask)), "");
            System.out.println(trade);
            JSONObject jsonobjectTrade = new JSONObject(trade);
            String result = jsonobjectTrade.getString("result");
            if (result.equals("true")) {
                id = jsonobjectTrade.getString("order_id");
                System.out.println(id);

                equalizationReal(okcoinPost, id);

                System.out.println(sdf.format(new Date()) + " BTC buyed: " + amountToTrade + "   ,Ask: " + ask + "   ,Bid: " + bid);
                String account = okcoinPost.userinfo();
                System.out.println(account);
                System.out.println();
                System.out.println();
            }

        }

        if (type.equals("buy") && balanceBTC >= amountToTrade && Double.parseDouble(bid) > averagePriceBought) {
            String trade = okcoinPost.trade(symbol, "sell_market", "", String.valueOf(amountToTrade));
            System.out.println(trade);
            JSONObject jsonobjectTrade = new JSONObject(trade);
            String result = jsonobjectTrade.getString("result");
            if (result.equals("true")) {
                id = jsonobjectTrade.getString("order_id");
                System.out.println(id);

                equalizationReal(okcoinPost, id);

                System.out.println(sdf.format(new Date()) + " BTC sold: " + amountToTrade + "   ,Ask: " + ask + "   ,Bid: " + bid);
                String account = okcoinPost.userinfo();
                System.out.println(account);
                System.out.println();
                System.out.println();
            }

        }

        amountTemp = 0;
    }

}
