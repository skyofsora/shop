package com.shop.service;

import com.shop.constant.ItemSellStatus;
import com.shop.entity.Item;
import com.shop.entity.ItemImg;
import com.shop.repository.ItemImgRepository;
import com.shop.repository.ItemRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductMaker {
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemImgRepository itemImgRepository;

    public void make(String search) throws InterruptedException {
        String url = "https://shopping.naver.com/home";
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(url);
        driver.findElement(By.cssSelector("span._combineHeader_expansion_search_inner_1VxB3")).click();
        WebElement input = driver.findElement(By.cssSelector("._searchInput_input_text_2Jhbj"));
        input.sendKeys(search);
        input.sendKeys(Keys.ENTER);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int page = 1;
        while (page < 10) {
            js.executeScript("window.scrollTo({ top: 0, behavior: \"smooth\"});");
            for (int i = 0; i < 10; i++) {
                js.executeScript("window.scrollTo({ top: document.body.scrollHeight, behavior: \"smooth\"});");
                Thread.sleep(1000);
            }
            js.executeScript("window.scrollTo({ top: document.body.scrollHeight / 2, behavior: \"smooth\"});");

            List<WebElement> list = driver.findElements(By.cssSelector(".listContainer_list_inner__sqs6W>div>div>div"));

            for (WebElement e : list) {
                try {
                    String title = e.findElement(By.cssSelector(".product_info_tit__c5_pb")).getText();
                    String price = e.findElement(By.cssSelector(".product_num__iQsWh")).getText();
                    String mall;
                    try {
                        mall = e.findElement(By.cssSelector(".product_mall__v9966")).getText();
                    } catch (Exception ee) {
                        mall = e.findElement(By.cssSelector(".product_seller__pfVOU")).getText();
                    }
                    String img = e.findElement(By.cssSelector(".product_img_area__1aA4L>img")).getAttribute("src");
                    try {
                        Item item = new Item();
                        item.setItemNm(title);
                        item.setPrice(Integer.parseInt(price.replaceAll("[^0-9]", "")));
                        item.setItemSellStatus(ItemSellStatus.SELL);
                        item.setStockNumber(123);
                        item.setItemDetail(mall);
                        itemRepository.save(item);
                        ItemImg itemImg = new ItemImg();
                        itemImg.setImgName("이미지");
                        itemImg.setRepImgYn("Y");
                        itemImg.setImgUrl(img);
                        itemImg.setOriImgName("이미지");
                        itemImg.setItem(item);
                        itemImgRepository.save(itemImg);
                    } catch (Exception exception) {
                        System.out.println(exception.getMessage());
                    }
                } catch (Exception ee) {
                    ee.fillInStackTrace();
                }
            }
            boolean check = true;
            while (check) {
                try {
                    driver.findElement(By.cssSelector(".paginator_btn_next__3fcZx")).click();
                    System.out.println(page++ + "페이지 작업 완료");
                    check = false;
                } catch (Exception e) {
                    e.fillInStackTrace();
                }
             }
        }
        System.out.println("모든 작업 완료");
        driver.close();
    }
}
