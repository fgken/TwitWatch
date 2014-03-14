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
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.String;

class TwitWatch {
	public static void main(String[] args) {
		Twitter twitter = new TwitterFactory().getSingleton();

		if( args.length < 5 ) {
			System.out.println("TwitWatch MailAddress ConsumerKey ConsumerSecret AccessToken AccessTokenSecret");
			return;
		}
		
		String mailAddress = args[0];
		String ConsumerKey = args[1];
		String ConsumerSecret = args[2];
		String AccessToken = args[3];
		String AccessTokenSecret = args[4];

		System.out.println("Fetching Hash...");

		// Read Settings
		// http://docs.oracle.com/javase/jp/1.5.0/api/java/util/Properties.html
		// http://www.java-tips.org/java-se-tips/java.util/how-to-use-an-ini-file.html
		// http://www.ipa.go.jp/security/awareness/vendor/programmingv2/contents/c901.html

		// Get AccessToken by OAuth
		try {
			twitter.setOAuthConsumer(ConsumerKey, ConsumerSecret);
			RequestToken requestToken = twitter.getOAuthRequestToken();
			AccessToken accessToken = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while(null == accessToken) {
				System.out.println("Open the following URL and grant access to your account:");
				System.out.println(requestToken.getAuthorizationURL());
				System.out.println("Enter the PIN(if available) or just hit enter.{PIN]:");
				//String pin = br.readLine();
				String pin="";

				try {
					if(pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken, pin);
					} else {
						//accessToken = twitter.getOAuthAccessToken();
						accessToken = new AccessToken(AccessToken, AccessTokenSecret);
						twitter.setOAuthAccessToken(accessToken);
					}
				} catch(TwitterException te) {
					if(401 == te.getStatusCode()) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}
		} catch(Exception e) {
			System.err.println("Error: OAuth - " + e.getMessage());
		}

		// Create and Run Twitter Watchers
		List<TwitterWatcher> watchers = new ArrayList<TwitterWatcher>();
	
		for(TwitterWatcher watcher : watchers) {
			watcher.start();
		}

		while(true) {
			try {
				Thread.sleep(60*60*1000);
			} catch(Exception e) {
				e.printStackTrace();
			}
			if(false)break;
		}

		for(TwitterWatcher watcher : watchers) {
			try {
				watcher.stopWatching();
				watcher.join();
			} catch(InterruptedException e){
				e.printStackTrace();
			}
		}

		System.out.println("TwitWatch!");
	}
}
