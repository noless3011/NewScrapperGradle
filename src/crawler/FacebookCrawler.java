package crawler;

import model.*;

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
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FacebookCrawler implements ICrawlerFacebook {
    @Override
    public void crawlFacebookList() {
        List<Facebook> articles = new ArrayList<>();
        WebDriver driver = getPageSource(2);
        List<WebElement> posts = driver.findElements(By.xpath("//div[@class='x1yztbdb x1n2onr6 xh8yej3 x1ja2u2z']"));
        articles = crawlData(posts,articles);
        System.out.println(articles.size());
        saveToJson(articles);
        driver.quit();
    }

    //Lưu object vào json
    @Override
    public void saveToJson(List<Facebook> list) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode ngNodes =mapper.createArrayNode();
            for(Facebook post : list){
                ObjectNode ngNode =mapper.createObjectNode();
                ngNode.put("id", post.getID());
                ngNode.put("sourceUrl", post.getSourceUrl());
                ngNode.put("author", post.getAuthor());
                ngNode.put("content", post.getContent());
                ngNode.put("publishedDate", String.valueOf(post.getPublishedAt()));
                ngNode.put("Reaction",post.getNumber_of_reaction());
                ngNode.put("Comment",post.getNumber_of_comment());
                ngNode.put("Share",post.getNumber_of_share());
                ngNode.put("ImgUrl", post.getImgUrl());
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
    public List<Facebook> getFacebookFromJson() {
        List<Facebook> articles = new ArrayList<>();
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
            String like = node.get("Reaction").asText();
            String cmt = node.get("Comment").asText();
            String share = node.get("Share").asText();
            String img = node.get("imgUrl").asText();
            articles.add(new Facebook(author,content,prettyTime,sourceUrl,like, cmt, share,img));
        }
        return articles;

    }



    //Truy cap va lay ma nguon trang web
    private WebDriver getPageSource(int numScroll) {
        // Khởi tạo ChromeOptions
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--lang=en");
        // Ẩn cửa sổ Chrome

        //Khoi  tao Webdriver
        System.setProperty("webdriver.chrome.driver", "src/chromedriver.exe");
        WebDriver driver = new ChromeDriver(options);

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




    private List<Facebook> crawlData(List<WebElement> posts, List<Facebook> articles){
        for(WebElement post : posts){
            //Lay link bai viet
            WebElement links = post.findElement(By.xpath(".//span[@class='x4k7w5x x1h91t0o x1h9r5lt x1jfb8zj xv2umb2 x1beo9mf xaigb6o x12ejxvf x3igimt xarpa2k xedcshv x1lytzrv x1t2pt76 x7ja8zs x1qrby5j']//a"));
            String link = extractSubstring(links.getAttribute("href"),"?__cft__");
            System.out.println(link);

            //Lay thoi gian
            String time = extractSubstring(links.getText(), "Shared");
            LocalDateTime prettyTime = parseDateTime(time);

            //Lay noi dung
            String cnt =post.findElement(By.xpath(".//div[@dir='auto']")).getText();
            Content content = new Content(cnt.replace(" \n "," "));

            //Lay hinh anh
            WebElement images = post.findElement(By.xpath("//div[@class='x10l6tqk x13vifvy']//img"));
            String img = null;
            if(images != null){
                img = images.getAttribute("src");
            }

            //Lay luong tuong tac
            WebElement likes = post.findElement(By.xpath(".//div[@class='x6s0dn4 x78zum5 x1iyjqo2 x6ikm8r x10wlt62']"));
            String[] parts = likes.getText().split("\\s*\\n\\s*");
            String like = parts[1];
            System.out.println(like);
            WebElement cmt_share = post.findElement(By.xpath(".//div[@class='x9f619 x1n2onr6 x1ja2u2z x78zum5 x2lah0s x1qughib x1qjc9v5 xozqiw3 x1q0g3np xykv574 xbmpl8g x4cne27 xifccgj']"));
            String[] numbers = cmt_share.getText().split("\\s*\\n\\s*");
            String cmt = numbers[0];
            String share = numbers[1];
            System.out.println(cmt);
            System.out.println(share);

            //Them object vao list
            articles.add(new Facebook("Blockchain.com",content,prettyTime,link,like,cmt,share,img));
        }
        return articles;
    }



    //Chinh sua chuoi
    private String extractSubstring(String originalString, String index) {
        int endIndex = originalString.indexOf(index);
        if (endIndex != -1) {
            return originalString.substring(0, endIndex);
        } else {
            return originalString; // Trả về chuỗi ban đầu nếu không tìm thấy chuỗi index
        }
    }



    //Chinh sua thoi gian
    private LocalDateTime parseDateTime(String input) {
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
        a.crawlFacebookList();
    }
}



