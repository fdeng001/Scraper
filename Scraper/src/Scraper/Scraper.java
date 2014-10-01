package Scraper;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import org.jsoup.select.Elements;

import java.net.*;
import java.text.NumberFormat;
import java.io.*;

public class Scraper {
	public static void main(String[] args) {
		if (args.length == 0 || args.length > 2) {
			System.out.println("Please input valid parameters.");
			return;
		}
		/* when one arguments are provided,
		 * return result number
		 */
		if (args.length == 1) {
			int num_results = Scraper.ResultNumber(args[0]);
			if (num_results == 0) {
				System.out.println("No results found.");
			} else {
				System.out.println("Number of results is : " + num_results);
			}
		}

		/* when two arguments are provided,
		 * return result objects for the specific page
		 */
		
		if (args.length == 2) {
			try {
				int num_results = Scraper.ResultNumber(args[0]);
				if (num_results == 0) {
					System.out.println("No results found.");
				} else {
					System.out.println("Number of results is : " + num_results);
				}

				int number = Integer.parseInt(args[1]);
				if (number <= 0) {
					System.out.println("You should give a valid page value.");
				}

				String page_link = Scraper.findPage(args[0], number);
				if (page_link == null) {
					System.out.println("This page is not found.");
				} else {
					System.out.println("Below is the resuls list in the page number:" + number);
					Scraper.showResultInfo(page_link);
				}

			} catch (NumberFormatException nfe){
				System.out.println("You should give an integer value to the second parameter.");
				return ;
			}
		}
		

	}
	/*
	 * get result number from the keyword
	 * 
	 */

	public static int ResultNumber(String keyword){
		String tmpKeyword = keyword.trim();
		if(tmpKeyword=="" || keyword ==null)
			return 0;
		String newurl = translateUrl(keyword);
		String wholeurl = "http://www.walmart.com/search/search-ng.do?ic=16_0&Find=Find&search_query="+newurl+"&Find=Find&search_constraint=0";
		String html = getUrl(wholeurl);
		/* Jsoup
		 * Convert Java String to DOM document
		 */
		Document doc = Jsoup.parse(html);
		try{
			String contentText = doc.select(".result-summary-container").text();
			if(contentText==null)
				return 0;
			else{
				String[] contentlist = contentText.split(" ");
				int res = Integer.parseInt(contentlist[3]);
				return res;
			}
		}
		catch(Exception e){
			return  0;
		}

	}
	/*
	 * translate keyword to acceptable url's keyword
	 * example: input :"digital camera", "  digital   camera",
	 * 			output:"digital+camera", "++digital+++camera"
	 */
	public static String translateUrl(String url){
		String resUrl = url.replaceAll(" ", "%20");
		return resUrl;
	}

	/*
	 * get the url's document
	 */
	public static String getUrl(String url){
		URL urlObj = null;
		try{
			urlObj = new URL(url);
		}
		catch(MalformedURLException e){
			System.out.println("The url was malformed!");
			return "";
		}
		URLConnection urlCon = null;
		BufferedReader in = null;
		String outputText = "";
		try{
			urlCon = urlObj.openConnection();
			in = new BufferedReader(new
					InputStreamReader(urlCon.getInputStream()));
			String line = "";
			while((line = in.readLine()) != null){
				outputText += line;
			}
			in.close();
		}catch(IOException e){
			System.out.println("There was an error connecting to the URL");
			return "";
		}
		return outputText;
	}

	/*
	 * find the specific page for the keyword.
	 */
	public static String findPage(String keyword, int number) {
		String tmpKeyword = keyword.trim();
		if(tmpKeyword=="" || keyword ==null)
			return null;
		String newurl = translateUrl(keyword);
		String start = "http://www.walmart.com/search/?query="+newurl;
		/*If page 1 is wanted, return the current page */
		if (number == 1) {
			return start;
		}

		String html = getUrl(start);

		/* Convert Java String to DOM document */
		Document doc = Jsoup.parse(html);

		if (doc.select("div.paginator") != null) {
			try{
				int pageNum = Integer.parseInt(doc.select("ul.paginator-list > li").last()
						. select("> a").text());
				if (number <= pageNum) {
					String link = "http://www.walmart.com/search/?query="+newurl+"&page="+number+"&cat_id=0";
					return link;
				} else {
					return null;
				}
			}
			catch(Exception e){
				return null;
			}
		}

		return null;
	}

	/*
	 * print the result list
	 */
	public static void showResultInfo(String url) {
		String jsp = getUrl(url);
		if (jsp == null) {
			System.out.println("Failed to retrieve web page.");
			return ;
		}

		/* Jsoup
		 * Convert Java String to DOM document
		 */
		Document doc = Jsoup.parse(jsp);
		Elements currentPrice = null;
		int num = doc.select("div.js-tile.tile-landscape").size();
		for (int i = 0; i < num; i++){
			currentPrice = null;
			String price="";
			//two digit after decimal
			final NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(2);
			nf.setMaximumFractionDigits(2);
			nf.setGroupingUsed(false);
			
			boolean skip =false; // some price is "Click here for price"
			Element current = doc.select("div.js-tile.tile-landscape").get(i);

			String name = current.select("a.js-product-title").text();
			System.out.println("Title/Product Name: "+name);

			Element currentPricecontainer = current.select(".tile-row").first();

			if(currentPricecontainer.select(".item-price-container").first()!=null){
				currentPrice=currentPricecontainer.select(".item-price-container");
				if(currentPrice.attr("baseprice")!=""){ // if this item is "Click here for price"
					String priceS = currentPrice.attr("baseprice");
					priceS = nf.format(Double.parseDouble(priceS));
					price = "$"+priceS;// it will show baseprice.
					skip = true;
				}
				else{
					if(currentPrice.select("span.price.price-display").first()!=null){
						if(currentPrice.select("span.price-from").first()==null)//some price is "from xxx"
							currentPrice = currentPrice.select("span.price.price-display");//regular price
					}
					//above include out of stock
					else{
						currentPrice = currentPrice.select(".price-label");//Price shown in Checkout
					}
				}
			}
			
			if(!skip){
				try{
					price = currentPrice.text();
				}
				catch(Exception e){
					price = "";
				}
			}
			else{
				skip=false;
			}
			System.out.println("Price: "+price);
			System.out.println("******************************************");
		}
	}
}
