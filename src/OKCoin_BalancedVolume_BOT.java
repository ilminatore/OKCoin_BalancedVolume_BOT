/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package okcoin_balancedvolume_bot;

import okcoin_balancedvolume_bot.OKCoinAPI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
//import org.json.JSONArray;
//import org.json.JSONObject;

/**
 *
 * @author Marco
 */
public class OKCoin_BalancedVolume_BOT {

    static String api_key = "";  //OKCoin apiKey
    static String secret_key = "";  //OKCoin secret_key
    static String url_prex = "https://www.okcoin.cn";  //OKCoin link

    public static void main(String args[]) throws Exception {

        double[] input = inputReader();
        boolean exit = false;

        OKCoinAPI okcoinPost = new OKCoinAPI(url_prex, api_key, secret_key);
        OKCoinAPI okcoinGet = new OKCoinAPI(url_prex);

        //btcc.connectAndTrade("accountInfo", "0");
        //btcc.connectAndTrade("buyOrder2Market", "0.001");
        Trader trader = new Trader(okcoinPost, okcoinGet, input);

        trader.setBalanceDemo(okcoinGet);

        while (exit == false) {
            Thread.sleep(100);
            trader.setData(okcoinPost, okcoinGet);
        }
    }

    public static double[] inputReader() throws IOException {

        int totalTraders = 1;
        double sequenceTraders[] = new double[totalTraders];

        sequenceTraders[0] = 1;

        System.out.println("Insert API key:");
        InputStreamReader readerApi = new InputStreamReader(System.in);
        BufferedReader inputApi = new BufferedReader(readerApi);
        
        api_key = inputApi.readLine();
        
        System.out.println("Insert Secret key:");
        InputStreamReader readerSecret = new InputStreamReader(System.in);
        BufferedReader inputSecret = new BufferedReader(readerSecret);
        
        secret_key = inputSecret.readLine();
 
        return sequenceTraders;
    }
}
