package searchengine;


import java.io.IOException;
import java.util.List;


import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import model.Article;
import model.Facebook;
import model.Tweet;

public class AddDocument {
	
	private EntityRecognition entityrecognition = new EntityRecognition();
	
	public void tweet(IndexWriter writer,Tweet tweet) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("author", tweet.getAuthor(), Field.Store.YES));
		doc.add(new TextField("content", tweet.getContent().toString(), Field.Store.YES));
		doc.add(new TextField("view", tweet.getNumber_of_view(), Field.Store.YES));
		doc.add(new TextField("like", tweet.getNumber_of_liked(), Field.Store.YES));
		doc.add(new TextField("comment", tweet.getNumber_of_comment(),Field.Store.YES));
		long date = DateRange.formatterTimeToEpochSecond(tweet.getPublishedAt());
		doc.add(new TextField("date", Long.toString(date), Field.Store.YES));
		doc.add(new TextField("url", tweet.getSourceUrl(), Field.Store.YES));
		List <String> hashtags = tweet.getHashtags();
		for (String hashtag : hashtags ) {
			doc.add(new TextField("hashtag", hashtag, Field.Store.YES));
		}
		doc.add(new TextField("indexType", "Tweet", Field.Store.YES));
		List<String> entities = entityrecognition.SimpleEntityRecognition(tweet.getContent().toString());
		for (String entity: entities) {
			doc.add(new TextField("entity", entity, Field.Store.YES));
		}
		writer.addDocument(doc);
	}
	
	public void article(IndexWriter writer, Article article) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("author", article.getAuthor(), Field.Store.YES));
		if (article.getTitle() == null) {
			doc.add(new TextField("title", "", Field.Store.YES));
		} else
			doc.add(new TextField("title", article.getTitle(), Field.Store.YES));
		doc.add(new TextField("content", article.getContent().toString(), Field.Store.YES));
		long date = DateRange.formatterTimeToEpochSecond(article.getPublishedAt());
		doc.add(new TextField("date", Long.toString(date), Field.Store.YES));
		doc.add(new TextField("url", article.getSourceUrl(), Field.Store.YES));
		doc.add(new TextField("indexType", "Article", Field.Store.YES));
		List<String> entities = entityrecognition.SimpleEntityRecognition(article.getContent().toString());
		for (String entity : entities) {
			doc.add(new TextField("entity", entity, Field.Store.YES));
		}
		writer.addDocument(doc);
	}
	
	public void facebook(IndexWriter writer, Facebook facebook) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("author", facebook.getAuthor(), Field.Store.YES));
		doc.add(new TextField("content", facebook.getContent().toString(), Field.Store.YES));
		long date = DateRange.formatterTimeToEpochSecond(facebook.getPublishedAt());
		doc.add(new TextField("date", Long.toString(date), Field.Store.YES));
		doc.add(new TextField("url", facebook.getSourceUrl(), Field.Store.YES));
		doc.add(new TextField("comment", facebook.getNumber_of_comment(), Field.Store.YES));
		doc.add(new TextField("reaction", facebook.getNumber_of_reaction(), Field.Store.YES));
		doc.add(new TextField("share", facebook.getNumber_of_share(), Field.Store.YES));
		doc.add(new TextField("urlimg", facebook.getImgUrl(), Field.Store.YES));
		doc.add(new TextField("indexType", "Facebook", Field.Store.YES));
		List<String> entities = entityrecognition.SimpleEntityRecognition(facebook.getContent().toString());
		for (String entity: entities) {
			doc.add(new TextField("entity", entity, Field.Store.YES));
		}
		writer.addDocument(doc);
	}
	
}
