import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.auth.RequestToken;
import twitter4j.auth.AccessToken;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.String;
import java.lang.Thread;

class TwitterWatcher extends Thread {
	private Twitter twitter;
	private String queryStr = "";
	private List<String> indeedRegExpList = new ArrayList<String>();
	private String exceptRegExp;
	private String mailAddress = "";
	private long lastTweetId = 0;
	private long interval = 3*60*60;		// seconds
	private boolean running = true;
	private List<String> mailedTweets = new ArrayList<String>();
	private int maxMailedTweets = 100;

	private int iii=0;

	TwitterWatcher(Twitter twitter) {
		this.twitter = twitter;
	}

	TwitterWatcher(Twitter twitter, String queryStr, String indeedRegExp, String exceptRegExp, String mailAddress) {
		this.twitter = twitter;
		this.queryStr = queryStr;
		this.indeedRegExpList.add(indeedRegExp);
		this.exceptRegExp = exceptRegExp;
		this.mailAddress = mailAddress;
	}

	TwitterWatcher(Twitter twitter, String queryStr, List<String> indeedRegExpList, String exceptRegExp, String mailAddress) {
		this.twitter = twitter;
		this.queryStr = queryStr;
		this.exceptRegExp = exceptRegExp;
		this.mailAddress = mailAddress;
		for(String indeedRegExp : indeedRegExpList) {
			this.indeedRegExpList.add(".*"+indeedRegExp+".*");
		}
	}

	public void run() {
		while(running) {
			List<Status> tweets;
		
			tweets = getSearchedTweets(twitter, queryStr, indeedRegExpList, exceptRegExp, lastTweetId);
			if(tweets != null && tweets.isEmpty() == false) {
				String mailText = "";
				lastTweetId = tweets.get(0).getId();
		   		for(Status tweet : tweets) {
					mailText += tweet.getCreatedAt()+" "+tweet.getUser().getName()
						+"("+tweet.getUser().getScreenName()+")"+"\n"
						+tweet.getText().replaceAll("\n", " ")+"\n\n";
				}
				try{
					// Send mail
					Process process = Runtime.getRuntime().exec("sendmail "+mailAddress);
					PrintStream out = new PrintStream(process.getOutputStream());
					out.println("Subject: "+queryStr+" : "+indeedRegExpList.toString()+"\n"
								+ "Content-type: text/plain; charset="+System.getProperty("file.encoding")+"\n"
								+ mailText+"\n.\n");
					out.close();
					Thread.sleep(10000);
					process.destroy();
					System.out.println("Send Mail" + mailText);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			// Sleep
			for(int i=0; i<interval && running; i++) {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Queryに指定できる演算子は，下記URL参照
	// https://twitter.com/search-home
	private List<Status> getSearchedTweets(Twitter twitter, String queryStr, List<String> indeedRegExpList, String exceptRegExp, long sinceId) {
		List<Status> resultTweets = new ArrayList<Status>();
		List<Status> searchedTweets;

		// get the 100 most recent tweets tagged #mnp
		for(int page=1; page<=1; page++) {
			Query query = new Query(queryStr);
			query.setResultType(query.RECENT);
			query.setCount(100);
			query.setSinceId(sinceId);
			// Todo: using Paging class
			//query.setPage(page);
			try {
				searchedTweets = twitter.search(query).getTweets();
			} catch(Exception e) {
				System.err.println("Error: QueryResult.getTweets - " + e.getMessage());
				return null;
			}

			for(Status tweet : searchedTweets) {
				String text = tweet.getText();
				text = text.replaceAll("\n", " ");

				boolean indeed = true;
				for(String indeedRegExp : indeedRegExpList) {
					if(text.matches(indeedRegExp) == false) {
						System.out.println("--- log ---\n"+indeedRegExp+" "+text);
						indeed = false;
						break;
					}
				}

				if(indeed == true && text.matches(exceptRegExp) == false) {
					boolean overlaped = false;

					// avoid overlap
					for(String mailedText : mailedTweets){
						System.out.println("--- cmp: \ntext      ="+text+"\nmailedText="+mailedText);
						if(text.equals(mailedText)) {
							System.out.println("--- Overlaped ---"+iii++);
							overlaped = true;
							break;
						}
					}
					
					if(overlaped == false) {
						if(mailedTweets.size() >= maxMailedTweets) {
							if(!mailedTweets.isEmpty()) mailedTweets.remove(0);
						}else{
							System.out.println("--- Add mailedTweets: size="+mailedTweets.size()+"\ntext="+text);
							mailedTweets.add(text);
						}
						resultTweets.add(tweet);
					}else{
						System.out.println("--- Avoid overlap ---\n"+text);
					}
				}
			}
		}

		return resultTweets;
	}

	void setInterval(long interval) {
		this.interval = interval;
	}

	void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}

	void stopWatching() {
		running = false;
	}
}
