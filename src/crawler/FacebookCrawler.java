package crawler;

import model.*;
import model.Content;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FacebookCrawler extends ICrawler {
    @Override
    public void CrawlArticleList() {
        List<Article> articles = new ArrayList<>();
        WebDriver driver = getPageSource(100);
        List<WebElement> posts = driver.findElements(By.xpath("//div[@class='x1yztbdb x1n2onr6 xh8yej3 x1ja2u2z']"));
        articles = crawlData(posts,articles);
        System.out.println(articles.size());
        SaveToJson(articles);
    }

    @Override
    public Content CrawlArticleContent(int id) {
        return null;
    }

    //Lưu object vào json
    @Override
    public void SaveToJson(List<Article> list) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode ngNodes =mapper.createArrayNode();
            for(Article post : list){
                ObjectNode ngNode =mapper.createObjectNode();
                ngNode.put("id", post.getID());
                ngNode.put("sourceUrl", post.getSourceUrl());
                ngNode.put("sourceName", post.getSourceName());
                ngNode.put("author", post.getAuthor());
                ngNode.put("content", post.getContent());
                ngNode.put("publishedDate", String.valueOf(post.getPublishedAt()));
                ngNodes.add(ngNode);
            }
            ObjectNode root = mapper.createObjectNode();
            root.set("FacebookPost",ngNodes);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("src/crawler/facebook.json"),root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    //Lấy object từ file json
    @Override
    public List<Article> GetArticlesFromJson() {
        List<Article> articles = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.reader();
        ObjectNode fbObject;
        try {
            fbObject = reader.forType(new TypeReference<ObjectNode>() {
            }).readValue(new File("src/Storage/facebook.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayNode arrayNode = fbObject.withArray("FacebookPost");
        for(JsonNode node : arrayNode){
            String sourceUrl = node.get("sourceUrl").asText();
            String author = node.get("author").asText();
            Content content = new Content(node.get("content").asText());
            String time = node.get("publishedDate").asText().substring(0,16);
            LocalDateTime prettyTime = parseDateTime(time);
            articles.add(new Article("",author,content,prettyTime,sourceUrl));
        }
        return articles;

    }



    //Truy cap va lay ma nguon trang web
    public static WebDriver getPageSource(int numScroll) {
        //Khoi  tao Webdriver
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        WebDriver driver = new ChromeDriver();

        //Truy cap trang web
        String url = "https://www.facebook.com/blockchain";
        driver.get(url);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        WebElement button = driver.findElement(By.xpath("//div[@class='x92rtbv x10l6tqk x1tk7jg1 x1vjfegm']"));
        button.click();

        //Cuon trang
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int i = 0; i < numScroll; i++) {
            long initialHeight = (long) js.executeScript("return document.body.scrollHeight");

            // Thực hiện lăn chuột với JavaScript Executor
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");

            // Sử dụng Selenium Wait để đợi đến khi trang đã hoàn tất tải thêm tweet mới
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.loading-spinner")));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return driver;
    }




    public List<Article> crawlData(List<WebElement> posts, List<Article> articles){
        for(WebElement post : posts){
            //Lay link bai viet
            WebElement links = post.findElement(By.xpath(".//span[@class='x4k7w5x x1h91t0o x1h9r5lt x1jfb8zj xv2umb2 x1beo9mf xaigb6o x12ejxvf x3igimt xarpa2k xedcshv x1lytzrv x1t2pt76 x7ja8zs x1qrby5j']//a"));
            String link = links.getAttribute("href");
            System.out.println(link);

            //Lay thoi gian
            String time = extractSubstring(links.getText(), "Shared");
            System.out.println(time);
            LocalDateTime prettyTime = parseDateTime(time);

            //Lay noi dung
            Content content =new Content(post.findElement(By.xpath(".//div[@dir='auto']")).getText());

            //Lay hinh anh
            WebElement images = post.findElement(By.xpath("//div[@class='x10l6tqk x13vifvy']//img"));
            if(images != null){
                Image image = new Image(images.getAttribute("src"));
                content.AddElement(image);
            }

            //Them object vao list
            articles.add(new Article("","Blockchain.com",content,prettyTime,link));
        }
        return articles;
    }



    //Chinh sua chuoi
    public static String extractSubstring(String originalString, String index) {
        int endIndex = originalString.indexOf(index);
        if (endIndex != -1) {
            return originalString.substring(0, endIndex);
        } else {
            return originalString; // Trả về chuỗi ban đầu nếu không tìm thấy chuỗi index
        }
    }



    //Chinh sua thoi gian
    public static LocalDateTime parseDateTime(String input) {
        LocalDateTime dateTime = null;

        if (input.contains("d")) {
            // Xử lý trường hợp "n d" là n ngày trước
            int daysAgo = Integer.parseInt(input.split(" ")[0]);
            dateTime = LocalDateTime.now().minusDays(daysAgo);
        }else {
            if (input.contains("h") || input.contains("m")) {
                dateTime = LocalDateTime.now();
            } else {
                if (input.contains("at")) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM 'at' HH:mm yyyy");
                    dateTime = LocalDateTime.parse(input + " " + LocalDateTime.now().getYear(), formatter);
                }else{
                    if (input.contains(",")) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM,yyyy HH:mm");
                        dateTime = LocalDateTime.parse(input + " 00:00", formatter);
                    }else{
                        if(input.contains("-")){
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                            dateTime = LocalDateTime.parse(input,formatter);
                        }else{
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm");
                        dateTime = LocalDateTime.parse(input + " " + LocalDateTime.now().getYear() + " 00:00", formatter);
                        }
                    }
                }
            }
        }
        return dateTime;
    }



    public static void main(String[] args) {
        FacebookCrawler a = new FacebookCrawler();
        a.CrawlArticleList();
    }
}



