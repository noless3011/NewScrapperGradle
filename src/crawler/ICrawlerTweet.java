package crawler;

import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import adapter.ProgressCallback;
import model.Article;
import model.Tweet;

public interface ICrawlerTweet {
	//Hàm này sẽ thu thập 1 lượng article mà người dùng nhập vào tron UI
	public abstract void crawlTweetList(int amount);
	//Need to check if the tweet already exist to not have to store a duplicate using the id of the article
	public abstract void saveToJson(List<Tweet> list);
	public abstract List<Article> getTweetFromJson();
}
